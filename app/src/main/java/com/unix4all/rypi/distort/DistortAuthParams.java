package com.unix4all.rypi.distort;

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
