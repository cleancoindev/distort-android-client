package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
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

import javax.net.ssl.HttpsURLConnection;

public class ConversationsActivity extends AppCompatActivity implements NewConversationFragment.NewConversationListener {

    private DistortAuthParams mLoginParams;

    private RecyclerView mGroupsView;
    private GroupAdapter mGroupsAdapter;

    private FetchGroupsTask mGroupsTask;
    private FetchAccountTask mAccountTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        Intent thisIntent = getIntent();
        mLoginParams = new DistortAuthParams();
        mLoginParams.setHomeserverAddress(thisIntent.getStringExtra(DistortAuthParams.EXTRA_HOMESERVER));
        mLoginParams.setHomeserverProtocol(thisIntent.getStringExtra(DistortAuthParams.EXTRA_HOMESERVER_PROTOCOL));
        mLoginParams.setPeerId(thisIntent.getStringExtra(DistortAuthParams.EXTRA_PEER_ID));
        mLoginParams.setAccountName(thisIntent.getStringExtra(DistortAuthParams.EXTRA_ACCOUNT_NAME));
        mLoginParams.setCredential(thisIntent.getStringExtra(DistortAuthParams.EXTRA_CREDENTIAL));

        // Init toolbar
        Toolbar toolbar = findViewById(R.id.groupToolbar);
        toolbar.setTitle(R.string.title_activity_conversations);
        setSupportActionBar(toolbar);

        // Setup list of groups-list properties
        mGroupsView = (RecyclerView) findViewById(R.id.groupsView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ConversationsActivity.this, LinearLayoutManager.VERTICAL, false);
        mGroupsView.setLayoutManager(linearLayoutManager);
        mGroupsView.addItemDecoration(new DividerItemDecoration(ConversationsActivity.this, DividerItemDecoration.VERTICAL));

        // Prepare for datasets
        mGroupsAdapter = new GroupAdapter(ConversationsActivity.this, new ArrayList<DistortGroup>(), mLoginParams);
        mGroupsView.setAdapter(mGroupsAdapter);

        // Fetch account parameters
        mAccountTask = new FetchAccountTask(this);
        mAccountTask.execute();

        // Discover groups for this account
        mGroupsTask = new FetchGroupsTask(this);
        mGroupsTask.execute();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCreateNewConversation();
            }
        });
    }

    private void showCreateNewConversation() {
        FragmentManager fm = getSupportFragmentManager();
        NewConversationFragment newConversationFragment = NewConversationFragment.newInstance();
        newConversationFragment.show(fm, "fragment_newConversationLayout");
    }

    @Override
    public void onFinishConvoFieldInputs(String friendlyName, String peerId) {
        // TODO: Attempt to add group to homeserver (PUT /groups)
    }

    /**
     * Represents an asynchronous login/registration task used to retrieve all the user's groups
     */
    public class FetchGroupsTask extends AsyncTask<Void, Void, Boolean> {

        private int errorCode;
        private Activity mActivity;

        FetchGroupsTask(Activity activity) {
            mActivity = activity;
            errorCode = 0;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            errorCode = 0;

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
                if(response == null) {
                    return false;
                }

                // Read all groups
                response.beginArray();
                final ArrayList<DistortGroup> allGroups = new ArrayList<>();
                while(response.hasNext()) {
                    allGroups.add(DistortGroup.readGroupJson(response));
                }
                response.endArray();
                response.close();

                // Can only update UI from UI thread
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for(int i = 0; i < allGroups.size(); i++) {
                            mGroupsAdapter.addOrUpdateGroup(allGroups.get(i));
                        }
                    }
                });

                return true;
            } catch (MalformedURLException e) {
                errorCode = -3;
                return false;
            } catch (IOException e) {
                errorCode = -4;
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            Log.d("GET-GROUPS", String.valueOf(errorCode));

            // TODO: Handle failure
            if(!success) {

            }
        }
    }

    /**
     * Represents an asynchronous login/registration task used to retrieve all the user's groups
     */
    public class FetchAccountTask extends AsyncTask<Void, Void, Boolean> {

        private int errorCode;
        private Activity mActivity;

        FetchAccountTask(Activity activity) {
            mActivity = activity;
            errorCode = 0;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            errorCode = 0;

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

                    Log.d("GET-ACCOUNT", "Account ( " + accountName + "," + enabled + "," + peerId + " )");
                } else {
                    // TODO: Failed to retrieve account, handle error
                }
                response.close();

                return true;
            } catch (MalformedURLException e) {
                errorCode = -3;
                return false;
            } catch (IOException e) {
                errorCode = -4;
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            Log.d("GET-ACCOUNT", String.valueOf(errorCode));

            if (success) {

            } else {

            }
        }
    }
}