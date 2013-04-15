
package com.exam.wifi;

import java.util.*;

import android.app.Activity;
import android.content.*;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.net.wifi.*;
import android.os.Bundle;
import android.util.*;
import android.view.*;
import android.widget.*;

/**
 * @author isyou@yujinrobot.com (Inseon You)
 */
public class WiFiFunActivity extends Activity {
	
	WifiManager wifiManager = null ;
	TextView txtWiFiStatus = null ;
	EditText editAPList = null ;
	EditText editSSID = null ;
	EditText editPassword = null ;
	IntentFilter wifiFilter = null ;
	List<ScanResult> scanResults ;
	List<WifiConfiguration> configs ;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        txtWiFiStatus = (TextView) findViewById(R.id.wifi_status);
        editAPList = (EditText) findViewById(R.id.edit_scan);
        editSSID = (EditText) findViewById(R.id.edit_SSID);
        editPassword = (EditText) findViewById(R.id.edit_Password);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        
        wifiFilter = new IntentFilter();
        wifiFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        
        UpdateWifiStatus(wifiManager.getWifiState());
        
        getConfiguredNetworks() ;
    }
    
    public void onClick(View v){
    	switch(v.getId()) {
    		case R.id.btn_enable :
    			if(wifiManager.isWifiEnabled())
    				Toast.makeText(this, "Already Enabled.", Toast.LENGTH_SHORT).show();
    			else
    				wifiManager.setWifiEnabled(true);
    			break ;
    		case R.id.btn_disable :
    			if( !wifiManager.isWifiEnabled() )
    				Toast.makeText(this, "Already Disabled.", Toast.LENGTH_SHORT).show();
    			else
    				wifiManager.setWifiEnabled(false);
    			break ;
    		case R.id.btn_configuredNet :
    			editAPList.setText("");
    			getConfiguredNetworks() ;
    			break ;
    		case R.id.btn_scan :
    			editAPList.setText("");
    			wifiManager.startScan();
    			break;
    		case R.id.btn_connect :
    			wifiConnect();
    			break ;
    	}
    }
    
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    	if(wifiFilter != null) {
    		registerReceiver(wifiEventReceiver, wifiFilter);
    		//Toast.makeText(this, "registrReceiver()", Toast.LENGTH_SHORT).show();
    	}
    }
    
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    	if(wifiFilter != null) {
    		unregisterReceiver(wifiEventReceiver);
    		//Toast.makeText(this, "unregisterReceiver", Toast.LENGTH_SHORT).show();
    	}
    }
    
    private void UpdateWifiStatus(int wifiStatus){
        switch(wifiStatus){
        case WifiManager.WIFI_STATE_DISABLED :
        	txtWiFiStatus.setText("WIFI_STATE_DISABLED");
        	break ;
        case WifiManager.WIFI_STATE_DISABLING :
        	txtWiFiStatus.setText("WIFI_STATE_DISABLING");
        	break ;
        case WifiManager.WIFI_STATE_ENABLED :
        	txtWiFiStatus.setText("WIFI_STATE_ENABLED");
        	break ;
        case WifiManager.WIFI_STATE_ENABLING :
        	txtWiFiStatus.setText("WIFI_STATE_ENABLING");
        	break ;
        case WifiManager.WIFI_STATE_UNKNOWN :
        	txtWiFiStatus.setText("WIFI_STATE_UNKNOWN");
        	break ;
        }
    }
    
    private BroadcastReceiver wifiEventReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if(intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
				scanResults = wifiManager.getScanResults();
				editAPList.setText("");
				for(ScanResult result : scanResults){
					Log.d("MyBroadcastReceiver", result.SSID);
					editAPList.append(result.toString() + "\n\n");
				}
			}
		}
	};
	
	private void getConfiguredNetworks()
	{
		configs = wifiManager.getConfiguredNetworks();
		editAPList.setText("");
		for(WifiConfiguration config : configs){
			//Log.d("ConfiguredNetworks", config.SSID);
			editAPList.append(config.toString() + "\n\n");
		}
	}
	
	private void wifiConnect()
	{
		String ssid = editSSID.getText().toString();
		String pw = editPassword.getText().toString();
		
		scanResults = wifiManager.getScanResults();
		
		ScanResult foundResult = null ;
		for(ScanResult result : scanResults){
			if(ssid.equals(result.SSID)) {
				Log.d("connect", "Found ScanResult");
				foundResult = result ;
				break ;
			}
		}
		
		if( foundResult == null ) {
			Toast.makeText(this, "Sorry!" + ssid + " is not found!", Toast.LENGTH_SHORT).show();
			return ;
		}
		
		configs = wifiManager.getConfiguredNetworks() ;
		
		for(WifiConfiguration config : configs){
			if(("\"" + ssid + "\"").equals(config.SSID)) {
				Log.d("connect", "Found SavedConfiguration");
				wifiManager.enableNetwork(config.networkId, true);
				return ;
			}
		}
		
		WifiConfiguration config = new WifiConfiguration() ;
		config.SSID = "\"" + foundResult.SSID + "\"" ;
		config.priority = 40 ;		
		
		if(foundResult.capabilities.contains("WPA")) {
			config.status = WifiConfiguration.Status.ENABLED ;
			config.preSharedKey = "\"" + pw + "\"" ;
		} else if(foundResult.capabilities.contains("WEB")){
			config.wepKeys[0] = "\"" + pw + "\"" ;
			config.wepTxKeyIndex = 0 ;
		} else {
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		}
				
		int newId = wifiManager.addNetwork(config);
		if(newId < 0){
			Log.e("connect", "Fail addNetwork()");
		} 
		else {
			if(wifiManager.enableNetwork(newId, true)) {
				Log.e("connect", "NewID = " + newId + " Success enableNetwork()");
				Toast.makeText(this, "Trying to connect to " + config.SSID, Toast.LENGTH_SHORT).show();
				wifiManager.saveConfiguration();
			}
			else {
				Log.e("connect", "Fail enableNetwork()");
			}
		}
		
	}
}