package com.ros.turtlebot.apps.rocon;

import java.net.URI;
import java.net.URISyntaxException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.TextView;
import android.widget.Toast;

public class RoconMainActivity extends RosBaseActivity {

	TextView txtVersion = null ;
	PowerManager.WakeLock wakeLock = null ;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.nfc_reader);
	
	    // TODO Auto-generated method stub
	    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "ROCON");
	    wakeLock.acquire();
	    
	    txtInformation = (TextView) findViewById(R.id.INFORMATION);
	    txtVersion = (TextView) findViewById(R.id.VERSION);
	    
	    txtVersion.setText("버전:" + R.string.Version + "/릴리즈:" + R.string.ReleaseDate);
	    Intent intent = getIntent();
	    nfc_table = intent.getStringExtra("table");
	    nfc_weblink = intent.getStringExtra("weblink");
	    nfc_masteruri = intent.getStringExtra("masteruri");
	}
	
	@SuppressLint("NewApi")
	public void startMasterChooser() {
		 URI uri;
         try {
           uri = new URI(nfc_masteruri);
         } catch (URISyntaxException e) {
           //throw new RosRuntimeException(e);
        	 Toast.makeText(this, "Invalid MasterURI - " + nfc_masteruri, Toast.LENGTH_SHORT).show() ;
        	 return ;
         }
         
         nodeMainExecutorService.setMasterUri(uri);
         
      // Run init() in a new thread as a convenience since it often requires
         // network access.
         new AsyncTask<Void, Void, Void>() {
           @Override
           protected Void doInBackground(Void... params) {
             init(nodeMainExecutorService);
             return null;
           }
         }.execute();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		wakeLock.release();
	}

}
