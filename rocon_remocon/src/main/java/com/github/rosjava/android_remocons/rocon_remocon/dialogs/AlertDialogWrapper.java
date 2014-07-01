/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2013, Yujin Robot.
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 * * Neither the name of Willow Garage, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.rosjava.android_remocons.rocon_remocon.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

/**
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 *
 * Wraps the alert dialog so it can be used as a yes/no function
 */
public class AlertDialogWrapper {
    protected int state;
    protected AlertDialog dialog;
    protected Activity context;
    protected boolean enablePositive = true;

    public AlertDialogWrapper(Activity context,
                              AlertDialog.Builder builder, String yesButton, String noButton) {
        state = 0;
        this.context = context;
        dialog = builder
                .setPositiveButton(yesButton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                state = 1;
                            }
                        })
                .setNegativeButton(noButton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                state = 2;
                            }
                        }).create();
    }

    public AlertDialogWrapper(Activity context,
                              AlertDialog.Builder builder, String okButton) {
        state = 0;
        this.context = context;
        dialog = builder.setNeutralButton(okButton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        state = 1;
                    }
                }).create();
    }

    public void setTitle(String m) {
        dialog.setTitle(m);
    }

    public void setMessage(String m) {
        dialog.setMessage(m);
    }

    public boolean show(String m) {
        setMessage(m);
        return show();
    }

    public boolean show() {
        state = 0;
        context.runOnUiThread(new Runnable() {
            public void run() {
                dialog.show();
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enablePositive);
            }
        });
        // Kind of a hack. Do we know a better way?
        while (state == 0) {
            try {
                Thread.sleep(1L);
            } catch (Exception e) {
                break;
            }
        }
        dismiss();
        return state == 1;
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
