package de.awi.floenavigation.aismessages;


import static de.awi.floenavigation.aismessages.AIVDM.convertToString;
import static de.awi.floenavigation.aismessages.AIVDM.strbuildtodec;

public class StaticDataReport {

    private int msgInd;
    private int repeatInd;
    private long mmsi;
    private int partNum;
    private String vesselName;
    private int spare_1;
    private int shipType;
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
