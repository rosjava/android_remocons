/*
 * Copyright (C) 2013 OSRF.
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

package com.github.rosjava.android_remocons.concert_remocon.from_app_mng;

import android.util.Log;

import com.github.rosjava.android_remocons.concert_remocon.StatusPublisher;
import com.google.common.base.Preconditions;

import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import concert_msgs.RemoconApp;
import concert_msgs.RequestInteraction;
import concert_msgs.RequestInteractionRequest;
import concert_msgs.RequestInteractionResponse;
import concert_msgs.GetRolesAndApps;
import concert_msgs.GetRolesAndAppsRequest;
import concert_msgs.GetRolesAndAppsResponse;

/**
 * This class implements the services and topics required to communicate
 * with the robot app manager. Typically to use this class its a three
 * step process:
 *
 * 1) provide a callback via one of the setXXX methods
 * 2) set the function type you want to call (e.g. start_app, platform_info)
 * 3) execute the app manager instance.
 *
 * INSTANCES MAY ONLY EVER BE EXECUTED ONCE!
 *
 * Essentially you are creating a node when creating an instance, and
 * rosjava isolates each service/topic to each 'node'.
 *
 * See the RosAppActivity or RobotActivity (in android_remocons) for
 * examples.
 *
 * TODO: (DJS) move these into the .rapp_manager module as separate classes.
 * Since these can only be executed once, there is no real advantage to having
 * them together and ultimately just makes it difficult to follow the code.
 */
public class ConcertAppsManager extends AbstractNodeMain {

    // unique identifier to key string variables between activities.
    // TODO I make it compatible current apps; not needed if we rewrite as concert apps
    public static final String PACKAGE = com.github.rosjava.android_apps.application_management.AppManager.PACKAGE;

    public static final int ACTION_NONE        = 0;  // TODO make enum
    public static final int ACTION_LIST_APPS   = 1;
    public static final int ACTION_REQUEST_APP = 2;

	private static final String listService    = "/concert/interactions/get_roles_and_apps";
    private static final String requestService = "/concert/interactions/request_interaction";

    private int action = ACTION_NONE;
    private String userRole;
	private RemoconApp selectedApp;
	private ServiceResponseListener<RequestInteractionResponse> requestServiceResponseListener;
	private ServiceResponseListener<GetRolesAndAppsResponse> listServiceResponseListener;

	private ConnectedNode connectedNode;

	public ConcertAppsManager(final String userRole) {
        Preconditions.checkNotNull(userRole);

        this.userRole = userRole;
	}

	public void setAction(int action) {
		this.action = action;
	}
	
	public void setSelectedApp(RemoconApp app) {
		this.selectedApp = app;
	}

    public RemoconApp getSelectedApp() {
        return this.selectedApp;
    }


	public void setRequestService(ServiceResponseListener<RequestInteractionResponse> requestServiceResponseListener) {
		this.requestServiceResponseListener = requestServiceResponseListener;
	}

	public void setListService(ServiceResponseListener<GetRolesAndAppsResponse> listServiceResponseListener) {
		this.listServiceResponseListener = listServiceResponseListener;
	}

	public void listApps() {
		ServiceClient<GetRolesAndAppsRequest, GetRolesAndAppsResponse> listAppsClient;
		try {
			Log.d("ConcertRemocon", "List apps service client created [" + listService + "]");
			listAppsClient = connectedNode.newServiceClient(listService, GetRolesAndApps._TYPE);
		} catch (ServiceNotFoundException e) {
            Log.w("ConcertRemocon", "List apps service not found [" + listService + "]");
			throw new RosRuntimeException(e);
		}
		final GetRolesAndAppsRequest request = listAppsClient.newMessage();

        request.getRoles().add(userRole);
        request.setPlatformInfo(StatusPublisher.getInstance().getPlatformInfo());

		listAppsClient.call(request, listServiceResponseListener);
		Log.d("ConcertRemocon", "List apps service call done [" + listService + "]");
	}

    public void requestApp() {
        ServiceClient<RequestInteractionRequest, RequestInteractionResponse> requestAppClient;
        try {
            Log.d("ConcertRemocon", "Request app service client created [" + requestService + "]");
            requestAppClient = connectedNode.newServiceClient(requestService, RequestInteraction._TYPE);
        } catch (ServiceNotFoundException e) {
            Log.w("ConcertRemocon", "Request app service not found [" + requestService + "]");
            throw new RosRuntimeException(e);
        }
        final RequestInteractionRequest request = requestAppClient.newMessage();

        request.setRole(userRole);
        request.setApplication(selectedApp.getName());
        request.setServiceName(selectedApp.getServiceName());
        request.setPlatformInfo(StatusPublisher.getInstance().getPlatformInfo());

        requestAppClient.call(request, requestServiceResponseListener);
        Log.d("ConcertRemocon", "Request app service call done [" + requestService + "]");
    }

    @Override
	public GraphName getDefaultNodeName() {
		return null;
	}

    /**
     * This provides a few ways to create and execute service/topic nodes with an app manager object.
     *
     * Note - you should only ever call (via NodeMainExecutor.execute() this once! It will fail
     * due to this instance being non-unique in the set of rosjava nodemains for this activity.
     *
     * @param connectedNode
     */
	@Override
	public void onStart(final ConnectedNode connectedNode) {
        if (this.connectedNode != null) {
            Log.e("ConcertRemocon", "app manager instances can only be executed once at a time [" + action + "].");
            return;
        }

        this.connectedNode = connectedNode;

        Log.d("ConcertRemocon", "onStart() - " + action);

        switch (action) {
            case ACTION_NONE:
                break;
            case ACTION_REQUEST_APP:
                requestApp();
                break;
            case ACTION_LIST_APPS:
             	listApps();       // call roles_and_apps service
                break;
            default:
                Log.w("ConcertRemocon", "Unrecogniced action requested: " + action);
        }

        if (this.connectedNode != null) {
            this.connectedNode.shutdown();
            this.connectedNode = null;
        }
	}
}
