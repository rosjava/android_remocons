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

import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.RawMessage;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Subscriber;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import concert_msgs.RoleAppList;
import concert_msgs.GetRolesAndApps;
import concert_msgs.GetRolesAndAppsRequest;
import concert_msgs.GetRolesAndAppsResponse;
import rocon_app_manager_msgs.StartApp;
import rocon_app_manager_msgs.StartAppRequest;
import rocon_app_manager_msgs.StartAppResponse;
import rocon_app_manager_msgs.StopApp;
import rocon_app_manager_msgs.StopAppRequest;
import rocon_app_manager_msgs.StopAppResponse;
import rocon_std_msgs.Icon;
import rocon_std_msgs.PlatformInfo;

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
	static public final String PACKAGE = "com.github.rosjava.android_remocons.concert_remocon.from_app_mng.ConcertAppsManager";
	private static final String startTopic = "start_app";
	private static final String stopTopic  = "stop_app";
	private static final String listService = "/concert/get_roles_and_apps";

	private String appName;
    private String userRole;
	private ServiceResponseListener<StartAppResponse> startServiceResponseListener;
	private ServiceResponseListener<StopAppResponse> stopServiceResponseListener;
	private ServiceResponseListener<GetRolesAndAppsResponse> listServiceResponseListener;
    private MessageListener<RoleAppList> appListListener;
	private Subscriber<RoleAppList> subscriber;

	private ConnectedNode connectedNode;
	private String function = null;

	public ConcertAppsManager(final String appName, final String userRole) {
		this.appName  = appName;
        this.userRole = userRole;
	}

	public void setFunction(String function) {
		this.function = function;
	}
	
	public void setAppName(String appName) {
		this.appName = appName;
	}

    public void setAppListSubscriber(MessageListener<RoleAppList> appListListener) {
        this.appListListener = appListListener;
    }

    public void setStartService(
			ServiceResponseListener<StartAppResponse> startServiceResponseListener) {
		this.startServiceResponseListener = startServiceResponseListener;
	}

	public void setStopService(
			ServiceResponseListener<StopAppResponse> stopServiceResponseListener) {
		this.stopServiceResponseListener = stopServiceResponseListener;
	}

	public void setListService(
			ServiceResponseListener<GetRolesAndAppsResponse> listServiceResponseListener) {
		this.listServiceResponseListener = listServiceResponseListener;
	}

	public void listApps() {
		ServiceClient<GetRolesAndAppsRequest, GetRolesAndAppsResponse> listAppsClient;
		try {
			Log.d("ApplicationManagement", "List app service client created [" + listService + "]");
			listAppsClient = connectedNode.newServiceClient(listService, GetRolesAndApps._TYPE);
		} catch (ServiceNotFoundException e) {
            Log.w("ApplicationManagement", "List app service not found [" + listService + "]");
			throw new RosRuntimeException(e);
		}
		final GetRolesAndAppsRequest request = listAppsClient.newMessage();

        request.getRoles().add(userRole);
        request.getPlatformInfo().setOs(PlatformInfo.OS_ANDROID);
        request.getPlatformInfo().setVersion(PlatformInfo.VERSION_ANDROID_JELLYBEAN);
        request.getPlatformInfo().setPlatform(PlatformInfo.PLATFORM_TABLET);
        request.getPlatformInfo().setSystem(PlatformInfo.SYSTEM_ROSJAVA);
        request.getPlatformInfo().setPlatform(PlatformInfo.NAME_ANY);

		listAppsClient.call(request, listServiceResponseListener);
		Log.d("ApplicationManagement", "List apps service call done [" + listService + "]");
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
            Log.e("ApplicationManagement", "app manager instances may only ever be executed once [" + function + "].");
            return;
        }
        this.connectedNode = connectedNode;
    	listApps();       // call roles_and_apps service
	}
}
