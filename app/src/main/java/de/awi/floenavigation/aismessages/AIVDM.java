package de.awi.floenavigation.aismessages;

import android.util.Log;

import java.util.Arrays;

/**
 * This class is called from {@link AISDecodingService} to segregate the ASCII packet
 * on the basis of comma. The payload is segregated from the entire packet sequence and used for further
 * processing.
 */
public class AIVDM {

    /**
     * TAG used for logging
     */
    private static final String TAG = "AIVDM";
    /**
     * Stores the packet name, identifies this as an AIVDM or AIVDO packet.
     */
    private String packetName;
    /**
     * Stores the count of fragments in the currently accumulating message
     */
    private int fragCount;
    /**
     * Stores the fragment number of the received sentence
     */
    private int fragNum;
    /**
     * Stores the sequential message ID for multi-sentence messages
     */
    private int seqMsgID;
    /**
     * Stores the radio channel code. AIS uses the high side of the duplex from two VHF radio channels:
     * AIS Channel A is 161.975Mhz (87B);
     * AIS Channel B is 162.025Mhz (88B).
     */
    private char channelCode;
    /**
     * Stores the data payload
     */
    private String payload;
    /**
     * Stores the the number of fill bits requires to pad the data payload to a 6 bit boundary
     * and the NMEA 0183 data-integrity checksum for the sentence received.
     */
    private String eod;

    /**
     * Default constructor to initialize the packet fields
     */
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

    /**
     * Getter to get the packet name
     * @return returns the packet name
     */
    public String getPacketName()
    {
        return packetName;
    }

    /**
     * Getter to get the frag count
     * @return returns the count of fragments
     */
    public int getFragCount()
    {
        return fragCount;
    }

    /**
     * Getter to get the sequential message ID
     * @return returns the sequential message ID
     */
    public int getSeqMsgID()
    {
        return seqMsgID;
    }

    /**
     * Getter to get the channel code
     * @return returns the channel code
     */
    public char getChannelCode()
    {
        return channelCode;
    }

    /**
     * Getter to get the payload
     * @return returns the payload
     */
    public String getPayload()
    {
        return payload;
    }

    /**
     * Getter to get the checksum
     * @return returns the checksum received in the sentence
     */
    public String getEod()
    {
        return eod;
    }

    /**
     * Sets the data to each of the variables by splitting the packet
     * Used regular expressions to split the data
     * @param dataExtr
     */
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

    /**
     * Used to change ASCII characters of the payload to decimal and then to binary format
     * @return returns the binary output of the ASCII payload
     */
    StringBuilder decodePayload()
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
    }


    /**
     * Called from {@link AIVDM#decodePayload()}
     * @param val receives the ascii character value in decimal and does manipulation of the value as per the protocol
     * @return returns the manipulated decimal value
     */
    private int asciitodec(int val)
    {
        val = val - 48;
        if(val > 40)
            val -= 8;
        return val;
    }

    /**
     * Generic function implemented to process the payload
     * Receives the binary data and splits according to the begin and the end index and converts to a decimal value
     * which represents a valid data for the various fields of the AIS payload
     * @param begin index of the beginning of a member of the AIS payload
     * @param end index of the end of a member of the AIS payload
     * @param len represents the length of the member
     * @param binLocal Binary equivalent of the payload
     * @param type Datatype value used to truncate the data field as per the datatype value expected from the calling function
     * @param <T> Generic data type (template concept of java)
     * @return returns the decimal equivalent
     */
    static <T> Object strbuildtodec(int begin, int end, int len, StringBuilder binLocal, Class<?> type, boolean signedNum)
    {
        try{

            char[] array = new char[len];
            binLocal.getChars(begin,(end + 1),array,0);

            if(signedNum && array[0] == '1'){
                String str = new String(array);
                Long decValue = Long.parseLong(str, 2);
                char[] invert = new char[len];
                Arrays.fill(invert, '1');
                decValue ^= Integer.parseInt(new String(invert), 2);
                decValue += 1;
                decValue = -decValue;
                if(type == int.class)
                    return (int)(long)decValue;
                else
                    return decValue;
            }

            long decimal = 0;
            for(int pow = len; pow > 0; pow--)
            {
                if(array[pow - 1] == '1')
                    decimal += Math.pow(2,len - pow);
            }

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

        }

    }

    /**
     * Generic function to process the payload and return the value in form of String instead of decimal value
     * @param begin index of the beginning of a member of the AIS payload
     * @param end index of the end of a member of the AIS payload
     * @param len represents the length of the member
     * @param bin Binary equivalent of the payload
     * @return returns the String equivalent
     */
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
