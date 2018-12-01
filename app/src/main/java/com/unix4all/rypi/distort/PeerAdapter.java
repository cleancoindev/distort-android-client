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
import java.util.Locale;
import java.util.Random;

public class PeerAdapter extends RecyclerView.Adapter<PeerViewHolder> {
    private ArrayList<DistortPeer> mPeersData;
    private Context mContext;
    private String mGroupDatabaseId;

    public PeerAdapter(Context context, ArrayList<DistortPeer> peers, String groupDatabaseId) {
        mPeersData = peers;
        if(mPeersData == null) {
            mPeersData = new ArrayList<DistortPeer>();
        }
        mContext = context;
        mGroupDatabaseId = groupDatabaseId;
    }

    @Override
    public PeerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_peer, parent, false);
        return new PeerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PeerViewHolder holder, int position) {
        final DistortPeer peer = mPeersData.get(position);

        // Attempt to set a human readable identifier for peer in decreasing order of pleasantness
        String friendlyName = peer.getNickname();
        if(friendlyName == null || friendlyName.isEmpty()) {
            friendlyName = peer.getAccountName();
            if(friendlyName == null || friendlyName.isEmpty()) {
                friendlyName = peer.getPeerId();
            }
        }

        // Set Icon text and colour
        holder.mIcon.setText(friendlyName.substring(0, 1));
        Random mRandom = new Random();
        final int colour = Color.argb(255, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256));
        ((GradientDrawable) holder.mIcon.getBackground()).setColor(colour);

        // Set human readable name
        holder.mNickname.setText(friendlyName);

        // Set peer id to IPFS-hash[:account-name]
        String peerId = peer.getPeerId();
        if(peer.getAccountName() != null && !peer.getAccountName().isEmpty() && !peer.getAccountName().equals("root")) {
            peerId += peer.getAccountName();
        }
        holder.mPeerId.setText(peerId);

        final PeerViewHolder peerViewHolder = holder;
        holder.mPeerContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mIntent = new Intent(mContext, MessagingActivity.class);

                // Put group and peer fields
                mIntent.putExtra("peerDatabaseId", peer.getId());
                mIntent.putExtra("groupDatabaseId", mGroupDatabaseId);
                mIntent.putExtra("icon", peerViewHolder.mIcon.getText().toString());
                mIntent.putExtra("colorIcon", colour);
                mContext.startActivity(mIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPeersData.size();
    }

    public void addOrUpdatePeer(DistortPeer peer) {
        int position = mPeersData.size();
        for(int i = 0; i < mPeersData.size(); i++) {
            if(mPeersData.get(i).getId().equals(peer.getId())) {
                position = i;
                break;
            }
        }
        if(position == mPeersData.size()) {
            mPeersData.add(position, peer);
            notifyItemInserted(position);
        } else {
            // Only nickname and groups should change over time
            mPeersData.get(position).setNickname(peer.getNickname());
            mPeersData.get(position).setGroups(peer.getGroups());
            notifyItemChanged(position);
        }
    }

    public void removeItem(int position) {
        mPeersData.remove(position);
        notifyItemRemoved(position);
    }
}

class PeerViewHolder extends RecyclerView.ViewHolder {
    TextView mIcon;
    TextView mNickname;
    TextView mPeerId;
    ConstraintLayout mPeerContainer;

    public PeerViewHolder(View itemView) {
        super(itemView);

        mIcon = itemView.findViewById(R.id.peerIcon);
        mNickname = itemView.findViewById(R.id.peerNickname);
        mPeerId = itemView.findViewById(R.id.peerId);
        mPeerContainer = itemView.findViewById(R.id.peerContainer);
    }
}