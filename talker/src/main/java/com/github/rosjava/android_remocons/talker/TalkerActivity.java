package com.github.rosjava.android_remocons.talker;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.view.RosTextView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.rosjava_tutorial_pubsub.Talker;

import java.io.IOException;

//import std_msgs.String;

public class TalkerActivity extends RosActivity
{
    private Toast lastToast;
    private RosTextView<std_msgs.String> rosTextView;

    public TalkerActivity()
    {
        super("Talker", "Talker");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        rosTextView = (RosTextView<std_msgs.String>) findViewById(R.id.text);
        rosTextView.setTopicName("chatter");
        rosTextView.setMessageType(std_msgs.String._TYPE);
        rosTextView.setMessageToStringCallable(new MessageCallable<String, std_msgs.String>() {
            @Override
            public String call(std_msgs.String message) {
                Log.e("Talker", "callback received [" + message.getData() + "]");
                return message.getData();
            }
        });
//        setDefaultMasterName(getString(R.string.default_robot));
//        setDefaultAppName(getString(R.string.paired_app_name));
//        setDashboardResource(R.id.top_bar);
//        setMainWindowResource(R.layout.main);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        //String topic = remaps.get(getString(R.string.chatter_topic));
//        super.init(nodeMainExecutor);

        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
            Log.e("Talker", "master uri [" + getMasterUri() + "]");
            Talker talker = new Talker();
            nodeMainExecutor.execute(talker, nodeConfiguration);
            nodeMainExecutor.execute(rosTextView, nodeConfiguration);
        } catch (IOException e) {
        }

    }
}



