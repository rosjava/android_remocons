package com.github.rosjava.android_remocons.rocon_remocon.dialogs;

/**
 * Created by jorge on 11/7/13.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;

/**
 * Wraps the progress dialog so it can be used to show/vanish easily
 */
public class ProgressDialogWrapper {
    private ProgressDialog progressDialog;
    private Activity activity;

    public ProgressDialogWrapper(Activity activity) {
        this.activity = activity;
        progressDialog = null;
    }

    public void dismiss() {
        Log.d("Remocon", "Stopping the spinner");
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            }
        });
    }

    public void show(final String title, final String text) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (progressDialog != null) {
                    Log.d("Remocon", "Restarting the spinner with a new message");
                    progressDialog.dismiss();
                }

                progressDialog = ProgressDialog.show(activity, title, text, true, true);
                progressDialog.setCancelable(false);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            }
        });
    }
}
