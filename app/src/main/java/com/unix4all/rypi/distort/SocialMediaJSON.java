package com.unix4all.rypi.distort;

import android.content.SharedPreferences;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

// Object for managing all social media identities linked with account
public class SocialMediaJSON {

    // Interface for a social media platform
    public interface Platform {
        void writeJSON(JsonWriter json) throws IOException;
        String getPlatform();
        String getHandle();
        String getKey();
    }

    // First implementation, Twitter
    public static class TwitterPlatform implements Platform {
        String mHandle;
        String mAccessToken;
        String mAccessSecret;
        String mConsumerKey;
        String mConsumerSecret;

        public TwitterPlatform(String handle, String key) throws IOException {
            mHandle = handle;

            // Twitter requires multiple keys, stored locally as JSON string
            StringReader sr = new StringReader(key);
            JsonReader keyJson = new JsonReader(sr);
            keyJson.beginObject();
            while(keyJson.hasNext()) {
                String keyName = keyJson.nextName();
                if(key.equals("access_token")) {
                    mAccessToken = keyJson.nextString();
                } else if(keyName.equals("access_token_secret")) {
                    mAccessSecret = keyJson.nextString();
                } else if(keyName.equals("consumer_key")) {
                    mConsumerKey = keyJson.nextString();
                } else if(keyName.equals("consumer_secret")) {
                    mConsumerSecret = keyJson.nextString();
                } else {
                    keyJson.skipValue();
                }
            }
            keyJson.endObject();
        }
        public TwitterPlatform(String handle, String token, String secret, String consumerKey, String consumerSecret) {
            mHandle = handle;
            mAccessToken = token;
            mAccessSecret = secret;
            mConsumerKey = consumerKey;
            mConsumerSecret = consumerSecret;
        }

        public String getKey() {
            try {
                // Twitter requires multiple keys, stored locally as JSON string
                StringWriter sw = new StringWriter();
                JsonWriter keyJson = new JsonWriter(sw);
                keyJson.beginObject();
                keyJson.name("access_token").value(mAccessToken);
                keyJson.name("access_token_secret").value(mAccessSecret);
                keyJson.name("consumer_key").value(mConsumerKey);
                keyJson.name("consumer_secret").value(mConsumerSecret);
                keyJson.endObject();
                return sw.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        public String getHandle() {
            return mHandle;
        }

        @Override
        public void writeJSON(JsonWriter json) throws IOException {
            // Write to file
            json.beginObject();
            json.name("platform").value(getPlatform());
            json.name("handle").value(mHandle);
            json.name("key").value(getKey());
            json.endObject();
        }
        @Override
        public String getPlatform() {
            return "twitter";
        }
    }

    // Read an individual social-media platform from list
    private static Platform readPlatformJSON(JsonReader json) throws  IOException {
        String platform = null;
        String key = null;
        String handle = null;

        json.beginObject();
        while(json.hasNext()) {
            String keyName = json.nextName();
            if(keyName.equals("platform")) {
                platform = json.nextString();
            } else if(keyName.equals("handle")) {
                handle = json.nextString();
            } else if(keyName.equals("key")) {
                key = json.nextString();
            } else {
                json.skipValue();
            }
        }
        json.endObject();

        if(platform == null || key == null || handle == null) {
            throw new IOException();
        }

        if(platform.equals("twitter")) {
            return new TwitterPlatform(handle, key);
        } else {
            throw new IOException();
        }
    }

    // Class to manage links
    Map<String, Platform> mPlatforms;
    public SocialMediaJSON(Map<String, Platform> platforms) {
        mPlatforms = platforms;
    }
    public void put(Platform platform) {
        mPlatforms.put(platform.getPlatform(), platform);
    }
    public void remove(String platform) {
        mPlatforms.remove(platform);
    }
    public void setPlatforms(Map<String, Platform> platform) {
        mPlatforms = platform;
    }
    public Map<String, Platform> getPlatforms() {
        return mPlatforms;
    }

    // Reading links from JSON
    public static SocialMediaJSON readJson(JsonReader json) {
        SocialMediaJSON socialMediaJSON = new SocialMediaJSON(new HashMap<String, Platform>());
        try {
            json.beginArray();
            while (json.hasNext()) {
                Platform p = readPlatformJSON(json);
                socialMediaJSON.put(p);
            }
            json.endArray();
        } catch (Exception err) {
            err.printStackTrace();
            socialMediaJSON = new SocialMediaJSON(new HashMap<String, Platform>());
        } finally {
            return socialMediaJSON;
        }
    }
    // Reading social-media JSON from shared preferences
    public static SocialMediaJSON readPreferences(SharedPreferences sharedPref, String fullAddress) {
        // Get social media links for this specific account, default empty array
        String socialMediaStr = sharedPref.getString(fullAddress, "[]");
        StringReader sr = new StringReader(socialMediaStr);
        return readJson(new JsonReader(sr));
    }

    // Writing links to JSON
    public void writeJSON(JsonWriter json) throws IOException {
        json.beginArray();
        for(Map.Entry<String, Platform> platform : mPlatforms.entrySet()) {
            platform.getValue().writeJSON(json);
        }
        json.endArray();
    }
    // Writing social-media JSON to shared preferences
    public void writePreferences(SharedPreferences sharedPref, String fullAddress) {
        SharedPreferences.Editor prefEditor = sharedPref.edit();

        // Write contents to JSON string and apply
        StringWriter sw = new StringWriter();
        try {
            writeJSON(new JsonWriter(sw));
        } catch(Exception err) {
            err.printStackTrace();
        }

        prefEditor.putString(fullAddress, sw.toString());
        prefEditor.apply();
    }
}
