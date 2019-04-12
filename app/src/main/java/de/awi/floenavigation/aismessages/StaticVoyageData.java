package de.awi.floenavigation.aismessages;


import static de.awi.floenavigation.aismessages.AIVDM.convertToString;
import static de.awi.floenavigation.aismessages.AIVDM.strbuildtodec;

/**
 * Class to process Type 5 messages of Class B transponders.
 * The packet is used to associate a MMSI with a name on either class A or class B equipment.
 * Message has a total of 424 bits, occupying two AIVDM sentences.
 * In practice, the information in these fields (especially ETA and destination) is not reliable,
 * as it has to be hand-updated by humans rather than gathered automatically from sensors.
 * Also note that it is fairly common in the wild for this message to have a wrong bit length (420 or 422).
 * Robust decoders should ignore trailing garbage and deal gracefully with a slightly truncated destination field.
 *
 */
public class StaticVoyageData {

    /**
     * Message type, constant value of 5
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
     * Version of the AIS
     * 0 - valid number
     * 1-3 - future editions
     */
    private int aisVersion;
    /**
     * IMO ship ID Number
     */
    private int imoNumber;
    /**
     * 7 Six-bit characters
     */
    private String callSign;
    /**
     * 20 sixbit chars
     * Bits 48-89 are as described in ITU-R 1371-4. In earlier versions to 1371-3 this was one sixbit-encoded
     * 42-bit (7-character) string field, the name of the AIS equipment vendor.
     * The last 4 characters of the string are reinterpreted as a model/serial numeric pair.
     *
     */
    private String vesselName;
    /**
     * Ship and cargo classification
     */
    private int shipType;
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
     * EPFD position fix types - GPS, GLONASS etc.
     */
    private int postnFixType;
    /**
     * 1-12; 0=N/A(default)
     */
    private int month;
    /**
     * 1-31; 0=N/A(default)
     */
    private int day;
    /**
     * 0-23; 24=N/A(default)
     */
    private int hour;
    /**
     * 0-59; 60=N/A(default)
     */
    private int minute;
    /**
     * Meters/10
     */
    private int draught;
    /**
     * 20 6-bit chars
     */
    private String destn;
    /**
     * 0=Data terminal ready
     * 1=Not ready(default)
     */
    private boolean dte;
    private int reserved;

    /**
     * Constructor to initialize the fields with default values
     */
    public StaticVoyageData()
    {

        msgInd = -1;
        repeatInd = -1;
        mmsi =  -1;
        aisVersion = -1;
        imoNumber = -1;
        callSign = "";
        vesselName = "";
        shipType = -1;
        dimToBow = -1;
        dimToStern = -1;
        dimToPort = -1;
        dimToStarBoard = -1;
        postnFixType = -1;
        month = -1;
        day = -1;
        hour = -1;
        minute = -1;
        month = -1;
        draught = -1;
        destn = "";
        dte = false;
        reserved = -1;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#msgInd}
     */
    public int getMsgInd()
    {
        return msgInd;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#repeatInd}
     */
    public int getRepeatInd()
    {
        return repeatInd;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#mmsi}
     */
    public long getMMSI()
    {
        return mmsi;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#aisVersion}
     */
    public int getAisVersion()
    {
        return aisVersion;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#imoNumber}
     */
    public int getImoNumber()
    {
        return imoNumber;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#callSign}
     */
    public String getCallSign()
    {
        return callSign;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#vesselName}
     */
    public String getVesselName()
    {
        return vesselName;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#shipType}
     */
    public int getShipType()
    {
        return shipType;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#dimToBow}
     */
    public int getDimToBow()
    {
        return dimToBow;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#dimToStern}
     */
    public int getDimToStern()
    {
        return dimToStern;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#dimToPort}
     */
    public int getDimToPort()
    {
        return dimToPort;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#dimToStarBoard}
     */
    public int getDimToStarBoard()
    {
        return dimToStarBoard;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#postnFixType}
     */
    public int getPostnFixType()
    {
        return postnFixType;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#month}
     */
    public int getMonth()
    {
        return month;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#day}
     */
    public int getDay()
    {
        return day;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#hour}
     */
    public int getHour()
    {
        return hour;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#minute}
     */
    public int getMinute()
    {
        return minute;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#draught}
     */
    public int getDraught()
    {
        return draught;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#destn}
     */
    public String getDestn()
    {
        return destn;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#dte}
     */
    public boolean getDte()
    {
        return dte;
    }

    /**
     * @return returns the value of {@link StaticVoyageData#reserved}
     */
    public int getReserved()
    {
        return reserved;
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
        aisVersion = (int)strbuildtodec(38,39,2,bin,int.class,false);
        imoNumber = (int)strbuildtodec(40,69,30,bin,int.class,false);
        callSign = convertToString(70,111,42,bin);//strbuildtodec(70,111,42,bin,int.class).toString();
        vesselName = convertToString(112,231,120,bin);//strbuildtodec(112,231,120,bin,int.class).toString();
        shipType = (int)strbuildtodec(232,239,8,bin,int.class,false);
        dimToBow = (int)strbuildtodec(240,248,9,bin,int.class,false);
        dimToStern = (int)strbuildtodec(249,257,9,bin,int.class,false);
        dimToPort = (int)strbuildtodec(258,263,6,bin,int.class,false);
        dimToStarBoard = (int)strbuildtodec(264,269,6,bin,int.class,false);
        postnFixType = (int)strbuildtodec(270,273,4,bin,int.class,false);
        month = (int)strbuildtodec(274,277,4,bin,int.class,false);
        day = (int)strbuildtodec(278,282,5,bin,int.class,false);
        hour = (int)strbuildtodec(283,287,5,bin,int.class,false);
        minute = (int)strbuildtodec(288,293,6,bin,int.class,false);
        draught = (int)strbuildtodec(294,301,8,bin,int.class,false) / 10;
       // destn = strbuildtodec(302,421,120,bin,int.class).toString();
        //dte = (int)strbuildtodec(422,422,1,bin,int.class) > 0;
        //reserved = (int)strbuildtodec(423,423,1,bin,int.class);
    }




};
