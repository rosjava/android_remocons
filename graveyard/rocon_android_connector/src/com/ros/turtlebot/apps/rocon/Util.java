package com.ros.turtlebot.apps.rocon;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.ros.exception.RosRuntimeException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.google.common.collect.Lists;

public class Util {

	// Hex help
		private static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1',
				(byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6',
				(byte) '7', (byte) '8', (byte) '9', (byte) 'A', (byte) 'B',
				(byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F' };
				
		public Util() {
			// TODO Auto-generated constructor stub
		}
		
		public static String getHexString(byte[] raw, int len) {
			byte[] hex = new byte[3 * len];
			int index = 0;
			int pos = 0;

			for (byte b : raw) {
				if (pos >= len)
					break;

				pos++;
				int v = b & 0xFF;
				hex[index++] = HEX_CHAR_TABLE[v >>> 4];
				hex[index++] = HEX_CHAR_TABLE[v & 0xF];
				hex[index++] = ' ';
			}

			return new String(hex);
		}
		
		public static byte[] concat(byte[]... arrays) {
	    	int length = 0;
	    	for (byte[] array : arrays) {
	    		length += array.length;
	    	}
	    	
	    	byte[] result = new byte[length];
	    	int pos = 0;
	    	for (byte[] array : arrays) {
	    		System.arraycopy(array, 0, result, pos, array.length);
	    		pos += array.length;
	    	}
	    	
	    	return result;
	    }
		
		public static String getWifiAddress(WifiManager wifiManager, long timeout ) {
			int interval = 500 ;		// ms
			int count = (int) timeout / interval ;
			int ip = 0 ;
			for(int i = 0 ; i <= count ; i++)
			{
				WifiInfo wInfo = wifiManager.getConnectionInfo() ;
				ip = wInfo.getIpAddress() ;
				if(ip == 0) {
					try {
						Thread.sleep(interval);
					} catch (InterruptedException e) { e.printStackTrace();}
				}
				else
					break ;
			}
			
			String ipString = String.format("%d.%d.%d.%d", (ip & 0xff), (ip>>8 & 0xff), (ip>>16 & 0xff), (ip>>24 & 0xff));
			return  ipString ;
		}
		

	    private static Collection<InetAddress> getAllInetAddresses() {
	        List<NetworkInterface> networkInterfaces;
	        try {
	          networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
	        } catch (SocketException e) {
	          throw new RosRuntimeException(e);
	        }
	        List<InetAddress> inetAddresses = Lists.newArrayList();
	        for (NetworkInterface networkInterface : networkInterfaces) {
	          inetAddresses.addAll(Collections.list(networkInterface.getInetAddresses()));
	        }
	        return inetAddresses;
	      }
	    
	    public static InetAddress getSiteLocalAddress() {
	    	for(InetAddress address : getAllInetAddresses()) {
		    	if( !address.isLoopbackAddress() && !address.isLinkLocalAddress() && address.isSiteLocalAddress())
		    		return address ;
		    }
	    	
	    	return null ;
	    }
	    
	    @SuppressLint("NewApi")
		public static String getAndroidVersionName() {
	    	String name = "";
	    	int version = -1 ;
	    	for(Field field : Build.VERSION_CODES.class.getFields()) {
	    		try {
					version = field.getInt(new Object());
				} catch (IllegalArgumentException e) { e.printStackTrace(); } 
	    		catch (IllegalAccessException e) {	e.printStackTrace(); }
	    		
	    		if(version == Build.VERSION.SDK_INT) {
	    			name = field.getName() ;
	    			Log.d("Util", "Android Version Name = " + name);
	    			return name ; 			
	    		}
	    	}
	    	Log.d("Util", "Cannot find Android Version Name");
	    	return name ;
	    }
		
		
}
