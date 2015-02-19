package com.rocon.nfc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String PREFS_KEY_TABLE = "TABLE_KEY";
	private static final String PREFS_KEY_SSID = "SSID_KEY";
	private static final String PREFS_KEY_PW = "PW_KEY";
	private static final String PREFS_KEY_URI = "URI_KEY";
	private static final String PREFS_KEY_WEB = "WEB_KEY";
	
	String table = "";
	String ssid = "";
	String password = "";
	String masteruri = "";
	String weblink = "";
	
	EditText editTable = null ;
	EditText editSsid = null ;
	EditText editPassword = null ;
	EditText editMasterUri = null ;
	EditText editWebLink = null ;
	Button btnWrite = null ;
	TextView txtText = null ;
	
	RoconNfcManager mNfcManager = null ;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		editTable = (EditText) findViewById(R.id.table);
	    editSsid = (EditText) findViewById(R.id.ssid);
	    editPassword = (EditText) findViewById(R.id.pasword);
	    editMasterUri = (EditText) findViewById(R.id.master_uri);
	    editWebLink = (EditText) findViewById(R.id.web_link);
	    btnWrite = (Button) findViewById(R.id.nfc_write);
	    txtText = (TextView) findViewById(R.id.text);
	    
	    table = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_TABLE, editTable.getText().toString());
	    ssid = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_SSID, editSsid.getText().toString());
	    password = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_PW, editPassword.getText().toString());
	    masteruri = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_URI, editMasterUri.getText().toString());
	    weblink = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_WEB, editWebLink.getText().toString());
	    
	    editTable.setText(table);
	    editSsid.setText(ssid);
	    editPassword.setText(password);
	    editMasterUri.setText(masteruri);
	    editWebLink.setText(weblink);
	    
	    mNfcManager = new RoconNfcManager(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
    	
    	if(mNfcManager != null)
    		mNfcManager.disableForegroundDispatch() ;
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
    	
    	if(mNfcManager != null)
    		mNfcManager.enableForegroundDispatch() ;
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		
		if(mNfcManager != null) {
			if(mNfcManager.onNewIntent(intent)) {
				txtText.setText("You can write Nfc tag");
				btnWrite.setEnabled(true);
			}
		}
	}
	
	public void onClick(View v) {
		
		switch(v.getId()){
			case R.id.nfc_write :
				table = editTable.getText().toString();
				ssid = editSsid.getText().toString();
				password = editPassword.getText().toString();
				masteruri = editMasterUri.getText().toString();
				weblink = editWebLink.getText().toString();
				
				String payload = "ta:" + table + ";;" + "ss:" + ssid + ";;" + "pw:" + password + ";;" + "mu:" + masteruri + ";;" + "wl:" + weblink ;
								
				boolean success = mNfcManager.writeMimeNdefMessage(payload, true);
				
				if(success)
					txtText.setText("Success to write NFC Tag!");
				else
					txtText.setText("Fail to write NFC Tag!");
				
				SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
			    editor.putString(PREFS_KEY_TABLE, table);
			    editor.putString(PREFS_KEY_SSID, ssid);
			    editor.putString(PREFS_KEY_PW, password);
			    editor.putString(PREFS_KEY_URI, masteruri);
			    editor.putString(PREFS_KEY_WEB, weblink);
			    editor.commit();
		}
	}

}
