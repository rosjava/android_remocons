package com.github.rosjava.android_remocons.common_tools.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import com.github.robotics_in_concert.rocon_rosjava_core.rosjava_utils.ByteArrays;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Locale;

public class NfcManager {

    private Context mContext = null;
    private PendingIntent mPendingIntent = null;
    private IntentFilter[] mFilters;
    private String[][] mTechList;
    private Intent mPassedIntent = null;
    private NfcAdapter mNfcAdapter = null;
    private String mCurrentNdefString = "";

    public NfcManager(Context context) {
        mContext = context;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);

        Intent targetIntent = new Intent(mContext, mContext.getClass());
        targetIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(mContext, 0, targetIntent, 0);

        IntentFilter filter_1 = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter filter_2 = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter filter_3 = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);

        try {
            filter_1.addDataType("*/*");
            filter_2.addDataType("*/*");
            filter_3.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }

        mFilters = new IntentFilter[]{filter_1, filter_2, filter_3};
        mTechList = new String[][]{new String[]{NfcF.class.getName()},
                new String[]{MifareClassic.class.getName()},
                new String[]{NfcA.class.getName()},
                new String[]{NfcB.class.getName()},
                new String[]{NfcV.class.getName()},
                new String[]{Ndef.class.getName()},
                new String[]{NdefFormatable.class.getName()},
                new String[]{MifareUltralight.class.getName()},
                new String[]{IsoDep.class.getName()}};
    }

    public boolean checkNfcStatus() {
        return mNfcAdapter.isEnabled();
    }

    public boolean changeNfcStatus(boolean enable) {

        if (mNfcAdapter == null) return false;

        boolean success = false;
        Class<?> nfcManagerClass = null;
        Method setNfcEnabled = null, setNfcDisabled = null;

        if (enable) {
            try {
                nfcManagerClass = Class.forName(mNfcAdapter.getClass().getName());
                setNfcEnabled = nfcManagerClass.getDeclaredMethod("enable");
                setNfcEnabled.setAccessible(true);
                success = (Boolean) setNfcEnabled.invoke(mNfcAdapter);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                System.out.println(e.toString());
            }

        } else {
            try {
                nfcManagerClass = Class.forName(mNfcAdapter.getClass().getName());
                setNfcDisabled = nfcManagerClass.getDeclaredMethod("disable");
                setNfcDisabled.setAccessible(true);
                success = (Boolean) setNfcDisabled.invoke(mNfcAdapter);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return success;
    }

    public boolean enableForegroundDispatch() {
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch((Activity) mContext, mPendingIntent, mFilters, mTechList);
            return true;
        } else {
            return false;
        }
    }

    public boolean disableForegroundDispatch() {

        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch((Activity) mContext);
            return true;
        } else {
            return false;
        }
    }

    public boolean onNewIntent(Intent intent) {

        mPassedIntent = intent;
        String action = mPassedIntent.getAction();

        Toast.makeText(mContext, action, Toast.LENGTH_SHORT).show();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_NDEF_DISCOVERED.equalsIgnoreCase(action))
            return true;
        else
            return false;
    }

    public String processTag() {

        if (mPassedIntent == null) return "NFC Tag is not discovered.";

        Parcelable[] rawMsgs = mPassedIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        if (rawMsgs == null) {
            return "NDEF Message is null";
        }

        mCurrentNdefString = "";

        NdefMessage[] msgs = new NdefMessage[rawMsgs.length];

        for (int i = 0; i < rawMsgs.length; i++) {
            msgs[i] = (NdefMessage) rawMsgs[i];
            mCurrentNdefString += ndefMessageToString(msgs[i]);
        }

        return mCurrentNdefString;
    }

    public String ndefMessageToString(NdefMessage message) {

        String ndefString = "";
        NdefRecord[] ndef_records = message.getRecords();

        ndefString += "**Num of NdefRecord : " + ndef_records.length + "\n";

        for (int i = 0; i < ndef_records.length; i++) {
            String temp = "**Record No. " + i + "\n";
            byte[] type = ndef_records[i].getType();
            byte[] id = ndef_records[i].getId();
            byte[] pl = ndef_records[i].getPayload();
            byte[] arr = ndef_records[i].toByteArray();

            temp = temp + "- TNF=" + ndef_records[i].getTnf() +
                       "\n - TYPE=" + ByteArrays.getHexString(type, type.length) + " " + new String(type) +
                       "\n - ID=" + ByteArrays.getHexString(id, id.length) + " " + new String(id) +
                       "\n - PayLoad=" + ByteArrays.getHexString(pl, pl.length) + " " + new String(pl) +
                       "\n - ByteArray=" + ByteArrays.getHexString(arr, arr.length) + " " + new String(arr) + "\n";

            ndefString += temp;
        }

        return ndefString;
    }

    public byte[] getPayload() {

        if (mPassedIntent == null)
            return null;

        Parcelable[] rawMsgs = mPassedIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        if (rawMsgs == null) {
            return null;
        }

        NdefMessage[] msgs = new NdefMessage[rawMsgs.length];

        for (int i = 0; i < rawMsgs.length; i++) {
            msgs[i] = (NdefMessage) rawMsgs[i];
        }

        NdefRecord[] records = msgs[0].getRecords();
        if (records.length > 0)
            return records[0].getPayload();

        return null;
    }

    private NdefRecord createTextRecord(String text, Locale locale, boolean encodeInUtf8) {

        final byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));
        final Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        final byte[] textBytes = text.getBytes(utfEncoding);
        final int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        final char status = (char) (utfBit + langBytes.length);
        final byte[] data = ByteArrays.concat(new byte[]{(byte) status}, langBytes, textBytes);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }

    public boolean writeTextNdefMessage(String payload, boolean isAAR) {
        NdefRecord record = createTextRecord(payload, Locale.KOREAN, true);
        NdefMessage msg = null;
        if (isAAR)
            msg = new NdefMessage(new NdefRecord[]{record, NdefRecord.createApplicationRecord(mContext.getPackageName())});
        else
            msg = new NdefMessage(new NdefRecord[]{record});

        return writeNdefMessage(msg);
    }

    public boolean writeTextNdefMessage(byte[] payload, String AAR) {
        final byte[] langBytes = Locale.KOREAN.getLanguage().getBytes(Charset.forName("US-ASCII"));
        final Charset utfEncoding = Charset.forName("UTF-8");
        final int utfBit = 0;
        final char status = (char) (utfBit + langBytes.length);
        final byte[] data = ByteArrays.concat(new byte[]{(byte) status}, langBytes, payload);

        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
        NdefMessage msg = null;
        if (AAR != null)
            msg = new NdefMessage(new NdefRecord[]{record, NdefRecord.createApplicationRecord(AAR)});
        else
            msg = new NdefMessage(new NdefRecord[]{record});

        return writeNdefMessage(msg);
    }

    public boolean writeUriNdefMessage(String payload, String AAR) {

        NdefRecord record = new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI,
                NdefRecord.RTD_URI, new byte[0], payload.getBytes());
        NdefMessage msg = null;
        if (AAR != null)
            msg = new NdefMessage(new NdefRecord[]{record, NdefRecord.createApplicationRecord(AAR)});
        else
            msg = new NdefMessage(new NdefRecord[]{record});

        return writeNdefMessage(msg);
    }

    public boolean writeMimeNdefMessage(String payload, String AAR) {

        NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                ("application/" + mContext.getPackageName()).getBytes(), new byte[0], payload.getBytes());
        NdefMessage msg = null;
        if (AAR != null)
            msg = new NdefMessage(new NdefRecord[]{record, NdefRecord.createApplicationRecord(AAR)});
        else
            msg = new NdefMessage(new NdefRecord[]{record});

        return writeNdefMessage(msg);
    }

// This needs API 16 and we will not use it by now, so commented
//    public boolean writeCustomNdefMessage(String payload, String AAR) {
//
//        NdefRecord record = NdefRecord.createExternal("Yujin Robot", "Cafe demo",  payload.getBytes());
//        NdefMessage msg = null ;
//        if (AAR != null)
//            msg = new NdefMessage(new NdefRecord[] {record, NdefRecord.createApplicationRecord(AAR)});
//        else
//            msg = new NdefMessage(new NdefRecord[] {record});
//
//        return writeNdefMessage(msg);
//    }

    public boolean writeNdefMessage(NdefMessage message) {

        if (mPassedIntent == null) return false;

        Tag tag = mPassedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Ndef ndefTag = Ndef.get(tag);

        try {
            ndefTag.connect();
        } catch (IOException e) {
            Log.e("NfcWriter", "Connect to tag failed. " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        try {
            ndefTag.writeNdefMessage(message);
        } catch (TagLostException e) {
            Log.e("NfcWriter", "The tag left the field. " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            Log.e("NfcWriter", "Message writing failure. " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (FormatException e) {
            Log.e("NfcWriter", "Malformed NDEF message. " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        try {
            ndefTag.close();
        } catch (IOException e) {
            Log.e("NfcWriter", "Close tag failed. " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
