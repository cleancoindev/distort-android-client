package com.unix4all.rypi.distort;

import android.support.annotation.Nullable;
import android.util.JsonReader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;

public class DistortJson {

    public static class ResponseString {
        public final int mCode;
        public final String mResponse;

        ResponseString (int code, String response) {
            mCode = code;
            mResponse = response;
        }
    }

    public static class DistortException extends Exception {
        private final String mError;
        private final Integer mResponseCode;

        DistortException(String error, Integer responseCode) {
            super(error);

            mError = error;
            mResponseCode = responseCode;
        }

        // Getters
        @Override
        public String getMessage() {
            return mError;
        }
        public Integer getResponseCode() {
            return mResponseCode;
        }

        static public DistortException readJson(JsonReader json, Integer responseCode) throws IOException {
            String errorString = "";

            json.beginObject();
            while(json.hasNext()) {
                String key = json.nextName();
                if(key.equals("error")) {
                    errorString = json.nextString();
                } else {
                    json.skipValue();
                }
            }
            json.endObject();

            return new DistortException(errorString, responseCode);
        }
    }

    public static @Nullable JsonReader GetJSONFromURL(HttpURLConnection myConnection, DistortAuthParams loginParams) throws DistortException {
        Integer response = 0;
        JsonReader jsonReader = null;
        try {
            myConnection.setRequestMethod("GET");

            // Set request header fields
            myConnection.setRequestProperty("User-Agent", "distort-android-v0.1");
            myConnection.setRequestProperty("Accept","*/*");

            // Auth header fields
            myConnection.setRequestProperty("peerid", loginParams.getPeerId());
            myConnection.setRequestProperty("authtoken", loginParams.getCredential());
            if(loginParams.getAccountName().length() > 0) {
                myConnection.setRequestProperty("accountname", loginParams.getAccountName());
            }


            // Make connection and determine response
            myConnection.connect();
            response = myConnection.getResponseCode();

            // Reading the response
            if(response == 200) {
                jsonReader = new JsonReader(new InputStreamReader(myConnection.getInputStream(), "UTF-8"));
            } else {
                // Not response code 200, format is {"error": "..."}
                jsonReader = new JsonReader(new InputStreamReader(myConnection.getErrorStream(), "UTF-8"));
                throw DistortException.readJson(jsonReader, response);
            }
        } catch(IOException err) {
            throw new DistortException(err.getMessage(), response);
        } finally {
            myConnection.disconnect();
        }

        return jsonReader;
    }

    public static ResponseString GetResponseStringFromURL(HttpURLConnection myConnection, DistortAuthParams loginParams) throws DistortException {
        Integer responseCode = 0;
        ResponseString responseString;
        try {
            myConnection.setRequestMethod("GET");

            // Set request header fields
            myConnection.setRequestProperty("User-Agent", "distort-android-v0.1");
            myConnection.setRequestProperty("Accept", "*/*");

            // Auth header fields
            myConnection.setRequestProperty("peerid", loginParams.getPeerId());
            myConnection.setRequestProperty("authtoken", loginParams.getCredential());
            if (loginParams.getAccountName().length() > 0) {
                myConnection.setRequestProperty("accountname", loginParams.getAccountName());
            }

            // Make connection and determine response
            myConnection.connect();
            responseCode = myConnection.getResponseCode();
            BufferedReader reader;
            if (responseCode == 200) {
                reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(myConnection.getInputStream())));
            } else {
                reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(myConnection.getErrorStream())));
            }
            String inputLine;
            StringBuffer sb = new StringBuffer();
            while ((inputLine = reader.readLine()) != null) {
                sb.append(inputLine);
            }

            responseString = new ResponseString(responseCode, sb.toString());
        } catch(IOException err) {
            throw new DistortException(err.getMessage(), responseCode);
        } finally {
            myConnection.disconnect();
        }

        return responseString;
    }

    public static @Nullable JsonReader PutBodyGetJSONFromURL(HttpURLConnection myConnection, DistortAuthParams loginParams, Map<String, String> bodyParams) throws DistortException {
        try {
            myConnection.setRequestMethod("PUT");
        } catch (IOException err) {
            throw new DistortException(err.getMessage(), 0);
        }

        return SendAndReadJSONFromURL(myConnection, loginParams, bodyParams);
    }

    public static @Nullable JsonReader PostBodyGetJSONFromURL(HttpURLConnection myConnection, DistortAuthParams loginParams, Map<String, String> bodyParams) throws DistortException {
        try {
            myConnection.setRequestMethod("POST");
        } catch (IOException err) {
            throw new DistortException(err.getMessage(), 0);
        }

        return SendAndReadJSONFromURL(myConnection, loginParams, bodyParams);
    }

    public static @Nullable JsonReader DeleteBodyGetJSONFromURL(HttpURLConnection myConnection, DistortAuthParams loginParams, Map<String, String> bodyParams) throws DistortException {
        try {
            myConnection.setRequestMethod("DELETE");
        } catch (IOException err) {
            throw new DistortException(err.getMessage(), 0);
        }

        return SendAndReadJSONFromURL(myConnection, loginParams, bodyParams);
    }

    public static @Nullable JsonReader SendAndReadJSONFromURL(HttpURLConnection myConnection, DistortAuthParams loginParams, Map<String, String> bodyParams) throws DistortException {
        myConnection.setDoOutput(true);

        String paramString = "";
        boolean first = true;
        for (Map.Entry<String, String> entry : bodyParams.entrySet())
        {
            if(first) {
                first = false;
            } else {
                paramString += '&';
            }
            paramString += URLEncoder.encode(entry.getKey()) + "=" + URLEncoder.encode(entry.getValue());
        }
        byte[] postData = paramString.getBytes(Charset.forName("UTF-8"));

        JsonReader jsonReader = null;
        Integer response = 0;

        try {

            // Set request header fields
            myConnection.setRequestProperty("User-Agent", "distort-android-v0.1");
            myConnection.setRequestProperty("Accept", "*/*");
            myConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            myConnection.setRequestProperty("charset", "utf-8");
            myConnection.setRequestProperty("Content-Length", Integer.toString(postData.length));

            // Auth header fields
            myConnection.setRequestProperty("peerid", loginParams.getPeerId());
            myConnection.setRequestProperty("authtoken", loginParams.getCredential());
            if (loginParams.getAccountName().length() > 0) {
                myConnection.setRequestProperty("accountname", loginParams.getAccountName());
            }

            // Make connection and determine response
            myConnection.connect();

            DataOutputStream wr = new DataOutputStream(myConnection.getOutputStream());
            wr.write(postData);
            wr.flush();

            response = myConnection.getResponseCode();

            // Reading the response
            if (response == 200) {
                jsonReader = new JsonReader(new InputStreamReader(myConnection.getInputStream(), "UTF-8"));
            } else {
                // Not response code 200, format is {"error": "..."}
                jsonReader = new JsonReader(new InputStreamReader(myConnection.getErrorStream(), "UTF-8"));
                throw DistortException.readJson(jsonReader, response);
            }
        } catch(IOException err) {
            throw new DistortException(err.getMessage(), response);
        } finally {
            myConnection.disconnect();
        }

        return jsonReader;
    }
}
