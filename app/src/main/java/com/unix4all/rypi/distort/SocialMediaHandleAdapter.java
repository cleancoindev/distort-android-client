package com.unix4all.rypi.distort;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SocialMediaHandleAdapter extends RecyclerView.Adapter<SocialMediaHandleAdapter.ViewHolder> {

    private ArrayList<String> mHandlesData;
    private Context mContext;
    private ItemClickListener mClickListener;

    public SocialMediaHandleAdapter(Context context, ArrayList<String> handles) {
        mHandlesData = handles;
        mContext = context;
    }

    @Override
    public SocialMediaHandleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_social_media_handle, parent, false);
        return new SocialMediaHandleAdapter.ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String handle = mHandlesData.get(position);
        holder.mHandleText.setText(handle);

        ((GradientDrawable) holder.mLayout.getBackground()).setColor(mContext.getResources().getColor(R.color.twitterHandle));
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mHandlesData.size();
    }

    public String getItem(int position) {
        return mHandlesData.get(position);
    }

    public void updateToSet(Set<String> handles) {
        Set<String> checkedHandles = new HashSet<>();

        for(int i = mHandlesData.size()-1; i >= 0; i--) {
            final String handle = mHandlesData.get(i);
            if(!handles.contains(handle)) {
                mHandlesData.remove(i);
                notifyItemRemoved(i);
            }
            checkedHandles.add(handle);
        }
        int p = mHandlesData.size();
        for(String handle : handles) {
            if(!checkedHandles.contains(handle)) {
                mHandlesData.add(handle);
                notifyItemInserted(p++);
            }
            checkedHandles.add(handle);
        }
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView mHandleText;
        LinearLayout mLayout;

        ViewHolder(View itemView) {
            super(itemView);
            mHandleText = itemView.findViewById(R.id.socialMediaHandle);
            mLayout = itemView.findViewById(R.id.socialMediaHandleLayout);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) {
                mClickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }
}
