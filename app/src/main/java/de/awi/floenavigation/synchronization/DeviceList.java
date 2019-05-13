package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

/**
 * Creates a Device list object with getters and setters for all the parameters of a {@link DatabaseHelper#deviceListTable} Table in Database.
 * Used by {@link SampleMeasurementSync} to create a new device list Object to be inserted into the Database.
 *
 * @see SyncActivity
 * @see SampleMeasurementSync#onClickDeviceListPullButton()
 * @see de.awi.floenavigation.synchronization
 */
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

    /**
     * Default Constructor.
     * @param context Used to create a {@link DatabaseHelper} object.
     */
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

    /**
     * Inserts the values of the fixed station parameters into {@link #deviceListContent}
     */
    private void generateContentValues(){
        deviceListContent = new ContentValues();
        deviceListContent.put(DatabaseHelper.deviceID, this.deviceID);
        deviceListContent.put(DatabaseHelper.deviceName, this.deviceName);
        deviceListContent.put(DatabaseHelper.deviceShortName, this.deviceShortName);
        deviceListContent.put(DatabaseHelper.deviceType, this.deviceType);
    }

    /**
     * Inserts the device list content values created from pulling data from the Server into the local Database.
     */
    public void insertDeviceListInDB(){
        generateContentValues();
        long result = db.insert(DatabaseHelper.deviceListTable, null, deviceListContent);
        if(result == -1){
            Log.d(TAG, "Insertion failed");
        } else{
            Log.d(TAG, "Device List Updated");
        }
    }

    /**
     * Set the device ID value
     * @param deviceID
     */
    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    /**
     * Set the device name
     * @param deviceName
     */
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     * Set the device short name
     * @param deviceShortName
     */
    public void setDeviceShortName(String deviceShortName) {
        this.deviceShortName = deviceShortName;
    }

    /**
     * Set the type of the device value
     * @param deviceType
     */
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

}
