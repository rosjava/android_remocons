package com.github.rosjava.android_remocons.rocon_remocon;

import android.util.Log;

import com.google.common.base.Preconditions;

import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

import java.util.UUID;

import rocon_interaction_msgs.RemoconStatus;

import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.*;

/**
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 *
 * Publishes the remocon platform info and current role/app being run (if selected) in a latched topic.
 * Singleton class, intended to survive along the whole remocon session, including go and back to apps.
 */
public class StatusPublisher implements NodeMain {
    public  static final String NODE_NAME = "remocon_status_pub_node";

    private static final String REMOCON_NAME = "android_remocon";
    private static final String REMOCON_UUID = UUID.randomUUID().toString().replaceAll("-", "");

    private static StatusPublisher   instance;

    private Publisher<RemoconStatus> publisher;

    private boolean initialized = false;
    private RemoconStatus status;

    private StatusPublisher() {}

    public static StatusPublisher getInstance() {
        if (instance == null) {
            instance = new StatusPublisher();
            Log.d("RoconRemocon", "Remocon status publisher created");
        }

        return instance;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(NODE_NAME);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Preconditions.checkArgument(! initialized, "Remocon status publisher already initialized");

        // Prepare latched publisher
        publisher = connectedNode.newPublisher("/remocons/" + REMOCON_NAME + "_" + REMOCON_UUID,
                                               rocon_interaction_msgs.RemoconStatus._TYPE);
        publisher.setLatchMode(true);

        // Prepare and publish default status; platform info and uuid remain for the whole session
        status = publisher.newMessage();
        status.setPlatformInfo(ANDROID_PLATFORM_INFO);
//        status.getPlatformInfo().setName(REMOCON_NAME);
//        status.getPlatformInfo().setOs(PlatformInfo.OS_ANDROID);
//        status.getPlatformInfo().setVersion(PlatformInfo.VERSION_ANDROID_JELLYBEAN);
//        status.getPlatformInfo().setPlatform(PlatformInfo.PLATFORM_TABLET);
//        status.getPlatformInfo().setSystem(PlatformInfo..SYSTEM_ROSJAVA);
//        status.getPlatformInfo().setName(REMOCON_NAME);
//        status.getUuid().getUuid().setByte(0, 0);//REMOCON_UUID.getBytes());
        status.setUuid(REMOCON_UUID);  // TODO hack!  uuid is a byte[16] array but like this it fails msg delivery! must be cause the weird rosjava version of byte[] reserves 255 bytes buffer
        status.setRunningApp(false);
        //status.setAppName("");  // not yet implemented

        publisher.publish(status);

        initialized = true;
        Log.i("Remocon", "Remocon status publisher initialized");
    }

    @Override
    public void onShutdown(Node node) {
        Preconditions.checkArgument(initialized, "Remocon status publisher not initialized");

        publisher.shutdown();
        initialized = false;
        Log.i("Remocon", "Remocon status publisher shutdown");
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        Log.e("Remocon", "Remocon status publisher error: " + throwable.getMessage());
    }

    public void update(boolean runningApp, String appName) {
        Log.d("Remocon", "Remocon status publisher updated. Running app: " + runningApp);

        // not yet implemented
        // status.setRunningApp(runningApp);
        // if (runningApp == false || appName == null)
        //    status.setAppName("");
        // else
        //    status.setAppName(appName);

        publisher.publish(status);
    }
}
