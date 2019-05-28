package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class PeerConversationActivity extends AppCompatActivity implements NewConversationFragment.NewConversationListener, TimedRemoveFragment.OnFragmentFinishedListener {
    private final PeerConversationActivity mActivity = this;

    private DistortAuthParams mLoginParams;
    private DistortGroup mGroup;

    private RecyclerView mPeerConversationsView;
    private ConversationAdapter mConversationAdapter;

    private PeerServiceBroadcastReceiver mPeerServiceReceiver;
    private ConversationServiceBroadcastReceiver mConversationServiceReceiver;

    private AddPeerTask mAddPeerTask;
    private RemovePeerTask mRemovePeerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_conversations);

        // Use shared preferences to fetch authorization params
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.account_credentials_preferences_key), Context.MODE_PRIVATE);
        mLoginParams = new DistortAuthParams();
        mLoginParams.setHomeserverAddress(sharedPref.getString(DistortAuthParams.EXTRA_HOMESERVER, null));
        mLoginParams.setHomeserverProtocol(sharedPref.getString(DistortAuthParams.EXTRA_HOMESERVER_PROTOCOL, null));
        mLoginParams.setPeerId(sharedPref.getString(DistortAuthParams.EXTRA_PEER_ID, null));
        mLoginParams.setAccountName(sharedPref.getString(DistortAuthParams.EXTRA_ACCOUNT_NAME, null));
        mLoginParams.setCredential(sharedPref.getString(DistortAuthParams.EXTRA_CREDENTIAL, null));

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            String groupDatabaseId = bundle.getString("groupDatabaseId");
            mGroup = getGroupFromLocal(groupDatabaseId);
        } else {
            throw new RuntimeException("No bundle given to PeerConversationActivity");
        }

        // Init toolbar
        Toolbar toolbar = findViewById(R.id.peerConversationsToolbar);
        toolbar.setTitle(R.string.title_activity_conversations);
        setSupportActionBar(toolbar);

        // Setup list of groups-list properties
        mPeerConversationsView = (RecyclerView) findViewById(R.id.peerConversationsView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(PeerConversationActivity.this, LinearLayoutManager.VERTICAL, false);
        mPeerConversationsView.setLayoutManager(linearLayoutManager);
        mPeerConversationsView.addItemDecoration(new DividerItemDecoration(PeerConversationActivity.this, DividerItemDecoration.VERTICAL));

        // Prepare for datasets
        HashMap<String, DistortConversation> conversationSet = getConversationsFromLocal();
        HashMap<String, DistortPeer> peerSet = getPeersFromLocal();
        ArrayList<DistortConversation> conversations = new ArrayList<>();

        // Add all conversations and optionally find associated peer
        for(Map.Entry<String, DistortConversation> conversation : conversationSet.entrySet()) {
            DistortConversation c = conversation.getValue();
            String fullAddress = DistortPeer.toFullAddress(c.getPeerId(), c.getAccountName());
            if(peerSet.get(fullAddress) != null) {
                c.setFriendlyName(peerSet.get(fullAddress).getNickname());
                conversations.add(c);
            }
        }
        mConversationAdapter = new ConversationAdapter(PeerConversationActivity.this, conversations);
        mPeerConversationsView.setAdapter(mConversationAdapter);

        // Add peers which publicly belong to this group, but we don't have a conversation with
        for(Map.Entry<String, DistortPeer> peer : peerSet.entrySet()) {
            DistortPeer p = peer.getValue();
            if(p.getGroups().get(mGroup.getName()) != null && conversationSet.get(p.getFullAddress()) == null) {
                DistortConversation c = new DistortConversation(null, mGroup.getId(), p.getPeerId(), p.getAccountName(), 0, new Date(0));
                c.setFriendlyName(p.getNickname());
                mConversationAdapter.addOrUpdateConversation(c);
            }
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddNewPeer();
            }
        });
    }

    // Getting local values
    private DistortGroup getGroupFromLocal(String groupDatabaseId) {
        return DistortBackgroundService.getLocalGroups(this).get(groupDatabaseId);
    }
    private HashMap<String, DistortPeer> getPeersFromLocal() {
        return DistortBackgroundService.getLocalPeers(this);
    }
    private HashMap<String, DistortConversation> getConversationsFromLocal() {
        return DistortBackgroundService.getLocalConversations(this);
    }

    private void showAddNewPeer() {
        FragmentManager fm = getSupportFragmentManager();
        NewConversationFragment newConvoFragment = NewConversationFragment.newInstance();
        newConvoFragment.show(fm, "fragment_newConversationLayout");
    }

    public void showRemovePeer(Integer position) {
        FragmentManager fm = getSupportFragmentManager();

        String title = getResources().getString(R.string.title_remove_peer);
        String description = getResources().getString(R.string.description_remove_peer);

        TimedRemoveFragment timedRemoveGroupFragment = TimedRemoveFragment.newInstance(this, title, description, position);
        timedRemoveGroupFragment.show(fm, "fragment_removePeerLayout");
    }

    @Override
    public void onFinishConvoFieldInputs(String friendlyName, String peerId, String accountName) {
        mAddPeerTask = new AddPeerTask(this, friendlyName, peerId, accountName);
        mAddPeerTask.execute();
    }

    @Override
    public void onFragmentFinished(Boolean removeChoice, @Nullable Integer position) {
        if(removeChoice && position != null) {
            DistortConversation c = mConversationAdapter.getItem(position);
            mRemovePeerTask = new RemovePeerTask(this, c.getPeerId(), c.getAccountName(), position);
            mRemovePeerTask.execute();
        }
    }

    // Handle successful retrieval of peers
    @Override
    protected void onStart() {
        mPeerServiceReceiver = new PeerServiceBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.ACTION_FETCH_PEERS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mPeerServiceReceiver, intentFilter);

        // Start fetch peers task
        DistortBackgroundService.startActionFetchPeers(getApplicationContext());

        mConversationServiceReceiver = new ConversationServiceBroadcastReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.ACTION_FETCH_CONVERSATIONS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mConversationServiceReceiver, intentFilter);

        DistortBackgroundService.startActionFetchConversations(getApplicationContext(), mGroup.getId());

        super.onStart();
    }
    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPeerServiceReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mConversationServiceReceiver);
        super.onStop();
    }
    public class PeerServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("DISTORT-RECEIVE", "Received Fetch Peers response.");

            final HashMap<String, DistortPeer> allPeers = getPeersFromLocal();

            // Can only update UI from UI thread
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for(Map.Entry<String, DistortPeer> peer : allPeers.entrySet()) {
                        DistortPeer p = peer.getValue();

                        // Ensure peer belongs to group before allowing to message in group
                        if(p.getGroups().get(mGroup.getName()) != null) {
                            DistortConversation c = new DistortConversation(null, mGroup.getId(), p.getPeerId(), p.getAccountName(), 0, new Date(0));
                            c.setFriendlyName(p.getNickname());
                            mConversationAdapter.addOrUpdateConversation(c);
                        }
                    }
                }
            });
        }
    }

    public class ConversationServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("DISTORT-RECEIVE", "Received Fetch Conversations response.");

            final HashMap<String, DistortConversation> conversations = getConversationsFromLocal();
            final HashMap<String, DistortPeer> peers = getPeersFromLocal();

            // Can only update UI from UI thread
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for(Map.Entry<String, DistortConversation> conversation : conversations.entrySet()) {
                        DistortConversation c = conversation.getValue();

                        // Set nickname to conversation if exists. Only show conversations with added peers
                        String fullAddress = DistortPeer.toFullAddress(c.getPeerId(), c.getAccountName());
                        DistortPeer p = peers.get(fullAddress);
                        if(p != null && p.getGroups().get(mGroup.getName()) != null) {
                            c.setFriendlyName(p.getNickname());
                            mConversationAdapter.addOrUpdateConversation(c);
                        }
                    }
                }
            });
        }
    }


    /**
     * Represents an asynchronous task used to add a peer to account
     */
    public class AddPeerTask extends AsyncTask<Void, Void, Boolean> {

        private Activity mActivity;
        private String mFriendlyName;
        private String mPeerId;
        private String mAccountName;
        private String mErrorString;

        AddPeerTask(Activity activity, String friendlyName, String peerId, String accountName) {
            mActivity = activity;
            mFriendlyName = friendlyName;
            mPeerId = peerId;
            mAccountName = accountName;
            mErrorString = "";
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorString = "";

            // Attempt authentication against a network service.
            try {
                JsonReader response = null;
                String url = mLoginParams.getHomeserverAddress() + "peers";

                HashMap<String, String> bodyParams = new HashMap<>();
                bodyParams.put("nickname", mFriendlyName);
                bodyParams.put("peerId", mPeerId);
                bodyParams.put("accountName", mAccountName);

                URL homeserverEndpoint = new URL(url);
                if (DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.PostBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.PostBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                }

                // Read all messages in messages and out messages
                final DistortPeer newPeer = DistortPeer.readJson(response);
                response.close();

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (newPeer.getGroups().get(mGroup.getName()) != null) {
                            DistortConversation c = new DistortConversation(null, mGroup.getId(), mPeerId, mAccountName, 0, new Date(0));
                            c.setFriendlyName(newPeer.getFriendlyName());
                            mConversationAdapter.addOrUpdateConversation(c);
                        }
                    }
                });

                return true;
            } catch (DistortJson.DistortException e) {
                if (e.getResponseCode() == 404) {
                    Log.d("ADD-PEER", e.getMessage());
                    mErrorString = getString(R.string.error_missing_cert);
                } else if (e.getResponseCode() == 500) {
                    Log.d("ADD-PEER", e.getMessage());
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
            mAddPeerTask = null;
            if (success) {
                // Invalidated local peers cache, refetch
                Context appContext = getApplicationContext();
                DistortBackgroundService.startActionFetchPeers(appContext);
                DistortBackgroundService.startActionFetchConversations(appContext, mGroup.getName());
            } else {
                Snackbar.make(findViewById(R.id.peerConversationsConstraintLayout), mErrorString,
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        }
    }

    /**
     * Represents an asynchronous task used to remove a peer
     */
    public class RemovePeerTask extends AsyncTask<Void, Void, Boolean> {

        private Activity mActivity;
        private String mPeerId;
        private @Nullable String mAccountName;
        private Integer mPosition;
        private String mErrorString;

        RemovePeerTask(Activity activity, String peerId, @Nullable String accountName, Integer position) {
            mActivity = activity;
            mPeerId = peerId;
            mAccountName = accountName;
            mPosition = position;
            mErrorString = "";
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorString = "";

            // Attempt authentication against a network service.
            try {
                JsonReader response = null;
                String url = mLoginParams.getHomeserverAddress() + "peers";

                HashMap<String, String> bodyParams = new HashMap<>();
                bodyParams.put("peerId", mPeerId);
                if(mAccountName != null && !mAccountName.isEmpty()) {
                    bodyParams.put("accountName", mAccountName);
                }

                URL homeserverEndpoint = new URL(url);
                if(DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.DeleteBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.DeleteBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                }
                response.close();

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mConversationAdapter.removeItem(mPosition);
                    }
                });

                return true;
            } catch (DistortJson.DistortException e) {
                if (e.getResponseCode() == 400) {
                    // The peer's IPFS ID was not specified
                    mErrorString = e.getMessage();
                } else if (e.getResponseCode() == 404) {
                    // The specified peer does not exist (locally)
                    mErrorString = e.getMessage();
                } else if (e.getResponseCode() == 500) {
                    Log.d("REMOVE-PEER", e.getMessage());
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
            mRemovePeerTask = null;
            if (success) {
                // Invalidated local peers cache, refetch
                DistortBackgroundService.startActionFetchPeers(getApplicationContext());
            } else {
                Snackbar.make(findViewById(R.id.peerConversationsConstraintLayout), mErrorString,
                    Snackbar.LENGTH_LONG)
                    .show();
            }
        }
    }
}