package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

public class FixedStation {

    private static final String TAG = "FIXED_STATION";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private Context appContext;

    private String stationName;
    private double latitude;
    private double longitude;
    private double recvdLatitude;
    private double recvdLongitude;
    private double alpha;
    private double distance;
    private double xPosition;
    private double yPosition;
    private String stationType;
    private String updateTime;
    private double sog;
    private double cog;
    private int packetType;
    private int isPredicted;
    private int predictionAccuracy;
    private int isLocationReceived;
    private int mmsi;

    private ContentValues fixedStnContent;

    public FixedStation(Context context){
        appContext = context;
        try {
            dbHelper = DatabaseHelper.getDbInstance(appContext);
            db = dbHelper.getReadableDatabase();
        } catch(SQLException e){
            Log.d(TAG, "Database Exception");
            e.printStackTrace();
        }
    }

    private void generateContentValues(){
        fixedStnContent = new ContentValues();
        fixedStnContent.put(DatabaseHelper.stationName, this.stationName);
        fixedStnContent.put(DatabaseHelper.latitude, this.latitude);
        fixedStnContent.put(DatabaseHelper.longitude, this.longitude);
        fixedStnContent.put(DatabaseHelper.recvdLatitude, this.recvdLatitude);
        fixedStnContent.put(DatabaseHelper.recvdLongitude, this.recvdLongitude);
        fixedStnContent.put(DatabaseHelper.alpha, this.alpha);
        fixedStnContent.put(DatabaseHelper.distance, this.distance);
        fixedStnContent.put(DatabaseHelper.xPosition, this.xPosition);
        fixedStnContent.put(DatabaseHelper.yPosition, this.yPosition);
        fixedStnContent.put(DatabaseHelper.stationType, this.stationType);
        fixedStnContent.put(DatabaseHelper.updateTime, this.updateTime);
        fixedStnContent.put(DatabaseHelper.sog, this.sog);
        fixedStnContent.put(DatabaseHelper.cog, this.cog);
        fixedStnContent.put(DatabaseHelper.packetType, this.packetType);
        fixedStnContent.put(DatabaseHelper.isPredicted, this.isPredicted);
        fixedStnContent.put(DatabaseHelper.predictionAccuracy, this.predictionAccuracy);
        fixedStnContent.put(DatabaseHelper.isLocationReceived, this.isLocationReceived);
        fixedStnContent.put(DatabaseHelper.mmsi, this.mmsi);
    }

    public void insertFixedStationInDB(){
        generateContentValues();
        int result = db.update(DatabaseHelper.fixedStationTable, fixedStnContent, DatabaseHelper.mmsi + " = ?", new String[] {String.valueOf(mmsi)});
        if(result == 0){
            db.insert(DatabaseHelper.fixedStationTable, null, fixedStnContent);
            Log.d(TAG, "Fixed Station Added");
        } else{
            Log.d(TAG, "Fixed Station Updated");
        }
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getRecvdLatitude() {
        return recvdLatitude;
    }

    public void setRecvdLatitude(double recvdLatitude) {
        this.recvdLatitude = recvdLatitude;
    }

    public double getRecvdLongitude() {
        return recvdLongitude;
    }

    public void setRecvdLongitude(double recvdLongitude) {
        this.recvdLongitude = recvdLongitude;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getxPosition() {
        return xPosition;
    }

    public void setxPosition(double xPosition) {
        this.xPosition = xPosition;
    }

    public double getyPosition() {
        return yPosition;
    }

    public void setyPosition(double yPosition) {
        this.yPosition = yPosition;
    }

    public String getStationType() {
        return stationType;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public double getSog() {
        return sog;
    }

    public void setSog(double sog) {
        this.sog = sog;
    }

    public double getCog() {
        return cog;
    }

    public void setCog(double cog) {
        this.cog = cog;
    }

    public int getPacketType() {
        return packetType;
    }

    public void setPacketType(int packetType) {
        this.packetType = packetType;
    }

    public int getIsPredicted() {
        return isPredicted;
    }

    public void setIsPredicted(int isPredicted) {
        this.isPredicted = isPredicted;
    }

    public int getPredictionAccuracy() {
        return predictionAccuracy;
    }

    public void setPredictionAccuracy(int predictionAccuracy) {
        this.predictionAccuracy = predictionAccuracy;
    }

    public int getIsLocationReceived() {
        return isLocationReceived;
    }

    public void setIsLocationReceived(int isLocationReceived) {
        this.isLocationReceived = isLocationReceived;
    }

    public int getMmsi() {
        return mmsi;
    }

    public void setMmsi(int mmsi) {
        this.mmsi = mmsi;
    }
}
