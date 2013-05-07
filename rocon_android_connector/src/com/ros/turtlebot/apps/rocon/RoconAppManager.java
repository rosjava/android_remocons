package com.ros.turtlebot.apps.rocon;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceResponseBuilder;

import android.util.Log;

import rocon_app_manager_msgs.* ;

public class RoconAppManager extends AbstractNodeMain {

	public static final String ACTION_PLATFORMINFO = "platform_info";
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
	
	private String nodeName = "" ;
	private String actionName = "" ;
	private String serviceName = "";
	

	public RoconAppManager(String nodeName, String actionName) {
		this.nodeName = nodeName ;
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
		if(actionName.equals(ACTION_PLATFORMINFO))
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
		serviceName = nodeName + "/" + ACTION_PLATFORMINFO ;
		if(connectedNode != null)
			connectedNode.newServiceServer (serviceName, GetPlatformInfo._TYPE, platformInfoResponseBuilder) ;
		else
			Log.e("RoconAppManager", "onStartPlatformInfo()-connectedNode is null");
	}
	
	private void onStartAppList() {
		Log.d("RoconAppManager", "onStartAppList()");
		serviceName = nodeName + "/" + ACTION_APPLIST ;
		if(connectedNode != null)
			connectedNode.newServiceServer(serviceName, GetAppList._TYPE, appListResponseBuilder) ;
		else
			Log.e("RoconAppManager", "onStartAppList()-connectedNode is null");
	}
	
	private void onStartStatus() {
		Log.d("RoconAppManager", "onStartStatus()");
		serviceName = nodeName + "/" + ACTION_STATUS ;
		if(connectedNode != null)
			connectedNode.newServiceServer(serviceName, Status._TYPE, statusResponseBuilder) ;
		else
			Log.e("RoconAppManager", "onStartStatus()-connectedNode is null");
	}
	
	private void onStartInvite() {
		Log.d("RoconAppManager", "onStartInvite()");
		serviceName = nodeName + "/" + ACTION_INVITE ;
		if(connectedNode != null)
			connectedNode.newServiceServer(serviceName, Invite._TYPE, inviteResponseBuilder) ;
		else
			Log.e("RoconAppManager", "onStartInvite()-connectedNode is null");
	}
	
	private void onStartApp() {
		Log.d("RoconAppManager", "onStartApp()");
		serviceName = nodeName + "/" + ACTION_STARTAPP ;
		if(connectedNode != null)
			connectedNode.newServiceServer(serviceName, StartApp._TYPE, startAppResponseBuilder) ;
		else
			Log.e("RoconAppManager", "onStartApp()-connectedNode is null");
	}
	
	
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName ;
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
