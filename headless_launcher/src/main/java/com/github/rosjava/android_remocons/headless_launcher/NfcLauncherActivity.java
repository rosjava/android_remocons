package com.github.rosjava.android_remocons.headless_launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.github.rosjava.android_remocons.common_tools.master.ConcertChecker;
import com.github.rosjava.android_remocons.common_tools.master.MasterDescription;
import com.github.rosjava.android_remocons.common_tools.master.MasterId;
import com.github.rosjava.android_remocons.common_tools.master.RoconDescription;
import com.github.rosjava.android_remocons.common_tools.nfc.NfcManager;
import com.github.rosjava.android_remocons.common_tools.rocon.AppLauncher;
import com.github.rosjava.android_remocons.common_tools.rocon.AppsManager;
import com.github.rosjava.android_remocons.common_tools.system.Util;
import com.github.rosjava.android_remocons.common_tools.system.WifiChecker;

import org.ros.exception.RemoteException;
import org.ros.node.service.ServiceResponseListener;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rocon_interaction_msgs.GetInteractionResponse;
import rocon_interaction_msgs.Interaction;
import rocon_interaction_msgs.RequestInteractionResponse;

import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_APP_HASH_FIELD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_MASTER_HOST_FIELD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_MASTER_PORT_FIELD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_PASSWORD_FIELD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_PAYLOAD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_SSID_FIELD_LENGTH;

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
    private String errorString;

    private String ssid;
    private String password;
    private String masterHost;
    private short  masterPort;
    private int    appHash;
    private short  extraData;
    private MasterId masterId;
    private Interaction app;
    private RoconDescription concert;


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
            toast("Connected to " + ssid, Toast.LENGTH_LONG);

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
            Log.e("NfcLaunch", "ERROR:" + e.getMessage());
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
        String encryption = "WPA2";    // not needed
        masterId = new MasterId(masterUri, ssid, encryption, password);
        errorString = "";
        final WifiChecker wc = new WifiChecker(
                new WifiChecker.SuccessHandler() {
                    public void handleSuccess() {
                        launchStep = launchStep.next();
                    }
                },
                new WifiChecker.FailureHandler() {
                    public void handleFailure(String reason){
                        launchStep = Step.ABORT_LAUNCH;
                        errorString = reason;
                    }
                },
                new WifiChecker.ReconnectionHandler() {
                    public boolean doReconnection(String from, String to) {
                        // Switch to the SSID on tag; this is the place if we want to ask for permit
                        Log.i("NfcLaunch", "Switching from " + from + " to " + to);
                        toast("Switching from " + from + " to " + to, Toast.LENGTH_SHORT);
                        return true;
                    }
                }
        );
        toast("Connecting to " + ssid + "...", Toast.LENGTH_SHORT);
        wc.beginChecking(masterId, (WifiManager) getSystemService(WIFI_SERVICE));

        if (waitFor(Step.VALIDATE_CONCERT, 15) == false) {
            if(errorString.length() == 0) {
                throw new Exception("Cannot connect to " + ssid + ": Aborting app launch");
            }
            else{
                throw new Exception("Cannot connect to " + ssid + ": "+errorString);
            }
        }
    }

    private void checkConcert() throws Exception {
        final ConcertChecker cc = new ConcertChecker(
                new ConcertChecker.ConcertDescriptionReceiver() {
                    @Override
                    public void receive(RoconDescription roconDescription) {
                        concert = roconDescription;
                        if ( concert.getConnectionStatus() == MasterDescription.UNAVAILABLE ) {
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

        if (waitFor(Step.GET_NFC_APP_INFO, 15) == false) {
            throw new Exception("Cannot connect to " + masterId.getMasterUri() + ". Aborting app launch");
        }
    }

    private void getAppConfig() throws Exception {
        AppsManager am = new AppsManager(new AppsManager.FailureHandler() {
            public void handleFailure(String reason) {
                Log.e("NfcLaunch", "Cannot get app info: " + reason);
                launchStep = Step.ABORT_LAUNCH;
            }
        });

        am.setupAppInfoService(new ServiceResponseListener<GetInteractionResponse>() {
            @Override
            public void onSuccess(GetInteractionResponse getInteractionResponse) {
                if (getInteractionResponse.getResult() == true) {
                    app = getInteractionResponse.getInteraction();
                    launchStep = launchStep.next();
                } else {
                    Log.i("NfcLaunch", "App with hash " + appHash + " not found in concert");
                    launchStep = Step.ABORT_LAUNCH;
                }
            }

            @Override
            public void onFailure(RemoteException e) {
                Log.e("NfcLaunch", "Get app info failed. " + e.getMessage());
                launchStep = Step.ABORT_LAUNCH;
            }
        });


        am.init(concert.getInteractionsNamespace());
        am.getAppInfo(masterId, appHash);

        toast("Requesting app info for hash " + appHash + "...", Toast.LENGTH_SHORT);

        if (waitFor(Step.REQUEST_PERMIT, 15) == false) {
            am.shutdown();
            throw new Exception("Cannot get app info for hash " + appHash + ". Aborting app launch");
        }

        // Add the extra data integer we got from the NFC tag as a new parameter for the app
        // Useful when we want to tailor app behavior depending to the tag that launched it
        //String params = app.getParameters();
        Yaml param_yaml = new Yaml();
        Map<String, String> params = (Map<String, String>) param_yaml.load(app.getParameters());

        if (params != null && params.size() > 0){
            params.put("extra_data",String.valueOf(extraData));
            Yaml yaml = new Yaml();
            app.setParameters(yaml.dump(params));
        }
    }

    private void getUsePermit() throws Exception {
        AppsManager am = new AppsManager(new AppsManager.FailureHandler() {
            public void handleFailure(String reason) {
                Log.e("NfcLaunch", "Cannot request app use: " + reason);
                launchStep = Step.ABORT_LAUNCH;
            }
        });


        am.setupRequestService(new ServiceResponseListener<rocon_interaction_msgs.RequestInteractionResponse>() {
            @Override
            public void onSuccess(RequestInteractionResponse requestInteractionResponse) {
                if (requestInteractionResponse.getResult() == true) {
                    launchStep = launchStep.next();
                } else {
                    Log.i("NfcLaunch", "Concert deny app use. " + requestInteractionResponse.getMessage());
                    toast("Concert deny app use. " + requestInteractionResponse.getMessage(), Toast.LENGTH_LONG);
                    launchStep = Step.ABORT_LAUNCH;
                }
            }

            @Override
            public void onFailure(RemoteException e) {
                Log.e("NfcLaunch", "Request app use failed. " + e.getMessage());
                launchStep = Step.ABORT_LAUNCH;
            }
        });

//        am.setupRequestService(new ServiceResponseListener<concert_msgs.RequestInteractionResponse>() {
//            @Override
//            public void onSuccess(concert_msgs.RequestInteractionResponse response) {
//                if (response.getResult() == true) {
//                    launchStep = launchStep.next();
//                } else {
//                    Log.i("NfcLaunch", "Concert deny app use. " + response.getMessage());
//                    toast("Concert deny app use. " + response.getMessage(), Toast.LENGTH_LONG);
//                    launchStep = Step.ABORT_LAUNCH;
//                }
//            }
//
//            @Override
//            public void onFailure(RemoteException e) {
//                Log.e("NfcLaunch", "Request app use failed. " + e.getMessage());
//                launchStep = Step.ABORT_LAUNCH;
//            }
//        });
        am.init(concert.getInteractionsNamespace());
        am.requestAppUse(masterId, app.getRole(), app);
        toast("Requesting permit to use " + app.getDisplayName() + "...", Toast.LENGTH_SHORT);

        if (waitFor(Step.LAUNCH_APP, 15) == false) {
            am.shutdown();
            throw new Exception("Cannot get permission to use " + app.getDisplayName() + ". Aborting app launch");
        }
    }

    private void launchApp() throws Exception {
        //AppLauncher.Result result = AppLauncher.launch(this, concert, app);
        AppLauncher.Result result = AppLauncher.launch(this, concert, app);

        if (result == AppLauncher.Result.SUCCESS) {
            Log.i("NfcLaunch", app.getDisplayName() + " successfully launched");
            toast(app.getDisplayName() + " successfully launched; have fun!", Toast.LENGTH_SHORT);
        }
        else if (result == AppLauncher.Result.NOT_INSTALLED) {
            // App not installed; ask for going to play store to download the missing app

            Log.i("NfcLaunch", "Showing not-installed dialog.");
            final String installPackage =app.getName().substring(0, app.getName().lastIndexOf("."));

            Bundle bundle = new Bundle();
            bundle.putString("InstallPackageName", installPackage);
            bundle.putString("MainContext", "This concert app requires a client user interface app, "
                            + "but the applicable app is not installed.\n"
                            + "Would you like to install the app from the market place?");

            Intent popup= new Intent(getApplicationContext(), AlertDialogActivity.class);
            popup.putExtras(bundle);
            NfcLauncherActivity.this.startActivity(popup);
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
