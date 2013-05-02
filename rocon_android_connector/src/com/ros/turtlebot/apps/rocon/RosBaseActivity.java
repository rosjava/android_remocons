package com.ros.turtlebot.apps.rocon;

import java.util.ArrayList;
import java.util.List;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.exception.ServiceException;
import org.ros.internal.node.response.Response;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceResponseBuilder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import rocon_app_manager_msgs.AppDescription;
import rocon_app_manager_msgs.Constants;
import rocon_app_manager_msgs.ErrorCodes;
import rocon_app_manager_msgs.GetAppListRequest;
import rocon_app_manager_msgs.GetAppListResponse;
import rocon_app_manager_msgs.GetPlatformInfo;
import rocon_app_manager_msgs.GetPlatformInfoRequest;
import rocon_app_manager_msgs.GetPlatformInfoResponse;
import rocon_app_manager_msgs.InviteRequest;
import rocon_app_manager_msgs.InviteResponse;
import rocon_app_manager_msgs.PlatformInfo;
import rocon_app_manager_msgs.StartAppRequest;
import rocon_app_manager_msgs.StartAppResponse;
import rocon_app_manager_msgs.StatusRequest;
import rocon_app_manager_msgs.StatusResponse;

public abstract class RosBaseActivity extends RosActivity {

	private NodeConfiguration nodeConfiguration;
	private NodeMainExecutor nodeMainExecutor;
	
	// ROS service Info.
	protected String appName = "" ;
	protected String nodeName = "" ;
	protected String platform = "" ;
	protected String deviceName = "" ;
	protected String versionName = "" ;
	protected String remoteTargetname = "" ;
	protected String applicationNameSpace = "" ;
	protected String appStatus = "" ;
	
	// NFC Info.
	protected String nfc_table = "";
	protected String nfc_ssid = "";
	protected String nfc_password = "";
	protected String nfc_masteruri = "";
	protected String nfc_weblink = "";

	RoconAppManager platformInfo_manager = null ;
	RoconAppManager appList_manager = null ;
	RoconAppManager invite_manager = null ;
	RoconAppManager status_manager = null ;
	RoconAppManager startApp_manager = null ;
	
	public RosBaseActivity() {
		super("Rocon Service", "Rocon Service");
		// TODO Auto-generated constructor stub
		Log.d("RosBaseActivity", "RosBaseActivity");
	}
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		Log.d("RosBaseActivity", "onCreate");
		super.onCreate(savedInstanceState);
		appName = "android" ;
		platform = "android" ;
		nodeName = appName + java.util.UUID.randomUUID().toString().replace("-", "") ; 
		deviceName = android.os.Build.MODEL ;
		versionName = android.os.Build.DISPLAY ;
		appStatus = Constants.APP_STOPPED ;
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		appStatus = Constants.APP_STOPPED ;
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		// TODO Auto-generated method stub
		Log.d("RosBaseActivity", "init" + getMasterUri());
		this.nodeMainExecutor = nodeMainExecutor ;
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		this.nodeConfiguration = NodeConfiguration.newPublic(Util.getWifiAddress(wifiManager, 2000), getMasterUri());
		
		platformInfo_manager = new RoconAppManager(nodeName, RoconAppManager.ACTION_PLATFORMINFO);
		init_PlatformInfo() ;
		
		appList_manager = new RoconAppManager(nodeName, RoconAppManager.ACTION_APPLIST) ;
		init_AppList() ;
		
		invite_manager = new RoconAppManager(nodeName, RoconAppManager.ACTION_INVITE) ;
		init_Invite() ;
		
		status_manager =  new RoconAppManager(nodeName, RoconAppManager.ACTION_STATUS) ;
		init_Status() ;
		
		//** Register 'start_app' service after receiving invite request
		//startApp_manager = new RoconAppManager(nodeName, RoconAppManager.ACTION_STARTAPP) ;
		//init_StartApp() ;
	}
	
	private void init_PlatformInfo() {
		Log.d("RosBaseActivity", "init_PlatformInfo()");
		platformInfo_manager.setPlatformInfoResponseBuilder(
				new ServiceResponseBuilder<GetPlatformInfoRequest, GetPlatformInfoResponse>() {
			
			@Override
			public void build(GetPlatformInfoRequest request, GetPlatformInfoResponse response)
					throws ServiceException {
				// TODO Auto-generated method stub
				// ----
				PlatformInfo msg = platformInfo_manager.getConnectedNode().getTopicMessageFactory().newFromType(PlatformInfo._TYPE);
				msg.setName(appName);
				msg.setPlatform(platform);
				msg.setRobot(deviceName);
				msg.setSystem(versionName);
				
				response.setPlatformInfo(msg);
			}
		});
		
		nodeMainExecutor.execute(platformInfo_manager, nodeConfiguration.setNodeName(nodeName + "/" + RoconAppManager.ACTION_PLATFORMINFO));
	}
	
	private void init_AppList() {
		Log.d("RosBaseActivity", "init_AppList()");
		appList_manager.setAppListResponseBuilder(new ServiceResponseBuilder<GetAppListRequest, GetAppListResponse>() {

			@Override
			public void build(GetAppListRequest request, GetAppListResponse response)
					throws ServiceException {
				// TODO Auto-generated method stub
				// ----
				List<rocon_app_manager_msgs.AppDescription> app_list = new ArrayList<rocon_app_manager_msgs.AppDescription>();
				AppDescription app = appList_manager.getConnectedNode().getTopicMessageFactory().newFromType(AppDescription._TYPE);
				app.setDescription("Orders drinks");
				app.setDisplay("Cafe Order App");
				app.setName("OrderDrinks");
				app.setPlatform(appName + "." + versionName + "." + deviceName);
				app.setStatus(appStatus) ;
				app_list.add(app);
				response.setApps(app_list);
			}
		});
		
		nodeMainExecutor.execute(appList_manager, nodeConfiguration.setNodeName(nodeName + "/" + RoconAppManager.ACTION_APPLIST));
	}
	
	private void init_Invite() {
		Log.d("RosBaseActivity", "init_Invite()");
		invite_manager.setInviteResponseBuilder(new ServiceResponseBuilder<InviteRequest, InviteResponse>() {

			@Override
			public void build(InviteRequest request, InviteResponse response)
					throws ServiceException {
				// TODO Auto-generated method stub
				remoteTargetname = request.getRemoteTargetName() ;
				applicationNameSpace = request.getApplicationNamespace() ;
				// ----
				response.setResult(true);
				
				startApp_manager = new RoconAppManager(nodeName, RoconAppManager.ACTION_STARTAPP) ;
				init_StartApp() ;
			}
		}) ;
		
		nodeMainExecutor.execute(invite_manager, nodeConfiguration.setNodeName(nodeName + "/" + RoconAppManager.ACTION_INVITE)) ;
	}
	
	private void init_Status() {
		Log.d("RosBaseActivity", "init_Status()");
		status_manager.setStatusResponseBuilder(new ServiceResponseBuilder<StatusRequest, StatusResponse>() {

			@Override
			public void build(StatusRequest request, StatusResponse response)
					throws ServiceException {
				// TODO Auto-generated method stub
				// ----
				response.setApplicationNamespace(applicationNameSpace);
				response.setAppStatus(appStatus);
				response.setRemoteController(remoteTargetname);
			}
		}) ;
		
		nodeMainExecutor.execute(status_manager, nodeConfiguration.setNodeName(nodeName + "/" + RoconAppManager.ACTION_STATUS));
	}

	private void init_StartApp() {
		Log.d("RosBaseActivity", "init_StartApp()");
		startApp_manager.setStartAppResponseBuilder(new ServiceResponseBuilder<StartAppRequest, StartAppResponse>() {

			@Override
			public void build(StartAppRequest request, StartAppResponse response)
					throws ServiceException {
				// TODO Auto-generated method stub
				
				String application_name = request.getName() ;
				List<rocon_app_manager_msgs.Remapping> list_remapping = request.getRemappings() ;
				// -----
				response.setAppNamespace(applicationNameSpace);
				response.setMessage("message");
				response.setStarted(true);
				response.setErrorCode(ErrorCodes.SUCCESS);
				
				//** Execute application (in this case, internet browser and web-page)
				String app_uri = nfc_weblink + "?tableid=" + nfc_table + "&ssid=" + nfc_ssid + "&password=" + nfc_password + "&concertaddress=" + getMasterUri().getHost() ;
	    		
	    		//Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://dorothyweb.businesscatalyst.com/main.html"));
	    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(app_uri));
	    		startActivity(intent);
	    		
	    		//** Change application status
	    		appStatus = Constants.APP_RUNNING ;
			}
		}) ;
		
		nodeMainExecutor.execute(startApp_manager, nodeConfiguration.setNodeName(nodeName + "/" + RoconAppManager.ACTION_STARTAPP));
	}
}
