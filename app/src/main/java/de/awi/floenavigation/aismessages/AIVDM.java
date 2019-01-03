package de.awi.floenavigation.aismessages;

import android.util.Log;

import java.util.Arrays;

public class AIVDM {

    private static final String TAG = "AIVDM";
    private String packetName;
    private int fragCount;
    private int fragNum;
    private int seqMsgID;
    private char channelCode;
    private String payload;
    private String eod;

    AIVDM()
    {
        packetName = null;
        fragCount = -1;
        fragNum = -1;
        seqMsgID = -1;
        channelCode = '-';
        payload = null;
        eod = null;
    }

    public String getPacketName()
    {
        return packetName;
    }

    public int getFragCount()
    {
        return fragCount;
    }

    public int getSeqMsgID()
    {
        return seqMsgID;
    }

    public char getChannelCode()
    {
        return channelCode;
    }

    public String getPayload()
    {
        return payload;
    }

    public String getEod()
    {
        return eod;
    }


    //Should be called by the WiFi Service
    public void setData(String[] dataExtr)
    {
        try{
            packetName = dataExtr[0];
            fragCount = Integer.parseInt(dataExtr[1].split("\\.")[0]);
            fragNum = Integer.parseInt(dataExtr[2].split("\\.")[0]);
            seqMsgID = (("".equals(dataExtr[3]))? -1 : Integer.parseInt(dataExtr[3]));
            channelCode = ((dataExtr[4].length() > 0)? dataExtr[4].charAt(0) : '-');
            payload = dataExtr[5];
            eod = dataExtr[6].split("\\*")[1];
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public StringBuilder decodePayload()
    {

        char[] array = payload.toCharArray();
        StringBuilder binary = new StringBuilder();

        //converting to ascii
        for(char ch : array)
        {
            int val = (int)ch;
            int value = asciitodec(val);
            String binVal = String.format("%6s",Integer.toString(value,2)).replace(' ','0');
            binary.append(binVal);
        }

        return binary;
        /*
        byte[] bytes = payload.getBytes();
        StringBuilder binary = new StringBuilder();
        for (byte b : bytes)
        {
            int val = b;
            for (int i = 0; i < 8; i++)
            {
                binary.append((val & 128) == 0 ? 0 : 1);
                val <<= 1;
            }
            // binary.append(' ');
        }
        return binary; */
    }


    private int asciitodec(int val)
    {
        val = val - 48;
        if(val > 40)
            val -= 8;
        return val;
    }

    static <T> Object strbuildtodec(int begin, int end, int len, StringBuilder binLocal, Class<?> type)
    {
        try{

            char[] array = new char[len];
            binLocal.getChars(begin,(end + 1),array,0);

            long decimal = 0;
            for(int pow = len; pow > 0; pow--)
            {
                if(array[pow - 1] == '1')
                    decimal += Math.pow(2,len - pow);
            }
    //		System.out.println("dec: " + decimal);
            if(type == int.class)
                return (int)(long)decimal;
            else
                return decimal;
        }catch (IndexOutOfBoundsException e) {
            String text = String.valueOf(e.getStackTrace());
            Log.d(TAG, text);
            if(type == int.class)
                return (int)(long)0;
            else
                return (long) 0;
            //return 0;
        }
        //return Integer.parseInt(new String(array));
    }

    public static String convertToString(int begin, int end, int len, StringBuilder bin){

        try {
            char[] array = new char[len];
            bin.getChars(begin, (end + 1), array, 0);
            int binLen = 6;
            StringBuilder stringValue = new StringBuilder();

            //char[] array = new char[binLen];
            int beginIndex = 0;
            int endIndex = 0;
            for (beginIndex = 0, endIndex = 6; endIndex < len; beginIndex += binLen, endIndex += binLen) {

                char[] newArray = Arrays.copyOfRange(array, beginIndex, endIndex);
                int length = newArray.length;
                long decimal = 0;
                for (int pow = length; pow > 0; pow--) {
                    if (newArray[pow - 1] == '1')
                        decimal += Math.pow(2, length - pow);
                }
                decimal = (int) (long) decimal;
                if((decimal >= 32 && decimal <= 63) || decimal == 0 ) {
                    //do Nothing
                } else {
                    decimal += 64;
                }

                stringValue.append((char) decimal);

            }

            return stringValue.toString().trim();
        }catch (IndexOutOfBoundsException e) {
            String text = String.valueOf(e.getStackTrace());
            Log.d(TAG, text);
            return null;
        }
    }

};
