package com.unix4all.rypi.distort;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

public class SettingsActivity extends AppCompatActivity implements ChangePasswordFragment.ChangePasswordListener {
    private final Activity mActivity = this;

    private DistortAuthParams mLoginParams;

    private ArrayList<DistortGroup> mGroups;
    private ArrayList<String> mGroupNames;
    private DistortAccount mAccount;

    private Switch mEnabledToggle;
    private Spinner mActiveGroupSpinner;
    private Button mChangePswdButton;
    private UpdateAccountTask mUpdateAccountTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mLoginParams = DistortAuthParams.getAuthenticationParams(this);
        mAccount = DistortBackgroundService.getLocalAccount(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);

        mEnabledToggle = findViewById(R.id.settingsEnabledSwitch);
        mEnabledToggle.setChecked(mAccount.getEnabled());
        mEnabledToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                HashMap<String, String> p = new HashMap<>();
                p.put("accountName", mLoginParams.getAccountName());
                p.put("enabled", String.valueOf(mEnabledToggle.isChecked()));
                mUpdateAccountTask = new UpdateAccountTask(p);
                mUpdateAccountTask.execute();
            }
        });
        if(mAccount.getAccountName().equals("root")) {
            mEnabledToggle.setEnabled(false);
        }


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

            if(groupEntry.getValue().getName().equals(mAccount.getActiveGroup())) {
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
        mActiveGroupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view,
                                       int position, long id) {
                HashMap<String, String> p = new HashMap<>();
                p.put("accountName", mLoginParams.getAccountName());
                if(mGroups.get(position) == null) {
                    // Remove active group
                    p.put("activeGroup", "");
                } else {
                    p.put("activeGroup", mGroups.get(position).getName());
                }
                mUpdateAccountTask = new UpdateAccountTask(p);
                mUpdateAccountTask.execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {}
        });

        mChangePswdButton = findViewById(R.id.changePasswordButton);
        mChangePswdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangePassword();
            }
        });
    }

    public void showChangePassword() {
        FragmentManager fm = getSupportFragmentManager();
        ChangePasswordFragment changePasswordFragment = ChangePasswordFragment.newInstance(mLoginParams.getPeerId());
        changePasswordFragment.show(fm, "fragment_changePassword");
    }

    @Override
    public void onChangePasswordFinished(String authToken) {
        HashMap<String, String> p = new HashMap<>();
        p.put("accountName", mLoginParams.getAccountName());
        p.put("authToken", authToken);
        mUpdateAccountTask = new UpdateAccountTask(p);
        mUpdateAccountTask.execute();
    }

    /**
     * Represents an asynchronous task used to update account
     */
    public class UpdateAccountTask extends AsyncTask<Void, Void, Boolean> {

        private String mErrorString;
        private HashMap<String, String> mBodyParams;

        UpdateAccountTask(HashMap<String, String> bodyParams) {
            mErrorString = "";
            mBodyParams = bodyParams;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorString = "";

            // Attempt authentication against a network service.
            try {
                JsonReader response;
                String url = mLoginParams.getHomeserverAddress() + "account";

                URL homeserverEndpoint = new URL(url);
                if(DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.putBodyGetJSONFromURL(myConnection, mLoginParams, mBodyParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.putBodyGetJSONFromURL(myConnection, mLoginParams, mBodyParams);
                }
                response.close();


                return true;
            } catch (DistortJson.DistortException e) {
                if (e.getResponseCode() == 403) {           // Not authorized to update specified account
                    mErrorString = e.getMessage();
                } else if (e.getResponseCode() == 404) {    // Account to update does not exist
                    mErrorString = e.getMessage();
                } else if (e.getResponseCode() == 500) {
                    Log.d("UPDATE-ACCOUNT", e.getMessage());
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

        @SuppressLint("ApplySharedPref")
        @Override
        protected void onPostExecute(final Boolean success) {
            mUpdateAccountTask = null;
            if (success) {
                // Check if password was changed
                if(mBodyParams.containsKey("authToken")) {
                    String token = mBodyParams.get("authToken");
                    mLoginParams.setCredential(token);

                    SharedPreferences sp = getSharedPreferences(
                            getString(R.string.account_credentials_preferences_key), Context.MODE_PRIVATE);

                    // Wait for write since background needs updated token
                    sp.edit().putString(DistortAuthParams.EXTRA_CREDENTIAL, token).commit();
                }

                Context context = getApplicationContext();
                DistortBackgroundService.startActionFetchAccount(context);
                DistortBackgroundService.startActionFetchGroups(context);
            } else {
                Log.e("UPDATE-ACCOUNT", mErrorString);
                // TODO: Reset relevant fields on error
                // Reset enable-status on error
                mEnabledToggle.setChecked(mAccount.getEnabled());

                Snackbar.make(findViewById(R.id.settingsConstraintLayout), mErrorString,
                    Snackbar.LENGTH_LONG)
                    .show();
            }
        }
    }

}
