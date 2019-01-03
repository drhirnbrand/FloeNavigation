package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

public class DeviceList {

    private static final String TAG = "DeviceList";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private String deviceID;
    private String deviceName;
    private String deviceShortName;

    private String deviceType;
    private Context appContext;
    ContentValues deviceListContent;

    public DeviceList(Context context){
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
        deviceListContent = new ContentValues();
        deviceListContent.put(DatabaseHelper.deviceID, this.deviceID);
        deviceListContent.put(DatabaseHelper.deviceName, this.deviceName);
        deviceListContent.put(DatabaseHelper.deviceShortName, this.deviceShortName);
        deviceListContent.put(DatabaseHelper.deviceType, this.deviceType);
    }

    public void insertDeviceListInDB(){
        generateContentValues();
        long result = db.insert(DatabaseHelper.deviceListTable, null, deviceListContent);
        if(result == -1){
            Log.d(TAG, "Insertion failed");
        } else{
            Log.d(TAG, "Device List Updated");
        }
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceShortName(String deviceShortName) {
        this.deviceShortName = deviceShortName;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

}
