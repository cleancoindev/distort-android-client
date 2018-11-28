package com.unix4all.rypi.distort;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private ArrayList<DistortMessage> mMessagesData;

    public MessageAdapter(ArrayList<DistortMessage> messages) {
        mMessagesData = messages;
        if(mMessagesData == null) {
            mMessagesData = new ArrayList<DistortMessage>();
        }
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        if(mMessagesData.get(position).getType().equals(DistortMessage.TYPE_IN)) {
            InMessage inMsg = (InMessage) mMessagesData.get(position);

            // Set text fields
            String fromStr = inMsg.getFromPeerId();
            if(inMsg.getFromAccount() != null && inMsg.getFromAccount().length() > 0 && !inMsg.getFromAccount().equals("root")) {
                fromStr += ":" + inMsg.getFromAccount();
            }
            holder.mFrom.setText(fromStr);
            holder.mFrom.setGravity(Gravity.LEFT);
            holder.mMessage.setText(inMsg.getMessage());
            holder.mMessage.setGravity(Gravity.LEFT);
        } else {
            OutMessage outMsg = (OutMessage) mMessagesData.get(position);

            holder.mFrom.setText(R.string.message_from_self);
            holder.mFrom.setGravity(Gravity.RIGHT);
            holder.mMessage.setText(outMsg.getMessage());
            holder.mMessage.setGravity(Gravity.RIGHT);
        }
    }

    @Override
    public int getItemCount() {
        return mMessagesData.size();
    }

    public void addOrUpdateMessage(DistortMessage msg) {
        int position = mMessagesData.size();
        for(int i = 0; i < mMessagesData.size(); i++) {
            if(mMessagesData.get(i).getId().equals(msg.getId())) {
                position = i;
                break;
            }
        }
        if(position == mMessagesData.size()) {
            mMessagesData.add(position, msg);
            notifyItemInserted(position);
        } else {
            if(mMessagesData.get(position).getType().equals(DistortMessage.TYPE_IN)) {
                InMessage inMsg = (InMessage) msg;
                ((InMessage)mMessagesData.get(position)).setVerified(inMsg.getVerified());
            } else {
                OutMessage outMsg = (OutMessage) msg;
                ((OutMessage)mMessagesData.get(position)).setLastStatusChange(outMsg.getmLastStatusChange());
                ((OutMessage)mMessagesData.get(position)).setStatus(outMsg.getStatus());
            }

            notifyItemChanged(position);
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
    ConstraintLayout mMessageContainer;

    public MessageViewHolder(View itemView) {
        super(itemView);

        mFrom = itemView.findViewById(R.id.messageFrom);
        mMessage = itemView.findViewById(R.id.messageContent);
        mMessageContainer = itemView.findViewById(R.id.messageContainer);
    }
}