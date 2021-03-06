package com.unix4all.rypi.distort;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class GroupAdapter extends RecyclerView.Adapter<GroupViewHolder> {
    private ArrayList<DistortGroup> mGroupsData;
    private GroupsActivity mContext;
    private @Nullable String mActiveGroup;

    public GroupAdapter(GroupsActivity context, ArrayList<DistortGroup> groups, @Nullable String activeGroup) {
        mGroupsData = groups;
        if(mGroupsData == null) {
            mGroupsData = new ArrayList<>();
        }
        mContext = context;
        mActiveGroup = activeGroup;
    }

    @Override
    public GroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.recycler_view_group, parent, false);
        return new GroupViewHolder(view, mContext);
    }

    @Override
    public void onBindViewHolder(GroupViewHolder holder, final int position) {
        final DistortGroup g = mGroupsData.get(position);

        // Set group colour if active
        if(g.getName().equals(mActiveGroup)) {
            ((GradientDrawable) holder.mGroupContainer.getBackground()).setColor(mContext.getResources().getColor(R.color.activeGroupColour));
        } else {
            ((GradientDrawable) holder.mGroupContainer.getBackground()).setColor(mContext.getResources().getColor(R.color.disabledGroupColour));
        }

        // Set Icon text and colour
        holder.mIcon.setText(g.getName().substring(0, 1));
        SharedPreferences sp = mContext.getSharedPreferences(mContext.getString(R.string.icon_colours_preferences_keys),
                Context.MODE_PRIVATE);
        int colour = sp.getInt(g.getName()+":colour", 0);
        if(colour == 0) {
            Random mRandom = new Random();
            colour = Color.argb(255, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256));
            sp.edit().putInt(g.getName()+":colour", colour).apply();
        }
        ((GradientDrawable) holder.mIcon.getBackground()).setColor(colour);

        // Set text fields
        holder.mName.setText(g.getName());
        holder.mIndex.setText(String.format(Locale.getDefault(),"Group level: %d", g.getSubgroupLevel()));
        final GroupViewHolder gvh = holder;
        holder.mGroupContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mIntent = new Intent(mContext, PeerConversationActivity.class);

                // Put group fields
                mIntent.putExtra("groupName", g.getName());
                mContext.startActivity(mIntent);
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

    public void updateActiveGroup(@Nullable String newActiveGroup) {
        @Nullable String old = mActiveGroup;
        mActiveGroup = newActiveGroup;

        if(old == null || old.isEmpty()) {
            if(newActiveGroup == null || newActiveGroup.isEmpty()) {
                return;
            }

            // Set new active group only
            for(int i = 0; i < mGroupsData.size(); i++) {
                if(mGroupsData.get(i).getName().equals(newActiveGroup)) {
                    // No old active group, set new group to active and exit
                    notifyItemChanged(i);
                    return;
                }
            }
        } else if(old.equals(newActiveGroup)) {
            return;
        }

        boolean unsetOld = false;
        boolean setNew = newActiveGroup == null || newActiveGroup.isEmpty();
        for(int i = 0; i < mGroupsData.size(); i++) {
            if(mGroupsData.get(i).getName().equals(old)) {
                // Unset previous active group
                notifyItemChanged(i);
                if(setNew) {
                    return;
                } else {
                    unsetOld = true;
                }
            } else if(mGroupsData.get(i).getName().equals(newActiveGroup)) {
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
            if(mGroupsData.get(i).getName().equals(group.getName())) {
                position = i;
                break;
            }
        }
        if(position == mGroupsData.size()) {
            mGroupsData.add(position, group);
            notifyItemInserted(position);
        } else {
            if(!mGroupsData.get(position).getSubgroupIndex().equals(group.getSubgroupIndex())) {
                mGroupsData.get(position).setSubgroupIndex(group.getSubgroupIndex());
                notifyItemChanged(position);
            }
        }
    }

    public void removeItem(int position) {
        mGroupsData.remove(position);
        notifyItemRemoved(position);
    }
}

class GroupViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
    GroupsActivity mContext;

    TextView mIcon;
    TextView mName;
    TextView mIndex;
    ConstraintLayout mGroupContainer;

    public GroupViewHolder(View itemView, GroupsActivity context) {
        super(itemView);

        mContext = context;
        mIcon = itemView.findViewById(R.id.groupIcon);
        mName = itemView.findViewById(R.id.groupName);
        mIndex = itemView.findViewById(R.id.groupIndex);
        mGroupContainer = itemView.findViewById(R.id.groupContainer);
        mGroupContainer.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        mContext.getMenuInflater().inflate(R.menu.menu_group, menu);
        for(int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setOnMenuItemClickListener(this);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.setSubgroupMenuItem:
                mContext.showChangeGroupLevel(this.getAdapterPosition());
                break;
            case R.id.removeGroupMenuItem:
                mContext.showRemoveGroup(this.getAdapterPosition());
                break;
            case R.id.setColourItem:
                mContext.openColourPicker(this.getAdapterPosition());
                break;
            default:
                return false;
        }
        return true;
    }
}