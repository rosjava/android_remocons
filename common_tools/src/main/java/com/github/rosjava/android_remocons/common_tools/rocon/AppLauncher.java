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

package com.github.rosjava.android_remocons.common_tools.rocon;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.util.Patterns;

import com.github.robotics_in_concert.rocon_rosjava_core.rocon_interactions.InteractionMode;
import com.github.rosjava.android_remocons.common_tools.master.RoconDescription;

import org.yaml.snakeyaml.Yaml;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

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
        NOTHING,
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

    public enum AppType {
        NATIVE,
        URL,
        WEB_URL,
        WEB_APP,
        NOTHING;
    }
    /**
     * Launch a client app for the given concert app.
     */
    static public Result launch(final Activity parent, final RoconDescription concert,
                                final rocon_interaction_msgs.Interaction app){
        Log.i("AppLaunch", "launching concert app " + app.getDisplayName() + " on service " + app.getNamespace());
        // On android apps, app name will be an intent action, while for web apps it will be its URL
        AppType app_type = checkAppType(app.getName());
        if(app_type == AppType.URL){
            return launchUrl(parent, concert, app);
        }
        else if(app_type == AppType.WEB_URL){
            return launchWebUrl(parent, concert, app);
        }
        else if(app_type == AppType.WEB_APP){
            return launchWebApp(parent, concert, app);
        }
        else if(app_type == AppType.NATIVE){
            return launchAndroidApp(parent, concert, app);
        }
        else if(app_type == AppType.NOTHING){
            return Result.NOTHING;
        }
        else{
            return Result.NOTHING;
        }
    }
    /**
     * Check the application name whether web_url(*) or web_app(*)
     */
    static public AppType checkAppType(String app_name){
        String web_url_desc = "web_url(";
        String web_app_desc = "web_app(";
        if (Patterns.WEB_URL.matcher(app_name).matches() == true) {
            return AppType.URL;
        }
        else if(app_name.length() == 0){
            return AppType.NOTHING;
        }
        else if(app_name.contains(web_app_desc)){
            return AppType.WEB_APP;
        }
        else if(app_name.contains(web_url_desc)){
            return AppType.WEB_URL;
        }
        else{
            return AppType.NATIVE;
        }
    }

    /**
     * Launch a client android app for the given concert app.
     */
    static private Result launchAndroidApp(final Activity parent, final RoconDescription concert,
                                           final rocon_interaction_msgs.Interaction app) {

        // Create the Intent from rapp's name, pass it parameters and remaps and start it
        Result result  = Result.OTHER_ERROR;
        String appName = app.getName();
        Intent intent = new Intent(appName);
        // Copy all app data to "extra" data in the intent.
        intent.putExtra(Constants.ACTIVITY_SWITCHER_ID + "." + InteractionMode.CONCERT + "_app_name", appName);
        intent.putExtra(RoconDescription.UNIQUE_KEY, concert);
        intent.putExtra("RemoconActivity", Constants.ACTIVITY_ROCON_REMOCON);
        intent.putExtra("Parameters", app.getParameters());  // YAML-formatted string

        // Remappings come as a messages list that make YAML parser crash, so we must digest if for him
        if ((app.getRemappings() != null) && (app.getRemappings().size() > 0)) {
            String remaps = "{";
            for (rocon_std_msgs.Remapping remap: app.getRemappings())
                remaps += remap.getRemapFrom() + ": " + remap.getRemapTo() + ", ";
            remaps = remaps.substring(0, remaps.length() - 2) + "}";
            intent.putExtra("Remappings", remaps);
        }
        try {
            Log.i("AppLaunch", "trying to start activity (action: " + appName + " )");
            parent.startActivity(intent);
            result = Result.SUCCESS;
        } catch (ActivityNotFoundException e) {
            Log.i("AppLaunch", "activity not found for action and find package name: " + appName);
            result = launchAndroidAppWithPkgName(parent, concert, app);
        }
        return result;
    }

    static private Result launchAndroidAppWithPkgName(final Activity parent, final RoconDescription concert,
                                           final rocon_interaction_msgs.Interaction app) {
        // Create the Intent from rapp's name, pass it parameters and remaps and start it
        String appName = app.getName();
        PackageManager manager = parent.getPackageManager();
        Intent intent = manager.getLaunchIntentForPackage(appName);
        if(intent != null) {
            // Copy all app data to "extra" data in the intent.
            intent.putExtra(Constants.ACTIVITY_SWITCHER_ID + "." + InteractionMode.CONCERT + "_app_name", appName);
            intent.putExtra(RoconDescription.UNIQUE_KEY, concert);
            intent.putExtra("RemoconActivity", Constants.ACTIVITY_ROCON_REMOCON);
            intent.putExtra("Parameters", app.getParameters());  // YAML-formatted string

            // Remappings come as a messages list that make YAML parser crash, so we must digest if for him
            if ((app.getRemappings() != null) && (app.getRemappings().size() > 0)) {
                String remaps = "{";
                for (rocon_std_msgs.Remapping remap : app.getRemappings())
                    remaps += remap.getRemapFrom() + ": " + remap.getRemapTo() + ", ";
                remaps = remaps.substring(0, remaps.length() - 2) + "}";
                intent.putExtra("Remappings", remaps);
            }
            try {
                Log.i("AppLaunch", "trying to start activity (action: " + appName + " )");
                parent.startActivity(intent);
                return Result.SUCCESS;
            } catch (ActivityNotFoundException e) {
                Log.i("AppLaunch", "activity not found for action: " + appName);
            }
        }
        return Result.NOT_INSTALLED.withMsg("Android app not installed");
    }

    /**
     * Launch a client url for the given concert app.
     */
    static private Result launchUrl (final Activity parent, final RoconDescription concert,
                                       final rocon_interaction_msgs.Interaction app) {
        try
        {
            // Validate the URL before starting anything
            String app_name = "";
            app_name = app.getName();
            URL appURL = new URL(app_name);
            //2014.12.03 comment by dwlee
            //reason of blocking, Not necessary in web app launcher.
            /*
            AsyncTask<URL, Void, String> asyncTask = new AsyncTask<URL, Void, String>() {
                @Override
                protected String doInBackground(URL... urls) {
                    try {
                        HttpURLConnection urlConnection = (HttpURLConnection)urls[0].openConnection();
                        int unused_responseCode = urlConnection.getResponseCode();
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
            */

            // We pass concert URL, parameters and remaps as URL parameters
            String appUriStr = app_name;
            Uri appURI =  Uri.parse(appUriStr);

            // Create an action view intent and pass rapp's name + extra information as URI
            Intent intent = new Intent(Intent.ACTION_VIEW, appURI);
            intent.putExtra(Constants.ACTIVITY_SWITCHER_ID + "." + InteractionMode.CONCERT + "_app_name",app_name);

            Log.i("AppLaunch", "trying to start web app (URI: " + appUriStr + ")");
            parent.startActivity(intent);
            return Result.SUCCESS;
        }
        catch (MalformedURLException e)
        {
            return Result.MALFORMED_URI.withMsg("App URL is not valid. " + e.getMessage());
        }
        catch (ActivityNotFoundException e) {
            // This cannot happen for a web site, right? must mean that I have no web browser!
            return Result.NOT_INSTALLED.withMsg("Activity not found for view action??? muoia???");
        } catch (Exception e)
        {
            return Result.OTHER_ERROR.withMsg(e.getMessage());
        }
    }

    /**
     * Launch a client web url for the given concert app.
     */
    static private Result launchWebUrl(final Activity parent, final RoconDescription concert,
                                       final rocon_interaction_msgs.Interaction app) {
        try
        {
            // Validate the URL before starting anything
            String app_name = "";
            String app_type = "web_url";
            app_name = app.getName().substring(app_type.length()+1,app.getName().length()-1);

            URL appURL = new URL(app_name);

            //2014.12.03 comment by dwlee
            //reason of blocking, Not necessary in web app launcher.
            /*
            AsyncTask<URL, Void, String> asyncTask = new AsyncTask<URL, Void, String>() {
                @Override
                protected String doInBackground(URL... urls) {
                    try {
                        HttpURLConnection urlConnection = (HttpURLConnection)urls[0].openConnection();
                        int unused_responseCode = urlConnection.getResponseCode();
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
            */

            // We pass concert URL, parameters and remaps as URL parameters
            String appUriStr = app_name;
            Uri appURI =  Uri.parse(appUriStr);

            // Create an action view intent and pass rapp's name + extra information as URI
            Intent intent = new Intent(Intent.ACTION_VIEW, appURI);
            intent.putExtra(Constants.ACTIVITY_SWITCHER_ID + "." + InteractionMode.CONCERT + "_app_name",app_name);

            Log.i("AppLaunch", "trying to start web url (URI: " + appUriStr + ")");
            parent.startActivity(intent);
            return Result.SUCCESS;
        }
        catch (MalformedURLException e)
        {
            return Result.MALFORMED_URI.withMsg("App URL is not valid. " + e.getMessage());
        }
        catch (ActivityNotFoundException e) {
            // This cannot happen for a web site, right? must mean that I have no web browser!
            return Result.NOT_INSTALLED.withMsg("Activity not found for view action??? muoia???");
        }
        catch (Exception e)
        {
            return Result.OTHER_ERROR.withMsg(e.getMessage());
        }
    }
    /**
     * Launch a client web app for the given concert app.
     */
    static private Result launchWebApp(final Activity parent, final RoconDescription concert,
                                       final rocon_interaction_msgs.Interaction app) {
        try
        {
            // Validate the URL before starting anything
            String app_name = "";
            String app_type = "";
            app_type = "web_app";
            app_name = app.getName().substring(app_type.length()+1,app.getName().length()-1);
            URL appURL = new URL(app_name);
            //2014.05.27 comment by dwlee
            //reason of blocking, Not necessary in web app launcher.
            /*
            Log.i("AppLaunch", "Connection test (URI: " + app_name + ")");
            AsyncTask<URL, Void, String> asyncTask = new AsyncTask<URL, Void, String>() {
                @Override
                protected String doInBackground(URL... urls) {
                    try {
                        HttpURLConnection urlConnection = (HttpURLConnection)urls[0].openConnection();
                        int unused_responseCode = urlConnection.getResponseCode();
                        urlConnection.disconnect();
                        return urlConnection.getResponseMessage();
                    }
                    catch (IOException e) {
                        return e.getMessage();
                    }
                }
            }.execute(appURL);

            String result = asyncTask.get(15, TimeUnit.SECONDS);

            if (result == null || (result.startsWith("OK") == false && result.startsWith("ok") == false)) {
                Log.i("AppLaunch", "Connection test Fail (URI: " + app_name + ")");
                return Result.CANNOT_CONNECT.withMsg(result);
            }
            Log.i("AppLaunch", "Connection test Success (URI: " + app_name + ")");
            */

            // We pass concert URL, parameters and remaps as URL parameters
            String appUriStr = app_name;
            String interaction_data = "{";
            //add remap
            String remaps = "\"remappings\": {";
            if ((app.getRemappings() != null) && (app.getRemappings().size() > 0)) {
                for (rocon_std_msgs.Remapping remap: app.getRemappings())
                    remaps += "\"" + remap.getRemapFrom() + "\":\"" + remap.getRemapTo() + "\",";
                remaps = remaps.substring(0, remaps.length() - 1) + "}";
            }
            else{
                remaps+="}";
            }
            remaps += ",";
            interaction_data += remaps;

            //add displayname
            String displayname = "\"display_name\":";
            if ((app.getDisplayName() != null) && (app.getDisplayName().length() > 0)) {
                displayname += "\"" + app.getDisplayName() +"\"";
            }
            displayname +=",";
            interaction_data += displayname;

            //add parameters
            String parameters = "\"parameters\": {";
            if ((app.getParameters() != null) && (app.getParameters().length() > 2)) {
                Yaml yaml = new Yaml();
                Map<String, String> params = (Map<String, String>) yaml.load(app.getParameters());
                for( String key : params.keySet() ) {
                    parameters += "\"" + key + "\":\"" + String.valueOf(params.get(key))+"" + "\",";
                }
                parameters = parameters.substring(0, parameters.length() - 1);
            }

            parameters +="}";
            interaction_data += parameters;
            interaction_data +="}";

            appUriStr = appUriStr + "?" + "interaction_data=" + URLEncoder.encode(interaction_data);
            appURL.toURI(); // throws URISyntaxException if fails; probably a redundant check
            Uri appURI =  Uri.parse(appUriStr);

            // Create an action view intent and pass rapp's name + extra information as URI
            Intent intent = new Intent(Intent.ACTION_VIEW, appURI);
            intent.putExtra(Constants.ACTIVITY_SWITCHER_ID + "." + InteractionMode.CONCERT + "_app_name",app_name);

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
        catch (Exception e)
        {
            return Result.OTHER_ERROR.withMsg(e.getMessage());
        }
    }
}
