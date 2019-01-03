package de.awi.floenavigation.services;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.NavigationFunctions;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class AlphaCalculationService extends IntentService {

    private static final String TAG = "AlphaCalculationService";
    private double beta;
    private int originMMSI;
    private int stationMMSI;
    private double originLatitude;
    private double originLongitude;
    private double stationLatitude;
    private double stationLongitude;
    private double distance;
    private double stationX;
    private double stationY;
    private double theta;
    private double alpha;
    private Cursor mobileStationCursor;
    Timer timer = new Timer();
    private static final int TIMER_PERIOD = 10 * 1000;
    private static final int TIMER_DELAY = 0;

    private BroadcastReceiver broadcastReceiver;
    private long gpsTime;
    private long timeDiff;

    private static boolean stopTimer  = false;

    private static AlphaCalculationService instance = null;


    public AlphaCalculationService() {
        super("AlphaCalculationService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }

    }

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

    public static boolean isInstanceCreated(){
        return instance != null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null){

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(!stopTimer) {
                        DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                        SQLiteDatabase db = dbHelper.getReadableDatabase();
                        if (readFromDatabase(db)) {
                            if (mobileStationCursor.moveToFirst()) {
                                do {
                                    stationLatitude = mobileStationCursor.getDouble(mobileStationCursor.getColumnIndex(DatabaseHelper.latitude));
                                    stationLongitude = mobileStationCursor.getDouble(mobileStationCursor.getColumnIndex(DatabaseHelper.longitude));
                                    stationMMSI = mobileStationCursor.getInt(mobileStationCursor.getColumnIndex(DatabaseHelper.mmsi));
                                    theta = NavigationFunctions.calculateAngleBeta(originLatitude, originLongitude, stationLatitude, stationLongitude);
                                    alpha = Math.abs(theta - beta);
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
                                    //Log.d(TAG, "MMSI:  " + String.valueOf(stationMMSI) +  "Alpha " + String.valueOf(alpha)  +  "Distance " + String.valueOf(distance));
                                    db.update(DatabaseHelper.mobileStationTable, alphaUpdate, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(stationMMSI)});

                                } while (mobileStationCursor.moveToNext());
                                mobileStationCursor.close();
                            } else {
                                Log.d(TAG, "Error with Mobile Station Cursor");
                            }

                        } else {
                            Log.d(TAG, "Error Reading from Database");
                        }
                    } else{
                        timer.cancel();
                    }
                }
            }, TIMER_DELAY, TIMER_PERIOD);

        }


    }

    public static void stopTimer(boolean stop){
        stopTimer = stop;
    }

    public static boolean getStopTimer(){
        return stopTimer;
    }

    private boolean readFromDatabase(SQLiteDatabase db){
        try {
            Cursor baseStationCursor = db.query(DatabaseHelper.baseStationTable,
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
            Cursor fixedStationCursor = db.query(DatabaseHelper.fixedStationTable,
                    new String[] {DatabaseHelper.latitude, DatabaseHelper.longitude},
                    DatabaseHelper.mmsi +" = ?",
                    new String[] {String.valueOf(originMMSI)},
                    null, null, null);
            if (fixedStationCursor.getCount() != 1){
                Log.d(TAG, "Error Reading Origin Latitude Longtidue");
                return false;
            } else{
                if(fixedStationCursor.moveToFirst()){
                    originLatitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.latitude));
                    originLongitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.longitude));
                }
            }
            Cursor betaCursor = db.query(DatabaseHelper.betaTable,
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
        }
    }

    private class ReadfromDB extends AsyncTask<Void, Void, Boolean>{

        double betaValue;

        @Override
        protected void onPreExecute(){

        }

        protected Boolean doInBackground(Void...voids){

            DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            try{
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor betaCursor = db.query(DatabaseHelper.betaTable,
                        new String[] {DatabaseHelper.beta, DatabaseHelper.updateTime},
                        null, null,
                        null, null, null);
                Cursor mobileStationCursor = db.query(DatabaseHelper.mobileStationTable,
                        new String[] {DatabaseHelper.mmsi, DatabaseHelper.latitude, DatabaseHelper.longitude},
                        null, null,
                        null, null, null);
                if(betaCursor.getCount() == 1) {
                    if (betaCursor.moveToFirst()) {
                        betaValue = betaCursor.getDouble(betaCursor.getColumnIndex(DatabaseHelper.beta));

                    }
                    betaCursor.close();
                    return true;
                } else {
                    Log.d(TAG, "Error in Beta Table");
                    return false;
                }
            } catch(SQLiteException e){
                e.printStackTrace();
                return false;
            }

        }

        @Override
        protected void onPostExecute(Boolean result){
            if (!result){
                Log.d(TAG, "Database Unavailable");
            } else{
                beta = betaValue;
            }
        }
    }

}
