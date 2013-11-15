package com.github.rosjava.android_remocons.rocon_nfc_writer;

import java.security.InvalidParameterException;

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

        public static byte[] toFixSizeBytes(String input, short length, byte padding) {
            if (input.length() > length)
                throw new InvalidParameterException(length + "exceeds limit in "
                                                  + (input.length() - length) + " chars");

            byte[] result = new byte[length];
            byte[] source = input.getBytes();
            for (int i = 0; i < length; i++)
                result[i] = i < source.length ? source[i] : padding;

            return result;
        }

        public static byte[] toBytes(short input)
        {
            byte[] result = new byte[2];
            result[0] = (byte) (input >> 8);
            result[1] = (byte) (input);

            return result;
        }
}
