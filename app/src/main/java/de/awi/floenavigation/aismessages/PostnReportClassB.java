package de.awi.floenavigation.aismessages;

import static de.awi.floenavigation.aismessages.AIVDM.strbuildtodec;

/**
 * Class to process Type 18 messages for vessels using Class B transmitters
 * In normal operation, an AIS transceiver will broadcast a position report (type 1, 2, or 3) every 2 to 10 seconds
 * depending on the vessel’s speed while underway, and every 3 minutes while the vessel is at anchor and stationary. It will send a type 5 identification every 6 minutes.
 */
public class PostnReportClassB {

    /**
     * Message type, constant value of 18
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
    private int regReserved1;
    /**
     * Speed over ground is in 0.1-knot resolution from 0 to 102 knots.
     * Value 1023 indicates speed is not available, value 1022 indicates 102.2 knots or higher.
     */
    private double speed;
    /**
     * The position accuracy flag indicates the accuracy of the fix.
     * A value of 1 indicates a DGPS-quality fix with an accuracy of < 10ms. 0,
     * the default, indicates an unaugmented GNSS fix with accuracy > 10m.
     */
    private boolean accuracy;
    /**
     * Longitude is given in in 1/10000 min; divide by 600000.0 to obtain degrees.
     * Values up to plus or minus 180 degrees, East = positive, West \= negative.
     * A value of 181 degrees (0x6791AC0 hex) indicates that longitude is not available and is the default.
     */
    private double lon;
    /**
     * Latitude is given in in 1/10000 min; divide by 600000.0 to obtain degrees.
     * Values up to plus or minus 90 degrees, North = positive, South = negative.
     * A value of 91 degrees (0x3412140 hex) indicates latitude is not available and is the default.
     */
    private double lat;
    /**
     * Course over ground will be 3600 (0xE10) if that data is not available.
     */
    private double course;
    /**
     * 0 to 359 degrees, 511 = not available.
     */
    private int heading;
    /**
     * Second of UTC timestamp
     */
    private int sec;
    private int regReserved2;
    /**
     * 0=Class B SOTDMA unit 1=Class B CS (Carrier Sense) unit
     */
    private boolean csUnit;
    /**
     * 0=No visual display, 1=Has display, (Probably not reliable).
     */
    private boolean dispFlag;
    /**
     *
     * If 1, unit is attached to a VHF voice radio with DSC capability.
     */
    private boolean dscFlag;
    /**
     * Base stations can command units to switch frequency.
     * If this flag is 1, the unit can use any part of the marine channel.
     */
    private boolean bandFlag;
    /**
     *
     * If 1, unit can accept a channel assignment via Message Type 22.
     */
    private boolean msg22Flag;
    /**
     *
     * Assigned-mode flag: 0 = autonomous mode (default), 1 = assigned mode.
     */
    private boolean assigned;
    /**
     * The RAIM flag indicates whether Receiver Autonomous Integrity Monitoring is being used to
     * check the performance of the EPFD. 0 = RAIM not in use (default), 1 = RAIM in use.
     */
    private boolean raim;
    /**
     * Diagnostic information for the radio system
     */
    private int radio;

    /**
     * Constructor to initialize the fields with default values
     */
    public PostnReportClassB()
    {
        msgInd = -1;
        repeatInd = -1;
        mmsi =  -1;
        regReserved1 = -1;
        speed = -1;
        accuracy = false;
        lon = -1;
        lat = -1;
        course = -1;
        heading = -1;
        sec = -1;
        regReserved2 = -1;
        csUnit = false;
        dispFlag = false;
        dscFlag = false;
        bandFlag = false;
        msg22Flag = false;
        assigned = false;
        raim = false;
        radio = -1;

    }

    /**
     * @return returns the value of {@link PostnReportClassB#msgInd}
     */
    public int getMsgInd()
    {
        return msgInd;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#repeatInd}
     */
    public int getRepeatInd()
    {
        return repeatInd;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#mmsi}
     */
    public long getMMSI()
    {
        return mmsi;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#regReserved1}
     */
    public int getRegReserved1()
    {
        return regReserved1;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#speed}
     */
    public double getSpeed()
    {
        return speed;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#accuracy}
     */
    public boolean getAccuracy()
    {
        return accuracy;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#lon}
     */
    public double getLongitude()
    {
        return lon;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#lat}
     */
    public double getLatitude()
    {
        return lat;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#course}
     */
    public double getCourse()
    {
        return course;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#heading}
     */
    public int getHeading()
    {
        return heading;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#sec}
     */
    public int getSeconds()
    {
        return sec;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#regReserved2}
     */
    public int getRegReserved2()
    {
        return regReserved2;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#csUnit}
     */
    public boolean getCsUnit()
    {
        return csUnit;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#dispFlag}
     */
    public boolean getDispFlag()
    {
        return dispFlag;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#dscFlag}
     */
    public boolean getDscFlag()
    {
        return dscFlag;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#bandFlag}
     */
    public boolean getBandFlag()
    {
        return bandFlag;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#msg22Flag}
     */
    public boolean getMsg22Flag()
    {
        return msg22Flag;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#assigned}
     */
    public boolean getAssigned()
    {
        return assigned;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#raim}
     */
    public boolean getRaim()
    {
        return raim;
    }

    /**
     * @return returns the value of {@link PostnReportClassB#radio}
     */
    public long getRadio()
    {
        return radio;
    }

    /**
     * Function to assign the values of the various fields of the payload
     * @param bin Binary data equivalent of the ASCII character set of payload
     */
    public void setData(StringBuilder bin)
    {
        msgInd = (int)strbuildtodec(0,5,6,bin,int.class);
        repeatInd = (int)strbuildtodec(6,7,2,bin,int.class);
        mmsi =  (long)strbuildtodec(8,37,30,bin,long.class);
        regReserved1 = (int)strbuildtodec(38,45,8,bin,int.class);
        speed = (long)strbuildtodec(46,55,10,bin,long.class) / 10.0;
        accuracy = (int)strbuildtodec(56,56,1,bin,int.class) > 0;
        lon = (long)strbuildtodec(57,84,28,bin,long.class) / 600000.0;
        lat = (long)strbuildtodec(85,111,27,bin,long.class) / 600000.0;
        course = (long)strbuildtodec(112,123,12,bin,long.class) / 10.0;
        heading = (int)strbuildtodec(124,132,9,bin,int.class);
        sec = (int)strbuildtodec(133,138,6,bin,int.class);
        regReserved2 = (int)strbuildtodec(139,140,2,bin,int.class);
        csUnit = (int)strbuildtodec(141,141,1,bin,int.class) > 0;
        dispFlag = (int)strbuildtodec(142,142,1,bin,int.class) > 0;
        dscFlag = (int)strbuildtodec(143,143,1,bin,int.class) > 0;
        bandFlag = (int)strbuildtodec(144,144,1,bin,int.class) > 0;
        msg22Flag = (int)strbuildtodec(145,145,1,bin,int.class) > 0;
        assigned = (int)strbuildtodec(146,146,1,bin,int.class) > 0;
        raim = (int)strbuildtodec(147,147,1,bin,int.class) > 0;
        radio = (int)strbuildtodec(148,167,20,bin,int.class);
    }

};
