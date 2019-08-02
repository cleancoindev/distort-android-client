package com.unix4all.rypi.distort;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


public class GettingStartedFragment extends DialogFragment {
    private String mCaption;
    private String mGettingStartedString;

    public interface GettingStartedCloseListener {
        void OnGettingStartedClose();
    }

    public GettingStartedFragment() {}

    public static GettingStartedFragment newInstance(String caption, String gettingStartedString) {
        GettingStartedFragment fragment = new GettingStartedFragment();
        Bundle args = new Bundle();
        args.putString("caption", caption);
        args.putString("gettingStartedString", gettingStartedString);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCaption = getArguments().getString("caption");
            mGettingStartedString = getArguments().getString("gettingStartedString");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_getting_started, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView captionView = view.findViewById(R.id.gettingStartedCaption);
        captionView.setText(mCaption);

        Button okButton = view.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
    }

    @Override
    public void onDetach() {
        SharedPreferences.Editor edit = getActivity().getSharedPreferences(
                getString(R.string.getting_started_preferences_key), Context.MODE_PRIVATE).edit();
        edit.putBoolean(mGettingStartedString, true);
        edit.apply();

        ((GettingStartedCloseListener)getActivity()).OnGettingStartedClose();
        super.onDetach();
    }
}
