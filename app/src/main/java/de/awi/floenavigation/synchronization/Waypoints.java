package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

public class Waypoints {

    private static final String TAG = "WAYPOINTS";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private Context appContext;

    private double latitude;
    private double longitude;
    private double xPosition;
    private double yPosition;
    private String updateTime;
    private String labelID;
    private String label;


    private ContentValues waypointsContent;

    public Waypoints(Context context){
        appContext = context;
        try {
            dbHelper = DatabaseHelper.getDbInstance(appContext);
            db = dbHelper.getReadableDatabase();
        } catch(SQLException e){
            Log.d(TAG, "Database Exception");
            e.printStackTrace();
        }
    }

    private void generateContentValues() {
        waypointsContent = new ContentValues();
        waypointsContent.put(DatabaseHelper.latitude, this.latitude);
        waypointsContent.put(DatabaseHelper.longitude, this.longitude);
        waypointsContent.put(DatabaseHelper.xPosition, this.xPosition);
        waypointsContent.put(DatabaseHelper.yPosition, this.yPosition);
        waypointsContent.put(DatabaseHelper.updateTime, this.updateTime);
        waypointsContent.put(DatabaseHelper.labelID, this.labelID);
        waypointsContent.put(DatabaseHelper.label, this.label);
    }

    public void insertWaypointsInDB(){
        generateContentValues();
        int result = db.update(DatabaseHelper.waypointsTable, waypointsContent, DatabaseHelper.labelID + " = ?", new String[] {this.labelID});
        if(result == 0){
            db.insert(DatabaseHelper.waypointsTable, null, waypointsContent);
            Log.d(TAG, "Waypoint Added");
        } else{
            Log.d(TAG, "Waypoint Updated");
        }
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
