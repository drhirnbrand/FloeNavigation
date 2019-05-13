package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

/**
 * Creates an AIS Station object with getters and setters for  all the parameters of a {@link DatabaseHelper#stationListTable} Table in Database.
 * Used by {@link StationListSync} to create a new AIS Station Object to be inserted in to the Database.
 *
 * @see SyncActivity
 * @see StationListSync
 * @see de.awi.floenavigation.synchronization
 */

public class StationList {

    private static final String TAG = "STATION_LIST";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private Context appContext;

    /**
     * Name of the AIS Station
     */
    private String stationName;

    /**
     * MMSI of the AIS Station
     */
    private int mmsi;

    /**
     * Local variable. {@link ContentValues} object which will be inserted in to the {@link DatabaseHelper#stationListTable} Table.
     */
    private ContentValues stnListContent;

    /**
     * Default Constructor.
     * @param context Used to create a {@link DatabaseHelper} object.
     */
    public StationList(Context context){
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
     * Inserts the values of the Static Station in to {@link #stnListContent}
     */

    private void generateContentValues(){
        stnListContent = new ContentValues();
        stnListContent.put(DatabaseHelper.stationName, this.stationName);
        stnListContent.put(DatabaseHelper.mmsi, this.mmsi);
    }

    /**
     * Inserts the Static Station created from pulling Data from the Server in to the local Database.
     */

    public void insertStationInDB(){
        generateContentValues();
        int result = db.update(DatabaseHelper.stationListTable, stnListContent, DatabaseHelper.mmsi + " = ?", new String[] {String.valueOf(mmsi)});
        if(result == 0){
            db.insert(DatabaseHelper.stationListTable, null, stnListContent);
            Log.d(TAG, "Station Added");
        } else{
            Log.d(TAG, "Station Updated");
        }
    }

    /**
     * Get the Name of the AIS Station
     * @return Station Name
     */

    public String getStationName() {
        return stationName;
    }

    /**
     * Set the AIS Station Name
     * @param stationName AIS Station Name
     */

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    /**
     * Get the MMSI of the AIS Station
     * @return MMSI of the AIS Station
     */
    public int getMmsi() {
        return mmsi;
    }

    /**
     * Set the AIS Station MMSI
     * @param mmsi AIS Station MMSI
     */
    public void setMmsi(int mmsi) {
        this.mmsi = mmsi;
    }
}
