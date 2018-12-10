package com.unix4all.rypi.distort;

import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;

public class DistortAccount {
    private String mId;
    private String mPeerId;
    private String mAccountName;
    private Boolean mEnabled;
    private @Nullable String mActiveGroupId;

    DistortAccount(String id, String peerId, String accountName, Boolean enabled, String activeGroupId) {
        mId = id;
        mPeerId = peerId;
        mAccountName = accountName;
        mEnabled = enabled;
        mActiveGroupId = activeGroupId;
    }

    public String getId() {
        return mId;
    }
    public String getPeerId() {
        return mPeerId;
    }
    public String getAccountName() {
        return mAccountName;
    }
    public Boolean getEnabled() {
        return mEnabled;
    }
    @Nullable
    public String getActiveGroupId() {
        return mActiveGroupId;
    }

    public void setId(String id) {
        mId = id;
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
    public void setActiveGroupId(@Nullable String activeGroupId) {
        mActiveGroupId = activeGroupId;
    }

    public static DistortAccount readJson(JsonReader reader) throws IOException {
        String id = null;
        String accountName = null;
        String activeGroupId = null;
        Boolean enabled = null;
        String peerId = null;

        // Read all fields from account
        reader.beginObject();
        while(reader.hasNext()) {
            String key = reader.nextName();
            if(key.equals("_id")) {
                id = reader.nextString();
            } else if(key.equals("accountName")) {
                accountName = reader.nextString();
            } else if(key.equals("activeGroup")) {
                if(reader.peek().equals(JsonToken.BEGIN_OBJECT)) {
                    activeGroupId = DistortGroup.readJson(reader).getId();
                } else {
                    activeGroupId = reader.nextString();
                }
            } else if(key.equals("enabled")) {
                enabled = reader.nextBoolean();
            } else if(key.equals("peerId")) {
                peerId = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        if(id != null && accountName != null && enabled != null && peerId != null) {
            String activeGroupStr = "";
            if (activeGroupId != null) {
                activeGroupStr += "," + activeGroupId;
            }
            Log.d("READ-ACCOUNT", "Account ( " + id + "," + peerId + "," + accountName + "," + enabled + activeGroupStr + " )");

            return new DistortAccount(id, peerId, accountName, enabled, activeGroupId);
        } else {
            throw new IOException("Missing parameters");
        }
    }


    // Write this object to JSON
    public void writeJson(JsonWriter json) throws IOException {
        // Read all fields from group
        json.beginObject();
        json.name("_id").value(mId);
        json.name("peerId").value(mPeerId);
        json.name("accountName").value(mAccountName);
        json.name("enabled").value(mEnabled);
        if(mActiveGroupId != null && !mActiveGroupId.isEmpty()) {
            json.name("activeGroup").value(mActiveGroupId);
        }
        json.endObject();
    }
}