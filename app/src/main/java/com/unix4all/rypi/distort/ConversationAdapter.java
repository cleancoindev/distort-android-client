package com.unix4all.rypi.distort;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
        SharedPreferences sp = mContext.getSharedPreferences(mContext.getString(R.string.icon_colours_preferences_keys),
                Context.MODE_PRIVATE);
        int colour = sp.getInt(conversation.getUniqueLabel()+":colour", 0);
        if(colour == 0) {
            Random mRandom = new Random();
            colour = Color.argb(255, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256));
            sp.edit().putInt(conversation.getUniqueLabel()+":colour", colour).apply();
        }
        ((GradientDrawable) holder.mIcon.getBackground()).setColor(colour);
        final int finalColour = colour;

        // Set human readable name
        holder.mNickname.setText(friendlyName);

        // Set peer id to IPFS-hash[:account-name]
        String peerId = conversation.getFullAddress();
        holder.mPeerId.setText(peerId);

        holder.mPeerContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mIntent = new Intent(mContext, MessagingActivity.class);

                // Put group and peer fields
                mIntent.putExtra("peerId", conversation.getPeerId());
                mIntent.putExtra("accountName", conversation.getAccountName());
                mIntent.putExtra("nickname", conversation.getNickname());
                mIntent.putExtra("groupDatabaseId", conversation.getGroupId());
                if(conversation.getId() != null) {
                    mIntent.putExtra("conversationDatabaseId", conversation.getId());
                }
                mIntent.putExtra("icon", holder.mIcon.getText().toString());
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

    public void addOrUpdateConversationPeer(DistortPeer peer, String groupId) {
        int position = mConversationsData.size();
        for(int i = 0; i < mConversationsData.size(); i++) {
            DistortConversation c = mConversationsData.get(i);
            if(c.getFullAddress().equals(peer.getFullAddress())) {
                position = i;
                break;
            }
        }

        if(position == mConversationsData.size()) {
            // Add new empty conversation to list for given peer
            DistortConversation c = new DistortConversation(null, groupId, peer.getPeerId(), peer.getAccountName(), 0, null);
            mConversationsData.add(position, c);
            notifyItemInserted(position);
        } else {
            // Peer object can only update nickname of conversation
            mConversationsData.get(position).setNickname(peer.getFriendlyName());
            notifyItemChanged(position);
        }
    }

    public void addOrUpdateConversation(DistortConversation conversation) {
        int position = mConversationsData.size();
        for(int i = 0; i < mConversationsData.size(); i++) {
            DistortConversation c = mConversationsData.get(i);
            if(c.getFullAddress().equals(conversation.getFullAddress())) {
                position = i;
                break;
            }
        }

        if(position == mConversationsData.size()) {
            mConversationsData.add(position, conversation);
            notifyItemInserted(position);
        } else {
            // Conversation object cannot modify a set nickname value
            mConversationsData.get(position).setId(conversation.getId());
            mConversationsData.get(position).setLatestStatusChangeDate(conversation.getLatestStatusChangeDate());
            mConversationsData.get(position).setHeight(conversation.getHeight());
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