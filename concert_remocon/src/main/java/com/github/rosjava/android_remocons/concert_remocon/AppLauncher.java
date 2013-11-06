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
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.github.rosjava.android_apps.application_management.ConcertDescription;
import com.github.rosjava.android_remocons.concert_remocon.from_app_mng.ConcertAppsManager;

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
        intent.putExtra(ConcertAppsManager.PACKAGE + ".concert_app_name", className);
        intent.putExtra(ConcertDescription.UNIQUE_KEY, currentConcert);
        intent.putExtra("PairedManagerActivity", "com.github.rosjava.android_remocons.concert_remocon.ConcertRemocon");
        intent.putExtra("ChooserURI", uri.toString());
//      intent.putExtra("Parameters", app.getParameters());
//      intent.putExtra("Remappings", app.getRemappings());

        if ((app.getParameters() != null) && (app.getParameters().size() > 0)) {
            ArrayList<String> param_keys = new ArrayList<String>(app.getParameters().size());
            ArrayList<String> param_values = new ArrayList<String>(app.getParameters().size());

            for (rocon_std_msgs.KeyValue param: app.getParameters()) {
                param_keys.add(param.getKey());
                param_values.add(param.getValue());
            }

            intent.putExtra("ParametersKeys", param_keys);
            intent.putExtra("ParametersValues", param_values);
        }

        if ((app.getRemappings() != null) && (app.getRemappings().size() > 0)) {
            ArrayList<String> remap_from = new ArrayList<String>(app.getRemappings().size());
            ArrayList<String> remap_to = new ArrayList<String>(app.getRemappings().size());

            for (rocon_std_msgs.Remapping remap: app.getRemappings()) {
                remap_from.add(remap.getRemapFrom());
                remap_to.add(remap.getRemapTo());
            }

            intent.putExtra("RemappingsFrom", remap_from);
            intent.putExtra("RemappingsTo", remap_to);
        }
//
//
//// TODO extract both list to veryfy it works!
//
//        ArrayList<String> param_keys = intent.getStringArrayListExtra("ParametersKeys");
//        ArrayList<String> param_values = intent.getStringArrayListExtra("ParametersValues");
//
//        if (((param_keys != null) && (param_keys.size() > 0)) &&
//            ((param_values != null) && (param_values.size() > 0))) {
//            if (param_keys.size() != param_values.size()) {
//                Log.e("ApplicationManagement",
//                        "Remappings from and to lists sizes doesn't match (" + param_keys.size() + " != " + param_values.size());
//                return false;
//            }
//
//            ArrayList<Parameter> params = new ArrayList<Parameter>(param_keys.size());
//            for (int i = 0; i < param_keys.size(); i++) {
//                params.add(new Parameter(param_keys.get(i), param_values.get(i)));
//            }
//        }
//
//
//
//        ArrayList<String> remap_from = intent.getStringArrayListExtra("RemappingsFrom");
//        ArrayList<String> remap_to = intent.getStringArrayListExtra("RemappingsTo");
//
//        if (((remap_from != null) && (remap_from.size() > 0)) &&
//            ((remap_to != null) && (remap_to.size() > 0))) {
//            if (remap_from.size() != remap_to.size()) {
//                Log.e("ApplicationManagement",
//                      "Remappings from and to lists sizes doesn't match (" + remap_from.size() + " != " + remap_to.size());
//                return false;
//            }
//
//            ArrayList<Remapping> remaps = new ArrayList<Remapping>(remap_from.size());
//            for (int i = 0; i < remap_from.size(); i++) {
//                remaps.add(new Remapping(remap_from.get(i), remap_to.get(i)));
//            }
//        }


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