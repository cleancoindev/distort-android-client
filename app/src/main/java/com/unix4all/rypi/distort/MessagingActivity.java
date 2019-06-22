package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MessagingActivity extends AppCompatActivity {
    private final MessagingActivity mActivity = this;

    private DistortAuthParams mLoginParams;
    private MessageDatabaseHelper mDatabaseHelper;

    // Header fields
    private TextView mIcon;
    private TextView mName;
    private TextView mFullAddress;

    // Peer we are messaging
    private String mPeerId;
    private String mAccountName;
    private String mFriendlyName;
    private String mFullAddressString;
    private @Nullable DistortPeer mPeer;
    private @Nullable DistortConversation mConversation;

    // Group we are messaging on
    private DistortGroup mGroup;
    private MessageServiceBroadcastReceiver mServiceReceiver;
    private BackgroundErrorBroadcastReceiver mBackgroundErrorReceiver;
    private SendMessageTask mSendMessageTask;

    // Bottom layout
    private RecyclerView mMessagesView;
    private MessageAdapter mMessagesAdapter;
    private TextView mSendMessageView;
    private EditText mMessageEdit;


    private void setConversationParameters(@Nullable Bundle bundle) {
        if(bundle == null) {
            throw new RuntimeException("No bundle given to PeerConversationActivity");
        }

        mIcon.setText(bundle.getString("icon"));

        // Find group
        String groupName = bundle.getString("groupName");
        mGroup = getGroupFromLocal(groupName);

        // Setup peer info and get peer object if exists
        mPeerId = bundle.getString("peerId");
        mAccountName = bundle.getString("accountame");
        mFullAddressString = DistortPeer.toFullAddress(mPeerId, mAccountName);
        mPeer = getPeerFromLocal(mFullAddressString);

        // Get conversation object if exists
        String conversationLabel = DistortConversation.toUniqueLabel(groupName, mFullAddressString);
        mConversation = getConversationFromLocal(conversationLabel);

        // Colour is stored in preferences
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.icon_colours_preferences_keys), Context.MODE_PRIVATE);
        ((GradientDrawable) mIcon.getBackground()).setColor(
                sharedPref.getInt(DistortConversation.toUniqueLabel(groupName, mFullAddressString, new String[]{"colour"}), 0));

        mFriendlyName = bundle.getString("nickname");
        if(mFriendlyName == null) {
            if(mPeer != null) {
                mFriendlyName = mPeer.getFriendlyName();
            } else if(mConversation != null) {
                mFriendlyName = mConversation.getFriendlyName();
            } else {
                mFriendlyName = mFullAddressString;
            }
        }

        // Set text fields
        mFullAddress.setText(mFullAddressString);
        mName.setText(mFriendlyName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        mLoginParams = DistortAuthParams.getAuthenticationParams(this);
        mDatabaseHelper = new MessageDatabaseHelper(this, mLoginParams.getFullAddress());

        // Setup top fields
        mIcon = findViewById(R.id.messagesIcon);
        mName = findViewById(R.id.messagesName);
        mFullAddress = findViewById(R.id.messagesDetails);

        // Setup list of message-list properties
        mMessagesView = (RecyclerView) findViewById(R.id.messagesView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MessagingActivity.this, LinearLayoutManager.VERTICAL, false);
        mMessagesView.setLayoutManager(linearLayoutManager);

        // Setup other fields
        mSendMessageView = (TextView) findViewById(R.id.sendMessageView);
        final Activity thisActivity = this;
        mSendMessageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Only send non-empty messages
                if(!mMessageEdit.getText().toString().isEmpty() && mSendMessageTask == null) {
                    mSendMessageTask = new SendMessageTask(thisActivity);
                    mSendMessageTask.execute();
                }
            }
        });
        mMessageEdit = (EditText) findViewById(R.id.messageEdit);
    }

    // Getting local values
    private DistortGroup getGroupFromLocal(String groupName) {
        return DistortBackgroundService.getLocalGroups(this).get(groupName);
    }
    private DistortConversation getConversationFromLocal(String conversationLabel) {
        return DistortBackgroundService.getLocalConversations(this).get(conversationLabel);
    }
    private DistortPeer getPeerFromLocal(String peerFullAddress) {
        return DistortBackgroundService.getLocalPeers(this).get(peerFullAddress);
    }
    private ArrayList<DistortMessage> getMessagesFromLocal(String conversationLabel, @Nullable Integer startIndex, @Nullable Integer endIndex) {
        return DistortBackgroundService.getLocalConversationMessages(mDatabaseHelper, conversationLabel, startIndex, endIndex);
    }

    @Override
    protected void onDestroy() {
        mDatabaseHelper.close();
        super.onDestroy();
    }

    // Handle successful retrieval of groups
    @Override
    protected void onStart() {
        mBackgroundErrorReceiver = new BackgroundErrorBroadcastReceiver(findViewById(R.id.messagesConstraintLayout), this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.BACKGROUND_ERROR);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBackgroundErrorReceiver, intentFilter);

        mServiceReceiver = new MessagingActivity.MessageServiceBroadcastReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.ACTION_FETCH_MESSAGES);
        LocalBroadcastManager.getInstance(this).registerReceiver(mServiceReceiver, intentFilter);

        super.onStart();
    }
    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBackgroundErrorReceiver);
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Update intent parameters
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.messaging_preference_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor preferenceEditor = sharedPref.edit();

        Bundle bundle = getIntent().getExtras();
        setConversationParameters(bundle);
        mMessagesAdapter = new MessageAdapter(this, null, mFriendlyName);
        mMessagesView.setAdapter(mMessagesAdapter);

        // Use group name + peer full-address to uniquely identify a conversation,
        // even before a message has been sent or received
        preferenceEditor.putString("activeConversation", DistortConversation.toUniqueLabel(mGroup.getName(), mFullAddressString));
        preferenceEditor.apply();

        if(mConversation != null) {
            final ArrayList<DistortMessage> allMessages = getMessagesFromLocal(mConversation.getUniqueLabel(), null, null);
            int height = allMessages.size();
            final List<DistortMessage> messages = allMessages.subList(Math.max(height - 20, 0), height);
            for (int i = 0; i < messages.size(); i++) {
                mMessagesAdapter.addOrUpdateMessage(messages.get(i));
            }
            int position = mMessagesAdapter.getItemCount() - 1;
            if(position > 0) {
                mMessagesView.scrollToPosition(position);
            }

            DistortBackgroundService.startActionFetchMessages(this, mConversation.getUniqueLabel());
        }

        super.onResume();
    }
    @Override
    protected void onPause() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.messaging_preference_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor preferenceEditor = sharedPref.edit();
        preferenceEditor.putString("activeConversation", null);
        preferenceEditor.apply();

        super.onPause();
    }
    public class MessageServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String conversationLabel = intent.getStringExtra("conversationLabel");

            // Parse string for indexes of updated messages
            String updatedMessagesString = intent.getStringExtra("updatedMessages");
            final String[] tmpArray;
            final Integer[] updatedMessages;
            if(updatedMessagesString != null && !updatedMessagesString.isEmpty()) {
                tmpArray = updatedMessagesString.split(":");
            } else {
                tmpArray = new String[]{};
            }
            updatedMessages = new Integer[tmpArray.length];
            for(int i = 0; i < tmpArray.length; i++) {
                updatedMessages[i] = Integer.valueOf(tmpArray[i]);
            }

            if(conversationLabel != null && !conversationLabel.isEmpty()) {
                mConversation = getConversationFromLocal(conversationLabel);
                final ArrayList<DistortMessage> allMessages = getMessagesFromLocal(mConversation.getUniqueLabel(), null, null);

                // Can only update UI from UI thread
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for(int i : updatedMessages) {
                            mMessagesAdapter.addOrUpdateMessage(allMessages.get(i));
                        }
                        int position = mMessagesAdapter.getItemCount() - 1;
                        if (position > 0) {
                            mMessagesView.smoothScrollToPosition(position);
                        }
                    }
                });
            }
        }
    }

    /**
     * Represents an asynchronous messaging task used to send a single message
     */
    public class SendMessageTask extends AsyncTask<Void, Void, Boolean> {

        private Activity mActivity;
        private String mErrorString;

        SendMessageTask(Activity activity) {
            mActivity = activity;
            mErrorString = "";
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorString = "";

            // Attempt authentication against a network service.
            try {
                JsonReader response = null;
                String url = mLoginParams.getHomeserverAddress() + "groups/" + Uri.encode(mGroup.getName());

                HashMap<String, String> bodyParams = new HashMap<>();
                bodyParams.put("toPeerId", mPeerId);
                if(mAccountName != null && !mAccountName.isEmpty()) {
                    bodyParams.put("toAccountName", mAccountName);
                }
                bodyParams.put("message", mMessageEdit.getText().toString());

                URL homeserverEndpoint = new URL(url);
                if(DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.putBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.putBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                }
                if(response == null) {
                    return false;
                }

                // Read all messages in messages and out messages
                final OutMessage newMessage = OutMessage.readJson(response);
                response.close();

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Remove message from textfield
                        mMessageEdit.getText().clear();
                        mMessagesAdapter.addOrUpdateMessage(newMessage);
                        mMessagesView.smoothScrollToPosition(mMessagesAdapter.getItemCount()-1);
                    }
                });

                return true;
            } catch (DistortJson.DistortException e) {
                if (e.getResponseCode() == 400) {           // Fields were filled improperly
                    mErrorString = e.getMessage();
                } else if (e.getResponseCode() == 404) {    // Account does not belong to group, or missing peer's certificate
                    mErrorString = e.getMessage();
                } else if (e.getResponseCode() == 500) {
                    Log.d("SEND-MESSAGE", e.getMessage());
                    mErrorString = getString(R.string.error_server_error);
                } else {
                    mErrorString = e.getMessage();
                }
                return false;
            } catch (IOException e) {
                mErrorString = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSendMessageTask = null;
            if (success) {
                // We just invalidated conversations cache, update
                DistortBackgroundService.startActionFetchConversations(getApplicationContext(), mGroup.getName());

                String newMessageConversationLabel = DistortConversation.toUniqueLabel(mGroup.getName(), mFullAddressString);
                DistortBackgroundService.startActionFetchMessages(getApplicationContext(), newMessageConversationLabel);
            } else {
                mMessageEdit.setError(mErrorString);
                Log.e("SEND-MESSAGE", mErrorString);
            }
        }
    }
}
