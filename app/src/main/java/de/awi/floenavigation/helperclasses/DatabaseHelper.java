package de.awi.floenavigation.helperclasses;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "FloeNavigation";
    private static final int DB_VERSION = 2;
    private static final String TAG = "DatabaseHelper";

    private static DatabaseHelper dbInstance;

    public static final int firstStationIndex = 0;
    public static final int secondStationIndex = 1;
    public static final int LATITUDE_INDEX = 0;
    public static final int LONGITUDE_INDEX = 1;
    public static final int INITIALIZATION_SIZE = 2;
    public static final int NUM_OF_BASE_STATIONS = 2;
    public static final int NUM_OF_DEVICES = 1234;
    public static final int IS_LOCATION_RECEIVED_INITIAL_VALUE = 0;
    public static final int IS_LOCATION_RECEIVED = 1;
    public static final double ORIGIN_DISTANCE = 0.0;
    public static final double ORIGIN = 1;
    public static final String error_threshold = "ERROR_THRESHOLD";
    public static final String prediction_accuracy_threshold = "PREDICTION_ACCURACY_THRESHOLD";
    public static final String lat_long_view_format = "LATITUDE_LONGITUDE_VIEW_FORMAT";
    public static final String decimal_number_significant_figures = "DECIMAL_NUMBER_SIGNIFICANT_FIGURES";
    public static final String initial_setup_time = "INITIAL_SETUP_TIME";
    public static final String tabletId = "TABLET_ID";
    public static final String sync_server_hostname = "SYNC_SERVER_HOSTNAME";
    public static final String sync_server_port = "SYNC_SERVER_PORT";


    //Database Tables Names
    public static final String fixedStationTable = "AIS_FIXED_STATION_POSITION";
    public static final String stationListTable = "AIS_STATION_LIST";
    public static final String mobileStationTable = "AIS_MOBILE_STATION_POSITION";
    public static final String usersTable = "USERS";
    public static final String sampleMeasurementTable = "SAMPLE_MEASUREMENT";
    public static final String deviceListTable = "DEVICE_LIST";
    public static final String waypointsTable = "WAYPOINTS";
    public static final String configParametersTable = "CONFIGURATION_PARAMETERS";
    public static final String baseStationTable = "BASE_STATIONS";
    public static final String betaTable = "BETA_TABLE";
    public static final String staticStationListTable = "STATION_LIST";
    public static final String stationListDeletedTable = "STATION_LIST_DELETED";
    public static final String fixedStationDeletedTable = "FIXED_STATION_DELETED";
    public static final String staticStationDeletedTable = "STATIC_STATION_DELETED";
    public static final String baseStationDeletedTable = "BASE_STATION_DELETED";
    public static final String waypointDeletedTable = "WAYPOINT_DELETED";
    public static final String userDeletedTable = "USERS_DELETED";



    //Database Fields Names
    public static final String stationName = "AIS_STATION_NAME";
    public static final String latitude = "LATITUDE";
    public static final String longitude = "LONGITUDE";
    public static final String recvdLatitude = "RECEIVED_LATITUDE";
    public static final String recvdLongitude = "RECEIVED_LONGITUDE";
    public static final String xPosition = "X_POSITION";
    public static final String yPosition = "Y_POSITION";
    public static final String sog = "SPEED_OVER_GROUND";
    public static final String cog = "COURSE_OVER_GROUND";
    public static final String alpha = "ALPHA";
    public static final String beta = "BETA";
    public static final String packetType = "LAST_RECEIVED_PACKET_TYPE";
    public static final String predictionAccuracy = "PREDICTION_ACCURACY";
    public static final String mmsi = "MMSI";
    public static final String staticStationName = "STATION_NAME";
    public static final String stationType = "STATION_TYPE";
    public static final String distance = "DISTANCE";
    public static final String deviceType = "DEVICE_TYPE";
    public static final String updateTime = "UPDATE_TIME";
    public static final String isCalculated = "IS_COORDINATE_CALCULATED";
    public static final String isPredicted = "IS_POSITION_PREDICTED";
    public static final String incorrectMessageCount = "INCORRECT_MESSAGE_COUNT";
    public static final String validationCheckTime = "VALIDATION_CHECK_TIME";
    public static final String isLocationReceived = "IS_LOCATION_RECEIVED";
    public static final String userName = "USERNAME";
    public static final String password = "PASSWORD";
    public static final String deviceID = "DEVICE_ID";
    public static final String deviceName = "DEVICE_NAME";
    public static final String deviceShortName = "DEVICE_SHORT_NAME";
    public static final String operation = "OPERATION";
    public static final String labelID = "LABEL_ID";
    public static final String label = "LABEL";
    public static final String parameterName = "PARAMETER_NAME";
    public static final String parameterValue = "PARAMETER_VALUE";
    public static final String isOrigin = "IS_ORIGIN";
    public static final String origin = "ORIGIN";
    public static final String basestn1 = "BASE_STN";
    public static final String deleteTime = "DELETE_TIME";
    public static final String comment = "COMMENT";

    //Initial Position of Setup Points in Custom Coordinate System
    public static final long station1InitialX = 0;
    public static final long station1InitialY = 0;
    public static final long station2InitialX = 500;
    public static final long station2InitialY = 0;
    public static final double station1Alpha = 0.0;
    public static final double station2Alpha = 0.0;
    public static final int MOBILE_STATION_IS_CALCULATED = 1;
    public static final int BASESTN1 = 1000;
    public static final int BASESTN2 = 1001;

    public static List<String> deviceNames;
    public static List<String> deviceShortNames;
    public static List<String> deviceIDs;
    public static List<String> deviceTypes;

    public static final String[] stationTypes = {
            "Tent",
            "Hut",
            "Mast",
            "Fixpoint",
    };

    public static final String[] configurationParameters = {
            "ERROR_THRESHOLD",
            "PREDICTION_ACCURACY_THRESHOLD",
            "LATITUDE_LONGITUDE_VIEW_FORMAT",
            "DECIMAL_NUMBER_SIGNIFICANT_FIGURES",
            "INITIAL_SETUP_TIME",
            "SYNC_SERVER_HOSTNAME",
            "SYNC_SERVER_PORT",
            "TABLET_ID"
    };

    //public static final int MOTHER_SHIP_MMSI = 211202460;
    //For Testing purposes
    public static final int MOTHER_SHIP_MMSI = 211590050;


    public DatabaseHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){

        createTables(db, 0, DB_VERSION);
        //Default config params
        insertDefaultConfigParams(db, error_threshold, "10");
        insertDefaultConfigParams(db, prediction_accuracy_threshold, String.valueOf(3 * 60 * 1000));
        insertDefaultConfigParams(db, lat_long_view_format, "1");
        insertDefaultConfigParams(db, decimal_number_significant_figures, "5");
        insertDefaultConfigParams(db, initial_setup_time, String.valueOf(60 * 1000));
        insertDefaultConfigParams(db, sync_server_hostname, "192.168.137.1");
        insertDefaultConfigParams(db, sync_server_port, String.valueOf(80));

        //Create a Default User
        insertUser(db, "awi", "awi");


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        createTables(db, oldVersion, newVersion);
    }



    private static void createTables(SQLiteDatabase db, int oldVersion, int newVersion){
        if(oldVersion < 1) {
            //SQLiteDatabase db = this.getWritableDatabase();
            try {
                //Create AIS Station List Table
                db.execSQL("CREATE TABLE " + stationListTable + "(" + mmsi + " INTEGER PRIMARY KEY," +
                        stationName + " TEXT NOT NULL );");

                //Create AIS Fixed Station Position Table
                db.execSQL("CREATE TABLE " + fixedStationTable + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        stationName + " TEXT, " +
                        latitude + " REAL, " +
                        longitude + " REAL, " +
                        recvdLatitude + " REAL, " +
                        recvdLongitude + " REAL, " +
                        alpha + " REAL, " +
                        distance + " REAL, " +
                        xPosition + " REAL, " +
                        yPosition + " REAL, " +
                        stationType + " TEXT, " +
                        updateTime + " TEXT, " +
                        sog + " REAL, " +
                        cog + " REAL, " +
                        packetType + " INTEGER, " +
                        isPredicted + " NUMERIC, " +
                        incorrectMessageCount + " INTEGER, " +
                        validationCheckTime + " TEXT, " +
                        predictionAccuracy + " NUMERIC, " +
                        isLocationReceived + " NUMERIC, " +
                        mmsi + " INTEGER UNIQUE NOT NULL);");

                //Create Base Stations Table
                db.execSQL("CREATE TABLE " + baseStationTable + "(" + mmsi + " INTEGER PRIMARY KEY, " +
                        isOrigin + " NUMERIC, " +
                        stationName + " TEXT NOT NULL);");

                //Create Beta Table
                db.execSQL("CREATE TABLE " + betaTable + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        beta + " REAL NOT NULL, " +
                        updateTime + " TEXT);");

                //Create Users Table
                db.execSQL("CREATE TABLE " + usersTable + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        userName + " TEXT UNIQUE NOT NULL, " +
                        password + " TEXT);");

                //Create Configuration Parameters Table
                db.execSQL("CREATE TABLE " + configParametersTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        parameterName + " TEXT, " +
                        parameterValue + " TEXT); ");

                db.execSQL("CREATE TABLE " + stationListDeletedTable + " (" + mmsi + " INTEGER PRIMARY KEY, " +
                        deleteTime + " TEXT); ");

                db.execSQL("CREATE TABLE " + fixedStationDeletedTable + " (" + mmsi + " INTEGER PRIMARY KEY, " +
                        deleteTime + " TEXT); ");

                db.execSQL("CREATE TABLE " + baseStationDeletedTable + " (" + mmsi + " INTEGER PRIMARY KEY, " +
                        deleteTime + " TEXT); ");

                db.execSQL("CREATE TABLE " + userDeletedTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        userName + " TEXT UNIQUE NOT NULL, " +
                        deleteTime + " TEXT); ");

                //Create Mobile Station Table
                db.execSQL("CREATE TABLE " + mobileStationTable + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        stationName + " TEXT, " +
                        latitude + " REAL, " +
                        longitude + " REAL, " +
                        sog + " REAL, " +
                        cog + " REAL, " +
                        alpha + " REAL, " +
                        distance + " REAL, " +
                        xPosition + " REAL, " +
                        yPosition + " REAL, " +
                        updateTime + " TEXT, " +
                        isCalculated + " NUMERIC, " +
                        packetType + " INTEGER, " +
                        mmsi + " INTEGER NOT NULL);");

                //Create Sample/Measurement Table
                db.execSQL("CREATE TABLE " + sampleMeasurementTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        deviceID + " TEXT," +
                        deviceName + " TEXT, " +
                        deviceShortName + " TEXT, " +
                        operation + " TEXT, " +
                        deviceType + " TEXT, " +
                        comment + " TEXT, " +
                        latitude + " REAL, " +
                        longitude + " REAL, " +
                        xPosition + " REAL, " +
                        yPosition + " REAL, " +
                        updateTime + " TEXT, " +
                        labelID + " TEXT, " +
                        label + " TEXT);");

                //Create DeviceList Table
                db.execSQL("CREATE TABLE " + deviceListTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        deviceID + " TEXT," +
                        deviceName + " TEXT, " +
                        deviceShortName + " TEXT, " +
                        deviceType + " TEXT);");

                //Create StationList Table
                db.execSQL("CREATE TABLE " + staticStationListTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        staticStationName + " TEXT UNIQUE NOT NULL, " +
                        stationType + " TEXT, " +
                        xPosition + " REAL, " +
                        yPosition + " REAL, " +
                        alpha + " REAL, " +
                        distance + " REAL);");

                //Create Waypoints Table
                db.execSQL("CREATE TABLE " + waypointsTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        latitude + " REAL, " +
                        longitude + " REAL, " +
                        xPosition + " REAL, " +
                        yPosition + " REAL, " +
                        updateTime + " TEXT, " +
                        labelID + " TEXT UNIQUE NOT NULL, " +
                        label + " TEXT); ");


                db.execSQL("CREATE TABLE " + staticStationDeletedTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        staticStationName + " TEXT UNIQUE NOT NULL, " +
                        deleteTime + " TEXT); ");

                db.execSQL("CREATE TABLE " + waypointDeletedTable + " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        labelID + " TEXT UNIQUE NOT NULL, " +
                        deleteTime + " TEXT); ");


                //Only for debugging purpose
                //insertDeviceList(db);

                //return true;
            } catch (SQLiteException e) {
                Log.d(TAG, "Database Unavailable");
                //return false;
            }
        }
        if(oldVersion < 2){
            try{
                db.execSQL("Delete from " + fixedStationTable);
                db.execSQL("Delete from " + fixedStationDeletedTable);
                db.execSQL("Delete from " + stationListTable);
                db.execSQL("Delete from " + stationListDeletedTable);
                db.execSQL("Delete from " + baseStationTable);
                db.execSQL("Delete from " + baseStationDeletedTable);
                db.execSQL("Delete from " + betaTable);
                db.execSQL("Delete from " + sampleMeasurementTable);
                db.execSQL("Delete from " + waypointsTable);
                db.execSQL("Delete from " + waypointDeletedTable);
                db.execSQL("Delete from " + staticStationListTable);
                db.execSQL("Delete from " + staticStationDeletedTable);

                db.execSQL("ALTER TABLE " + sampleMeasurementTable + " ADD COLUMN " + comment + " TEXT;");
            } catch (SQLException e){
                Log.d(TAG, "Database Unavailable");
                //return false;
            }
        }

    }

    public static void insertUser(SQLiteDatabase db, String name, String pass){
        ContentValues defaultUser = new ContentValues();
        defaultUser.put(userName, name);
        defaultUser.put(password, pass);
        db.insert(usersTable, null, defaultUser);
    }

    private static void insertDefaultConfigParams(SQLiteDatabase db, String name, String value){
        ContentValues defaultConfigParam = new ContentValues();
        defaultConfigParam.put(parameterName, name);
        defaultConfigParam.put(parameterValue, value);
        db.insert(configParametersTable, null, defaultConfigParam);
    }

    /******************Only for debugging purpose**************************/
    private static void insertDeviceList(SQLiteDatabase db){

        String[] deviceShortNames = {"3DCAM", "8-CTL", "AC-9", "AGSS"};
        String[] deviceLongNames = {"3D camera", "8-Channel Temperature Lance",
                "Absorption and beam attenuation", "Accoustic Geodetic Seafloor Station",};

        for(int index = 0; index < stationTypes.length; index++) {
            ContentValues mContentValues = new ContentValues();
            mContentValues.put(deviceID, index);
            mContentValues.put(deviceName, deviceLongNames[index]);
            mContentValues.put(deviceShortName, deviceShortNames[index]);
            mContentValues.put(deviceType, stationTypes[index]);
            db.insert(deviceListTable, null, mContentValues);
        }
    }
    /********************************************/

    public String searchPassword(String uname, Context context){
        SQLiteOpenHelper dbHelper = getDbInstance(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        //String query = "SELECT USERNAME, PASSWORD FROM USERS";
        Cursor cursor = db.query(usersTable,
                new String[]{userName, password},
                null, null, null, null, null);
        String user, pwd;
        pwd = "Not Found";

        if (cursor.moveToFirst()){


            do {

                user = cursor.getString(0);
                if (user.equals(uname)){
                    pwd = cursor.getString(1);

                    break;
                }
            } while(cursor.moveToNext());
        }
        cursor.close();
        //db.close();

        return pwd;
    }

    public  double[] readBaseCoordinatePointsLatLon(Context context){
        double[] coordinates = new double[4];
        try {
            SQLiteOpenHelper dbHelper = getDbInstance(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.query(fixedStationTable,
                    new String[]{latitude, longitude},
                    "(X_Position = ? AND Y_POSITION = ?) OR (X_POSITION = ? AND Y_POSITION = ?)",
                    new String[]{Long.toString(station1InitialX), Long.toString(station1InitialY), Long.toString(station2InitialX), Long.toString(station2InitialY)},
                    null, null, null);
            if (cursor.moveToFirst()) {
                int i = 0;
                do {

                    coordinates[i] = cursor.getDouble(cursor.getColumnIndex(latitude));
                    coordinates[i + 1] = cursor.getDouble(cursor.getColumnIndex(longitude));
                    i += 2;
                } while (cursor.moveToNext());
            }
            cursor.close();
            //db.close();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Unavailable");
            e.printStackTrace();
        }
        return coordinates;
    }

    public static synchronized DatabaseHelper getDbInstance(Context context){
        if (dbInstance == null){
            dbInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return dbInstance;
    }


    public static void loadDeviceList(Context mContext){


        deviceTypes = new ArrayList<String>();
        deviceIDs = new ArrayList<String>();
        deviceNames = new ArrayList<String>();
        deviceShortNames = new ArrayList<String>();

        try {
            SQLiteOpenHelper dbHelper = getDbInstance(mContext);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor mDeviceListCursor = db.query(deviceListTable, null,
                    null, null, null, null, null);
            if (mDeviceListCursor.moveToFirst()) {
                do {
                    deviceIDs.add(mDeviceListCursor.getString(mDeviceListCursor.getColumnIndex(deviceID)));
                    deviceNames.add(mDeviceListCursor.getString(mDeviceListCursor.getColumnIndex(deviceName)));
                    deviceShortNames.add(mDeviceListCursor.getString(mDeviceListCursor.getColumnIndex(deviceShortName)));
                    deviceTypes.add(mDeviceListCursor.getString(mDeviceListCursor.getColumnIndex(deviceType)));
                } while (mDeviceListCursor.moveToNext());
            }
            mDeviceListCursor.close();
        }catch (SQLException e){
            Log.d(TAG, "Database Unavailable");
            e.printStackTrace();
        }
    }

    public static ArrayAdapter<String> advancedSearchTextView(Context mContext){

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_dropdown_item_1line, deviceShortNames);
        return adapter;
    }

    public static ArrayList<String> getDeviceAttributes(String devShortName){

        int arrayIndex;
        ArrayList<String> selectedDeviceAttributes = new ArrayList<String>();
        for (arrayIndex = 0; arrayIndex < deviceShortNames.size(); arrayIndex++){
            if (deviceShortNames.get(arrayIndex).equals(devShortName)){
                selectedDeviceAttributes.add(deviceIDs.get(arrayIndex));
                selectedDeviceAttributes.add(deviceNames.get(arrayIndex));
                selectedDeviceAttributes.add(deviceTypes.get(arrayIndex));
                return selectedDeviceAttributes;
            }
        }
        Log.d(TAG, "Device attributes not found");
        return null;
    }

    public static boolean readCoordinateDisplaySetting(Context context){
        boolean changeFormat = false;
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor formatCursor = db.query(DatabaseHelper.configParametersTable,
                    new String[] {DatabaseHelper.parameterName, DatabaseHelper.parameterValue},
                    DatabaseHelper.parameterName + " = ?",
                    new String[] {DatabaseHelper.lat_long_view_format},
                    null, null, null);
            if (formatCursor.getCount() == 1){
                if(formatCursor.moveToFirst()){
                    String formatValue = formatCursor.getString(formatCursor.getColumnIndexOrThrow(DatabaseHelper.parameterValue));
                    if(formatValue.equals("0")){
                        changeFormat = true;
                    } else if(formatValue.equals("1")){
                        changeFormat = false;
                    }
                }
                formatCursor.close();
                return changeFormat;
            } else{
                Log.d(TAG, "Error with Display Format");
                return changeFormat;
            }
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
            e.printStackTrace();
            return changeFormat;
        }
    }

    public static int readSiginificantDigitsSetting(Context context){
        int significantFigure = 5;
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor mSignificantFiguresCursor = db.query(DatabaseHelper.configParametersTable,
                    new String[] {DatabaseHelper.parameterName, DatabaseHelper.parameterValue},
                    DatabaseHelper.parameterName + " = ?",
                    new String[] {DatabaseHelper.decimal_number_significant_figures},
                    null, null, null);
            if (mSignificantFiguresCursor.getCount() == 1){
                if(mSignificantFiguresCursor.moveToFirst()){
                    significantFigure = Integer.parseInt(mSignificantFiguresCursor.getString(mSignificantFiguresCursor.getColumnIndexOrThrow(DatabaseHelper.parameterValue)));
                }
                mSignificantFiguresCursor.close();

            } else{
                Log.d(TAG, "Error Reading the Significant Figure Parameter");

            }
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
            e.printStackTrace();
        }
        return significantFigure;
    }

    public static boolean updateCoordinateDisplaySetting(Context context, boolean changeDegFormat){
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String inputValue;
            if(changeDegFormat){
                inputValue = "0";
            } else{
                inputValue = "1";
            }
            ContentValues configParamsContents = new ContentValues();
            //configParamsContents.put(DatabaseHelper.parameterName, lat_long_view_format);
            configParamsContents.put(DatabaseHelper.parameterValue, inputValue);
            db.update(DatabaseHelper.configParametersTable, configParamsContents, DatabaseHelper.parameterName + " = ?",
                    new String[] {lat_long_view_format});
            return true;
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
            e.printStackTrace();
            return false;
        }
    }
}
