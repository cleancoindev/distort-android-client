package com.unix4all.rypi.distort;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.icu.util.TimeZone;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private Context mContext;
    private ArrayList<DistortMessage> mMessagesData;
    private String mFriendlyName;

    public MessageAdapter(Context context, @Nullable ArrayList<DistortMessage> messages, String friendlyName) {
        mContext = context;
        mMessagesData = messages;
        if(messages == null) {
            mMessagesData = new ArrayList<>();
        }
        mFriendlyName = friendlyName;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy", Locale.US);
        timeFormat.setTimeZone(java.util.TimeZone.getTimeZone(TimeZone.getDefault().getID()));

        if(mMessagesData.get(position).getType().equals(DistortMessage.TYPE_IN)) {
            InMessage inMsg = (InMessage) mMessagesData.get(position);

            // Set text fields
            String fromStr = mFriendlyName;

            // Ensure message is verified or warn user
            if(inMsg.getVerified()) {
                // Set message colour for received message
                ((GradientDrawable) holder.mMessageContainer.getBackground()).setColor(mContext.getResources().getColor(R.color.messageReceived));
            } else {
                // Set colour for unverified warning
                ((GradientDrawable) holder.mMessageContainer.getBackground()).setColor(mContext.getResources().getColor(R.color.messageUnverified));
                fromStr += " - **UNVERIFIED**";
            }

            // Set text and gravity
            holder.mFrom.setText(fromStr);
            holder.mFrom.setGravity(Gravity.START);
            holder.mMessage.setText(inMsg.getMessage());
            holder.mMessage.setGravity(Gravity.START);
            holder.mTime.setText(timeFormat.format(inMsg.getDateReceived()));
            holder.mTime.setGravity(Gravity.START);
        } else {
            OutMessage outMsg = (OutMessage) mMessagesData.get(position);

            // Set message colour based on status
            switch(outMsg.getStatus()) {
                case OutMessage.STATUS_ENQUEUED:
                    ((GradientDrawable) holder.mMessageContainer.getBackground()).setColor(ContextCompat.getColor(mContext, R.color.messageEnqueued));
                    break;
                case OutMessage.STATUS_SENT:
                    ((GradientDrawable) holder.mMessageContainer.getBackground()).setColor(ContextCompat.getColor(mContext, R.color.messageSent));
                    break;
                case OutMessage.STATUS_CANCELLED:
                    ((GradientDrawable) holder.mMessageContainer.getBackground()).setColor(ContextCompat.getColor(mContext, R.color.messageCancelled));
                    break;
            }

            // Set text and gravity
            holder.mFrom.setText(R.string.message_from_self);
            holder.mFrom.setGravity(Gravity.END);
            holder.mMessage.setText(outMsg.getMessage());
            holder.mMessage.setGravity(Gravity.END);
            holder.mTime.setText(timeFormat.format(outMsg.getLastStatusChange()));
            holder.mTime.setGravity(Gravity.END);
        }
    }

    @Override
    public int getItemCount() {
        return mMessagesData.size();
    }

    public void addOrUpdateMessage(DistortMessage msg) {
        // Maintain a sorted array. Search for largest-indexed message not greater than the input message
        int i = mMessagesData.size();

        // Logarithmic binary search
        int start = 0;
        int end = i;
        while(start < end) {
            int mid = (start+end)/2;
            int j = mMessagesData.get(mid).getIndex();
            if(j == msg.getIndex()) {
                i = j;
                break;
            } else if(j < msg.getIndex()) {
                start = mid+1;
            } else {
                end = mid;
            }
        }

        if(i == mMessagesData.size() || mMessagesData.get(i).getIndex() > msg.getIndex()) {
            mMessagesData.add(end, msg);
            notifyItemInserted(end);
        } else {
            if(msg.getType().equals(DistortMessage.TYPE_IN)) {
                InMessage inMsg = (InMessage) msg;
                ((InMessage)mMessagesData.get(i)).setVerified(inMsg.getVerified());
            } else if(msg.getType().equals(DistortMessage.TYPE_OUT)) {
                OutMessage outMsg = (OutMessage) msg;
                ((OutMessage)mMessagesData.get(i)).setLastStatusChange(outMsg.getLastStatusChange());
                ((OutMessage)mMessagesData.get(i)).setStatus(outMsg.getStatus());
            } else {
                Log.e("MESSAGE-ADAPTER", "Improper message: " + msg);
            }

            notifyItemChanged(i);
        }
    }

    public void resetAdapter(ArrayList<DistortMessage> messages) {
        int lenOld = mMessagesData.size();
        int lenNew = messages.size();
        mMessagesData = messages;

        for(int i = 0; i < Math.min(lenOld, lenNew); i++) {
            notifyItemChanged(i);
        }

        if(lenNew > lenOld) {
            for(int i = lenOld; i < lenNew; i++) {
                notifyItemInserted(i);
            }
        } else {
            for(int i = lenOld-1; i >= lenNew; i--) {
                notifyItemRemoved(i);
            }
        }
    }

    public int getMessageIndex(int position) {
        return mMessagesData.get(position).getIndex();
    }
}

class MessageViewHolder extends RecyclerView.ViewHolder {
    TextView mFrom;
    TextView mMessage;
    TextView mTime;
    ConstraintLayout mMessageContainer;

    public MessageViewHolder(View itemView) {
        super(itemView);

        mFrom = itemView.findViewById(R.id.messageFrom);
        mMessage = itemView.findViewById(R.id.messageContent);
        mTime = itemView.findViewById(R.id.messageTime);
        mMessageContainer = itemView.findViewById(R.id.messageContainer);
    }
}