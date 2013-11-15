package com.github.rosjava.android_remocons.rocon_nfc_writer;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.security.InvalidParameterException;

public class RoconNfcWriter extends Activity {

    private static final String PREFS_KEY_SSID = "SSID_KEY";
    private static final String PREFS_KEY_PSWD = "PSWD_KEY"; // password
    private static final String PREFS_KEY_HOST = "HOST_KEY"; // master URI host
    private static final String PREFS_KEY_PORT = "PORT_KEY"; // master URI port
    private static final String PREFS_KEY_NAID = "NAID_KEY"; // NFC app ID
    private static final String PREFS_KEY_DATA = "DATA_KEY"; // extra (app specific) data
    private static final String PREFS_KEY_AAR  =  "AAR_KEY"; // AAR (Android Application Record)

    private String ssid;
    private String password;
    private String masterHost;
    private String masterPort;
    private String nfcAppId;
    private String extraData;
    private String appRecord;

    private EditText editSsid;
    private EditText editPassword;
    private EditText editMasterHost;
    private EditText editMasterPort;
    private EditText editNfcAppId;
    private EditText editExtraData;
    private EditText editAppRecord;

    private Button buttonWrite;
    private TextView messageText;

    private RoconNfcManager mNfcManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editSsid = (EditText) findViewById(R.id.ssid);
        editPassword = (EditText) findViewById(R.id.pasword);
        editMasterHost = (EditText) findViewById(R.id.master_host);
        editMasterPort = (EditText) findViewById(R.id.master_port);
        editNfcAppId = (EditText) findViewById(R.id.nfc_app_id);
        editExtraData = (EditText) findViewById(R.id.extra_data);
        editAppRecord = (EditText) findViewById(R.id.app_record);

        buttonWrite = (Button) findViewById(R.id.nfc_write);
        messageText = (TextView) findViewById(R.id.message);

        ssid       = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_SSID, editSsid.getText().toString());
        password   = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_PSWD, editPassword.getText().toString());
        masterHost = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_HOST, editMasterHost.getText().toString());
        masterPort = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_PORT, editMasterPort.getText().toString());
        nfcAppId   = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_NAID, editNfcAppId.getText().toString());
        extraData  = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_DATA, editExtraData.getText().toString());
        appRecord  = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_AAR,  editAppRecord.getText().toString());

        editSsid.setText(ssid);
        editPassword.setText(password);
        editMasterHost.setText(masterHost);
        editMasterPort.setText(masterPort);
        editNfcAppId.setText(nfcAppId);
        editExtraData.setText(extraData);
        editAppRecord.setText(appRecord);

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
                messageText.setText("You can write Nfc tag");
                messageText.setTextColor(0xFF00FF00);
                buttonWrite.setEnabled(true);
            }
        }
    }

    public void onNfcWrite(View v) {

        try {
            ssid       = editSsid.getText().toString();
            password   = editPassword.getText().toString();
            masterHost = editMasterHost.getText().toString();
            masterPort = editMasterPort.getText().toString();
            nfcAppId   = editNfcAppId.getText().toString();
            extraData  = editExtraData.getText().toString();
            appRecord  = editAppRecord.getText().toString();

            if (appRecord.length() > 58)  // TODO make limits a parameter or at least a constant
                throw new InvalidParameterException("AAR limited to 58 chars");

            byte[] content = Util.concat(Util.toFixSizeBytes(ssid,       (short)16, (byte)0),
                                         Util.toFixSizeBytes(password,   (short)16, (byte)0),
                                         Util.toFixSizeBytes(masterHost, (short)16, (byte)0),
                                         Util.toBytes(Short.decode(masterPort)),
                                         Util.toBytes(Short.decode(nfcAppId)),
                                         Util.toBytes(Short.decode(extraData)));

                // Max payload size of our Ultralight C NFC tags is 137 bytes, so...
                //   ssid             16 bytes
                //   pwd              16 bytes
                //   host             16 bytes
                //   port              2 bytes
                //   nfcid             2 bytes
                //   data              2 bytes
                //   TOTAL            54 bytes
                //   AAR header needs 25 bytes, so AR max size is 58 bytes

                boolean success = mNfcManager.writeTextNdefMessage(content, appRecord);

                if(success) {
                    messageText.setText("Success to write NFC Tag!");
                    messageText.setTextColor(0xFF00FF00);
                }
                else {
                    messageText.setText("Fail to write NFC Tag!");
                    messageText.setTextColor(0xFFFF0000);
                }

                SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                editor.putString(PREFS_KEY_SSID, ssid);
                editor.putString(PREFS_KEY_PSWD, password);
                editor.putString(PREFS_KEY_HOST, masterHost);
                editor.putString(PREFS_KEY_PORT, masterPort);
                editor.putString(PREFS_KEY_NAID, nfcAppId);
                editor.putString(PREFS_KEY_DATA, extraData);
                editor.putString(PREFS_KEY_AAR,  appRecord);
                editor.commit();
        }
        catch (InvalidParameterException e) {
            Toast.makeText(this, "Respect field max size: " + e.getMessage(), Toast.LENGTH_LONG).show();
            messageText.setText("Review fields format");
            messageText.setTextColor(0xFFFF0000);
        }
        catch (IllegalArgumentException e) {
            Toast.makeText(this, "Field must contain a short integer: " + e.getMessage(), Toast.LENGTH_LONG).show();
            messageText.setText("Review fields format");
            messageText.setTextColor(0xFFFF0000);
        }
    }
}
