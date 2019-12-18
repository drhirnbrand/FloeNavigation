package de.awi.floenavigation.util;

public abstract class Constants {

    public static final String DB_NAME = "FloeNavigation2";
    /**
     * Constant defining the MMSI of the Mothership which will be shown as a Star on the Mapview. By
     * default this should be the MMSI of
     * Polarstern.
     */

    //public static final int MOTHER_SHIP_MMSI = 211202460;
    //For Testing purposes
    public static final Long MOTHER_SHIP_MMSI = Long.valueOf(211202460);//230070870;

    public static final String EXTRA_AIS_PACKET_KEY = "AISPacket";
    /**
     * Default IP Address/Hostname of the AIS Transponder with which the App will try to make a
     * Telnet Connection to read the incoming
     * AIS Data Stream. Used by {@link de.awi.floenavigation.network.NetworkMonitor} and {@link
     * de.awi.floenavigation.aismessages.AISMessageReceiver} to
     * create a Telnet Connection.
     */
    public static final String DST_ADDRESS = "192.168.0.1";
    /**
     * Default Port on the AIS Transponder with which the App will try to make a Telnet Connection
     * to read the incoming
     * AIS Data Stream. Used by {@link de.awi.floenavigation.network.NetworkMonitor} and {@link
     * de.awi.floenavigation.aismessages.AISMessageReceiver} to
     * create a Telnet Connection.
     */
    public static final int DST_PORT = 2000;
}
