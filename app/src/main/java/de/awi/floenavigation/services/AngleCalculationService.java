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


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class AngleCalculationService extends IntentService {

    private static final String TAG = "AngleCalculationService";
    private static final int INITIALIZATION_SIZE = 2;
    private double[] stationLatitude;
    private double[] stationLongitude;
    private double alpha;
    private double[] beta;
    private final Handler mHandler;
    private int[] mmsi;
    private int mmsiInDBTable;
    private static final int CALCULATION_TIME = 10 * 1000;
    private Cursor mBaseStnCursor, mFixedStnCursor, mBetaCursor;
    private BroadcastReceiver broadcastReceiver;
    private long gpsTime;
    private long timeDiff;

    private static AngleCalculationService instance = null;
    private static boolean stopRunnable = false;



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

    public static boolean isInstanceCreated(){
        return instance != null;
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        instance = this;
        if (intent != null) {
            Runnable betaRunnable = new Runnable() {
                @Override
                public void run() {
                    if(!stopRunnable) {
                        try {
                            SQLiteOpenHelper databaseHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                            //SQLiteOpenHelper databaseHelper = new DatabaseHelper(getApplicationContext());
                            SQLiteDatabase db = databaseHelper.getReadableDatabase();


                            mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[] {DatabaseHelper.mmsi}, null,
                                    null, null, null, null);
                            if (mBaseStnCursor.getCount() != DatabaseHelper.NUM_OF_BASE_STATIONS){
                                Log.d(TAG, "Error Reading from Base Station Table ");
                            } else {
                                if (mBaseStnCursor.moveToFirst()) {
                                    int index = 0;
                                    do {
                                        mmsi[index] = mBaseStnCursor.getInt(mBaseStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                                        index++;
                                    } while (mBaseStnCursor.moveToNext());
                                    mBaseStnCursor.close();

                                    betaAngleCalculation(db);
                                    //alphaAngleCalculation(db);

                                }
                            }
                            //db.close();
                            mHandler.postDelayed(this, CALCULATION_TIME);
                        }catch (SQLException e){
                            String text = "Database unavailable";
                            Log.d(TAG, text);
                        }
                    }
                    else{
                        mHandler.removeCallbacks(this);
                    }
                }
            };


            mHandler.post(betaRunnable);
        }
    }

    public static void setStopRunnable(boolean runnable){
        stopRunnable = runnable;
    }

    public static boolean getStopRunnable(){
        return stopRunnable;
    }

    private void betaAngleCalculation(SQLiteDatabase db){
        //Beta Angle Calculation
        mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable,
                new String[]{DatabaseHelper.mmsi, DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.alpha}, null,
                null, null, null, null);
        long numOfStations = DatabaseUtils.queryNumEntries(db, DatabaseHelper.fixedStationTable);
        stationLatitude = new double[(int) numOfStations];
        stationLongitude = new double[(int) numOfStations];
        beta = new double[(int) numOfStations - 1];
        if (mFixedStnCursor.moveToFirst()) {
            int index = 0, betaIndex = 0;
            do {
                mmsiInDBTable = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                if (mmsiInDBTable == mmsi[DatabaseHelper.firstStationIndex]){
                    stationLatitude[DatabaseHelper.firstStationIndex] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                    stationLongitude[DatabaseHelper.firstStationIndex] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                }else if (mmsiInDBTable == mmsi[DatabaseHelper.secondStationIndex]){
                    stationLatitude[DatabaseHelper.secondStationIndex] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                    stationLongitude[DatabaseHelper.secondStationIndex] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                    beta[betaIndex] = NavigationFunctions.calculateAngleBeta(stationLatitude[0], stationLongitude[0], stationLatitude[1], stationLongitude[1]);
                    //Log.d(TAG, "Lat1: " + stationLatitude[0] + " Lon1: " + stationLongitude[0]);
                    //Log.d(TAG, "Lat2: " + stationLatitude[1] + " Lon2: " + stationLongitude[1]);
                    Log.d(TAG, "Beta[" + String.valueOf(betaIndex) + "]" + String.valueOf(beta[betaIndex]));
                    betaIndex++;
                }else {
                    stationLatitude[index] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                    stationLongitude[index] = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                    alpha = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.alpha));
                    double theta = NavigationFunctions.calculateAngleBeta(stationLatitude[0], stationLongitude[0], stationLatitude[index], stationLongitude[index]);
                    beta[betaIndex] = Math.abs(theta - alpha);
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
    }

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

    private void updateDataintoDatabase(SQLiteDatabase db, double beta){
        ContentValues mContentValues = new ContentValues();
        mContentValues.put(DatabaseHelper.beta, beta);
        mContentValues.put(DatabaseHelper.updateTime, String.valueOf(System.currentTimeMillis() - timeDiff));
        db.update(DatabaseHelper.betaTable, mContentValues, null, null);
    }

    private void alphaAngleCalculation(SQLiteDatabase db){
        //Alpha Angle Calculation
        long numOfEntries = DatabaseUtils.queryNumEntries(db, DatabaseHelper.fixedStationTable);
        if (numOfEntries > DatabaseHelper.NUM_OF_BASE_STATIONS) {
            //Alpha angle calculation
            double fixedStationLatitude;
            double fixedStationLongitude;
            double fixedStationBeta;
            int fixedStationMMSI;
            ContentValues mContentValues = new ContentValues(); //for updating alpha value

            mBetaCursor = db.query(DatabaseHelper.betaTable, new String[]{DatabaseHelper.beta, DatabaseHelper.updateTime},
                    null, null, null, null, null);


            //Log.d(TAG, String.valueOf(mBetaCursor.getDouble(mBetaCursor.getColumnIndex(DatabaseHelper.beta))));
            if (mBetaCursor.getCount() == 1){
                if (mBetaCursor.moveToFirst()) {
                    fixedStationBeta = mBetaCursor.getDouble(mBetaCursor.getColumnIndex(DatabaseHelper.beta));

                    mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable,
                            new String[]{DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.mmsi}, DatabaseHelper.mmsi + " != ? AND " + DatabaseHelper.mmsi + " != ?",
                            new String[]{Integer.toString(mmsi[0]), Integer.toString(mmsi[1])},
                            null, null, null);
                    Log.d(TAG, String.valueOf(mFixedStnCursor.getCount()));
                    if (mFixedStnCursor.moveToFirst()) {
                        do {
                            fixedStationLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                            fixedStationLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                            fixedStationMMSI = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                            double theta = NavigationFunctions.calculateAngleBeta(stationLatitude[0], stationLongitude[0], fixedStationLatitude, fixedStationLongitude);
                            double alpha = Math.abs(theta - fixedStationBeta);
                            double distance = NavigationFunctions.calculateDifference(stationLatitude[0], stationLongitude[0], fixedStationLatitude, fixedStationLongitude);
                            double stationX = distance * Math.cos(Math.toRadians(alpha));
                            double stationY = distance * Math.sin(Math.toRadians(alpha));
                            Log.d(TAG, "mmsi: " + String.valueOf(fixedStationMMSI) + " Alpha: " + String.valueOf(alpha));
                            //Log.d(TAG, "stationX: " + stationX + "stationY: " + stationY);
                            mContentValues.put(DatabaseHelper.alpha, alpha);
                            mContentValues.put(DatabaseHelper.xPosition, stationX);
                            mContentValues.put(DatabaseHelper.yPosition, stationY);
                            db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(fixedStationMMSI)});
                        } while (mFixedStnCursor.moveToNext());
                        mFixedStnCursor.close();
                    }else {
                        Log.d(TAG, "Error mFixedStnCursor");
                    }
                    mBetaCursor.close();
                }else {
                    Log.d(TAG, "Error mBetaCursor");
                }
            }else{
                Log.d(TAG, "Error reading from Beta Table");
            }
        } else {
            Log.d(TAG, "No New Stations Deployed");
        }
    }

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
