/*
* Software License Agreement (BSD License)
*
* Copyright (c) 2011, Willow Garage, Inc.
* Copyright (c) 2013, OSRF.
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

package com.github.rosjava.android_remocons.concert_remocon;

import java.net.URI;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.github.rosjava.android_apps.application_management.ConcertDescription;

public class AppLauncher {
    static private final String CLIENT_TYPE = "android";


    /**
     * Launch a client app for the given concert app.
     */
    static public boolean launch(final Activity parentActivity, concert_msgs.RemoconApp app,
                                 URI uri, ConcertDescription currentConcert, boolean runningNodes) {
//    ArrayList<ClientAppData> android_apps = new ArrayList<ClientAppData>();

        // Create the Intent from rapp's name, pass it parameters and remaps and start it
        // TODO: issue 1: request permit to role manager to start the app
        // TODO: show a dialog with app info and start confirmation

        if (parentActivity instanceof ConcertRemocon) {
            ((ConcertRemocon) parentActivity).onAppClicked(app);
        } else {
            Log.i("ConcertRemocon", "Could not launch because parent is not an appchooser");
            return false;
        }

        Log.i("ConcertRemocon", "launching concert app " + app.getDisplayName() + " on service " + app.getServiceName());

        String className = app.getName();

        // Create an intent for this app
        Intent intent = new Intent();

        // Set up standard intent fields.
        intent.setAction(className);

        // Copy all app data to "extra" data in the intent.
        intent.putExtra(AppsManager.PACKAGE + ".concert_app_name", className);
        intent.putExtra(ConcertDescription.UNIQUE_KEY, currentConcert);
        intent.putExtra("PairedManagerActivity", "com.github.rosjava.android_remocons.concert_remocon.ConcertRemocon");
        intent.putExtra("ChooserURI", uri.toString());
        intent.putExtra("Parameters", app.getParameters());  // YAML-formatted string

        // Remappings come as a messages list that make YAML parser crash, so we must digest if for him
        if ((app.getRemappings() != null) && (app.getRemappings().size() > 0)) {
            String remaps = "{";
            for (rocon_std_msgs.Remapping remap: app.getRemappings())
                remaps += remap.getRemapFrom() + ": " + remap.getRemapTo() + ", ";
            remaps = remaps.substring(0, remaps.length() - 2) + "}";
            intent.putExtra("Remappings", remaps);
        }

//      intent.putExtra("runningNodes", runningNodes);
//      intent.putExtra("PairedManagerActivity", "com.github.concertics_in_concert.rocon_android.concert_remocon.ConcertRemocon");

        try {
            Log.i("ConcertRemocon", "trying to start activity (action: " + className + " )");
            parentActivity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.i("ConcertRemocon", "activity not found for action: " + className);
        }

        final String installPackage = className.substring(0, className.lastIndexOf("."));

        Log.i("ConcertRemocon", "showing not-installed dialog.");

        // For now, just show a failure dialog.
        AlertDialog.Builder dialog = new AlertDialog.Builder(parentActivity);
        dialog.setTitle("Android app not installed.");
        dialog.setMessage("This concert app requires a client user interface app, but the applicable app"
                        + " is not installed. Would you like to install the app from the market place?");
        dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dlog, int i) {
                Uri uri = Uri.parse("market://details?id=" + installPackage);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                parentActivity.startActivity(intent);
            }
        }

        );
        dialog.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dlog, int i) {
                dlog.dismiss();
            }
        });
        dialog.show();
        return false;
    }
}