package com.github.rosjava.android_remocons.concert_remocon;

import com.google.common.base.Preconditions;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.util.UUID;

import concert_msgs.RemoconStatus;
import rocon_std_msgs.PlatformInfo;

/**
 * Created by jorge on 11/6/13.
 *
 * Publishes the remocon platform info and current role/app being run (if selected) in a latched topic.
 */
public class StatusPublisher extends AbstractNodeMain {

    private static final String REMOCON_NAME = "android_remocon";
    private static final String REMOCON_UUID = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);

    private static StatusPublisher   instance;

    private Publisher<RemoconStatus> publisher;

    private boolean initialized = false;
    private RemoconStatus status;

    private StatusPublisher() {}

    public static StatusPublisher getInstance() {
        if (instance == null) {
            instance = new StatusPublisher();
            android.util.Log.e("8888888888888888888888888888888888888888888888888888888888888888888888888888888", "NEWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
        }

        return instance;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("remocon_status_node");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        // Prepare latched publisher
        publisher = connectedNode.newPublisher("/remocons/" + REMOCON_NAME + "_" + REMOCON_UUID,
                                               "concert_msgs/RemoconStatus");
        publisher.setLatchMode(true);

        // Prepare and publish default status; platform info and uuid remain for the whole session
        status = publisher.newMessage();
        status.getPlatformInfo().setOs(PlatformInfo.OS_ANDROID);
        status.getPlatformInfo().setVersion(PlatformInfo.VERSION_ANDROID_JELLYBEAN);
        status.getPlatformInfo().setPlatform(PlatformInfo.PLATFORM_TABLET);
        status.getPlatformInfo().setSystem(PlatformInfo.SYSTEM_ROSJAVA);
        status.getPlatformInfo().setName(REMOCON_NAME);
//        status.getUuid().getUuid().setByte(0, 0);//REMOCON_UUID.getBytes());
        status.setUuid(REMOCON_UUID);  // TODO hack!  uuid is a byte[16] array but like this it fails msg delivery! must be cause the weird rosjava version of byte[] reserves 255 bytes buffer
        status.setRunningApp(false);
        status.setAppName("");

        publisher.publish(status);

        initialized = true;
        android.util.Log.e("8888888888888888888888888888888888888888888888888888888888888888888888888888888", "READYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY");
    }

    public PlatformInfo getPlatformInfo() {
        Preconditions.checkArgument(initialized, "Remocon status publisher not initialized");

        return status.getPlatformInfo();
    }

    public void update(boolean runningApp, String appName) {
        android.util.Log.e("8888888888888888888888888888888888888888888888888888888888888888888888888888888", "GOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO   " + runningApp);

        status.setRunningApp(runningApp);
        if (runningApp == false || appName == null)
            status.setAppName("");
        else
            status.setAppName(appName);

        publisher.publish(status);
    }

    public void shutdown() {
        android.util.Log.e("8888888888888888888888888888888888888888888888888888888888888888888888888888888", "KILLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLL");
        Preconditions.checkArgument(initialized, "Remocon status publisher not initialized");

        publisher.shutdown();
    }
}
