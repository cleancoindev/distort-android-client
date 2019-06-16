package com.unix4all.rypi.distort;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;


public class SetGroupLevelFragment extends DialogFragment {
    private Spinner mLevelSpinner;
    private Button mSetLevelButton;

    private String mGroupName;
    private int mLevel;

    public SetGroupLevelFragment() {
        // Required empty public constructor
    }

    public static SetGroupLevelFragment newInstance(String groupName, int subgroupLevel) {
        SetGroupLevelFragment f = new SetGroupLevelFragment();

        Bundle bundle = new Bundle();
        bundle.putString("groupName", groupName);
        bundle.putInt("subgroupLevel", subgroupLevel);
        f.setArguments(bundle);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle b = this.getArguments();
        mGroupName = b.getString("groupName");
        mLevel = b.getInt("subgroupLevel");

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_group_level, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLevelSpinner = view.findViewById(R.id.setGroupLevel);
        mLevelSpinner.setSelection(mLevel);

        mSetLevelButton = view.findViewById(R.id.setGroupLevelButton);
        mSetLevelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = mLevelSpinner.getSelectedItemPosition();
                if(position == AdapterView.INVALID_POSITION) {
                    return;
                }

                NewGroupFragment.NewGroupListener listener = (NewGroupFragment.NewGroupListener) getActivity();
                listener.onFinishGroupFieldInputs(mGroupName, position);
                dismiss();
            }
        });
    }
}
