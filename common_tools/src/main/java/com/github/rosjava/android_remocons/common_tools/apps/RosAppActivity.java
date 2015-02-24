/*
 * Copyright (C) 2013 OSRF.
 * Copyright (c) 2013, Yujin Robot.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.rosjava.android_remocons.common_tools.apps;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.github.robotics_in_concert.rocon_rosjava_core.rocon_interactions.InteractionMode;
import com.github.rosjava.android_remocons.common_tools.dashboards.Dashboard;
import com.github.rosjava.android_remocons.common_tools.master.MasterDescription;
import com.github.rosjava.android_remocons.common_tools.rocon.Constants;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.exception.RosRuntimeException;
import org.ros.namespace.NameResolver;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.yaml.snakeyaml.Yaml;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 * 
 * Modified to work in standalone, paired (robot) and concert modes.
 * Also now handles parameters and remappings.
 */
public abstract class RosAppActivity extends RosActivity {

    /*
      By default we assume the ros app activity is launched independently. The following attribute is
      used to identify when it has instead been launched by a controlling application (e.g. remocons)
      in paired, one-to-one, or concert mode.
     */
    private InteractionMode appMode = InteractionMode.STANDALONE;
	private String masterAppName = null;
	private String defaultMasterAppName = null;
	private String defaultMasterName = "";
    private String androidApplicationName; // descriptive helper only
    private String remoconActivity = null;  // The remocon activity to start when finishing this app
                                            // e.g. com.github.rosjava.android_remocons.robot_remocon.RobotRemocon
    private Serializable remoconExtraData = null; // Extra data for remocon (something inheriting from MasterDescription)

	private int dashboardResourceId = 0;
	private int mainWindowId = 0;
	private Dashboard dashboard = null;
	private NodeConfiguration nodeConfiguration;
	private NodeMainExecutor nodeMainExecutor;
	protected MasterNameResolver masterNameResolver;
    protected MasterDescription masterDescription;

    // By now params and remaps are only available for concert apps; that is, appMode must be CONCERT
    protected AppParameters params = new AppParameters();
    protected AppRemappings remaps = new AppRemappings();

    protected void setDashboardResource(int resource) {
		dashboardResourceId = resource;
	}

	protected void setMainWindowResource(int resource) {
		mainWindowId = resource;
	}

	protected void setDefaultMasterName(String name) {
		defaultMasterName = name;
	}

	protected void setDefaultAppName(String name) {
        defaultMasterAppName = name;
	}

	protected void setCustomDashboardPath(String path) {
		dashboard.setCustomDashboardPath(path);
	}

	protected RosAppActivity(String notificationTicker, String notificationTitle) {
		super(notificationTicker, notificationTitle);
        this.androidApplicationName = notificationTitle;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (mainWindowId == 0) {
			Log.e("RosApp",
					"You must set the dashboard resource ID in your RosAppActivity");
			return;
		}
		if (dashboardResourceId == 0) {
			Log.e("RosApp",
					"You must set the dashboard resource ID in your RosAppActivity");
			return;
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(mainWindowId);

		masterNameResolver = new MasterNameResolver();

		if (defaultMasterName != null) {
			masterNameResolver.setMasterName(defaultMasterName);
		}

// FAKE concert remocon invocation
//        MasterId mid = new MasterId("http://192.168.10.129:11311", "http://192.168.10.129:11311", "DesertStorm3", "WEP2", "yujin0610");
//        MasterDescription  md = MasterDescription.createUnknown(mid);
//        getIntent().putExtra(MasterDescription.UNIQUE_KEY, md);
//        getIntent().putExtra(AppManager.PACKAGE + ".concert_app_name", "KKKK");
//        getIntent().putExtra("PairedManagerActivity", "com.github.rosjava.android_remocons.rocon_remocon.Remocon");
//        getIntent().putExtra("ChooserURI", "http://192.168.10.129:11311");
//        getIntent().putExtra("Parameters", "{pickup_point: pickup}");
//        getIntent().putExtra("Remappings", "{ 'cmd_vel':'/robot_teleop/cmd_vel', 'image_color':'/robot_teleop/image_color/compressed_throttle' }");

// FAKE robot remocon invocation
//        MasterId mid = new MasterId("http://192.168.10.211:11311", "http://192.168.10.167:11311", "DesertStorm3", "WEP2", "yujin0610");
//        MasterDescription  md = MasterDescription.createUnknown(mid);
//        md.setMasterName("grieg");
//        md.setMasterType("turtlebot");
//        getIntent().putExtra(MasterDescription.UNIQUE_KEY, md);
//        getIntent().putExtra(AppManager.PACKAGE + ".paired_app_name", "KKKK");
//        getIntent().putExtra("PairedManagerActivity", "com.github.rosjava.android_remocons.robot_remocon.RobotRemocon");
////        getIntent().putExtra("RemoconURI", "http://192.168.10.129:11311");
//        getIntent().putExtra("Parameters", "{pickup_point: pickup}");
//        getIntent().putExtra("Remappings", "{ 'cmd_vel':'/robot_teleop/cmd_vel', 'image_color':'/robot_teleop/image_color/compressed_throttle' }");


        for (InteractionMode mode : InteractionMode.values()) {
            // The remocon specifies its type in the app name extra content string, useful information for the app
            masterAppName = getIntent().getStringExtra(Constants.ACTIVITY_SWITCHER_ID + "." + mode + "_app_name");
            if (masterAppName != null) {
                appMode = mode;
                break;
            }
        }

        if (masterAppName == null) {
            // App name extra content key not present on intent; no remocon started the app, so we are standalone app
            Log.e("RosApp", "We are running as standalone :(");
            masterAppName = defaultMasterAppName;
            appMode = InteractionMode.STANDALONE;
		}
        else
        {
            // Managed app; take from the intent all the fancy stuff remocon put there for us

            // Extract parameters and remappings from a YAML-formatted strings; translate into hash maps
            // We create empty maps if the strings are missing to avoid continuous if ! null checks
            Yaml yaml = new Yaml();

            String paramsStr = getIntent().getStringExtra("Parameters");
            String remapsStr = getIntent().getStringExtra("Remappings");

            Log.d("RosApp", "Parameters: " + paramsStr);
            Log.d("RosApp", "Remappings: " + remapsStr);

            try {
                if ((paramsStr != null) && (! paramsStr.isEmpty())) {
                    LinkedHashMap<String, Object> paramsList = (LinkedHashMap<String, Object>)yaml.load(paramsStr);
                    if (paramsList != null) {
                        params.putAll(paramsList);
                        Log.d("RosApp", "Parameters: " + paramsStr);
                    }
                }
            } catch (ClassCastException e) {
                Log.e("RosApp", "Cannot cast parameters yaml string to a hash map (" + paramsStr + ")");
                throw new RosRuntimeException("Cannot cast parameters yaml string to a hash map (" + paramsStr + ")");
            }

            try {
                if ((remapsStr != null) && (! remapsStr.isEmpty())) {
                    LinkedHashMap<String, String> remapsList = (LinkedHashMap<String, String>)yaml.load(remapsStr);
                    if (remapsList != null) {
                        remaps.putAll(remapsList);
                        Log.d("RosApp", "Remappings: " + remapsStr);
                    }
                }
            } catch (ClassCastException e) {
                Log.e("RosApp", "Cannot cast parameters yaml string to a hash map (" + remapsStr + ")");
                throw new RosRuntimeException("Cannot cast parameters yaml string to a hash map (" + remapsStr + ")");
            }

            remoconActivity = getIntent().getStringExtra("RemoconActivity");

            // Master description is mandatory on managed apps, as it contains master URI
            if (getIntent().hasExtra(MasterDescription.UNIQUE_KEY)) {
                // Keep a non-casted copy of the master description, so we don't lose the inheriting object
                // when switching back to the remocon. Not fully sure why this works and not if casting
                remoconExtraData = getIntent().getSerializableExtra(MasterDescription.UNIQUE_KEY);

                try {
                    masterDescription =
                            (MasterDescription) getIntent().getSerializableExtra(MasterDescription.UNIQUE_KEY);
                } catch (ClassCastException e) {
                    Log.e("RosApp", "Master description expected on intent on " + appMode + " mode");
                    throw new RosRuntimeException("Master description expected on intent on " + appMode + " mode");
                }
            }
            else {
                // TODO how should I handle these things? try to go back to remocon? Show a message?
                Log.e("RosApp", "Master description missing on intent on " + appMode + " mode");
                throw new RosRuntimeException("Master description missing on intent on " + appMode + " mode");
            }
        }

        if (dashboard == null) {
			dashboard = new Dashboard(this);
			dashboard.setView((LinearLayout) findViewById(dashboardResourceId),
					new LinearLayout.LayoutParams(
							LinearLayout.LayoutParams.WRAP_CONTENT,
							LinearLayout.LayoutParams.WRAP_CONTENT));
		}
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		this.nodeMainExecutor = nodeMainExecutor;
		nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
                .newNonLoopback().getHostAddress(), getMasterUri());

        if (appMode == InteractionMode.STANDALONE) {
            dashboard.setRobotName(masterNameResolver.getMasterName());
        }
        else {
            masterNameResolver.setMaster(masterDescription);
            dashboard.setRobotName(masterDescription.getMasterName());  // TODO dashboard not working for concerted apps (Issue #32)

            if (appMode == InteractionMode.PAIRED) {
                dashboard.setRobotName(masterDescription.getMasterType());
            }
        }

        // Run master namespace resolver
        nodeMainExecutor.execute(masterNameResolver, nodeConfiguration.setNodeName("masterNameResolver"));
        masterNameResolver.waitForResolver();

        nodeMainExecutor.execute(dashboard, nodeConfiguration.setNodeName("dashboard"));
    }

	protected NameResolver getMasterNameSpace() {
		return masterNameResolver.getMasterNameSpace();
	}

	protected void onAppTerminate() {
		RosAppActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new AlertDialog.Builder(RosAppActivity.this)
						.setTitle("App Termination")
						.setMessage(
								"The application has terminated on the server, so the client is exiting.")
						.setCancelable(false)
						.setNeutralButton("Exit",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
                                        RosAppActivity.this.finish();
									}
								}).create().show();
			}
		});
	}

	@Override
	public void startMasterChooser() {
		if (appMode == InteractionMode.STANDALONE) {
			super.startMasterChooser();
		} else {
			try {
                nodeMainExecutorService.setMasterUri(new URI(masterDescription.getMasterUri()));
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        RosAppActivity.this.init(nodeMainExecutorService);
                        return null;
                    }
                }.execute();
			} catch (URISyntaxException e) {
                // Remocon cannot be such a bastard to send as a wrong URI...
				throw new RosRuntimeException(e);
			}
		}
	}

	protected void releaseMasterNameResolver() {
		nodeMainExecutor.shutdownNodeMain(masterNameResolver);
	}

	protected void releaseDashboardNode() {
		nodeMainExecutor.shutdownNodeMain(dashboard);
	}

    /**
     * Whether this ros app activity should be responsible for
     * starting and stopping a paired master application.
     *
     * This responsibility is relinquished if the application
     * is controlled from a remocon, but required if the
     * android application is connecting and running directly.
     *
     * @return boolean : true if it needs to be managed.
     */
    private boolean managePairedRobotApplication() {
        return ((appMode == InteractionMode.STANDALONE) && (masterAppName != null));
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if (appMode != InteractionMode.STANDALONE) {  // i.e. it's a managed app
            Log.i("RosApp", "app terminating and returning control to the remocon.");
            // Restart the remocon, supply it with the necessary information and stop this activity
			Intent intent = new Intent();
			intent.putExtra(Constants.ACTIVITY_SWITCHER_ID + "." + appMode + "_app_name", "AppChooser");
            intent.putExtra(MasterDescription.UNIQUE_KEY, remoconExtraData);
            intent.setAction(remoconActivity);
			intent.addCategory("android.intent.category.DEFAULT");
			startActivity(intent);
			finish();
		} else {
            Log.i("RosApp", "backpress processing for RosAppActivity");
        }
		super.onBackPressed();
	}
}
