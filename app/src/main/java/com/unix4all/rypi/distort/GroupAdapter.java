package com.unix4all.rypi.distort;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.Nullable;
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
    private @Nullable String mActiveGroupId;

    public GroupAdapter(GroupsActivity context, ArrayList<DistortGroup> groups, @Nullable String activeGroupId) {
        mGroupsData = groups;
        if(mGroupsData == null) {
            mGroupsData = new ArrayList<>();
        }
        mContext = context;
        mActiveGroupId = activeGroupId;
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
        if(g.getId().equals(mActiveGroupId)) {
            ((GradientDrawable) holder.mGroupContainer.getBackground()).setColor(mContext.getResources().getColor(R.color.activeGroupColour));
        } else {
            ((GradientDrawable) holder.mGroupContainer.getBackground()).setColor(mContext.getResources().getColor(R.color.disabledGroupColour));
        }

        // Set Icon text and colour
        holder.mIcon.setText(g.getName().substring(0, 1));
        SharedPreferences sp = mContext.getSharedPreferences(mContext.getString(R.string.icon_colours_preferences_keys),
                Context.MODE_PRIVATE);
        int colour = sp.getInt(g.getId()+":colour", 0);
        if(colour == 0) {
            Random mRandom = new Random();
            colour = Color.argb(255, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256));
            sp.edit().putInt(g.getId()+":colour", colour).apply();
        }
        ((GradientDrawable) holder.mIcon.getBackground()).setColor(colour);

        // Set text fields
        holder.mName.setText(g.getName());
        holder.mIndex.setText(String.format(Locale.getDefault(),"Node index: %d", g.getSubgroupIndex()));
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

    public void updateActiveGroup(@Nullable String newActiveGroupId) {
        @Nullable String oldId = mActiveGroupId;
        mActiveGroupId = newActiveGroupId;

        if(oldId == null) {
            if(newActiveGroupId == null) {
                return;
            }

            // Set new active group only
            for(int i = 0; i < mGroupsData.size(); i++) {
                if(mGroupsData.get(i).getId().equals(newActiveGroupId)) {
                    // No old active group, set new group to active and exit
                    notifyItemChanged(i);
                    return;
                }
            }
        } else if(oldId.equals(newActiveGroupId)) {
            return;
        }

        boolean unsetOld = false;
        boolean setNew = (newActiveGroupId == null);
        for(int i = 0; i < mGroupsData.size(); i++) {
            if(mGroupsData.get(i).getId().equals(oldId)) {
                // Unset active group
                notifyItemChanged(i);
                if(setNew) {
                    return;
                } else {
                    unsetOld = true;
                }
            } else if(mGroupsData.get(i).getId().equals(newActiveGroupId)) {
                // Set active group
                notifyItemChanged(i);
                if(unsetOld) {
                    return;
                } else {
                    setNew = true;
                }
            }
        }
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
    TextView mIndex;
    ConstraintLayout mGroupContainer;

    public GroupViewHolder(View itemView) {
        super(itemView);

        mIcon = itemView.findViewById(R.id.groupIcon);
        mName = itemView.findViewById(R.id.groupName);
        mIndex = itemView.findViewById(R.id.groupIndex);
        mGroupContainer = itemView.findViewById(R.id.groupContainer);
    }
}