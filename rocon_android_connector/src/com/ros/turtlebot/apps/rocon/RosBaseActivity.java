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

import android.util.Log;

import rocon_app_manager.AppDescription;
import rocon_app_manager.Constants;
import rocon_app_manager.ErrorCodes;
import rocon_app_manager.GetAppListRequest;
import rocon_app_manager.GetAppListResponse;
import rocon_app_manager.GetPlatformInfoRequest;
import rocon_app_manager.GetPlatformInfoResponse;
import rocon_app_manager.InviteRequest;
import rocon_app_manager.InviteResponse;
import rocon_app_manager.PlatformInfo;
import rocon_app_manager.StartAppRequest;
import rocon_app_manager.StartAppResponse;
import rocon_app_manager.StatusRequest;
import rocon_app_manager.StatusResponse;

public abstract class RosBaseActivity extends RosActivity {

	private NodeConfiguration nodeConfiguration;
	private NodeMainExecutor nodeMainExecutor;
	protected String appName = "";
	protected String remoteTargetname = "" ;
	protected String applicationNameSpace = "" ;
	
	protected String table = "";
	protected String ssid = "";
	protected String password = "";
	protected String masteruri = "";
	protected String weblink = "";

	RoconAppManager platformInfo_manager = null ;
	RoconAppManager appList_manager = null ;
	RoconAppManager invite_manager = null ;
	RoconAppManager status_manager = null ;
	RoconAppManager startApp_manager = null ;
	
	public RosBaseActivity() {
		super("Rocon Service", "Rocon Service");
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		// TODO Auto-generated method stub
		Log.d("RosBaseActivity", "init" + getMasterUri());
		this.nodeMainExecutor = nodeMainExecutor ;
		this.nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
				.newNonLoopback().getHostAddress(), getMasterUri());
		
		platformInfo_manager = new RoconAppManager(appName, RoconAppManager.ACTION_PLATFOMINFO);
		initPlatformInfo() ;
		
		appList_manager = new RoconAppManager(appName, RoconAppManager.ACTION_APPLIST) ;
		initAppList() ;
		
		invite_manager = new RoconAppManager(appName, RoconAppManager.ACTION_INVITE) ;
		initInvite() ;
		
		status_manager =  new RoconAppManager(appName, RoconAppManager.ACTION_STATUS) ;
		initStatus() ;
		
		startApp_manager = new RoconAppManager(appName, RoconAppManager.ACTION_STARTAPP) ;
		initStartApp() ;
	}
	
	private void initPlatformInfo() {
		Log.d("RosBaseActivity", "initPlatformInfo()");
		platformInfo_manager.setPlatformInfoResponseBuilder(
				new ServiceResponseBuilder<GetPlatformInfoRequest, GetPlatformInfoResponse>() {
			
			@Override
			public void build(GetPlatformInfoRequest request, GetPlatformInfoResponse response)
					throws ServiceException {
				// TODO Auto-generated method stub
				// ----
				PlatformInfo msg = platformInfo_manager.getConnectedNode().getTopicMessageFactory().newFromType(PlatformInfo._TYPE);
				msg.setName("android");
				msg.setPlatform("android");
				msg.setRobot("Device Name");
				msg.setSystem("Version Name");
				
				response.setPlatformInfo(msg);
			}
		});
		
		nodeMainExecutor.execute(platformInfo_manager, nodeConfiguration.setNodeName("android_platform_info"));
	}
	
	private void initAppList() {
		Log.d("RosBaseActivity", "initAppList()");
		appList_manager.setAppListResponseBuilder(new ServiceResponseBuilder<GetAppListRequest, GetAppListResponse>() {

			@Override
			public void build(GetAppListRequest request, GetAppListResponse response)
					throws ServiceException {
				// TODO Auto-generated method stub
				// ----
				List<rocon_app_manager.AppDescription> app_list = new ArrayList<rocon_app_manager.AppDescription>();
				AppDescription app = appList_manager.getConnectedNode().getTopicMessageFactory().newFromType(AppDescription._TYPE);
				app.setDescription("OrderDrinks");
				app.setDisplay("Cafe Order App");
				app.setName("Order drinks");
				app.setPlatform("android");
				app.setStatus(Constants.APP_RUNNING);
				app_list.add(app);
				response.setApps(app_list);
			}
		});
		
		nodeMainExecutor.execute(appList_manager, nodeConfiguration.setNodeName("android_list_apps"));
	}
	
	private void initInvite() {
		Log.d("RosBaseActivity", "initInvite()");
		invite_manager.setInviteResponseBuilder(new ServiceResponseBuilder<InviteRequest, InviteResponse>() {

			@Override
			public void build(InviteRequest request, InviteResponse response)
					throws ServiceException {
				// TODO Auto-generated method stub
				remoteTargetname = request.getRemoteTargetName() ;
				applicationNameSpace = request.getApplicationNamespace() ;
				// ----
				response.setResult(true);
			}
		}) ;
		
		nodeMainExecutor.execute(invite_manager, nodeConfiguration.setNodeName("android_invite"));
	}
	
	private void initStatus() {
		Log.d("RosBaseActivity", "initStatus()");
		status_manager.setStatusResponseBuilder(new ServiceResponseBuilder<StatusRequest, StatusResponse>() {

			@Override
			public void build(StatusRequest request, StatusResponse response)
					throws ServiceException {
				// TODO Auto-generated method stub
				// ----
				response.setApplicationNamespace(applicationNameSpace);
				response.setAppStatus(Constants.APP_RUNNING);
				response.setRemoteController("concert_master");
			}
		}) ;
		
		nodeMainExecutor.execute(status_manager, nodeConfiguration.setNodeName("android_status"));
	}

	private void initStartApp() {
		Log.d("RosBaseActivity", "initStartApp()");
		startApp_manager.setStartAppResponseBuilder(new ServiceResponseBuilder<StartAppRequest, StartAppResponse>() {

			@Override
			public void build(StartAppRequest request, StartAppResponse response)
					throws ServiceException {
				// TODO Auto-generated method stub
				
				String application_name = request.getName() ;
				List<rocon_app_manager.Remapping> list_remapping = request.getRemappings() ;
				// -----
				response.setAppNamespace(applicationNameSpace);
				response.setMessage("message");
				response.setStarted(true);
				response.setErrorCode(ErrorCodes.SUCCESS);
			}
		}) ;
		
		nodeMainExecutor.execute(startApp_manager, nodeConfiguration.setNodeName("android_start_app"));
	}
}
