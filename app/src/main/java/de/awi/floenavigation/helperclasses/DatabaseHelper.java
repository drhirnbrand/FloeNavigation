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

    /**
     * Name of the local database instance
     */
    private static final String DB_NAME = "FloeNavigation";

    /**
     * The current version of this Database. This needs to be updated with every new version of the App which changes the Database
     * Schema. This is used by the {@link #onUpgrade(SQLiteDatabase, int, int)} function to check if the Database needs to updated
     * when the App is installed.
     */
    private static final int DB_VERSION = 2;
    private static final String TAG = "DatabaseHelper";

    /**
     * A static instance of {@link DatabaseHelper} which is used to provide access to the Database by other activities and classes.
     */
    private static DatabaseHelper dbInstance;

    /**
     * Index value for the Origin of Floe's Coordinate System. This index value is used in different classes and activities whenever an
     * array of Station parameters (such as MMSIs or Latitude/Longitudes) is created this will be the index in the array at which the
     * values for the Origin station will be found.
     */
    public static final int firstStationIndex = 0;

    /**
     * Index value for the x-Axis Marker of Floe's Coordinate System. This index value is used in different classes and activities whenever an
     * an array of Station parameters (such as MMSIs or Latitude/Longitudes) is created this will be the index in the array at which the
     * values for the x-Axis Marker station will be found.
     */
    public static final int secondStationIndex = 1;

    /**
     * Index value for Latitude in an array of {Latitude, Longitude}. Currently all coordinates in the App are created in an array such
     * that the Latitude is at this index value.
     */
    public static final int LATITUDE_INDEX = 0;

    /**
     * Index value for Longitude in an array of {Latitude, Longitude}. Currently all coordinates in the App are created in an array such
     * that the Longitude is at this index value.
     */
    public static final int LONGITUDE_INDEX = 1;

    /**
     * Specifies the number of base stations which will be used to create the Floe's coordinate system. This is used extensively by
     * {@link de.awi.floenavigation.initialsetup.SetupActivity} and other activities as well to initialize its arrays to the correct
     * size.
     */
    public static final int INITIALIZATION_SIZE = 2;

    /**
     * Similar to {@link #INITIALIZATION_SIZE}. Specifies the minimum number of base/fixed station which can be used to maintain the
     * Grid. This is used in {@link de.awi.floenavigation.admin.RecoveryActivity} to check if sufficient number of Fixed/Base Stations
     * are left. This is also used for updating the ActionBar icon for Grid Setup.
     */
    public static final int NUM_OF_BASE_STATIONS = 2;
    public static final int NUM_OF_DEVICES = 1234;

    /**
     * Initial Value which is inserted by default in the column {@link #isLocationReceived} in the Database table {@link #fixedStationTable}.
     */
    public static final int IS_LOCATION_RECEIVED_INITIAL_VALUE = 0;

    /**
     * Specifies the value which is inserted in the column {@link #isLocationReceived} in the Database table {@link #fixedStationTable} by the
     * {@link de.awi.floenavigation.aismessages.AISDecodingService} when a position report is received.
     */
    public static final int IS_LOCATION_RECEIVED = 1;

    /**
     * Constant defining the value to insert in the column {@link #distance} in the Database table {@link #fixedStationTable} when Grid Initial
     * Configuration is done. As it is the origin so its distance from itself should be 0.
     */
    public static final double ORIGIN_DISTANCE = 0.0;

    /**
     * Specifies the value which is inserted in the Database table {@link #baseStationTable} if the station is the Origin station.
     */
    public static final double ORIGIN = 1;

    /**
     * Name of the Configuration Parameter which defines the maximum correct distance in meters between the predicted coordinates and received coordinates
     * for a Fixed Station. If the distance between the predicted coordinates and received coordinates is above the value specified by
     * this Configuration Parameter it will be considered as an incorrect Prediction.
     */
    public static final String error_threshold = "ERROR_THRESHOLD";

    /**
     * Name of the Configuration Parameter which defines the minimum time during which if three incorrect prediction occurs for a Fixed
     * Station it will be considered to be broken from the Floe. Used extensively by
     * {@link de.awi.floenavigation.services.ValidationService}.
     */
    public static final String prediction_accuracy_threshold = "PREDICTION_ACCURACY_THRESHOLD";

    /**
     * Name of the Configuration Parameter which sets the display format for Latitude and Longitude on the App interface. It can either
     * be only degree with decimal values (Degree.xxxx) or Degrees Minutes Seconds with Direction (Degree° Minutes' Seconds'' Direction).
     */
    public static final String lat_long_view_format = "LATITUDE_LONGITUDE_VIEW_FORMAT";

    /**
     * Name of the Configuration Parameter which set the number of significant figures to show for any decimal field to show on the
     * interface of the App. Please note this only defines it for displaying purposes, the calculations in the App use the complete
     * {@link Double} data type length.
     */
    public static final String decimal_number_significant_figures = "DECIMAL_NUMBER_SIGNIFICANT_FIGURES";

    /**
     * Name of the Configuration Parameter which sets the time for which the Prediction timer in
     * {@link de.awi.floenavigation.initialsetup.SetupActivity} will run for.
     */
    public static final String initial_setup_time = "INITIAL_SETUP_TIME";

    /**
     * Name of the Configuration Parameter which gives the Unique ID of the tablet. The value of this Configuration Parameter
     * is appended to all {@link de.awi.floenavigation.synchronization.Waypoints} labels created on this tablet.
     */
    public static final String tabletId = "TABLET_ID";

    /**
     * Name of the Configuration Parameter whose value gives the IP/Hostname of the Synchronization Server with which the App will try
     * to synchronize its data with in {@link de.awi.floenavigation.synchronization.SyncActivity}.
     */
    public static final String sync_server_hostname = "SYNC_SERVER_HOSTNAME";

    /**
     * Name of the Configuration Parameter whose value gives the Port on the Synchronization Server with which the App will try
     * to synchronize its data with in {@link de.awi.floenavigation.synchronization.SyncActivity}.
     */
    public static final String sync_server_port = "SYNC_SERVER_PORT";


    //Database Tables Names
    /**
     * Table which stores the details of every Fixed Station deployed on the Floe. It stores the location parameters of each Fixed Station
     * and is used to create the Coordinate System of the Floe. It is one of most tables in the Database and should be maintained properly.
     * For each Fixed Station the details being stored are the distance from origin, the angle Alpha from x-Axis of the Floe's coordinate
     * system, the x and y coordinates in the Floe's Coordinate system and the received and predicted coordinates in Latitude and Longitude.
     * This table is also used to predict and validate each Fixed Station and to detect if a station has broken from the Floe.
     * Data from this table is displayed on the Grid.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared after which the new data is pulled from the Synchronization
     *     Server.
     * </p>
     * For more details check the Database Schema.
     */
    public static final String fixedStationTable = "AIS_FIXED_STATION_POSITION";

    /**
     * Table which stores a list of names and MMSIs of Fixed Stations. The {@link de.awi.floenavigation.aismessages.AISDecodingService} checks
     * the MMSI for each decoded AIS message in this table and if the MMSI exists in this table that means that the AIS message is from a Fixed
     * Station and then the actual data received in the AIS message is inserted in the table {@link #fixedStationTable}.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared after which the new data is pulled from the Synchronization
     *     Server and populated in this table.
     * </p>
     */
    public static final String stationListTable = "AIS_STATION_LIST";

    /**
     * This table stores the parameters of Mobile Stations. If the {@link de.awi.floenavigation.aismessages.AISDecodingService} does not find
     * the MMSI from a decoded AIS message in the {@link #stationListTable} it will insert the data received in the AIS message in this table.
     * The {@link de.awi.floenavigation.services.AlphaCalculationService} then calculates the position of each station in this table with in the
     * Floe's Coordinate system. Data from this table is displayed on the Grid.
     * <p>
     *     This table is not synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}.
     * </p>
     */
    public static final String mobileStationTable = "AIS_MOBILE_STATION_POSITION";

    /**
     * Table name for the Database table which stores a list of Administrative Users of the App and their passwords. Only Users who exist
     * in this table have access to the Admin activities of the App.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared after which the new data is pulled from the Synchronization
     *     Server and populated in this table.
     * </p>
     */
    public static final String usersTable = "USERS";

    /**
     * Table name for the Database table which stores the Sample/Measurements taken from this tablet.
     * During {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity} activity when a new Sample is taken its
     * location parameters and other important information is stored in this table along with the label of Sample/Measurement which is
     * then Synchronized with Synchronization Server.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared. Data from the Synchronization Server is not pulled
     *     for this table.
     * </p>
     */
    public static final String sampleMeasurementTable = "SAMPLE_MEASUREMENT";

    /**
     * Table name for the Database table which stores the Devices with which Sample/Measurement are taken. This table is empty when
     * App is used for the first time and it is populated during the Synchronization process with the data imported from the D-Ship Server.
     * This table contains the name, shortname and unique Device ID of the devices. The data from this table is read and populated in the
     * fields in the {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity}. Data from this table is <b>not</b>
     * displayed on the Grid.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data is not pushed to the Sync Server, however, its local copy is cleared after which the new data is pulled from the Synchronization
     *     Server and populated in this table.
     * </p>
     */
    public static final String deviceListTable = "DEVICE_LIST";

    /**
     * Table name for the Database table which stores the Waypoints created from this tablet. A waypoint can be any point of interest on
     * the Ice Floe where a User may want to return to or a User may want to avoid. A series of waypoints can also create a track on the ice.
     * During {@link de.awi.floenavigation.waypoint.WaypointActivity} activity when a new Waypoint is created its location parameters and
     * other important information is stored in this table along with the label of the Waypoint which is displayed on the Grid.
     * Data from this table is displayed on the Grid.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared after which the new data is pulled from the Synchronization
     *     Server and populated in this table.
     * </p>
     */
    public static final String waypointsTable = "WAYPOINTS";

    /**
     * Table name for the Database table which stores all the Configuration Parameters names and their values. These Configuration
     * Parameters are used by different Acitivities and Background Services which define the behavior of the App. Details about each
     * Configuration Parameter can be found with the parameter name.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}, however
     *     only some Configuration Parameters are synchronized. The data from the parameters which are synchronized is pushed to the Sync
     *     Server, and its local copy is cleared after which the new data is pulled from the Synchronization Server and populated in this
     *     table.
     * </p>
     */
    public static final String configParametersTable = "CONFIGURATION_PARAMETERS";

    /**
     * Table name for the Database table which stores the name and MMSI numbers of the two Fixed Stations (Origin and x-Axis marker) with
     * which the Floe's Coordinate System was created during {@link de.awi.floenavigation.initialsetup}. The MMSIs of these two are stored
     * separately because we can then retrieve the Origin location data from {@link #fixedStationTable} by checking the MMSI from this
     * table, and it is also used in {@link de.awi.floenavigation.admin.RecoveryActivity} and {@link de.awi.floenavigation.services.ValidationService}
     * as the rows for the MMSIs stored in this table should not be deleted completely from the {@link #fixedStationTable} else we will
     * not have an Origin.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared after which the new data is pulled from the Synchronization
     *     Server and populated in this table.
     * </p>
     */
    public static final String baseStationTable = "BASE_STATIONS";

    /**
     * Table name for the Database table which stores the latest value for the Angle Beta which defines the Floe's Coordinate System with
     * in the Geographical Coordinate System of the world. The value of the angle is calculated and inserted in this table by
     * {@link de.awi.floenavigation.services.AngleCalculationService} at regular intervals. This value is then used extensively
     * through out the App by different Activities and Background Services.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared after which the new data is pulled from the Synchronization
     *     Server and populated in this table.
     * </p>
     */
    public static final String betaTable = "BETA_TABLE";

    /**
     * Table name for the Database table which stores the Static Stations. Static Stations are fixed points on the ice which may be
     * a tent or a hut or as specified by {@link #stationTypes} like a Fixed Station but these stations do not have an AIS transponder.
     * This table stores the location parameters such as x,y coordinates of the station with in the Floe's Coordinate System which are
     * calculated and inserted in the table by {@link de.awi.floenavigation.deployment.StaticStationFragment}.
     * Data from this table is displayed on the Grid.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared after which the new data is pulled from the Synchronization
     *     Server and populated in this table.
     * </p>
     */
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
    public static final int MOTHER_SHIP_MMSI = 211202460;//230070870;


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
        insertDefaultConfigParams(db, initial_setup_time, String.valueOf(5 * 60 * 1000));
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
        Cursor cursor = null;
        String pwd = "Not Found";
        try {
            SQLiteOpenHelper dbHelper = getDbInstance(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //String query = "SELECT USERNAME, PASSWORD FROM USERS";
            cursor = db.query(usersTable,
                    new String[]{userName, password},
                    null, null, null, null, null);
            String user;


            if (cursor.moveToFirst()) {
                do {

                    user = cursor.getString(0);
                    if (user.equals(uname)) {
                        pwd = cursor.getString(1);

                        break;
                    }
                } while (cursor.moveToNext());
            }
        }catch(SQLException e){
            Log.d(TAG, "Database Unavailable");
        }finally {
            if (cursor != null){
                cursor.close();
            }
        }
        return pwd;
    }

    public  double[] readBaseCoordinatePointsLatLon(Context context){
        double[] coordinates = new double[4];
        Cursor cursor = null;
        try {
            SQLiteOpenHelper dbHelper = getDbInstance(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            cursor = db.query(fixedStationTable,
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
        }finally {
            if(cursor != null){
                cursor.close();
            }
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

        Cursor mDeviceListCursor = null;
        deviceTypes = new ArrayList<String>();
        deviceIDs = new ArrayList<String>();
        deviceNames = new ArrayList<String>();
        deviceShortNames = new ArrayList<String>();

        try {
            SQLiteOpenHelper dbHelper = getDbInstance(mContext);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            mDeviceListCursor = db.query(deviceListTable, null,
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
        }finally {
            if(mDeviceListCursor != null){
                mDeviceListCursor.close();
            }
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
        Cursor formatCursor = null;
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            formatCursor = db.query(DatabaseHelper.configParametersTable,
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
        }finally {
            if(formatCursor != null){
                formatCursor.close();
            }
        }
    }

    public static int readSiginificantDigitsSetting(Context context){
        int significantFigure = 5;
        Cursor mSignificantFiguresCursor = null;
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            mSignificantFiguresCursor = db.query(DatabaseHelper.configParametersTable,
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
        }finally {
            if(mSignificantFiguresCursor != null){
                mSignificantFiguresCursor.close();
            }
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
