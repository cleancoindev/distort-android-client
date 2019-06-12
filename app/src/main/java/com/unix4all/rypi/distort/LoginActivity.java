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

import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Base64;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_ACCOUNT_NAME;
import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_CREDENTIAL;
import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_HOMESERVER;
import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_HOMESERVER_PROTOCOL;
import static com.unix4all.rypi.distort.DistortAuthParams.EXTRA_PEER_ID;

/**
 * A login screen that offers login via homeserver / password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Regex Pattern to identify if a string is a valid homeserver address, as well as
     * fetch relevant substring from the URL.
     */
    private static final Pattern IS_ADDRESS_PATTERN = Pattern.compile("^(http(s)?://)?(([a-zA-Z0-9.-]+\\.[a-z]+)|((0*[0-9]|0*[1-9][0-9]|0*1[0-9][0-9]|0*2[0-4][0-9]|0*25[0-5])\\.){3}(0*[0-9]|0*[1-9][0-9]|0*1[0-9][0-9]|0*2[0-4][0-9]|0*25[0-5]))(:[0-9]*)?(/[a-zA-Z0-9%/.-]*)?$");


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
        } else if (!isAddressValid(homeserverAddr)) {
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

    private boolean isAddressValid(String address) {
        return IS_ADDRESS_PATTERN.matcher(address).matches();
    }

    @Override
    protected void onResume() {
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

        super.onResume();
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
        private String mAccount;
        private String mToken;
        private String mErrorString;

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

            mAccount = account;
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

            if(mAccount != null && mAccount.length() > 0) {
                authParams.setAccountName(mAccount);
            }

            // First discover IPFS Peer ID
            String ipfsURL = mAddress + "ipfs";

            URL homeserverEndpoint = new URL(ipfsURL);
            Matcher matcher = IS_ADDRESS_PATTERN.matcher(mAddress);
            matcher.find();

            DistortJson.ResponseString response;
            if (DistortAuthParams.PROTOCOL_HTTPS.equals(matcher.group(1))) {
                mProtocol = DistortAuthParams.PROTOCOL_HTTPS;
                authParams.setHomeserverProtocol(mProtocol);

                HttpsURLConnection myConnection;
                myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.GetMessageStringFromURL(myConnection, authParams);
            } else {
                mProtocol = DistortAuthParams.PROTOCOL_HTTP;
                authParams.setHomeserverProtocol(mProtocol);

                HttpURLConnection myConnection;
                myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.GetMessageStringFromURL(myConnection, authParams);
            }

            mPeerId = response.mResponse;
            authParams.setPeerId(mPeerId);

            // Attempt to generate token from password
            PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
            generator.init(mPassword.getBytes(), mPeerId.getBytes(), 1000);
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
            DistortAuthParams authParams;

            // Attempt authentication against a network service.
            try {
                if(mToken == null || mToken.length() == 0) {
                    try {
                        authParams = generateTokenFromFields();
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
                    mAccount = sharedPref.getString(EXTRA_ACCOUNT_NAME, null);

                    authParams = new DistortAuthParams(mAddress, mProtocol, mPeerId, mAccount, mToken);
                }

                // Create string to test authentication parameters
                String loginURL = mAddress + "groups";

                URL homeserverEndpoint = new URL(loginURL);
                if(DistortAuthParams.PROTOCOL_HTTPS.equals(mProtocol)) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    DistortJson.GetJSONFromURL(myConnection, authParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    DistortJson.GetJSONFromURL(myConnection, authParams);
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
                // Save login  credentials for later ease
                SharedPreferences sharedPref = mContext.getSharedPreferences(
                        getString(R.string.account_credentials_preferences_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor preferenceEditor = sharedPref.edit();
                preferenceEditor.putString(EXTRA_HOMESERVER, mAddress);
                preferenceEditor.putString(EXTRA_HOMESERVER_PROTOCOL, mProtocol);
                preferenceEditor.putString(EXTRA_PEER_ID, mPeerId);
                preferenceEditor.putString(EXTRA_ACCOUNT_NAME, mAccount);
                preferenceEditor.putString(EXTRA_CREDENTIAL, mToken);
                preferenceEditor.commit();

                // Create background tasks (and add to alarm if not already running)
                Context appContext = getApplicationContext();
                Intent intent;
                PendingIntent pendingIntent;
                Long timeInterval;
                AlarmManager alarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

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

                intent = new Intent(mContext, GroupsActivity.class);
                startActivity(intent);
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

