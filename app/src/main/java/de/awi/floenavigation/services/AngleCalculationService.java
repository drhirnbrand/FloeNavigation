package de.awi.floenavigation.services;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.synchronization.SyncActivity;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * {@link AngleCalculationService} class is used to calculate {@link DatabaseHelper#beta} at regular intervals from all the
 * fixed stations w.r.t the origin and subsequently calculate the corresponding coordinates in the grid
 * </p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class AngleCalculationService extends IntentService {

    private static final String TAG = "AngleCalculationService";
    /**
     * Specifies the number of base stations
     */
    private static final int INITIALIZATION_SIZE = 2;
    /**
     * array to store the station latitudinal position
     */
    private double[] stationLatitude;
    /**
     * array to store the station longitudinal positions
     */
    private double[] stationLongitude;
    /**
     * Angle calculated between the x-axis and the mobile station
     */
    private double alpha;
    /**
     * Variable used to store the value of {@value DatabaseHelper#beta}
     * It is the angle between the x-axis and the geographic longitudinal axis
     */
    private double[] beta;
    /**
     * Handler to execute the runnable
     */
    private final Handler mHandler;
    /**
     * array to store the mmsi's
     */
    private int[] mmsi;
    /**
     * mmsi extracted from the database
     */
    private int mmsiInDBTable;
    /**
     * Timer period to execute the timer task periodically
     */
    private static final int CALCULATION_TIME = 10 * 1000;
    /**
     * Cursor object to iterate over database tables
     */
    private Cursor mBaseStnCursor = null, mFixedStnCursor = null, mBetaCursor = null;
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
     * Not used
     */
    private static AngleCalculationService instance = null;
    /**
     * <code>true</code> to stop the timer
     * <code>false</code> otherwise
     */
    private static boolean stopRunnable = false;

    /**
     * Default constructor
     */
    public AngleCalculationService() {

        super("AngleCalculationService");

        this.mHandler = new Handler();
        mmsi = new int[INITIALIZATION_SIZE];
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent){
                    gpsTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());
                    timeDiff = System.currentTimeMillis() - gpsTime;

                }
            };
        }
    }

    /**
     * onCreate method of the activity lifecycle used to register the {@link #broadcastReceiver}
     * and to receive the {@link #gpsTime}
     */
    @Override
    public void onCreate(){
        super.onCreate();

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
     * Runnable is initialized to run every {@value #CALCULATION_TIME} msecs
     * In the runnable task, base stations which are used for initial grid setup
     * are stored in {@link #mmsi} for further calculations
     *
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        instance = this;
        if (intent != null) {
            Runnable betaRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!stopRunnable) {
                            try {
                                SQLiteOpenHelper databaseHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                                //SQLiteOpenHelper databaseHelper = new DatabaseHelper(getApplicationContext());
                                SQLiteDatabase db = databaseHelper.getReadableDatabase();


                                mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.isOrigin},
                                        null, null, null, null, DatabaseHelper.isOrigin + " DESC");
                                if (mBaseStnCursor.getCount() != DatabaseHelper.NUM_OF_BASE_STATIONS) {
                                    Log.d(TAG, "Error Reading from Base Station Table ");
                                } else {
                                    if (mBaseStnCursor.moveToFirst()) {
                                        int index = 0;
                                        do {
                                            mmsi[index] = mBaseStnCursor.getInt(mBaseStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                                            Log.d(TAG, "MMSI: " + mmsi[index]);
                                            index++;
                                        } while (mBaseStnCursor.moveToNext());
                                        mBaseStnCursor.close();

                                        betaAngleCalculation(db);
                                        //alphaAngleCalculation(db);

                                    }
                                }
                                //db.close();
                                mHandler.postDelayed(this, CALCULATION_TIME);
                            } catch (SQLException e) {
                                String text = "Database unavailable";
                                Log.d(TAG, text);
                            }
                        } else {
                            mHandler.removeCallbacks(this);
                        }
                    }catch (SQLException e){
                        Log.d(TAG, "Database Error");
                        e.printStackTrace();
                    }finally {
                        if (mBaseStnCursor != null){
                            mBaseStnCursor.close();
                        }
                    }
                }
            };


            mHandler.post(betaRunnable);
        }
    }

    /**
     * function called from {@link de.awi.floenavigation.initialsetup.SetupActivity#runServices(Context)}
     * {@link SyncActivity#stopServices()}
     * @param runnable flag to set {@link #stopRunnable}
     */
    public static void setStopRunnable(boolean runnable){
        stopRunnable = runnable;
    }

    /**
     *
     * @return returns value of {@link #stopRunnable}
     */
    public static boolean getStopRunnable(){
        return stopRunnable;
    }

    /**
     * This function is called from {@link #onHandleIntent(Intent)}
     * {@link #beta} angle is calculated from all the fixed stations {@link DatabaseHelper#fixedStationTable} w.r.t the
     * origin fixed station {@link #mmsi} by subtracting {@link #alpha} from angle theta, which is the angle between the axis connecting origin and
     * the fixed station and the longitudinal axis
     * For origin fixed station {@link #beta} is not calculated
     * Each {@link #beta} calculated is stored in each index of {@link #beta}
     * Further the beta angles are averaged and stored in the database {@link DatabaseHelper#betaTable}
     * @param db SQLiteDatabase object
     */
    private void betaAngleCalculation(SQLiteDatabase db){
        //Beta Angle Calculation
        try {

            mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable,
                    new String[]{DatabaseHelper.mmsi, DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.alpha}, null,
                    null, null, null, null);
            long numOfStations = DatabaseUtils.queryNumEntries(db, DatabaseHelper.fixedStationTable);
            stationLatitude = new double[(int) numOfStations];
            stationLongitude = new double[(int) numOfStations];
            beta = new double[(int) numOfStations - 1];
            Log.d(TAG, "Here MMSI First Index:" + mmsi[DatabaseHelper.firstStationIndex]);
            Log.d(TAG, "Here MMSI Second Index:" + mmsi[DatabaseHelper.secondStationIndex]);
            if (mFixedStnCursor.moveToFirst()) {
                int index = 0, betaIndex = 0;
                do {
                    mmsiInDBTable = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                    if (mmsiInDBTable == mmsi[DatabaseHelper.firstStationIndex]) {
                        stationLatitude[DatabaseHelper.firstStationIndex] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                        stationLongitude[DatabaseHelper.firstStationIndex] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                        Log.d(TAG, "Here: Lat1: " + stationLatitude[0] + " Lon1: " + stationLongitude[0]);
                    } else if (mmsiInDBTable == mmsi[DatabaseHelper.secondStationIndex]) {
                        stationLatitude[DatabaseHelper.secondStationIndex] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                        stationLongitude[DatabaseHelper.secondStationIndex] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                        beta[betaIndex] = NavigationFunctions.calculateAngleBeta(stationLatitude[0], stationLongitude[0], stationLatitude[1], stationLongitude[1]);
                        Log.d(TAG, "Lat1: " + stationLatitude[0] + " Lon1: " + stationLongitude[0]);
                        Log.d(TAG, "Lat2: " + stationLatitude[1] + " Lon2: " + stationLongitude[1]);
                        Log.d(TAG, "Beta[" + String.valueOf(betaIndex) + "]" + String.valueOf(beta[betaIndex]));
                        betaIndex++;
                    } else {
                        stationLatitude[index] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                        stationLongitude[index] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                        alpha = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.alpha));
                        double theta = NavigationFunctions.calculateAngleBeta(stationLatitude[0], stationLongitude[0], stationLatitude[index], stationLongitude[index]);
                        //beta[betaIndex] = Math.abs(theta - alpha);
                        Log.d(TAG, "Theta " + betaIndex + ": " + theta);
                        beta[betaIndex] = theta - alpha;
                        Log.d(TAG, "Lat1: " + stationLatitude[0] + " Lon1: " + stationLongitude[0] + " alpha: " + alpha);
                        Log.d(TAG, "Lat2: " + stationLatitude[index] + " Lon2: " + stationLongitude[index] + " alpha: " + alpha);
                        Log.d(TAG, "Beta[" + String.valueOf(betaIndex) + "]" + String.valueOf(beta[betaIndex]));
                        betaIndex++;
                    }
                    index++;
                } while (mFixedStnCursor.moveToNext());

                double avgBetaValue = averageBetaCalculation(beta);
                updateDataintoDatabase(db, avgBetaValue);

                mFixedStnCursor.close();
            }
        }catch (SQLException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }finally {
            if (mFixedStnCursor != null){
                mFixedStnCursor.close();
            }
        }
    }

    /**
     * To calculate average of the values present in {@link #beta}
     * @param beta beta received
     * @return returns the averaged value
     */
    private double averageBetaCalculation(double[] beta){

        double avg_beta;
        double sum = 0;

        for (double aBeta : beta) {
            sum += aBeta;
        }
        avg_beta = sum / beta.length;

        Log.d(TAG, "AvgBeta" + String.valueOf(avg_beta));
        return avg_beta;
    }

    /**
     * Updates the database table {@link DatabaseHelper#betaTable} with the averaged beta value
     * @param db SQLiteDatabase object
     * @param beta averaged beta
     */
    private void updateDataintoDatabase(SQLiteDatabase db, double beta){
        ContentValues mContentValues = new ContentValues();
        mContentValues.put(DatabaseHelper.beta, beta);
        mContentValues.put(DatabaseHelper.updateTime, String.valueOf(System.currentTimeMillis() - timeDiff));
        db.update(DatabaseHelper.betaTable, mContentValues, null, null);
    }

    /**
     * onDestroy method of the activity life cycle used to unregister the {@link #broadcastReceiver}
     */
    @Override
    public void onDestroy(){
        super.onDestroy();
        instance = null;
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }


}
