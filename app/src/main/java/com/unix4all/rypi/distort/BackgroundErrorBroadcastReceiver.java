package com.unix4all.rypi.distort;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

public class BackgroundErrorBroadcastReceiver extends BroadcastReceiver {
    private View view;
    private Activity mActivity;

    BackgroundErrorBroadcastReceiver(View v, Activity activity) {
        view = v;
        mActivity = activity;
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        final String error = intent.getStringExtra("error");
        Log.e("BACKGROUND-ERROR", error);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(view, error,
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        });

    }
}
