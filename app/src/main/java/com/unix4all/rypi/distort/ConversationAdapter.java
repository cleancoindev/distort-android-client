package com.unix4all.rypi.distort;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationViewHolder> {
    private ArrayList<DistortConversation> mConversationsData;
    private PeerConversationActivity mContext;

    public ConversationAdapter(PeerConversationActivity context, ArrayList<DistortConversation> conversations) {
        mContext = context;
        if(conversations == null) {
            mConversationsData = new ArrayList<>();
        } else {
            mConversationsData = conversations;
        }
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_peer, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ConversationViewHolder holder, final int position) {
        final DistortConversation conversation = mConversationsData.get(position);

        // Attempt to set a human readable identifier for peer in decreasing order of pleasantness
        String friendlyName = conversation.getFriendlyName();

        // Set Icon text and colour
        holder.mIcon.setText(friendlyName.substring(0, 1));
        Random mRandom = new Random();
        final int colour = Color.argb(255, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256));
        ((GradientDrawable) holder.mIcon.getBackground()).setColor(colour);

        // Set human readable name
        holder.mNickname.setText(friendlyName);

        // Set peer id to IPFS-hash[:account-name]
        String peerId = conversation.getFullAddress();
        holder.mPeerId.setText(peerId);

        holder.mPeerContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start background work
                DistortBackgroundService.startActionFetchMessages(view.getContext(), conversation.getId());

                Intent mIntent = new Intent(mContext, MessagingActivity.class);

                // Put group and peer fields
                mIntent.putExtra("peerId", conversation.getPeerId());
                mIntent.putExtra("accountName", conversation.getAccountName());
                mIntent.putExtra("friendlyName", conversation.getFriendlyName());
                mIntent.putExtra("groupDatabaseId", conversation.getGroupId());
                if(conversation.getId() != null) {
                    mIntent.putExtra("conversationDatabaseId", conversation.getId());
                }
                mIntent.putExtra("icon", holder.mIcon.getText().toString());
                mIntent.putExtra("colorIcon", colour);
                mContext.startActivity(mIntent);
            }
        });
        holder.mPeerContainer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mContext.showRemovePeer(position);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mConversationsData.size();
    }

    public DistortConversation getItem(int arrayIndex) {
        return mConversationsData.get(arrayIndex);
    }

    public void addOrUpdateConversation(DistortConversation conversation) {
        int position = mConversationsData.size();
        for(int i = 0; i < mConversationsData.size(); i++) {
            DistortConversation c = mConversationsData.get(i);
            if(c.getGroupId().equals(conversation.getGroupId()) && c.getFullAddress().equals(conversation.getFullAddress())) {
                position = i;
                break;
            }
        }
        if(position == mConversationsData.size()) {
            mConversationsData.add(position, conversation);
            notifyItemInserted(position);
        } else {
            // Only nickname and groups should change over time
            mConversationsData.get(position).setId(conversation.getId());
            mConversationsData.get(position).setFriendlyName(conversation.getPlainFriendlyName());
            mConversationsData.get(position).setLatestStatusChangeDate(conversation.getLatestStatusChangeDate());
            if(conversation.getHeight() > mConversationsData.get(position).getHeight()) {
                mConversationsData.get(position).setHeight(conversation.getHeight());
            }
            notifyItemChanged(position);
        }
    }

    public void removeItem(int position) {
        mConversationsData.remove(position);
        notifyItemRemoved(position);
    }
}

class ConversationViewHolder extends RecyclerView.ViewHolder {
    TextView mIcon;
    TextView mNickname;
    TextView mPeerId;
    ConstraintLayout mPeerContainer;

    public ConversationViewHolder(View itemView) {
        super(itemView);

        mIcon = itemView.findViewById(R.id.peerIcon);
        mNickname = itemView.findViewById(R.id.peerNickname);
        mPeerId = itemView.findViewById(R.id.peerId);
        mPeerContainer = itemView.findViewById(R.id.peerContainer);
    }
}