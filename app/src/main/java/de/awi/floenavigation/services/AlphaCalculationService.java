package de.awi.floenavigation.services;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Timer;

import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.synchronization.SyncActivity;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * {@link AlphaCalculationService} class is used to calculate {@link DatabaseHelper#alpha} at
 * regular intervals for all the
 * mobile stations and subsequently calculate the corresponding coordinates in the grid
 * </p>
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class AlphaCalculationService extends IntentService {

    public static final String INTENT_TAG = "CalculateAlpha";
    public static final String MMSI_TAG = "MMSI";
    public static final String LATITUDE_TAG = "LAT";
    public static final String LONGITUDE_TAG = "LON";

    private static final String TAG = "AlphaCalculationService";
    /**
     * Variable used to store the value of {@value DatabaseHelper#beta}
     * It is the angle between the x-axis and the geographic longitudinal axis
     */
    private double beta;
    /**
     * Variable used to store the mmsi of the origin fixed station
     */
    private int originMMSI;
    /**
     * Variable used to store the mmsi's of the mobile stations
     */
    private int stationMMSI;
    /**
     * Variable used to store the latitude coordinate of the origin fixed station
     * Read from the internal local database table {@link DatabaseHelper#fixedStationTable}
     */
    private double originLatitude;
    /**
     * Variable used to store the longitude coordinate of the origin fixed station
     * Read from the internal local database table {@link DatabaseHelper#fixedStationTable}
     */
    private double originLongitude;
    /**
     * Variable used to store the latitude of the mobile station
     * Read from the internal local database table {@link DatabaseHelper#mobileStationTable}
     */
    private double stationLatitude;
    /**
     * Variable used to store the longitude of the mobile station
     * Read from the internal local database table {@link DatabaseHelper#mobileStationTable}
     */
    private double stationLongitude;
    /**
     * distance calculated between the origin fixed station and the mobile station in meters
     */
    private double distance;
    /**
     * X axis value in meters of the mobile station
     */
    private double stationX;
    /**
     * Y axis value in meters of the mobile station
     */
    private double stationY;
    /**
     * Angle calculated between the origin fixed station and the mobile station
     */
    private double theta;
    /**
     * Angle calculated between the x-axis and the mobile station
     */
    private double alpha;
    /**
     * Mobile station cursor to iterate over the rows of the database table {@link
     * DatabaseHelper#mobileStationTable}
     */
    private Cursor mobileStationCursor = null;
    /**
     * Timer to execute the task of calculating angle periodically
     */
    Timer timer = new Timer();
    /**
     * Timer value
     */
    private static final int TIMER_PERIOD = 10 * 1000;
    /**
     * Delay before starting the timer
     */
    private static final int TIMER_DELAY = 0;

    /**
     * Broadcast receiver to receive the {@link GPS_Service#GPSTime}
     */
    private BroadcastReceiver broadcastReceiver;
    /**
     * variable to store the gps time received from {@link #broadcastReceiver}
     */
    private long gpsTime;
    /**
     * It is used to synchronize the update time with the gps time
     * Stores the timing difference between {@link System#currentTimeMillis()} and {@link
     * GPS_Service#GPSTime}
     */
    private long timeDiff;

    /**
     * <code>true</code> to stop the timer
     * <code>false</code> otherwise
     */
    private static boolean stopTimer = false;

    /**
     * Not used - only for debugging
     */
    private static AlphaCalculationService instance = null;


    /**
     * Default constructor
     */
    public AlphaCalculationService() {
        super(TAG);
    }

    //    /**
    //     * onDestroy method of the activity life cycle used to unregister the {@link
    //     #broadcastReceiver}
    //     */
    //    @Override
    //    public void onDestroy() {
    //        super.onDestroy();
    //        instance = null;
    //
    //        if (broadcastReceiver != null) {
    //            unregisterReceiver(broadcastReceiver);
    //            broadcastReceiver = null;
    //        }
    //
    //    }


    /**
     * onCreate method of the activity lifecycle used to register the {@link #broadcastReceiver}
     * and to receive the {@link #gpsTime}
     */
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent) {
        Log.d(TAG, String.format("Intent %s", intent));

        if (intent == null) {
            return;
        }

        stationMMSI = intent.getIntExtra(MMSI_TAG, 0);
        stationLatitude = intent.getDoubleExtra(LATITUDE_TAG, Double.MAX_VALUE);
        stationLongitude = intent.getDoubleExtra(LONGITUDE_TAG, Double.MAX_VALUE);
        timeDiff = intent.getLongExtra(GPS_Service.GPSTime, 0);

        updateStation(stationMMSI, stationLatitude, stationLongitude);
    }

    private void updateStation(final int stationMMSI, final double stationLatitude,
                               final double stationLongitude) {
        Log.d(TAG,
              String.format("Updating MMSI %s, lat %.5f lon %.5f", stationMMSI, stationLatitude,
                            stationLongitude));

        DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        if (!readFromDatabase(db)) {
            return;
        }

        if (stationMMSI == 0) {
            return;
        }
        if (stationLatitude == Double.MAX_VALUE) {
            return;
        }
        if (stationLongitude == Double.MAX_VALUE) {
            return;
        }

        final NavigationFunctions.TransformedCoordinates t = NavigationFunctions
                .transform(originLatitude, originLongitude, stationLatitude, stationLongitude,
                           beta);

        theta = t.getTheta();
        distance = t.getDistance();

        stationX = t.getX();
        stationY = t.getY();

        Log.i(TAG, String.format("Station %s, %.5f,%.5f", stationMMSI, stationLatitude,
                                 stationLongitude));
        Log.i(TAG, String.format("Station %s with Origin %s %.5f,%.5f that is d=%.2f, a=%.2f",
                                 stationMMSI, originMMSI, originLatitude, originLongitude, distance,
                                 alpha));
        Log.i(TAG, String.format("Station %s on grid %.2f, %.2f", stationMMSI, stationX, stationY));

        updateDatabase(stationMMSI, stationX, stationY, alpha, distance);
    }

    /**
     * This method is invoked on the worker thread
     * {@link #timer} is initialized with a Timer task to run every {@value #TIMER_PERIOD} msecs
     * In the task for each mobile station read from the internal local database {@link
     * DatabaseHelper#mobileStationTable}
     * {@link #distance}, {@link #alpha}, {@link #stationX} and {@link #stationY} are calculated and
     * stored
     * in the {@link DatabaseHelper#mobileStationTable} table
     *
     * @param intent Intent
     */
    // FIXME: The alpha calculation needs to be done only when a new mobile station packet is
    //  received. Otherwise the station will wander around,
    //  since its lat/lon location is not predicted it will not follow the drift. With each
    //  received packet it will jump to the correct x,y. And then
    // it will wander off again.
    //    @Override
    //    protected void onHandleIntent(Intent intent) {
    //        if (intent != null) {
    //
    //            Log.d(TAG, "Intent: " + intent.getAction());
    //        }
    //
    //
    //    }

/*
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (!stopTimer) {
                            DatabaseHelper dbHelper =
                                    DatabaseHelper.getDbInstance(getApplicationContext());
                            SQLiteDatabase db = dbHelper.getReadableDatabase();
                            if (readFromDatabase(db)) {
                                if (mobileStationCursor != null &&
                                        mobileStationCursor.moveToFirst()) {
                                    do {
                                        stationLatitude = mobileStationCursor.getDouble(
                                                mobileStationCursor
                                                        .getColumnIndex(DatabaseHelper.latitude));
                                        stationLongitude = mobileStationCursor.getDouble(
                                                mobileStationCursor
                                                        .getColumnIndex(DatabaseHelper.longitude));
                                        stationMMSI = mobileStationCursor.getInt(mobileStationCursor
                                                                                         .getColumnIndex(
                                                                                                 DatabaseHelper.mmsi));
                                        theta = NavigationFunctions
                                                .calculateAngleBeta(originLatitude, originLongitude,
                                                                    stationLatitude,
                                                                    stationLongitude);
                                        //alpha = Math.abs(theta - beta);
                                        alpha = theta - beta;
                                        distance = NavigationFunctions
                                                .calculateDifference(originLatitude,
                                                                     originLongitude,
                                                                     stationLatitude,
                                                                     stationLongitude);
                                        stationX = distance * Math.cos(Math.toRadians(alpha));
                                        stationY = distance * Math.sin(Math.toRadians(alpha));

                                        Log.i(TAG,
                                              String.format("Station %s, %.5f,%.5f", stationMMSI,
                                                            stationLatitude, stationLongitude));
                                        Log.i(TAG, String.format(
                                                "Station %s with Origin %s %.5f,%.5f that is d=%" +
                                                        ".2f, a=%.2f", stationMMSI, originMMSI,
                                                originLatitude, originLongitude, distance, alpha));
                                        Log.i(TAG, String.format("Station %s on grid %.2f, %.2f",
                                                                 stationMMSI, stationX, stationY));

                                        ContentValues alphaUpdate = new ContentValues();
                                        alphaUpdate.put(DatabaseHelper.alpha, alpha);
                                        alphaUpdate.put(DatabaseHelper.distance, distance);
                                        alphaUpdate.put(DatabaseHelper.xPosition, stationX);
                                        alphaUpdate.put(DatabaseHelper.yPosition, stationY);
                                        alphaUpdate.put(DatabaseHelper.updateTime, String.valueOf(
                                                System.currentTimeMillis() - timeDiff));
                                        alphaUpdate.put(DatabaseHelper.isCalculated,
                                                        DatabaseHelper
                                                        .MOBILE_STATION_IS_CALCULATED);
                                        //Log.d(TAG, "MMSI:  " + String.valueOf(stationMMSI) +
                                        // "Alpha " + String.valueOf(alpha)  +  "Distance " +
                                        // String.valueOf(distance));
                                        db.update(DatabaseHelper.mobileStationTable, alphaUpdate,
                                                  DatabaseHelper.mmsi + " = ?",
                                                  new String[]{String.valueOf(stationMMSI)});

                                    } while (mobileStationCursor.moveToNext());
                                    mobileStationCursor.close();
                                } else {
                                    Log.d(TAG, "Error with Mobile Station Cursor");
                                }

                            } else {
                                Log.d(TAG, "Error Reading from Database");
                            }
                        } else {
                            timer.cancel();
                        }
                    } catch (SQLException e) {
                        Log.d(TAG, "Database Error");
                        e.printStackTrace();
                    } finally {
                        if (mobileStationCursor != null) {
                            mobileStationCursor.close();
                        }
                    }
                }
            }, TIMER_DELAY, TIMER_PERIOD);
*/


    /**
     * function called from
     * {@link de.awi.floenavigation.initialsetup.SetupActivity#runServices(Context)}
     * and {@link SyncActivity#stopServices()}
     *
     * @param stop flag to set {@link #stopTimer}
     */
    public static void stopTimer(boolean stop) {
        stopTimer = stop;
    }

    /**
     * @return returns value of {@link #stopTimer}
     */
    public static boolean getStopTimer() {
        return stopTimer;
    }

    /**
     * Reads required parameters of {@link DatabaseHelper#baseStationTable}, {@link
     * DatabaseHelper#fixedStationTable}
     * and {@link DatabaseHelper#betaTable} tables
     *
     * @param db SQLiteDatabase object
     * @return <code>true</code> if successful in reading all the required parameters
     * from the
     * database
     */
    // FIXME: When origin is BASESTN1 or BASESTN2 virtual replacement, x/y needs to be
    //  decoded
    //  based on other base station.
    private boolean readFromDatabase(SQLiteDatabase db) {
        try (Cursor baseStationCursor = db
                .query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi},
                       DatabaseHelper.isOrigin + " = ?",
                       new String[]{String.valueOf(DatabaseHelper.ORIGIN)}, null, null, null)) {

            if (baseStationCursor.moveToFirst()) {
                originMMSI = baseStationCursor
                        .getInt(baseStationCursor.getColumnIndex(DatabaseHelper.mmsi));
            } else {
                Log.d(TAG, "There is no Origin defined in the Database!");
                return false;
            }
        } catch (SQLException e) {
            Log.e(TAG, "Unable to get Origin MMSI from database!", e);
            return false;
        }

        try (Cursor fixedStationCursor = db.query(DatabaseHelper.fixedStationTable,
                                                  new String[]{DatabaseHelper.latitude,
                                                               DatabaseHelper.longitude},
                                                  DatabaseHelper.mmsi + " = ?",
                                                  new String[]{String.valueOf(originMMSI)}, null,
                                                  null, null)) {

            if (fixedStationCursor.moveToFirst()) {
                originLatitude = fixedStationCursor
                        .getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.latitude));
                originLongitude = fixedStationCursor
                        .getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.longitude));
            } else {
                Log.e(TAG, "Unable to retrieve Origin's latitude and longitude from database!");
                return false;
            }
        } catch (SQLException e) {
            Log.e(TAG, "Unable to get Origin data from database!", e);
            return false;
        }

        try (Cursor betaCursor = db.query(DatabaseHelper.betaTable,
                                          new String[]{DatabaseHelper.beta,
                                                       DatabaseHelper.updateTime}, null, null, null,
                                          null, null)) {
            if (betaCursor.moveToFirst()) {
                beta = betaCursor.getDouble(betaCursor.getColumnIndex(DatabaseHelper.beta));
            } else {
                Log.e(TAG, "There is no data in the BETA table!");
                return false;
            }
        } catch (SQLException e) {
            Log.e(TAG, "Unable to get BETA data from database!", e);
            return false;
        }
        return true;
    }


    public void updateDatabase(final int stationMMSI, final double x, final double y,
                               final double alpha, final double distance) {
        DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        ContentValues alphaUpdate = new ContentValues();
        alphaUpdate.put(DatabaseHelper.alpha, alpha);
        alphaUpdate.put(DatabaseHelper.distance, distance);
        alphaUpdate.put(DatabaseHelper.xPosition, x);
        alphaUpdate.put(DatabaseHelper.yPosition, y);
        alphaUpdate.put(DatabaseHelper.updateTime,
                        String.valueOf(System.currentTimeMillis() - timeDiff));
        alphaUpdate.put(DatabaseHelper.isCalculated, DatabaseHelper.MOBILE_STATION_IS_CALCULATED);

        try {
            db.update(DatabaseHelper.mobileStationTable, alphaUpdate, DatabaseHelper.mmsi + " = ?",
                      new String[]{String.valueOf(stationMMSI)});
        } catch (SQLException e) {
            Log.e(TAG, String.format("SQL Error, Updating x,y on %s with x = %.3f, y = %.3f",
                                     stationMMSI, x, y), e);
        }

    }
}
