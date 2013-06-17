package com.github.ros_java.android_remocons.robot_remocon;

import java.net.URI;

import android.util.Log;

import org.ros.exception.RemoteException;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceResponseListener;
import org.ros.internal.node.client.ParameterClient;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.namespace.GraphName;
import org.ros.address.InetAddressFactory;
import org.ros.node.NodeConfiguration;
import org.ros.android.NodeMainExecutorService;
import com.github.ros_java.android_apps.application_management.AppManager;
import com.github.ros_java.android_apps.application_management.RobotNameResolver;

import rocon_app_manager_msgs.PlatformInfo;
import rocon_app_manager_msgs.GetPlatformInfo;
import rocon_app_manager_msgs.GetPlatformInfoRequest;
import rocon_app_manager_msgs.GetPlatformInfoResponse;

/**
 * Created by snorri on 18/06/13.
 */
public class PlatformInfoService extends AbstractNodeMain {
    private PlatformInfo platformInfo;
    private NodeMainExecutorService nodeMainExecutorService;
    private URI uri;
    private NodeConfiguration nodeConfiguration;
    private RobotNameResolver robotNameResolver;
    private ServiceResponseListener<GetPlatformInfoResponse> platformInfoServiceResponseListener;
    private ConnectedNode connectedNode;

    public PlatformInfoService() {
        nodeMainExecutorService = new NodeMainExecutorService();
        try {
            uri = new java.net.URI("http://192.168.1.3:11312");
            nodeMainExecutorService.setMasterUri(uri);
            nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
                    .newNonLoopback().getHostAddress(), uri);
            ParameterClient paramClient = new ParameterClient(
                    NodeIdentifier.forNameAndUri("/master_checker", uri.toString()), uri);
            String robotName = (String) paramClient.getParam(GraphName.of("robot/name")).getResult();

            robotNameResolver = new RobotNameResolver();
            robotNameResolver.setRobotName(robotName);
            nodeMainExecutorService.execute(robotNameResolver,
                    nodeConfiguration.setNodeName("robotNameResolver"));
            robotNameResolver.waitForResolver();
            platformInfoServiceResponseListener = new ServiceResponseListener<GetPlatformInfoResponse>() {
                @Override
                public void onSuccess(GetPlatformInfoResponse message) {
                    Log.i("Dude", "platform info retrieved successfully");
                    platformInfo = message.getPlatformInfo();
                }

                @Override
                public void onFailure(RemoteException e) {
                    Log.e("Dude", "failed to get platform information!");
                }
            };

            AppManager platformInfoService = new AppManager("", robotNameResolver.getRobotNameSpace());
            platformInfoService.setFunction("platform_info");
            nodeMainExecutorService.execute(platformInfoService, nodeConfiguration.setNodeName("dudes_node"));
            while (platformInfo == null) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }
            nodeMainExecutorService.shutdown();
        } catch (java.net.URISyntaxException e) {
        }
    }

    public PlatformInfo call() {
        return platformInfo;
    }
    @Override
    public void onStart(final ConnectedNode connectedNode) {
        if (this.connectedNode != null) {
            Log.e("RobotRemocon", "app manager instances may only ever be executed once.");
            return;
        }
        this.connectedNode = connectedNode;
        // do stuff
    }
}
