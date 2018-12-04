package com.unix4all.rypi.distort;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PeerConversationActivity extends AppCompatActivity implements NewConversationFragment.NewConversationListener {
    private final PeerConversationActivity mActivity = this;

    private DistortAuthParams mLoginParams;
    private DistortGroup mGroup;

    private RecyclerView mPeerConversationsView;
    private ConversationAdapter mConversationAdapter;

    private PeerServiceBroadcastReceiver mPeerServiceReceiver;
    private ConversationServiceBroadcastReceiver mConversationServiceReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_conversations);

        // Use shared preferences to fetch authorization params
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.credentials_preferences_key), Context.MODE_PRIVATE);
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
            }
            conversations.add(c);
        }
        mConversationAdapter = new ConversationAdapter(PeerConversationActivity.this, conversations);
        mPeerConversationsView.setAdapter(mConversationAdapter);

        // Add peers which publicly belong to this group
        for(Map.Entry<String, DistortPeer> peer : peerSet.entrySet()) {
            DistortPeer p = peer.getValue();
            if(p.getGroups().get(mGroup.getName()) != null) {
                DistortConversation c = new DistortConversation(null, mGroup.getId(), p.getPeerId(), p.getAccountName(), 0, new Date(0));
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

    @Override
    public void onFinishConvoFieldInputs(String friendlyName, String peerId) {
        // TODO: Attempt to add group to homeserver (PUT /groups)
    }

    // Handle successful retrieval of peers
    @Override
    protected void onStart() {
        mPeerServiceReceiver = new PeerServiceBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.ACTION_FETCH_PEERS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mPeerServiceReceiver, intentFilter);

        // Start fetch peers task
        // DistortBackgroundService.startActionFetchPeers(getApplicationContext());

        mConversationServiceReceiver = new ConversationServiceBroadcastReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.ACTION_FETCH_CONVERSATIONS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mConversationServiceReceiver, intentFilter);

        // Start fetch peers task
        // DistortBackgroundService.startActionFetchPeers(getApplicationContext());

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

                        // Set nickname to conversation if exists
                        String fullAddress = DistortPeer.toFullAddress(c.getPeerId(), c.getAccountName());
                        if(peers.get(fullAddress) != null) {
                            c.setFriendlyName(peers.get(fullAddress).getNickname());
                        }
                        mConversationAdapter.addOrUpdateConversation(c);
                    }
                }
            });
        }
    }
}