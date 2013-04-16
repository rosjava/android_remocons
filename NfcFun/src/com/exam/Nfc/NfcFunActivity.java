package com.exam.Nfc;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class NfcFunActivity extends Activity implements CompoundButton.OnCheckedChangeListener {
	
	//NfcAdapter nfcAdapter = null ;
	YujinNfcManager mNfcManager = null ;
	TextView txtNfcStat = null ;
	EditText editOutput = null ;
	Switch swNfcOnOff = null ;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mNfcManager = new YujinNfcManager(this);
        txtNfcStat = (TextView) findViewById(R.id.text);
        editOutput = (EditText) findViewById(R.id.output);
        swNfcOnOff = (Switch) findViewById(R.id.nfc_switch);
        
        swNfcOnOff.setOnCheckedChangeListener(this);
        
        checkNfcStatus();
    }
    
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    	checkNfcStatus() ;
    	if(mNfcManager != null)
    		mNfcManager.enableForegroundDispatch() ;
    }
    
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    	
    	if(mNfcManager != null)
    		mNfcManager.disableForegroundDispatch() ;
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	// TODO Auto-generated method stub
    	super.onNewIntent(intent);
    	
    	if( mNfcManager.onNewIntent(intent) ) {
    		String msg = mNfcManager.processTag();
    		editOutput.setText(msg);
    	}
    }
    
    
    public void checkNfcStatus()
    {	
        if(mNfcManager.checkNfcStatus()) {
        	txtNfcStat.setText("NFC 태스를 스캔하세요");
        	swNfcOnOff.setChecked(true);
        } else {
        	txtNfcStat.setText("사용하기 전에 NFC를 활성화 하세요.");
        	swNfcOnOff.setChecked(false);
        }
    }
    
    public void onClick(View v) {
    	switch(v.getId()) {
    		case R.id.nfcwrite :
    			Intent intent = new Intent(this, NfcWriteActivity.class);
    			startActivity(intent);
    			break ;
    	}
    }

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		switch(buttonView.getId()) {
			case R.id.nfc_switch :
					boolean success = mNfcManager.changeNfcStatus(isChecked);
					Toast.makeText(this, "NFC " + (isChecked ? "On " : "Off ") + (success ? "Success" : "Fail"), Toast.LENGTH_SHORT).show();
					//checkNfcStatus();
					
		}
	}
}