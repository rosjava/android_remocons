package com.ros.turtlebot.apps.rocon;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.ros.android.MasterChooser;
import org.ros.android.RosActivity;
import org.ros.exception.RosRuntimeException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class NfcReaderActivity extends Activity {

	WifiManager wifiManager = null ;
	IntentFilter wifiFilter = null ;
	List<ScanResult> scanResults ;
	List<WifiConfiguration> configs ;
	Vibrator vibrator = null ;
	
	boolean connecting = false ;
	
	String nfc_table = "";
	String nfc_ssid = "";
	String nfc_password = "";
	String nfc_masteruri = "";
	String nfc_weblink = "";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    //setContentView(R.layout.nfc_reader);
	
	    // TODO Auto-generated method stub
	    wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	    wifiFilter = new IntentFilter();
	    wifiFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		vibrator.vibrate(500);
        
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    RoconNfcManager nfcManager = new RoconNfcManager(this);
	    nfcManager.onNewIntent(intent);
	    
	    if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))
	    {
	    	//** Step 1. Parsing NFC Data
	    	String payload = nfcManager.getPayload();
	    	String[] params = payload.split(";;");
	    	
	    	for(String param : params) {
	    		if(param.startsWith("ta:"))
	    			nfc_table = param.substring(3);
	    		else if(param.startsWith("ss:"))
	    			nfc_ssid = param.substring(3);
	    		else if(param.startsWith("pw:"))
	    			nfc_password = param.substring(3);
	    		else if(param.startsWith("mu:"))
	    			nfc_masteruri = param.substring(3);
	    		else if(param.startsWith("wl:"))
	    			nfc_weblink = param.substring(3);
	    	}
	    	
	    	if(nfc_table.length() == 0 ||
	    		nfc_ssid.length() == 0 ||
    				nfc_masteruri.length() == 0 ||
    					nfc_weblink.length() == 0)
	    	{
	    		String stat = "NFC Data is wrong!" + "\n-Table No : " + nfc_table + "\n-SSID : " + nfc_ssid + "\n-Password : " + nfc_password + 
	    							"\n-MasterURI : " + nfc_masteruri + "\n-WebLink : " + nfc_weblink ;
	    		Toast.makeText(this, stat, Toast.LENGTH_LONG).show();
	    	}
	    	
	    	//** Step 2. Enable wifi if it's disabled
	    	if(!wifiManager.isWifiEnabled())
	    		wifiManager.setWifiEnabled(true);
	    	
	    	//** Step 3. Get wifi configured networks and start scan access points.
	    	wifiManager.startScan();
	    	configs = wifiManager.getConfiguredNetworks() ;
	    		    		    	
	    	//** Step 4. Try to connect to wireless network (ssid/password)
	    	// This step starts after completion of the wifi scan
	    }
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
		
		finish();
	}
	
	private BroadcastReceiver wifiEventReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
			if(intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				//** Step 4.
				scanResults = wifiManager.getScanResults();
				if(connecting == false) {
					connecting = true ;
					wifiConnect() ;
					
					Intent rocon_intent = new Intent(NfcReaderActivity.this, RoconMainActivity.class);
					rocon_intent.putExtra("table", nfc_table);
					rocon_intent.putExtra("weblink", nfc_weblink);
					rocon_intent.putExtra("masteruri", nfc_masteruri);
					
					startActivity(rocon_intent);
				}
			}
		}
	};
		
	
	private void wifiConnect()
	{
		Toast.makeText(this, "wifiConnect()", Toast.LENGTH_SHORT).show();
		
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
