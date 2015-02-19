package com.github.rosjava.android_remocons.robot_remocon;


import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.github.rosjava.android_apps.application_management.AppManager;
import com.github.rosjava.android_apps.application_management.MasterChecker;
import com.github.rosjava.android_apps.application_management.RobotDescription;
import com.github.rosjava.android_apps.application_management.RobotId;
import com.github.rosjava.android_apps.application_management.WifiChecker;
import com.github.rosjava.android_remocons.common_tools.nfc.NfcManager;
import com.github.rosjava.android_remocons.common_tools.nfc.NfcReaderActivity;
import com.github.rosjava.android_remocons.common_tools.system.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_MASTER_HOST_FIELD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_PASSWORD_FIELD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_PAYLOAD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_SSID_FIELD_LENGTH;

/**
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public class NfcLauncherActivity extends NfcReaderActivity {

    private enum Step {
        STARTUP,
        CONNECT_SSID,
        CHECK_ROBOT,
        START_ROBOT,
        ABORT_LAUNCH;

        public Step next() {
            return this.ordinal() < Step.values().length - 1
                    ? Step.values()[this.ordinal() + 1]
                    : null;
        }
    }

    private Step launchStep = Step.STARTUP;
    private Toast lastToast;
    private Vibrator vibrator;
    private NfcManager nfcManager;

    private String ssid;
    private String password;
    private String masterHost;
    private short  masterPort;
    private RobotId masterId;
    private RobotDescription robot;


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

	    	//** Step 3. Validate the robot: check for specific topics on masterUri
            checkRobot();

            Log.i("NfcLaunch", "Robot " + masterId.getMasterUri() + " up and running");
            toast("Robot " + masterId.getMasterUri() + " up and running", Toast.LENGTH_SHORT);

            //** Step 4. Start the robot!
            startRobot();

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
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onPause() {
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

        launchStep = launchStep.next();
    }

    private void connectToSSID() throws Exception {
        String masterUri  = "http://" + masterHost + ":" + masterPort;
        String controlUri = null;      // not needed  WARN; if set, rebot remocon will fail! TODO remove from everywhere
        String encryption = "WPA2";    // not needed
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("URL",masterUri);
        params.put("CURL",controlUri);
        params.put("WIFI",ssid);
        params.put("WIFIENC",encryption);
        params.put("WIFIPW",password);
        masterId = new RobotId(params);
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
                        // TODO should I ask for permit? maybe it's a bit rude to switch network without asking!
                        Log.i("NfcLaunch", "Switching from " + from + " to " + to);
                        toast("Switching from " + from + " to " + to, Toast.LENGTH_SHORT);
                        return true;
                    }
                }
        );
        toast("Connecting to " + ssid + "...", Toast.LENGTH_SHORT);
        wc.beginChecking(masterId, (WifiManager) getSystemService(WIFI_SERVICE));

        if (waitFor(Step.CHECK_ROBOT, 15) == false) {
            throw new Exception("Cannot connect to " + ssid + ". Aborting app launch");
        }
    }

    private void checkRobot() throws Exception {
        final MasterChecker mc = new MasterChecker(
                new MasterChecker.RobotDescriptionReceiver() {
                    public void receive(RobotDescription robotDescription) {
                        robot = robotDescription;
                        if ( robot.getConnectionStatus() == RobotDescription.UNAVAILABLE ) {
                            // Check that it's not busy
                            Log.e("NfcLaunch", "Robot is unavailable: busy serving another remote controller");
                            launchStep = Step.ABORT_LAUNCH;
                        } else {
                            launchStep = launchStep.next();
                        }
                    }
                },
                new MasterChecker.FailureHandler() {
                    public void handleFailure(String reason) {
                        Log.e("NfcLaunch", "Cannot contact ROS master: " + reason);
                        launchStep = Step.ABORT_LAUNCH;
                    }
                }
        );
        toast("Validating " + masterId.getMasterUri() + "...", Toast.LENGTH_SHORT);
        mc.beginChecking(masterId);

        if (waitFor(Step.START_ROBOT, 10) == false) {
            throw new Exception("Cannot connect to " + masterId.getMasterUri() + ". Aborting app launch");
        }
    }

    private void startRobot() throws Exception {
        Intent intent = new Intent("com.github.rosjava.android_remocons.robot_remocon.RobotRemocon");
        intent.putExtra(RobotDescription.UNIQUE_KEY, robot);
        intent.putExtra(AppManager.PACKAGE + "." + "app_name", "NfcLauncher");
        startActivity(intent);
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
