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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Patterns;

import com.github.rosjava.android_apps.application_management.AppManager;
import com.github.rosjava.android_apps.application_management.ConcertDescription;
import com.github.rosjava.android_apps.application_management.RosAppActivity;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * A rewrite of robot_remocon/AppLauncher that...
 *  - works with concerts
 *  - can start web apps
 *  - headless; only reports error codes
 *
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public class AppLauncher {

    public enum Result {
        SUCCESS,
        NOT_INSTALLED,
        CANNOT_CONNECT,
        MALFORMED_URI,
        CONNECT_TIMEOUT,
        OTHER_ERROR;

        public String message;

        Result withMsg(String message) {
            this.message = message;
            return this;
        }
    }

    /**
     * Launch a client app for the given concert app.
     */
    static public Result launch(final Activity parent, final ConcertDescription concert,
                                final concert_msgs.RemoconApp app) {

        Log.i("AppLaunch", "launching concert app " + app.getDisplayName() + " on service " + app.getServiceName());

        // On android apps, app name will be an intent action, while for web apps it will be its URL
        if (Patterns.WEB_URL.matcher(app.getName()).matches() == true) {
            return launchWebApp(parent, concert, app);
        }
        else {
            return launchAndroidApp(parent, concert, app);
        }
    }


    /**
     * Launch a client android app for the given concert app.
     */
    static private Result launchAndroidApp(final Activity parent, final ConcertDescription concert,
                                           final concert_msgs.RemoconApp app) {

        // Create the Intent from rapp's name, pass it parameters and remaps and start it
        String appName = app.getName();
        Intent intent = new Intent(appName);

        // Copy all app data to "extra" data in the intent.
        intent.putExtra(AppManager.PACKAGE + "." + RosAppActivity.AppMode.CONCERT + "_app_name", appName);
        intent.putExtra(ConcertDescription.UNIQUE_KEY, concert);
        intent.putExtra("RemoconActivity", "com.github.rosjava.android_remocons.concert_remocon.ConcertRemocon"); // TODO must be a RoconConstant!
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
            Log.i("AppLaunch", "trying to start activity (action: " + appName + " )");
            parent.startActivity(intent);
            return Result.SUCCESS;
        } catch (ActivityNotFoundException e) {
            Log.i("AppLaunch", "activity not found for action: " + appName);
        }
        return Result.NOT_INSTALLED.withMsg("Android app not installed");
    }


    /**
     * Launch a client web app for the given concert app.
     */
    static private Result launchWebApp(final Activity parent, final ConcertDescription concert,
                                       final concert_msgs.RemoconApp app) {
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
                return Result.CANNOT_CONNECT.withMsg(result);
            }

            // We pass concert URL, parameters and remaps as URL parameters
            String appUriStr = app.getName();
            appUriStr += "?" + "MasterURI=" + concert.getMasterUri();
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

            Log.i("AppLaunch", "trying to start web app (URI: " + appUriStr + ")");
            parent.startActivity(intent);
            return Result.SUCCESS;
        }
        catch (URISyntaxException e) {
            return Result.MALFORMED_URI.withMsg("Cannot convert URL into URI. " + e.getMessage());
        }
        catch (MalformedURLException e)
        {
            return Result.MALFORMED_URI.withMsg("App URL is not valid. " + e.getMessage());
        }
        catch (ActivityNotFoundException e) {
            // This cannot happen for a web site, right? must mean that I have no web browser!
            return Result.NOT_INSTALLED.withMsg("Activity not found for view action??? muoia???");
        }
        catch (TimeoutException e)
        {
            return Result.CONNECT_TIMEOUT.withMsg("Timeout waiting for app");
        }
        catch (Exception e)
        {
            return Result.OTHER_ERROR.withMsg(e.getMessage());
        }
    }
}
