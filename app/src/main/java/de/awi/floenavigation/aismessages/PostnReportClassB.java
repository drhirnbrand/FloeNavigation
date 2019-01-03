package de.awi.floenavigation.aismessages;

import static de.awi.floenavigation.aismessages.AIVDM.strbuildtodec;

public class PostnReportClassB {

    private int msgInd;
    private int repeatInd;
    private long mmsi;
    private int regReserved1;
    private double speed;
    private boolean accuracy;
    private double lon;
    private double lat;
    private double course;
    private int heading;
    private int sec;
    private int regReserved2;
    private boolean csUnit;
    private boolean dispFlag;
    private boolean dscFlag;
    private boolean bandFlag;
    private boolean msg22Flag;
    private boolean assigned;
    private boolean raim;
    private int radio;

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

    public int getRegReserved1()
    {
        return regReserved1;
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

    public int getRegReserved2()
    {
        return regReserved2;
    }

    public boolean getCsUnit()
    {
        return csUnit;
    }

    public boolean getDispFlag()
    {
        return dispFlag;
    }

    public boolean getDscFlag()
    {
        return dscFlag;
    }

    public boolean getBandFlag()
    {
        return bandFlag;
    }

    public boolean getMsg22Flag()
    {
        return msg22Flag;
    }

    public boolean getAssigned()
    {
        return assigned;
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
