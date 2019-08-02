package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import yuku.ambilwarna.AmbilWarnaDialog;

public class GroupsActivity extends AppCompatActivity implements NewGroupFragment.NewGroupListener,
        TimedRemoveFragment.OnFragmentFinishedListener, GettingStartedFragment.GettingStartedCloseListener {
    private final GroupsActivity mActivity = this;

    private DistortAuthParams mLoginParams;

    private RecyclerView mGroupsView;
    private GroupAdapter mGroupsAdapter;

    private AddGroupTask mAddGroupTask;
    private RemoveGroupTask mRemoveGroupTask;

    private GroupServiceBroadcastReceiver mGroupServiceReceiver;
    private AccountServiceBroadcastReceiver mAccountServiceReceiver;
    private BackgroundErrorBroadcastReceiver mBackgroundErrorReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        mLoginParams = DistortAuthParams.getAuthenticationParams(this);

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
        mGroupsAdapter = new GroupAdapter(this, new ArrayList<DistortGroup>(), null);
        mGroupsView.setAdapter(mGroupsAdapter);
        HashMap<String, DistortGroup> allGroups = DistortBackgroundService.getLocalGroups(this, mLoginParams.getFullAddress());
        for(Map.Entry<String, DistortGroup> group : allGroups.entrySet()) {
            mGroupsAdapter.addOrUpdateGroup(group.getValue());
        }

        // Set active group if can
        DistortAccount account = DistortBackgroundService.getLocalAccount(this);
        if(account != null) {
            String activeGroup = account.getActiveGroup();
            if(activeGroup != null && !activeGroup.isEmpty()) {
                mGroupsAdapter.updateActiveGroup(activeGroup);
            }
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showJoinNewGroup();
            }
        });

        // First login handling
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.getting_started_preferences_key), Context.MODE_PRIVATE);
        if(!sharedPref.getBoolean("gettingStartedGroups", false)) {
            View groups = findViewById(R.id.groupsView);
            groups.setVisibility(View.GONE);

            fab.setVisibility(View.INVISIBLE);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            GettingStartedFragment f = GettingStartedFragment.newInstance(
                    getString(R.string.text_getting_started_groups), "gettingStartedGroups");
            ft.replace(R.id.gettingStartedGroupsLayout, f, "fragment_gettingStartedGroupsLayout").addToBackStack(null).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater findMenuItems = getMenuInflater();
        findMenuItems.inflate(R.menu.menu_options, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.aboutOption:
                FragmentManager fm = getSupportFragmentManager();
                AboutFragment aboutFragment = AboutFragment.newInstance();
                aboutFragment.show(fm, "fragment_aboutLayout");
                return true;
            case R.id.settingsOption:
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                return true;
            case R.id.accountOption:
                intent = new Intent(this, AccountActivity.class);
                this.startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showJoinNewGroup() {
        FragmentManager fm = getSupportFragmentManager();
        NewGroupFragment newGroupFragment = NewGroupFragment.newInstance();
        newGroupFragment.show(fm, "fragment_newGroupLayout");
    }

    public void showRemoveGroup(Integer groupIndex) {
        FragmentManager fm = getSupportFragmentManager();

        String title = getResources().getString(R.string.title_remove_group);
        String description = getResources().getString(R.string.description_remove_group);

        TimedRemoveFragment timedRemoveGroupFragment = TimedRemoveFragment.newInstance(this, title, description, groupIndex);
        timedRemoveGroupFragment.show(fm, "fragment_removeGroupLayout");
    }

    public void openColourPicker(final int groupIndex) {
        final String name = mGroupsAdapter.getItem(groupIndex).getName();
        final String id = name+":colour";

        final SharedPreferences sp = getSharedPreferences(getString(R.string.icon_colours_preferences_keys),
                Context.MODE_PRIVATE);
        final int defaultColour = sp.getInt(id, 0);
        AmbilWarnaDialog colourDialog = new AmbilWarnaDialog(this, defaultColour, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {}

            @Override
            public void onOk(AmbilWarnaDialog dialog, int colour) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt(id, colour);
                editor.apply();
                mGroupsAdapter.notifyItemChanged(groupIndex);
            }
        });
        colourDialog.show();
    }

    public void showChangeGroupLevel(int position) {
        DistortGroup g = mGroupsAdapter.getItem(position);

        FragmentManager fm = getSupportFragmentManager();
        SetGroupLevelFragment setGroupLevelFragment = SetGroupLevelFragment.newInstance(g.getName(), g.getSubgroupLevel());
        setGroupLevelFragment.show(fm, "fragment_setGroupLevelLayout");
    }

    @Override
    public void onFinishGroupFieldInputs(String groupName, Integer subgroupLevel) {
        mAddGroupTask = new AddGroupTask(this, groupName, subgroupLevel);
        mAddGroupTask.execute();
    }

    @Override
    public void onTimedRemoveFinished(Boolean removeChoice, @Nullable Integer groupIndex) {
        if(removeChoice) {
            String groupName = mGroupsAdapter.getItem(groupIndex).getName();
            mRemoveGroupTask = new RemoveGroupTask(this, groupName, groupIndex);
            mRemoveGroupTask.execute();
        }
    }

    @Override
    public void OnGettingStartedClose() {
        View groups = findViewById(R.id.groupsView);
        groups.setVisibility(View.VISIBLE);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
    }

    // Handle successful retrieval of groups
    @Override
    protected void onStart() {
        mBackgroundErrorReceiver = new BackgroundErrorBroadcastReceiver(findViewById(R.id.groupConstraintLayout), this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.BACKGROUND_ERROR);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBackgroundErrorReceiver, intentFilter);

        mGroupServiceReceiver = new GroupsActivity.GroupServiceBroadcastReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.ACTION_FETCH_GROUPS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mGroupServiceReceiver, intentFilter);

        mAccountServiceReceiver = new GroupsActivity.AccountServiceBroadcastReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(DistortBackgroundService.ACTION_FETCH_ACCOUNT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mAccountServiceReceiver, intentFilter);

        // Fetch groups and account
        DistortBackgroundService.startActionScheduleSecondaryServices(getApplicationContext());

        super.onStart();
    }
    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGroupServiceReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mAccountServiceReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBackgroundErrorReceiver);
        super.onStop();
    }

    public class GroupServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final HashMap<String, DistortGroup> allGroups = DistortBackgroundService.getLocalGroups(getApplicationContext(), mLoginParams.getFullAddress());
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for(Map.Entry<String, DistortGroup> group : allGroups.entrySet()) {
                        mGroupsAdapter.addOrUpdateGroup(group.getValue());
                    }
                }
            });
        }
    }

    public class AccountServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Context appContext = getApplicationContext();
            final DistortAccount account = DistortBackgroundService.getLocalAccount(appContext);

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mGroupsAdapter.updateActiveGroup(account.getActiveGroup());
                }
            });
        }
    }

    /**
     * Represents an asynchronous task used to add a group to account
     */
    public class AddGroupTask extends AsyncTask<Void, Void, Boolean> {

        private Activity mActivity;
        private String mGroupName;
        private Integer mSubgroupLevel;
        private String mErrorString;

        AddGroupTask(Activity activity, String groupName, Integer subgroupLevel) {
            mActivity = activity;
            mGroupName = groupName;
            mSubgroupLevel = subgroupLevel;
            mErrorString = "";
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorString = "";

            // Attempt authentication against a network service.
            try {
                JsonReader response = null;
                String url = mLoginParams.getHomeserverAddress() + "groups";

                HashMap<String, String> bodyParams = new HashMap<>();
                bodyParams.put("name", mGroupName);
                bodyParams.put("subgroupLevel", String.valueOf(mSubgroupLevel));

                URL homeserverEndpoint = new URL(url);
                if(DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.postBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.postBodyGetJSONFromURL(myConnection, mLoginParams, bodyParams);
                }

                // Read new group response
                final DistortGroup newGroup = DistortGroup.readJson(response);
                response.close();

                // If new group is the only group, then it will be active
                if(mGroupsAdapter.getItemCount() == 0) {
                    mGroupsAdapter.updateActiveGroup(newGroup.getName());
                }

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mGroupsAdapter.addOrUpdateGroup(newGroup);
                    }
                });

                return true;
            } catch (DistortJson.DistortException e) {
                if (e.getResponseCode() == 400) {           // Incorrectly formatted fields
                    mErrorString = e.getMessage();
                } else if (e.getResponseCode() == 500) {
                    Log.d("ADD-GROUP", e.getMessage());
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
            mAddGroupTask = null;
            if (success) {
                // Invalidated local groups cache, refetch
                DistortBackgroundService.startActionFetchGroups(getApplicationContext());
            } else {
                Snackbar.make(findViewById(R.id.groupConstraintLayout), mErrorString,
                    Snackbar.LENGTH_LONG)
                    .show();
            }
        }
    }


    /**
     * Represents an asynchronous task used to remove a group
     */
    public class RemoveGroupTask extends AsyncTask<Void, Void, Boolean> {

        private Activity mActivity;
        private String mGroupName;
        private int mIndex;
        private String mErrorString;

        RemoveGroupTask(Activity activity, String groupName, int groupIndex) {
            mActivity = activity;
            mGroupName = groupName;
            mIndex = groupIndex;
            mErrorString = "";
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorString = "";

            // Attempt authentication against a network service.
            try {
                JsonReader response = null;
                String url = mLoginParams.getHomeserverAddress() + "groups/" + Uri.encode(mGroupName);

                HashMap<String, String> bodyParams = new HashMap<>();

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
                        mGroupsAdapter.removeItem(mIndex);
                    }
                });

                return true;
            } catch (DistortJson.DistortException e) {
                if (e.getResponseCode() == 404) {           // User is not a member of specified account
                    mErrorString = e.getMessage();
                } else if (e.getResponseCode() == 500) {
                    Log.d("REMOVE-GROUP", e.getMessage());
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
            mRemoveGroupTask = null;
            if (success) {
                // Invalidated local groups cache, refetch
                DistortBackgroundService.startActionFetchGroups(getApplicationContext());
            } else {
                Snackbar.make(findViewById(R.id.groupConstraintLayout), mErrorString,
                    Snackbar.LENGTH_LONG)
                    .show();
            }
        }
    }
}