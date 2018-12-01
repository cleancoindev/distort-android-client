package com.unix4all.rypi.distort;

import android.util.JsonReader;
import android.util.JsonWriter;
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
    void setDateReceived(String jsDate) throws IOException {
        try {
            mDateReceived = DistortMessage.mongoDateFormat.parse(jsDate);
        } catch (ParseException e) {
            throw new IOException("Server sent invalid date-string: " + jsDate);
        }
    }
    void setDateReceived(Date date) {
        mDateReceived = date;
    }

    public static InMessage readMessageJson(JsonReader jsonReader) throws IOException {
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
    public static ArrayList<InMessage> readArrayJson(JsonReader jsonReader) throws IOException {
        ArrayList<InMessage> messages = new ArrayList<>();

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
        json.name("from").beginObject();
            json.name("accountName").value(mFromAccount);
            json.name("peerId").value(mFromPeerId);
        json.endObject();
        json.name("verified").value(mVerified);
        json.name("_id").value(getId());
        json.name("index").value(getIndex());
        json.name("message").value(getMessage());
        json.name("dateReceived").value(mongoDateFormat.format(mDateReceived));
        json.endObject();
    }
}
