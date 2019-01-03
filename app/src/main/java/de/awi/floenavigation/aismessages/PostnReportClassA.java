package de.awi.floenavigation.aismessages;

import android.util.Log;

import static de.awi.floenavigation.aismessages.AIVDM.strbuildtodec;

public class PostnReportClassA {

    private static final String TAG = "PostnReportClassA";
    private int msgInd;
    private int repeatInd;
    private long mmsi;
    private int status;
    private int turn;
    private double speed;
    private boolean accuracy;
    private double lon;
    private double lat;
    private double course;
    private int heading;
    private int sec;
    private int maneuver;
    private boolean raim;
    private long radio;

    public PostnReportClassA()
    {
//		System.out.println(bin);
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

    public int getStatus()
    {
        return status;
    }

    public int getTurn()
    {
        return turn;
    }

    public double getSpeed()
    {
        return speed;
    }

    public boolean getAccuracy()
    {
        return accuracy;
    }

    public double getLongitude()
    {
        return lon;
    }

    public double getLatitude()
    {
        return lat;
    }

    public double getCourse()
    {
        return course;
    }

    public int getHeading()
    {
        return heading;
    }

    public int getSeconds()
    {
        return sec;
    }

    public int getManeuver()
    {
        return maneuver;
    }

    public boolean getRaim()
    {
        return raim;
    }

    public long getRadio()
    {
        return radio;
    }

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
        /*
        Log.d(TAG, "msgInd " + msgInd);
        Log.d(TAG, "repeatInd " + repeatInd);
        Log.d(TAG, "mmsi " + mmsi);
        Log.d(TAG, "status " + status);
        Log.d(TAG, "turn " + turn);
        Log.d(TAG, "speed " + speed);
        Log.d(TAG, "accuracy " + accuracy);
        Log.d(TAG, "lon " + lon);
        Log.d(TAG, "lat " + lat);
        Log.d(TAG, "course " + course);
        Log.d(TAG, "heading " + heading);
        Log.d(TAG, "sec " + sec);
        Log.d(TAG, "maneuver " + maneuver);
        Log.d(TAG, "raim " + raim);
        Log.d(TAG, "radio " + radio);*/
    }


};
