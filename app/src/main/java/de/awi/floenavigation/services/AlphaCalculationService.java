package de.awi.floenavigation.services;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.synchronization.SyncActivity;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * {@link AlphaCalculationService} class is used to calculate {@link DatabaseHelper#alpha} at regular intervals for all the
 * mobile stations and subsequently calculate the corresponding coordinates in the grid
 * </p>
 *
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class AlphaCalculationService extends IntentService {

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
     * Mobile station cursor to iterate over the rows of the database table {@link DatabaseHelper#mobileStationTable}
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
     * Stores the timing difference between {@link System#currentTimeMillis()} and {@link GPS_Service#GPSTime}
     */
    private long timeDiff;

    /**
     * <code>true</code> to stop the timer
     * <code>false</code> otherwise
     */
    private static boolean stopTimer  = false;

    /**
     * Not used - only for debugging
     */
    private static AlphaCalculationService instance = null;

    /**
     * Time at which the position was last predicted
     */
    private double predictionTime;

    /**
     * Time at which the AIS packet was last received
     */
    private double updateTime;

    /**
     * Default constructor
     */
    public AlphaCalculationService() {
        super("AlphaCalculationService");
    }

    /**
     * onDestroy method of the activity life cycle used to unregister the {@link #broadcastReceiver}
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }

    }

    /**
     * onCreate method of the activity lifecycle used to register the {@link #broadcastReceiver}
     * and to receive the {@link #gpsTime}
     */
    @Override
    public void onCreate(){
        super.onCreate();
        instance = this;
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent){
                    gpsTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());
                    timeDiff = System.currentTimeMillis() - gpsTime;

                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));
    }

    /**
     * This method is invoked on the worker thread
     * {@link #timer} is initialized with a Timer task to run every {@value #TIMER_PERIOD} msecs
     * In the task for each mobile station read from the internal local database {@link DatabaseHelper#mobileStationTable}
     * {@link #distance}, {@link #alpha}, {@link #stationX} and {@link #stationY} are calculated and stored
     * in the {@link DatabaseHelper#mobileStationTable} table
     *
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null){

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (!stopTimer) {
                            DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                            SQLiteDatabase db = dbHelper.getReadableDatabase();
                            if (readFromDatabase(db)) {
                                if (mobileStationCursor != null && mobileStationCursor.moveToFirst()) {
                                    do {
                                        stationLatitude = mobileStationCursor.getDouble(mobileStationCursor.getColumnIndex(DatabaseHelper.latitude));
                                        stationLongitude = mobileStationCursor.getDouble(mobileStationCursor.getColumnIndex(DatabaseHelper.longitude));
                                        Log.d(TAG, "MMSI: " + String.valueOf(stationMMSI) + " Mobile Coord: " + String.valueOf(stationLatitude) + "," + String.valueOf(stationLongitude));
                                        stationMMSI = mobileStationCursor.getInt(mobileStationCursor.getColumnIndex(DatabaseHelper.mmsi));
                                        theta = NavigationFunctions.calculateAngleBeta(originLatitude, originLongitude, stationLatitude, stationLongitude);
                                        //alpha = Math.abs(theta - beta);
                                        alpha = theta - beta;
                                        distance = NavigationFunctions.calculateDifference(originLatitude, originLongitude, stationLatitude, stationLongitude);
                                        stationX = distance * Math.cos(Math.toRadians(alpha));
                                        stationY = distance * Math.sin(Math.toRadians(alpha));
                                        ContentValues alphaUpdate = new ContentValues();
                                        alphaUpdate.put(DatabaseHelper.alpha, alpha);
                                        alphaUpdate.put(DatabaseHelper.distance, distance);
                                        alphaUpdate.put(DatabaseHelper.xPosition, stationX);
                                        alphaUpdate.put(DatabaseHelper.yPosition, stationY);
                                        alphaUpdate.put(DatabaseHelper.updateTime, String.valueOf(System.currentTimeMillis() - timeDiff));
                                        alphaUpdate.put(DatabaseHelper.isCalculated, DatabaseHelper.MOBILE_STATION_IS_CALCULATED);
                                        Log.d(TAG, "MMSI:  " + String.valueOf(stationMMSI) +  "Alpha " + String.valueOf(alpha)  +  "Distance " + String.valueOf(distance)
                                        + "Theta " + String.valueOf(theta));
                                        db.update(DatabaseHelper.mobileStationTable, alphaUpdate, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(stationMMSI)});

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
                    }catch (SQLException e){
                        Log.d(TAG, "Database Error");
                        e.printStackTrace();
                    }finally {
                        if (mobileStationCursor != null){
                            mobileStationCursor.close();
                        }
                    }
                }
            }, TIMER_DELAY, TIMER_PERIOD);

        }


    }

    /**
     * function called from {@link de.awi.floenavigation.initialsetup.SetupActivity#runServices(Context)}
     * and {@link SyncActivity#stopServices()}
     * @param stop flag to set {@link #stopTimer}
     */
    public static void stopTimer(boolean stop){
        stopTimer = stop;
    }

    /**
     *
     * @return returns value of {@link #stopTimer}
     */
    public static boolean getStopTimer(){
        return stopTimer;
    }

    /**
     * Reads required parameters of {@link DatabaseHelper#baseStationTable}, {@link DatabaseHelper#fixedStationTable}
     * and {@link DatabaseHelper#betaTable} tables
     * @param db SQLiteDatabase object
     * @return <code>true</code> if successful in reading all the required parameters from the database
     */
    private boolean readFromDatabase(SQLiteDatabase db){
        Cursor baseStationCursor = null;
        Cursor fixedStationCursor = null;
        Cursor betaCursor = null;
        try {
            baseStationCursor = db.query(DatabaseHelper.baseStationTable,
                    new String[] {DatabaseHelper.mmsi},
                    DatabaseHelper.isOrigin +" = ?",
                    new String[]{String.valueOf(DatabaseHelper.ORIGIN)},
                    null, null, null);
            if (baseStationCursor.getCount() != 1){
                Log.d(TAG, "Error Reading from BaseStation Table");
                return false;
            } else{
                if(baseStationCursor.moveToFirst()){
                    originMMSI = baseStationCursor.getInt(baseStationCursor.getColumnIndex(DatabaseHelper.mmsi));
                }
            }
            fixedStationCursor = db.query(DatabaseHelper.fixedStationTable,
                    new String[] {DatabaseHelper.latitude, DatabaseHelper.longitude,
                            DatabaseHelper.recvdLatitude, DatabaseHelper.recvdLongitude, DatabaseHelper.predictionTime, DatabaseHelper.updateTime},
                    DatabaseHelper.mmsi +" = ? AND " + DatabaseHelper.isLocationReceived + " = ?",
                    new String[] {String.valueOf(originMMSI), String.valueOf(DatabaseHelper.LOCATIONRECEIVED)},
                    null, null, null);
            if (fixedStationCursor.getCount() != 1){
                Log.d(TAG, "Error Reading Origin Latitude Longtidue");
                return false;
            } else{
                if(fixedStationCursor.moveToFirst()){
                    updateTime = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.updateTime));
                    predictionTime = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.predictionTime));
                    if (updateTime >= predictionTime) {
                        originLatitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.recvdLatitude));
                        originLongitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.recvdLongitude));
                    }else {
                        originLatitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.latitude));
                        originLongitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.longitude));
                    }
                }
            }
            betaCursor = db.query(DatabaseHelper.betaTable,
                    new String[]{DatabaseHelper.beta, DatabaseHelper.updateTime},
                    null, null,
                    null, null, null);
            mobileStationCursor = db.query(DatabaseHelper.mobileStationTable,
                    new String[]{DatabaseHelper.mmsi, DatabaseHelper.latitude, DatabaseHelper.longitude},
                    null, null,
                    null, null, null);
            if (betaCursor.getCount() == 1) {
                if (betaCursor.moveToFirst()) {
                    beta = betaCursor.getDouble(betaCursor.getColumnIndex(DatabaseHelper.beta));
                }

            } else {
                Log.d(TAG, "Error in Beta Table");
                return false;
            }
            betaCursor.close();
            baseStationCursor.close();
            fixedStationCursor.close();
            return true;
        } catch (SQLiteException e){
            e.printStackTrace();
            Log.d(TAG, "Error reading Database");
            return false;
        }finally {
            if (baseStationCursor != null){
                baseStationCursor.close();
            }
            if (fixedStationCursor != null){
                fixedStationCursor.close();
            }
            if (betaCursor != null){
                betaCursor.close();
            }
        }
    }
}
