package com.github.rosjava.android_remocons.headless_launcher;


import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.github.rosjava.android_apps.application_management.ConcertDescription;
import com.github.rosjava.android_apps.application_management.MasterId;
import com.github.rosjava.android_remocons.common_tools.AppLauncher;
import com.github.rosjava.android_remocons.common_tools.AppsManager;
import com.github.rosjava.android_remocons.common_tools.Util;
import com.github.rosjava.android_remocons.common_tools.NfcManager;
import com.github.rosjava.android_remocons.common_tools.WifiChecker;
import com.github.rosjava.android_remocons.common_tools.ConcertChecker;

import org.ros.exception.RemoteException;
import org.ros.internal.message.DefaultMessageFactory;
import org.ros.internal.message.definition.MessageDefinitionReflectionProvider;
import org.ros.node.service.ServiceResponseListener;

import concert_msgs.RequestInteractionResponse;

import static com.github.rosjava.android_remocons.common_tools.RoconConstants.*;

/**
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public class NfcLauncherActivity extends Activity {

    private enum Step {
        STARTUP,
        CONNECT_TO_SSID,
        VALIDATE_CONCERT,
        GET_NFC_APP_INFO,
        REQUEST_PERMIT,
        LAUNCH_APP,
        ABORT_LAUNCH;

        public Step next() {
            return this.ordinal() < Step.values().length - 1
                    ? Step.values()[this.ordinal() + 1]
                    : null;
        }
    }

    private Step     launchStep = Step.STARTUP;
    private Toast    lastToast;
    private Vibrator vibrator;
    private NfcManager nfcManager;

    private String ssid;
    private String password;
    private String masterHost;
    private short  masterPort;
    private int    appHash;
    private short  extraData;
    private MasterId masterId;
    private String role;
    private concert_msgs.RemoconApp app;
    private ConcertDescription concert;


	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(500);
            toast(getString(R.string.app_name) + " started", Toast.LENGTH_SHORT);

            Intent intent = getIntent();
            String action = intent.getAction();
            Log.d("NfcLaunch", action + " action started");

            nfcManager = new NfcManager(this);
            nfcManager.onNewIntent(intent);

            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) == false)
            {
                // This should not append unless we are debugging
                throw new Exception("Not started by NDEF_DISCOVERED action; this activity is only intended to run that way");
            }

            //** Step 1. Parsing NFC Data
            parseNFCData();

            Log.i("NfcLaunch", "NFC tag read");
            toast("NFC tag read", Toast.LENGTH_SHORT);

            //** Step 2. Connect to SSID
            connectToSSID();

            Log.i("NfcLaunch", "Connected to " + ssid);
            toast("Connected to " + ssid, Toast.LENGTH_SHORT);

	    	//** Step 3. Validate the concert: check for specific topics on masterUri
            checkConcert();

            Log.i("NfcLaunch", "Concert " + masterId.getMasterUri() + " up and running");
            toast("Concert " + masterId.getMasterUri() + " up and running", Toast.LENGTH_SHORT);

            //** Step 4. Retrieve app basic info given its app hash
            getAppConfig();

            Log.i("NfcLaunch", app.getDisplayName() + " configuration received from concert");
            toast(app.getDisplayName() + " configuration received from concert", Toast.LENGTH_SHORT);

            //** Step 5. Request permission to use the app
            getUsePermit();

            Log.i("NfcLaunch", app.getDisplayName() + " ready to launch!");
            toast(app.getDisplayName() + " ready to launch!", Toast.LENGTH_SHORT);

            //** Step 6. Launch the app!
            launchApp();

            //** Terminate this app
            finish();
        }
        catch (Exception e) {
            // TODO make and "error sound"
            Log.e("NfcLaunch", e.getMessage());
            toast(e.getMessage(), Toast.LENGTH_LONG);
            finish();
        }
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}

    private void parseNFCData() throws Exception {
        byte[] payload = nfcManager.getPayload();
        if (payload.length != NFC_PAYLOAD_LENGTH + 3) // 1 byte for status and 2 lang bytes
        {
            throw new Exception("Payload doesn't match expected length: "
                    + payload.length +" != " + NFC_PAYLOAD_LENGTH);
        }

        int offset = 3; // skip 1 byte for status and 2 lang bytes
        ssid       = Util.toString(payload, offset, NFC_SSID_FIELD_LENGTH).trim();
        offset    += NFC_SSID_FIELD_LENGTH;
        password   = Util.toString(payload, offset, NFC_PASSWORD_FIELD_LENGTH).trim();
        offset    += NFC_PASSWORD_FIELD_LENGTH;
        masterHost = Util.toString(payload, offset, NFC_MASTER_HOST_FIELD_LENGTH).trim();
        offset    += NFC_MASTER_HOST_FIELD_LENGTH;
        masterPort = Util.toShort(payload, offset);
        offset    += NFC_MASTER_PORT_FIELD_LENGTH;
        appHash    = Util.toInteger(payload, offset);
        offset    += NFC_APP_HASH_FIELD_LENGTH;
        extraData  = Util.toShort(payload, offset);

        launchStep = launchStep.next();
    }

    private void connectToSSID() throws Exception {
        String masterUri  = "http://" + masterHost + ":" + masterPort;
        String controlUri = masterUri; // not needed
        String encryption = "WPA2";    // not needed
        masterId = new MasterId(masterUri, controlUri, ssid, encryption, password);

        final WifiChecker wc = new WifiChecker(
                new WifiChecker.SuccessHandler() {
                    public void handleSuccess() {
                        launchStep = launchStep.next();
                    }
                },
                new WifiChecker.FailureHandler() {
                    public void handleFailure(String reason) {
                        launchStep = Step.ABORT_LAUNCH;
                    }
                },
                new WifiChecker.ReconnectionHandler() {
                    public boolean doReconnection(String from, String to) {
//                        if (from == null) {
//                            wifiDialog.setMessage("To use this concert, you must connect to a wifi network. You are currently not connected to a wifi network. Would you like to connect to the correct wireless network?");
//                        } else {
//                            wifiDialog.setMessage("To use this concert, you must switch wifi networks. Do you want to switch from "
//                                    + from + " to " + to + "?");
//                        }
                        // TODO should I ask for permit? maybe it's a bit rude to switch network without asking!
                        Log.i("NfcLaunch", "Switching from " + from + " to " + to);
                        toast("Switching from " + from + " to " + to, Toast.LENGTH_SHORT);
                        return true;
                    }
                }
        );
        toast("Connecting to " + ssid + "...", Toast.LENGTH_SHORT);
        wc.beginChecking(masterId, (WifiManager) getSystemService(WIFI_SERVICE));

        if (waitFor(Step.VALIDATE_CONCERT, 15) == false) {
            throw new Exception("Cannot connect to " + ssid + ". Aborting app launch");
        }
    }

    private void checkConcert() throws Exception {
        final ConcertChecker cc = new ConcertChecker(
                new ConcertChecker.ConcertDescriptionReceiver() {
                    public void receive(ConcertDescription concertDescription) {
                        concert = concertDescription;
                        if ( concert.getConnectionStatus() == ConcertDescription.UNAVAILABLE ) {
                            // Check that it's not busy
                            Log.e("NfcLaunch", "Concert is unavailable: busy serving another remote controller");
                            launchStep = Step.ABORT_LAUNCH;
                        } else {
                            launchStep = launchStep.next();
                        }
                    }
                },
                new ConcertChecker.FailureHandler() {
                    public void handleFailure(String reason) {
                        Log.e("NfcLaunch", "Cannot contact ROS master: " + reason);
                        launchStep = Step.ABORT_LAUNCH;
                    }
                }
        );
        toast("Validating " + masterId.getMasterUri() + "...", Toast.LENGTH_SHORT);
        cc.beginChecking(masterId);

        if (waitFor(Step.GET_NFC_APP_INFO, 10) == false) {
            throw new Exception("Cannot connect to " + masterId.getMasterUri() + ". Aborting app launch");
        }
    }

    private void getAppConfig() throws Exception {
        //concert_msgs.GetAppInfo appInfo;  request.setAppId(appHash);
        MessageDefinitionReflectionProvider messageDefinitionProvider = new MessageDefinitionReflectionProvider();
        DefaultMessageFactory messageFactory = new DefaultMessageFactory(messageDefinitionProvider);
        app = messageFactory.newFromType(concert_msgs.RemoconApp._TYPE);
        app.setName("http://chimek.yujinrobot.com/dorothy/dorothy_web_menu.html");
        app.setParameters("{'masterip':'192.168.10.233','bridgeport':9090,'tableid':3}");
        app.setServiceName("cybernetic_piracy");
        app.setDisplayName("Cafe Dorothy");

        String appURI  = "http://chimek.yujinrobot.com/dorothy/dorothy_web_menu.html";
        role = "Pirate";
        String appName = "Cafe Dorothy";
        rocon_std_msgs.Remapping remaps;
        String service = "cybernetic_piracy";
//String params = "{'masterip':'192.168.10.233','bridgeport':9090,'tableid':3}";
        launchStep = launchStep.next();

        // Add the extra data integer we got from the NFC tag as a new parameter for the app
        // Useful when we want to tailor app behavior depending to the tag that launched it
        String params = app.getParameters();
        params = params.substring(0, params.lastIndexOf('}')) + ", 'extra_data':" + String.valueOf(extraData) + "}";
        app.setParameters(params);

        if (waitFor(Step.REQUEST_PERMIT, 10) == false) {
            throw new Exception("Cannot get app info for hash " + appHash + ". Aborting app launch");
        }
    }

    private void getUsePermit() throws Exception {
        AppsManager am = new AppsManager(new AppsManager.FailureHandler() {
            public void handleFailure(String reason) {
                Log.e("NfcLaunch", "Cannot request app use: " + reason);
                launchStep = Step.ABORT_LAUNCH;
            }
        });
        am.setupRequestService(new ServiceResponseListener<RequestInteractionResponse>() {
            @Override
            public void onSuccess(concert_msgs.RequestInteractionResponse response) {
                if (response.getResult() == true) {
                    launchStep = launchStep.next();
                } else {
                    Log.i("NfcLaunch", "Concert deny app use. " + response.getMessage());
                    toast("Concert deny app use. " + response.getMessage(), Toast.LENGTH_SHORT);
                    launchStep = Step.ABORT_LAUNCH;
                }
            }

            @Override
            public void onFailure(RemoteException e) {
                Log.e("NfcLaunch", "Request app use failed. " + e.getMessage());
                launchStep = Step.ABORT_LAUNCH;
            }
        });
        am.requestAppUse(masterId, role, app);
        toast("Requesting permit to use " + app.getDisplayName() + "...", Toast.LENGTH_SHORT);

        if (waitFor(Step.LAUNCH_APP, 10) == false) {
            am.shutdown();
            throw new Exception("Cannot get permission to use " + app.getDisplayName() + ". Aborting app launch");
        }
    }

    private void launchApp() throws Exception {
        AppLauncher.Result result = AppLauncher.launch(this, concert, app);
        if (result == AppLauncher.Result.SUCCESS) {
            Log.i("NfcLaunch", app.getDisplayName() + " successfully launched");
            toast(app.getDisplayName() + " successfully launched; have fun!", Toast.LENGTH_SHORT);
        }
        else {
            // I could also show an "app not-installed" dialog and ask for going to play store to download the
            // missing app, but... this would stop to be a headless launcher! But maybe is a good idea, anyway
            throw new Exception("Launch " + app.getDisplayName() + " failed. " + result.message);
        }
    }

    private boolean waitFor(final Step step, final int timeout) {
        AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                while ((launchStep != step) && (launchStep != Step.ABORT_LAUNCH)) {
                    try { Thread.sleep(200); }
                    catch (InterruptedException e) { return false; }
                }
                return (launchStep == step); // returns false on ABORT_LAUNCH or InterruptedException
            }
        }.execute();
        try {
            return asyncTask.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e("NfcLaunch", "Async task interrupted. " + e.getMessage());
            return false;
        } catch (ExecutionException e) {
            Log.e("NfcLaunch", "Async task execution error. " + e.getMessage());
            return false;
        } catch (TimeoutException e) {
            Log.e("NfcLaunch", "Async task timeout (" + timeout + " s). " + e.getMessage());
            return false;
        }
    }

    private void toast(final String message, final int length) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // We overwrite only short duration toast, as the long ones are normally important
                if ((lastToast != null) && (lastToast.getDuration() == Toast.LENGTH_SHORT))
                    lastToast.cancel();
                lastToast = Toast.makeText(getBaseContext(), message, length);
                lastToast.show();
            }
        });
    }
}
