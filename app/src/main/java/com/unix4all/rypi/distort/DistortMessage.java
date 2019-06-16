package com.unix4all.rypi.distort;

public abstract class DistortMessage {
    private String mMessage;
    private Integer mIndex;

    public static final String TYPE_IN = "IN";
    public static final String TYPE_OUT = "OUT";

    protected static final String mongoDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // Getters
    public abstract String getType();
    public Integer getIndex() {
        return mIndex;
    }
    public String getMessage() {
        return mMessage;
    }

    // Setters
    public void setIndex(Integer index) {
        mIndex = index;
    }
    public void setMessage(String message) {
        mMessage = message;
    }
}