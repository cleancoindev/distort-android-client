package com.unix4all.rypi.distort;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
        SimpleDateFormat format = new SimpleDateFormat(mongoDateFormat, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            mLastStatusChange = format.parse(jsDate);
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
            if(key.equals("status")) {
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

    public static ArrayList<OutMessage> readArrayJsonForConversation(JsonReader jsonReader) throws IOException {
        ArrayList<OutMessage> messages = new ArrayList<>();

        jsonReader.beginArray();
        while(jsonReader.hasNext()) {
            OutMessage m = readJson(jsonReader);
            messages.add(m);
        }
        jsonReader.endArray();

        return messages;
    }
}
