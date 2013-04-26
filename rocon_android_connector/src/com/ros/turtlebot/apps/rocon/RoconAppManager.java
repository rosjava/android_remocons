package com.ros.turtlebot.apps.rocon;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceResponseBuilder;

import android.util.Log;

import rocon_app_manager.* ;

public class RoconAppManager extends AbstractNodeMain {

	public static final String ACTION_PLATFOMINFO = "platfom_info";
	public static final String ACTION_APPLIST = "list_apps";
	public static final String ACTION_INVITE = "invite";
	public static final String ACTION_STATUS = "status";
	public static final String ACTION_STARTAPP = "start_app";

	private ServiceResponseBuilder<GetPlatformInfoRequest, GetPlatformInfoResponse> platformInfoResponseBuilder = null ;
	private ServiceResponseBuilder<GetAppListRequest, GetAppListResponse> appListResponseBuilder = null ;
	private ServiceResponseBuilder<InviteRequest, InviteResponse> inviteResponseBuilder = null ;
	private ServiceResponseBuilder<StatusRequest, StatusResponse> statusResponseBuilder = null ;
	private ServiceResponseBuilder<StartAppRequest, StartAppResponse> startAppResponseBuilder = null ;
	
	private ConnectedNode connectedNode = null ;
	
	private String appName = "" ;
	private String actionName = "" ;
	private String serviceName = "";
	

	public RoconAppManager(String appName, String actionName) {
		this.appName = appName ;
		this.actionName = actionName ;
	}
	

	@Override
	public GraphName getDefaultNodeName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onStart(ConnectedNode connectedNode) {
		// TODO Auto-generated method stub
		Log.d("RoconAppManager", "onStart() - " + actionName);
		this.connectedNode = connectedNode ;
		if(actionName.equals(ACTION_PLATFOMINFO))
			onStartPlatformInfo() ;
		else if(actionName.equals(ACTION_APPLIST))
			onStartAppList() ;
		else if(actionName.equals(ACTION_STATUS))
			onStartStatus() ;
		else if(actionName.equals(ACTION_INVITE))
			onStartInvite() ;
		else if(actionName.equals(ACTION_STARTAPP))
			onStartApp() ;
	}
	
	private void onStartPlatformInfo() {
		Log.d("RoconAppManager", "onStartPlatformInfo()");
		serviceName = appName + "/" + ACTION_PLATFOMINFO ;
		connectedNode.newServiceServer (serviceName, PlatformInfo._TYPE, platformInfoResponseBuilder) ;
	}
	
	private void onStartAppList() {
		Log.d("RoconAppManager", "onStartAppList()");
		serviceName = appName + "/" + ACTION_APPLIST ;
		connectedNode.newServiceServer(serviceName, GetAppList._TYPE, appListResponseBuilder) ;
	}
	
	private void onStartStatus() {
		Log.d("RoconAppManager", "onStartStatus()");
		serviceName = appName + "/" + ACTION_STATUS ;
		connectedNode.newServiceServer(serviceName, Status._TYPE, statusResponseBuilder) ;
	}
	
	private void onStartInvite() {
		Log.d("RoconAppManager", "onStartInvite()");
		serviceName = appName + "/" + ACTION_INVITE ;
		connectedNode.newServiceServer(serviceName, Invite._TYPE, inviteResponseBuilder) ;
	}
	
	private void onStartApp() {
		Log.d("RoconAppManager", "onStartApp()");
		serviceName = appName + "/" + ACTION_STARTAPP ;
		connectedNode.newServiceServer(serviceName, StartApp._TYPE, startAppResponseBuilder) ;
	}
	
	
	public void setAppName(String appName) {
		this.appName = appName ;
	}
	
	public void setActionName(String actionName) {
		this.actionName = actionName ;
	}
	
	public String getActionName() {
		return this.actionName ;
	}
	
	public void setPlatformInfoResponseBuilder(
			ServiceResponseBuilder<GetPlatformInfoRequest, GetPlatformInfoResponse> platformInfoResponseBuilder) {
		this.platformInfoResponseBuilder = platformInfoResponseBuilder ;
	}
	
	public void setAppListResponseBuilder(
			ServiceResponseBuilder<GetAppListRequest, GetAppListResponse> appListResponseBuilder) {
		this.appListResponseBuilder = appListResponseBuilder ;
	}
	
	public void setInviteResponseBuilder(
			ServiceResponseBuilder<InviteRequest, InviteResponse> inviteResponseBuilder) {
		this.inviteResponseBuilder = inviteResponseBuilder ;
	}
	
	public void setStatusResponseBuilder(
			ServiceResponseBuilder<StatusRequest, StatusResponse> statusResponseBuilder) {
		this.statusResponseBuilder = statusResponseBuilder ;
	}
	
	public void setStartAppResponseBuilder(
			ServiceResponseBuilder<StartAppRequest, StartAppResponse> startAppResponseBuilder) {
		this.startAppResponseBuilder = startAppResponseBuilder ;
	}
	
	public ConnectedNode getConnectedNode() {
		return this.connectedNode ;
	}
	

}
