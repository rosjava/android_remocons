/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
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

package com.github.rosjava.android_remocons.rocon_remocon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.rosjava.android_remocons.common_tools.master.ConcertChecker;
import com.github.rosjava.android_remocons.common_tools.master.RoconDescription;
import com.github.rosjava.android_remocons.common_tools.system.WifiChecker;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Data class behind view of one item in the list of ROS Masters. Gets created with a master URI and a
 * local host name, then starts a {@link com.github.rosjava.android_remocons.common_tools.master.ConcertChecker}
 * to look up concert name and type.
 *
 * @author hersh@willowgarage.com
 */
public class MasterItem implements ConcertChecker.ConcertDescriptionReceiver,
        ConcertChecker.FailureHandler {
    private ConcertChecker checker;
    private View view;
    private RoconDescription description;
    private MasterChooser parentMca;
    private String errorReason;

    public MasterItem(RoconDescription roconDescription, MasterChooser parentMca) {
        errorReason = "";
        this.parentMca = parentMca;
        this.description = roconDescription;
        this.description.setConnectionStatus(RoconDescription.CONNECTING);
        if (WifiChecker.wifiValid(this.description.getMasterId(),
                (WifiManager) parentMca.getSystemService(parentMca.WIFI_SERVICE))) {
            checker = new ConcertChecker(this, this);
            checker.beginChecking(this.description.getMasterId());
        } else {
            errorReason = "Wrong WiFi Network";
            description.setConnectionStatus(RoconDescription.WIFI);
            safePopulateView();
        }
    }

    public boolean isOk() {
        return this.description.getConnectionStatus().equals(RoconDescription.OK);
    }

    public void handleSuccess() {
        checker.beginChecking(this.description.getMasterId());
    }

    @Override
    public void receive(RoconDescription roconDescription) {
        description.copyFrom(roconDescription);
        safePopulateView();
    }

    @Override
    public void handleFailure(String reason) {
        errorReason = reason;
        description.setConnectionStatus(RoconDescription.ERROR);
        safePopulateView();
    }

    public View getView(Context context, View convert_view, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Using convert_view here seems to cause the wrong view to show
        // up sometimes, so I'm always making new ones.
        view = inflater.inflate(R.layout.master_item, null);
        populateView();
        return view;
    }

    private void safePopulateView() {
        if (view != null) {
            final MasterChooser mca = parentMca;
            view.post(new Runnable() {
                @Override
                public void run() {
                    populateView();
                    mca.writeMasterList();
                }
            });
        }
    }

    private void populateView() {
        Log.i("MasterItem", "connection status = " + description.getConnectionStatus());
        boolean isOk = description.getConnectionStatus().equals(RoconDescription.OK);
        boolean isUnavailable = description.getConnectionStatus().equals(RoconDescription.UNAVAILABLE);
        boolean isWifi = description.getConnectionStatus().equals(RoconDescription.WIFI);
        boolean isError = description.getConnectionStatus().equals(RoconDescription.ERROR);
        boolean isConnecting = description.getConnectionStatus().equals(RoconDescription.CONNECTING);
        ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress_circle);
        progress.setIndeterminate(true);
        progress.setVisibility(isConnecting ? View.VISIBLE : View.GONE);
        ImageView errorImage = (ImageView) view.findViewById(R.id.error_icon);
        errorImage.setVisibility(isError ? View.VISIBLE : View.GONE);
        ImageView iv = (ImageView) view.findViewById(R.id.concert_icon);
        iv.setVisibility((isOk || isWifi || isUnavailable) ? View.VISIBLE : View.GONE);
        if (isWifi) {
            iv.setImageResource(R.drawable.wifi_question_mark);
        } else if (description.getMasterIconData() == null) {
            iv.setImageResource(R.drawable.question_mark);
        } else if (description.getMasterIconData().array().length > 0 && description.getMasterIconFormat() != null &&
                (description.getMasterIconFormat().equals("jpeg") || description.getMasterIconFormat().equals("png"))) {
            ChannelBuffer buffer = description.getMasterIconData();
            Bitmap iconBitmap = BitmapFactory.decodeByteArray(buffer.array(), buffer.arrayOffset(), buffer.readableBytes());
            if (iconBitmap != null) {
                iv.setImageBitmap(iconBitmap);
            } else {
                iv.setImageResource(R.drawable.question_mark);
            }
        } else {
            iv.setImageResource(R.drawable.question_mark);
        }
        if (isUnavailable) {
            // Be nice to do alpha here, but that is api 16 and we are targeting 10.
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0); //0 means grayscale
            ColorMatrixColorFilter cf = new ColorMatrixColorFilter(matrix);
            iv.setColorFilter(cf);
        }
        TextView tv;
        tv = (TextView) view.findViewById(R.id.uri);
        tv.setText(description.getMasterId().toString());
        tv = (TextView) view.findViewById(R.id.name);
        tv.setText(description.getMasterFriendlyName());
        tv = (TextView) view.findViewById(R.id.status);
        tv.setText(errorReason);
    }
}
