package com.unix4all.rypi.distort;

import android.content.Context;
import android.net.Uri;
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
import android.widget.EditText;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewConversationFragment#newInstance} factory method to
 * create an instance of this fragment.
 * create an instance of this fragment.
 */
public class NewConversationFragment extends DialogFragment implements TextView.OnEditorActionListener {

    /**
     * Text field inputs
     */
    private EditText mFriendlyName;
    private EditText mPeerId;


    /**
     * Allow passing data back to activities
     */
    public interface NewConversationListener {
        void onFinishConvoFieldInputs(String friendlyName, String peerId);
    }

    public NewConversationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment NewConversationFragment.
     */
    public static NewConversationFragment newInstance() {
        NewConversationFragment fragment = new NewConversationFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_conversation, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find EditText views
        mFriendlyName = (EditText) view.findViewById(R.id.friendlyName);
        mPeerId = (EditText) view.findViewById(R.id.peerId);

        // Set dialog title
        getDialog().setTitle(R.string.title_create_new_conversation);

        // Open keyboard at first input
        mFriendlyName.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        // Allow input field to close dialog
        mPeerId.setOnEditorActionListener(this);
    }

    private boolean isValidPeerId(String peerId) {
        return IpfsHash.isIpfsHash(peerId);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(EditorInfo.IME_ACTION_DONE == actionId) {
            return finishDialog();
        }
        return false;
    }

    private boolean finishDialog() {
        String friendlyName = mFriendlyName.getText().toString();
        String peerId = mPeerId.getText().toString();

        // TODO: Proper error handling messages
        if(friendlyName.isEmpty()) {
            mFriendlyName.setError(getResources().getString(R.string.error_field_required));
            mFriendlyName.requestFocus();
            return false;
        }
        if(peerId.isEmpty()) {
            mPeerId.setError(getResources().getString(R.string.error_field_required));
            mPeerId.requestFocus();
            return false;
        }
        if(isValidPeerId(peerId)) {
            mPeerId.setError(getResources().getString(R.string.error_invalid_hash));
            mPeerId.requestFocus();
            return false;
        }

        // Return entered input
        NewConversationListener listener = (NewConversationListener) getActivity();
        listener.onFinishConvoFieldInputs(friendlyName, peerId);

        // Close dialog
        dismiss();
        return true;
    }
}
