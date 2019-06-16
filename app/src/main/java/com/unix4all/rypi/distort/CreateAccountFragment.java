package com.unix4all.rypi.distort;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

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
import java.util.HashMap;
import java.util.regex.Matcher;

import javax.net.ssl.HttpsURLConnection;


public class CreateAccountFragment extends Fragment {

    private Button mCreateAccount;
    private Button mScanQrCode;
    private EditText mHomeserverEdit;
    private EditText mAccountNameEdit;
    private EditText mPasswordEdit;
    private EditText mCreationTokenEdit;
    private DistortAuthParams mAuthParams;

    private IntentIntegrator qrCodeScan;
    private AccountCreationTask mAccountCreationTask;

    private OnAccountCreationListener mCreationListener;

    public interface OnAccountCreationListener {
        void OnAccountCreationFinished(@Nullable DistortAuthParams account);
    }

    public CreateAccountFragment() {
        // Required empty public constructor
    }

    public static CreateAccountFragment newInstance(String homeserver, String accountName, String password) {
        CreateAccountFragment caf = new CreateAccountFragment();
        Bundle b = new Bundle();
        b.putString("homeserver", homeserver);
        b.putString("accountName", accountName);
        b.putString("password", password);
        caf.setArguments(b);
        return caf;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        qrCodeScan = new IntentIntegrator(getActivity());

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_account, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuthParams = null;
        Bundle b = this.getArguments();

        mCreateAccount = view.findViewById(R.id.createAccountButton);
        mCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String homeserver = mHomeserverEdit.getText().toString();
                String accountName = mAccountNameEdit.getText().toString();
                String password = mPasswordEdit.getText().toString();
                String creationToken = mCreationTokenEdit.getText().toString();

                String errorStr = null;
                EditText errorView = null;
                if(homeserver.isEmpty()) {
                    errorView = mHomeserverEdit;
                    errorStr = getString(R.string.error_field_required);
                } else if(!DistortAuthParams.isAddressValid(homeserver)) {
                    errorView = mHomeserverEdit;
                    errorStr = getString(R.string.error_invalid_address);
                } else if(accountName.isEmpty()) {
                    errorView = mAccountNameEdit;
                    errorStr = getString(R.string.error_field_required);
                } else if(password.isEmpty()) {
                    errorView = mPasswordEdit;
                    errorStr = getString(R.string.error_field_required);
                } else if(creationToken.isEmpty()) {
                    errorView = mCreationTokenEdit;
                    errorStr = getString(R.string.error_field_required);
                }

                if(errorView != null) {
                    errorView.requestFocus();
                    errorView.setError(errorStr);

                    mAuthParams = null;
                } else {
                    mAccountCreationTask = new AccountCreationTask(getActivity(), homeserver, accountName, password, creationToken);
                    mAccountCreationTask.execute();
                }
            }
        });

        mScanQrCode = view.findViewById(R.id.createAccountQrButton);
        mScanQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrCodeScan.forSupportFragment(CreateAccountFragment.this).initiateScan();
            }
        });

        mHomeserverEdit = view.findViewById(R.id.createAccountHomeserver);
        mHomeserverEdit.setText(b.getString("homeserver", ""));
        mHomeserverEdit.requestFocus();

        mAccountNameEdit = view.findViewById(R.id.createAccountName);
        mAccountNameEdit.setText(b.getString("accountName", ""));
        mPasswordEdit = view.findViewById(R.id.createAccountPassword);
        mPasswordEdit.setText(b.getString("password", ""));
        mCreationTokenEdit = view.findViewById(R.id.createAccountToken);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnAccountCreationListener) {
            mCreationListener = (OnAccountCreationListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAccountCreationListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCreationListener.OnAccountCreationFinished(mAuthParams);
        mCreationListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(result != null && result.getContents() != null) {
            mCreationTokenEdit.setText(result.getContents());
        }
    }

    // Determine peer account in use and send account-creation request
    private class AccountCreationTask extends AsyncTask<Void, Void, Boolean> {
        private final Context mContext;
        private String mAddress;
        private String mAccountName;
        private String mPassword;
        private String mCreationToken;

        // Return values
        private String mPeerId;
        private String mProtocol;
        private String mAuthToken;

        // Error code: int identifying input to set error to
        // -1 : address
        // -2 : account
        // -3 : password??
        // -4 : creation token
        private int mErrorCode = 0;
        private String mErrorString;

        AccountCreationTask(Context ctx, String address, String account, String password, String creationToken) {
            mContext = ctx;
            mAddress = address;
            mAccountName = account;
            mPassword = password;
            mCreationToken = creationToken;

            // Require that mAddress ends in '/' for consistency
            if(mAddress.charAt(mAddress.length()-1) != '/') {
                mAddress += "/";
            }

            mErrorString = "";
        }

        private String getPeerIdAndAuthToken() throws DistortJson.DistortException, IOException {
            // Discover IPFS Peer ID
            String ipfsURL = mAddress + "ipfs";

            URL homeserverEndpoint = new URL(ipfsURL);
            Matcher matcher = DistortAuthParams.IS_ADDRESS_PATTERN.matcher(mAddress);
            matcher.find();

            DistortJson.ResponseString response;
            if (DistortAuthParams.PROTOCOL_HTTPS.equals(matcher.group(1))) {
                mProtocol = DistortAuthParams.PROTOCOL_HTTPS;

                HttpsURLConnection myConnection;
                myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getMessageStringFromURL(myConnection, null);
            } else {
                mProtocol = DistortAuthParams.PROTOCOL_HTTP;

                HttpURLConnection myConnection;
                myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                response = DistortJson.getMessageStringFromURL(myConnection, null);
            }
            mPeerId = response.mResponse;

            // Attempt to generate token from password
            PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
            generator.init(mPassword.getBytes(StandardCharsets.UTF_8), mPeerId.getBytes(StandardCharsets.UTF_8), 1000);
            KeyParameter passwordBasedKey = (KeyParameter)generator.generateDerivedMacParameters(256);
            mAuthToken = new String(Base64.encode(passwordBasedKey.getKey()), Charset.forName("UTF-8"));

            Log.d("ACCOUNT-CREATION", "IPFS identity: " + response.mResponse);
            Log.d("ACCOUNT-CREATION", "Authentication token: " + response.mResponse);
            return response.mResponse;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorString = "";

            // Attempt authentication against a network service.
            try {
                try {
                    getPeerIdAndAuthToken();
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

                // Create account URL and request body parameters
                String createAccountUrl = mAddress + "create-account";
                HashMap<String, String> bodyParams = new HashMap<>();
                bodyParams.put("peerId", mPeerId);
                bodyParams.put("accountName", mAccountName);
                bodyParams.put("authToken", mAuthToken);
                bodyParams.put("signature", mCreationToken);

                URL homeserverEndpoint = new URL(createAccountUrl);
                if(DistortAuthParams.PROTOCOL_HTTPS.equals(mProtocol)) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    DistortJson.postBodyGetJSONFromURL(myConnection, null, bodyParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    DistortJson.postBodyGetJSONFromURL(myConnection, null, bodyParams);
                }

                mAuthParams = new DistortAuthParams(mAddress, mProtocol, mPeerId, mAccountName, mAuthToken);

                // Return true only if authentication was successful
                return true;
            } catch (DistortJson.DistortException e) {
                switch(e.getResponseCode()) {
                    case 400:
                        // Account exists
                        Log.d("ACCOUNT-CREATION", e.getMessage());
                        mErrorString = getString(R.string.error_account_already_exists);
                        mErrorCode = -2;
                        break;
                    case 401:
                        // Invalid signature/creation-token
                        Log.d("ACCOUNT-CREATION", e.getMessage());
                        mErrorString = getString(R.string.error_invalid_signature);
                        mErrorCode = -4;
                        break;
                    case 404:
                        // No root accounts for identity peerId
                        Log.d("ACCOUNT-CREATION", e.getMessage());
                        mErrorString = getString(R.string.error_no_accounts) + ":" + mPeerId;
                        mErrorCode = -1;
                        break;
                    case 500:
                        // Internal error
                        Log.d("ACCOUNT-CREATION", e.getMessage());
                        mErrorString = getString(R.string.error_server_error);
                        mErrorCode = -1;
                        break;
                    default:
                        mErrorString = e.getMessage();
                        mErrorCode = -1;
                }

                return false;
            } catch (MalformedURLException e) {
                // Since we assured URL is valid with regex, only occurs if DNS resolution fails
                mErrorString = getString(R.string.error_resolve_address);

                return false;
            } catch (IOException e) {
                mErrorString = e.getMessage();

                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAccountCreationTask = null;

            if (success) {
                getActivity().onBackPressed();
            } else {
                // To first letter uppercase
                mErrorString = mErrorString.substring(0, 1).toUpperCase() + mErrorString.substring(1);

                // Display error
                switch(mErrorCode) {
                    case -1:
                        mHomeserverEdit.setError(mErrorString);
                        mHomeserverEdit.requestFocus();
                        break;
                    case -2:
                        mAccountNameEdit.setError(mErrorString);
                        mAccountNameEdit.requestFocus();
                        break;
                    case -3:
                        mPasswordEdit.setError(mErrorString);
                        mPasswordEdit.requestFocus();
                        break;
                    case -4:
                        mCreationTokenEdit.setError(mErrorString);
                        mCreationTokenEdit.requestFocus();
                        break;
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAccountCreationTask = null;
        }
    }
}
