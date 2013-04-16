package com.exam.Nfc;

import android.app.*;
import android.content.*;
import android.net.*;
import android.nfc.*;
import android.os.Bundle;
import android.util.*;
import android.view.*;
import android.webkit.*;
import android.webkit.WebSettings.*;
import android.widget.*;

public class NfcReaderActivity extends Activity {

	private WebView wv = null ;
	/** Called when the activity is first created. */
//	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    // TODO Auto-generated method stub
	    //setContentView(R.layout.nfcread);
	    //wv = (WebView) findViewById(R.id.webview);
	    	    
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    YujinNfcManager nfcManager = new YujinNfcManager(this);
	    nfcManager.onNewIntent(intent);
	    
	    if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))
	    {
	    	String payload = nfcManager.getPayload();
	    	//Toast.makeText(this, payload, Toast.LENGTH_SHORT).show();
	    	
	    	String table = "";
	    	String ssid = "";
	    	String password = "";
	    	String masteruri = "";
	    	String weblink = "";
	    	String[] params = payload.split(";;");
	    	
	    	for(String param : params){
	    		if(param.startsWith("ta:"))
	    			table = param.substring(3);
	    		else if(param.startsWith("ss:"))
	    			ssid = param.substring(3);
	    		else if(param.startsWith("pw:"))
	    			password = param.substring(3);
	    		else if(param.startsWith("mu"))
	    			masteruri = param.substring(3);
	    		else if(param.startsWith("wl"))
	    			weblink = param.substring(3);
	    	}
	    	
	    	if(weblink.contains("http://"))
	    	{
	    		String uri = weblink + "?table=" + table + "&ssid=" + ssid + "&password=" + password + "&masteruri=" + masteruri ;
	    		
	    		intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://dorothyweb.businesscatalyst.com/main.html"));
	    		startActivity(intent);
	    		
	    		wv = new WebView(this);
	    		wv.setFocusable(true);
	    		wv.setFocusableInTouchMode(true);
	    		WebSettings settings = wv.getSettings();
	    		settings.setJavaScriptEnabled(true);
	    		settings.setBuiltInZoomControls(true);
	    		settings.setSupportZoom(true);
	    		
	    		//settings.setPluginState(PluginState.ON);
	    		//settings.setDefaultZoom(WebSettings.ZoomDensity.CLOSE);
	    		//settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
	    		
	    		
	    		wv.setWebViewClient(new WebViewClient() {
	    			@Override
	    			public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    				// TODO Auto-generated method stub
	    				//return super.shouldOverrideUrlLoading(view, url);
	    				view.loadUrl(url);
	    				return true ;
	    			}
	    		});
	    		
	    		wv.loadUrl("http://dorothyweb.businesscatalyst.com/main.html");
	    		
	    		setContentView(wv);
	    	}
	    }
	    
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		switch(keyCode){
			case KeyEvent.KEYCODE_BACK :
				if(wv.canGoBack()) {
					wv.goBack();
					return true ;
				}
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Toast.makeText(this, "onResume()-nfcReader", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Toast.makeText(this, "onPause()-nfcReader", Toast.LENGTH_SHORT).show();
	}
	

}
