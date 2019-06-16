package com.unix4all.rypi.distort;

import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

// Class representing a group
public class DistortConversation {
    private final String mGroupName;
    private String mPeerId;
    private String mAccountName;
    private @Nullable String mNickname;
    private Integer mHeight;
    private @Nullable Date mLatestStatusChangeDate;

    protected static final String mongoDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    DistortConversation(String group, String peerId, String accountName, Integer height, @Nullable Date latestChangeDate) {
        mGroupName = group;
        mPeerId = peerId;
        mAccountName = accountName;
        mHeight = height;
        mLatestStatusChangeDate = latestChangeDate;
    }

    public static String toUniqueLabel(String group, String peerFullAddress) {
        return group + ":" + peerFullAddress;
    }
    public static String toUniqueLabel(String group, String peerFullAddress, String[] args) {
        String r = group + ":" + peerFullAddress;
        for(int i = 0; i < args.length; i++) {
            r += ":" + args[i];
        }
        return r;
    }

    // Getters
    public String getGroup() {
        return mGroupName;
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
        return toUniqueLabel(mGroupName, getFullAddress());
    }

    // Setters
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
    static DistortConversation readJson(JsonReader json, @Nullable String groupName) throws IOException {
        String group = groupName;
        String peerId = null;
        String accountName = null;
        Integer height = null;
        Date latestChangeDate = null;

        SimpleDateFormat format = new SimpleDateFormat(mongoDateFormat);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Read all fields from conversation
        json.beginObject();
        while(json.hasNext()) {
            String key = json.nextName();
            if(key.equals("group")) {
                group = json.nextString();
            } else if(key.equals("height")) {
                height = json.nextInt();
            } else if(key.equals("peerId")) {
                peerId = json.nextString();
            } else if(key.equals("accountName")) {
                accountName = json.nextString();
            } else if(key.equals("latestStatusChangeDate")) {
                try {
                    latestChangeDate = format.parse(json.nextString());
                } catch (ParseException e) {
                    latestChangeDate = new Date(0);
                }
            } else {
                json.skipValue();
            }
        }
        json.endObject();

        if(group != null && peerId != null && accountName != null && height != null && latestChangeDate != null) {
            Log.d("READ-CONVERSATION", "Conversation ( " + group + "," + peerId + "," + accountName + "," + height + "," + format.format(latestChangeDate) + " )");
            return new DistortConversation(group, peerId, accountName, height, latestChangeDate);
        } else {
            throw new IOException("Missing conversation parameters");
        }
    }

    // Write this object to JSON
    public void writeJson(JsonWriter json) throws IOException {
        SimpleDateFormat format = new SimpleDateFormat(mongoDateFormat);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Write all fields from conversation
        json.beginObject();
        json.name("group").value(mGroupName);
        json.name("peerId").value(mPeerId);
        json.name("accountName").value(mAccountName);
        json.name("height").value(mHeight);
        json.name("latestStatusChangeDate").value(format.format(mLatestStatusChangeDate));
        json.endObject();
    }
}