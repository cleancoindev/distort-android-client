package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

public class SettingsActivity extends AppCompatActivity {
    private final Activity mActivity = this;

    private DistortAuthParams mLoginParams;

    private ArrayList<DistortGroup> mGroups;
    private ArrayList<String> mGroupNames;
    private DistortAccount mAccount;

    private Switch mEnabledToggle;
    private Spinner mActiveGroupSpinner;
    private Button mSaveButton;
    private UpdateAccountTask mUpdateAccountTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Use shared preferences to fetch authorization params
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.account_preferences_key), Context.MODE_PRIVATE);
        mLoginParams = new DistortAuthParams();
        mLoginParams.setHomeserverAddress(sharedPref.getString(DistortAuthParams.EXTRA_HOMESERVER, null));
        mLoginParams.setHomeserverProtocol(sharedPref.getString(DistortAuthParams.EXTRA_HOMESERVER_PROTOCOL, null));
        mLoginParams.setPeerId(sharedPref.getString(DistortAuthParams.EXTRA_PEER_ID, null));
        mLoginParams.setAccountName(sharedPref.getString(DistortAuthParams.EXTRA_ACCOUNT_NAME, null));
        mLoginParams.setCredential(sharedPref.getString(DistortAuthParams.EXTRA_CREDENTIAL, null));
        mAccount = DistortBackgroundService.getLocalAccount(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);

        mEnabledToggle = findViewById(R.id.settingsEnabledSwitch);
        mEnabledToggle.setChecked(mAccount.getEnabled());

        Integer selectedItem = 0;
        HashMap<String, DistortGroup> groupMap = DistortBackgroundService.getLocalGroups(this);
        mGroups = new ArrayList<>();
        mGroups.add(null);
        mGroupNames = new ArrayList<>();
        mGroupNames.add("");
        int i = 1;
        for(HashMap.Entry<String, DistortGroup> groupEntry : groupMap.entrySet()) {
            mGroups.add(groupEntry.getValue());
            mGroupNames.add(groupEntry.getValue().getName());

            if(groupEntry.getValue().getId().equals(mAccount.getActiveGroupId())) {
                selectedItem = i;
            }
            i++;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, mGroupNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mActiveGroupSpinner = findViewById(R.id.activeGroupSpinner);
        mActiveGroupSpinner.setAdapter(adapter);
        mActiveGroupSpinner.setSelection(selectedItem);

        mSaveButton = findViewById(R.id.saveAccountButton);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mUpdateAccountTask == null) {
                    mUpdateAccountTask = new UpdateAccountTask(mActivity);
                    mUpdateAccountTask.execute();
                }
            }
        });
    }

    /**
     * Represents an asynchronous task used to update account
     */
    public class UpdateAccountTask extends AsyncTask<Void, Void, Boolean> {

        private Activity mActivity;
        private int mErrorCode;

        UpdateAccountTask(Activity activity) {
            mActivity = activity;
            mErrorCode = 0;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorCode = 0;

            // Attempt authentication against a network service.
            try {
                JsonReader response = null;
                String url = mLoginParams.getHomeserverAddress() + "account";

                HashMap<String, String> bodyParams = new HashMap<>();
                bodyParams.put("accountName", mLoginParams.getAccountName());
                bodyParams.put("enabled", String.valueOf(mEnabledToggle.isChecked()));
                int position = mActiveGroupSpinner.getSelectedItemPosition();
                if(mGroups.get(position) == null) {
                    bodyParams.put("activeGroup", "");
                } else {
                    bodyParams.put("activeGroup", mGroups.get(mActiveGroupSpinner.getSelectedItemPosition()).getId());
                }

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
                response.close();


                return true;
            } catch (DistortJson.DistortException e) {
                mErrorCode = -1;
                e.printStackTrace();
                Log.e("UPDATE-ACCOUNT", e.getMessage() + " : " + String.valueOf(e.getResponseCode()));

                return false;
            } catch (IOException e) {
                mErrorCode = -2;
                e.printStackTrace();

                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mUpdateAccountTask = null;
            Log.d("UPDATE-ACCOUNT", String.valueOf(mErrorCode));

            // TODO: Handle errors here
            if (success) {
                DistortBackgroundService.startActionFetchAccount(mActivity);
                DistortBackgroundService.startActionFetchGroups(mActivity);
            } else {

            }
        }
    }

}
