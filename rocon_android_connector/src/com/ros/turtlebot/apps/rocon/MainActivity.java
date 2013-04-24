package com.ros.turtlebot.apps.rocon;

import org.ros.android.RosActivity;
import org.ros.node.NodeMainExecutor;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends RosActivity
{
    /*protected MainActivity() {
		super("Rocon Service", "Rocon Service");
		// TODO Auto-generated constructor stub
	}*/

	protected MainActivity() {
		super("Rocon Service", "Rocon Service");
		// TODO Auto-generated constructor stub
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    @Override
    public void startMasterChooser() {
    	// TODO Auto-generated method stub
    	//super.startMasterChooser();
    	Toast.makeText(this, "startMasterChooser", Toast.LENGTH_SHORT).show();
    }

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		// TODO Auto-generated method stub
		
	}
}
