package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class AccountActivity extends AppCompatActivity {
    private DistortAuthParams mLoginParams;

    private ImageView mAccountBarCode;
    private TextView mWaitingText;
    private TextView mAccountId;
    private Button mOpenSignDialogButton;
    private Button mCloseButton;

    private GenerateQrCodeTask mGenerateQrCodeTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mLoginParams = DistortAuthParams.getAuthenticationParams(this);

        mAccountBarCode = findViewById(R.id.accountBarcode);

        // Set text code
        String fullAddress = DistortPeer.toFullAddress(mLoginParams.getPeerId(), mLoginParams.getAccountName());
        mAccountId = findViewById(R.id.accountIdText);
        mAccountId.setText(fullAddress);
        mGenerateQrCodeTask = new GenerateQrCodeTask(fullAddress);
        mGenerateQrCodeTask.execute();

        // Allow button to close dialog
        mCloseButton = findViewById(R.id.closeButton);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        final Activity self = this;
        mOpenSignDialogButton = findViewById(R.id.openSignDialogButton);
        mOpenSignDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(self, SigningActivity.class);
                self.startActivity(intent);
            }
        });

        mWaitingText = findViewById(R.id.waitingText);
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

                runOnUiThread(new Runnable() {
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
