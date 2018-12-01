package com.unix4all.rypi.distort;

import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DistortPeer {
    private final String mId;
    private String mNickname;
    private String mPeerId;
    private String mAccountName;
    private HashMap<String, Integer> mGroups;

    DistortPeer(String id, String nickname, String peerId, @Nullable String accountName, @Nullable ArrayList<String> groupsIndexCouple) {
        mId = id;
        mNickname = nickname;
        mPeerId = peerId;

        if(accountName == null || accountName.isEmpty()) {
            mAccountName = "root";
        } else {
            mAccountName = accountName;
        }

        mGroups = new HashMap<>();
        if(groupsIndexCouple != null) {
            for(int i = 0; i < groupsIndexCouple.size(); i++) {
                // Get index of last ':' to divide group name from subgroup index
                String couple = groupsIndexCouple.get(i);
                int index = -1;
                for(int j = couple.length()-1; j > 0; j--) {
                    if(couple.charAt(j) == ':') {
                        index = j;
                        break;
                    }
                }

                String groupName = couple.substring(0, index);
                Integer subgroupIndex = Integer.parseInt(couple.substring(index + 1));
                mGroups.put(groupName, subgroupIndex);
            }
        }
    }

    public static String toFullAddress(String peerId, @Nullable String accountName) {
        String fullAddress = peerId;
        if(accountName != null && !accountName.isEmpty() && !accountName.equals("root")) {
            fullAddress += ":" + accountName;
        }

        return fullAddress;
    }

    // Getters
    public String getId() {
        return mId;
    }
    public String getNickname() {
        return mNickname;
    }
    public String getPeerId() {
        return mPeerId;
    }
    public String getAccountName() {
        return mAccountName;
    }
    public String getFullAddress() {
        return toFullAddress(mPeerId, mAccountName);
    }
    public HashMap<String, Integer> getGroups() {
        return mGroups;
    }

    // Setters
    public void setNickname(String nickname) {
        mNickname = nickname;
    }
    public void setPeerId(String peerId) {
        mPeerId = peerId;
    }
    public void setAccountName(String accountName) {
        mAccountName = accountName;
    }
    public void setGroups(HashMap<String, Integer> groups) {
        mGroups = groups;
    }


    // Static parsing function
    static DistortPeer readPeerJson(JsonReader json) throws IOException {
        String nickname = null;
        String peerId = null;
        String accountName = null;
        String id = null;
        ArrayList<String> groupIndexCouples = new ArrayList<>();

        // Read all fields from group
        json.beginObject();
        while(json.hasNext()) {
            // name subgroupIndex height lastReadIndex
            String key = json.nextName();
            if(key.equals("nickname")) {
                nickname = json.nextString();
            } else if(key.equals("_id")) {
                id = json.nextString();
            } else if(key.equals("peerId")) {
                peerId = json.nextString();
            } else if(key.equals("accountName")) {
                accountName = json.nextString();
            } else if(key.equals("cert")) {
                json.beginObject();
                while(json.hasNext()) {
                    String certKey = json.nextName();
                    if(certKey.equals("groups")) {
                        json.beginArray();
                        while(json.hasNext()) {
                            groupIndexCouples.add(json.nextString());
                        }
                        json.endArray();
                    } else {
                        json.skipValue();
                    }
                }
                json.endObject();
            } else {
                json.skipValue();
            }
        }
        json.endObject();

        if(id != null && peerId != null) {
            return new DistortPeer(id, nickname, peerId, accountName, groupIndexCouples);
        } else {
            throw new IOException();
        }
    }

    // Write this object to JSON
    public void writePeerJson(JsonWriter json) throws IOException {
        // Read all fields from group
        json.beginObject();
        json.name("nickname").value(mNickname);
        json.name("_id").value(mId);
        json.name("peerId").value(mPeerId);
        json.name("accountName").value(mAccountName);
        json.name("cert").beginObject();
            json.name("groups").beginArray();
            for(Map.Entry<String, Integer> group : mGroups.entrySet()) {
                json.value(group.getKey() + ':' + group.getValue());
            }
            json.endArray();    // Groups array
        json.endObject();       // Cert object
        json.endObject();       // Peer object
    }
}
