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

/**
 * This class provides the methods necessary to Setup and Maintain the {@link SQLiteDatabase} of the App. This is the most important class
 * in the App. When the App is installed on a tablet this Class creates and sets up the Database schema and inserts default values in
 * specific tables. The rest of the Activities and Background Services use this class to access the database and update any values if
 * required.
 * @see SQLiteDatabase
 * @see SQLiteOpenHelper
 */
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

    public static final double LOCATIONRECEIVED = 1;
    /**
     * It is a counter which is appended to the {@link #tabletId}, which forms the label id in the sample and measurement
     * menu.
     * This counter is automatically incremented whenever the user wishes to add a new sample, thus providing the user with a
     * default name for the sample. It is incremented until synchronization of the database tables with the server.
     * This counter is initialized again to '1' after synchronization.
     * The user can however change the label id if this suggestion is not acceptable and can enter another value.
     *
     */
    public static int SAMPLE_ID_COUNTER = 1;

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


    public static final String packet_threshold_time = "PACKET_THRESHOLD_TIME";

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
     * This table contains the name, shortname, type and unique Device ID of the devices. The data from this table is read and populated in the
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

    /**
     * Table name for the Database table which stores a list of deleted Fixed Stations. This table contains the MMSIs and deletion time
     * of each Fixed Station which was deleted from this particular instance of the App whether by Recovery Activity or Validation Service.
     * Whenever a Fixed Station is recovered it specific row is deleted from the {@link #stationListTable} and its MMSI is added to
     * this table. When the Synchronization process is run this table is synchronized with the Sync Server which then deletes the
     * same station from all the other tablets.
     * <p>
     *     When a Fixed Station is deleted either by Recovery or by Validation Service its entry has to be deleted from two or three
     *     different database tables: {@link #stationListTable}, {@link #fixedStationTable} and if it is the origin or x-Axis marker
     *     then from {@link #baseStationTable}. This means that when a fixed station is deleted its MMSI should be added to two or three
     *     different tables: {@link #stationListDeletedTable}, {@link #fixedStationDeletedTable} and in case it is the orign or x-Axis
     *     marker {@link #baseStationDeletedTable}. These deleted tables are then Synchronized with the Sync Server which removes
     *     the entries in these tables from its tables and pushes those tables to other instance of the App so that when a fixed station
     *     is removed from one tablet it is removed from all tablet.
     * </p>
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared. No data is pulled from the Sync Server for this table.
     * </p>
     */
    public static final String stationListDeletedTable = "STATION_LIST_DELETED";

    /**
     * Table name for the Database table which stores a list of deleted Fixed Stations. This table contains the MMSIs and deletion time
     * of each Fixed Station which was deleted from this particular instance of the App whether by Recovery Activity or Validation Service.
     * Whenever a Fixed Station is recovered it specific row is deleted from the {@link #fixedStationTable} and its MMSI is added to this table.
     * When the Synchronization process is run this table is synchronized with the Sync Server which then deletes the same station from
     * all the other tablets.
     * <p>
     *     When a Fixed Station is deleted either by Recovery or by Validation Service its entry has to be deleted from two or three
     *     different database tables: {@link #stationListTable}, {@link #fixedStationTable} and if it is the origin or x-Axis marker
     *     then from {@link #baseStationTable}. This means that when a fixed station is deleted its MMSI should be added to two or three
     *     different tables: {@link #stationListDeletedTable}, {@link #fixedStationDeletedTable} and in case it is the orign or x-Axis
     *     marker {@link #baseStationDeletedTable}. These deleted tables are then Synchronized with the Sync Server which removes
     *     the entries in these tables from its tables and pushes those tables to other instance of the App so that when a fixed station
     *     is removed from one tablet it is removed from all tablet.
     * </p>
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared. No data is pulled from the Sync Server for this table.
     * </p>
     */
    public static final String fixedStationDeletedTable = "FIXED_STATION_DELETED";

    /**
     * Table name for the Database table which stores a list of deleted Static Stations. This table contains the names and deletion time
     * of each Static Station which was deleted from this particular instance of the App. Whenever a Static Station is recovered it specific
     * row is deleted from the {@link #staticStationListTable} and its name is added to this table. When the Synchronization process is run
     * this table is synchronized with the Sync Server which then deletes the same station from all the other tablets.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared. No data is pulled from the Sync Server for this table.
     * </p>
     */
    public static final String staticStationDeletedTable = "STATIC_STATION_DELETED";

    /**
     * Table name for the Database table which stores a list of deleted Base Stations (Base Stations are origin and the x-Axis marker).
     * This table contains the MMSIs and deletion time of each Base Station which was deleted from this particular instance of the App
     * whether by Recovery Activity or Validation Service. Whenever a Base Station is recovered it specific row is deleted from the
     * {@link #baseStationTable} and its MMSI is added to this table. When the Synchronization process is run this table is
     * synchronized with the Sync Server which then deletes the same station from all the other tablets.
     * <p>
     *     When a Fixed Station is deleted either by Recovery or by Validation Service its entry has to be deleted from two or three
     *     different database tables: {@link #stationListTable}, {@link #fixedStationTable} and if it is the origin or x-Axis marker
     *     then from {@link #baseStationTable}. This means that when a fixed station is deleted its MMSI should be added to two or three
     *     different tables: {@link #stationListDeletedTable}, {@link #fixedStationDeletedTable} and in case it is the orign or x-Axis
     *     marker {@link #baseStationDeletedTable}. These deleted tables are then Synchronized with the Sync Server which removes
     *     the entries in these tables from its tables and pushes those tables to other instance of the App so that when a fixed station
     *     is removed from one tablet it is removed from all tablet.
     * </p>
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared. No data is pulled from the Sync Server for this table.
     * </p>
     */
    public static final String baseStationDeletedTable = "BASE_STATION_DELETED";

    /**
     * Table name for the Database table which stores a list of deleted Waypoints. This table contains the names and deletion time
     * of each Waypoint which was deleted from this particular instance of the App. Whenever a Waypoint is deleted its specific
     * row is deleted from the {@link #waypointsTable} and its name is added to this table. When the Synchronization process is run
     * this table is synchronized with the Sync Server which then deletes the same waypoint from all the other tablets.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared. No data is pulled from the Sync Server for this table.
     * </p>
     */
    public static final String waypointDeletedTable = "WAYPOINT_DELETED";

    /**
     * Table name for the Database table which stores a list of deleted Admin Users. This table contains the names and deletion time
     * of each Admin User which was deleted from this particular instance of the App. Whenever an Admin User is deleted its specific
     * row is deleted from the {@link #usersTable} and its name is added to this table. When the Synchronization process is run
     * this table is synchronized with the Sync Server which then deletes the same User from all the other tablets.
     * <p>
     *     This table is synchronized with Synchronization Server during {@link de.awi.floenavigation.synchronization.SyncActivity}. This
     *     table's data pushed to the Sync Server, and its local copy is cleared. No data is pulled from the Sync Server for this table.
     * </p>
     */
    public static final String userDeletedTable = "USERS_DELETED";



    //Database Fields Names
    /**
     * Column name for an AIS Station name. This column name is used in all tables which maintain any AIS station data, such as Fixed Stations
     * or Mobile Stations. This is a TEXT field.
     * <p>
     *     A column of this name is present in several database tables such as {@link #stationListTable}, {@link #fixedStationTable},
     *     {@link #baseStationTable}, {@link #mobileStationTable} and their respective deleted tables if it exists.
     * </p>
     */
    public static final String stationName = "AIS_STATION_NAME";

    /**
     * Column name for storing the Latitude of any point or station. This column name is used in all tables which maintain any geographic data,
     * such as Fixed Stations or Waypoints etc. The data type for this field REAL.
     * <p>
     *     A column of this name is present in several database tables such as {@link #waypointsTable}, {@link #fixedStationTable},
     *     {@link #mobileStationTable} and {@link #sampleMeasurementTable}.
     * </p>
     */
    public static final String latitude = "LATITUDE";

    /**
     * Column name for storing the Longitude of any point or station. This column name is used in all tables which maintain any geographic data,
     * such as Fixed Stations or Waypoints etc. The data type for this field REAL.
     * <p>
     *     A column of this name is present in several database tables such as {@link #waypointsTable}, {@link #fixedStationTable},
     *     {@link #mobileStationTable} and {@link #sampleMeasurementTable}.
     * </p>
     */
    public static final String longitude = "LONGITUDE";

    /**
     * Column name for storing the Latitude received from the AIS data of a Fixed Station. This column is used in {@link #fixedStationTable}
     * where data from this column is compared with data from {@link #latitude} column by the {@link de.awi.floenavigation.services.ValidationService}
     * to check if the Fixed Station has broken off. The data type for this field REAL.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}.
     * </p>
     */
    public static final String recvdLatitude = "RECEIVED_LATITUDE";

    /**
     * Column name for storing the Longitude received from the AIS data of a Fixed Station. This column is used in {@link #fixedStationTable}
     * where data from this column is compared with data from {@link #latitude} column by the {@link de.awi.floenavigation.services.ValidationService}
     * to check if the Fixed Station has broken off. The data type for this field REAL.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}.
     * </p>
     */
    public static final String recvdLongitude = "RECEIVED_LONGITUDE";

    /**
     * Column name for storing the x coordinates of any point or station in the Floe's Coordinate System. This column name is used in all
     * tables which maintain any location data, such as Fixed Stations or Waypoints etc. Data from this column is used to display
     * the stations/waypoints on the Grid. The data type for this field REAL.
     * <p>
     *     A column of this name is present in several database tables such as {@link #waypointsTable}, {@link #fixedStationTable},
     *     {@link #mobileStationTable}, {@link #staticStationListTable} and {@link #sampleMeasurementTable}.
     * </p>
     */
    public static final String xPosition = "X_POSITION";

    /**
     * Column name for storing the y coordinates of any point or station in the Floe's Coordinate System. This column name is used in all
     * tables which maintain any location data, such as Fixed Stations or Waypoints etc. Data from this column is used to display
     * the stations/waypoints on the Grid. The data type for this field REAL.
     * <p>
     *     A column of this name is present in several database tables such as {@link #waypointsTable}, {@link #fixedStationTable},
     *     {@link #mobileStationTable}, {@link #staticStationListTable} and {@link #sampleMeasurementTable}.
     * </p>
     */
    public static final String yPosition = "Y_POSITION";

    /**
     * Column name for storing the Speed Over Ground of an AIS Station as received by {@link de.awi.floenavigation.aismessages.AISDecodingService}.
     * This column is used in {@link #fixedStationTable} where data from this column is used by {@link de.awi.floenavigation.services.PredictionService}
     * to predict the next position of the Fixed Station. The data type for this field REAL.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}.
     * </p>
     */
    public static final String sog = "SPEED_OVER_GROUND";

    /**
     * Column name for storing the Course Over Ground of an AIS Station as received by {@link de.awi.floenavigation.aismessages.AISDecodingService}.
     * This column is used in {@link #fixedStationTable} where data from this column is used by {@link de.awi.floenavigation.services.PredictionService}
     * to predict the next position of the Fixed Station. The data type for this field REAL.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}.
     * </p>
     */
    public static final String cog = "COURSE_OVER_GROUND";

    /**
     * Column name storing the Angle Alpha. Alpha is the angle a station/waypoint makes with the x-Axis of the Floe's Coordinate System.
     * It is used to calculate the {@link #xPosition} and {@link #yPosition} of a Station or Waypoint. This column is used in any table
     * which maintains any location data. The data type for this field REAL.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}, {@link #mobileStationTable} and
     *     {@link #staticStationListTable}.
     * </p>
     */
    public static final String alpha = "ALPHA";

    /**
     * Column name storing the Angle Beta. Beta is the angle  the x-Axis of the Floe's Coordinate System makes with longitudinal axis
     * of the Geographic Coordinate system (The World Coordinate system). This angle defines the Floe's Coordinate System and it is
     * calculated and updated at regular intervals by {@link de.awi.floenavigation.services.AngleCalculationService}.
     * The data type for this field REAL.
     * <p>
     *     A column of this name is present in the database table {@link #betaTable}.
     * </p>
     */
    public static final String beta = "BETA";

    /**
     * Column name for storing the type of AIS Message received from the AIS Transponder of a Fixed Station. The AIS protocol is made
     * up of several different types of messages and only certain types of messages are used in this App. The data from this column
     * tells whether the last received AIS packet was a Position Report or a Static Data Report. The data type for this field INTEGER.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}.
     * </p>
     */
    public static final String packetType = "LAST_RECEIVED_PACKET_TYPE";

    /**
     * Specifies the name for a Column which gives how much accurate the predictions have been for a Fixed Station. This is a
     * NUMERIC field which is incremented  by the {@link de.awi.floenavigation.services.ValidationService} every time the distance between
     * the received coordinates ({@link #recvdLatitude}, {@link #recvdLongitude}), and the predicted coordinates
     * ({@link #latitude}, {@link #longitude}) exceeds the value specified by {@link #error_threshold}. If the value in this field goes
     * above a certain value specified by a ratio of {@link #prediction_accuracy_threshold} with
     * {@link de.awi.floenavigation.services.ValidationService#VALIDATION_TIME}, and the value in {@link #incorrectMessageCount} goes
     * above <b>three</b> then that Fixed Station will considered to be broken from the Floe.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}.
     * </p>
     */
    public static final String predictionAccuracy = "PREDICTION_ACCURACY";

    /**
     * Column name for storing the MMSI of an AIS Station. MMSI is a unique number associated with each AIS Station. This is an INTEGER
     * field which is used to identify AIS Stations. This column is used in any table which maintains AIS Data.
     * <p>
     *     A column of this name is present in the database table {@link #stationListTable}, {@link #fixedStationTable},
     *     {@link #baseStationTable}, {@link #mobileStationTable}.
     * </p>
     */
    public static final String mmsi = "MMSI";

    /**
     * This column specifies the unique Station name for a Static Station. This is TEXT field which is used to identify
     * Static Stations.
     * <p>
     *     A column of this name is present in the database table {@link #staticStationListTable}.
     * </p>
     */
    public static final String staticStationName = "STATION_NAME";

    /**
     * Specifies the name of a Column which stores the type of a Fixed or Static Station. Fixed and Static Station have a type associated
     * with them. The different types of stations is specified by {@link #stationTypes}. This is a TEXT field.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}, {@link #staticStationListTable}.
     * </p>
     */
    public static final String stationType = "STATION_TYPE";

    /**
     * Specifies the name of a Column which stores the distance in meters of a station or waypoint from the Origin of the Floe's Coordinate
     * system. This field along with {@link #alpha} is used to calculate the {@link #xPosition} and {@link #yPosition} of a Station
     * or Waypoint. This column is used in any table which maintains any location data. The data type for this field REAL.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}, {@link #mobileStationTable} and
     *     {@link #staticStationListTable}.
     * </p>
     */
    public static final String distance = "DISTANCE";

    /**
     * Specifies the name of a Column which stores the type of Device. Devices are used to take Sample/Measurements in
     * {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity}. This is a TEXT field.
     * <p>
     *     A column of this name is present in the database table {@link #deviceListTable} and {@link #sampleMeasurementTable}.
     * </p>
     */
    public static final String deviceType = "DEVICE_TYPE";

    /**
     * Specifies the name of a Column which stores the time when a specific event happened. This field has different meanings in different
     * tables. The column in {@link #fixedStationTable} and {@link #mobileStationTable} specify the time at which the last AIS packet was
     * received. The same column in {@link #betaTable} specifies the time at which the current value of {@link #beta} was calculated
     * and inserted in the table. This field in {@link #sampleMeasurementTable} and {@link #waypointsTable} specify the time at which
     * the sample was taken and the time at which the Waypoint was created respectively. This is a TEXT field
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}, {@link #mobileStationTable},
     *     {@link #betaTable}, {@link #waypointsTable} and {@link #sampleMeasurementTable}.
     * </p>
     */
    public static final String updateTime = "UPDATE_TIME";

    /**
     * Specifies the name of a Column which tells whether the location parameters of a Mobile Station haven been calculated or not. When
     * an AIS Packet is received and decoded by {@link de.awi.floenavigation.aismessages.AISDecodingService} it checks for the MMSI in
     * {@link #stationListTable}, if the MMSI is not there it will insert the AIS Data in {@link #mobileStationTable}. However, the
     * x, y coordinates of the Mobile Station are not calculated immediately so it cannot be displayed on Grid then.
     * When the x,y coordinates of the mobile station are calculated by the {@link de.awi.floenavigation.services.AlphaCalculationService}
     * it sets the value of this field in the {@link #mobileStationTable} and only is it displayed by the Grid. This is a NUMERIC field.
     * <p>
     *     A column of this name is present in the database table {@link #mobileStationTable}.
     * </p>
     */
    public static final String isCalculated = "IS_COORDINATE_CALCULATED";

    /**
     * Specifies the name of a Column which tells whether the current values of the Geographic coordinates
     * ({@link #latitude}, {@link #longitude}) of a Fixed Station are predicted. When a new packet is received by the
     * {@link de.awi.floenavigation.aismessages.AISDecodingService} it clears this field and when the {@link de.awi.floenavigation.services.PredictionService}
     * updates these fields it sets this field. The data type of this field is NUMERIC.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}.
     * </p>
     */
    public static final String isPredicted = "IS_POSITION_PREDICTED";

    /**
     * Specifies the name of a Column which gives the count of consecutive incorrect predictions for a Fixed Station. This is an INTEGER
     * field which is incremented  by the {@link de.awi.floenavigation.services.ValidationService} every time the distance between
     * the received coordinates ({@link #recvdLatitude}, {@link #recvdLongitude}), and the predicted coordinates
     * ({@link #latitude}, {@link #longitude}) exceeds the value specified by {@link #error_threshold}. If the value in this field goes
     * above <b>three</b> and the value in {@link #predictionAccuracy} goes above a specific value then that Fixed Station is considered
     * to be broken from the Floe.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}.
     * </p>
     * @see #predictionAccuracy
     */
    public static final String incorrectMessageCount = "INCORRECT_MESSAGE_COUNT";

    /**
     * Column name for storing the time at which {@link de.awi.floenavigation.services.ValidationService} last checked this particular
     * Fixed Station. The columns {@link #incorrectMessageCount} and {@link #predictionAccuracy} are incremented only if the
     * {@link #updateTime} for a station is later than this time. This is a TEXT field
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}.
     * </p>
     * @see #predictionAccuracy
     * @see #incorrectMessageCount
     */
    public static final String validationCheckTime = "VALIDATION_CHECK_TIME";

    /**
     * Column name for storing the time at which {@link de.awi.floenavigation.services.PredictionService} last predicted the position for
     * this particular Fixed Station. This is a TEXT field
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}.
     * </p>
     * @see #predictionAccuracy
     */
    public static final String predictionTime = "PREDICTION_TIME";
    /**
     * Column name for a field which is set when an AIS packet is received from a Fixed Station. This is specially useful in deployment
     * of new station. When a new station is deployed in the first step only the name and MMSI of the station are inserted in the
     * {@link #fixedStationTable} and {@link #stationListTable}. When the {@link de.awi.floenavigation.aismessages.AISDecodingService}
     * receives an AIS packet from the new station it will check for the MMSI in {@link #stationListTable} and then insert the Position
     * Report data in {@link #fixedStationTable} and set this field to 1 and only then will the deployment of the Fixed Station be
     * complete. The data type for this field is NUMERIC.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationTable}.
     * </p>
     */
    public static final String isLocationReceived = "IS_LOCATION_RECEIVED";

    /**
     * Specifies the Column name for storing the unique Usernames in the {@link #usersTable} table. This is a TEXT field and is used to
     * identify an Admin User.
     * <p>
     *     A column of this name is present in the database table {@link #usersTable}.
     * </p>
     */
    public static final String userName = "USERNAME";

    /**
     * Specifies the Column name for storing the password for a user in the {@link #usersTable} table. This is a TEXT field and is used to
     * identify an Admin User.
     * <p>
     *     A column of this name is present in the database table {@link #usersTable}.
     * </p>
     */
    public static final String password = "PASSWORD";

    /**
     * Column name for storing the Unique ID of a Device. This is a Unique Identifier associated with each device, from which Sample/Measurement
     * can be taken, this ID is generated by the D-Ship Server and then imported in the Synchronization process along with other device data.
     * This is a TEXT field and the data from this field is a part of the label generated for eah Sample taken from the App which is then
     * pushed to Synchronization Server and then onwards to the D-Ship Server.
     * <p>
     *     A column of this name is present in the database table {@link #deviceListTable} and {@link #sampleMeasurementTable}.
     * </p>
     */
    public static final String deviceID = "DEVICE_ID";

    /**
     * Column name for storing the name of a Device. This is the full name of each device, from which Sample/Measurement
     * can be taken, this is imported from the D-Ship Server in the Synchronization process along with other device data.
     * This is a TEXT field.
     * <p>
     *     A column of this name is present in the database table {@link #deviceListTable} and {@link #sampleMeasurementTable}.
     * </p>
     */
    public static final String deviceName = "DEVICE_NAME";

    /**
     * Column name for storing the name of a Device. This is the short name of a device, which is imported from the D-Ship Server in the
     * Synchronization process along with other device data. This field is used to create the {@link android.widget.AutoCompleteTextView}
     * in the layout of the {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity} and based on the value selected
     * other fields of the Device are populated automatically in the Layout.
     * This is a TEXT field.
     * <p>
     *     A column of this name is present in the database table {@link #deviceListTable} and {@link #sampleMeasurementTable}.
     * </p>
     */
    public static final String deviceShortName = "DEVICE_SHORT_NAME";

    /**
     * Column name for storing the operation performed during Sample/Measurement. The data in this field specifies for each Sample/Measurement
     * whether the operation performed was a Sample or a Measurement. This is a TEXT field and the data from this field is a part of the label
     * generated for eah Sample/Measurement taken from the App which is then pushed to Synchronization Server and then onwards
     * to the D-Ship Server.
     * <p>
     *     A column of this name is present in the database table {@link #sampleMeasurementTable}.
     * </p>
     */
    public static final String operation = "OPERATION";

    /**
     * Column name for storing the text/label associated with each Waypoint or Sample/Measurement. This is a TEXT field which is used
     * to store the Unique Waypoint name or in case of Sample/Measurement a simple Text. The data from this field is a part of the label
     * generated for eah Sample/Measurement taken from the App which is then pushed to Synchronization Server and then onwards
     * to the D-Ship Server.
     * <p>
     *     A column of this name is present in the database table {@link #sampleMeasurementTable} and {@link #waypointsTable}.
     * </p>
     */
    public static final String labelID = "LABEL_ID";

    /**
     * Column name for storing the complete label generated with in the App for each Waypoint or Sample/Measurement. This is a TEXT
     * field which is generated by the App when a Sample/Measurement is created or when a new Waypoint is created. This field is created
     * by appending specific parameters of the Waypoint or Sample/Measurement in Comma Separated Values (CSV). For a Waypoint the format
     * of the label is: time (UTC), tabletLatitude, tabletLongitude, current x Position, current y position, 0, {@link #labelID}.
     * For a Sample/Measurement the label is: time (UTC), tabletLatitude, tabletLongitude, current x Position, current y position,
     * {@link #labelID}, {@link #operation}, {@link #comment}, {@link #deviceID}. The time value is formatted to yyyyMMdd'D'HHmmss.
     * <p>
     *     A column of this name is present in the database table {@link #sampleMeasurementTable} and {@link #waypointsTable}.
     * </p>
     */
    public static final String label = "LABEL";

    /**
     * Column name for storing the name of Configuration Parameters. These Configuration Parameters are used by different Activities
     * and Background Services and their values define the behavior of the App. This is a TEXT field and the data which can be inserted
     * in this field is defined by the array {@link #configurationParameters}.
     * <p>
     *     A column of this name is present in the database table {@link #configParametersTable}.
     * </p>
     */
    public static final String parameterName = "PARAMETER_NAME";

    /**
     * Column name for storing the value of a Configuration Parameter. These Configuration Parameters are used by different Activities
     * and Background Services and their values define the behavior of the App. This is a TEXT field.
     * <p>
     *     A column of this name is present in the database table {@link #configParametersTable}.
     * </p>
     */
    public static final String parameterValue = "PARAMETER_VALUE";

    /**
     * Column name for storing the flag which indicates which Fixed Station is the Origin station. The Origin Station is the most important
     * station as the {@link #distance} and {@link #alpha} parameters for other stations are calculated with respect to the Origin station.
     * This is a NUMERIC field and its value is set 1 for only one station in the {@link #baseStationTable} during the
     * {@link de.awi.floenavigation.initialsetup.GridSetupActivity}. When the location data for the origin is required the {@link #baseStationTable}
     * is queried selecting only that station for which this field value is 1. This returns the MMSI of the Origin station and then
     * {@link #fixedStationTable} can be queried for location data of the origin with its MMSI.
     * <p>
     *     A column of this name is present in the database table {@link #baseStationTable}.
     * </p>
     */
    public static final String isOrigin = "IS_ORIGIN";

    /**
     * Column name for storing the time at which a given Station (Fixed or Static) or Waypoint was deleted. When a Station or Waypoint is
     * recovered its entry is removed from its given tables and a new entry is inserted in the Deleted tables along with the time of deletion.
     * The deleted tables are then synchronized during the Synchronization Process so that the same station or Waypoint is deleted from
     * all the other tablets. This is a TEXT field.
     * <p>
     *     A column of this name is present in the database table {@link #fixedStationDeletedTable}, {@link #stationListDeletedTable},
     *     {@link #staticStationDeletedTable}, {@link #waypointDeletedTable}, {@link #userDeletedTable} and {@link #baseStationDeletedTable}.
     * </p>
     */
    public static final String deleteTime = "DELETE_TIME";

    /**
     * Column name for storing the Comment field of a Sample/Measurement. A comment is a free text associated with each Sample/Measurement.
     * This is a TEXT field and the data from this field is a part of the label generated for eah Sample taken from the App which is then
     * pushed to Synchronization Server and then onwards to the D-Ship Server.
     * <p>
     *     A column of this name is present in the database table {@link #sampleMeasurementTable}.
     * </p>
     */
    public static final String comment = "COMMENT";


    /**
     * A string specifying the value to insert in the {@link #stationName} field of the {@link #fixedStationTable} in case the Origin
     * station is recovered or is broken.
     * <p>
     *     When any Fixed Station is recovered its entire row in the {@link #fixedStationTable} is deleted, however for the Origin and
     *     the x-Axis the row cannot be deleted as these stations define the angle {@link #beta} and the parameters for other
     *     stations and waypoints and sample/measurements such as {@link #alpha} and {@link #distance} are calculated in reference to
     *     these stations. So when the Origin or x-Axis marker is recovered their entries are deleted from the {@link #stationListTable},
     *     however in the {@link #fixedStationTable} the MMSI and Station Name are replaced with a predefined number and text respectively.
     *     The location data for the stations is then updated only by the {@link de.awi.floenavigation.services.PredictionService} and since
     *     their MMSI is removed from {@link #stationListTable}, no AIS data from their MMSI will be inserted in {@link #fixedStationTable}.
     * </p>
     */
    public static final String origin = "ORIGIN";

    /**
     * A string specifying the value to insert in the {@link #stationName} field of the {@link #fixedStationTable} in case the x-Axis
     * station is recovered or is broken.
     * <p>
     *     When any Fixed Station is recovered its entire row in the {@link #fixedStationTable} is deleted, however for the Origin and
     *     the x-Axis the row cannot be deleted as these stations define the angle {@link #beta} and the parameters for other
     *     stations and waypoints and sample/measurements such as {@link #alpha} and {@link #distance} are calculated in reference to
     *     these stations. So when the Origin or x-Axis marker is recovered their entries are deleted from the {@link #stationListTable},
     *     however in the {@link #fixedStationTable} the MMSI and Station Name are replaced with a predefined number and text respectively.
     *     The location data for the stations is then updated only by the {@link de.awi.floenavigation.services.PredictionService} and since
     *     their MMSI is removed from {@link #stationListTable}, no AIS data from their MMSI will be inserted in {@link #fixedStationTable}.
     * </p>
     */
    public static final String basestn1 = "BASE_STN";
    //Initial Position of Setup Points in Custom Coordinate System
    /**
     * Default value of x coordinate to insert in Database when deploying the Origin during Grid Initial Configuration. As it is
     * the Origin its x,y are {0,0}
     */
    public static final long station1InitialX = 0;

    /**
     * Default value of y coordinate to insert in Database when deploying the Origin during Grid Initial Configuration. As it is
     * the Origin its x,y are {0,0}
     */
    public static final long station1InitialY = 0;
    //public static final long station2InitialX = 500;

    /**
     * Default value of y coordinate to insert in Database when deploying the x-Axis marker during Grid Initial Configuration. As it is
     * the Origin its x,y are {distance from origin,0}
     */
    public static final long station2InitialY = 0;

    /**
     * Default value of angle {@link #alpha} to insert in Database when deploying the Origin during Grid Initial Configuration. As it is
     * the Origin and Alpha is the angle any point makes with the x-Axis of the Floe's Coordinate system so for both the Origin and the
     * x-Axis marker Alpha is 0.
     */
    public static final double station1Alpha = 0.0;

    /**
     * Default value of angle {@link #alpha} to insert in Database when deploying the x-Axis marker during Grid Initial Configuration. As it is
     * the Origin and Alpha is the angle any point makes with the x-Axis of the Floe's Coordinate system so for both the Origin and the
     * x-Axis marker Alpha is 0.
     */
    public static final double station2Alpha = 0.0;

    /**
     * The value to set of the field {@link #isCalculated} in the Database table {@link #mobileStationTable} when {@link de.awi.floenavigation.services.AlphaCalculationService}
     * calculates the location parameters of the Mobile Station. The {@link de.awi.floenavigation.grid.GridActivity} only displays those
     * Mobile Stations whose {@link #isCalculated} has been set to this value.
     */
    public static final int MOBILE_STATION_IS_CALCULATED = 1;

    /**
     * The value to insert in the {@link #mmsi} field of the {@link #fixedStationTable} in case the Origin
     * station is recovered or is broken.
     * <p>
     *     When any Fixed Station is recovered its entire row in the {@link #fixedStationTable} is deleted, however for the Origin and
     *     the x-Axis the row cannot be deleted as these stations define the angle {@link #beta} and the parameters for other
     *     stations and waypoints and sample/measurements such as {@link #alpha} and {@link #distance} are calculated in reference to
     *     these stations. So when the Origin or x-Axis marker is recovered their entries are deleted from the {@link #stationListTable},
     *     however in the {@link #fixedStationTable} the MMSI and Station Name are replaced with a predefined number and text respectively.
     *     The location data for the stations is then updated only by the {@link de.awi.floenavigation.services.PredictionService} and since
     *     their MMSI is removed from {@link #stationListTable}, no AIS data from their MMSI will be inserted in {@link #fixedStationTable}.
     * </p>
     */
    public static final int BASESTN1 = 1000;

    /**
     * The value to insert in the {@link #mmsi} field of the {@link #fixedStationTable} in case the x-Axis marker
     * station is recovered or is broken.
     * <p>
     *     When any Fixed Station is recovered its entire row in the {@link #fixedStationTable} is deleted, however for the Origin and
     *     the x-Axis the row cannot be deleted as these stations define the angle {@link #beta} and the parameters for other
     *     stations and waypoints and sample/measurements such as {@link #alpha} and {@link #distance} are calculated in reference to
     *     these stations. So when the Origin or x-Axis marker is recovered their entries are deleted from the {@link #stationListTable},
     *     however in the {@link #fixedStationTable} the MMSI and Station Name are replaced with a predefined number and text respectively.
     *     The location data for the stations is then updated only by the {@link de.awi.floenavigation.services.PredictionService} and since
     *     their MMSI is removed from {@link #stationListTable}, no AIS data from their MMSI will be inserted in {@link #fixedStationTable}.
     * </p>
     */
    public static final int BASESTN2 = 1001;

    /**
     * {@link List} to hold the {@link #deviceNames} of all the Devices existing in the Database table {@link #deviceListTable}. This list
     * is used to populate the Device details on the layout of {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity},
     * once a {@link #deviceShortName} has been selected from the {@link android.widget.AutoCompleteTextView}.
     */
    public static List<String> deviceNames;

    /**
     * {@link List} to hold the {@link #deviceShortName} of all the Devices existing in the Database table {@link #deviceListTable}. This list
     * is used to provide the drop down list in {@link android.widget.AutoCompleteTextView} on the layout of
     * {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity}.
     */
    public static List<String> deviceShortNames;

    /**
     * {@link List} to hold the {@link #deviceID} of all the Devices existing in the Database table {@link #deviceListTable}. This list
     * is used to populate the Device details on the layout of {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity},
     * once a {@link #deviceShortName} has been selected from the {@link android.widget.AutoCompleteTextView}.
     */
    public static List<String> deviceIDs;

    /**
     * {@link List} to hold the {@link #deviceType} of all the Devices existing in the Database table {@link #deviceListTable}. This list
     * is used to populate the Device details on the layout of {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity},
     * once a {@link #deviceShortName} has been selected from the {@link android.widget.AutoCompleteTextView}.
     */
    public static List<String> deviceTypes;

    /**
     * String array which provides the values which can be stored in {@link #stationType}. This array is used to populate the
     * {@link android.widget.Spinner} in the layout of {@link de.awi.floenavigation.deployment.DeploymentActivity}. The same array is used
     * for both Static and Fixed Station.
     */
    public static final String[] stationTypes = {
            "Tent",
            "Hut",
            "Mast",
            "Fixpoint",
    };

    /**
     * String array which provides names of the Configuration Parameters to be stored in the Database table {@link #configParametersTable}.
     * When the App is installed on a tablet for the first time, the {@link #parameterName} column of the {@link #configParametersTable}
     * is populated with this array and default values..
     */
    public static final String[] configurationParameters = {
            "ERROR_THRESHOLD",
            "PREDICTION_ACCURACY_THRESHOLD",
            "LATITUDE_LONGITUDE_VIEW_FORMAT",
            "DECIMAL_NUMBER_SIGNIFICANT_FIGURES",
            "INITIAL_SETUP_TIME",
            "SYNC_SERVER_HOSTNAME",
            "SYNC_SERVER_PORT",
            "TABLET_ID",
            "PACKET_THRESHOLD_TIME"
    };

    /**
     * Constant defining the MMSI of the Mothership which will be shown as a Star on the Mapview. By default this should be the MMSI of
     * Polarstern.
     */
    //public static final int MOTHER_SHIP_MMSI = 211202460;
    //For Testing purposes
    public static final int MOTHER_SHIP_MMSI = 211202460;//230070870;



    /**
     * Default Constructor
     */
    public DatabaseHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Default {@link SQLiteOpenHelper#onCreate(SQLiteDatabase)}. This method is called when the App is installed on a tablet. It creates
     * the Database tables, populates the {@link #configParametersTable} with default values of each {@link #parameterName} and creates
     * a default Admin User.
     */
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
        insertDefaultConfigParams(db, packet_threshold_time, String.valueOf(5 * 60 * 1000));

        //Create a Default User
        insertUser(db, "awi", "awi");


    }

    /**
     * Default {@link SQLiteOpenHelper#onUpgrade(SQLiteDatabase, int, int)} . This method is called when the App is installed on a tablet
     * which already has an older version of this App running. It just clears the data from the earlier tables and creates any new columns
     * or tables which were not a part of the old version of the database.
     * @param oldVersion The {@link #DB_VERSION} currently running on the App installed.
     * @param newVersion The {@link #DB_VERSION} of the updated Database.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        createTables(db, oldVersion, newVersion);
    }

    /**
     * This method sets up the Database schema for the App to run. If this the first installation of the App on a tablet it will create
     * all the tables and define the columns of each table and set up the complete schema. If there is an older version of the App already
     * running on the Tablet. It will just clear the data from most of the tables and add the {@link #comment} field to the
     * {@link #sampleMeasurementTable} as it was part of the previous version of this App.
     * <p>
     *     Please note that it will not clear data from {@link #configParametersTable} or {@link #usersTable} but only clear data from
     *     those tables which are used for creating and maintaining the Coordinate System.
     * </p>
     * @param oldVersion The {@link #DB_VERSION} currently running on the App installed.
     * @param newVersion The {@link #DB_VERSION} of the updated Database.
     */
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
                        predictionTime + " TEXT, " +
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
                        //operation + " TEXT, " +
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

    /**
     * Inserts a User and its specified Password in the Database table {@link #usersTable}. Used to create a new Admin User.
     * @param name {@link #userName} of the new User.
     * @param pass {@link #password} of the new User.
     */
    public static void insertUser(SQLiteDatabase db, String name, String pass){
        ContentValues defaultUser = new ContentValues();
        defaultUser.put(userName, name);
        defaultUser.put(password, pass);
        db.insert(usersTable, null, defaultUser);
    }

    /**
     * Inserts a new Configuration Parameter and its value in the Database table {@link #configParametersTable}. This is used when the
     * App is installed on the tablet and {@link #configParametersTable} needs to be populated with default values.
     * @param name {@link #parameterName} of the Configuration Parameter.
     * @param value {@link #parameterValue} of the Configuration Parameter.
     */
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

    /**
     * Given a {@link #userName} this method will search the {@link #usersTable} to check if the user exists and if it exists it will
     * return the {@link #password} of that User. This is used by {@link de.awi.floenavigation.admin.LoginPage} to validate the Admin User
     * and this is also used to check for replications when creating a new User.
     * @param uname {@link #userName} of the User
     * @param context The {@link Context} in which the App is running.
     * @return The {@link #password} for the given {@link #userName}.
     */
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

    /*
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
    */

    /**
     * Creates an instance of {@link DatabaseHelper}. This ensures that the only one Activity or Service is accessing the Database at a
     * time and prevents concurrent read or write operations.
     * @param context The {@link Context} in which the App is running.
     * @return A {@link DatabaseHelper} object.
     */
    public static synchronized DatabaseHelper getDbInstance(Context context){
        if (dbInstance == null){
            dbInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return dbInstance;
    }

    /**
     * Loads the Device data from the Database table {@link #deviceListTable} and populates each column in their respective {@link List}.
     * This method is called by the {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity} to load the Device data
     * when take a Sample/Measurement
     * @param mContext The {@link Context} in which the App is running.
     */
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

    /**
     * Creates an {@link ArrayAdapter} which is attached to the {@link List} of {@link #deviceShortNames}.
     * @param mContext The {@link Context} in which the App is running.
     * @return An {@link ArrayAdapter} of type String which is attached to {@link #deviceShortNames}.
     */
    public static ArrayAdapter<String> advancedSearchTextView(Context mContext){

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_dropdown_item_1line, deviceShortNames);
        return adapter;
    }

    /**
     * When a {@link #deviceShortName} is selected by the User in the {@link android.widget.AutoCompleteTextView} on the layout of the
     * {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity}, the other attributes of the same device need to be
     * Auto filled in the layout. This method returns the rest of the device attributes given its Short name in an {@link ArrayList}
     * which can then be used to populate the rest of the fields on the Layout.
     * @param devShortName {@link #deviceShortName} of the Device selected from {@link android.widget.AutoCompleteTextView}.
     * @return An {@link ArrayList} containing the {@link #deviceID}, {@link #deviceName}, {@link #deviceType} for the given
     * {@link #deviceShortName}.
     */
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

    /**
     * This method reads the {@link #configParametersTable} to read the current value of the parameter {@link #lat_long_view_format}. The
     * value stored in the table is either 0 (display format will be Degree° Minutes' Seconds'' Direction) or 1 (Degree.xxx).
     * @param context The {@link Context} in which the App is running.
     * @return <code>true</code> if display format is Degree° Minutes' Seconds'' Direction or <code>false</code> if format is Degree.xxx.
     */
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

    /**
     * This method reads the {@link #configParametersTable} to read the current value of the parameter {@link #decimal_number_significant_figures}.
     * @param context The {@link Context} in which the App is running.
     * @return The number of significant figures to display on the interface of the App.
     */
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

    /**
     * This method updates the value of the Configuration Parameter {@link #lat_long_view_format}.
     * @param context The {@link Context} in which the App is running.
     * @param changeDegFormat if <code>true</code> the value stored will 0 (display format will be Degree° Minutes' Seconds'' Direction)
     *                        else the value stored will be 1 (display format will be Degree.xxx).
     * @return <code>true</code> if the value is updated successfully.
     */
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
