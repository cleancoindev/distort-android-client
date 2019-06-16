package com.unix4all.rypi.distort;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


public class RenamePeerFragment extends DialogFragment {
    private EditText mNewName;
    private Button mRenameButton;

    private String mPeerId;
    private String mAccountName;
    private @Nullable String mNickname;

    public interface OnRenamePeerFragmentListener {
        void OnRenamePeer(String nickname, String peerId, String accountName);
    }

    public RenamePeerFragment() {
        // Required empty public constructor
    }

    public static RenamePeerFragment newInstance(String peerId, String accountName, @Nullable String nickname) {
        RenamePeerFragment f = new RenamePeerFragment();

        Bundle bundle = new Bundle();
        bundle.putString("peerId", peerId);
        bundle.putString("accountName", accountName);
        bundle.putString("nickname", nickname);
        f.setArguments(bundle);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle b = this.getArguments();
        mPeerId = b.getString("peerId");
        mAccountName = b.getString("accountName");
        mNickname = b.getString("nickname");

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rename_peer, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNewName = view.findViewById(R.id.renamePeerText);
        if(mNickname != null) {
            mNewName.setText(mNickname);
        }

        mRenameButton = view.findViewById(R.id.renamePeerButton);
        mRenameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mNewName.getText().toString();
                if(name.isEmpty()) {
                    mNewName.setError(getString(R.string.error_field_required));
                    return;
                }

                OnRenamePeerFragmentListener listener = (OnRenamePeerFragmentListener) getActivity();
                listener.OnRenamePeer(name, mPeerId, mAccountName);
                dismiss();
            }
        });
    }
}
