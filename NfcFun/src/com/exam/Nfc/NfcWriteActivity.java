package com.exam.Nfc;

import com.exam.Nfc.*;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public class NfcWriteActivity extends Activity {

	EditText editTable = null ;
	EditText editSsid = null ;
	EditText editPassword = null ;
	EditText editMasterUri = null ;
	EditText editWebLink = null ;
	Button btnWrite = null ;
	TextView txtText = null ;
	
	YujinNfcManager mNfcManager = null ;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    // TODO Auto-generated method stub
	    setContentView(R.layout.nfcwrite);
	    
	    editTable = (EditText) findViewById(R.id.table);
	    editSsid = (EditText) findViewById(R.id.ssid);
	    editPassword = (EditText) findViewById(R.id.pasword);
	    editMasterUri = (EditText) findViewById(R.id.master_uri);
	    editWebLink = (EditText) findViewById(R.id.web_link);
	    btnWrite = (Button) findViewById(R.id.nfc_write);
	    txtText = (TextView) findViewById(R.id.text);
	    
	    mNfcManager = new YujinNfcManager(this);
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
				String table = editTable.getText().toString();
				String ssid = editSsid.getText().toString();
				String password = editPassword.getText().toString();
				String masteruri = editMasterUri.getText().toString();
				String weblink = editWebLink.getText().toString();
				
				String payload = "ta:" + table + ";;" + "ss:" + ssid + ";;" + "pw:" + password + ";;" + "mu:" + masteruri + ";;" + "wl:" + weblink ;
								
				boolean success = mNfcManager.writeMimeNdefMessage(payload, true);
				
				if(success)
					txtText.setText("Success to write NFC Tag!");
				else
					txtText.setText("Fail to write NFC Tag!");
		}
	}

}
