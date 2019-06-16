package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

public class PeerConversationActivity extends AppCompatActivity
        implements NewConversationFragment.NewConversationListener, TimedRemoveFragment.OnFragmentFinishedListener, RenamePeerFragment.OnRenamePeerFragmentListener {
    private final PeerConversationActivity mActivity = this;

    private DistortAuthParams mLoginParams;
    private DistortGroup mGroup;

    private RecyclerView mPeerConversationsView;
    private ConversationAdapter mConversationAdapter;

    private PeerServiceBroadcastReceiver mPeerServiceReceiver;
    private ConversationServiceBroadcastReceiver mConversationServiceReceiver;
    private BackgroundErrorBroadcastReceiver mBackgroundErrorReceiver;

    private AddPeerTask mAddPeerTask;
    private RemovePeerTask mRemovePeerTask;

    private void populateNewData() {
        // Prepare for datasets
        HashMap<String, DistortPeer> peers = getPeersFromLocal();

        Set<String> keepSet = new HashSet<>();

        // Add peers which publicly belong to this group but do
        for(Map.Entry<String, DistortPeer> peer : peers.entrySet()) {
            DistortPeer p = peer.getValue();

            // Ensure peer belongs to group before allowing to message in group
            if(p.getGroups().containsKey(mGroup.getName()) && !keepSet.contains(p.getFullAddress())) {
                mConversationAdapter.addOrUpdateConversationPeer(p);
                keepSet.add(p.getFullAddress());
            }
        }

        // Remove unneeded conversations from list
        for(int i = mConversationAdapter.getItemCount()-1; i >= 0; i--) {
            DistortConversation c = mConversationAdapter.getItem(i);
            if(!keepSet.contains(c.getFullAddress())) {
                mConversationAdapter.removeItem(i);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_conversations);

        mLoginParams = DistortAuthParams.getAuthenticationParams(this);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            mGroup = getGroupFromLocal(bundle.getString("groupName"));
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

        // Setup peer/conversation list
        mConversationAdapter = new ConversationAdapter(PeerConversationActivity.this, new ArrayList<DistortConversation>(), mGroup.getName());
        mPeerConversationsView.setAdapter(mConversationAdapter);
        populateNewData();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddNewPeer();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            String groupName = bundle.getString("groupName");
            mGroup = getGroupFromLocal(groupName);
        } else {
            throw new RuntimeException("No bundle given to PeerConversationActivity");
        }
    }

    // Getting local values
    private DistortGroup getGroupFromLocal(String groupName) {
        return DistortBackgroundService.getLocalGroups(this).get(groupName);
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

    public void showRenamePeer(int position) {
        DistortConversation dc = mConversationAdapter.getItem(position);

        FragmentManager fm = getSupportFragmentManager();
        RenamePeerFragment f = RenamePeerFragment.newInstance(dc.getPeerId(), dc.getAccountName(), dc.getNickname());
        f.show(fm, "fragment_renamePeerLayout");
    }

    @Override
    public void onFinishConvoFieldInputs(String friendlyName, String peerId, String accountName) {
        if(mAddPeerTask != null) {
            Snackbar.make(findViewById(R.id.peerConversationsConstraintLayout),
                    getString(R.string.error_simultaneous_operations), Snackbar.LENGTH_LONG).show();
            return;
        }

        mAddPeerTask = new AddPeerTask(this, friendlyName, peerId, accountName);
        mAddPeerTask.execute();
    }

    @Override
    public void onTimedRemoveFinished(Boolean removeChoice, @Nullable Integer position) {
        if(removeChoice && position != null) {
            DistortConversation c = mConversationAdapter.getItem(position);
            mRemovePeerTask = new RemovePeerTask(this, c.getPeerId(), c.getAccountName(), position);
            mRemovePeerTask.execute();
        }
    }

    @Override
    public void OnRenamePeer(String nickname, String peerId, String accountName) {
        if(mAddPeerTask != null) {
            Snackbar.make(findViewById(R.id.peerConversationsConstraintLayout),
                getString(R.string.error_simultaneous_operations), Snackbar.LENGTH_LONG).show();
            return;
        }

        mAddPeerTask = new AddPeerTask(this, nickname, peerId, accountName);
        mAddPeerTask.execute();
    }

    // Handle successful retrieval of peers
    @Override
    protected void onStart() {
        // Handle errors
        mBackgroundErrorReceiver = new BackgroundErrorBroadcastReceiver(findViewById(R.id.peerConversationsConstraintLayout), this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.BACKGROUND_ERROR);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBackgroundErrorReceiver, intentFilter);

        // Fetch peers
        mPeerServiceReceiver = new PeerServiceBroadcastReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.ACTION_FETCH_PEERS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mPeerServiceReceiver, intentFilter);

        // Fetch conversations
        //mConversationServiceReceiver = new ConversationServiceBroadcastReceiver();
        //intentFilter = new IntentFilter();
        //intentFilter.addAction(DistortBackgroundService.ACTION_FETCH_CONVERSATIONS);
        //LocalBroadcastManager.getInstance(this).registerReceiver(mConversationServiceReceiver, intentFilter);

        // Fetch peers and conversations
        DistortBackgroundService.startActionFetchPeers(getApplicationContext());
        DistortBackgroundService.startActionFetchConversations(getApplicationContext(), mGroup.getName());

        super.onStart();
    }
    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPeerServiceReceiver);
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mConversationServiceReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBackgroundErrorReceiver);
        super.onStop();
    }

    public class PeerServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("DISTORT-RECEIVE", "Received Fetch Peers response.");

            // Can only update UI from UI thread
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    populateNewData();
                }
            });
        }
    }

    public class ConversationServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("DISTORT-RECEIVE", "Received Fetch Conversations response.");

            // Can only update UI from UI thread
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    populateNewData();
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
                    response = DistortJson.postBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.postBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                }

                // Read all messages in messages and out messages
                final DistortPeer newPeer = DistortPeer.readJson(response);
                response.close();

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (newPeer.getGroups().get(mGroup.getName()) != null) {
                            mConversationAdapter.addOrUpdateConversationPeer(newPeer);
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
                    response = DistortJson.deleteBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.deleteBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
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