package com.ros.turtlebot.apps.rocon;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;
import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeMainExecutor;

import com.google.common.collect.Lists;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
	private static final String PREFS_KEY_TABLE = "TABLE_KEY";
	private static final String PREFS_KEY_SSID = "SSID_KEY";
	private static final String PREFS_KEY_PW = "PW_KEY";
	private static final String PREFS_KEY_URI = "URI_KEY";
	private static final String PREFS_KEY_WEB = "WEB_KEY";
	
	WifiManager wifiManager = null ;
	IntentFilter wifiFilter = null ;
	List<ScanResult> scanResults ;
	List<WifiConfiguration> configs ;
	
	Intent rocon_intent = null ;
	
	String nfc_table = "";
	String nfc_ssid = "";
	String nfc_password = "";
	String nfc_masteruri = "";
	String nfc_weblink = "";
	
	EditText editTable = null ;
	EditText editSsid = null ;
	EditText editPassword = null ;
	EditText editMasterUri = null ;
	EditText editWebLink = null ;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	    wifiFilter = new IntentFilter();
	    wifiFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        
      //** Step 2. Enable wifi if it's disabled
    	if(!wifiManager.isWifiEnabled())
    		wifiManager.setWifiEnabled(true);
    	
    	
    	//** Step 3. Get wifi configured networks and start scan access points.
    	configs = wifiManager.getConfiguredNetworks() ;
    	wifiManager.startScan();
    	
    	//** Step 4. Try to connect to wireless network (ssid/password)
    	// This step starts after completion of the wifi scan
    	
    	editTable = (EditText) findViewById(R.id.table);
	    editSsid = (EditText) findViewById(R.id.ssid);
	    editPassword = (EditText) findViewById(R.id.pasword);
	    editMasterUri = (EditText) findViewById(R.id.master_uri);
	    editWebLink = (EditText) findViewById(R.id.web_link);
	    
	    nfc_table = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_TABLE, editTable.getText().toString());
	    nfc_ssid = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_SSID, editSsid.getText().toString());
	    nfc_password = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_PW, editPassword.getText().toString());
	    nfc_masteruri = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_URI, editMasterUri.getText().toString());
	    nfc_weblink = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_WEB, editWebLink.getText().toString());
	    
	    editTable.setText(nfc_table);
	    editSsid.setText(nfc_ssid);
	    editPassword.setText(nfc_password);
	    editMasterUri.setText(nfc_masteruri);
	    editWebLink.setText(nfc_weblink);
    }
    
    
    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(wifiFilter != null)
    		registerReceiver(wifiEventReceiver, wifiFilter);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if(wifiFilter != null)
    		unregisterReceiver(wifiEventReceiver);
	}
	
	
	private BroadcastReceiver wifiEventReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.d("MainActivity", "onReceive");
			if(intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				//** Step 4.
				scanResults = wifiManager.getScanResults();
				
				/*if(connecting == false) {
					connecting = true ;
					wifiConnect() ;
				}*/
			}
		}
	};
	
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.start_rocon :
			{
				nfc_table = editTable.getText().toString() ;
			    nfc_weblink = editWebLink.getText().toString() ;
			    nfc_ssid = editSsid.getText().toString() ;
			    nfc_password = editPassword.getText().toString() ;
			    nfc_masteruri = editMasterUri.getText().toString() ;
		        
		    	rocon_intent = new Intent(MainActivity.this, RoconMainActivity.class);
				rocon_intent.putExtra("table", nfc_table);
				rocon_intent.putExtra("weblink", nfc_weblink);
				rocon_intent.putExtra("masteruri", nfc_masteruri);
				
				SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
			    editor.putString(PREFS_KEY_TABLE, nfc_table);
			    editor.putString(PREFS_KEY_SSID, nfc_ssid);
			    editor.putString(PREFS_KEY_PW, nfc_password);
			    editor.putString(PREFS_KEY_URI, nfc_masteruri);
			    editor.putString(PREFS_KEY_WEB, nfc_weblink);
			    editor.commit();
				
				wifiConnect() ;
				startActivity(rocon_intent);

				finish();
			}
			break ;
		}
	}
	
	private void wifiConnect()
	{
		Log.d("MainActivity", "wifiConnect");
		
		scanResults = wifiManager.getScanResults();
		if(scanResults == null || scanResults.size() == 0) {
			Toast.makeText(this, "No access point is found.", Toast.LENGTH_SHORT).show();
			return ;
		}
		
		ScanResult foundResult = null ;
		for(ScanResult result : scanResults){
			if(nfc_ssid.equals(result.SSID)) {
				foundResult = result ;
				break ;
			}
		}
		
		if( foundResult == null ) {
			Toast.makeText(this, "Sorry!" + nfc_ssid + " is not found!", Toast.LENGTH_SHORT).show();
			return ;
		}
		
		configs = wifiManager.getConfiguredNetworks() ;
				
		for(WifiConfiguration config : configs){
			if(("\"" + nfc_ssid + "\"").equals(config.SSID)) {
				wifiManager.enableNetwork(config.networkId, true);
				return ;
			}
		}
		
		WifiConfiguration config = new WifiConfiguration() ;
		config.SSID = "\"" + foundResult.SSID + "\"" ;
		config.priority = 40 ;		
		
		if(foundResult.capabilities.contains("WPA")) {
			config.status = WifiConfiguration.Status.ENABLED ;
			config.preSharedKey = "\"" + nfc_password + "\"" ;
		} else if(foundResult.capabilities.contains("WEB")){
			config.wepKeys[0] = "\"" + nfc_password + "\"" ;
			config.wepTxKeyIndex = 0 ;
		} else {
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		}
		
		int newId = wifiManager.addNetwork(config);
		if(newId < 0) {
			Toast.makeText(this, "Sorry! Fail to add network " + nfc_ssid, Toast.LENGTH_SHORT).show();
			return ;
		} 
		else {
			if(wifiManager.enableNetwork(newId, true)) {
				Toast.makeText(this, "Trying to connect to " + config.SSID, Toast.LENGTH_SHORT).show();
				wifiManager.saveConfiguration();
			}
			else {
				Toast.makeText(this, "Sorry! Fail to connect to " + nfc_ssid, Toast.LENGTH_SHORT).show();
			}
		}
	}
}
