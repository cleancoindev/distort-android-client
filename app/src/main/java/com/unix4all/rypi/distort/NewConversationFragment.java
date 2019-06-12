package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewConversationFragment#newInstance} factory method to
 * create an instance of this fragment.
 * create an instance of this fragment.
 */
public class NewConversationFragment extends DialogFragment {

    /**
     * Text field inputs
     */
    private EditText mFriendlyName;
    private EditText mPeerId;
    private EditText mAccountName;
    private Button mAddPeer;
    private Button mScanQrCode;

    private IntentIntegrator qrCodeScan;

    private static final Pattern IS_ACCOUNT_ID_PATTERN = Pattern.compile("^([a-zA-Z0-9]+)(:(.+))?$");


    /**
     * Allow passing data back to activities
     */
    public interface NewConversationListener {
        void onFinishConvoFieldInputs(String friendlyName, String peerId, String accountName);
    }

    public NewConversationFragment() {
        // Required empty public constructor
    }

    public static NewConversationFragment newInstance() {
        NewConversationFragment fragment = new NewConversationFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        qrCodeScan = new IntentIntegrator(getActivity());

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_conversation, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find EditText views
        mFriendlyName = view.findViewById(R.id.newPeerFriendlyName);
        mPeerId = view.findViewById(R.id.newPeerId);
        mAccountName = view.findViewById(R.id.newPeerAccountName);
        mAddPeer = view.findViewById(R.id.newPeerAddButton);

        // Allow QR code scans
        mScanQrCode = view.findViewById(R.id.scanQRCodeButton);
        final Activity activity = getActivity();
        mScanQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrCodeScan.forSupportFragment(NewConversationFragment.this).initiateScan();
            }
        });


        // Set dialog title
        getDialog().setTitle(R.string.title_create_new_conversation);

        // Open keyboard at first input
        mFriendlyName.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        // Allow input field to close dialog
        mAddPeer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishDialog();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(result != null && result.getContents() != null) {
            result.getContents();
            Matcher matcher = IS_ACCOUNT_ID_PATTERN.matcher(result.getContents());
            if(matcher.matches()) {
                mPeerId.setText(matcher.group(1));
                mAccountName.setText(matcher.group(3));
            } else {
                mPeerId.setText("");
                mAccountName.setText("");
                Snackbar.make(getView().findViewById(R.id.newConversationLayout), R.string.error_invalid_barcode, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private boolean finishDialog() {
        String friendlyName = mFriendlyName.getText().toString();
        String peerId = mPeerId.getText().toString();
        String accountName = mAccountName.getText().toString();

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
        if(!IpfsHash.isIpfsHash(peerId)) {
            mPeerId.setError(getResources().getString(R.string.error_invalid_hash));
            mPeerId.requestFocus();
            return false;
        }
        if(accountName == null || accountName.isEmpty()) {
            accountName = "root";
        }

        // Return entered input
        NewConversationListener listener = (NewConversationListener) getActivity();
        listener.onFinishConvoFieldInputs(friendlyName, peerId, accountName);

        // Close dialog
        dismiss();
        return true;
    }
}
