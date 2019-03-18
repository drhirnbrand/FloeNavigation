package de.awi.floenavigation.aismessages;

import android.util.Log;

import static de.awi.floenavigation.aismessages.AIVDM.strbuildtodec;

/**
 * Class to process Type 1, 2 and 3 messages of class A transponders which shares a common reporting structure for navigational information;
 * In normal operation, an AIS transceiver will broadcast a position report (type 1, 2, or 3) every 2 to 10 seconds
 * depending on the vessel’s speed while underway, and every 3 minutes while the vessel is at anchor and stationary. It will send a type 5 identification every 6 minutes.
 */
public class PostnReportClassA {

    private static final String TAG = "PostnReportClassA";
    /**
     * Message type, constant value ranging from 1-3
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
     * It stores the navigation status of the AIS transponder
     */
    private int status;
    /**
     * Values between 0 and 708 degrees/min coded by ROTAIS=4.733 * SQRT(ROTsensor) degrees/min
     * where ROTsensor is the Rate of Turn as input by an external Rate of Turn Indicator.
     * ROTAIS is rounded to the nearest integer value. Thus, to decode the field value, divide by 4.733
     * and then square it. Sign of the field value should be preserved when squaring it, otherwise the
     * left/right indication will be lost.
     */
    private int turn;
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
    /**
     * Value 1 : No special maneuver
     * Value 2 : Special maneuver (such as regional passing arrangement)
     */
    private int maneuver;
    /**
     * The RAIM flag indicates whether Receiver Autonomous Integrity Monitoring is being used to
     * check the performance of the EPFD. 0 = RAIM not in use (default), 1 = RAIM in use.
     */
    private boolean raim;
    /**
     * Diagnostic information for the radio system
     */
    private long radio;

    /**
     * Constructor to initialize the fields with default values
     */
    public PostnReportClassA()
    {
        msgInd = -1;
        repeatInd = -1;
        mmsi =  -1;
        status = -1;
        turn = -1;
        speed = -1;
        accuracy = false;
        lon = -1;
        lat = -1;
        course = -1;
        heading = -1;
        sec = -1;
        maneuver = -1;
        raim = false;
        radio = -1;

    }

    /**
     * @return returns the value of {@link PostnReportClassA#msgInd}
     */
    public int getMsgInd()
    {
        return msgInd;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#repeatInd}
     */
    public int getRepeatInd()
    {
        return repeatInd;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#mmsi}
     */
    public long getMMSI()
    {
        return mmsi;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#status}
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#turn}
     */
    public int getTurn()
    {
        return turn;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#speed}
     */
    public double getSpeed()
    {
        return speed;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#accuracy}
     */
    public boolean getAccuracy()
    {
        return accuracy;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#lon}
     */
    public double getLongitude()
    {
        return lon;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#lat}
     */
    public double getLatitude()
    {
        return lat;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#course}
     */
    public double getCourse()
    {
        return course;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#heading}
     */
    public int getHeading()
    {
        return heading;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#sec}
     */
    public int getSeconds()
    {
        return sec;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#maneuver}
     */
    public int getManeuver()
    {
        return maneuver;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#raim}
     */
    public boolean getRaim()
    {
        return raim;
    }

    /**
     * @return returns the value of {@link PostnReportClassA#radio}
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
        status = (int)strbuildtodec(38,41,4,bin,int.class);
        turn = (int)strbuildtodec(42,49,8,bin,int.class);
        speed = (long)strbuildtodec(50,59,10,bin,long.class)/ 10.0;
        accuracy = ((int) (strbuildtodec(60, 60, 1, bin, int.class)) > 0 );
        lon = (long)strbuildtodec(61,88,28,bin,long.class)/600000.0;
        lat = (long)strbuildtodec(89,115,27,bin,long.class)/600000.0;
        course = (long)strbuildtodec(116,127,12,bin,long.class)/ 10.0;
        heading = (int)strbuildtodec(128,136,9,bin,int.class);
        sec = (int)strbuildtodec(137,142,6,bin,int.class);
        maneuver = (int)strbuildtodec(143,144,2,bin,int.class);
        raim = ((int) (strbuildtodec(148,148,1,bin,int.class)) > 0);
        radio = (long)strbuildtodec(149,167,19,bin,long.class);
    }


};
