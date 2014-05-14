package com.github.rosjava.android_remocons.rocon_remocon;

import android.util.Log;

import com.google.common.base.Preconditions;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import rocon_interaction_msgs.Pair;

/**
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 *
 * Publishes the remocon platform info and current role/app being run (if selected) in a latched topic.
 * Singleton class, intended to survive along the whole remocon session, including go and back to apps.
 */
public class PairSubscriber implements NodeMain {
    public  static final String NODE_NAME = "remocon_pair_subscriber_node";

    private static PairSubscriber instance = null;

    private Subscriber<Pair> pair_subsciber;

    private boolean initialized = false;
    private int paired_app_hash;
    StatusPublisher statusPublisher = StatusPublisher.getInstance();


    private PairSubscriber() {}

    public static PairSubscriber getInstance() {
        if (instance == null) {
            instance = new PairSubscriber();
            Log.i("Remocon", "Remocon pair subscriber created");
        }

        return instance;
    }

    public boolean isInitialized() {
        return initialized;
    }
    public void setAppHash(int app_hash){
        paired_app_hash = app_hash;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(NODE_NAME);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Preconditions.checkArgument(! initialized, "Remocon pair subscriber already initialized");

        paired_app_hash = 0;
        pair_subsciber =
                connectedNode.newSubscriber("interactions/pairing",Pair._TYPE);

        pair_subsciber.addMessageListener(new MessageListener<Pair>() {
            @Override
            public void onNewMessage(Pair pair) {
                if (paired_app_hash != 0){
                    if(pair.getRemocon().equals(statusPublisher.REMOCON_FULL_NAME) && pair.getRapp().equals("")){
                        paired_app_hash = 0;
                        statusPublisher.update(false,paired_app_hash, null);
                    }
                }
            }
        });

        initialized = true;
        Log.i("Remocon", "Remocon pair subscriber initialized");
    }

    @Override
    public void onShutdown(Node node) {
        Preconditions.checkArgument(initialized, "Remocon pair subscriber not initialized");
        pair_subsciber.shutdown();
        initialized = false;
        Log.i("Remocon", "Remocon pair subscriber shutdown");
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        Log.e("Remocon", "Remocon status publisher error: " + throwable.getMessage());
    }
}
