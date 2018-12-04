package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_ACCOUNT_NAME;
import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_CREDENTIAL;
import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_HOMESERVER;
import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_HOMESERVER_PROTOCOL;
import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_PEER_ID;

public class MessagingActivity extends AppCompatActivity {
    private final MessagingActivity mActivity = this;

    private DistortAuthParams mLoginParams;

    // Header fields
    private TextView mIcon;
    private TextView mName;
    private TextView mFullAddress;

    // Peer we are messaging
    private String mPeerId;
    private String mAccountName;
    private String mFriendlyName;
    private @Nullable DistortPeer mPeer;
    private @Nullable DistortConversation mConversation;

    // Group we are messaging on
    private DistortGroup mGroup;
    private MessageServiceBroadcastReceiver mServiceReceiver;
    private SendMessageTask mSendMessageTask;

    // Bottom layout
    private RecyclerView mMessagesView;
    private MessageAdapter mMessagesAdapter;
    private TextView mSendMessageView;
    private EditText mMessageEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);
        Intent thisIntent = getIntent();

        // Use shared preferences to fetch authorization params
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.credentials_preferences_key), Context.MODE_PRIVATE);
        mLoginParams = new DistortAuthParams();
        mLoginParams.setHomeserverAddress(sharedPref.getString(DistortAuthParams.EXTRA_HOMESERVER, null));
        mLoginParams.setHomeserverProtocol(sharedPref.getString(DistortAuthParams.EXTRA_HOMESERVER_PROTOCOL, null));
        mLoginParams.setPeerId(sharedPref.getString(DistortAuthParams.EXTRA_PEER_ID, null));
        mLoginParams.setAccountName(sharedPref.getString(DistortAuthParams.EXTRA_ACCOUNT_NAME, null));
        mLoginParams.setCredential(sharedPref.getString(DistortAuthParams.EXTRA_CREDENTIAL, null));

        // Setup top fields
        mIcon = findViewById(R.id.messagesIcon);
        mName = findViewById(R.id.messagesName);
        mFullAddress = findViewById(R.id.messagesDetails);
        ArrayList<DistortMessage> conversationMessages = new ArrayList<>();
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            mIcon.setText(bundle.getString("icon"));
            ((GradientDrawable) mIcon.getBackground()).setColor(bundle.getInt("colorIcon"));

            // Setup peer info
            mPeerId = bundle.getString("peerId");
            mAccountName = bundle.getString("accountame");
            String fullAddress = DistortPeer.toFullAddress(mPeerId, mAccountName);
            mFullAddress.setText(fullAddress);
            mPeer = getPeerFromLocal(fullAddress);

            mFriendlyName = bundle.getString("friendlyName");
            if(mFriendlyName == null) {
                mFriendlyName = mPeer.getFriendlyName();
                Log.d("ACTIVITY-MESSAGES", "Set nickname: " + mPeer.getNickname());
            }
            

            String groupDatabaseId = bundle.getString("groupDatabaseId");
            mGroup = getGroupFromLocal(groupDatabaseId);

            String conversationId = bundle.getString("conversationDatabaseId");
            if(conversationId != null && !conversationId.isEmpty()) {
                mConversation = getConversationFromLocal(conversationId);
                conversationMessages = getMessagesFromLocal(conversationId);

            }
            mName.setText(mFriendlyName);
        } else {
            throw new RuntimeException("No bundle given to PeerConversationActivity");
        }

        // Setup list of message-list properties
        mMessagesView = (RecyclerView) findViewById(R.id.messagesView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MessagingActivity.this, LinearLayoutManager.VERTICAL, false);
        mMessagesView.setLayoutManager(linearLayoutManager);
        mMessagesAdapter = new MessageAdapter(this, conversationMessages, mFriendlyName);
        mMessagesView.setAdapter(mMessagesAdapter);
        int position = mMessagesAdapter.getItemCount() - 1;
        if(position > 0) {
            mMessagesView.smoothScrollToPosition(position);
        }

        // Setup other fields
        mSendMessageView = (TextView) findViewById(R.id.sendMessageView);
        final Activity thisActivity = this;
        mSendMessageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Only send non-empty messages
                if(!mMessageEdit.getText().toString().isEmpty()) {
                    mSendMessageTask = new SendMessageTask(thisActivity);
                    mSendMessageTask.execute();
                }
            }
        });
        mMessageEdit = (EditText) findViewById(R.id.messageEdit);
    }

    // Getting local values
    private DistortGroup getGroupFromLocal(String groupDatabaseId) {
        return DistortBackgroundService.getLocalGroups(this).get(groupDatabaseId);
    }
    private DistortConversation getConversationFromLocal(String conversationDatabaseId) {
        return DistortBackgroundService.getLocalConversations(this).get(conversationDatabaseId);
    }
    private DistortPeer getPeerFromLocal(String peerFullAddress) {
        return DistortBackgroundService.getLocalPeers(this).get(peerFullAddress);
    }
    private ArrayList<DistortMessage> getMessagesFromLocal(String conversationDatabaseId) {
        return DistortBackgroundService.getLocalConversationMessages(this, conversationDatabaseId);
    }

    // Handle successful retrieval of groups
    @Override
    protected void onStart() {
        mServiceReceiver = new MessagingActivity.MessageServiceBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.ACTION_FETCH_MESSAGES);
        LocalBroadcastManager.getInstance(this).registerReceiver(mServiceReceiver, intentFilter);

        // DistortBackgroundService.startActionFetchMessages(getApplicationContext());
        super.onStart();
    }
    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceReceiver);
        super.onStop();
    }
    @Override
    protected void onResume() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.messaging_preference_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor preferenceEditor = sharedPref.edit();
        preferenceEditor.putBoolean("active", true);
        preferenceEditor.commit();

        super.onResume();
    }
    @Override
    protected void onPause() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.messaging_preference_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor preferenceEditor = sharedPref.edit();
        preferenceEditor.putBoolean("active", false);
        preferenceEditor.commit();

        super.onPause();
    }
    public class MessageServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String conversationDatabaseId = intent.getStringExtra("conversationDatabaseId");
            if(conversationDatabaseId != null && !conversationDatabaseId.isEmpty()) {
                mConversation = getConversationFromLocal(conversationDatabaseId);
                final ArrayList<DistortMessage> messages = getMessagesFromLocal(conversationDatabaseId);

                // Can only update UI from UI thread
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < messages.size(); i++) {
                            mMessagesAdapter.addOrUpdateMessage(messages.get(i));
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
     * Represents an asynchronous login/registration task used to retrieve all the user's groups
     */
    public class SendMessageTask extends AsyncTask<Void, Void, Boolean> {

        private Activity mActivity;
        private int mErrorCode;

        SendMessageTask(Activity activity) {
            mActivity = activity;
            mErrorCode = 0;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorCode = 0;

            // Attempt authentication against a network service.
            try {
                JsonReader response = null;
                String url = mLoginParams.getHomeserverAddress() + "groups/" + URLEncoder.encode(mGroup.getName());

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
                    response = DistortJson.PutBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.PutBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                }
                if(response == null) {
                    return false;
                }

                // Read all messages in messages and out messages
                final OutMessage newMessage = OutMessage.readMessageJson(response);
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
                mErrorCode = -1;
                e.printStackTrace();
                Log.e("SEND-MESSAGE", e.getMessage() + " : " + String.valueOf(e.getResponseCode()));

                return false;
            } catch (IOException e) {
                mErrorCode = -2;
                e.printStackTrace();

                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSendMessageTask = null;
            Log.d("SEND-MESSAGE", String.valueOf(mErrorCode));

            // TODO: Handle errors here
            if (!success) {

            }
        }
    }
}
