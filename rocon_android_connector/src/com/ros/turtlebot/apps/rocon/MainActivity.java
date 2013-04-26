package com.ros.turtlebot.apps.rocon;

import org.ros.android.RosActivity;
import org.ros.node.NodeMainExecutor;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends RosBaseActivity
{

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

}
