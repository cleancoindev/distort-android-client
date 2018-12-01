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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class GroupsActivity extends AppCompatActivity implements NewGroupFragment.NewGroupListener {
    private final GroupsActivity mActivity = this;

    private DistortAuthParams mLoginParams;

    private RecyclerView mGroupsView;
    private GroupAdapter mGroupsAdapter;

    private FetchAccountTask mAccountTask;

    private GroupServiceBroadcastReceiver mServiceReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        // Use shared preferences to fetch authorization params
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.credentials_preferences_key), Context.MODE_PRIVATE);
        mLoginParams = new DistortAuthParams();
        mLoginParams.setHomeserverAddress(sharedPref.getString(DistortAuthParams.EXTRA_HOMESERVER, null));
        mLoginParams.setHomeserverProtocol(sharedPref.getString(DistortAuthParams.EXTRA_HOMESERVER_PROTOCOL, null));
        mLoginParams.setPeerId(sharedPref.getString(DistortAuthParams.EXTRA_PEER_ID, null));
        mLoginParams.setAccountName(sharedPref.getString(DistortAuthParams.EXTRA_ACCOUNT_NAME, null));
        mLoginParams.setCredential(sharedPref.getString(DistortAuthParams.EXTRA_CREDENTIAL, null));

        // Init toolbar
        Toolbar toolbar = findViewById(R.id.groupToolbar);
        toolbar.setTitle(R.string.title_activity_groups);
        setSupportActionBar(toolbar);

        // Setup list of groups-list properties
        mGroupsView = (RecyclerView) findViewById(R.id.groupsView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(GroupsActivity.this, LinearLayoutManager.VERTICAL, false);
        mGroupsView.setLayoutManager(linearLayoutManager);
        mGroupsView.addItemDecoration(new DividerItemDecoration(GroupsActivity.this, DividerItemDecoration.VERTICAL));

        // Prepare for datasets
        mGroupsAdapter = new GroupAdapter(GroupsActivity.this, new ArrayList<DistortGroup>(), mLoginParams);
        mGroupsView.setAdapter(mGroupsAdapter);

        // Fetch account parameters
        mAccountTask = new FetchAccountTask(this);
        mAccountTask.execute();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showJoinNewGroup();
            }
        });
    }

    private void showJoinNewGroup() {
        FragmentManager fm = getSupportFragmentManager();
        NewGroupFragment newGroupFragment = NewGroupFragment.newInstance();
        newGroupFragment.show(fm, "fragment_newGroupLayout");
    }

    @Override
    public void onFinishGroupFieldInputs(String groupName, Integer subgroupIndex) {
        // TODO: Attempt to add group to homeserver (PUT /groups)
    }

    // Getting local values
    private HashMap<String, DistortGroup> getGroupsFromLocal() {
        return DistortBackgroundService.getLocalGroups(this);
    }

    // Handle successful retrieval of groups
    @Override
    protected void onStart() {
        mServiceReceiver = new GroupsActivity.GroupServiceBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.ACTION_FETCH_GROUPS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mServiceReceiver, intentFilter);

        DistortBackgroundService.startActionFetchGroups(getApplicationContext());
        super.onStart();
    }
    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceReceiver);
        super.onStop();
    }
    public class GroupServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Can only update UI from UI thread
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    HashMap<String, DistortGroup> allGroups = getGroupsFromLocal();

                    for(Map.Entry<String, DistortGroup> group : allGroups.entrySet()) {
                        mGroupsAdapter.addOrUpdateGroup(group.getValue());
                    }
                }
            });
        }
    }

    /**
     * Represents an asynchronous task used to retrieve a user account
     */
    public class FetchAccountTask extends AsyncTask<Void, Void, Boolean> {

        private int mErrorCode;
        private Activity mActivity;

        FetchAccountTask(Activity activity) {
            mActivity = activity;
            mErrorCode = 0;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorCode = 0;

            // Attempt authentication against a network service.
            try {
                JsonReader response = null;
                URL homeserverEndpoint = new URL(mLoginParams.getHomeserverAddress() + "account");
                if(DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.GetJSONFromURL(myConnection, mLoginParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.GetJSONFromURL(myConnection, mLoginParams);
                }
                if(response == null) {
                    return false;
                }

                String accountName = null;
                DistortGroup activeGroup = null;
                Boolean enabled = null;
                String peerId = null;

                // Read all fields from group
                response.beginObject();
                while(response.hasNext()) {
                    String key = response.nextName();
                    Log.d("GET-ACCOUNT-KEY", key);

                    if(key.equals("accountName")) {
                        accountName = response.nextString();
                    } else if(key.equals("activeGroup")) {
                        activeGroup = DistortGroup.readGroupJson(response);
                        activeGroup.setIsActive(true);
                    } else if(key.equals("enabled")) {
                        enabled = response.nextBoolean();
                    } else if(key.equals("peerId")) {
                        peerId = response.nextString();
                    } else {
                        response.skipValue();
                    }
                }
                if(accountName != null && enabled != null && peerId != null) {
                    String activeGroupStr = "";
                    if(activeGroup != null) {
                        activeGroupStr += "," + activeGroup.getId();


                        // Can only update UI from UI thread, and requires final parameter passing
                        final DistortGroup g = activeGroup;
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mGroupsAdapter.addOrUpdateGroup(g);
                            }
                        });
                    }

                    Log.d("GET-ACCOUNT", "Account ( " + accountName + "," + enabled + "," + peerId + activeGroupStr + " )");
                } else {
                    // TODO: Failed to retrieve account, handle error
                }
                response.close();

                return true;
            } catch (DistortJson.DistortException e) {
                mErrorCode = -1;
                e.printStackTrace();
                Log.e("FETCH-ACCOUNT", e.getMessage() + " : " + String.valueOf(e.getResponseCode()));

                return false;
            } catch (IOException e) {
                mErrorCode = -2;
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAccountTask = null;
            Log.d("GET-ACCOUNT", String.valueOf(mErrorCode));

            if (success) {

            } else {

            }
        }
    }
}