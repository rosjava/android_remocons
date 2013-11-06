/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * Copyright (c) 2013, OSRF.
 * Copyright (c) 2013, Yujin Concert.
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

package com.github.rosjava.android_remocons.concert_remocon;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;
import org.ros.android.NodeMainExecutorService;
import org.ros.android.RosActivity;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.NodeMain;
import org.ros.node.service.ServiceResponseListener;
import com.github.rosjava.android_apps.application_management.ConcertDescription;
import com.github.rosjava.android_apps.application_management.ConcertId;
import com.github.rosjava.android_remocons.concert_remocon.from_app_mng.ConcertAppsManager;
import com.github.rosjava.android_remocons.concert_remocon.from_app_mng.ControlChecker;
import com.github.rosjava.android_remocons.concert_remocon.from_app_mng.WifiChecker;
import com.github.rosjava.android_remocons.concert_remocon.from_app_mng.ConcertChecker;
import com.google.common.base.Preconditions;

import concert_msgs.GetRolesAndAppsResponse;
import concert_msgs.RemoconApp;
import concert_msgs.RequestInteractionResponse;

public class ConcertRemocon extends RosActivity {

    /* startActivityForResult Request Codes */
	private static final int CONCERT_MASTER_CHOOSER_REQUEST_CODE = 1;

    private String concertAppName = null;
    private String defaultConcertAppName = null;
    private String defaultConcertName = null;
    private ConcertDescription concertDescription;
    private NodeMainExecutor nodeMainExecutor;
    private NodeConfiguration nodeConfiguration;
	private ArrayList<RemoconApp> availableAppsCache;
    private TextView concertNameView;
	private Button leaveConcertButton;
	private ProgressDialog progress;
	private ProgressDialogWrapper progressDialog;
	private AlertDialogWrapper wifiDialog;
	private AlertDialogWrapper evictDialog;
	private AlertDialogWrapper errorDialog;
    private ConcertAppsManager appsManager;
    private StatusPublisher statusPublisher;
	private boolean alreadyClicked = false;
	private boolean validatedConcert;
	private boolean runningNodes = false;
	private long availableAppsCacheTime;

    /*
      By default we assume the remocon has just launched independantly, however
      it can be launched upon the closure of one of its children applications.
     */
    private boolean fromApplication = false;  // true if it is a remocon activity getting control from a closing application

	private void stopProgress() {
        Log.i("ConcertRemocon", "Stopping the spinner");
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
		private Activity context;

		public AlertDialogWrapper(Activity context,
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

		public AlertDialogWrapper(Activity context,
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
		private Activity activity;

		public ProgressDialogWrapper(Activity activity) {
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
			progressDialog = ProgressDialog.show(activity, title, text, true, true);
			progressDialog.setCancelable(false);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		}
	}

	public ConcertRemocon() {
        super("ConcertRemocon", "ConcertRemocon");
        availableAppsCacheTime = 0;
		availableAppsCache = new ArrayList<RemoconApp>();
        statusPublisher = StatusPublisher.getInstance();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);

        concertAppName = getIntent().getStringExtra(ConcertAppsManager.PACKAGE + ".concert_app_name");
        if (concertAppName == null) {
            concertAppName = defaultConcertAppName;
        } else if (concertAppName.equals("AppChooser")) { // ugly legacy identifier, it's misleading so change it sometime
            Log.i("ConcertRemocon", "reinitialising from a closing remocon application");
            fromApplication = true;
        } else {
            // DJS: do we need anything here? I think the first two cases cover everything
        }

		concertNameView = (TextView) findViewById(R.id.concert_name_view);
	//	leaveConcertButton = (Button) findViewById(R.id.leave_button);
//        leaveConcertButton.setVisibility(View.VISIBLE);
	}

    /**
     * This gets processed as soon as the application returns it
     * with a uri - this is either as a result of the master chooser
     * or as in the case when it has been relaunched by an application,
     * from intents set by the application.
     *
     * Here we configure the remocon environment for a particular concert,
     * listing apps and providing the required triggers for interacting
     * with that concert.
     *
     * @param nodeMainExecutor
     */
	@Override
	protected void init(final NodeMainExecutor nodeMainExecutor) {
        this.nodeMainExecutor = nodeMainExecutor;
        nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
                .newNonLoopback().getHostAddress(), getMasterUri());

        final String userRole = concertDescription.getCurrentRole();
        appsManager = new ConcertAppsManager(userRole);
        appsManager.setListService(new ServiceResponseListener<concert_msgs.GetRolesAndAppsResponse> () {
            @Override
            public void onSuccess(concert_msgs.GetRolesAndAppsResponse response) {
                java.util.List<concert_msgs.RoleAppList>  kk = response.getData();
                concert_msgs.RoleAppList a = kk.get(0);
                availableAppsCache = (ArrayList<RemoconApp>)a.getRemoconApps();
                nodeMainExecutor.shutdownNodeMain(appsManager);
                Log.i("ConcertRemocon", "RoleAppList Publication: "
                        + availableAppsCache.size() + " apps");
                availableAppsCacheTime = System.currentTimeMillis();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateAppList(availableAppsCache);
                    }
                });
            }

            @Override
            public void onFailure(RemoteException e) {
                nodeMainExecutor.shutdownNodeMain(appsManager);
                Log.e("ConcertRemocon", "Retrive rapps for role " + userRole + " failed: " + e.getMessage());
            }

            });

        appsManager.setRequestService(new ServiceResponseListener<concert_msgs.RequestInteractionResponse>() {
            @Override
            public void onSuccess(concert_msgs.RequestInteractionResponse response) {
                Looper.prepare();

                boolean allowed = response.getResult();
                String  reason = response.getMessage();
                if (allowed) {
                    Log.i("ConcertRemocon", "Concert allowed use selected app. " + reason);
                    RemoconApp app = appsManager.getSelectedApp();
                    nodeMainExecutor.shutdownNodeMain(appsManager);
                    if (AppLauncher.launch(ConcertRemocon.this, app, getMasterUri(),
                            concertDescription, runningNodes) == true) {
                        if (progress != null) {
                            progress.dismiss();
                        }
                        statusPublisher.update(true, app.getName());
                        finish();
                    } else {
                        statusPublisher.update(false, null);
                        onAppClicked(app);
                    }
                }
                else {
                    nodeMainExecutor.shutdownNodeMain(appsManager);
                    Log.i("ConcertRemocon", "Concert denies the use of selected app. " + reason);
                    Toast.makeText(ConcertRemocon.this, "Refused: " + reason, Toast.LENGTH_SHORT).show();
                }


                availableAppsCacheTime = System.currentTimeMillis();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateAppList(availableAppsCache);
                    }
                });
            }

            @Override
            public void onFailure(RemoteException e) {
                Log.e("ConcertRemocon", "Retrive rapps for role " + userRole + " failed: " + e.getMessage());
            }

        });

        appsManager.setAction(ConcertAppsManager.ACTION_LIST_APPS);
        nodeMainExecutor.execute(appsManager, nodeConfiguration.setNodeName("list_apps_srv_node"));

        nodeMainExecutor.execute(statusPublisher, nodeConfiguration.setNodeName("remocon_status_pub_node"));
    }

    /**
     * Initialise from an intent triggered by either the returning concert
     * chooser or a concert app.
     *
     * Validation will occur slightly differently in both cases - when
     * returning from the app, it will assume its already
     * in control and skip the invitation step (see validateConcert).
     *
     * This eventually passes control back to the generic init function
     * (TODO: can probably incorporate this into that function).
     *
     * @param intent
     */
    void init(Intent intent) {
        URI uri;
        try {
            concertDescription = (ConcertDescription) intent
                    .getSerializableExtra(ConcertDescription.UNIQUE_KEY);

            validatedConcert = false;
            validateConcert(concertDescription.getConcertId());

            uri = new URI(concertDescription.getConcertId()
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
                while (!validatedConcert) {
                    // should use a sleep here to avoid burnout
                    try { Thread.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
                }
                ConcertRemocon.this.init(nodeMainExecutorService);
                return null;
            }
        }.execute();
    }
    /**
     * The main result gathered here is that from the concert master chooser
     * which is started on top of the initial ConcertRemocon Activity.
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
			if (requestCode == CONCERT_MASTER_CHOOSER_REQUEST_CODE) {
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
     * case it doesn't have to go choosing a concert). In that case, send
     * it directly to the concert validation and initialisation steps.
     */
	@Override
	public void startMasterChooser() {
		if (!fromApplication) {
			super.startActivityForResult(new Intent(this,
					ConcertChooser.class),
					CONCERT_MASTER_CHOOSER_REQUEST_CODE);
		} else {
            if (getIntent().hasExtra(ConcertDescription.UNIQUE_KEY)) {
                init(getIntent());
                Log.i("ConcertRemocon", "closing remocon application and successfully retrieved the concert description via intents.");
            } else {
                Log.e("ConcertRemocon", "closing remocon application didn't return the concert description - *spank*.");
            }
        }
	}

	public void validateConcert(final ConcertId id) {
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
		final ConcertChecker mc = new ConcertChecker(
				new ConcertChecker.ConcertDescriptionReceiver() {
					public void receive(ConcertDescription concertDescription) {
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
                            if ( concertDescription.getConnectionStatus() == ConcertDescription.UNAVAILABLE ) {
                                errorDialog.show("Concert is unavailable : busy serving another remote controller.");
                                errorDialog.dismiss();
                                startMasterChooser();
                            } else {

                                validatedConcert = true;   // for us this is enough check!

                                // Invitation checker - should reconstruct the control/master/invitation checkers
                                // Note the control checker is doing user control checking, not used by turtlebots, but by pr2?
//                                NodeMainExecutorService nodeMainExecutorService = new NodeMainExecutorService();
//                                NodeConfiguration nodeConfiguration;
//                                try {
//                                    URI uri = new URI(concertDescription.getMasterUri());
//                                    nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), uri);
//                                } catch (URISyntaxException e) {
//                                    return; // should handle this
//                                }

//                                InvitationServiceClient client = new InvitationServiceClient(concertDescription.getGatewayName(), concertDescription.getConcertName());
//                                nodeMainExecutorService.execute(client, nodeConfiguration.setNodeName("send_invitation_node"));
//                                Boolean result = client.waitForResponse();
//                                nodeMainExecutorService.shutdownNodeMain(client);
//                                if ( !result ) {
//                                    errorDialog.show("Timed out trying to invite the concert for pairing mode.");
//                                    errorDialog.dismiss();
//                                    startMasterChooser();
//                                } else {
//                                    if ( client.getInvitationResult().equals(Boolean.TRUE) ) {
//                                        validatedConcert = true;
//                                    } else {
//                                        startMasterChooser();
//                                    }
//                                }
                            }
                        } else { // fromApplication
                            // Working on the lovely assumption that we're already controlling the rapp manager
                            // since we come from a running app. Note that this code is run after platform info
                            // checks have been made (see MasterChecker).
                            validatedConcert = true;
                        }
					}
				}, new ConcertChecker.FailureHandler() {
					public void handleFailure(String reason) {
						final String reason2 = reason;
                        // Kill the connecting to ros master dialog.
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
                        // TODO : gracefully abort back to the concert master chooser instead.
                        finish();
					}
				});

		// Ensure the concert is in a good state
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
						errorDialog.show("Cannot connect to control concert: "
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
										+ " is running custom software on this concert. Do you want to evict this user?"
										+ m);
						runOnUiThread(new Runnable() {
							public void run() {
								final ProgressDialogWrapper p = progressDialog;
								if (p != null) {
									p.show("Connecting...",
											"Deactivating concert");
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
									p.show("Connecting...", "Starting concert");
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
											"Checking concert state");
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
						errorDialog.show("Cannot connect to concert WiFi: "
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
									.setMessage("To use this concert, you must connect to a wifi network. You are currently not connected to a wifi network. Would you like to connect to the correct wireless network?");
						} else {
							wifiDialog
									.setMessage("To use this concert, you must switch wifi networks. Do you want to switch from "
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

    public void onAppClicked(final RemoconApp app) {

		// TODO do I need this?

	}

	protected void updateAppList(final ArrayList<RemoconApp> apps) {
		Log.d("ConcertRemocon", "updating app list gridview");
		GridView gridview = (GridView) findViewById(R.id.gridview);
		AppAdapter appAdapter = new AppAdapter(ConcertRemocon.this, apps);
		gridview.setAdapter(appAdapter);
		registerForContextMenu(gridview);
		gridview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                RemoconApp app = apps.get(position);

                appsManager.setSelectedApp(apps.get(position));
                appsManager.setAction(ConcertAppsManager.ACTION_REQUEST_APP);
                nodeMainExecutor.execute(appsManager, nodeConfiguration.setNodeName("request_app_srv_node"));



//				if (AppLauncher.launch(ConcertRemocon.this, app, getMasterUri(),
//					                   concertDescription, runningNodes) == true) {
//					if (progress != null) {
//						progress.dismiss();
//					}
//                    statusPublisher.update(true, app.getName());
//					finish();
//				} else {
//                    statusPublisher.update(false, null);
//                    onAppClicked(app);
//                }
			}
		});
		Log.d("ConcertRemocon", "app list gridview updated");
	}

    /**
     * This returns the activity to the concert master chooser
     * activity. It will get triggered via either a backpress
     * or the button provided in the ConcertRemocon activity.
     */
    public void leaveConcertClicked(View view) {
        if (appsManager != null) {
            nodeMainExecutor.shutdownNodeMain(appsManager);
        }

        availableAppsCache.clear();
        startActivityForResult(new Intent(this, ConcertChooser.class),
                CONCERT_MASTER_CHOOSER_REQUEST_CODE);

        statusPublisher.shutdown();
        nodeMainExecutor.shutdownNodeMain(statusPublisher);
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
    public void onBackPressed() {
        leaveConcertClicked(null);
    }

    /////////////////////////////
    // NodeMain implementation //
    /////////////////////////////

//    @Override
//    public GraphName getDefaultNodeName() {
//        return GraphName.of("concert_remocon_node");
//    }
//
//    @Override
//    public void onStart(ConnectedNode connectedNode) {
//   //     statusPublisher.init(connectedNode);
//        Log.e("8888888888888888888888888888","dddddddddddddddddddddddddd");
//    }
//
//    @Override
//    public void onShutdown(Node node) {
//
//    }
//
//    @Override
//    public void onShutdownComplete(Node node) {
//
//    }
//
//    @Override
//    public void onError(Node node, Throwable throwable) {
//
//    }
}