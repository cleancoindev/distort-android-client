package com.unix4all.rypi.distort;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

public class OutMessage extends DistortMessage {
    private String mStatus;
    private Date mLastStatusChange;

    public static final String STATUS_SENT = "sent";
    public static final String STATUS_ENQUEUED = "enqueued";
    public static final String STATUS_CANCELLED = "cancelled";

    // Getters
    public String getType() {
        return this.TYPE_OUT;
    }
    public String getStatus() {
        return mStatus;
    }
    public Date getLastStatusChange() {
        return mLastStatusChange;
    }

    // Setters
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

    public static OutMessage readJson(JsonReader jsonReader) throws IOException {
        OutMessage message = new OutMessage();

        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            if(key.equals("_id")) {
                message.setId(jsonReader.nextString());
            } else if(key.equals("conversation")) {
                message.setConversationId(jsonReader.nextString());
            } else if(key.equals("status")) {
                message.setStatus(jsonReader.nextString());
            } else  if(key.equals("index")) {
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

    public static ArrayList<OutMessage> readArrayJsonForConversation(JsonReader jsonReader, String conversationDatabaseId) throws IOException {
        ArrayList<OutMessage> messages = new ArrayList<>();

        jsonReader.beginArray();
        while(jsonReader.hasNext()) {
            OutMessage m = readJson(jsonReader);
            m.setConversationId(conversationDatabaseId);
            messages.add(m);
        }
        jsonReader.endArray();

        return messages;
    }

    // Write this object to JSON
    public void writeJson(JsonWriter json) throws IOException {
        json.beginObject();
        json.name("status").value(mStatus);
        json.name("_id").value(getId());
        json.name("index").value(getIndex());
        json.name("message").value(getMessage());
        json.name("lastStatusChange").value(mongoDateFormat.format(mLastStatusChange));
        json.endObject();
    }
}
