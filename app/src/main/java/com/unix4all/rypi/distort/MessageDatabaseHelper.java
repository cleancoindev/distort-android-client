package com.unix4all.rypi.distort;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;

public class MessageDatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "distort";
    private static final String TABLE_MESSAGES = "messages";

    private String mAccount;

    public MessageDatabaseHelper(Context ctx, String account) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        mAccount = account;
    }

    private String c(String conversationLabel) {
        return mAccount + ":" + conversationLabel;
     }

     public String getFullAddress() {
        return mAccount;
     }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
            + "conversation TEXT NOT NULL,"
            + "id INTEGER NOT NULL, type TEXT NOT NULL, verified INTEGER,"
            + "status TEXT, message TEXT NOT NULL, date INTEGER NOT NULL,"
                + "PRIMARY KEY(conversation, id)"
        + ")";

        db.execSQL(CREATE_MESSAGES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);

        // Create tables again
        onCreate(db);
    }

    public void addMessage(String conversationLabel, DistortMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("type", message.getType());
        values.put("id", message.getIndex());
        values.put("message", message.getMessage());
        values.put("conversation", c(conversationLabel));
        if(message.getType().equals(DistortMessage.TYPE_IN)) {
            InMessage inMsg = (InMessage)message;
            values.put("date", inMsg.getDateReceived().getTime());
            values.put("verified", inMsg.getVerified() ? 1 : 0);
            db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        } else {
            OutMessage outMsg = (OutMessage)message;
            values.put("status", outMsg.getStatus());
            values.put("date", outMsg.getLastStatusChange().getTime());
            db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }

        db.close(); // Closing database connection
    }

    public DistortMessage getMessage(String conversationLabel, int index) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_MESSAGES, new String[] {"type", "id", "message", "date", "status", "verified"}, "id=? AND conversation='?'",
                new String[] {String.valueOf(index), c(conversationLabel)}, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        DistortMessage message;
        if(cursor.getString(0).equals(DistortMessage.TYPE_IN)) {
            message = new InMessage();
            message.setIndex(cursor.getInt(1));
            message.setMessage(cursor.getString(2));
            ((InMessage) message).setDateReceived(new Date(cursor.getLong(3)));
            ((InMessage) message).setVerified(cursor.getInt(5) == 1);
        } else {
            message = new OutMessage();
            message.setIndex(cursor.getInt(1));
            message.setMessage(cursor.getString(2));
            ((OutMessage) message).setLastStatusChange(new Date(cursor.getLong(3)));
            ((OutMessage) message).setStatus(cursor.getString(4));
        }

        return message;
    }

    // Map of messages keyed by their index
    public ArrayList<DistortMessage> getMessagesInRange(String conversationLabel, int startIndex, @Nullable Integer endIndex) {
        ArrayList<DistortMessage> messages = new ArrayList<>();
        StringBuilder selectQuery = new StringBuilder("SELECT type, id, message, date, status, verified FROM ")
                .append(TABLE_MESSAGES).append(" WHERE")
                .append(" conversation='").append(c(conversationLabel)).append("' AND")
                .append(" id >= ").append(startIndex);
        if(endIndex != null) {
            selectQuery.append(" AND id <= ").append(endIndex);
        }
        selectQuery.append(" ORDER BY id ASC");

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery.toString(), null);

        if (cursor.moveToFirst()) {
            do {
                DistortMessage message;
                if(cursor.getString(0).equals(DistortMessage.TYPE_IN)) {
                    message = new InMessage();
                    message.setIndex(cursor.getInt(1));
                    message.setMessage(cursor.getString(2));
                    ((InMessage) message).setDateReceived(new Date(cursor.getLong(3)));
                    ((InMessage) message).setVerified(cursor.getInt(5) == 1);
                } else {
                    message = new OutMessage();
                    message.setIndex(cursor.getInt(1));
                    message.setMessage(cursor.getString(2));
                    ((OutMessage) message).setLastStatusChange(new Date(cursor.getLong(3)));
                    ((OutMessage) message).setStatus(cursor.getString(4));
                }

                // Adding contact to list
                messages.add(message);
            } while (cursor.moveToNext());
        }

        // return contact list
        return messages;
    }

    public long getMessagesCount(String conversationLabel) {
        String countQuery = "SELECT  id FROM " + TABLE_MESSAGES + " WHERE conversation=" + c(conversationLabel);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }
}
