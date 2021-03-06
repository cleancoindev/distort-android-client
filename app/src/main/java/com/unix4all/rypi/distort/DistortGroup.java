package com.unix4all.rypi.distort;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;

// Class representing a group
public class DistortGroup {
    private String mName;
    private Integer mSubgroupIndex;

    DistortGroup(String name, Integer subgroupIndex) {
        mName = name;
        mSubgroupIndex = subgroupIndex;
    }

    public static int getSubgroupLevel(int index) {
        int i = 0;
        while(index > 0) {
            index = (index-1) / 2;
            i++;
        }
        return i;
    }

    // Getters
    public String getName() {
        return mName;
    }
    public Integer getSubgroupIndex() {
        return mSubgroupIndex;
    }
    public Integer getSubgroupLevel() {
        return getSubgroupLevel(mSubgroupIndex);
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
        Integer subgroupIndex = null;

        // Read all fields from group
        json.beginObject();
        while(json.hasNext()) {
            // name subgroupIndex height lastReadIndex
            String key = json.nextName();
            if(key.equals("name")) {
                name = json.nextString();
            } else if(key.equals("subgroupIndex")) {
                subgroupIndex = json.nextInt();
            } else {
                json.skipValue();
            }
        }
        json.endObject();

        if(name != null && subgroupIndex != null) {
            Log.d("READ-GROUP", "Group ( " + name + "," + subgroupIndex + " )");
            return new DistortGroup(name, subgroupIndex);
        } else {
            throw new IOException("Missing group parameters");
        }
    }

    // Write this object to JSON
    public void writeJson(JsonWriter json) throws IOException {
        // Read all fields from group
        json.beginObject();
        json.name("name").value(mName);
        json.name("subgroupIndex").value(mSubgroupIndex);
        json.endObject();
    }
}