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

package com.github.ros_java.android_remocons.robot_remocon;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import org.ros.address.InetAddressFactory;
import org.ros.android.NodeMainExecutorService;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.message.MessageListener;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceResponseListener;
import com.github.ros_java.android_apps.application_management.AppManager;
import com.github.ros_java.android_apps.application_management.ControlChecker;
import com.github.ros_java.android_apps.application_management.MasterChecker;
import com.github.ros_java.android_apps.application_management.RobotId;
import com.github.ros_java.android_apps.application_management.RobotDescription;
import com.github.ros_java.android_apps.application_management.WifiChecker;
import com.github.ros_java.android_apps.application_management.rapp_manager.InvitationServiceClient;

import rocon_app_manager_msgs.App;
import rocon_app_manager_msgs.AppList;
import rocon_app_manager_msgs.StartAppResponse;
import rocon_app_manager_msgs.ErrorCodes;
import rocon_app_manager_msgs.StopAppResponse;

public class RobotRemocon extends RobotActivity {

    /* startActivityForResult Request Codes */
	private static final int ROBOT_MASTER_CHOOSER_REQUEST_CODE = 1;

	private static final int MULTI_RAPP_DISABLED = 1;
	private static final int CLOSE_EXISTING = 0;

	private TextView robotNameView;
	private ArrayList<App> availableAppsCache;
	private ArrayList<App> runningAppsCache;
	private Button deactivate;
	private Button stopAppsButton;
	private Button exchangeButton;
	private ProgressDialog progress;
	private ProgressDialogWrapper progressDialog;
	private AlertDialogWrapper wifiDialog;
	private AlertDialogWrapper evictDialog;
	private AlertDialogWrapper errorDialog;
    protected AppManager listAppsSubscriber;
	private boolean alreadyClicked = false;
	private boolean validatedRobot;
	private boolean runningNodes = false;
	private long availableAppsCacheTime;

	private void stopProgress() {
        Log.i("RobotRemocon", "Stopping the spinner");
		final ProgressDialog temp = progress;
		progress = null;
		if (temp != null) {
			runOnUiThread(new Runnable() {
				public void run() {
					temp.dismiss();
				}
			});
		}
	}

	/**
	 * Wraps the alert dialog so it can be used as a yes/no function
	 */
	private class AlertDialogWrapper {
		private int state;
		private AlertDialog dialog;
		private RobotActivity context;

		public AlertDialogWrapper(RobotActivity context,
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

		public AlertDialogWrapper(RobotActivity context,
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
			dialog = null;
		}
	}

	/**
	 * Wraps the progress dialog so it can be used to show/vanish easily
	 */
	private class ProgressDialogWrapper {
		private ProgressDialog progressDialog;
		private RobotActivity activity;

		public ProgressDialogWrapper(RobotActivity activity) {
			this.activity = activity;
			progressDialog = null;
		}

		public void dismiss() {
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
			progressDialog = null;
		}

		public void show(String title, String text) {
			if (progressDialog != null) {
				this.dismiss();
			}
			progressDialog = ProgressDialog.show(activity, title, text, true,
					true);
			progressDialog.setCancelable(false);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		}
	}

	public RobotRemocon() {
		super("RobotRemocon", "RobotRemocon");
		availableAppsCacheTime = 0;
		availableAppsCache = new ArrayList<App>();
		runningAppsCache = new ArrayList<App>();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setDashboardResource(R.id.top_bar);
		setMainWindowResource(R.layout.main);
		super.onCreate(savedInstanceState);

		robotNameView = (TextView) findViewById(R.id.robot_name_view);
		deactivate = (Button) findViewById(R.id.deactivate_robot);
		deactivate.setVisibility(deactivate.GONE);
		stopAppsButton = (Button) findViewById(R.id.stop_applications);
		stopAppsButton.setVisibility(stopAppsButton.GONE);
		exchangeButton = (Button) findViewById(R.id.exchange_button);
		exchangeButton.setVisibility(deactivate.GONE);
	}

    /**
     * This gets processed as soon as the application returns it
     * with a uri - this is either as a result of the master chooser
     * or as in the case when it has been relaunched by an application,
     * from intents set by the application.
     *
     * Here we configure the remocon environment for a particular robot,
     * listing apps and providing the required triggers for interacting
     * with that robot.
     *
     * @param nodeMainExecutor
     */
	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {

		super.init(nodeMainExecutor);

        // set up a subscriber to the applist topic so it can check
        // status of available and running apps.
        listAppsSubscriber = new AppManager("", getRobotNameSpaceResolver());
        listAppsSubscriber.setAppListSubscriber(new MessageListener<AppList>() {
            @Override
            public void onNewMessage(AppList message) {
                availableAppsCache = (ArrayList<App>) message.getAvailableApps();
                runningAppsCache = (ArrayList<App>) message.getRunningApps();
                ArrayList<String> runningAppsNames = new ArrayList<String>();
                int i = 0;
                for (i = 0; i < availableAppsCache.size(); i++) {
                    App item = availableAppsCache.get(i);
                    ArrayList<String> clients = new ArrayList<String>();
                    for (int j = 0; j < item.getPairingClients().size(); j++) {
                        clients.add(item.getPairingClients().get(j)
                                .getClientType());
                    }
                    if (!clients.contains("android")
                            && item.getPairingClients().size() != 0) {
                        availableAppsCache.remove(i);
                    }
                    if (item.getPairingClients().size() == 0) {
                        Log.i("RobotRemocon",
                                "Item name: " + item.getName());
                        runningAppsNames.add(item.getName());
                    }
                }
                Log.i("RobotRemocon", "AppList Publication: "
                        + availableAppsCache.size() + " apps");
                availableAppsCacheTime = System.currentTimeMillis();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateAppList(availableAppsCache,
                                runningAppsCache);
                    }
                });
            }
        });
        listAppsSubscriber.setFunction("list_apps");
        nodeMainExecutor.execute(listAppsSubscriber,
                nodeConfiguration.setNodeName("list_apps_subscriber_node"));
    }

    /**
     * Initialise from an intent triggered by either the returning robot
     * chooser or a robot app.
     *
     * Validation will occur slightly differently in both cases - when
     * returning from the app, it will assume its already
     * in control and skip the invitation step (see validateRobot).
     *
     * This eventually passes control back to the generic init function
     * (TODO: can probably incorporate this into that function).
     *
     * @param intent
     */
    void init(Intent intent) {
        URI uri;
        try {
            robotDescription = (RobotDescription) intent
                    .getSerializableExtra(RobotDescription.UNIQUE_KEY);

            robotNameResolver.setRobotName(robotDescription
                    .getRobotName());

            validatedRobot = false;
            validateRobot(robotDescription.getRobotId());

            uri = new URI(robotDescription.getRobotId()
                    .getMasterUri());
        } catch (URISyntaxException e) {
            throw new RosRuntimeException(e);
        }
        nodeMainExecutorService.setMasterUri(uri);
        // Run init() in a new thread as a convenience since it often
        // requires network access. This would be more robust if it
        // had a failure handler for uncontactable errors (override
        // onPostExecute) that occurred when calling init. In reality
        // this shouldn't happen often - only when the connection
        // is unavailable inbetween validating and init'ing.
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                while (!validatedRobot) {
                    // should use a sleep here to avoid burnout
                }
                RobotRemocon.this.init(nodeMainExecutorService);
                return null;
            }
        }.execute();
    }
    /**
     * The main result gathered here is that from the robot master chooser
     * which is started on top of the initial RobotRemocon Activity.
     * This proceeds to then set the uri and trigger the init() calls.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_CANCELED) {
			finish();
		} else if (resultCode == RESULT_OK) {
			if (requestCode == ROBOT_MASTER_CHOOSER_REQUEST_CODE) {
                init(data);
            } else {
				// Without a master URI configured, we are in an unusable state.
				nodeMainExecutorService.shutdown();
				finish();
			}
		}
	}

    /**
     * This is an override which diverts the usual startup once a node is
     * connected. Typically this would go to the master chooser, however
     * here we are sometimes returning from one of its child apps (in which
     * case it doesn't have to go choosing a robot). In that case, send
     * it directly to the robot validation and initialisation steps.
     */
	@Override
	public void startMasterChooser() {
		if (!fromApplication) {
			super.startActivityForResult(new Intent(this,
					RobotMasterChooser.class),
					ROBOT_MASTER_CHOOSER_REQUEST_CODE);
		} else {
            if (getIntent().hasExtra(RobotDescription.UNIQUE_KEY)) {
                init(getIntent());
                Log.i("RobotRemocon", "closing remocon application and successfully retrieved the robot description via intents.");
            } else {
                Log.e("RobotRemocon", "closing remocon application didn't return the robot description - *spank*.");
            }
        }
	}

	public void validateRobot(final RobotId id) {
		wifiDialog = new AlertDialogWrapper(this, new AlertDialog.Builder(this)
				.setTitle("Change Wifi?").setCancelable(false), "Yes", "No");
		evictDialog = new AlertDialogWrapper(this,
				new AlertDialog.Builder(this).setTitle("Evict User?")
						.setCancelable(false), "Yes", "No");
		errorDialog = new AlertDialogWrapper(this,
				new AlertDialog.Builder(this).setTitle("Could Not Connect")
						.setCancelable(false), "Ok");
		progressDialog = new ProgressDialogWrapper(this);
		final AlertDialogWrapper wifiDialog = new AlertDialogWrapper(this,
				new AlertDialog.Builder(this).setTitle("Change Wifi?")
						.setCancelable(false), "Yes", "No");

		// Run a set of checkers in series.
		// The last step - ensure the master is up.
		final MasterChecker mc = new MasterChecker(
				new MasterChecker.RobotDescriptionReceiver() {
					public void receive(RobotDescription robotDescription) {
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
								}
							}
						});
                        if(!fromApplication) {
                            // Check that it's not busy
                            if ( robotDescription.getConnectionStatus() == RobotDescription.UNAVAILABLE ) {
                                errorDialog.show("Robot is unavailable : busy serving another remote controller.");
                                errorDialog.dismiss();
                                startMasterChooser();
                            } else {
                                // Invitation checker - should reconstruct the control/master/invitation checkers
                                // Note the control checker is doing user control checking, not used by turtlebots, but by pr2?
                                NodeMainExecutorService nodeMainExecutorService = new NodeMainExecutorService();
                                NodeConfiguration nodeConfiguration;
                                try {
                                    URI uri = new URI(robotDescription.getMasterUri());
                                    nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), uri);
                                } catch (URISyntaxException e) {
                                    return; // should handle this
                                }
                                InvitationServiceClient client = new InvitationServiceClient(robotDescription.getRobotName());
                                nodeMainExecutorService.execute(client, nodeConfiguration.setNodeName("send_invitation_node"));
                                Boolean result = client.waitForResponse();
                                nodeMainExecutorService.shutdownNodeMain(client);
                                if ( !result ) {
                                    errorDialog.show("Timed out trying to invite the robot for pairing mode.");
                                    errorDialog.dismiss();
                                    startMasterChooser();
                                } else {
                                    if ( client.getInvitationResult().equals(Boolean.TRUE) ) {
                                        validatedRobot = true;
                                    } else {
                                        startMasterChooser();
                                    }
                                }
                            }
                        } else { // fromApplication
                            // Working on the lovely assumption that we're already controlling the rapp manager
                            // since we come from a running app. Note that this code is run after platform info
                            // checks have been made (see MasterChecker).
                            validatedRobot = true;
                        }
					}
				}, new MasterChecker.FailureHandler() {
					public void handleFailure(String reason) {
						final String reason2 = reason;
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
								}
							}
						});
						errorDialog.show("Cannot contact ROS master: "
								+ reason2);
						errorDialog.dismiss();
						finish();
					}
				});

		// Ensure the robot is in a good state
		final ControlChecker cc = new ControlChecker(
				new ControlChecker.SuccessHandler() {
					public void handleSuccess() {
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
									p.show("Connecting...",
											"Connecting to ROS master");
								}
							}
						});
						mc.beginChecking(id);
					}
				}, new ControlChecker.FailureHandler() {
					public void handleFailure(String reason) {
						final String reason2 = reason;
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
								}
							}
						});
						errorDialog.show("Cannot connect to control robot: "
								+ reason2);
						errorDialog.dismiss();
						finish();

					}
				}, new ControlChecker.EvictionHandler() {
					public boolean doEviction(String current, String message) {
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
								}
							}
						});
						String m = "";
						if (message != null) {
							m = " The user says: \"" + message + "\"";
						}
						evictDialog
								.setMessage(current
										+ " is running custom software on this robot. Do you want to evict this user?"
										+ m);
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.show("Connecting...",
											"Deactivating robot");
								}
							}
						});
						return evictDialog.show();
					}

					@Override
					public boolean doEviction(String user) {
						// TODO Auto-generated method stub
						return false;
					}
				}, new ControlChecker.StartHandler() {
					public void handleStarting() {
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
									p.show("Connecting...", "Starting robot");
								}
							}
						});
					}
				});

		// Ensure that the correct WiFi network is selected.
		final WifiChecker wc = new WifiChecker(
				new WifiChecker.SuccessHandler() {
					public void handleSuccess() {
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
									p.show("Connecting...",
											"Checking robot state");
								}
							}
						});
						cc.beginChecking(id);
					}
				}, new WifiChecker.FailureHandler() {
					public void handleFailure(String reason) {
						final String reason2 = reason;
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
								}
							}
						});
						errorDialog.show("Cannot connect to robot WiFi: "
								+ reason2);
						errorDialog.dismiss();
						finish();
					}
				}, new WifiChecker.ReconnectionHandler() {
					public boolean doReconnection(String from, String to) {
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.dismiss();
								}
							}
						});
						if (from == null) {
							wifiDialog
									.setMessage("To use this robot, you must connect to a wifi network. You are currently not connected to a wifi network. Would you like to connect to the correct wireless network?");
						} else {
							wifiDialog
									.setMessage("To use this robot, you must switch wifi networks. Do you want to switch from "
											+ from + " to " + to + "?");
						}
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.show("Connecting...",
											"Switching wifi networks");
								}
							}
						});
						return wifiDialog.show();
					}
				});
		progressDialog.show("Connecting...", "Checking wifi connection");
		wc.beginChecking(id, (WifiManager) getSystemService(WIFI_SERVICE));
	}

	public void onAppClicked(final App app, final boolean isClientApp) {

		boolean running = false;
		for (App i : runningAppsCache) {
			if (i.getName().equals(app.getName())) {
				running = true;
			}
		}

		if (!running && alreadyClicked == false) {
			alreadyClicked = true;

			AppManager appManager = new AppManager(app.getName(),
					getRobotNameSpaceResolver());
			appManager.setFunction("start");

			stopProgress();
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					stopProgress();
					progress = ProgressDialog.show(RobotRemocon.this,
							"Starting Rapp",
							"Starting " + app.getDisplayName() + "...", true,
							false);
					progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				}
			});

			appManager
					.setStartService(new ServiceResponseListener<StartAppResponse>() {
						@Override
						public void onSuccess(StartAppResponse message) {
							if (message.getStarted()) {
								Log.i("RobotRemocon", "Rapp started successfully [" + app.getDisplayName() + "]");
								alreadyClicked = false;
								// safeSetStatus("Started");
							} else if (message.getErrorCode() == ErrorCodes.MULTI_RAPP_NOT_SUPPORTED) {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										showDialog(MULTI_RAPP_DISABLED);
									}
								});

							} else {
								Log.w("RobotRemocon", message.getMessage());
								// safeSetStatus(message.getMessage());
							}
							stopProgress();
						}

						@Override
						public void onFailure(RemoteException e) {
							// safeSetStatus("Failed: " + e.getMessage());
							stopProgress();
						}
					});

			nodeMainExecutor.execute(appManager,
					nodeConfiguration.setNodeName("start_app"));

		}
	}

	protected void updateAppList(final ArrayList<App> apps,
			final ArrayList<App> runningApps) {
		Log.d("RobotRemocon", "updating app list gridview");
		GridView gridview = (GridView) findViewById(R.id.gridview);
		AppAdapter appAdapter = new AppAdapter(RobotRemocon.this, apps,
				runningApps);
		gridview.setAdapter(appAdapter);
		registerForContextMenu(gridview);
		gridview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {

				App app = availableAppsCache.get(position);

				if (runningAppsCache.size() > 0) {
					runningNodes = true;
					Log.i("RobotRemocon", "RunningAppsCache greater than zero.");
				}

				if (AppLauncher.launch(RobotRemocon.this, apps.get(position),
						getMasterUri(), robotDescription, runningNodes) == true) {
					if (progress != null) {
						progress.dismiss();
					}
					finish();
				} else
					onAppClicked(app, true);
			}
		});
		if (runningApps != null) {
			if (runningApps.toArray().length != 0) {
				stopAppsButton.setVisibility(stopAppsButton.VISIBLE);
			} else {
				stopAppsButton.setVisibility(stopAppsButton.GONE);
			}
		}
		Log.d("RobotRemocon", "app list gridview updated");
	}

	public void chooseNewMasterClicked(View view) {
        returnToRobotMasterChooser();
	}

    /**
     * This returns the activity to the robot master chooser
     * activity. It will get triggered via either a backpress
     * or the button provided in the RobotRemocon activity.
     */
    private void returnToRobotMasterChooser() {
        nodeMainExecutor.shutdownNodeMain(listAppsSubscriber);
        releaseRobotNameResolver();
        releaseDashboardNode(); // TODO this work costs too many times
        availableAppsCache.clear();
        runningAppsCache.clear();
        startActivityForResult(new Intent(this, RobotMasterChooser.class),
                ROBOT_MASTER_CHOOSER_REQUEST_CODE);
    }

	public void exchangeButtonClicked(View view) {
	}

	public void deactivateRobotClicked(View view) {
	}

    /**
     * Currently stops all/any robot applications and is called
     * when either the button to stop all applications has
     * been pressed or the android application terminates itself.
     */
    public void stopRobotApplication() {
        // Should find a way to stop the application without using *, i.e. keep
        // the previously rapp-initialised AppManager around and stop that from here.
        // Can we use just one AppManager?
        AppManager appManager = new AppManager("*", getRobotNameSpaceResolver());
        appManager.setFunction("stop");
        appManager
                .setStopService(new ServiceResponseListener<StopAppResponse>() {
                    @Override
                    public void onSuccess(StopAppResponse message) {
                        Log.i("RobotRemocon", "app stopped successfully");
                        availableAppsCache = new ArrayList<App>();
                        runningAppsCache = new ArrayList<App>();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateAppList(availableAppsCache,
                                        runningAppsCache);
                            }
                        });
                        progressDialog.dismiss();

                    }

                    @Override
                    public void onFailure(RemoteException e) {
                        Log.e("RobotRemocon", "app failed to stop!");
                    }
                });
        nodeMainExecutor.execute(appManager,
                nodeConfiguration.setNodeName("stop_app"));
    }

    /**
     * Callback for the button that appears in the app list view
     * when a paired robot app is running and either there is no
     * local android app, or for some reason, the local android app
     * closed without terminating the paired robot app.
     *
     * @param view
     */
	public void stopApplicationsClicked(View view) {

        /* Why trying to launch again? */
		/*
		for (App i : runningAppsCache) {
			Log.i("RobotRemocon", "sending intent to stop app to the app launcher [" + i.getName() + "]");
			AppLauncher
					.launch(this, i, getMasterUri(), robotDescription, false);
		}
        */

		progressDialog = new ProgressDialogWrapper(this);
		progressDialog.show("Stopping Applications",
				"Stopping all applications...");
        stopRobotApplication();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, R.string.exit);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case 0:
			finish();
			break;
		}
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		// readRobotList();
		final Dialog dialog;
		Button button;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
		case CLOSE_EXISTING:
			builder.setTitle("Stop Current Application?");
			builder.setMessage("There is an application already running. You cannot run two applications at once. Would you like to stop the current application?");
			// builder.setPositiveButton("Stop Current",
			// new DialogButtonClickHandler());
			// builder.setNegativeButton("Don't Stop",
			// new DialogButtonClickHandler());
			dialog = builder.create();
			break;
		case MULTI_RAPP_DISABLED:
			builder.setTitle("Multi-App Disabled on Robot");
			builder.setMessage("The mode for running multiple apps is disabled on the robot. If you would like to enable it then you can change the arguments that the App Manager gets in its launch file.");
			// builder.setNeutralButton("Okay", new DialogButtonClickHandler());
			dialog = builder.create();
			break;
		default:
			dialog = null;

		}
		return dialog;
	}

    @Override
    public void onBackPressed() {
        // this takes too long to complete and gets in the way of quickly shutting down.
        // returnToRobotMasterChooser();
        finish();
    }
}