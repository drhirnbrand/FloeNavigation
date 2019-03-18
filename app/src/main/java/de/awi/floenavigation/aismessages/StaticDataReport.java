package de.awi.floenavigation.aismessages;


import static de.awi.floenavigation.aismessages.AIVDM.convertToString;
import static de.awi.floenavigation.aismessages.AIVDM.strbuildtodec;

/**
 * Class to process Type 5 messages of Class B transponders.
 * The packet is used to associate a MMSI with a name on either class A or class B equipment.
 *
 * A "Type 24" may be in part A or part B format;
 * According to the standard, parts A and B are expected to be broadcast in adjacent pairs;
 * in the real world they may (due to quirks in various aggregation methods) be separated by other sentences
 * or even interleaved with different Type 24 pairs; decoders must cope with this. The interpretation of
 * some fields in Type B format changes depending on the range of the Type B MMSI field.
 * 160 bits for part A, 168 bits for part B.
 */
public class StaticDataReport {

    /**
     * Message type, constant value of 24
     */
    private int msgInd;
    /**
     * Message repeat count
     * The Repeat Indicator is a directive to an AIS transceiver that this message should be rebroadcast.
     * This was intended as a way of getting AIS messages around hills and other obstructions in coastal waters,
     * but is little used as base station coverage is more effective. It is intended that the bit be incremented
     * on each retransmission, to a maximum of three hops. A value of 3 indicates "Do not repeat".
     */
    private int repeatInd;
    /**
     * An MMSI is a Mobile Marine Service Identifier, a unique 9-digit ID for the ship’s radio(s).
     * The first three digits convey information about the country in which the ID was issued.
     * Different formats of MMSI are used for different classes of transmitter.
     * A MID is a three-digit decimal literal ranging from 201 to 775 that identifies a country or other maritime jurisdiction.
     */
    private long mmsi;
    /**
     * If the Part Number field is 0, the rest of the message is interpreted as a Part A;
     * if it is 1, the rest of the message is interpreted as a Part B; values 2 and 3 are not allowed.
     */
    private int partNum;
    /**
     * 20 sixbit chars
     * Bits 48-89 are as described in ITU-R 1371-4. In earlier versions to 1371-3 this was one sixbit-encoded
     * 42-bit (7-character) string field, the name of the AIS equipment vendor.
     * The last 4 characters of the string are reinterpreted as a model/serial numeric pair.
     *
     */
    private String vesselName;
    private int spare_1;
    /**
     * Ship and cargo classification
     */
    private int shipType;
    /**
     * (Part B) 3 six-bit chars
     */
    private String vendorID;
    private int unitModelCode;
    private int serialNum;
    private String callSign;
    private int dimToBow;
    private int dimToStern;
    private int dimToPort;
    private int dimToStarBoard;
    private long mothershipMMSI;
    private int spare_2;

    public StaticDataReport()
    {

        msgInd = -1;
        repeatInd = -1;
        mmsi =  -1;
        partNum = -1;
        vesselName = "";
        spare_1 = -1;
        shipType = -1;
        vendorID = "";
        unitModelCode = -1;
        serialNum = -1;
        callSign = "";
        dimToBow = -1;
        dimToStern = -1;
        dimToPort = -1;
        dimToStarBoard = -1;
        mothershipMMSI = -1;
        spare_2 = -1;
    }

    public int getMsgInd()
    {
        return msgInd;
    }

    public int getRepeatInd()
    {
        return repeatInd;
    }

    public long getMMSI()
    {
        return mmsi;
    }

    public int getPartNum()
    {
        return partNum;
    }

    public String getVesselName()
    {
        return vesselName;
    }

    public int getSpare_1()
    {
        return spare_1;
    }

    public int getShipType()
    {
        return shipType;
    }

    public String getvendorID()
    {
        return vendorID;
    }

    public int getUnitModelCode()
    {
        return unitModelCode;
    }

    public int getSerialNum()
    {
        return serialNum;
    }

    public String getCallSign()
    {
        return callSign;
    }

    public int getDimToBow()
    {
        return dimToBow;
    }

    public int getDimToStern()
    {
        return dimToStern;
    }

    public int getDimToPort()
    {
        return dimToPort;
    }

    public int getDimToStarBoard()
    {
        return dimToStarBoard;
    }

    public long getMothershipMMSI()
    {
        return mothershipMMSI;
    }

    public int getSpare_2()
    {
        return spare_2;
    }

    public void setData(StringBuilder bin)
    {
        msgInd = (int)strbuildtodec(0,5,6,bin,int.class);
        repeatInd = (int)strbuildtodec(6,7,2,bin,int.class);
        mmsi =  (long)strbuildtodec(8,37,30,bin,long.class);
        partNum = (int)strbuildtodec(38,39,2,bin,int.class);
        vesselName = convertToString(40,159,120,bin);//strbuildtodec(40,159,120,bin,int.class).toString();
        //spare_1 = (int)strbuildtodec(160,167,8,bin,int.class);
        shipType = (int)strbuildtodec(40,47,8,bin,int.class);
        vendorID = convertToString(48,65,18,bin);//strbuildtodec(48,65,18,bin,int.class).toString();
        unitModelCode = (int)strbuildtodec(66,69,4,bin,int.class);
        serialNum = (int)strbuildtodec(70,89,20,bin,int.class);
        callSign =  convertToString(90,131,42,bin);//strbuildtodec(90,131,42,bin,int.class).toString();
        dimToBow = (int)strbuildtodec(132,140,9,bin,int.class);
        dimToStern = (int)strbuildtodec(141,149,9,bin,int.class);
        dimToPort = (int)strbuildtodec(150,155,6,bin,int.class);
        dimToStarBoard = (int)strbuildtodec(156,161,6,bin,int.class);
        mothershipMMSI =  (long)strbuildtodec(132,161,30,bin,long.class);
        //spare_2 = (int)strbuildtodec(162,167,6,bin,int.class);
    }




};
