package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

/**
 * Creates a Waypoinot object with getters and setters for  all the parameters of a {@link DatabaseHelper#waypointsTable} Table in Database.
 * Used by {@link WaypointsSync} to create a new Waypoint Object to be inserted in to the Database.
 *
 * @see SyncActivity
 * @see WaypointsSync
 * @see de.awi.floenavigation.synchronization
 */
public class Waypoints {

    private static final String TAG = "WAYPOINTS";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private Context appContext;

    /**
     * Latitude of the tablet at the time when the Waypoint was created
     */
    private double latitude;

    /**
     * Longitude of the tablet at the time when the Waypoint was created
     */
    private double longitude;

    /**
     * X Coordinate of the Waypoint in the Floe Coordinate System
     */
    private double xPosition;

    /**
     * Y Coordinate of the Station in the Floe Coordinate System
     */
    private double yPosition;

    /**
     * Time in UTC when the Waypoint was created
     */
    private String updateTime;

    /**
     * Label of the Waypoint
     */
    private String labelID;

    /**
     * Unique string of the Waypoint created from appending all the fields of the Waypoint in CSV format
     */
    private String label;

    /**
     * Local variable. {@link ContentValues} object which will be inserted in to the Waypoint Table.
     */
    private ContentValues waypointsContent;

    /**
     * Default Constructor.
     * @param context Used to create a {@link DatabaseHelper} object.
     */
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

    /**
     * Inserts the values of the Static Station in to {@link #waypointsContent}
     */
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

    /**
     * Inserts the Waypoinnt created from pulling Data from the Server in to the local Database.
     */
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
