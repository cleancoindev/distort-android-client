package com.unix4all.rypi.distort;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ConversationsActivity extends AppCompatActivity implements NewConversationFragment.NewConversationListener {

    private String mHomserverAddress;
    private String mHomserverProtocol;
    private String mPeerId;
    private String mCredential;

    private FetchGroupsTask mBackgroundTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        Intent thisIntent = getIntent();
        mHomserverAddress = thisIntent.getStringExtra(LoginActivity.EXTRA_HOMESERVER);
        mHomserverProtocol = thisIntent.getStringExtra(LoginActivity.EXTRA_HOMESERVER_PROTOCOL);
        mPeerId = thisIntent.getStringExtra(LoginActivity.EXTRA_PEER_ID);
        mCredential = thisIntent.getStringExtra(LoginActivity.EXTRA_CREDENTIAL);

        // Discover groups for this account
        mBackgroundTask = new FetchGroupsTask();
        mBackgroundTask.execute();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

    private @Nullable JsonReader GetJSONFromURL(HttpURLConnection myConnection) throws ProtocolException, IOException {
        myConnection.setRequestMethod("GET");

        // Set request header fields
        myConnection.setRequestProperty("User-Agent", "distort-android-v0.1");
        myConnection.setRequestProperty("Accept","*/*");
        myConnection.setRequestProperty("peerid", mPeerId);
        myConnection.setRequestProperty("authtoken", mCredential);

        // Make connection and determine response
        myConnection.connect();
        int response = myConnection.getResponseCode();

        // Reading the response
        JsonReader jsonReader = null;
        if(response == 200) {
            jsonReader = new JsonReader(new InputStreamReader(myConnection.getInputStream(), "UTF-8"));
        }
        myConnection.disconnect();

        return jsonReader;
    }

    /**
     * Represents an asynchronous login/registration task used to retrieve all the user's groups
     */
    public class FetchGroupsTask extends AsyncTask<Void, Void, Boolean> {

        private int errorCode;

        FetchGroupsTask() {
            errorCode = 0;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            errorCode = 0;

            // Attempt authentication against a network service.
            try {
                JsonReader response = null;
                URL homeserverEndpoint = new URL(mHomserverAddress + "groups");
                if(LoginActivity.PROTOCOL_HTTPS.equals(mHomserverProtocol)) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    response = GetJSONFromURL(myConnection);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    response = GetJSONFromURL(myConnection);
                }
                if(response == null) {
                    return false;
                }

                // Read all groups
                response.beginArray();
                while(response.hasNext()) {
                    String name = null;
                    Integer subgroupIndex = null;
                    Integer height = null;
                    Integer lastReadIndex = null;

                    // Read all fields from group
                    response.beginObject();
                    while(response.hasNext()) {
                        // name subgroupIndex height lastReadIndex
                        String key = response.nextName();
                        if(key.equals("name")) {
                            name = response.nextString();
                        } else if(key.equals("subgroupIndex")) {
                            subgroupIndex = response.nextInt();
                        } else if(key.equals("height")) {
                            height = response.nextInt();
                        } else if(key.equals("lastReadIndex")) {
                            lastReadIndex = response.nextInt();
                        } else {
                            response.skipValue();
                        }
                    }
                    if(name != null && subgroupIndex != null && height != null && lastReadIndex != null) {
                        Log.d("GET-GROUPS", "Group ( " + name + "," + subgroupIndex + "," + height+ "," + lastReadIndex + " )");
                    }
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
            Log.d("GET-GROUPS", String.valueOf(errorCode));

            if (success) {

            } else {

            }
        }
    }
}