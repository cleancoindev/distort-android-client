package com.unix4all.rypi.distort;

import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Class representing a group
public class DistortConversation {
    private @Nullable String mId;
    private final String mGroupId;
    private String mPeerId;
    private String mAccountName;
    private @Nullable String mFriendlyName;
    private Integer mHeight;
    private Date mLatestStatusChangeDate;

    protected static final SimpleDateFormat mongoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    DistortConversation(@Nullable String id, String groupId, String peerId, String accountName, Integer height, String jsLatestChangeDate) {
        mId = id;
        mGroupId = groupId;
        mPeerId = peerId;
        mAccountName = accountName;
        mHeight = height;
        try {
            mLatestStatusChangeDate = mongoDateFormat.parse(jsLatestChangeDate);
        } catch (ParseException e) {
            mLatestStatusChangeDate = new Date(0);
        }
    }
    DistortConversation(@Nullable String id, String groupId, String peerId, String accountName, Integer height, Date latestChangeDate) {
        mId = id;
        mGroupId = groupId;
        mPeerId = peerId;
        mAccountName = accountName;
        mHeight = height;
        mLatestStatusChangeDate = latestChangeDate;
    }

    // Getters
    public @Nullable String getId() {
        return mId;
    }
    public String getGroupId() {
        return mGroupId;
    }
    public String getPeerId() {
        return mPeerId;
    }
    public String getAccountName() {
        return mAccountName;
    }
    public Integer getHeight() {
        return mHeight;
    }
    public String getFriendlyName() {
        if(mFriendlyName != null && !mFriendlyName.isEmpty()) {
            return mFriendlyName;
        }
        if(mAccountName == null || mAccountName.isEmpty() || mAccountName.equals("root")) {
            return mPeerId;
        }
        return mAccountName;
    }
    public @Nullable String getPlainFriendlyName() {
        return mFriendlyName;
    }
    public String getFullAddress() {
        return DistortPeer.toFullAddress(mPeerId, mAccountName);
    }
    public Date getLatestStatusChangeDate() {
        return mLatestStatusChangeDate;
    }

    // Setters
    public void setId(@Nullable String id) {
        if(id != null) {
            mId = id;
        }
    }
    public void setPeerId(String peerId) {
        mPeerId = peerId;
    }
    public void setAccountName(String accountName) {
        mAccountName = accountName;
    }
    public void setFriendlyName(@Nullable String friendlyName) {
        if(friendlyName != null) {
            mFriendlyName = friendlyName;
        }
    }
    public void setHeight(Integer height) {
        mHeight = height;
    }
    public void setLatestStatusChangeDate(Date latestStatusChangeDate) {
        mLatestStatusChangeDate = latestStatusChangeDate;
    }
    public void setLatestStatusChangeDate(String jsLatestChangeDate) {
        try {
            mLatestStatusChangeDate = mongoDateFormat.parse(jsLatestChangeDate);
        } catch (ParseException e) {
            mLatestStatusChangeDate = new Date(0);
        }
    }

    // Static parsing function
    static DistortConversation readConversationJson(JsonReader json, @Nullable String groupDatabaseId) throws IOException {
        String id = null;
        String groupId = groupDatabaseId;
        String peerId = null;
        String accountName = null;
        Integer height = null;
        Date latestChangeDate = null;

        // Read all fields from group
        json.beginObject();
        while(json.hasNext()) {
            String key = json.nextName();
            if(key.equals("_id")) {
                id = json.nextString();
            } else if(key.equals("group")) {
                groupId = json.nextString();
            } else if(key.equals("height")) {
                height = json.nextInt();
            } else if(key.equals("peerId")) {
                peerId = json.nextString();
            } else if(key.equals("accountName")) {
                accountName = json.nextString();
            } else if(key.equals("latestStatusChangeDate")) {
                try {
                    latestChangeDate = mongoDateFormat.parse(json.nextString());
                } catch (ParseException e) {
                    latestChangeDate = new Date(0);
                }
            } else {
                json.skipValue();
            }
        }
        json.endObject();

        if(groupId != null && peerId != null && accountName != null && height != null && latestChangeDate != null) {
            Log.d("READ-CONVERSATION", "Conversation ( " + groupId + "," + peerId + "," + accountName + "," + height+ "," + mongoDateFormat.format(latestChangeDate) + " )");
            return new DistortConversation(id, groupId, peerId, accountName, height, latestChangeDate);
        } else {
            throw new IOException();
        }
    }

    // Write this object to JSON
    public void writeConversationJson(JsonWriter json) throws IOException {
        // Read all fields from group
        json.beginObject();
        if(mId != null) {
            json.name("_id").value(mId);
        }
        json.name("group").value(mGroupId);
        json.name("peerId").value(mPeerId);
        json.name("accountName").value(mAccountName);
        json.name("height").value(mHeight);
        json.name("latestStatusChangeDate").value(mongoDateFormat.format(mLatestStatusChangeDate));
        json.endObject();
    }
}