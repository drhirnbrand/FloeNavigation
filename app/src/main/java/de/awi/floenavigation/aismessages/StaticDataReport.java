package de.awi.floenavigation.aismessages;


import static de.awi.floenavigation.aismessages.AIVDM.convertToString;
import static de.awi.floenavigation.aismessages.AIVDM.strbuildtodec;

/**
 * Class to process Type 24 messages.
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
    /**
     * (Part B) Model Code - part of the last 4 characters of the 7 character string field
     */
    private int unitModelCode;
    /**
     * (Part B) Serial Number - part of the last 4 characters of the 7 character string field
     */
    private int serialNum;
    /**
     * 7 Six-bit characters
     */
    private String callSign;
    /**
     * (Part B) - meters
     */
    private int dimToBow;
    /**
     * (Part B) - meters
     */
    private int dimToStern;
    /**
     * (Part B) - meters
     */
    private int dimToPort;
    /**
     * (Part B) - meters
     */
    private int dimToStarBoard;
    /**
     * An MMSI is associated with an auxiliary craft when it is of the form 98XXXYYYY,
     * where (1) the 98 in positions 1 and 2 is required to designate an auxiliary craft,
     * (2) the digits XXX in the 3, 4 and 5 positions are the MID (the three-digit country code as described in [ITU-MID]) and
     * (3) YYYY is any decimal literal from 0000 to 9999.
     */
    private long mothershipMMSI;
    private int spare_2;

    /**
     * Constructor to initialize the fields with default values
     */
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

    /**
     * @return returns the value of {@link StaticDataReport#msgInd}
     */
    public int getMsgInd()
    {
        return msgInd;
    }

    /**
     * @return returns the value of {@link StaticDataReport#repeatInd}
     */
    public int getRepeatInd()
    {
        return repeatInd;
    }

    /**
     * @return returns the value of {@link StaticDataReport#mmsi}
     */
    public long getMMSI()
    {
        return mmsi;
    }

    /**
     * @return returns the value of {@link StaticDataReport#partNum}
     */
    public int getPartNum()
    {
        return partNum;
    }

    /**
     * @return returns the value of {@link StaticDataReport#vesselName}
     */
    public String getVesselName()
    {
        return vesselName;
    }

    /**
     * @return returns the value of {@link StaticDataReport#spare_1}
     */
    public int getSpare_1()
    {
        return spare_1;
    }

    /**
     * @return returns the value of {@link StaticDataReport#shipType}
     */
    public int getShipType()
    {
        return shipType;
    }

    /**
     * @return returns the value of {@link StaticDataReport#vendorID}
     */
    public String getvendorID()
    {
        return vendorID;
    }

    /**
     * @return returns the value of {@link StaticDataReport#unitModelCode}
     */
    public int getUnitModelCode()
    {
        return unitModelCode;
    }

    /**
     * @return returns the value of {@link StaticDataReport#serialNum}
     */
    public int getSerialNum()
    {
        return serialNum;
    }

    /**
     * @return returns the value of {@link StaticDataReport#callSign}
     */
    public String getCallSign()
    {
        return callSign;
    }

    /**
     * @return returns the value of {@link StaticDataReport#dimToBow}
     */
    public int getDimToBow()
    {
        return dimToBow;
    }

    /**
     * @return returns the value of {@link StaticDataReport#dimToStern}
     */
    public int getDimToStern()
    {
        return dimToStern;
    }

    /**
     * @return returns the value of {@link StaticDataReport#dimToPort}
     */
    public int getDimToPort()
    {
        return dimToPort;
    }

    /**
     * @return returns the value of {@link StaticDataReport#dimToStarBoard}
     */
    public int getDimToStarBoard()
    {
        return dimToStarBoard;
    }

    /**
     * @return returns the value of {@link StaticDataReport#mothershipMMSI}
     */
    public long getMothershipMMSI()
    {
        return mothershipMMSI;
    }

    /**
     * @return returns the value of {@link StaticDataReport#spare_2}
     */
    public int getSpare_2()
    {
        return spare_2;
    }

    /**
     * Function to assign the values of the various fields of the payload
     * @param bin Binary data equivalent of the ASCII character set of payload
     */
    public void setData(StringBuilder bin)
    {
        msgInd = (int)strbuildtodec(0,5,6,bin,int.class,false);
        repeatInd = (int)strbuildtodec(6,7,2,bin,int.class,false);
        mmsi =  (long)strbuildtodec(8,37,30,bin,long.class,false);
        partNum = (int)strbuildtodec(38,39,2,bin,int.class,false);
        vesselName = convertToString(40,159,120,bin);//strbuildtodec(40,159,120,bin,int.class).toString();
        //spare_1 = (int)strbuildtodec(160,167,8,bin,int.class);
        shipType = (int)strbuildtodec(40,47,8,bin,int.class,false);
        vendorID = convertToString(48,65,18,bin);//strbuildtodec(48,65,18,bin,int.class).toString();
        unitModelCode = (int)strbuildtodec(66,69,4,bin,int.class,false);
        serialNum = (int)strbuildtodec(70,89,20,bin,int.class,false);
        callSign =  convertToString(90,131,42,bin);//strbuildtodec(90,131,42,bin,int.class).toString();
        dimToBow = (int)strbuildtodec(132,140,9,bin,int.class,false);
        dimToStern = (int)strbuildtodec(141,149,9,bin,int.class,false);
        dimToPort = (int)strbuildtodec(150,155,6,bin,int.class,false);
        dimToStarBoard = (int)strbuildtodec(156,161,6,bin,int.class,false);
        mothershipMMSI =  (long)strbuildtodec(132,161,30,bin,long.class,false);
        //spare_2 = (int)strbuildtodec(162,167,6,bin,int.class);
    }




};
