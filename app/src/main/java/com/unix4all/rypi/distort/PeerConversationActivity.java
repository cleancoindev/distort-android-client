package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class PeerConversationActivity extends AppCompatActivity implements NewConversationFragment.NewConversationListener {
    private final PeerConversationActivity mActivity = this;

    private DistortAuthParams mLoginParams;
    private DistortGroup mGroup;

    private RecyclerView mPeerConversationsView;
    private PeerAdapter mPeerAdapter;

    private PeerServiceBroadcastReceiver mServiceReceiver;

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
        mPeerAdapter = new PeerAdapter(PeerConversationActivity.this, new ArrayList<DistortPeer>(), mGroup.getId());
        mPeerConversationsView.setAdapter(mPeerAdapter);

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
        mServiceReceiver = new PeerServiceBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.ACTION_FETCH_PEERS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mServiceReceiver, intentFilter);

        // Start fetch peers task
        DistortBackgroundService.startActionFetchPeers(getApplicationContext());

        super.onStart();
    }
    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceReceiver);
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
                    HashMap<String, DistortPeer> allPeers = getPeersFromLocal();

                    for(Map.Entry<String, DistortPeer> peer : allPeers.entrySet()) {
                        mPeerAdapter.addOrUpdatePeer(peer.getValue());
                    }
                }
            });
        }
    }
}