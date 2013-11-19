/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * Copyright (c) 2013, OSRF.
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

package com.github.rosjava.android_remocons.common_tools;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Patterns;

import com.github.rosjava.android_apps.application_management.ConcertDescription;


/**
 * A rewrite of robot_remocon/AppLauncher to work with concerts. Also extended to start web apps.
 *
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public class AppLauncher {

    /**
     * Launch a client app for the given concert app.
     */
    static public boolean launch(final Activity parentActivity, concert_msgs.RemoconApp app,
                                 URI masterUri, ConcertDescription currentConcert) {  // TODO  uri is also in concert (as str)!

//        if (parentActivity instanceof ConcertRemocon) {
//            ((ConcertRemocon) parentActivity).onAppClicked(app);
//        } else {
//            Log.i("ConcertRemocon", "Could not launch because parent is not an appchooser");
//            return false;
//        }

        Log.i("ConcertRemocon", "launching concert app " + app.getDisplayName() + " on service " + app.getServiceName());

        // On android apps, app name will be an intent action, while for web apps it will be its URL
        if (Patterns.WEB_URL.matcher(app.getName()).matches() == true) {
            return launchWebApp(parentActivity, app, masterUri, currentConcert);
        }
        else {
            return launchAndroidApp(parentActivity, app, masterUri, currentConcert);
        }
    }


    /**
     * Launch a client android app for the given concert app.
     */
    static public boolean launchAndroidApp(final Activity parentActivity, concert_msgs.RemoconApp app,
                                           URI masterUri, ConcertDescription currentConcert) {

        // Create the Intent from rapp's name, pass it parameters and remaps and start it
        String appName = app.getName();
        Intent intent = new Intent(appName);

        // Copy all app data to "extra" data in the intent.
        intent.putExtra(AppsManager_BAK.PACKAGE + ".concert_app_name", appName);
        intent.putExtra(ConcertDescription.UNIQUE_KEY, currentConcert);
        intent.putExtra("PairedManagerActivity", "com.github.rosjava.android_remocons.concert_remocon.ConcertRemocon");
        intent.putExtra("ChooserURI", masterUri.toString());
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
            Log.i("ConcertRemocon", "trying to start activity (action: " + appName + " )");
            parentActivity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.i("ConcertRemocon", "activity not found for action: " + appName);
        }

        final String installPackage = appName.substring(0, appName.lastIndexOf("."));

        Log.i("ConcertRemocon", "Showing not-installed dialog.");

        // Show an "app not-installed" dialog and ask for going to play store to download the missing app
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


    /**
     * Launch a client web app for the given concert app.
     */
    static public boolean launchWebApp(final Activity parentActivity, concert_msgs.RemoconApp app,
                                       URI masterUri, ConcertDescription currentConcert) {
        try
        {
            // Validate the URL before starting anything
            URL appURL = new URL(app.getName());

            AsyncTask<URL, Void, String> asyncTask = new AsyncTask<URL, Void, String>() {
                @Override
                protected String doInBackground(URL... urls) {
                    try {
                        HttpURLConnection urlConnection = (HttpURLConnection)urls[0].openConnection();
                        int responseCode = urlConnection.getResponseCode();
                        urlConnection.disconnect();
                        return urlConnection.getResponseMessage();
                    }
                    catch (IOException e) {
                        return e.getMessage();
                    }
                }
            }.execute(appURL);
            String result = asyncTask.get(5, TimeUnit.SECONDS);
            if (result == null || (result.startsWith("OK") == false && result.startsWith("ok") == false)) {
                showErrorDialog(parentActivity, "Cannot start web app", result);
                return false;
            }

            // We pass concert URL, parameters and remaps as URL parameters
            String appUriStr = app.getName();
            appUriStr += "?" + "MasterURI=" + masterUri.toString();
            if ((app.getParameters() != null) && (app.getParameters().length() > 0)) {
                appUriStr += "&" + "params=" + URLEncoder.encode(app.getParameters());
            }

            // Remappings come as a messages list that make YAML parser crash, so we must digest if for him
            // TODO Single quotes seem to be necessary, but I didn't confirm yet
            if ((app.getRemappings() != null) && (app.getRemappings().size() > 0)) {
                String remaps = "{";
                for (rocon_std_msgs.Remapping remap: app.getRemappings())
                    remaps += "\'" + remap.getRemapFrom() + "\':\'" + remap.getRemapTo() + "\',";
                remaps = remaps.substring(0, remaps.length() - 1) + "}";
                appUriStr += "&" + "remaps=" + URLEncoder.encode(remaps);
            }

            appURL = new URL(appUriStr);
            appURL.toURI(); // throws URISyntaxException if fails; probably a redundant check
            Uri appURI =  Uri.parse(appUriStr);

            // Create an action view intent and pass rapp's name + extra information as URI
            Intent intent = new Intent(Intent.ACTION_VIEW, appURI);

            Log.i("ConcertRemocon", "trying to start web app (URI: " + appUriStr + ")");
            parentActivity.startActivity(intent);
            return true;
        }
        catch (URISyntaxException e) {
            showErrorDialog(parentActivity, "Cannot start web app", "Cannot convert URL into URI:\n" + e.getMessage());
            return false;
        }
        catch (MalformedURLException e)
        {
            showErrorDialog(parentActivity, "Cannot start web app", "App URL is not valid:\n" + e.getMessage());
            return false;
        }
        catch (ActivityNotFoundException e) {
            // This cannot happen for a web site, right? must mean that I have no web browser!
            showErrorDialog(parentActivity, "Cannot start web app", "activity not found for view action???");
            return false;
        }
        catch (TimeoutException e)
        {
            showErrorDialog(parentActivity, "Cannot start web app", "Timeout waiting for app");
            return false;
        }
        catch (Exception e)
        {
            showErrorDialog(parentActivity, "Cannot start web app", e.getMessage());
            return false;
        }
    }

    private static void showErrorDialog(final Activity parentActivity, String title, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(parentActivity);
//        dialog.setIcon(R.drawable.failure_small);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlog, int i) {
                // nothing todo?
            }
        });
        dialog.show();
    }
}
