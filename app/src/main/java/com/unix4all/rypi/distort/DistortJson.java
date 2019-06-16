package com.unix4all.rypi.distort;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.JsonReader;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
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

            if(!json.hasNext()) {
                return new DistortException("Received empty response", responseCode);
            }

            json.beginObject();
            while(json.hasNext()) {
                String key = json.nextName();
                if(key.equals("error")) {
                    errorString = json.nextString();
                    break;
                } else {
                    json.skipValue();
                }
            }
            json.endObject();

            return new DistortException(errorString, responseCode);
        }
    }

    public static String getQueryString(HashMap<String, String> parameters) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for(Map.Entry<String, String> entry : parameters.entrySet()) {
            if(first) {
                sb.append("?");
                first = false;
            } else {
                sb.append("&");
            }

            sb.append(Uri.encode(entry.getKey()))
                    .append("=")
                    .append(Uri.encode(entry.getValue()));
        }

        return sb.toString();
    }

    public static @Nullable JsonReader getJSONFromURL(HttpURLConnection myConnection, @Nullable DistortAuthParams loginParams) throws DistortException {
        Integer response = 0;
        JsonReader jsonReader = null;
        try {
            myConnection.setRequestMethod("GET");

            // Set request header fields
            myConnection.setRequestProperty("User-Agent", "distort-android-v0.1");
            myConnection.setRequestProperty("Accept","*/*");

            if(loginParams != null) {
                // Auth header fields
                myConnection.setRequestProperty("peerid", loginParams.getPeerId());
                myConnection.setRequestProperty("authtoken", loginParams.getCredential());
                if (loginParams.getAccountName().length() > 0) {
                    myConnection.setRequestProperty("accountname", loginParams.getAccountName());
                }
            }

            // Make connection and determine response
            myConnection.setConnectTimeout(5000);
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

    public static ResponseString getMessageStringFromURL(HttpURLConnection myConnection, @Nullable DistortAuthParams loginParams) throws DistortException {
        JsonReader json = getJSONFromURL(myConnection, loginParams);

        String message = "";
        try {
            json.beginObject();
            while(json.hasNext()) {
                String key = json.nextName();
                if(key.equals("message")) {
                    message = json.nextString();
                    break;
                } else {
                    json.skipValue();
                }
            }
            json.endObject();
        } catch(IOException err) {
            throw new DistortException(err.getMessage(), 200);
        }
        ResponseString responseString = new ResponseString(200, message);

        return responseString;
    }

    public static @Nullable JsonReader putBodyGetJSONFromURL(HttpURLConnection myConnection, @Nullable DistortAuthParams loginParams, Map<String, String> bodyParams) throws DistortException {
        try {
            myConnection.setRequestMethod("PUT");
        } catch (IOException err) {
            throw new DistortException(err.getMessage(), 0);
        }

        return sendAndReadJSONFromURL(myConnection, loginParams, bodyParams);
    }

    public static @Nullable JsonReader postBodyGetJSONFromURL(HttpURLConnection myConnection, @Nullable DistortAuthParams loginParams, Map<String, String> bodyParams) throws DistortException {
        try {
            myConnection.setRequestMethod("POST");
        } catch (IOException err) {
            throw new DistortException(err.getMessage(), 0);
        }

        return sendAndReadJSONFromURL(myConnection, loginParams, bodyParams);
    }

    public static @Nullable JsonReader deleteBodyGetJSONFromURL(HttpURLConnection myConnection, @Nullable DistortAuthParams loginParams, Map<String, String> bodyParams) throws DistortException {
        try {
            myConnection.setRequestMethod("DELETE");
        } catch (IOException err) {
            throw new DistortException(err.getMessage(), 0);
        }

        return sendAndReadJSONFromURL(myConnection, loginParams, bodyParams);
    }

    private static @Nullable JsonReader sendAndReadJSONFromURL(HttpURLConnection myConnection, @Nullable DistortAuthParams loginParams, Map<String, String> bodyParams) throws DistortException {
        myConnection.setDoOutput(true);

        JsonReader jsonReader = null;
        Integer response = 0;

        try {
            // Create parameters string
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : bodyParams.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            byte[] postData = sb.toString().getBytes(Charset.forName("UTF-8"));

            // Set request header fields
            myConnection.setRequestProperty("User-Agent", "distort-android-v0.1");
            myConnection.setRequestProperty("Accept", "*/*");
            myConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            myConnection.setRequestProperty("charset", "utf-8");
            myConnection.setRequestProperty("Content-Length", Integer.toString(postData.length));

            if(loginParams != null) {
                // Auth header fields
                myConnection.setRequestProperty("peerid", loginParams.getPeerId());
                myConnection.setRequestProperty("authtoken", loginParams.getCredential());
                if (loginParams.getAccountName().length() > 0) {
                    myConnection.setRequestProperty("accountname", loginParams.getAccountName());
                }
            }

            // Make connection and determine response
            myConnection.setConnectTimeout(5000);
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
