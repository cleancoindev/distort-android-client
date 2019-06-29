package com.unix4all.rypi.distort;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;


public class AccountSigningActivity extends AppCompatActivity {
    private RequestSignatureTask mRequestSignatureTask;
    private GenerateQrCodeTask mGenerateQrCodeTask;
    private DistortAuthParams mLoginParams;

    private LinearLayout mSigningLayout;
    private ImageView mSignatureBarcode;
    private TextView mWaitingText;
    private TextView mSignatureView;
    private EditText mSignTextEdit;
    private Button mSignTextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_signing);

        mLoginParams = DistortAuthParams.getAuthenticationParams(this);

        mSigningLayout = findViewById(R.id.signingLayout);

        mSignatureBarcode = findViewById(R.id.signedQrCode);
        mSignatureBarcode.setVisibility(View.INVISIBLE);

        mWaitingText = findViewById(R.id.waitingText);
        mWaitingText.setVisibility(View.GONE);

        mSignatureView = findViewById(R.id.signedTextView);
        mSignatureView.setVisibility(View.GONE);

        mSignTextEdit = findViewById(R.id.signTextEdit);
        mSignTextEdit.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        mSignTextButton = findViewById(R.id.signTextButton);
        mSignTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mGenerateQrCodeTask == null && mRequestSignatureTask == null) {
                    String plaintext = "create-account://" + mSignTextEdit.getText().toString();

                    if(!plaintext.isEmpty()) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(mSignTextEdit.getWindowToken(), 0);

                        mRequestSignatureTask = new RequestSignatureTask(plaintext);
                        mRequestSignatureTask.execute();
                    } else {
                        mSignTextEdit.setError(getString(R.string.error_field_required));
                        mSignTextEdit.requestFocus();
                    }
                }
            }
        });
    }

    private class RequestSignatureTask extends AsyncTask<Void, Void, Boolean> {
        String mPlaintext;
        String mSignature;
        String mErrorStr;

        RequestSignatureTask(String plaintext) {
            mPlaintext = plaintext;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                DistortJson.ResponseString signature;
                String queryAddress = mLoginParams.getHomeserverAddress() + "signatures";

                HashMap<String, String> queryParams = new HashMap<>();
                queryParams.put("plaintext", mPlaintext);
                queryAddress += DistortJson.getQueryString(queryParams);

                URL homeserverEndpoint = new URL(queryAddress);
                if(DistortAuthParams.PROTOCOL_HTTPS.equals(mLoginParams.getHomeserverProtocol())) {
                    HttpsURLConnection myConnection;
                    myConnection = (HttpsURLConnection) homeserverEndpoint.openConnection();
                    signature = DistortJson.getMessageStringFromURL(myConnection, mLoginParams);
                } else {
                    HttpURLConnection myConnection;
                    myConnection = (HttpURLConnection) homeserverEndpoint.openConnection();
                    signature = DistortJson.getMessageStringFromURL(myConnection, mLoginParams);
                }

                mSignature = signature.mResponse;
                return true;
            } catch (DistortJson.DistortException e) {
                // TODO: Base message off of response codes
                mErrorStr = e.getMessage();
                e.printStackTrace();
                Log.e("FETCH-SIGNATURE", String.valueOf(e.getResponseCode()));
                return false;
            } catch (IOException e) {
                mErrorStr = e.getMessage();
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mRequestSignatureTask = null;
            if(success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSignatureView.setText(mSignature);
                        mSignatureView.setVisibility(View.VISIBLE);
                        mWaitingText.setVisibility(View.VISIBLE);

                        mSignatureBarcode.setVisibility(View.VISIBLE);
                        mSignatureBarcode.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.generate_qr_code, null));

                        mGenerateQrCodeTask = new GenerateQrCodeTask(mSignature);
                        mGenerateQrCodeTask.execute();
                    }
                });
            } else {
                Snackbar.make(mSigningLayout, mErrorStr, Snackbar.LENGTH_LONG);
            }
        }
    }

    private class GenerateQrCodeTask extends AsyncTask<Void, Void, Boolean> {

        final int mLength = 512;
        final Bitmap img = Bitmap.createBitmap(mLength, mLength, Bitmap.Config.RGB_565);

        String mPlaintext;

        GenerateQrCodeTask(String plaintext) {
            mPlaintext = plaintext;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {

                final BitMatrix matrix = new QRCodeWriter().encode(mPlaintext, BarcodeFormat.QR_CODE, mLength, mLength);
                for(int i = 0; i < mLength; i ++) {
                    for(int j = 0; j < mLength; j++) {
                        img.setPixel(i, j, matrix.get(i, j) ? Color.BLACK : Color. WHITE);
                    }
                }
            } catch (WriterException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mGenerateQrCodeTask = null;
            if(success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSignatureBarcode.setImageBitmap(img);
                        mWaitingText.setVisibility(View.GONE);
                    }
                });
            } else {
                Snackbar.make(mSigningLayout, getString(R.string.error_generate_barcode), Snackbar.LENGTH_LONG);
            }
        }
    }
}
