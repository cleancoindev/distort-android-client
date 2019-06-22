package com.unix4all.rypi.distort;

import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;

public class DistortAccount {
    private String mPeerId;
    private String mAccountName;
    private Boolean mEnabled;
    private @Nullable String mActiveGroup;

    DistortAccount(String peerId, String accountName, Boolean enabled, @Nullable String activeGroup) {
        mPeerId = peerId;
        mAccountName = accountName;
        mEnabled = enabled;
        mActiveGroup = activeGroup;
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
    public Boolean getEnabled() {
        return mEnabled;
    }
    @Nullable
    public String getActiveGroup() {
        return mActiveGroup;
    }

    public void setPeerId(String peerId) {
        mPeerId = peerId;
    }
    public void setAccountName(String accountName) {
        mAccountName = accountName;
    }
    public void setEnabled(Boolean enabled) {
        mEnabled = enabled;
    }
    public void setActiveGroup(@Nullable String activeGroup) {
        mActiveGroup = activeGroup;
    }

    public static DistortAccount readJson(JsonReader reader) throws IOException {
        String accountName = null;
        String activeGroup = null;
        Boolean enabled = null;
        String peerId = null;

        // Read all fields from account
        reader.beginObject();
        while(reader.hasNext()) {
            String key = reader.nextName();
            if(key.equals("accountName")) {
                accountName = reader.nextString();
            } else if(key.equals("activeGroup")) {
                activeGroup = reader.nextString();
            } else if(key.equals("enabled")) {
                enabled = reader.nextBoolean();
            } else if(key.equals("peerId")) {
                peerId = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        if(accountName != null && enabled != null && peerId != null) {
            String activeGroupStr = "";
            if (activeGroup != null) {
                activeGroupStr += "," + activeGroup;
            }
            Log.d("READ-ACCOUNT", "Account ( " + peerId + "," + accountName + "," + enabled + activeGroupStr + " )");

            return new DistortAccount(peerId, accountName, enabled, activeGroup);
        } else {
            throw new IOException("Missing account parameters");
        }
    }


    // Write this object to JSON
    public void writeJson(JsonWriter json) throws IOException {
        // Write all fields from account
        json.beginObject();
        json.name("peerId").value(mPeerId);
        json.name("accountName").value(mAccountName);
        json.name("enabled").value(mEnabled);
        if(mActiveGroup != null && !mActiveGroup.isEmpty()) {
            json.name("activeGroup").value(mActiveGroup);
        }
        json.endObject();
    }
}