package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

public class StationList {

    private static final String TAG = "STATION_LIST";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private Context appContext;

    private String stationName;
    private int mmsi;

    private ContentValues stnListContent;

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

    private void generateContentValues(){
        stnListContent = new ContentValues();
        stnListContent.put(DatabaseHelper.stationName, this.stationName);
        stnListContent.put(DatabaseHelper.mmsi, this.mmsi);
    }

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

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public int getMmsi() {
        return mmsi;
    }

    public void setMmsi(int mmsi) {
        this.mmsi = mmsi;
    }
}
