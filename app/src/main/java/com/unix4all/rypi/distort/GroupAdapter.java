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

public class GroupAdapter extends RecyclerView.Adapter<GroupViewHolder> {
    private ArrayList<DistortGroup> mGroupsData;
    private GroupsActivity mContext;

    public GroupAdapter(GroupsActivity context, ArrayList<DistortGroup> groups) {
        mGroupsData = groups;
        if(mGroupsData == null) {
            mGroupsData = new ArrayList<>();
        }
        mContext = context;
    }

    @Override
    public GroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GroupViewHolder holder, final int position) {
        final DistortGroup g = mGroupsData.get(position);

        // Set group colour if active
        if(Boolean.valueOf(true).equals(g.getIsActive())) {
            ((GradientDrawable) holder.mGroupContainer.getBackground()).setColor(mContext.getResources().getColor(R.color.activeGroupColour));
        } else {
            ((GradientDrawable) holder.mGroupContainer.getBackground()).setColor(mContext.getResources().getColor(R.color.disabledGroupColour));
        }

        // Set Icon text and colour
        holder.mIcon.setText(g.getName().substring(0, 1));
        Random mRandom = new Random();
        final int colour = Color.argb(255, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256));
        ((GradientDrawable) holder.mIcon.getBackground()).setColor(colour);

        // Set text fields
        holder.mName.setText(g.getName());
        final GroupViewHolder gvh = holder;
        holder.mGroupContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mIntent = new Intent(mContext, PeerConversationActivity.class);

                // Put group fields
                mIntent.putExtra("groupDatabaseId", g.getId());
                mContext.startActivity(mIntent);
            }
        });
        holder.mGroupContainer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mContext.showRemoveGroup(position);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mGroupsData.size();
    }

    public DistortGroup getItem(int arrayIndex) {
        return mGroupsData.get(arrayIndex);
    }

    public void addOrUpdateGroup(DistortGroup group) {
        int position = mGroupsData.size();
        for(int i = 0; i < mGroupsData.size(); i++) {
            if(mGroupsData.get(i).getId().equals(group.getId())) {
                position = i;
                break;
            }
        }
        if(position == mGroupsData.size()) {
            mGroupsData.add(position, group);
            notifyItemInserted(position);
        } else {
            mGroupsData.get(position).setName(group.getName());
            mGroupsData.get(position).setSubgroupIndex(group.getSubgroupIndex());
            mGroupsData.get(position).setIsActive(group.getIsActive());

            notifyItemChanged(position);
        }
    }

    public void removeItem(int position) {
        mGroupsData.remove(position);
        notifyItemRemoved(position);
    }
}

class GroupViewHolder extends RecyclerView.ViewHolder {
    TextView mIcon;
    TextView mName;
    ConstraintLayout mGroupContainer;

    public GroupViewHolder(View itemView) {
        super(itemView);

        mIcon = itemView.findViewById(R.id.groupIcon);
        mName = itemView.findViewById(R.id.groupName);
        mGroupContainer = itemView.findViewById(R.id.groupContainer);
    }
}