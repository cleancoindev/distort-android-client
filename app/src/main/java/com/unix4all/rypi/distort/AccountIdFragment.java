package com.unix4all.rypi.distort;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;


public class AccountIdFragment extends DialogFragment {
    private static final String PEER_ID = "peerId";
    private static final String ACCOUNT_NAME = "accountName";

    private String mPeerId;
    private String mAccountName;
    private GenerateQrCodeTask mGenerateQrCodeTask;

    private ImageView mAccountBarCode;
    private TextView mWaitingText;
    private TextView mAccountId;
    private Button mCloseButton;

    public AccountIdFragment() {
        // Required empty public constructor
    }

    public static AccountIdFragment newInstance(String peerId, String accountName) {
        AccountIdFragment fragment = new AccountIdFragment();
        Bundle args = new Bundle();
        args.putString(PEER_ID, peerId);
        args.putString(ACCOUNT_NAME, accountName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = getArguments();
        mPeerId = args.getString(PEER_ID);
        mAccountName = args.getString(ACCOUNT_NAME);
        return inflater.inflate(R.layout.fragment_account_id, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        String fullAddress = DistortPeer.toFullAddress(mPeerId, mAccountName);

        // Generate QR code of Account ID
        mAccountBarCode = view.findViewById(R.id.accountQrCode);
        mAccountBarCode.setContentDescription(fullAddress);


        // Set text code
        mAccountId = view.findViewById(R.id.accountIdText);
        mAccountId.setText(fullAddress);
        mGenerateQrCodeTask = new GenerateQrCodeTask(fullAddress);
        mGenerateQrCodeTask.execute();

        // Allow button to close dialog
        mCloseButton = view.findViewById(R.id.closeButton);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mWaitingText = view.findViewById(R.id.waitingText);

        // Set dialog title
        getDialog().setTitle(R.string.menu_account_id);
    }

    private class GenerateQrCodeTask extends AsyncTask<Void, Void, Boolean> {

        String mFullAddress;

        GenerateQrCodeTask(String fullAddress) {
            mFullAddress = fullAddress;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                final int length = 512;
                final BitMatrix matrix = new QRCodeWriter().encode(mFullAddress, BarcodeFormat.QR_CODE, length, length);
                final Bitmap img = Bitmap.createBitmap(length, length, Bitmap.Config.RGB_565);
                for(int i = 0; i < length; i ++) {
                    for(int j = 0; j < length; j++) {
                        img.setPixel(i, j, matrix.get(i, j) ? Color.BLACK : Color. WHITE);
                    }
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAccountBarCode.setImageBitmap(img);
                        mWaitingText.setVisibility(View.INVISIBLE);
                    }
                });
            } catch (WriterException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mGenerateQrCodeTask = null;
        }
    }
}
