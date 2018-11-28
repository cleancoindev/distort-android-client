package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class MessagingActivity extends AppCompatActivity {

    private DistortAuthParams mLoginParams;

    // Header fields
    private TextView mIcon;
    private TextView mName;
    private TextView mDetails;

    private Integer mHeight;
    private Integer mInitUnread;
    private FetchMessagesTask mMessagesTask;

    // Bottom layout
    private RecyclerView mMessagesView;
    private MessageAdapter mMessagesAdapter;
    private TextView mSendMessageView;
    private EditText mMessageEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);
        Intent thisIntent = getIntent();

        // Setup authorization fields
        mLoginParams = new DistortAuthParams();
        mLoginParams.setHomeserverAddress(thisIntent.getStringExtra(DistortAuthParams.EXTRA_HOMESERVER));
        mLoginParams.setHomeserverProtocol(thisIntent.getStringExtra(DistortAuthParams.EXTRA_HOMESERVER_PROTOCOL));
        mLoginParams.setPeerId(thisIntent.getStringExtra(DistortAuthParams.EXTRA_PEER_ID));
        mLoginParams.setAccountName(thisIntent.getStringExtra(DistortAuthParams.EXTRA_ACCOUNT_NAME));
        mLoginParams.setCredential(thisIntent.getStringExtra(DistortAuthParams.EXTRA_CREDENTIAL));

        // Setup top fields
        mIcon = findViewById(R.id.messagesIcon);
        mName = findViewById(R.id.messagesName);
        mDetails = findViewById(R.id.messagesDetails);
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            mIcon.setText(bundle.getString("icon"));
            ((GradientDrawable) mIcon.getBackground()).setColor(bundle.getInt("colorIcon"));
            mName.setText(bundle.getString("name"));

            mHeight = bundle.getInt("height");
            mDetails.setText(String.format(Locale.US, "%s: %d",
                    getResources().getString(R.string.messages_total_height), mHeight));
            mInitUnread = bundle.getInt("unread");
        }

        // Setup list of message-list properties
        mMessagesView = (RecyclerView) findViewById(R.id.messagesView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MessagingActivity.this, LinearLayoutManager.VERTICAL, false);
        mMessagesView.setLayoutManager(linearLayoutManager);
        mMessagesView.addItemDecoration(new DividerItemDecoration(MessagingActivity.this, DividerItemDecoration.VERTICAL));
        mMessagesAdapter = new MessageAdapter(new ArrayList<DistortMessage>());
        mMessagesView.setAdapter(mMessagesAdapter);

        // Setup other fields
        mSendMessageView = (TextView) findViewById(R.id.sendMessageView);
        mMessageEdit = (EditText) findViewById(R.id.messageEdit);

        // Discover groups for this account
        // TODO: Start at a more reasonable index than always 0
        mMessagesTask = new FetchMessagesTask(this, mHeight, 0);
        mMessagesTask.execute();
    }


    /**
     * Represents an asynchronous login/registration task used to retrieve all the user's groups
     */
    public class FetchMessagesTask extends AsyncTask<Void, Void, Boolean> {

        private int errorCode;
        private Activity mActivity;
        private Integer mHeight;
        private Integer mStartIndex;

        FetchMessagesTask(Activity activity, Integer groupHeight, Integer startIndex) {
            mActivity = activity;
            mHeight = groupHeight;
            mStartIndex = startIndex;

            errorCode = 0;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            errorCode = 0;

            // Attempt authentication against a network service.
            try {
                JsonReader response = null;
                String url = mLoginParams.getHomeserverAddress() + "groups/" + mName.getText().toString() + "/" + String.valueOf(mStartIndex);
                URL homeserverEndpoint = new URL(url);
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

                // Read all messages in messages and out messages
                ArrayList<InMessage> inMessages = new ArrayList<>();
                ArrayList<OutMessage> outMessages = new ArrayList<>();
                response.beginObject();
                while(response.hasNext()) {
                    String key = response.nextName();
                    Log.d("GET-GROUP-MESSAGES", key);

                    if(key.equals("in")) {
                        inMessages = InMessage.readArrayJson(response);
                    } else if(key.equals("out")) {
                        outMessages = OutMessage.readArrayJson(response);
                    } else {
                        response.skipValue();
                    }
                }
                response.endObject();
                response.close();

                final DistortMessage allMessages[] = new DistortMessage[inMessages.size() + outMessages.size()];
                for(int inIndex = 0; inIndex < inMessages.size(); inIndex++) {
                    allMessages[inMessages.get(inIndex).getIndex()] = (DistortMessage) inMessages.get(inIndex);
                }
                for(int outIndex = 0; outIndex < outMessages.size(); outIndex++) {
                    allMessages[outMessages.get(outIndex).getIndex()] = (DistortMessage) outMessages.get(outIndex);
                }

                final ArrayList<DistortMessage> messages = new ArrayList<DistortMessage>();
                for(int i = 0; i < allMessages.length; i++) {
                    messages.add(allMessages[i]);
                }

                // Can only update UI from UI thread
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for(int i = 0; i < messages.size(); i++) {
                            mMessagesAdapter.addOrUpdateMessage(messages.get(i));
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
            } catch (ParseException e) {
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
