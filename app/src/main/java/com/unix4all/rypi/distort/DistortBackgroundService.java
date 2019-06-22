package com.unix4all.rypi.distort;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class DistortBackgroundService extends IntentService {
    // Actions Background Service can perform
    public static final String ACTION_FETCH_ACCOUNT = "com.unix4all.rypi.distort.action.FETCH_ACCOUNT";
    public static final String ACTION_FETCH_PEERS = "com.unix4all.rypi.distort.action.FETCH_PEERS";
    public static final String ACTION_FETCH_MESSAGES = "com.unix4all.rypi.distort.action.FETCH_MESSAGES";
    public static final String ACTION_FETCH_GROUPS = "com.unix4all.rypi.distort.action.FETCH_GROUPS";
    public static final String ACTION_FETCH_CONVERSATIONS = "com.unix4all.rypi.distort.action.FETCH_CONVERSATIONS";

    // Persistent service and scheduling
    public static final String ACTION_SCHEDULE_PRIMARY_SERVICES = "com.unix4all.rypi.distort.action.SCHEDULE_PRIMARY_SERVICES";
    public static final String ACTION_SCHEDULE_SECONDARY_SERVICES = "com.unix4all.rypi.distort.action.SCHEDULE_SECONDARY_SERVICES";

    // Report errors to activities
    public static final String BACKGROUND_ERROR = "com.unix4all.rypi.distort.action.BACKGROUND_ERROR";

    // Saved local files
    private static final String PEERS_FILE_NAME = "peers.json";
    private static final String GROUPS_FILE_NAME = "groups.json";
    private static final String CONVERSATIONS_FILE_NAME = "conversations.json";
    private static final AtomicInteger notificationId = new AtomicInteger(0);

    private DistortAuthParams mLoginParams;
    private MessageDatabaseHelper mDatabaseHelper;


    public DistortBackgroundService() {
        super("DistortBackgroundService");
    }

    @Override
    public void onDestroy() {
        if(mDatabaseHelper != null) {
            mDatabaseHelper.close();
        }

        super.onDestroy();
    }

    private void getAuthenticationParams() {
        // Use shared preferences to fetch authorization params
        SharedPreferences sharedPref = this.getSharedPreferences(
                this.getResources().getString(R.string.account_credentials_preferences_key), Context.MODE_PRIVATE);
        mLoginParams = DistortAuthParams.getAuthenticationParams(this);
    }

    private void reportError(String errorString) {
        if(errorString.length() == 0) {
            return;
        }

        Intent intent = new Intent();
        intent.setAction(BACKGROUND_ERROR);

        // Trim and first letter uppercase
        errorString = errorString.trim();
        errorString = errorString.substring(0, 1).toUpperCase() + errorString.substring(1);

        intent.putExtra("error", errorString);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    // Getting locally stored values
    // Load account from shared preferences
    public static @Nullable DistortAccount getLocalAccount(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.account_preferences_key), Context.MODE_PRIVATE);
        String peerId = sharedPref.getString("peerId", null);
        String accountName = sharedPref.getString("accountName", null);
        Boolean enabled = sharedPref.getBoolean("enabled", false);
        String activeGroup = sharedPref.getString("activeGroup", null);

        if(peerId != null && accountName != null && enabled != null) {
            return new DistortAccount(peerId, accountName, enabled, activeGroup);
        } else {
            return null;
        }
    }
    // Hashmap is keyed by peer full-address
    public static HashMap<String, DistortPeer> getLocalPeers(Context context) {
        HashMap<String, DistortPeer> peerSet = new HashMap<>();
        try {
            FileInputStream fis = context.openFileInput(PEERS_FILE_NAME);
            JsonReader json = new JsonReader(new InputStreamReader(fis));

            if(!json.peek().equals(JsonToken.BEGIN_ARRAY)) {
                return peerSet;
            }

            // Create JSON file and save
            json.beginArray();
            while (json.hasNext()) {
                DistortPeer peer = DistortPeer.readJson(json);
                peerSet.put(peer.getFullAddress(), peer);
            }
            json.endArray();
            json.close();
        } catch (Exception e) {
            Log.e("STORED-PEERS", "Could not parse peers file");
        }
        return peerSet;
    }
    // Hashmap is keyed by group name
    public static HashMap<String, DistortGroup> getLocalGroups(Context context) {
        HashMap<String, DistortGroup> groupSet = new HashMap<>();
        try {
            FileInputStream fis = context.openFileInput(GROUPS_FILE_NAME);
            JsonReader json = new JsonReader(new InputStreamReader(fis));

            if(!json.peek().equals(JsonToken.BEGIN_ARRAY)) {
                return groupSet;
            }

            // Create JSON file and save
            json.beginArray();
            while(json.hasNext()) {
                DistortGroup group = DistortGroup.readJson(json);
                groupSet.put(group.getName(), group);
            }
            json.endArray();
            json.close();
        } catch (Exception e) {
            Log.e("STORED-GROUPS", "Could not parse groups file");
        }
        return groupSet;
    }
    // Hashmap is keyed by conversation unique label, 'group:' + peerFullAddress + [':' + accountName]
    public static HashMap<String, DistortConversation> getLocalConversations(Context context) {
        HashMap<String, DistortConversation> conversationSet = new HashMap<>();
        try {
            FileInputStream fis = context.openFileInput(CONVERSATIONS_FILE_NAME);
            JsonReader json = new JsonReader(new InputStreamReader(fis));

            // Create JSON file and save
            json.beginArray();
            while(json.hasNext()) {
                DistortConversation c = DistortConversation.readJson(json, null);
                conversationSet.put(c.getUniqueLabel(), c);
            }
            json.endArray();
            json.close();
        } catch (Exception e) {
            Log.e("STORED-CONVERSATIONS", "Could not parse conversations file");
        }
        return conversationSet;
    }
    public static ArrayList<DistortMessage> getLocalConversationMessages(MessageDatabaseHelper db, String conversationLabel, @Nullable Integer startIndex, @Nullable Integer endIndex) {
        ArrayList<DistortMessage> messages = new ArrayList<>();
        if (conversationLabel == null || conversationLabel.isEmpty()) {
            return messages;
        }

        messages = db.getMessagesInRange(conversationLabel, startIndex == null ? 0 : startIndex, endIndex);
        db.close();

        return messages;
    }

    // Saving values to local storage
    // Save account to shared preferences
    private void saveAccountToLocal(DistortAccount account) {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.account_preferences_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("peerId", account.getPeerId());
        editor.putString("accountName", account.getAccountName());
        editor.putBoolean("enabled", account.getEnabled());
        editor.putString("activeGroup", account.getActiveGroup());
        editor.apply();
    }
    // Hashmap keyed by peer full-address
    private void savePeersToLocal(HashMap<String, DistortPeer> peers) {
        try {
            FileOutputStream fos = this.openFileOutput(PEERS_FILE_NAME, Context.MODE_PRIVATE);
            JsonWriter json = new JsonWriter(new OutputStreamWriter(fos));

            // Read from JSON file
            json.beginArray();
            for(Map.Entry<String, DistortPeer> peer : peers.entrySet()) {
                peer.getValue().writeJson(json);
            }
            json.endArray();
            json.close();
        } catch (Exception e) {
            reportError(e.getMessage());
        }
    }
    // Hashmap keyed by group database-ID
    private void saveGroupsToLocal(HashMap<String, DistortGroup> groups) {
        try {
            FileOutputStream fos = this.openFileOutput(GROUPS_FILE_NAME, Context.MODE_PRIVATE);
            JsonWriter json = new JsonWriter(new OutputStreamWriter(fos));

            // Read from JSON file
            json.beginArray();
            for(Map.Entry<String, DistortGroup> group : groups.entrySet()) {
                group.getValue().writeJson(json);
            }
            json.endArray();
            json.close();
        } catch (Exception e) {
            reportError(e.getMessage());
        }
    }
    // Hashmap keyed by conversation database-ID
    private void saveConversationsToLocal(HashMap<String, DistortConversation> conversations) {
        try {
            FileOutputStream fos = this.openFileOutput(CONVERSATIONS_FILE_NAME, Context.MODE_PRIVATE);
            JsonWriter json = new JsonWriter(new OutputStreamWriter(fos));

            // Read from JSON file
            json.beginArray();
            for(Map.Entry<String, DistortConversation> conversation : conversations.entrySet()) {
                conversation.getValue().writeJson(json);
            }
            json.endArray();
            json.close();
        } catch (Exception e) {
            reportError(e.getMessage());
        }
    }
    private void saveConversationMessagesToLocal(String conversationLabel, ArrayList<DistortMessage> messages) {
        // Add each message with this peer in this group
        for(DistortMessage msg : messages) {
            mDatabaseHelper.addMessage(conversationLabel, msg);
        }
    }

    // Helpers for starting services
    public static void startActionFetchAccount(Context context) {
        Intent intent = new Intent(context, DistortBackgroundService.class);
        intent.setAction(ACTION_FETCH_ACCOUNT);
        context.startService(intent);
    }
    public static void startActionFetchPeers(Context context) {
        Intent intent = new Intent(context, DistortBackgroundService.class);
        intent.setAction(ACTION_FETCH_PEERS);
        context.startService(intent);
    }
    public static void startActionFetchMessages(Context context, String conversationLabel) {
        Intent intent = new Intent(context, DistortBackgroundService.class);
        intent.setAction(ACTION_FETCH_MESSAGES);

        intent.putExtra("conversationLabel", conversationLabel);
        context.startService(intent);
    }
    public static void startActionFetchGroups(Context context) {
        Intent intent = new Intent(context, DistortBackgroundService.class);
        intent.setAction(ACTION_FETCH_GROUPS);
        context.startService(intent);
    }
    public static void startActionFetchConversations(Context context, String groupName) {
        Intent intent = new Intent(context, DistortBackgroundService.class);
        intent.setAction(ACTION_FETCH_CONVERSATIONS);

        intent.putExtra("groupName", groupName);

        context.startService(intent);
    }
    public static void startActionSchedulePrimaryServices(Context context) {
        Intent intent = new Intent(context, DistortBackgroundService.class);
        intent.setAction(ACTION_SCHEDULE_PRIMARY_SERVICES);

        context.startService(intent);
    }
    public static void startActionScheduleSecondaryServices(Context context) {
        Intent intent = new Intent(context, DistortBackgroundService.class);
        intent.setAction(ACTION_SCHEDULE_SECONDARY_SERVICES);

        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

            // Get stored params
            getAuthenticationParams();
            String fullAddress = mLoginParams.getFullAddress();

            // Create database connection if not set
            if(mDatabaseHelper == null) {
                mDatabaseHelper = new MessageDatabaseHelper(this, fullAddress);
            } else if(!mDatabaseHelper.getFullAddress().equals(fullAddress)) {
                mDatabaseHelper.close();
                mDatabaseHelper = new MessageDatabaseHelper(this, fullAddress);
            }

            final String action = intent.getAction();
            if (ACTION_FETCH_ACCOUNT.equals(action)) {
                Log.i("DISTORT-SERVICE", "Starting Fetch Account...");
                handleActionFetchAccount();
                Log.i("DISTORT-SERVICE", "Finished Fetch Account.");
            } else if (ACTION_FETCH_PEERS.equals(action)) {
                Log.i("DISTORT-SERVICE", "Starting Fetch Peers...");
                handleActionFetchPeers();
                Log.i("DISTORT-SERVICE", "Finished Fetch Peers.");
            } else if (ACTION_FETCH_MESSAGES.equals(action)) {
                Log.i("DISTORT-SERVICE", "Starting Fetch Messages...");
                String conversationLabel = intent.getStringExtra("conversationLabel");
                handleActionFetchConversationMessages(conversationLabel);
                Log.i("DISTORT-SERVICE", "Finished Fetch Messages.");
            } else if(ACTION_FETCH_GROUPS.equals(action)) {
                Log.i("DISTORT-SERVICE", "Starting Fetch Groups...");
                handleActionFetchGroups();
                Log.i("DISTORT-SERVICE", "Finished Fetch Groups.");
            } else if(ACTION_FETCH_CONVERSATIONS.equals(action)) {
                Log.i("DISTORT-SERVICE", "Starting Fetch Conversations...");
                String groupName = intent.getStringExtra("groupName");
                handleActionFetchGroupConversations(groupName);
                Log.i("DISTORT-SERVICE", "Finished Fetch Conversations.");
            } else if(ACTION_SCHEDULE_PRIMARY_SERVICES.equals(action)) {
                Log.i("DISTORT-SERVICE", "Starting Primary Service Scheduling...");
                handleActionSchedulePrimaryServices();
                Log.i("DISTORT-SERVICE", "Finished Primary Service Scheduling.");
            } else if(ACTION_SCHEDULE_SECONDARY_SERVICES.equals(action)) {
                Log.i("DISTORT-SERVICE", "Starting Secondary Service Scheduling...");
                handleActionScheduleSecondaryServices();
                Log.i("DISTORT-SERVICE", "Finished Secondary Service Scheduling.");
            }
        }
    }

    private @Nullable DistortAccount handleActionFetchAccount() {
        DistortAccount account = null;

        // Attempt authentication against a network service.
        try {
            JsonReader response = null;
            URL homeserverEndpoint = new URL(mLoginParams.getHomeserverAddress() + "account");
            if(DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                HttpsURLConnection myConnection;
                myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getJSONFromURL(myConnection, mLoginParams);
            } else {
                HttpURLConnection myConnection;
                myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getJSONFromURL(myConnection, mLoginParams);
            }

            // Read account object
            account = DistortAccount.readJson(response);
            response.close();
            saveAccountToLocal(account);

            // Let know about the successful service
            Intent intent = new Intent();
            intent.setAction(ACTION_FETCH_ACCOUNT);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        } catch (DistortJson.DistortException e) {
            // TODO: Base message off of response codes
            reportError(e.getMessage());
            e.printStackTrace();
            Log.e("FETCH-ACCOUNT", String.valueOf(e.getResponseCode()));
        } catch (IOException e) {
            reportError(e.getMessage());
            e.printStackTrace();
        }

        return account;
    }
    private HashMap<String, DistortPeer> handleActionFetchPeers() {
        HashMap<String, DistortPeer> peers = new HashMap<>();

        // Attempt authentication against a network service.
        try {
            JsonReader response = null;
            URL homeserverEndpoint = new URL(mLoginParams.getHomeserverAddress() + "peers");
            if(DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                HttpsURLConnection myConnection;
                myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getJSONFromURL(myConnection, mLoginParams);
            } else {
                HttpURLConnection myConnection;
                myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getJSONFromURL(myConnection, mLoginParams);
            }

            // Read all groups
            response.beginArray();
            while(response.hasNext()) {
                DistortPeer p = DistortPeer.readJson(response);
                peers.put(p.getFullAddress(), p);
            }
            response.endArray();
            response.close();
            savePeersToLocal(peers);

            // Let know about the successful service
            Intent intent = new Intent();
            intent.setAction(ACTION_FETCH_PEERS);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        } catch (DistortJson.DistortException e) {
            // TODO: Base message off of response codes
            reportError(e.getMessage());
            e.printStackTrace();
            Log.e("FETCH-PEERS", String.valueOf(e.getResponseCode()));
        } catch (IOException e) {
            reportError(e.getMessage());
            e.printStackTrace();
        }
        return peers;
    }

    /**
     * Handle action fetch messages in the provided background thread.
     */
    private ArrayList<DistortMessage> handleActionFetchConversationMessages(String conversationLabel) {
        // Load conversations from storage
        HashMap<String, DistortConversation> conversations = getLocalConversations(this);
        final DistortConversation localConversation = conversations.get(conversationLabel);

        // Store previously retrieved messages to append to
        ArrayList<DistortMessage> existingMessages;

        // Find existing copy of conversation in local storage
        Integer existingHeight;
        Integer startIndex;
        if(localConversation != null) {
            // Get known conversation messages and determine earliest message with volatile state (enqueued)
            ArrayList<DistortMessage> storedMessages = getLocalConversationMessages(mDatabaseHelper, conversationLabel, null, null);
            if(storedMessages.size() == 0) {
                startIndex = 0;
                existingHeight = 0;
            } else {
                // Default to starting one message after last known message (in or out)
                startIndex = storedMessages.get(storedMessages.size() - 1).getIndex() + 1;
                existingHeight = startIndex;

                for (int i = 0; i < storedMessages.size(); i++) {
                    DistortMessage m = storedMessages.get(i);
                    if (m.getType().equals(DistortMessage.TYPE_OUT)) {
                        if (((OutMessage) m).getStatus().equals(OutMessage.STATUS_ENQUEUED)) {
                            startIndex = m.getIndex();
                            break;
                        }
                    }
                }
            }

            // Found correct end of known-messages below starting-index, append to list
            existingMessages = new ArrayList<>(storedMessages.subList(0, startIndex));
        } else {
            return new ArrayList<>();
        }

        // See if conversation has an associated peer
        DistortPeer peer = getLocalPeers(this).get(localConversation.getFullAddress());
        if(peer != null) {
            localConversation.setNickname(peer.getNickname());
        }

        // Load groups from storage
        HashMap<String, DistortGroup> groups = getLocalGroups(this);
        String groupName = localConversation.getGroup();
        if(groups == null || groups.get(groupName) == null) {
            String errorString = "Group:" + groupName + " is not known locally";
            reportError(errorString);
            Log.e("FETCH-MESSAGES", errorString);
            return existingMessages;
        }
        DistortGroup group = groups.get(groupName);

        Log.d("FETCH-MESSAGES", "Fetching messages from group: " + group.getName() + ", peer: " + localConversation.getFriendlyName());

        // Attempt authentication against a network service.
        Context context = getApplicationContext();
        try {
            JsonReader response = null;
            String url = mLoginParams.getHomeserverAddress() + "groups/" + Uri.encode(group.getName()) + "/" + String.valueOf(startIndex);

            HashMap<String, String> queryParams = new HashMap<>();
            queryParams.put("peerId", localConversation.getPeerId());
            queryParams.put("accountName", localConversation.getAccountName());

            url += DistortJson.getQueryString(queryParams);
            URL homeserverEndpoint = new URL(url);
            if (DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                HttpsURLConnection myConnection;
                myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getJSONFromURL(myConnection, mLoginParams);
            } else {
                HttpURLConnection myConnection;
                myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getJSONFromURL(myConnection, mLoginParams);
            }

            // Read all messages in messages and out messages
            ArrayList<InMessage> inMessages = new ArrayList<>();
            ArrayList<OutMessage> outMessages = new ArrayList<>();
            response.beginObject();
            while (response.hasNext()) {
                String key = response.nextName();
                if (key.equals("in")) {
                    inMessages = InMessage.readArrayJsonForConversation(response);
                } else if (key.equals("out")) {
                    outMessages = OutMessage.readArrayJsonForConversation(response);
                } else {
                    response.skipValue();
                }
            }
            response.endObject();
            response.close();

            // Begin creating a consolidated and sorted list of new messages
            final DistortMessage receivedMessages[] = new DistortMessage[inMessages.size() + outMessages.size()];
            for (int inIndex = 0; inIndex < inMessages.size(); inIndex++) {
                DistortMessage m = (DistortMessage) inMessages.get(inIndex);
                receivedMessages[m.getIndex() - startIndex] = m;
            }
            for (int outIndex = 0; outIndex < outMessages.size(); outIndex++) {
                DistortMessage m = (DistortMessage) outMessages.get(outIndex);
                receivedMessages[m.getIndex() - startIndex] = m;
            }

            // Append to pre-discovered messages and determine if there are updates to notify
            boolean notifyOfMessage = false;
            ArrayList<Integer> updatedMessages = new ArrayList<>();
            for(DistortMessage dm : receivedMessages) {
                int index = dm.getIndex();

                // Determine if message is new or old
                if(index < existingHeight) {
                    // Message status may have changed since last updated
                    updatedMessages.add(index);
                } else {
                    // Message is guaranteed to be new to client, add to messages that will be refreshed
                    updatedMessages.add(index);

                    // Only notify activity if this is an incoming message
                    if(dm.getType().equals(DistortMessage.TYPE_IN)) {
                        notifyOfMessage = true;
                    }
                }

                // Add sorted message to list of all messages
                existingMessages.add(dm);
            }

            // Save messages
            saveConversationMessagesToLocal(conversationLabel, existingMessages);

            // Let know about the successful service
            SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.messaging_preference_key), Context.MODE_PRIVATE);
            String activeConversation = sharedPref.getString("activeConversation", "");

            // Use group-Id + peer full-address to uniquely identify a conversation,
            // even before a message has been sent or received
            if(activeConversation.equals(localConversation.getUniqueLabel())) {
                // Broadcast to messaging
                Intent intent = new Intent();
                intent.setAction(ACTION_FETCH_MESSAGES);
                intent.putExtra("conversationLabel", conversationLabel);

                if(updatedMessages.size() > 0) {
                    // Store updated messages to string for fast refreshes
                    String updatedMessagesString = "";
                    StringBuilder sb = new StringBuilder();
                    sb.append(updatedMessages.get(0));
                    for (int i = 1; i < updatedMessages.size(); i++) {
                        sb.append(":" + updatedMessages.get(i));
                    }
                    updatedMessagesString = sb.toString();
                    intent.putExtra("updatedMessages", updatedMessagesString);
                }

                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else if(notifyOfMessage) {
                // Received a previously unknown in-message, notify
                Intent intent = new Intent(context, MessagingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                // Pass arguments to launching activity
                intent.putExtra("icon", localConversation.getFriendlyName().substring(0, 1));
                intent.putExtra("peerId", localConversation.getPeerId());
                intent.putExtra("accountName", localConversation.getAccountName());
                intent.putExtra("groupName", groupName);
                intent.putExtra("conversationLabel", conversationLabel);
                PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                // Notification values
                String title = getString(R.string.notification_messages_title);
                String text = getString(R.string.notification_messages_text);
                Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                // Build notification with compatibility
                // https://stackoverflow.com/questions/46990995/on-android-8-1-api-27-notification-does-not-display
                NotificationCompat.Builder builder;
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    int importance = NotificationManager.IMPORTANCE_HIGH;
                    NotificationChannel notificationChannel = notificationManager.getNotificationChannel(conversationLabel);
                    if (notificationChannel == null) {
                        notificationChannel = new NotificationChannel(conversationLabel, localConversation.getFullAddress(), importance);
                        notificationChannel.setLightColor(Color.GREEN);
                        notificationChannel.enableVibration(true);
                        notificationManager.createNotificationChannel(notificationChannel);
                    }
                    builder = new NotificationCompat.Builder(context, conversationLabel);
                    builder.setContentTitle(title)                            // required
                            .setSmallIcon(R.drawable.ic_message_notification)   // required
                            .setContentText(text) // required
                            .setContentIntent(pIntent)
                            .setAutoCancel(true)
                            .setSound(ringtoneUri)
                            .setLights(0xFF00FF00, 500, 1500)
                            .setVibrate(new long[]{100, 300, 300, 400});
                } else {
                    builder = new NotificationCompat.Builder(context, conversationLabel);
                    builder.setContentTitle(title)                            // required
                            .setSmallIcon(R.drawable.ic_message_notification)   // required
                            .setContentText(text) // required
                            .setContentIntent(pIntent)
                            .setAutoCancel(true)
                            .setSound(ringtoneUri)
                            .setLights(0xFF00FF00, 500, 1500)
                            .setVibrate(new long[]{100, 300, 300, 400});
                }

                Notification notification = builder.build();
                notificationManager.notify(notificationId.getAndIncrement(), notification);
            }

        } catch (DistortJson.DistortException e) {
            // TODO: Base message off of response codes
            reportError(e.getMessage());
            e.printStackTrace();
            Log.e("FETCH-MESSAGES", String.valueOf(e.getResponseCode()));
        } catch (IOException e) {
            reportError(e.getMessage());
            e.printStackTrace();
        }
        return existingMessages;
    }

    private HashMap<String, DistortGroup> handleActionFetchGroups() {
        HashMap<String, DistortGroup> groups = new HashMap<>();

        // Attempt authentication against a network service.
        try {
            JsonReader response = null;
            URL homeserverEndpoint = new URL(mLoginParams.getHomeserverAddress() + "groups");
            if(DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                HttpsURLConnection myConnection;
                myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getJSONFromURL(myConnection, mLoginParams);
            } else {
                HttpURLConnection myConnection;
                myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getJSONFromURL(myConnection, mLoginParams);
            }

            // Read all groups
            response.beginArray();
            while(response.hasNext()) {
                DistortGroup g = DistortGroup.readJson(response);

                groups.put(g.getName(), g);
            }
            response.endArray();
            response.close();
            saveGroupsToLocal(groups);

            // Let know about the successful service
            Intent intent = new Intent();
            intent.setAction(ACTION_FETCH_GROUPS);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        } catch (DistortJson.DistortException e) {
            // TODO: Base message off of response codes
            reportError(e.getMessage());
            e.printStackTrace();
            Log.e("FETCH-GROUPS", String.valueOf(e.getResponseCode()));
        } catch (IOException e) {
            reportError(e.getMessage());
            e.printStackTrace();
        }
        return groups;
    }

    private HashMap<String, DistortConversation> handleActionFetchGroupConversations(String groupName) {
        // Load groups from storage
        HashMap<String, DistortGroup> groups = getLocalGroups(this);
        if(groups == null || groups.get(groupName) == null) {
            String errorStr = "Group:" + groupName + " is not known locally";
            reportError(errorStr);
            return null;
        }
        DistortGroup group = groups.get(groupName);

        // Use empty set since conversations may be deleted
        HashMap<String, DistortConversation> conversations = new HashMap<>();

        // Attempt authentication against a network service.
        try {
            JsonReader response = null;
            URL homeserverEndpoint = new URL(mLoginParams.getHomeserverAddress() + "groups/" + Uri.encode(group.getName()));
            if(DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                HttpsURLConnection myConnection;
                myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getJSONFromURL(myConnection, mLoginParams);
            } else {
                HttpURLConnection myConnection;
                myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getJSONFromURL(myConnection, mLoginParams);
            }

            // Read all groups
            response.beginArray();
            while(response.hasNext()) {
                DistortConversation c = DistortConversation.readJson(response, group.getName());
                conversations.put(c.getUniqueLabel(), c);
            }
            response.endArray();
            response.close();
            saveConversationsToLocal(conversations);

            // Let know about the successful service
            Intent intent = new Intent();
            intent.setAction(ACTION_FETCH_CONVERSATIONS);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        } catch (DistortJson.DistortException e) {
            // TODO: Base message off of response codes
            reportError(e.getMessage());
            e.printStackTrace();
            Log.e("FETCH-CONVERSATIONS", String.valueOf(e.getResponseCode()));
        } catch (IOException e) {
            reportError(e.getMessage());
            e.printStackTrace();
        }
        return conversations;
    }


    private void handleActionSchedulePrimaryServices() {
        try {
            HashMap<String, DistortConversation> storedConversations = getLocalConversations(this);

            // Fetch and traverse every group
            HashMap<String, DistortGroup> groups = getLocalGroups(this);
            for (Map.Entry<String, DistortGroup> group : groups.entrySet()) {

                // Fetch and traverse every conversation in this anonymity group
                HashMap<String, DistortConversation> conversations = handleActionFetchGroupConversations(group.getValue().getName());
                for (Map.Entry<String, DistortConversation> conversation : conversations.entrySet()) {
                    DistortConversation newConversation = conversation.getValue();

                    // Determine if conversation is out-of-date
                    DistortConversation stored = storedConversations.get(newConversation.getUniqueLabel());

                    if (stored == null || stored.getLatestStatusChangeDate().compareTo(newConversation.getLatestStatusChangeDate()) != 0) {
                        handleActionFetchConversationMessages(newConversation.getUniqueLabel());
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void handleActionScheduleSecondaryServices() {
        try {
            handleActionFetchGroups();
            handleActionFetchAccount();
            handleActionFetchPeers();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
