package com.unix4all.rypi.distort;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

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
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");

        if(mMessagesData.get(position).getType().equals(DistortMessage.TYPE_IN)) {
            InMessage inMsg = (InMessage) mMessagesData.get(position);

            // Set message colour for received message
            ((GradientDrawable) holder.mMessageContainer.getBackground()).setColor(mContext.getResources().getColor(R.color.messageReceived));

            // Set text fields
            String fromStr = mFriendlyName;

            // Set text and gravity
            holder.mFrom.setText(fromStr);
            holder.mFrom.setGravity(Gravity.LEFT);
            holder.mMessage.setText(inMsg.getMessage());
            holder.mMessage.setGravity(Gravity.LEFT);
            holder.mTime.setText(timeFormat.format(inMsg.getDateReceived()));
            holder.mTime.setGravity(Gravity.LEFT);
        } else {
            OutMessage outMsg = (OutMessage) mMessagesData.get(position);

            // Set message colour based on status
            if(outMsg.getStatus().equals(OutMessage.STATUS_ENQUEUED)) {
                ((GradientDrawable) holder.mMessageContainer.getBackground()).setColor(mContext.getResources().getColor(R.color.messageEnqueued));
            } else if(outMsg.getStatus().equals(OutMessage.STATUS_SENT)) {
                ((GradientDrawable) holder.mMessageContainer.getBackground()).setColor(mContext.getResources().getColor(R.color.messageSent));
            } else if(outMsg.getStatus().equals(OutMessage.STATUS_CANCELLED)) {
                ((GradientDrawable) holder.mMessageContainer.getBackground()).setColor(mContext.getResources().getColor(R.color.messageCancelled));
            }

            // Set text and gravity
            holder.mFrom.setText(R.string.message_from_self);
            holder.mFrom.setGravity(Gravity.RIGHT);
            holder.mMessage.setText(outMsg.getMessage());
            holder.mMessage.setGravity(Gravity.RIGHT);
            holder.mTime.setText(timeFormat.format(outMsg.getLastStatusChange()));
            holder.mTime.setGravity(Gravity.RIGHT);
        }
    }

    @Override
    public int getItemCount() {
        return mMessagesData.size();
    }

    public void addOrUpdateMessage(DistortMessage msg) {

        // Maintain a sorted array. Traverse from the end, where change is most expected
        int i = mMessagesData.size() - 1;
        for(; i >= 0; i--) {
            if(mMessagesData.get(i).getIndex() < msg.getIndex()) {
                break;
            }
        }
        i += 1;
        if(i == mMessagesData.size() || mMessagesData.get(i).getIndex() > msg.getIndex()) {
            mMessagesData.add(i, msg);
            notifyItemInserted(i);
        } else {
            if(mMessagesData.get(i).getType().equals(DistortMessage.TYPE_IN)) {
                InMessage inMsg = (InMessage) msg;
                ((InMessage)mMessagesData.get(i)).setVerified(inMsg.getVerified());
            } else {
                OutMessage outMsg = (OutMessage) msg;
                ((OutMessage)mMessagesData.get(i)).setLastStatusChange(outMsg.getLastStatusChange());
                ((OutMessage)mMessagesData.get(i)).setStatus(outMsg.getStatus());
            }

            notifyItemChanged(i);
        }
    }

    public void removeItem(int position) {
        mMessagesData.remove(position);
        notifyItemRemoved(position);
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