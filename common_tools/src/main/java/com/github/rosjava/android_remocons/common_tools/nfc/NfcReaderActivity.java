/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2013, Yujin Robot.
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.rosjava.android_remocons.common_tools.nfc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.github.robotics_in_concert.rocon_rosjava_core.rosjava_utils.ByteArrays;
import com.github.rosjava.android_remocons.common_tools.R;

import java.util.HashMap;

import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_MASTER_HOST_FIELD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_PASSWORD_FIELD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_PAYLOAD_LENGTH;
import static com.github.rosjava.android_remocons.common_tools.rocon.Constants.NFC_SSID_FIELD_LENGTH;

/**
 * Read NfcF tags and return the resulting hash map to the invoker action.
 */
public class NfcReaderActivity extends Activity {
    public static boolean enabled = true;

    private NfcManager nfcManager;
    private TextView textView;

    private HashMap<String, Object> data;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        try{
            setContentView(R.layout.nfc_tag_scan);
            textView = (TextView) findViewById(R.id.text);
            textView.setText("Scan a NFC tag");
            nfcManager = new NfcManager(this);
        }
        catch (Exception e) {
            Log.e("NfcReader", e.getMessage());
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nfcManager != null)
            nfcManager.enableForegroundDispatch();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if ((nfcManager != null) && (nfcManager.onNewIntent(intent))) {
            Log.i("NfcReader", "NFC tag read");
            byte[] payload = nfcManager.getPayload();
            if (payload.length != NFC_PAYLOAD_LENGTH + 3) // 1 byte for status and 2 lang bytes
            {
                Log.e("NfcReader", "Payload doesn't match expected length: " + payload.length +" != " + NFC_PAYLOAD_LENGTH);
                return;
            }

            data = new HashMap<String, Object>();

            int offset = 3; // skip 1 byte for status and 2 lang bytes
            data.put("WIFI", ByteArrays.toString(payload, offset, NFC_SSID_FIELD_LENGTH).trim());
            offset += NFC_SSID_FIELD_LENGTH;
            data.put("WIFIPW", ByteArrays.toString(payload, offset, NFC_PASSWORD_FIELD_LENGTH).trim());
            data.put("WIFIENC", "WPA2");
            offset += NFC_PASSWORD_FIELD_LENGTH;
            String host = ByteArrays.toString(payload, offset, NFC_MASTER_HOST_FIELD_LENGTH).trim();
            offset += NFC_MASTER_HOST_FIELD_LENGTH;
            short  port = ByteArrays.toShort(payload, offset);
            data.put("URL", "http://" + host + ":" + port);

            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcManager != null)
            nfcManager.disableForegroundDispatch();
    }

    @Override
    public void finish() {
        // Prepare data in tent
        Intent returnIntent = new Intent();
        if (data != null) {
            // Activity finished ok, return the data
            returnIntent.putExtra("tag_data", data);
            setResult(RESULT_OK, returnIntent);
        }
        else {
            setResult(RESULT_CANCELED, returnIntent);
        }

        super.finish();
    }
}
