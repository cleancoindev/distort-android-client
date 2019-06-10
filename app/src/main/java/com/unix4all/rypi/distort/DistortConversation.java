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
    private @Nullable String mNickname;
    private Integer mHeight;
    private @Nullable Date mLatestStatusChangeDate;

    protected static final SimpleDateFormat mongoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    DistortConversation(@Nullable String id, String groupId, String peerId, String accountName, Integer height, @Nullable Date latestChangeDate) {
        mId = id;
        mGroupId = groupId;
        mPeerId = peerId;
        mAccountName = accountName;
        mHeight = height;
        mLatestStatusChangeDate = latestChangeDate;
    }

    public static String toUniqueLabel(String groupId, String peerFullAddress) {
        return groupId + ":" + peerFullAddress;
    }
    public static String toUniqueLabel(String groupId, String peerFullAddress, String[] args) {
        String r = groupId + ":" + peerFullAddress;
        for(int i = 0; i < args.length; i++) {
            r += ":" + args[i];
        }
        return r;
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
        if(mNickname != null && !mNickname.isEmpty()) {
            return mNickname;
        } else if(!mAccountName.isEmpty() && !mAccountName.equals("root")) {
            return mAccountName;
        } else {
            return getFullAddress();
        }
    }
    public @Nullable String getNickname() {
        return mNickname;
    }
    public String getFullAddress() {
        return DistortPeer.toFullAddress(mPeerId, mAccountName);
    }
    public @Nullable Date getLatestStatusChangeDate() {
        return mLatestStatusChangeDate;
    }
    public String getUniqueLabel() {
        return toUniqueLabel(mGroupId, getFullAddress());
    }

    // Setters
    public void setId(String id) {
        mId = id;
    }
    public void setPeerId(String peerId) {
        mPeerId = peerId;
    }
    public void setAccountName(String accountName) {
        mAccountName = accountName;
    }
    public void setNickname(@Nullable String friendlyName) {
        if(friendlyName != null) {
            mNickname = friendlyName;
        }
    }
    public void setHeight(Integer height) {
        mHeight = height;
    }
    public void setLatestStatusChangeDate(Date latestStatusChangeDate) {
        mLatestStatusChangeDate = latestStatusChangeDate;
    }

    // Static parsing function
    static DistortConversation readJson(JsonReader json, @Nullable String groupDatabaseId) throws IOException {
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
    public void writeJson(JsonWriter json) throws IOException {
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