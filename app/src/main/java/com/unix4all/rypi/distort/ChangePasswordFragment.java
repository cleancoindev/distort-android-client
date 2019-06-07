package com.unix4all.rypi.distort;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Base64;

import java.nio.charset.Charset;


public class ChangePasswordFragment extends DialogFragment {
    EditText mNewPassword;
    EditText mOneMoreTime;
    Button mChange;
    String mPeerId;

    public interface ChangePasswordListener {
        void onChangePasswordFinished(String authToken);
    }

    public ChangePasswordFragment() {
        // Required empty public constructor
    }

    public static ChangePasswordFragment newInstance(String peerId) {
        ChangePasswordFragment fragment = new ChangePasswordFragment();

        Bundle b = new Bundle();
        b.putString("peerId", peerId);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mPeerId = savedInstanceState.getString("peerId", null);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_change_password, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find EditText views
        mNewPassword = (EditText) view.findViewById(R.id.newPasswordEdit);
        mOneMoreTime = (EditText) view.findViewById(R.id.oneMoreTimeEdit);
        mChange = (Button) view.findViewById(R.id.changePasswordButton);
        mChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = mNewPassword.getText().toString();
                if(newPassword.length() < 1) {
                    mNewPassword.setError(getString(R.string.error_invalid_password));
                    mNewPassword.requestFocus();
                } else if(newPassword.equals(mOneMoreTime.getText().toString())) {
                    PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
                    generator.init(newPassword.getBytes(), mPeerId.getBytes(), 1000);
                    KeyParameter passwordBasedKey = (KeyParameter)generator.generateDerivedMacParameters(256);
                    String token = new String(Base64.encode(passwordBasedKey.getKey()), Charset.forName("UTF-8"));

                    // Return entered input
                    ChangePasswordListener listener = (ChangePasswordListener) getActivity();
                    listener.onChangePasswordFinished(token);

                    // Close dialog
                    dismiss();
                } else {
                    mNewPassword.setError(getString(R.string.error_passwords_do_not_math));
                    mNewPassword.requestFocus();
                }
            }
        });

        // Set dialog title
        getDialog().setTitle(R.string.title_change_password);

        // Open keyboard at first input
        mNewPassword.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}
