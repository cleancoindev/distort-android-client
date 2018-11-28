package com.unix4all.rypi.distort;

import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;

// Class representing a group
public class DistortGroup {
    private final String mId;
    private String mName;
    private Integer mSubgroupIndex;
    private Integer mHeight;
    private Integer mLastReadIndex;
    private @Nullable Boolean mIsActive;

    DistortGroup(String name, String id, Integer subgroupIndex, Integer height, Integer lastReadIndex, @Nullable Boolean isActive) {
        mName = name;
        mId = id;
        mSubgroupIndex = subgroupIndex;
        mHeight = height;
        mLastReadIndex = lastReadIndex;
        mIsActive = isActive;
    }

    // Getters
    public String getName() {
        return mName;
    }
    public String getId() {
        return mId;
    }
    public Integer getSubgroupIndex() {
        return mSubgroupIndex;
    }
    public Integer getHeight() {
        return mHeight;
    }
    public Integer getLastReadIndex() {
        return mLastReadIndex;
    }
    public @Nullable Boolean getIsActive() {
        return mIsActive;
    }

    // Setters
    public void setName(String name) {
        mName = name;
    }
    public void setSubgroupIndex(Integer index) {
        mSubgroupIndex = index;
    }
    public void setHeight(Integer height) {
        mHeight = height;
    }
    public void setLastReadIndex(Integer lastReadIndex) {
        mLastReadIndex = lastReadIndex;
    }
    public void setIsActive(@Nullable Boolean isActive) {
        if(isActive != null) {
            mIsActive = isActive;
        }
    }

    // Static parsing function
    static DistortGroup readGroupJson(JsonReader json) throws IOException {
        String name = null;
        String id = null;
        Integer subgroupIndex = null;
        Integer height = null;
        Integer lastReadIndex = null;

        // Read all fields from group
        json.beginObject();
        while(json.hasNext()) {
            // name subgroupIndex height lastReadIndex
            String key = json.nextName();
            if(key.equals("name")) {
                name = json.nextString();
            } else if(key.equals("_id")) {
                id = json.nextString();
            } else if(key.equals("subgroupIndex")) {
                subgroupIndex = json.nextInt();
            } else if(key.equals("height")) {
                height = json.nextInt();
                Log.d("GET-GROUP-HEIGHT", height.toString());
            } else if(key.equals("lastReadIndex")) {
                lastReadIndex = json.nextInt();
            } else {
                json.skipValue();
            }
        }
        json.endObject();

        if(name != null && id != null && subgroupIndex != null && height != null && lastReadIndex != null) {
            Log.d("READ-GROUP", "Group ( " + name + "," + subgroupIndex + "," + height+ "," + lastReadIndex + "," + id + " )");
            return new DistortGroup(name, id, subgroupIndex, height, lastReadIndex, null);
        } else {
            throw new IOException();
        }
    }
}