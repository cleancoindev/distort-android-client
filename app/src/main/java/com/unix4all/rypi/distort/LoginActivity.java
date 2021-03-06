package com.unix4all.rypi.distort;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.twitter.sdk.android.core.Twitter;

import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Base64;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

import javax.net.ssl.HttpsURLConnection;

import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_ACCOUNT_NAME;
import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_CREDENTIAL;
import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_HOMESERVER;
import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_HOMESERVER_PROTOCOL;
import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_PEER_ID;

/**
 * A login screen that offers login via homeserver / password.
 */
public class LoginActivity extends AppCompatActivity implements CreateAccountFragment.OnAccountCreationListener, GettingStartedFragment.GettingStartedCloseListener {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mHomeserverView;
    private EditText mAccountNameView;
    private EditText mPasswordView;
    private Button mSignInButton;
    private Button mSignInWithStoredButton;
    private Button mCreateAccountButton;

    private View mProgressView;
    private View mLoginFormView;
    private @Nullable String mToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Set up the login form.
        mHomeserverView = (EditText) findViewById(R.id.homeserver);
        mAccountNameView = (EditText) findViewById(R.id.accountName);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
            }
        });

        mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mCreateAccountButton = findViewById(R.id.loginCreateAccountButton);
        mCreateAccountButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                View loginForm = findViewById(R.id.login_form);
                loginForm.setVisibility(View.GONE);

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                CreateAccountFragment f = CreateAccountFragment.newInstance(
                    mHomeserverView.getText().toString(),
                    mAccountNameView.getText().toString(),
                    mPasswordView.getText().toString());
                ft.replace(R.id.loginActivityLayout, f, "fragment_createAccountLayout").addToBackStack(null).commit();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.account_credentials_preferences_key), Context.MODE_PRIVATE);
        mToken = sharedPref.getString(EXTRA_CREDENTIAL, null);
        mSignInWithStoredButton = (Button) findViewById(R.id.signInWithStoredButton);

        final Context context = this;
        mSignInWithStoredButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuthTask = new UserLoginTask(context, mToken);
                mAuthTask.execute((Void) null);
            }
        });

        if(mToken != null) {
            showProgress(true);
            mSignInWithStoredButton.setVisibility(View.VISIBLE);
            mAuthTask = new UserLoginTask(this, mToken);
            mAuthTask.execute((Void) null);
        } else {
            mSignInWithStoredButton.setVisibility(View.INVISIBLE);
        }

        // Adding social-media account linking support, starting with Twitter
        Twitter.initialize(this);


        // First app-use handling
        sharedPref = getSharedPreferences(getString(R.string.getting_started_preferences_key), Context.MODE_PRIVATE);
        if(!sharedPref.getBoolean("gettingStarted", false)) {
            View loginForm = findViewById(R.id.login_form);
            loginForm.setVisibility(View.GONE);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            GettingStartedFragment f = GettingStartedFragment.newInstance(
                    getString(R.string.text_getting_started), "gettingStarted");
            ft.replace(R.id.loginActivityLayout, f, "fragment_gettingStartedLayout").addToBackStack(null).commit();
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid homeserver, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mHomeserverView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt. and reset authentication token
        mToken = null;
        String homeserverAddr = mHomeserverView.getText().toString();
        String account = mAccountNameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid address.
        if (TextUtils.isEmpty(homeserverAddr)) {
            mHomeserverView.setError(getString(R.string.error_field_required));
            focusView = mHomeserverView;
            cancel = true;
        } else if (!DistortAuthParams.isAddressValid(homeserverAddr)) {
            mHomeserverView.setError(getString(R.string.error_invalid_address));
            focusView = mHomeserverView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(this, homeserverAddr, password, account);
            mAuthTask.execute((Void) null);
        }
    }

    @Override
    protected void onStart() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.account_credentials_preferences_key), Context.MODE_PRIVATE);
        mToken = sharedPref.getString(EXTRA_CREDENTIAL, null);
        if(mToken != null) {
            mHomeserverView.setText(sharedPref.getString(EXTRA_HOMESERVER, ""));
            mAccountNameView.setText(sharedPref.getString(EXTRA_ACCOUNT_NAME, ""));
            mSignInWithStoredButton.setVisibility(View.VISIBLE);
        } else {
            mSignInWithStoredButton.setVisibility(View.INVISIBLE);
        }

        super.onStart();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public void LaunchMainApplication(DistortAuthParams authParams) {
        Context ctx = getApplicationContext();
        mPasswordView.setText("");

        // Save login  credentials for later ease
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                getString(R.string.account_credentials_preferences_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor preferenceEditor = sharedPref.edit();
        preferenceEditor.putString(EXTRA_HOMESERVER, authParams.getHomeserverAddress());
        preferenceEditor.putString(EXTRA_HOMESERVER_PROTOCOL, authParams.getHomeserverProtocol());
        preferenceEditor.putString(EXTRA_PEER_ID, authParams.getPeerId());
        preferenceEditor.putString(EXTRA_ACCOUNT_NAME, authParams.getAccountName());
        preferenceEditor.putString(EXTRA_CREDENTIAL, authParams.getCredential());
        preferenceEditor.commit();

        // Create background tasks (and add to alarm if not already running)
        Context appContext = getApplicationContext();
        Intent intent;
        PendingIntent pendingIntent;
        Long timeInterval;
        AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        // Create account,groups,peers task
        timeInterval = AlarmManager.INTERVAL_HALF_HOUR;
        intent = new Intent(appContext, DistortBackgroundService.class);
        intent.setAction(DistortBackgroundService.ACTION_SCHEDULE_SECONDARY_SERVICES);
        pendingIntent = PendingIntent.getService(appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + timeInterval, timeInterval, pendingIntent);
        DistortBackgroundService.startActionScheduleSecondaryServices(appContext);

        // Create conversations,messages task, higher frequency
        timeInterval = 180000L;
        intent = new Intent(appContext, DistortBackgroundService.class);
        intent.setAction(DistortBackgroundService.ACTION_SCHEDULE_PRIMARY_SERVICES);
        pendingIntent = PendingIntent.getService(appContext, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + timeInterval, timeInterval, pendingIntent);
        DistortBackgroundService.startActionSchedulePrimaryServices(appContext);

        intent = new Intent(ctx, GroupsActivity.class);
        startActivity(intent);
    }

    // Allow receiving finished account creation form
    public void OnAccountCreationFinished(@Nullable DistortAuthParams account) {
        View loginForm = findViewById(R.id.login_form);
        loginForm.setVisibility(View.VISIBLE);

        if(account != null) {
            LaunchMainApplication(account);
        }
    }

    // Receive closing of getting-started window
    public void OnGettingStartedClose() {
        View loginForm = findViewById(R.id.login_form);
        loginForm.setVisibility(View.VISIBLE);
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        private final Context mContext;
        private String mPassword;
        private String mAddress;
        private String mProtocol;
        private String mPeerId;
        private String mAccountName;
        private String mToken;
        private String mErrorString;

        DistortAuthParams mAuthParams;

        // Error codes:
        // 0 - success
        // -1 - homeserver issue
        // -2 - password issue
        // -2 - account name issue
        private int mErrorCode;

        UserLoginTask(Context ctx, String address, String password, String account) {
            mContext = ctx;
            mAddress = address;

            // Require that mAddress ends in '/' for consistency
            if(mAddress.charAt(mAddress.length()-1) != '/') {
                mAddress += "/";
            }

            mAccountName = account;
            mPassword = password;

            mErrorString = "";
            mErrorCode = 0;
        }
        UserLoginTask(Context ctx, String token) {
            mContext = ctx;
            mToken = token;

            mErrorString = "";
            mErrorCode = 0;
        }

        private DistortAuthParams generateTokenFromFields() throws DistortJson.DistortException, IOException {
            DistortAuthParams authParams = new DistortAuthParams();
            authParams.setHomeserverAddress(mAddress);

            if(mAccountName != null && mAccountName.length() > 0) {
                authParams.setAccountName(mAccountName);
            } else {
                authParams.setAccountName("root");
            }

            // First discover IPFS Peer ID
            String ipfsURL = mAddress + "ipfs";

            URL homeserverEndpoint = new URL(ipfsURL);
            Matcher matcher = DistortAuthParams.IS_ADDRESS_PATTERN.matcher(mAddress);
            matcher.find();

            DistortJson.ResponseString response;
            if (DistortAuthParams.PROTOCOL_HTTPS.equals(matcher.group(1))) {
                mProtocol = DistortAuthParams.PROTOCOL_HTTPS;
                authParams.setHomeserverProtocol(mProtocol);

                HttpsURLConnection myConnection;
                myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getMessageStringFromURL(myConnection, null);
            } else {
                mProtocol = DistortAuthParams.PROTOCOL_HTTP;
                authParams.setHomeserverProtocol(mProtocol);

                HttpURLConnection myConnection;
                myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getMessageStringFromURL(myConnection, null);
            }

            mPeerId = response.mResponse;
            authParams.setPeerId(mPeerId);

            // Attempt to generate token from password
            PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
            generator.init(mPassword.getBytes(StandardCharsets.UTF_8), mPeerId.getBytes(StandardCharsets.UTF_8), 1000);
            KeyParameter passwordBasedKey = (KeyParameter)generator.generateDerivedMacParameters(256);
            mToken = new String(Base64.encode(passwordBasedKey.getKey()), Charset.forName("UTF-8"));
            authParams.setCredential(mToken);

            Log.d("LOGIN-AUTH", mToken);
            return authParams;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorString = "";
            mErrorCode = 0;
            mAuthParams = null;

            // Attempt authentication against a network service.
            try {
                if(mToken == null || mToken.length() == 0) {
                    try {
                        mAuthParams = generateTokenFromFields();
                    } catch (MalformedURLException e) {
                        // Since we assured URL is valid with regex, only occurs if DNS resolution fails
                        mErrorString = getString(R.string.error_resolve_address);
                        mErrorCode = -1;
                        return false;
                    } catch (DistortJson.DistortException e) {
                        if(e.getResponseCode() == 500) {
                            mErrorString = getString(R.string.error_server_error);
                        } else {
                            mErrorString = e.getMessage();
                        }
                        mErrorCode = -1;
                        return false;
                    } catch (IOException e) {
                        mErrorString = e.getMessage();
                        mErrorCode = -1;
                        return false;
                    }
                } else {
                    SharedPreferences sharedPref = mContext.getSharedPreferences(
                            getString(R.string.account_credentials_preferences_key), Context.MODE_PRIVATE);
                    mAddress = sharedPref.getString(EXTRA_HOMESERVER, null);
                    mProtocol = sharedPref.getString(EXTRA_HOMESERVER_PROTOCOL, null);
                    mPeerId = sharedPref.getString(EXTRA_PEER_ID, null);
                    mAccountName = sharedPref.getString(EXTRA_ACCOUNT_NAME, "root");

                    mAuthParams = new DistortAuthParams(mAddress, mProtocol, mPeerId, mAccountName, mToken);
                }

                // Create string to test authentication parameters
                String loginURL = mAddress + "account";

                URL homeserverEndpoint = new URL(loginURL);
                if(DistortAuthParams.PROTOCOL_HTTPS.equals(mProtocol)) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    DistortJson.getJSONFromURL(myConnection, mAuthParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    DistortJson.getJSONFromURL(myConnection, mAuthParams);
                }

                // Return true only if authentication was successful
                return true;
            } catch (DistortJson.DistortException e) {
                if(e.getResponseCode() == 401) {
                    Log.d("LOGIN", e.getMessage());
                    mErrorString = getString(R.string.error_incorrect_password);
                    mErrorCode = -2;
                } else if(e.getResponseCode() == 404) {
                    Log.d("LOGIN", e.getMessage());
                    mErrorString = getString(R.string.error_invalid_account);
                    mErrorCode = -3;
                } else if(e.getResponseCode() == 500) {
                    Log.d("LOGIN", e.getMessage());
                    mErrorString = getString(R.string.error_server_error);
                    mErrorCode = -1;
                } else {
                    mErrorString = e.getMessage();
                    mErrorCode = -1;
                }

                return false;
            } catch (MalformedURLException e) {
                // Since we assured URL is valid with regex, only occurs if DNS resolution fails
                mErrorString = getString(R.string.error_resolve_address);
                mErrorCode = -1;

                return false;
            } catch (IOException e) {
                mErrorString = e.getMessage();
                mErrorCode = -1;

                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                LaunchMainApplication(mAuthParams);
            } else {
                // To first letter uppercase
                mErrorString = mErrorString.substring(0, 1).toUpperCase() + mErrorString.substring(1);

                // Display error
                if(mErrorCode == -1) {
                    mHomeserverView.setError(mErrorString);
                    mHomeserverView.requestFocus();
                } else if(mErrorCode == -2) {
                    mPasswordView.setError(mErrorString);
                    mPasswordView.requestFocus();
                } else if(mErrorCode == -3) {
                    mAccountNameView.setError(mErrorString);
                    mAccountNameView.requestFocus();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

