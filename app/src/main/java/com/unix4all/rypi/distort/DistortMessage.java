package com.unix4all.rypi.distort;

public abstract class DistortMessage {
    private String mId;
    private String mMessage;
    private DistortGroup mGroup;
    private Integer mIndex;

    public static final String TYPE_IN = "IN";
    public static final String TYPE_OUT = "OUT";

    // Getters
    public abstract String getType();
    public String getId() {
        return mId;
    }
    public DistortGroup getGroup() {
        return mGroup;
    }
    public Integer getIndex() {
        return mIndex;
    }
    public String getMessage() {
        return mMessage;
    }

    // Setters
    public void setId(String id) {
        mId = id;
    }
    public void setGroup(DistortGroup group) {
        mGroup = group;
    }
    public void setIndex(Integer index) {
        mIndex = index;
    }
    public void setMessage(String message) {
        mMessage = message;
    }
}