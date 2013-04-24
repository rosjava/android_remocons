package com.ros.turtlebot.apps.rocon;

import org.ros.android.RosActivity;
import org.ros.node.NodeMainExecutor;

import android.os.Bundle;
import android.widget.Toast;

public abstract class RoconBaseActivity extends RosActivity {

	protected RoconBaseActivity(String notificationTicker, String notificationTitle) {
		super(notificationTicker, notificationTitle);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		// TODO Auto-generated method stub
		//Toast.makeText(this, "RoconBase - init", Toast.LENGTH_SHORT).show();
	}

}
