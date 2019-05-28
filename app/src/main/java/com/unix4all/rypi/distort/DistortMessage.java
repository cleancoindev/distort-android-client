package com.unix4all.rypi.distort;

import java.text.SimpleDateFormat;
import java.util.Locale;

public abstract class DistortMessage {
    private String mMessage;
    private String mConversationId;
    private Integer mIndex;

    public static final String TYPE_IN = "IN";
    public static final String TYPE_OUT = "OUT";

    protected static final SimpleDateFormat mongoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    // Getters
    public abstract String getType();
    public String getConversationId() {
        return mConversationId;
    }
    public Integer getIndex() {
        return mIndex;
    }
    public String getMessage() {
        return mMessage;
    }

    // Setters
    public void setConversationId(String conversationDatabaseId) {
        mConversationId = conversationDatabaseId;
    }
    public void setIndex(Integer index) {
        mIndex = index;
    }
    public void setMessage(String message) {
        mMessage = message;
    }
}