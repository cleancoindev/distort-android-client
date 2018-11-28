package com.unix4all.rypi.distort;

import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class InMessage extends DistortMessage {
    private String mFromPeerId;
    private String mFromAccount;
    private Boolean mVerified;
    private Date mDateReceived;

    // Getters
    public String getType() {
        return this.TYPE_IN;
    }
    public String getFromPeerId() {
        return mFromPeerId;
    }
    public String getFromAccount() {
        return mFromAccount;
    }
    public Boolean getVerified() {
        return mVerified;
    }
    public Date getDateReceived() {
        return mDateReceived;
    }

    // Setters
    void setFromPeerId(String toPeerID) {
        mFromPeerId = toPeerID;
    }
    void setFromAccount(String toAccount) {
        mFromAccount = toAccount;
    }
    void setVerified(Boolean verified) {
        mVerified = verified;
    }
    void setDateReceived(String jsDate) throws ParseException {
        mDateReceived = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(jsDate);
    }
    void setDateReceived(Date date) {
        mDateReceived = date;
    }

    public static InMessage readMessageJson(JsonReader jsonReader) throws IOException, ParseException {
        InMessage message = new InMessage();

        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            Log.d("GET-IN-MESSAGE", key);

            if(key.equals("from")) {
                jsonReader.beginObject();
                while(jsonReader.hasNext()) {
                    String fromKey = jsonReader.nextName();
                    Log.d("GET-IN-MESSAGE-FROM", key);

                    if(fromKey.equals("accountName")) {
                        message.setFromAccount(jsonReader.nextString());
                    } else if (fromKey.equals("peerId")) {
                        message.setFromPeerId(jsonReader.nextString());
                    } else {
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
            } else if(key.equals("verified")) {
                message.setVerified(jsonReader.nextBoolean());
            } else if(key.equals("_id")) {
                message.setId(jsonReader.nextString());
            } else if(key.equals("index")) {
                message.setIndex(jsonReader.nextInt());
            } else if(key.equals("message")) {
                message.setMessage(jsonReader.nextString());
            } else if(key.equals("dateReceived")) {
                message.setDateReceived(jsonReader.nextString());
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();

        return message;
    }

    public static ArrayList<InMessage> readArrayJson(JsonReader jsonReader) throws IOException, ParseException {
        ArrayList<InMessage> messages = new ArrayList<>();

        jsonReader.beginArray();
        while(jsonReader.hasNext()) {
            messages.add(readMessageJson(jsonReader));
        }
        jsonReader.endArray();

        return messages;
    }
}
