package com.unix4all.rypi.distort;

import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;

// Class representing a group
public class DistortGroup {
    private final String mId;
    private String mName;
    private Integer mSubgroupIndex;

    DistortGroup(String name, String id, Integer subgroupIndex) {
        mName = name;
        mId = id;
        mSubgroupIndex = subgroupIndex;
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

    // Setters
    public void setName(String name) {
        mName = name;
    }
    public void setSubgroupIndex(Integer index) {
        mSubgroupIndex = index;
    }

    // Static parsing function
    static DistortGroup readJson(JsonReader json) throws IOException {
        String name = null;
        String id = null;
        Integer subgroupIndex = null;

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
            } else {
                json.skipValue();
            }
        }
        json.endObject();

        if(name != null && id != null && subgroupIndex != null) {
            Log.d("READ-GROUP", "Group ( " + name + "," + subgroupIndex + "," + id + " )");
            return new DistortGroup(name, id, subgroupIndex);
        } else {
            throw new IOException();
        }
    }

    // Write this object to JSON
    public void writeJson(JsonWriter json) throws IOException {
        // Read all fields from group
        json.beginObject();
        json.name("name").value(mName);
        json.name("_id").value(mId);
        json.name("subgroupIndex").value(mSubgroupIndex);
        json.endObject();
    }
}