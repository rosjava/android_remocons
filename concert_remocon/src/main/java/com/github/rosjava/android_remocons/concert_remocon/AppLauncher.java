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

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.github.rosjava.android_remocons.concert_remocon.from_app_mng.ConcertAppsManager;
import com.github.rosjava.android_remocons.concert_remocon.from_app_mng.ConcertDescription;

import rocon_app_manager_msgs.PairingClient;

public class AppLauncher {
  static private final String CLIENT_TYPE = "android";


  /** Launch a client app for the given concert app. */
  static public boolean launch(final ConcertActivity parentActivity, concert_msgs.RemoconApp app, URI uri,ConcertDescription currentConcert,boolean runningNodes) {
//    ArrayList<ClientAppData> android_apps = new ArrayList<ClientAppData>();

    /*
    if (parentActivity instanceof ConcertRemocon) {
      ((ConcertRemocon)parentActivity).onAppClicked(app, app.getPairingClients().size() > 0);
    } else {
      Log.i("ConcertRemocon", "Could not launch because parent is not an appchooser");
      if (app.getPairingClients().size() == 0) {
        Log.e("ConcertRemocon", "Not launching application!!!");
        return false;
      }
    }

    if (app.getPairingClients().size() == 0) {
      return false;
    }
    
    Log.i("ConcertRemocon", "launching concert app " + app.getName() + " -> found " + app.getPairingClients().size()
        + " client apps.");

    // Loop over all possible client apps to find the android ones.
    for (int i = 0; i < app.getPairingClients().size(); i++) {
      PairingClient client_app = app.getPairingClients().get(i);
      if (client_app.getClientType() != null && client_app.getClientType().equals(CLIENT_TYPE)) {
     ClientAppData data = new ClientAppData(client_app);
     android_apps.add(data);
      }
    }

    Log.i("ConcertRemocon", "launching concert app " + app.getName() + " -> found " + android_apps.size()
        + " compatible android apps.");

    // TODO: filter out android apps which are not appropriate for
    // this device by looking at specific entries in the manager_data_
    // map of each app in android_apps.
    ArrayList<ClientAppData> appropriateAndroidApps = android_apps;


    // TODO: support multiple android apps
    if (appropriateAndroidApps.size() != 1) {
      AlertDialog.Builder dialog = new AlertDialog.Builder(parentActivity);
      dialog.setTitle("Wrong Number of Android Apps");
      dialog.setMessage("There are " + appropriateAndroidApps.size() + " valid android apps, not 1 as there should be.");
      dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dlog, int i) {
            dlog.dismiss();
          }});
      dialog.show();
      return false;
    }

    //TODO: this installs the last android app in the set.
    String className = "";

    // Loop over all android apps, trying to launch one.
    for (int i = 0; i < appropriateAndroidApps.size(); i++) {
      ClientAppData appData = appropriateAndroidApps.get(i);
      Intent intent = appData.createIntent();
      intent.putExtra(ConcertAppsManager.PACKAGE + ".concert_app_name", app.getName());
      intent.putExtra(ConcertDescription.UNIQUE_KEY, currentConcert);
      intent.putExtra("ChooserURI", uri.toString());
      intent.putExtra("runningNodes", runningNodes);
      intent.putExtra("PairedManagerActivity", "com.github.concertics_in_concert.rocon_android.concert_remocon.ConcertRemocon");
      try {
        className = intent.getAction();
        Log.i("ConcertRemocon", "trying to startActivity( action: " + intent.getAction() + " )");
        parentActivity.startActivity(intent);
        return true;
      } catch (ActivityNotFoundException e) {
        Log.i("ConcertRemocon", "activity not found for action: " + intent.getAction());
      }
    }

    final String installPackage = className.substring(0, className.lastIndexOf("."));

    Log.i("ConcertRemocon", "showing not-installed dialog.");

    // TODO:
    // Loop over all android apps, trying to install one. (??)
    // For now, just show a failure dialog.
    AlertDialog.Builder dialog = new AlertDialog.Builder(parentActivity);
    dialog.setTitle("Android app not installed.");
    dialog
        .setMessage("This concert app requires a client user interface app, but none of the applicable android apps are installed. Would you like to install the app from the market place?");
    dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dlog, int i) {
        Uri uri = Uri.parse("market://details?id=" + installPackage);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        parentActivity.startActivity(intent);
      }
    });
    dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dlog, int i) {
        dlog.dismiss();
      }
    });
    dialog.show();*/
    return false;
  }
}