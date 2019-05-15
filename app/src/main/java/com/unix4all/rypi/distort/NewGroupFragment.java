package com.unix4all.rypi.distort;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewGroupFragment#newInstance} factory method to
 * create an instance of this fragment.
 * create an instance of this fragment.
 */
public class NewGroupFragment extends DialogFragment {

    /**
     * Text field inputs
     */
    private EditText mGroupName;
    private Spinner mGroupLevel;
    private Button mJoinGroup;
    private Button mDefaultGroup;


    /**
     * Allow passing data back to activities
     */
    public interface NewGroupListener {
        void onFinishGroupFieldInputs(String groupName, Integer subgroupLevel);
    }

    public NewGroupFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment NewGroupFragment.
     */
    public static NewGroupFragment newInstance() {
        NewGroupFragment fragment = new NewGroupFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_group, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find EditText views
        mGroupName = (EditText) view.findViewById(R.id.newGroupName);
        mGroupLevel = (Spinner) view.findViewById(R.id.newGroupLevel);
        mJoinGroup = (Button) view.findViewById(R.id.joinGroupButton);
        mDefaultGroup = (Button) view.findViewById(R.id.defaultGroupButton);
        mDefaultGroup.setText(R.string.text_default_group_name);


        // Set dialog title
        getDialog().setTitle(R.string.title_create_new_conversation);

        // Focus keyboard at start
        mGroupName.requestFocus();

        // Allow input field to close dialog
        mJoinGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishDialog();
            }
        });

        // Set group name to default
        mDefaultGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGroupName.setText(R.string.text_default_group_name);
            }
        });
    }

    private boolean finishDialog() {
        String groupName = mGroupName.getText().toString();
        Integer subgroupLevel = mGroupLevel.getSelectedItemPosition();

        // TODO: Proper error handling messages
        if(groupName.isEmpty()) {
            mGroupName.setError(getResources().getString(R.string.error_field_required));
            mGroupName.requestFocus();
            return false;
        }

        // Return entered input
        NewGroupListener listener = (NewGroupListener) getActivity();
        listener.onFinishGroupFieldInputs(groupName, subgroupLevel);

        // Close dialog
        dismiss();
        return true;
    }
}
