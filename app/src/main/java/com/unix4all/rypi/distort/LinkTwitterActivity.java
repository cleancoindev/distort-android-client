package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EdgeEffect;
import android.widget.EditText;

import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.User;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import retrofit2.Call;
import retrofit2.Response;

public class LinkTwitterActivity extends AppCompatActivity implements SocialMediaHandleAdapter.ItemClickListener {
    private RecyclerView mSearchResults;
    private EditText mHandleEdit;
    private Button mLinkButton;

    private UserTwitterApiClient twitterClient;
    private SocialMediaHandleAdapter mHandleAdapter;

    private FetchLinkTask mLinkTask;
    private SearchUsersTask mSearchUsersTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_twitter);
        final Activity self = this;

        // Setup Twitter parameters if authenticated
        final TwitterSession activeSession = TwitterCore.getInstance()
                .getSessionManager().getActiveSession();
        if(activeSession != null) {
            Log.d("GET-LINK", "There is an active session!");

            twitterClient = new UserTwitterApiClient(activeSession);
            TwitterCore.getInstance().addApiClient(activeSession, twitterClient);
        } else {
            Log.d("GET-LINK", "There are no sessions active..");

            twitterClient = null;
        }

        // User list
        mSearchResults = findViewById(R.id.twitterSearchList);
        mSearchResults.setLayoutManager(new LinearLayoutManager(this));
        mHandleAdapter = new SocialMediaHandleAdapter(this, new ArrayList<String>());
        mHandleAdapter.setClickListener(this);
        mSearchResults.setAdapter(mHandleAdapter);

        // Account-handle field
        mHandleEdit = findViewById(R.id.twitterHandleEdit);
        mHandleEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String handle = s.toString();
                if(!handle.isEmpty()) {
                    if (mSearchUsersTask == null && twitterClient != null) {
                        mSearchUsersTask = new SearchUsersTask(self, handle);
                        mSearchUsersTask.execute();
                    }
                } else {
                    HashSet<String> emptySet = new HashSet<>();
                    mHandleAdapter.updateToSet(emptySet);
                }
            }
        });

        // Link button
        mLinkButton = findViewById(R.id.linkTwitterButton);
        mLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mLinkTask == null) {
                    mLinkTask = new FetchLinkTask(self, mHandleEdit.getText().toString());
                    mLinkTask.execute();
                }
            }
        });
    }

    @Override
    public void onItemClick(View view, int position) {
        mHandleEdit.setText(mHandleAdapter.getItem(position));
    }

    public class FetchLinkTask extends AsyncTask<Void, Void, Boolean> {
        private Context mContext;
        private String mHandle;

        private String mErrorString;
        private String mRespPeerId;
        private String mRespAccountName;

        FetchLinkTask(Context ctx, String handle) {
            mContext = ctx;
            mHandle = handle;
            mErrorString = "";
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorString = "";
            mRespAccountName = null;
            mRespPeerId = null;
            DistortAuthParams login = DistortAuthParams.getAuthenticationParams(mContext);

            // Attempt authentication against a network service.
            try {
                JsonReader response = null;
                String url = login.getHomeserverAddress() + "social-media";
                HashMap<String, String> query = new HashMap<>();
                query.put("platform", "twitter");
                query.put("handle", mHandle);
                url += DistortJson.getQueryString(query);

                URL homeserverEndpoint = new URL(url);
                if(DistortAuthParams.PROTOCOL_HTTPS.equals(login.getHomeserverProtocol())) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.getJSONFromURL(myConnection, login);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    response = DistortJson.getJSONFromURL(myConnection, login);
                }

                response.beginObject();
                while(response.hasNext()) {
                    String key = response.nextName();
                    if(key.equals("peerId")) {
                        mRespPeerId = response.nextString();
                    } else if(key.equals("accountName")) {
                        mRespAccountName = response.nextString();
                    } else {
                        response.skipValue();
                    }
                }
                response.endObject();
                response.close();

                if(mRespAccountName == null || mRespPeerId == null) {
                    // Server returned incorrect fields
                    throw new DistortJson.DistortException("", 500);
                }
                return true;
            } catch (DistortJson.DistortException e) {
                if (e.getResponseCode() == 404) {           // User is not a member of specified account
                    mErrorString = e.getMessage();
                } else if (e.getResponseCode() == 500) {
                    Log.d("GET-LINK", e.getMessage());
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
            mLinkTask = null;
            if (success) {
                Intent intent = new Intent();
                intent.putExtra("accountName", mRespAccountName);
                intent.putExtra("peerId", mRespPeerId);
                intent.putExtra("handle", mHandle);

                setResult(RESULT_OK, intent);
                finish();
            } else {
                Snackbar.make(findViewById(R.id.linkTwitterConstraint), mErrorString,
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        }
    }


    public class SearchUsersTask extends AsyncTask<Void, Void, Boolean> {
        private Activity mContext;
        private String mHandle;

        private final HashSet<String> mRespHandles;
        private String mErrorString;

        SearchUsersTask(Activity ctx, String handle) {
            mContext = ctx;
            mHandle = handle;
            mErrorString = "";
            mRespHandles = new HashSet<>();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Call<List<User>> usersTask = twitterClient.getSearchUsersService().search(mHandle, 10);

            try {
                Response<List<User>> response = usersTask.execute();
                if(!response.isSuccessful()) {
                    throw new IOException(response.message());
                }

                // Update handles
                List<User> body = response.body();
                for(User u : body) {
                    mRespHandles.add(u.screenName);
                }

                return true;
            } catch(IOException e) {
                e.printStackTrace();
                mErrorString = getString(R.string.error_search_accounts_twitter);
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSearchUsersTask = null;
            if(success) {
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mHandleAdapter.updateToSet(mRespHandles);
                    }
                });
            } else {
                Snackbar.make(findViewById(R.id.linkTwitterConstraint), mErrorString,
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        }
    }
}
