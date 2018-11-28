package com.unix4all.rypi.distort;

import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class OutMessage extends DistortMessage {
    private String mToPeerId;
    private String mToAccount;
    private String mStatus;
    private Date mLastStatusChange;

    // Getters
    public String getType() {
        return this.TYPE_OUT;
    }
    public String getToPeerId() {
        return mToPeerId;
    }
    public String getToAccount() {
        return mToAccount;
    }
    public String getStatus() {
        return mStatus;
    }
    public Date getmLastStatusChange() {
        return mLastStatusChange;
    }

    // Setters
    void setToPeerId(String toPeerID) {
        mToPeerId = toPeerID;
    }
    void setToAccount(String toAccount) {
        mToAccount = toAccount;
    }
    void setStatus(String status) {
        mStatus = status;
    }
    void setLastStatusChange(String jsDate) throws ParseException {
        mLastStatusChange = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(jsDate);
    }
    void setLastStatusChange(Date date) {
        mLastStatusChange = date;
    }

    public static OutMessage readMessageJson(JsonReader jsonReader) throws IOException, ParseException {
        OutMessage message = new OutMessage();

        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            Log.d("GET-OUT-MESSAGE", key);

            if(key.equals("to")) {
                jsonReader.beginObject();
                while(jsonReader.hasNext()) {
                    String toKey = jsonReader.nextName();
                    Log.d("GET-OUT-MESSAGE-TO", key);

                    if(toKey.equals("accountName")) {
                        message.setToAccount(jsonReader.nextString());
                    } else if (toKey.equals("peerId")) {
                        message.setToPeerId(jsonReader.nextString());
                    } else {
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
            } else if(key.equals("status")) {
                message.setStatus(jsonReader.nextString());
            } else if(key.equals("_id")) {
                message.setId(jsonReader.nextString());
            } else if(key.equals("index")) {
                message.setIndex(jsonReader.nextInt());
            } else if(key.equals("message")) {
                message.setMessage(jsonReader.nextString());
            } else if(key.equals("lastStatusChange")) {
                message.setLastStatusChange(jsonReader.nextString());
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();

        return message;
    }

    public static ArrayList<OutMessage> readArrayJson(JsonReader jsonReader) throws IOException, ParseException {
        ArrayList<OutMessage> messages = new ArrayList<>();

        jsonReader.beginArray();
        while(jsonReader.hasNext()) {
            messages.add(readMessageJson(jsonReader));
        }
        jsonReader.endArray();

        return messages;
    }
}
