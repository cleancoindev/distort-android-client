package com.unix4all.rypi.distort;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.regex.Pattern;

public class DistortAuthParams {
    // Protocol strings for connecting to homeserver
    public static final String PROTOCOL_HTTPS = "https://";
    public static final String PROTOCOL_HTTP = "http://";

    // Pass login address and credentials to between activities
    public static final String EXTRA_HOMESERVER = "com.unix4all.rypi.distort.HOMESERVER";
    public static final String EXTRA_HOMESERVER_PROTOCOL = "com.unix4all.rypi.distort.HOMESERVER_PROTOCOL";
    public static final String EXTRA_CREDENTIAL = "com.unix4all.rypi.distort.CREDENTIAL";
    public static final String EXTRA_PEER_ID = "com.unix4all.rypi.distort.PEER_ID";
    public static final String EXTRA_ACCOUNT_NAME = "com.unix4all.rypi.distort.ACCOUNT_NAME";

    // Regex Pattern to identify if a string is a valid homeserver address, as well as fetch relevant substring from the URL
    public static final Pattern IS_ADDRESS_PATTERN = Pattern.compile("^(http(s)?://)?(([a-zA-Z0-9.-]+\\.[a-z]+)|((0*[0-9]|0*[1-9][0-9]|0*1[0-9][0-9]|0*2[0-4][0-9]|0*25[0-5])\\.){3}(0*[0-9]|0*[1-9][0-9]|0*1[0-9][0-9]|0*2[0-4][0-9]|0*25[0-5]))(:[0-9]*)?(/[a-zA-Z0-9%/.-]*)?$");

    // Private members
    private String mHomeserverAddress;
    private String mHomeserverProtocol;
    private String mPeerId;
    private String mAccountName;
    private String mCredential;

    // Constructors
    DistortAuthParams() {
        mHomeserverAddress = "";
        mHomeserverProtocol = "";
        mPeerId = "";
        mAccountName = "";
        mCredential = "";
    }
    DistortAuthParams(String homeserverAddress, String homeserverProtocol, String peerId, String accountName, String credential) {
        mHomeserverAddress = homeserverAddress;
        mHomeserverProtocol = homeserverProtocol;
        mPeerId = peerId;
        mAccountName = accountName;
        mCredential = credential;
    }

    public static boolean isAddressValid(String address) {
        return IS_ADDRESS_PATTERN.matcher(address).matches();
    }

    public static DistortAuthParams getAuthenticationParams(Context ctx) {
        // Use shared preferences to fetch authorization params
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.account_credentials_preferences_key), Context.MODE_PRIVATE);

        DistortAuthParams loginParams = new DistortAuthParams();
        loginParams.setHomeserverAddress(sharedPref.getString(DistortAuthParams.EXTRA_HOMESERVER, null));
        loginParams.setHomeserverProtocol(sharedPref.getString(DistortAuthParams.EXTRA_HOMESERVER_PROTOCOL, null));
        loginParams.setPeerId(sharedPref.getString(DistortAuthParams.EXTRA_PEER_ID, null));
        loginParams.setAccountName(sharedPref.getString(DistortAuthParams.EXTRA_ACCOUNT_NAME, "root"));
        loginParams.setCredential(sharedPref.getString(DistortAuthParams.EXTRA_CREDENTIAL, null));

        return loginParams;
    }

    // Getters
    public String getHomeserverAddress() {
        return mHomeserverAddress;
    }
    public String getHomeserverProtocol() {
        return mHomeserverProtocol;
    }
    public String getPeerId() {
        return mPeerId;
    }
    public String getAccountName() {
        return mAccountName;
    }
    public String getFullAddress() {
        return DistortPeer.toFullAddress(mPeerId, mAccountName);
    }
    public String getCredential() {
        return mCredential;
    }

    // Setters
    public void setHomeserverAddress(String homeserverAddress) {
        mHomeserverAddress = homeserverAddress;
    }
    public void setHomeserverProtocol(String homeserverProtocol) {
        mHomeserverProtocol = homeserverProtocol;
    }
    public void setPeerId(String peerId) {
        mPeerId = peerId;
    }
    public void setAccountName(String accountName) {
        mAccountName = accountName;
    }
    public void setCredential(String credential) {
        mCredential = credential;
    }
}
