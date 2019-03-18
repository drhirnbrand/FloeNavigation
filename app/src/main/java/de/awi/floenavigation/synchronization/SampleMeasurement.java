package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

/**
 * Creates a Sample Measurement object with getters and setters for all the parameters of a {@link DatabaseHelper#sampleMeasurementTable} Table in Database.
 * Used by {@link SampleMeasurementSync} to create a new Sample Measurement Object to be inserted into the Database.
 *
 * @see SyncActivity
 * @see SampleMeasurementSync
 * @see de.awi.floenavigation.synchronization
 */

public class SampleMeasurement {

    private static final String TAG = "SampleMeasurement";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private String deviceID;
    private String deviceName;
    private String deviceShortName;
    private String operation;
    private String deviceType;
    private double latitude;
    private double longitude;
    private double xPosition;
    private double yPosition;
    private String updateTime;
    private String labelID;
    private String comment;
    private String label;
    private Context appContext;
    ContentValues sample;

    /**
     * Default Constructor.
     * @param context Used to create a {@link DatabaseHelper} object.
     */
    public SampleMeasurement(Context context){
        appContext = context;
        try {
            dbHelper = DatabaseHelper.getDbInstance(appContext);
            db = dbHelper.getReadableDatabase();
        } catch(SQLException e){
            Log.d(TAG, "Database Exception");
            e.printStackTrace();
        }
    }

    /**
     * Inserts the values of the fixed station parameters into {@link #sample}
     */

    private void generateContentValues(){
        sample = new ContentValues();
        sample.put(DatabaseHelper.deviceID, this.deviceID);
        sample.put(DatabaseHelper.deviceName, this.deviceName);
        sample.put(DatabaseHelper.deviceShortName, this.deviceShortName);
        sample.put(DatabaseHelper.operation, this.operation);
        sample.put(DatabaseHelper.deviceType, this.deviceType);
        sample.put(DatabaseHelper.latitude, this.latitude);
        sample.put(DatabaseHelper.longitude, this.longitude);
        sample.put(DatabaseHelper.xPosition, this.xPosition);
        sample.put(DatabaseHelper.yPosition, this.yPosition);
        sample.put(DatabaseHelper.updateTime, this.updateTime);
        sample.put(DatabaseHelper.labelID, this.labelID);
        sample.put(DatabaseHelper.comment, this.comment);
        sample.put(DatabaseHelper.label, this.label);
    }

    /**
     * Inserts the Sample Measurement created from pulling Data from the Server into the local Database.
     * Unused for now as Samples are only pushed to the Sync Server and not pulled.
     */

    public void insertSampleInDB(){
        generateContentValues();
        int result = db.update(DatabaseHelper.sampleMeasurementTable, sample, DatabaseHelper.labelID + " = ?", new String[] {this.labelID});
        if(result == 0){
            db.insert(DatabaseHelper.sampleMeasurementTable, null, sample);
            Log.d(TAG, "Station Added");
        } else{
            Log.d(TAG, "Station Updated");
        }
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceShortName() {
        return deviceShortName;
    }

    public void setDeviceShortName(String deviceShortName) {
        this.deviceShortName = deviceShortName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
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

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getLabelID() {
        return labelID;
    }

    public void setLabelID(String labelID) {
        this.labelID = labelID;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
