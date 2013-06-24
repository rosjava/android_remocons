/*
 * Copyright (C) 2013 Daniel Stonier.
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

package com.github.ros_java.android_remocons.robot_remocon;

import java.net.URI;
import java.net.URISyntaxException;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.namespace.NameResolver;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceResponseListener;

import com.github.ros_java.android_apps.application_management.AppManager;
import com.github.ros_java.android_apps.application_management.Dashboard;
import com.github.ros_java.android_apps.application_management.RobotDescription;
import com.github.ros_java.android_apps.application_management.RobotNameResolver;

import rocon_app_manager_msgs.StopAppResponse;

/**
 * Design goal of this activity should be to handle almost everything
 * necessary for interaction with a robot/rocon app manager. This
 * involves direct interactions on services and topics, and also
 * necessary data transfer required for correct display of the
 * 'robot' screen in the RobotRemocon.
 *
 * This used to be part of the old RosAppActivity, but
 * that used quite heavily a 'what am i' process to work
 * out whether it was an app or a controlling manager (appchooser
 * or remocon) with very separate workflows that didn't
 * take much advantage of code sharing.
 */
public abstract class RobotActivity extends RosActivity {

	private String robotAppName = null;
	private String defaultRobotAppName = null;
	private String defaultRobotName = null;
    /*
      By default we assume the remocon has just launched independantly, however
      it can be launched upon the closure of one of its children applications.
     */
    protected boolean fromApplication = false;  // true if it is a remocon activity getting control from a closing application

	private int dashboardResourceId = 0;
	private int mainWindowId = 0;
	private Dashboard dashboard = null;
	protected NodeConfiguration nodeConfiguration;
    protected NodeMainExecutor nodeMainExecutor;
	private URI uri;
	protected RobotNameResolver robotNameResolver;
	protected RobotDescription robotDescription;

	protected void setDashboardResource(int resource) {
		dashboardResourceId = resource;
	}

	protected void setMainWindowResource(int resource) {
		mainWindowId = resource;
	}

	protected void setDefaultRobotName(String name) {
		defaultRobotName = name;
	}

	protected void setDefaultAppName(String name) {
        defaultRobotAppName = name;
	}

	protected void setCustomDashboardPath(String path) {
		dashboard.setCustomDashboardPath(path);
	}

	protected RobotActivity(String notificationTicker, String notificationTitle) {
		super(notificationTicker, notificationTitle);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (mainWindowId == 0) {
			Log.e("RobotRemocon",
					"You must set the dashboard resource ID in your RobotActivity");
			return;
		}
		if (dashboardResourceId == 0) {
			Log.e("RobotRemocon",
					"You must set the dashboard resource ID in your RobotActivity");
			return;
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(mainWindowId);

		robotNameResolver = new RobotNameResolver();

		if (defaultRobotName != null) {
			robotNameResolver.setRobotName(defaultRobotName);
		}

		robotAppName = getIntent().getStringExtra(
				AppManager.PACKAGE + ".robot_app_name");
		if (robotAppName == null) {
			robotAppName = defaultRobotAppName;
        } else if (robotAppName.equals("AppChooser")) {
            Log.i("RobotRemocon", "reinitialising from a closing remocon application");
            fromApplication = true;
		} else {
			// do we need anything here? I think the first two cases cover everything
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

        // TODO - name resolution below is a mess and hard to debug, clean it up (DJS)
        // robotDescription will get set by the robot master chooser as it exits
        // (see onActivityResult in RobotRemocon.java). Check for that here.
		if (robotDescription != null) {
            robotNameResolver.setRobot(robotDescription);
			dashboard.setRobotName(robotDescription.getRobotType());
		} else if (fromApplication) {
            robotNameResolver.setRobotName(getIntent().getStringExtra("RobotName"));
            dashboard.setRobotName(getIntent().getStringExtra("RobotType"));
		}
		nodeMainExecutor.execute(robotNameResolver,
				nodeConfiguration.setNodeName("robotNameResolver"));
        robotNameResolver.waitForResolver();

        // Dashboard
        if (robotDescription == null) {
            dashboard.setRobotName(getRobotNameSpaceResolver().getNamespace()
                .toString());
        }
        nodeMainExecutor.execute(dashboard,
				nodeConfiguration.setNodeName("dashboard"));

        // Child application post-handling
        if (fromApplication) {
            stopApp();
        }
    }

	protected NameResolver getAppNameSpace() {
		return robotNameResolver.getAppNameSpace();
	}

	protected NameResolver getRobotNameSpaceResolver() {
		return robotNameResolver.getRobotNameSpace();
	}

    protected String getRobotNameSpace() {
        return robotNameResolver.getRobotNameSpace().getNamespace().toString();
    }

    @Override
	public void startMasterChooser() {
		if (fromApplication) {
            Intent intent = new Intent();
            intent.putExtra(AppManager.PACKAGE + ".robot_app_name",
                    "AppChooser");
            try {
                uri = new URI(getIntent().getStringExtra("ChooserURI"));
            } catch (URISyntaxException e) {
                throw new RosRuntimeException(e);
            }

            nodeMainExecutorService.setMasterUri(uri);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    RobotActivity.this.init(nodeMainExecutorService);
                    return null;
                }
            }.execute();
		} else {
            super.startMasterChooser();
		}

	}

	protected void stopApp() {
		Log.i("RobotRemocon", "android application stopping a rapp [" + robotAppName + "]");
		AppManager appManager = new AppManager(robotAppName,
				getRobotNameSpaceResolver());
		appManager.setFunction("stop");

		appManager
				.setStopService(new ServiceResponseListener<StopAppResponse>() {
					@Override
					public void onSuccess(StopAppResponse message) {
                        if ( message.getStopped() ) {
						    Log.i("RobotRemocon", "rapp stopped successfully");
                        } else {
                            Log.i("RobotRemocon", "stop rapp request rejected [" + message.getMessage() + "]");
                        }
					}

					@Override
					public void onFailure(RemoteException e) {
						Log.e("RobotRemocon", "rapp failed to stop when requested!");
					}
				});
		nodeMainExecutor.execute(appManager,
				nodeConfiguration.setNodeName("stop_app"));
	}

	protected void releaseRobotNameResolver() {
		nodeMainExecutor.shutdownNodeMain(robotNameResolver);
	}

	protected void releaseDashboardNode() {
		nodeMainExecutor.shutdownNodeMain(dashboard);
	}

	@Override
	protected void onDestroy() {
        Log.d("RobotRemocon", "onDestroy()");
		super.onDestroy();
	}
}
