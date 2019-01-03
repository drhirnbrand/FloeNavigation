package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

/**
 * Creates a Static Station object with getters and setters for all the properties of a Static Station.
 * Used by {@link StaticStationSync} to create a new Static Station Object to be inserted in to the Database.
 *
 * @see SyncActivity
 * @see StaticStationSync
 * @see de.awi.floenavigation.synchronization
 */
public class StaticStation {

    private static final String TAG = "STATIC_STATION";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private Context appContext;

    /**
     * Name of the Static Station
     */
    private String stationName;

    /**
     * Type of the Static Station.
     */
    private String stationType;

    /**
     * X Coordinate of the Station in the Floe Coordinate System
     */
    private double xPosition;

    /**
     * Y Coordinate of the Station in the Floe Coordinate System
     */
    private double yPosition;

    /**
     * Angle of the Station from the x-Axis of the Floe Coordinate System
     */
    private double alpha;

    /**
     * Direct distance between the Station and the Origin
     */
    private double distance;


    /**
     * Local variable. {@link ContentValues} object which will be inserted in to the Static Station Table.
     */
    private ContentValues staticStnContent;

    /**
     * Default Constructor.
     * @param context Used to create a {@link DatabaseHelper} object.
     */
    public StaticStation(Context context){
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
     * Inserts the values of the Static Station in to {@link #staticStnContent}
     */

    private void generateContentValues() {
        staticStnContent = new ContentValues();
        staticStnContent.put(DatabaseHelper.staticStationName, this.stationName);
        staticStnContent.put(DatabaseHelper.alpha, this.alpha);
        staticStnContent.put(DatabaseHelper.distance, this.distance);
        staticStnContent.put(DatabaseHelper.xPosition, this.xPosition);
        staticStnContent.put(DatabaseHelper.yPosition, this.yPosition);
        staticStnContent.put(DatabaseHelper.stationType, this.stationType);
    }

    /**
     * Inserts the Static Station created from pulling Data from the Server in to the local Database.
     */

    public void insertStaticStationInDB(){
        generateContentValues();
        int result = db.update(DatabaseHelper.staticStationListTable, staticStnContent, DatabaseHelper.staticStationName + " = ?", new String[] {this.stationName});
        if(result == 0){
            db.insert(DatabaseHelper.staticStationListTable, null, staticStnContent);
            Log.d(TAG, "Static Station Added");
        } else{
            Log.d(TAG, "Static Station Updated");
        }
    }

    /**
     * Get the Name of the Static Station
     * @return Station Name
     */
    public String getStationName() {
        return stationName;
    }

    /**
     * Set the Static Station Name
     * @param stationName Static Station Name
     */

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    /**
     * Get Alpha of the Static Station
     * @return {@link #alpha}
     */

    public double getAlpha() {
        return alpha;
    }

    /**
     * Set Alpha
     * @param alpha {@link #alpha}
     */
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    /**
     * Get distance of the Static Station from Origin
     * @return {@link #distance}
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Set distance of the Static Station
     * @param distance {@link #distance}
     */

    public void setDistance(double distance) {
        this.distance = distance;
    }

    /**
     * Get X coordinates of the Static Station
     * @return {@link #xPosition}
     */
    public double getxPosition() {
        return xPosition;
    }

    /**
     * Set X Coordinates of the Static Station
     * @param xPosition {@link #xPosition}
     */
    public void setxPosition(double xPosition) {
        this.xPosition = xPosition;
    }

    /**
     * Set X Coordinates of the Static Station
     * @return  yPosition {@link #yPosition}
     */
    public double getyPosition() {
        return yPosition;
    }

    /**
     * Set X Coordinates of the Static Station
     * @param yPosition {@link #yPosition}
     */
    public void setyPosition(double yPosition) {
        this.yPosition = yPosition;
    }


    /**
     * Set X Coordinates of the Static Station
     * @return  stationType {@link #stationType}
     */
    public String getStationType() {
        return stationType;
    }

    /**
     * Set X Coordinates of the Static Station
     * @param stationType {@link #stationType}
     */
    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

}
