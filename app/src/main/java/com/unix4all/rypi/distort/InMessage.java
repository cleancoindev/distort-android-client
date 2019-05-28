package com.unix4all.rypi.distort;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

public class InMessage extends DistortMessage {
    private Boolean mVerified;
    private Date mDateReceived;

    // Getters
    public String getType() {
        return this.TYPE_IN;
    }
    public Boolean getVerified() {
        return mVerified;
    }
    public Date getDateReceived() {
        return mDateReceived;
    }

    // Setters
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

    public static InMessage readJson(JsonReader jsonReader) throws IOException {
        InMessage message = new InMessage();

        jsonReader.beginObject();
        while(jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            if(key.equals("conversation")) {
                message.setConversationId(jsonReader.nextString());
            } else if(key.equals("verified")) {
                message.setVerified(jsonReader.nextBoolean());
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
    public static ArrayList<InMessage> readArrayJsonForConversation(JsonReader jsonReader, String conversationDatabseId) throws IOException {
        ArrayList<InMessage> messages = new ArrayList<>();

        jsonReader.beginArray();
        while(jsonReader.hasNext()) {
            InMessage m = readJson(jsonReader);
            m.setConversationId(conversationDatabseId);
            messages.add(m);
        }
        jsonReader.endArray();

        return messages;
    }

    // Write this object to JSON
    public void writeJson(JsonWriter json) throws IOException {
        json.beginObject();
        json.name("verified").value(mVerified);
        json.name("index").value(getIndex());
        json.name("message").value(getMessage());
        json.name("dateReceived").value(mongoDateFormat.format(mDateReceived));
        json.endObject();
    }
}
