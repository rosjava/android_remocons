package com.github.rosjava.android_remocons.rocon_remocon.dialogs;

/**
 * Created by jorge on 11/7/13.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Custom alert dialog to prompt launching an app
 */
public class LaunchInteractionDialog extends AlertDialogWrapper {
    public LaunchInteractionDialog(Activity context) {
        super(context, new AlertDialog.Builder(context), "Launch", "Cancel");
    }

    public void setup(rocon_interaction_msgs.Interaction app, boolean allowed, String reason) {

        boolean hasValidIcon = false;
        String iconFormat = app.getIcon().getFormat();
        ChannelBuffer buffer = app.getIcon().getData();

        if (buffer.array().length > 0 && iconFormat != null &&
           (iconFormat.equals("jpeg") || iconFormat.equals("png"))) {
            Bitmap bitmap =
                    BitmapFactory.decodeByteArray(buffer.array(), buffer.arrayOffset(), buffer.readableBytes());
            if (bitmap != null) {
                Bitmap iconBitmap = Bitmap.createScaledBitmap(bitmap, 180, 180, false);
                dialog.setIcon(new BitmapDrawable(context.getResources(), iconBitmap));
                hasValidIcon = true;
            }
        }

        if (! hasValidIcon) {
            // dialog.setIcon(context.getResources().getDrawable(R.drawable.question_mark));
            // TODO default image?  concert icon?
        }

        dialog.setTitle(app.getDisplayName());

        if (allowed) {
            Log.i("Remocon", "This interaction is permitted [" + reason + "]");
            dialog.setMessage(app.getDescription() + "\n\nLovely, you are allowed to launch this interaction.");
            enablePositive = true;
        }
        else {
            Log.i("Remocon", "This interaction is not permitted [" + reason + "]");
            dialog.setMessage(app.getDescription() + "\n\nDude, this is not allowed - what did you do?\n" + reason);
            enablePositive = false;
        }
    }
}
