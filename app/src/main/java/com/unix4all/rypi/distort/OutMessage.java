package com.unix4all.rypi.distort;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class OutMessage extends DistortMessage {
    private String mToPeerId;
    private String mToAccount;
    private String mStatus;
    private Date mLastStatusChange;

    public static final String STATUS_SENT = "sent";
    public static final String STATUS_ENQUEUED = "enqueued";
    public static final String STATUS_CANCELLED = "cancelled";

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
    public Date getLastStatusChange() {
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
    void setLastStatusChange(String jsDate) throws IOException {
        try {
            mLastStatusChange = DistortMessage.mongoDateFormat.parse(jsDate);
        } catch (ParseException e) {
            throw new IOException("Server sent invalid date-string: " + jsDate);
        }
    }
    void setLastStatusChange(Date date) {
        mLastStatusChange = date;
    }

    public static OutMessage readMessageJson(JsonReader jsonReader) throws IOException {
        OutMessage message = new OutMessage();

        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            Log.d("GET-OUT-MESSAGE", key);

            if(key.equals("to")) {
                if(jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        String toKey = jsonReader.nextName();
                        Log.d("GET-OUT-MESSAGE-TO", key);

                        if (toKey.equals("accountName")) {
                            message.setToAccount(jsonReader.nextString());
                        } else if (toKey.equals("peerId")) {
                            message.setToPeerId(jsonReader.nextString());
                        } else {
                            jsonReader.skipValue();
                        }
                    }
                    jsonReader.endObject();
                } else {
                    // To value is a cert object, or ID of cert object. The ID is meaningless to us
                    jsonReader.skipValue();
                }
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

    public static ArrayList<OutMessage> readArrayJson(JsonReader jsonReader) throws IOException {
        ArrayList<OutMessage> messages = new ArrayList<>();

        jsonReader.beginArray();
        while(jsonReader.hasNext()) {
            messages.add(readMessageJson(jsonReader));
        }
        jsonReader.endArray();

        return messages;
    }

    // Write this object to JSON
    public void writeMessageJson(JsonWriter json) throws IOException {
        json.beginObject();
        json.name("to").beginObject();
            json.name("accountName").value(mToAccount);
            json.name("peerId").value(mToPeerId);
        json.endObject();
        json.name("status").value(mStatus);
        json.name("_id").value(getId());
        json.name("index").value(getIndex());
        json.name("message").value(getMessage());
        json.name("lastStatusChange").value(mongoDateFormat.format(mLastStatusChange));
        json.endObject();
    }
}
