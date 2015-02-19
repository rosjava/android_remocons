package com.github.rosjava.android_remocons.rocon_nfc_writer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.robotics_in_concert.rocon_rosjava_core.rosjava_utils.ByteArrays;
import com.github.rosjava.android_remocons.common_tools.nfc.NfcManager;

import java.security.InvalidParameterException;

import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_APP_RECORD_FIELD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_MASTER_HOST_FIELD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_PASSWORD_FIELD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_SSID_FIELD_LENGTH;

/**
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public class RoconNfcWriter extends Activity {

    private static final String PREFS_KEY_SSID = "SSID_KEY";
    private static final String PREFS_KEY_PSWD = "PSWD_KEY"; // password
    private static final String PREFS_KEY_HOST = "HOST_KEY"; // master URI host
    private static final String PREFS_KEY_PORT = "PORT_KEY"; // master URI port
    private static final String PREFS_KEY_HASH = "HASH_KEY"; // app hash code
    private static final String PREFS_KEY_DATA = "DATA_KEY"; // extra (app specific) data
    private static final String PREFS_KEY_AAR  =  "AAR_KEY"; // AAR (Android Application Record)

    private String ssid;
    private String password;
    private String masterHost;
    private String masterPort;
    private String appHash;
    private String extraData;
    private String appRecord;

    private EditText editSsid;
    private EditText editPassword;
    private EditText editMasterHost;
    private EditText editMasterPort;
    private EditText editAppHash;
    private EditText editExtraData;
    private EditText editAppRecord;

    private Button buttonWrite;
    private TextView messageText;

    private NfcManager mNfcManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editSsid       = (EditText) findViewById(R.id.ssid);
        editPassword   = (EditText) findViewById(R.id.pasword);
        editMasterHost = (EditText) findViewById(R.id.master_host);
        editMasterPort = (EditText) findViewById(R.id.master_port);
        editAppHash    = (EditText) findViewById(R.id.nfc_app_id);
        editExtraData  = (EditText) findViewById(R.id.extra_data);
        editAppRecord  = (EditText) findViewById(R.id.app_record);

        buttonWrite = (Button) findViewById(R.id.nfc_write);
        messageText = (TextView) findViewById(R.id.message);

        ssid       = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_SSID, editSsid.getText().toString());
        password   = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_PSWD, editPassword.getText().toString());
        masterHost = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_HOST, editMasterHost.getText().toString());
        masterPort = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_PORT, editMasterPort.getText().toString());
        appHash    = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_HASH, editAppHash.getText().toString());
        extraData  = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_DATA, editExtraData.getText().toString());
        appRecord  = getPreferences(MODE_PRIVATE).getString(PREFS_KEY_AAR,  editAppRecord.getText().toString());

        editSsid.setText(ssid);
        editPassword.setText(password);
        editMasterHost.setText(masterHost);
        editMasterPort.setText(masterPort);
        editAppHash.setText(appHash);
        editExtraData.setText(extraData);
        editAppRecord.setText(appRecord);

        mNfcManager = new NfcManager(this);
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
            appHash    = editAppHash.getText().toString();
            extraData  = editExtraData.getText().toString();
            appRecord  = editAppRecord.getText().toString();

            if (appRecord.length() > NFC_APP_RECORD_FIELD_LENGTH)
                throw new InvalidParameterException("AAR limited to " + NFC_APP_RECORD_FIELD_LENGTH + " chars");

            byte[] content = ByteArrays.concat(ByteArrays.toFixSizeBytes(ssid, NFC_SSID_FIELD_LENGTH, (byte)0),
                                         ByteArrays.toFixSizeBytes(password, NFC_PASSWORD_FIELD_LENGTH, (byte)0),
                                         ByteArrays.toFixSizeBytes(masterHost, NFC_MASTER_HOST_FIELD_LENGTH, (byte)0),
                                         ByteArrays.toBytes(Short.decode(masterPort)),
                                         ByteArrays.toBytes(Integer.decode(appHash)),
                                         ByteArrays.toBytes(Short.decode(extraData)));

                // Max payload size of our Ultralight C NFC tags is 137 bytes, so...
                //   ssid             16 bytes
                //   pwd              16 bytes
                //   host             16 bytes
                //   port              2 bytes
                //   hash              4 bytes
                //   data              2 bytes
                //   TOTAL            56 bytes
                // payload language and status byte require 3 bytes more
                // AAR header needs 22 bytes, so AR max size is 56 bytes

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
                editor.putString(PREFS_KEY_HASH, appHash);
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
