package com.ros.turtlebot.apps.rocon;


import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;

public class NfcReaderActivity extends RosBaseActivity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.nfc_reader);
	
	    // TODO Auto-generated method stub
	    Log.d("NfcReaderActivity", "onCreate");
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    RoconNfcManager nfcManager = new RoconNfcManager(this);
	    nfcManager.onNewIntent(intent);
	    
	    if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))
	    {
	    	String payload = nfcManager.getPayload();
	    	/*
	    	String table = "";
	    	String ssid = "";
	    	String password = "";
	    	String masteruri = "";
	    	String weblink = "";
	    	*/
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
	    		
	    		/*
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
	    		*/
	    	}
	    }
	}

}
