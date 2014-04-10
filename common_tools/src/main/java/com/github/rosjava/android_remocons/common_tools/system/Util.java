package com.github.rosjava.android_remocons.common_tools.system;

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

    public static String toString(byte[] input, int offset, int count) {
        if ((offset + count) > input.length)
            throw new ArrayIndexOutOfBoundsException("Requested chunk exceeds byte array limits");

        byte[] result = new byte[count];
        for (int i = 0; i < count; i++)
            result[i] = input[offset + i];

        return new String(result);
    }

    public static short toShort(byte[] input, int offset) {
        if ((offset + 2) > input.length)
            throw new ArrayIndexOutOfBoundsException("Requested chunk exceeds byte array limits");

        return (short) (input[offset + 1] & 0xFF |
                       (input[offset + 0] & 0xFF) << 8);
    }


    public static int toInteger(byte[] input, int offset) {
        if ((offset + 4) > input.length)
            throw new ArrayIndexOutOfBoundsException("Requested chunk exceeds byte array limits");

        return   input[offset + 3] & 0xFF        |
                (input[offset + 2] & 0xFF) << 8  |
                (input[offset + 1] & 0xFF) << 16 |
                (input[offset + 0] & 0xFF) << 24;
    }

    public static byte[] toFixSizeBytes(String input, int length, byte padding) {
        if (input.length() > length)
            throw new InvalidParameterException(length + "exceeds limit in "
                                              + (input.length() - length) + " chars");

        byte[] result = new byte[length];
        byte[] source = input.getBytes();
        for (int i = 0; i < length; i++)
            result[i] = i < source.length ? source[i] : padding;

        return result;
    }

    public static byte[] toBytes(int input)
    {
        byte[] result = new byte[4];
        result[0] = (byte) (input >> 24);
        result[1] = (byte) (input >> 16);
        result[2] = (byte) (input >> 8);
        result[3] = (byte) (input);

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
