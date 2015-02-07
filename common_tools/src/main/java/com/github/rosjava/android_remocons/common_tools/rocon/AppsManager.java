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

package com.github.rosjava.android_remocons.common_tools.rocon;

import android.os.AsyncTask;
import android.util.Log;

import com.github.rosjava.android_remocons.common_tools.master.MasterId;

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

import rocon_interaction_msgs.GetInteraction;
import rocon_interaction_msgs.GetInteractionRequest;
import rocon_interaction_msgs.GetInteractionResponse;
import rocon_interaction_msgs.GetInteractions;
import rocon_interaction_msgs.GetInteractionsRequest;
import rocon_interaction_msgs.GetInteractionsResponse;
import rocon_interaction_msgs.RequestInteraction;
import rocon_interaction_msgs.RequestInteractionRequest;
import rocon_interaction_msgs.RequestInteractionResponse;

import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.ANDROID_PLATFORM_INFO;

/**
 * This class has been derived from the original turtlebot robot app manager but now
 * tuned for interactions.
 *
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

    public enum Action {
        NONE, GET_INTERACTIONS_FOR_ROLE, GET_INTERACTION_INFO, REQUEST_INTERACTION_USE
    };

    private String role;
    private String interactionsNamespace;
    private Action action = Action.NONE;
 	private rocon_interaction_msgs.Interaction app;
    private int app_hash;  // need this separately since headless starts don't have any app info.
    private ConnectNodeThread connectThread;
    private ConnectedNode connectedNode;
    private NodeMainExecutorService nodeMainExecutorService;
    private FailureHandler failureCallback;
	private ServiceResponseListener<RequestInteractionResponse> requestServiceResponseListener;
	private ServiceResponseListener<GetInteractionsResponse>    getAppsServiceResponseListener;
    private ServiceResponseListener<GetInteractionResponse>     appInfoServiceResponseListener;


	public AppsManager(FailureHandler failureCallback) {

        this.failureCallback = failureCallback;
    }

    /**
     * This is done outside of the constructor to save it from having to be continually
     * reinstantiated in the rocon remocons. That does mean it requires some black magic,
     * i.e. construction -> init & setupXYZ -> execute
     *
     * @param interactionsNamespace
     */
    public void init(String interactionsNamespace) {
        this.interactionsNamespace = interactionsNamespace;
    }

    public void setupRequestService(ServiceResponseListener<RequestInteractionResponse> serviceResponseListener) {
        this.requestServiceResponseListener = serviceResponseListener;
    }

    public void setupGetInteractionsService(ServiceResponseListener<GetInteractionsResponse> serviceResponseListener) {
        this.getAppsServiceResponseListener = serviceResponseListener;
    }

    public void setupAppInfoService(ServiceResponseListener<GetInteractionResponse> serviceResponseListener) {
        this.appInfoServiceResponseListener = serviceResponseListener;
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

    public void getAppsForRole(final MasterId masterId, final String role) {
        this.action = Action.GET_INTERACTIONS_FOR_ROLE;
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

    public void requestAppUse(final MasterId masterId, final String role, final rocon_interaction_msgs.Interaction app) {
        this.action = Action.REQUEST_INTERACTION_USE;
        this.role = role;
        this.app  = app;
        this.app_hash = app.getHash();

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

    /**
     * This is called from the headless launcher where the only thing it knows is the
     * master id (for connection purposes and the hash stored in an nfc tag (or other).
     * We need to save this hash for later service calls.
     *
     * @param masterId
     * @param hash
     */
    public void getAppInfo(final MasterId masterId, final int hash) {
        this.action = Action.GET_INTERACTION_INFO;
        this.app_hash = hash;

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
                    getAppInfo();
                    return null;
                }
            }.execute();  // TODO: can we use this to incorporate a timeout to service calls?
        }
    }

    private void getAppsForRole() {
        // call get_roles_and_apps concert service
        ServiceClient<GetInteractionsRequest, GetInteractionsResponse> srvClient;
        String serviceName = this.interactionsNamespace + "/get_interactions";

        try {
            Log.d("AppsMng", "List apps service client created [" + serviceName + "]");
            srvClient = connectedNode.newServiceClient(serviceName, GetInteractions._TYPE);
        } catch (ServiceNotFoundException e) {
            Log.w("AppsMng", "List apps service not found [" + serviceName + "]");
            throw new RosRuntimeException(e); // TODO we should recover from this calling onFailure on listener
        }
        final GetInteractionsRequest request = srvClient.newMessage();

        request.getRoles().add(role);
        request.setUri(ANDROID_PLATFORM_INFO.getUri());

        srvClient.call(request, getAppsServiceResponseListener);
        Log.d("AppsMng", "List apps service call done [" + serviceName + "]");
    }

    private void requestAppUse() {
        // call request_interaction concert service
        ServiceClient<RequestInteractionRequest, RequestInteractionResponse> srvClient;
        String serviceName = this.interactionsNamespace + "/request_interaction";
        try {
            Log.d("AppsMng", "Request app service client created [" + serviceName + "]");
            srvClient = connectedNode.newServiceClient(serviceName, RequestInteraction._TYPE);
        } catch (ServiceNotFoundException e) {
            Log.w("AppsMng", "Request app service not found [" + serviceName + "]");
            throw new RosRuntimeException(e); // TODO we should recover from this calling onFailure on listener
        }
        final RequestInteractionRequest request = srvClient.newMessage();

        request.setHash(this.app.getHash());

        srvClient.call(request, requestServiceResponseListener);
        Log.d("AppsMng", "Request app service call done [" + serviceName + "]");
    }

    private void getAppInfo() {
        // call get_app concert service
        ServiceClient<GetInteractionRequest, GetInteractionResponse> srvClient;
        String serviceName = this.interactionsNamespace + "/get_interaction";
        try {
            Log.d("AppsMng", "Get app info service client created [" + serviceName + "]");
            srvClient = connectedNode.newServiceClient(serviceName, GetInteraction._TYPE);
        } catch (ServiceNotFoundException e) {
            Log.w("AppsMng", "Get app info not found [" + serviceName + "]");
            throw new RosRuntimeException(e); // TODO we should recover from this calling onFailure on listener
        }
        final GetInteractionRequest request = srvClient.newMessage();
        request.setHash(this.app_hash);

        srvClient.call(request, appInfoServiceResponseListener);
        Log.d("AppsMng", "Get app info service call done [" + serviceName + "]");
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

                // Check if the master exists by looking for a master parameter
                // getParam throws when it can't find the parameter (DJS: what does it throw?).
                // Could get it to look for a hardcoded rocon parameter for extra guarantees
                // (e.g. /rocon/version) however we'd still have to do some checking below
                // when the info is there but interactions not.
                ParameterClient paramClient = new ParameterClient(
                        NodeIdentifier.forNameAndUri("/concert_checker", concertUri.toString()), concertUri);
                String name = (String) paramClient.getParam(GraphName.of("/rosversion")).getResult();
                Log.i("Remocon", "Concert " + name + " found; retrieve additional information");

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
            case REQUEST_INTERACTION_USE:
                requestAppUse();
                break;
            case GET_INTERACTIONS_FOR_ROLE:
                getAppsForRole();
                break;
            case GET_INTERACTION_INFO:
                getAppInfo();
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
