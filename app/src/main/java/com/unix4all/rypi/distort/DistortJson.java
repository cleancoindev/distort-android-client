package com.unix4all.rypi.distort;

import android.support.annotation.Nullable;
import android.util.JsonReader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;

public class DistortJson {

    public static class ResponseString {
        public final int mCode;
        public final String mResponse;

        ResponseString (int code, String response) {
            mCode = code;
            mResponse = response;
        }
    }

    public static @Nullable JsonReader GetJSONFromURL(HttpURLConnection myConnection, DistortAuthParams loginParams) throws ProtocolException, IOException {
        myConnection.setRequestMethod("GET");

        // Set request header fields
        myConnection.setRequestProperty("User-Agent", "distort-android-v0.1");
        myConnection.setRequestProperty("Accept","*/*");
        myConnection.setRequestProperty("peerid", loginParams.getPeerId());
        myConnection.setRequestProperty("authtoken", loginParams.getCredential());
        if(loginParams.getAccountName().length() > 0) {
            myConnection.setRequestProperty("accountname", loginParams.getAccountName());
        }

        // Make connection and determine response
        myConnection.connect();
        int response = myConnection.getResponseCode();

        // Reading the response
        JsonReader jsonReader = null;
        if(response == 200) {
            jsonReader = new JsonReader(new InputStreamReader(myConnection.getInputStream(), "UTF-8"));
        }
        myConnection.disconnect();

        return jsonReader;
    }

    public static ResponseString GetResponseStringFromURL(HttpURLConnection myConnection, DistortAuthParams loginParams) throws ProtocolException, IOException {
        myConnection.setRequestMethod("GET");

        // Set request header fields
        myConnection.setRequestProperty("User-Agent", "distort-android-v0.1");
        myConnection.setRequestProperty("Accept","*/*");
        myConnection.setRequestProperty("peerid", loginParams.getPeerId());
        if(loginParams.getAccountName().length() > 0) {
            myConnection.setRequestProperty("accountname", loginParams.getAccountName());
        }
        myConnection.setRequestProperty("authtoken", loginParams.getCredential());

        // Make connection and determine response
        myConnection.connect();
        int responseCode = myConnection.getResponseCode();
        BufferedReader reader;
        if(responseCode == 200) {
            reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(myConnection.getInputStream())));
        } else {
            reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(myConnection.getErrorStream())));
        }
        String inputLine;
        StringBuffer sb = new StringBuffer();
        while ((inputLine = reader.readLine()) != null) {
            sb.append(inputLine);
        }
        ResponseString responseString = new ResponseString(responseCode, sb.toString());
        myConnection.disconnect();

        return responseString;
    }
}
