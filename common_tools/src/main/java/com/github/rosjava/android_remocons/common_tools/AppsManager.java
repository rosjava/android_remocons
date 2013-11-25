/*
 * Copyright (C) 2013, OSRF.
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

package com.github.rosjava.android_remocons.common_tools;

import android.os.AsyncTask;
import android.util.Log;

import com.github.rosjava.android_apps.application_management.MasterId;

import org.ros.address.InetAddressFactory;
import org.ros.android.NodeMainExecutorService;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.node.client.ParameterClient;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import java.net.URI;
import java.net.URISyntaxException;

import concert_msgs.GetRolesAndApps;
import concert_msgs.GetRolesAndAppsRequest;
import concert_msgs.GetRolesAndAppsResponse;
import concert_msgs.RequestInteraction;
import concert_msgs.RequestInteractionRequest;
import concert_msgs.RequestInteractionResponse;

import static com.github.rosjava.android_remocons.common_tools.RoconConstants.*;

/**
 * This class has been derived from RobotAppsManager in android_apps/application_management.
 * The original is quite messy, and this is not much better, so maybe needs extra refactoring.
 * Also RobotAppsManager claims that it can be executed once, but I modified this to execute
 * arbitrary times. It looks to work, but mechanism is a bit brittle.
 * Apps manager implements the services and topics required to interact with the concert roles
 * manager. Typically to use this class its a three step process:
 *
 * 1) instantiate and provide a general failure handler
 * 2) provide a callback via one of the setupXXXservice methods
 * 3) call the service you want; there's a public method per service
 * 4) wait for service response.
 *
 * Essentially you are creating a node when creating an instance, and rosjava isolates each
 * service/topic to each 'node'.
 *
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public class AppsManager extends AbstractNodeMain {
    public interface FailureHandler {
        /**
         * Called on failure with a short description of why it failed, like "exception" or "timeout".
         */
        void handleFailure(String reason);
    }

    // unique identifier to key string variables between activities.
    // TODO I make it compatible current apps; not needed if we rewrite as concert apps
    public static final String PACKAGE = com.github.rosjava.android_apps.application_management.AppManager.PACKAGE;

    public enum Action {
        NONE, GET_APPS_FOR_ROLE, REQUEST_APP_USE
    };

    private String role;
    private Action action = Action.NONE;
 	private concert_msgs.RemoconApp app;
    private ConnectNodeThread connectThread;
    private ConnectedNode connectedNode;
    private NodeMainExecutorService nodeMainExecutorService;
    private FailureHandler failureCallback;
	private ServiceResponseListener<RequestInteractionResponse> requestServiceResponseListener;
	private ServiceResponseListener<GetRolesAndAppsResponse>    getAppsServiceResponseListener;


	public AppsManager(FailureHandler failureCallback) {
        this.failureCallback = failureCallback;
    }

    public void setupRequestService(ServiceResponseListener<RequestInteractionResponse> serviceResponseListener) {
        this.requestServiceResponseListener = serviceResponseListener;
    }

    public void setupGetAppsService(ServiceResponseListener<GetRolesAndAppsResponse> serviceResponseListener) {
        this.getAppsServiceResponseListener = serviceResponseListener;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdown();  // TODO not warrantied to be called, so user should call shutdown; finalize works at all in Android???  I think no....
    }

    public void shutdown() {
        if (nodeMainExecutorService != null)
            nodeMainExecutorService.shutdownNodeMain(this);
        else
            Log.w("AppsMng", "Shutting down an uninitialized apps manager");
    }

    public void getAppsForRole(MasterId masterId, final String role) {
        this.action = Action.GET_APPS_FOR_ROLE;
        this.role = role;

        // If this is the first action requested, we need a connected node, what must be done in a different thread
        // The requested action will be executed once we have a connected node (this object itself) in onStart method
        if (this.connectedNode == null) {
            Log.d("AppsMng", "First action requested (" + this.action + "). Starting node...");
            new ConnectNodeThread(masterId).start();
        }
        else {
            // But we don't need all this if we already executed an action, and so have a connected node. Anyway,
            // we must execute any action asynchronously to avoid the android.os.NetworkOnMainThreadException
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    getAppsForRole();
                    return null;
                }
            }.execute();  // TODO: can we use this to incorporate a timeout to service calls?
        }
    }

    public void requestAppUse(MasterId masterId, final String role, final concert_msgs.RemoconApp app) {
        this.action = Action.REQUEST_APP_USE;
        this.role = role;
        this.app  = app;

        // If this is the first action requested, we need a connected node, what must be done in a different thread
        // The requested action will be executed once we have a connected node (this object itself) in onStart method
        if (this.connectedNode == null) {
            Log.d("AppsMng", "First action requested (" + this.action + "). Starting node...");
            new ConnectNodeThread(masterId).start();
        }
        else {
            // But we don't need all this if we already executed an action, and so have a connected node. Anyway,
            // we must execute any action asynchronously to avoid the android.os.NetworkOnMainThreadException
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    requestAppUse();
                    return null;
                }
            }.execute();
        }
    }

    private void getAppsForRole() {
        // call get_roles_and_apps service
        ServiceClient<GetRolesAndAppsRequest, GetRolesAndAppsResponse> listAppsClient;
        try {
            Log.d("AppsMng", "List apps service client created [" + GET_ROLES_AND_APPS_SRV + "]");
            listAppsClient = connectedNode.newServiceClient(GET_ROLES_AND_APPS_SRV, GetRolesAndApps._TYPE);
        } catch (ServiceNotFoundException e) {
            Log.w("AppsMng", "List apps service not found [" + GET_ROLES_AND_APPS_SRV + "]");
            throw new RosRuntimeException(e); // TODO we should recover from this calling onFailure on listener
        }
        final GetRolesAndAppsRequest request = listAppsClient.newMessage();

        request.getRoles().add(role);
        request.setPlatformInfo(ANDROID_PLATFORM_INFO);

        listAppsClient.call(request, getAppsServiceResponseListener);
        Log.d("AppsMng", "List apps service call done [" + GET_ROLES_AND_APPS_SRV + "]");
    }

    private void requestAppUse() {
        ServiceClient<RequestInteractionRequest, RequestInteractionResponse> requestAppClient;
        try {
            Log.d("AppsMng", "Request app service client created [" + REQUEST_INTERACTION_SRV + "]");
            requestAppClient = connectedNode.newServiceClient(REQUEST_INTERACTION_SRV, RequestInteraction._TYPE);
        } catch (ServiceNotFoundException e) {
            Log.w("AppsMng", "Request app service not found [" + REQUEST_INTERACTION_SRV + "]");
            throw new RosRuntimeException(e); // TODO we should recover from this calling onFailure on listener
        }
        final RequestInteractionRequest request = requestAppClient.newMessage();

        request.setRole(role);
        request.setApplication(app.getName());
        request.setServiceName(app.getServiceName());
        request.setPlatformInfo(ANDROID_PLATFORM_INFO);

        requestAppClient.call(request, requestServiceResponseListener);
        Log.d("AppsMng", "Request app service call done [" + REQUEST_INTERACTION_SRV + "]");
    }

    /**
     * Start a thread to get a node connected with the given masterId. Returns immediately
     * TODO: what happens if the thread is already running??? kill it first and then start a new one?
     */
    private class ConnectNodeThread extends Thread {
        private MasterId masterId;

        public ConnectNodeThread(MasterId masterId) {
            this.masterId = masterId;
            setDaemon(true);
            setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    failureCallback.handleFailure("exception: " + ex.getMessage());
                }
            });
        }

        @Override
        public void run() {
            try {
                URI concertUri = new URI(masterId.getMasterUri());

                // Check if the concert exists by looking for concert name parameter
                // getParam throws when it can't find the parameter.
                ParameterClient paramClient = new ParameterClient(
                        NodeIdentifier.forNameAndUri("/concert_checker", concertUri.toString()), concertUri);
                String name = (String) paramClient.getParam(GraphName.of(CONCERT_NAME_PARAM)).getResult();
                Log.i("ConcertRemocon", "Concert " + name + " found; retrieve additional information");

                nodeMainExecutorService = new NodeMainExecutorService();
                NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
                        InetAddressFactory.newNonLoopback().getHostAddress(), concertUri);
                nodeMainExecutorService.execute(AppsManager.this, nodeConfiguration.setNodeName("apps_manager_node"));

            } catch (URISyntaxException e) {
                Log.w("AppsMng", "invalid concert URI [" + masterId.getMasterUri() + "][" + e.toString() + "]");
                failureCallback.handleFailure("invalid concert URI");
            } catch (RuntimeException e) {
                // thrown if concert could not be found in the getParam call (from java.net.ConnectException)
                Log.w("AppsMng", "could not find concert [" + masterId.getMasterUri() + "][" + e.toString() + "]");
                failureCallback.handleFailure(e.toString());
            } catch (Throwable e) {
                Log.w("AppsMng", "exception while creating node in concert checker for URI " + masterId.getMasterUri(), e);
                failureCallback.handleFailure(e.toString());
            }
        }
    }

    @Override
	public GraphName getDefaultNodeName() {
		return null;
	}

    /**
     * Node started by NodeMainExecutor.execute(). Execute the requested action.
     *
     * @param connectedNode
     */
	@Override
	public void onStart(final ConnectedNode connectedNode) {
        if (this.connectedNode != null) {
            Log.e("AppsMng", "App manager re-started before previous shutdown; ignoring...");
            return;
        }

        this.connectedNode = connectedNode;

        Log.d("AppsMng", "onStart() - " + action);

        switch (action) {
            case NONE:
                Log.w("AppsMng", "Node started without specifying an action");
                break;
            case REQUEST_APP_USE:
                requestAppUse();
                break;
            case GET_APPS_FOR_ROLE:
                getAppsForRole();
                break;
            default:
                Log.w("AppsMng", "Unrecogniced action requested: " + action);
        }

        Log.d("AppsMng", "Done");
	}

    @Override
    public void onShutdown(Node node) {
        Log.d("AppsMng", "Shutdown connected node...");
        super.onShutdown(node);

        // Required so we get reconnected the next time
        this.connectedNode = null;
        Log.d("AppsMng", "Done; shutdown apps manager node main");
    }

    @Override
    public void onShutdownComplete(Node node) {
        super.onShutdownComplete(node);
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        super.onError(node, throwable);

        Log.e("AppsMng", node.getName().toString() + " node error: " + throwable.getMessage());
        failureCallback.handleFailure(node.getName().toString() + " node error: " + throwable.toString());
    }
}
