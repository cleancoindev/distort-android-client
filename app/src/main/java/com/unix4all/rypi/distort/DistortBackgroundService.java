package com.unix4all.rypi.distort;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
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

import javax.net.ssl.HttpsURLConnection;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class DistortBackgroundService extends IntentService {
    // Actions Background Service can perform
    public static final String ACTION_FETCH_PEERS = "com.unix4all.rypi.distort.action.FETCH_PEERS";
    public static final String ACTION_FETCH_MESSAGES = "com.unix4all.rypi.distort.action.FETCH_MESSAGES";
    public static final String ACTION_FETCH_GROUPS = "com.unix4all.rypi.distort.action.FETCH_GROUPS";

    // Saved local files
    private static final String PEERS_FILE_NAME = "peers.json";
    private static final String GROUPS_FILE_NAME = "groups.json";
    private static final String MESSAGES_FILE_NAME_START = "messages-";
    private static final String JSON_EXT = ".json";

    private DistortAuthParams mLoginParams;

    // Keyed by database-ID of peers
    private HashMap<String, DistortPeer> mPeers;

    // Keyed by database-ID of groups
    private HashMap<String, DistortGroup> mGroups;

    // Outermost hashmap is keyed by group database-ID, inner hashmap is keyed by peer full-address (IPFS-Hash[:account-name])
    // This is because may receive messages from identities not yet saved, as well as saves searching for db-id when receiving messages
    private HashMap<String, HashMap<String, ArrayList<DistortMessage>>> mGroupMessagesGroupedByPeer;

    public DistortBackgroundService() {
        super("DistortBackgroundService");

        mPeers = new HashMap<>();
        mGroups = new HashMap<>();
        mGroupMessagesGroupedByPeer = new HashMap<>();
    }

    private void getAuthenticationParams() {
        // Use shared preferences to fetch authorization params
        SharedPreferences sharedPref = this.getSharedPreferences(
                this.getResources().getString(R.string.credentials_preferences_key), Context.MODE_PRIVATE);
        mLoginParams = new DistortAuthParams();
        mLoginParams.setHomeserverAddress(sharedPref.getString(DistortAuthParams.EXTRA_HOMESERVER, null));
        mLoginParams.setHomeserverProtocol(sharedPref.getString(DistortAuthParams.EXTRA_HOMESERVER_PROTOCOL, null));
        mLoginParams.setPeerId(sharedPref.getString(DistortAuthParams.EXTRA_PEER_ID, null));
        mLoginParams.setAccountName(sharedPref.getString(DistortAuthParams.EXTRA_ACCOUNT_NAME, null));
        mLoginParams.setCredential(sharedPref.getString(DistortAuthParams.EXTRA_CREDENTIAL, null));
    }

    // Getting locally stored values
    public static HashMap<String, DistortPeer> getLocalPeers(Context context) {
        HashMap<String, DistortPeer> peerSet = new HashMap<>();
        try {
            FileInputStream fis = context.openFileInput(PEERS_FILE_NAME);
            JsonReader json = new JsonReader(new InputStreamReader(fis));

            // Create JSON file and save
            json.beginArray();
            while(json.hasNext()) {
                DistortPeer peer = DistortPeer.readPeerJson(json);
                peerSet.put(peer.getId(), peer);
            }
            json.endArray();
            json.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return peerSet;
    }
    public static HashMap<String, DistortGroup> getLocalGroups(Context context) {
        HashMap<String, DistortGroup> groupSet = new HashMap<>();
        try {
            FileInputStream fis = context.openFileInput(GROUPS_FILE_NAME);
            JsonReader json = new JsonReader(new InputStreamReader(fis));

            // Create JSON file and save
            json.beginArray();
            while(json.hasNext()) {
                DistortGroup group = DistortGroup.readGroupJson(json);
                groupSet.put(group.getId(), group);
            }
            json.endArray();
            json.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return groupSet;
    }
    public static HashMap<String, ArrayList<DistortMessage>> getLocalMessages(Context context, String groupDatabaseId) {
        HashMap<String, ArrayList<DistortMessage>> messagesSet = new HashMap<>();
        try {
            FileInputStream fis = context.openFileInput(MESSAGES_FILE_NAME_START + groupDatabaseId + JSON_EXT);
            JsonReader json = new JsonReader(new InputStreamReader(fis));

            if(!json.peek().equals(JsonToken.BEGIN_OBJECT)) {
                return new HashMap<>();
            }

            // Create JSON file and save
            json.beginObject();
            while(json.hasNext()) {
                String peerFullAddress = json.nextName();
                messagesSet.put(peerFullAddress, new ArrayList<DistortMessage>());

                json.beginArray();
                while(json.hasNext()) {
                    json.beginObject();
                    DistortMessage m;
                    if(json.nextName().equals(DistortMessage.TYPE_IN)) {
                        m = InMessage.readMessageJson(json);
                    } else {
                        m = OutMessage.readMessageJson(json);
                    }
                    messagesSet.get(peerFullAddress).add(m);
                    json.endObject();
                }
                json.endArray();
            }
            json.endObject();
            json.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return messagesSet;
    }

    // Saving values to local storage
    private void savePeersToLocal() {
        try {
            FileOutputStream fos = this.openFileOutput(PEERS_FILE_NAME, Context.MODE_PRIVATE);
            JsonWriter json = new JsonWriter(new OutputStreamWriter(fos));

            // Read from JSON file
            json.beginArray();
            for(Map.Entry<String, DistortPeer> peer : mPeers.entrySet()) {
                peer.getValue().writePeerJson(json);
            }
            json.endArray();
            json.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void saveGroupsToLocal() {
        try {
            FileOutputStream fos = this.openFileOutput(GROUPS_FILE_NAME, Context.MODE_PRIVATE);
            JsonWriter json = new JsonWriter(new OutputStreamWriter(fos));

            // Read from JSON file
            json.beginArray();
            for(Map.Entry<String, DistortGroup> group : mGroups.entrySet()) {
                group.getValue().writeGroupJson(json);
            }
            json.endArray();
            json.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void saveMessagesToLocal(String groupDatabaseId) {
        try {
            FileOutputStream fos = this.openFileOutput(MESSAGES_FILE_NAME_START + groupDatabaseId + JSON_EXT, Context.MODE_PRIVATE);
            JsonWriter json = new JsonWriter(new OutputStreamWriter(fos));

            // Read from JSON file
            json.beginObject();
            for(Map.Entry<String, ArrayList<DistortMessage>> peerConversations :
                mGroupMessagesGroupedByPeer.get(groupDatabaseId).entrySet()) {

                // Add each message with this peer in this group
                json.name(peerConversations.getKey()).beginArray();
                ArrayList<DistortMessage> messages = peerConversations.getValue();
                for(int i = 0; i < messages.size(); i++) {
                    DistortMessage m = messages.get(i);
                    if(m.getType().equals(DistortMessage.TYPE_IN)) {
                        json.beginObject().name(DistortMessage.TYPE_IN);
                        ((InMessage)m).writeMessageJson(json);
                        json.endObject();
                    } else {
                        json.beginObject().name(DistortMessage.TYPE_OUT);
                        ((OutMessage)m).writeMessageJson(json);
                        json.endObject();
                    }
                }
                json.endArray();
            }
            json.endObject();
            json.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts this service to perform action fetching peers. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFetchPeers(Context context) {
        Intent intent = new Intent(context, DistortBackgroundService.class);
        intent.setAction(ACTION_FETCH_PEERS);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action fetching messages. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFetchMessages(Context context) {
        Intent intent = new Intent(context, DistortBackgroundService.class);
        intent.setAction(ACTION_FETCH_MESSAGES);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action fetching groups. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFetchGroups(Context context) {
        Intent intent = new Intent(context, DistortBackgroundService.class);
        intent.setAction(ACTION_FETCH_GROUPS);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

            // Get stored params
            getAuthenticationParams();

            final String action = intent.getAction();
            if (ACTION_FETCH_PEERS.equals(action)) {
                Log.i("DISTORT-SERVICE", "Starting Fetch Peers...");
                handleActionFetchPeers();
                Log.i("DISTORT-SERVICE", "Finished Fetch Peers.");
            } else if (ACTION_FETCH_MESSAGES.equals(action)) {
                Log.i("DISTORT-SERVICE", "Starting Fetch Messages...");
                handleActionFetchMessages();
                Log.i("DISTORT-SERVICE", "Finished Fetch Messages.");
            } else if(ACTION_FETCH_GROUPS.equals(action)) {
                Log.i("DISTORT-SERVICE", "Starting Fetch Groups...");
                handleActionFetchGroups();
                Log.i("DISTORT-SERVICE", "Finished Fetch Groups.");
            }
        }
    }

    /**
     * Handle action fetch peers in the provided background thread.
     */
    private void handleActionFetchPeers() {
        // Attempt authentication against a network service.
        try {
            JsonReader response = null;
            URL homeserverEndpoint = new URL(mLoginParams.getHomeserverAddress() + "peers");
            if(DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                HttpsURLConnection myConnection;
                myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.GetJSONFromURL(myConnection, mLoginParams);
            } else {
                HttpURLConnection myConnection;
                myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.GetJSONFromURL(myConnection, mLoginParams);
            }

            // Read all groups
            response.beginArray();
            while(response.hasNext()) {
                DistortPeer p = DistortPeer.readPeerJson(response);
                mPeers.put(p.getId(), p);
            }
            response.endArray();
            response.close();
            savePeersToLocal();

            // Let know about the successful service
            Intent intent = new Intent();
            intent.setAction(ACTION_FETCH_PEERS);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        } catch (DistortJson.DistortException e) {
            // TODO: Handle background errors
            e.printStackTrace();
            Log.e("FETCH-PEERS", e.getMessage() + " : " + String.valueOf(e.getResponseCode()));
        } catch (IOException e) {
            // TODO: Handle background errors
            e.printStackTrace();
            Log.e("FETCH-PEERS", e.getMessage());
        }
    }

    /**
     * Handle action fetch messages in the provided background thread.
     */
    private void handleActionFetchMessages() {
        // Requires groups and peers in memory
        mGroups = getLocalGroups(this);
        Log.d("LOCAL-GROUPS", "Size:" + String.valueOf(mGroups.size()));
        mPeers = getLocalPeers(this);
        Log.d("LOCAL-PEERS", "Size:" + String.valueOf(mPeers.size()));

        for (Map.Entry<String, DistortGroup> groupEntry : mGroups.entrySet()) {
            Map<String, ArrayList<DistortMessage>> peerConversations = null;

            // Determine last known index for this group by any peer, as well as create necessary sets
            Integer startIndex = 0;
            if(mGroupMessagesGroupedByPeer.get(groupEntry.getKey()) != null) {
                peerConversations = mGroupMessagesGroupedByPeer.get(groupEntry.getKey());
            } else {
                mGroupMessagesGroupedByPeer.put(groupEntry.getKey(), new HashMap<String, ArrayList<DistortMessage>>());
                peerConversations = mGroupMessagesGroupedByPeer.get(groupEntry.getKey());
            }

            for (Map.Entry<String, DistortPeer> peer : mPeers.entrySet()) {
                String fullAddress = peer.getValue().getFullAddress();
                ArrayList<DistortMessage> peerConvo = peerConversations.get(fullAddress);
                if(peerConvo == null) {
                    peerConversations.put(fullAddress, new ArrayList<DistortMessage>());
                } else {
                    startIndex = Math.max(peerConvo.get(peerConvo.size() - 1).getIndex(), startIndex);
                }
            }

            // Attempt authentication against a network service.
            try {
                JsonReader response = null;
                String url = mLoginParams.getHomeserverAddress() + "groups/" + groupEntry.getValue().getName() + "/" + String.valueOf(startIndex);
                URL homeserverEndpoint = new URL(url);
                if (DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.GetJSONFromURL(myConnection, mLoginParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.GetJSONFromURL(myConnection, mLoginParams);
                }

                // Read all messages in messages and out messages
                ArrayList<InMessage> inMessages = new ArrayList<>();
                ArrayList<OutMessage> outMessages = new ArrayList<>();
                response.beginObject();
                while (response.hasNext()) {
                    String key = response.nextName();
                    Log.d("GET-GROUP-MESSAGES", key);

                    if (key.equals("in")) {
                        inMessages = InMessage.readArrayJson(response);
                    } else if (key.equals("out")) {
                        outMessages = OutMessage.readArrayJson(response);
                    } else {
                        response.skipValue();
                    }
                }
                response.endObject();
                response.close();

                // Begin creating a consolidated and sorted list of new messages
                final DistortMessage allMessages[] = new DistortMessage[inMessages.size() + outMessages.size()];
                for (int inIndex = 0; inIndex < inMessages.size(); inIndex++) {
                    DistortMessage m = (DistortMessage) inMessages.get(inIndex);
                    allMessages[m.getIndex() - startIndex] = m;
                }
                for (int outIndex = 0; outIndex < outMessages.size(); outIndex++) {
                    DistortMessage m = (DistortMessage) outMessages.get(outIndex);
                    allMessages[m.getIndex() - startIndex] = m;
                }

                // Insert into grouped sets for easy searching later
                for(int i = 0; i < allMessages.length; i++) {
                    if(allMessages[i].getType().equals(DistortMessage.TYPE_IN)) {
                        InMessage inMsg = (InMessage) allMessages[i];
                        String peerAddress = DistortPeer.toFullAddress(inMsg.getFromPeerId(), inMsg.getFromAccount());
                        ArrayList<DistortMessage> conversation = peerConversations.get(peerAddress);
                        if(conversation == null) {
                            conversation = new ArrayList<>();
                        }
                        conversation.add(inMsg);
                    } else {
                        OutMessage outMsg = (OutMessage) allMessages[i];
                        String peerAddress = DistortPeer.toFullAddress(outMsg.getToPeerId(), outMsg.getToAccount());
                        ArrayList<DistortMessage> conversation = peerConversations.get(peerAddress);
                        if(conversation == null) {
                            conversation = new ArrayList<>();
                        }
                        conversation.add(outMsg);
                    }
                }
                saveMessagesToLocal(groupEntry.getKey());
            } catch (DistortJson.DistortException e) {
                // TODO: Handle background errors
                e.printStackTrace();
                Log.e("FETCH-MESSAGES", e.getMessage() + " : " + String.valueOf(e.getResponseCode()));

                break;
            } catch (IOException e) {
                // TODO: Handle background errors
                e.printStackTrace();
                Log.e("FETCH-MESSAGES", e.getMessage());

                break;
            }
        }

        // Let know about the successful service
        Intent intent = new Intent();
        intent.setAction(ACTION_FETCH_MESSAGES);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void handleActionFetchGroups() {
        // Attempt authentication against a network service.
        try {
            JsonReader response = null;
            URL homeserverEndpoint = new URL(mLoginParams.getHomeserverAddress() + "groups");
            if(DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                HttpsURLConnection myConnection;
                myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.GetJSONFromURL(myConnection, mLoginParams);
            } else {
                HttpURLConnection myConnection;
                myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.GetJSONFromURL(myConnection, mLoginParams);
            }

            // Read all groups
            response.beginArray();
            while(response.hasNext()) {
                DistortGroup g = DistortGroup.readGroupJson(response);
                mGroups.put(g.getId(), g);
            }
            response.endArray();
            response.close();
            saveGroupsToLocal();

            // Let know about the successful service
            Intent intent = new Intent();
            intent.setAction(ACTION_FETCH_GROUPS);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        } catch (DistortJson.DistortException e) {
            // TODO: Handle background errors
            e.printStackTrace();
            Log.e("FETCH-GROUPS", e.getMessage() + " : " + String.valueOf(e.getResponseCode()));

        } catch (IOException e) {
            // TODO: Handle background errors
            e.printStackTrace();
            Log.e("FETCH-GROUPS", e.getMessage());
        }
    }
}
