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
import android.os.Handler;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.NavigationFunctions;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 *     {@link PredictionService} service is used to predict the future positions of all the fixed stations at a
 *     specified time interval
 *     Since the AIS packets are received every 3 minutes, it is desirable and necessary to predict the positions of the
 *     stations between these intervals at a higher rate, which helps the grid to show and monitor the fixed stations
 *     in a continuous manner.
 *     <p>
 *         Also {@link ValidationService} service takes use of the predicted positions by comparing it with
 *         received values to verify sea ice break scenario
 *     </p>
 * </p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class PredictionService extends IntentService {

    private final static String TAG = PredictionService.class.getSimpleName();
    /**
     * Handler to execute the runnable
     */
    private final Handler mPredictionHandler;
    /**
     * Timer period to execute the prediction service periodically
     */
    private final int PREDICTION_TIME = 10 * 1000;
    /**
     * Timer period to execute the validation service periodically
     */
    private static final int VALIDATION_TIME = 3 * 60 * 1000;
    /**
     * The threshold distance/difference between the received and the predicted location
     */
    public static int ERROR_THRESHOLD_VALUE;
    /**
     * The threshold time after the distance goes beyond {@link #ERROR_THRESHOLD_VALUE}
     */
    public static int PREDICTION_ACCURACY_THRESHOLD_VALUE;

    /**
     * distance calculated between the origin fixed station and any fixed station in meters
     */
    private double distance;
    /**
     * X axis value in meters of the fixed station
     */
    private double xPosition;
    /**
     * Y axis value in meters of the fixed station
     */
    private double yPosition;
    /**
     * Angle calculated between the x-axis and the fixed station
     */
    private double alpha;
    /**
     * Angle calculated between the axis connecting origin fixed station and the longitudinal axis and the waypoint
     */
    private double theta;
    /**
     * Origin fixed station latitude value
     */
    private double originLatitude;
    /**
     * Origin fixed station longitude value
     */
    private double originLongitude;
    /**
     * Origin MMSI
     */
    private int originMMSI;
    /**
     * X-axis fixed station MMSI
     */
    private int xAxisBaseStationMMSI;
    /**
     * Variable used to store the value of {@value DatabaseHelper#beta}
     * It is the angle between the x-axis and the geographic longitudinal axis
     */
    private double beta;

    /**
     * Time at which the position was last predicted
     */
    private double predictionTime;

    /**
     * Time at which the AIS packet was last received
     */
    private double updateTime;

    /**
     * Not used
     */
    private static PredictionService instance = null;

    /**
     * <code>true</code> to stop the runnable
     * <code>false</code> otherwise
     */
    private static boolean stopRunnable = false;
    /**
     * Broadcast receiver to get gps time
     */
    private BroadcastReceiver broadcastReceiver;
    /**
     * Variable to store the gps time
     */
    private long gpsTime;
    /**
     * Variable to store the time difference between the system clock and the gps time
     */
    private long timeDiff;

    /**
     * Default constructor
     * Initializes the Handler {@link #mPredictionHandler}
     */
    public PredictionService() {
        super("PredictionService");
        mPredictionHandler = new Handler();
    }

    @Override
    public void onCreate(){
        super.onCreate();
        instance = this;
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    gpsTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());
                    timeDiff = System.currentTimeMillis() - gpsTime;

                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));
    }

    /**
     * This method is invoked on the worker thread
     * Runnable is initialized to run every {@link #PREDICTION_TIME} msecs
     * Fixed stations are retrieved from the local database and new positions are calculated using {@link NavigationFunctions#calculateNewPosition(double, double, double, double)}.
     * If the origin and the x-axis fixed station are already broken off, instead of taking the received latitude and longitude values, previous predicted values
     * are taken to predict the future positions.
     * After the new positions are predicted the updated values are stored back to the corresponding columns of the fixed station table {@link DatabaseHelper#fixedStationTable}
     * in the local database.
     * if {@link #stopRunnable} is true, the runnable is stopped until {@link #setStopRunnable(boolean)} with false value is not received
     * from {@link de.awi.floenavigation.synchronization.SyncActivity#stopServices} and {@link de.awi.floenavigation.initialsetup.SetupActivity#runServices}
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

                Runnable predictionRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if(!stopRunnable) {
                            Cursor mFixedStnCursor = null;
                            try{
                                DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                                SQLiteDatabase db = dbHelper.getReadableDatabase();
                                if(getOriginCoordinates(db)) {
                                    double stationLatitude, stationLongitude, stationSOG, stationCOG;
                                    double[] predictedCoordinate;
                                    int mmsi;
                                    int predictionAccuracy;
                                    retrieveConfigurationParametersDatafromDB(db);

                                    mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.latitude, DatabaseHelper.longitude,
                                            DatabaseHelper.recvdLatitude, DatabaseHelper.recvdLongitude, DatabaseHelper.sog,
                                            DatabaseHelper.cog, DatabaseHelper.predictionTime, DatabaseHelper.updateTime, DatabaseHelper.predictionAccuracy},
                                            null,
                                            null, null, null, null);
                                    if (mFixedStnCursor.moveToFirst()) {
                                        do {
                                            mmsi = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                                            updateTime = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndexOrThrow(DatabaseHelper.updateTime));
                                            predictionTime = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndexOrThrow(DatabaseHelper.predictionTime));
//                                            predictionAccuracy = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.predictionAccuracy));
//                                            if (predictionAccuracy >= PREDICTION_ACCURACY_THRESHOLD_VALUE / VALIDATION_TIME) {
//                                                stationLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
//                                                stationLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
//                                            } else {
                                                if (updateTime > predictionTime) {
                                                    stationLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLatitude));
                                                    stationLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLongitude));
                                                } else {
                                                    stationLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                                                    stationLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                                                }
//                                            }
                                            stationSOG = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.sog));
                                            stationCOG = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.cog));
                                            calculateNewParams(mmsi, stationLatitude, stationLongitude);
                                            predictedCoordinate = NavigationFunctions.calculateNewPosition(stationLatitude, stationLongitude, stationSOG, stationCOG);
                                            ContentValues mContentValues = new ContentValues();
                                            mContentValues.put(DatabaseHelper.latitude, predictedCoordinate[DatabaseHelper.LATITUDE_INDEX]);
                                            mContentValues.put(DatabaseHelper.longitude, predictedCoordinate[DatabaseHelper.LONGITUDE_INDEX]);
                                            mContentValues.put(DatabaseHelper.xPosition, xPosition);
                                            mContentValues.put(DatabaseHelper.yPosition, yPosition);
                                            mContentValues.put(DatabaseHelper.distance, distance);
                                            mContentValues.put(DatabaseHelper.predictionTime, System.currentTimeMillis() - timeDiff);
                                            mContentValues.put(DatabaseHelper.alpha, alpha);
                                            mContentValues.put(DatabaseHelper.isPredicted, 1);
                                            db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
                                            Log.d(TAG, String.format("Station %s:  %.5f,%.5f", mmsi, stationLatitude, stationLongitude));
                                            Log.d(TAG, String.format("Station %s: Predicted %.5f,%.5f (%.3f,%.3f)", mmsi, predictedCoordinate[0] , predictedCoordinate[1], xPosition, yPosition));
                                        } while (mFixedStnCursor.moveToNext());
                                        mFixedStnCursor.close();
                                    } else {
                                        Log.d(TAG, "FixedStationTable Cursor Error");
                                    }
                                } else{
                                    Log.d(TAG, "Error Reading Origin Coordinates");
                                }

                                    mPredictionHandler.postDelayed(this, PREDICTION_TIME);

                            }catch (SQLException e){
                                String text = "Database unavailable";
                                Log.d(TAG, text);
                            }finally {
                                if (mFixedStnCursor != null){
                                    mFixedStnCursor.close();
                                }
                            }
                        } else{
                            mPredictionHandler.removeCallbacks(this);
                        }
                    }

                };
            mPredictionHandler.post(predictionRunnable);
        }
    }

    /**
     * To set the value of {@link #stopRunnable}
     * @param stop stop flag
     */
    public static void setStopRunnable(boolean stop){
        stopRunnable = stop;
    }

    /**
     * Getter function
     * @return returns the value of {@link #stopRunnable}
     */
    public static boolean getStopRunnable(){
        return stopRunnable;
    }

    /**
     * Function used to retrieve the values of {@link #ERROR_THRESHOLD_VALUE} and {@link #PREDICTION_ACCURACY_THRESHOLD_VALUE}
     * from the database table {@link DatabaseHelper#configParametersTable}
     * @param db SQLiteDatabase object
     */
    private void retrieveConfigurationParametersDatafromDB(SQLiteDatabase db){
        Cursor configParamCursor = null;
        try{
            configParamCursor = db.query(DatabaseHelper.configParametersTable, null, null,
                    null, null, null, null);
            String parameterName = null;
            int parameterValue = 0;

            if (configParamCursor.moveToFirst()){
                do{
                    parameterName = configParamCursor.getString(configParamCursor.getColumnIndex(DatabaseHelper.parameterName));
                    parameterValue = configParamCursor.getInt(configParamCursor.getColumnIndex(DatabaseHelper.parameterValue));

                    switch (parameterName) {
                        case DatabaseHelper.error_threshold:
                            ERROR_THRESHOLD_VALUE = parameterValue;
                            break;
                        case DatabaseHelper.prediction_accuracy_threshold:
                            PREDICTION_ACCURACY_THRESHOLD_VALUE = parameterValue;
                            break;
                    }
                }while (configParamCursor.moveToNext());
            }else {
                Log.d(TAG, "Config Parameter table cursor error");
            }
            configParamCursor.close();
        }catch (SQLException e){

            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        }finally {
            if (configParamCursor != null){
                configParamCursor.close();
            }
        }
    }

    /**
     * Calculates the {@link #xPosition}, {@link #yPosition}, {@link #alpha} and {@link #distance}
     * for the new predicted location of the fixed station
     * @param mmsi mmsi number of the fixed station
     * @param latitude predicted latitude value
     * @param longitude predicted longitude value
     */
    private void calculateNewParams(int mmsi, double latitude, double longitude ){
        if(mmsi == originMMSI){
            xPosition = 0.0;
            yPosition = 0.0;
            alpha = 0.0;
            distance = 0.0;
            Log.d(TAG, String.format("Origin %s %.5f/%.5f a=%.2f d=%.2f",originMMSI, latitude, longitude, alpha, distance));
        } else if(mmsi == xAxisBaseStationMMSI){
            xPosition = NavigationFunctions.calculateDifference(originLatitude, originLongitude, latitude, longitude);
            Log.d(TAG, "OL: " + originLatitude + ", " + originLongitude + " XL: " + latitude + ", " + longitude);
            yPosition = 0.0;
            alpha = 0.0;
            distance = xPosition;
            Log.d(TAG, String.format("Axis %s %.5f/%.5f a=%.2f d=%.2f",xAxisBaseStationMMSI, latitude, longitude, alpha, distance));
        } else {
            distance = NavigationFunctions.calculateDifference(originLatitude, originLongitude, latitude, longitude);
            theta = NavigationFunctions.calculateAngleBeta(originLatitude, originLongitude, latitude, longitude);
            //alpha = Math.abs(theta - beta);
            alpha = theta - beta;
            xPosition = distance * Math.cos(Math.toRadians(alpha));
            yPosition = distance * Math.sin(Math.toRadians(alpha));
        }
    }

    /**
     * Function to retrieve the origin fixed station coordinates
     * @param db SQLiteDatabase object
     * @return returns <code>true</code>, if retrieval is successful
     *                 <code>false</code>, otherwise
     */
    private boolean getOriginCoordinates(SQLiteDatabase db){
        Cursor baseStationCursor = null;
        Cursor betaCursor = null;
        Cursor fixedStationCursor = null;
        try {

            baseStationCursor = db.query(DatabaseHelper.baseStationTable,
                    new String[] {DatabaseHelper.mmsi, DatabaseHelper.isOrigin},
                     null,
                    null,
                    null, null, null);
            if (baseStationCursor.getCount() != DatabaseHelper.INITIALIZATION_SIZE){
                Log.d(TAG, "Error Reading from BaseStation Table");
                return false;
            } else{
                if(baseStationCursor.moveToFirst()){
                    do {
                        int isOrigin = baseStationCursor.getInt(baseStationCursor.getColumnIndexOrThrow(DatabaseHelper.isOrigin));
                        if(isOrigin == 1) {
                            originMMSI = baseStationCursor.getInt(baseStationCursor.getColumnIndex(DatabaseHelper.mmsi));
                            Log.d(TAG, " OriginMMSI " + String.valueOf(originMMSI));
                        } else if(isOrigin == 0){
                            xAxisBaseStationMMSI = baseStationCursor.getInt(baseStationCursor.getColumnIndex(DatabaseHelper.mmsi));
                            Log.d(TAG, " xAxisBaseStationMMSI " + String.valueOf(xAxisBaseStationMMSI));
                        } else{
                            Log.d(TAG, " Error Reading Base Stations. isOrigin Value: " + String.valueOf(isOrigin));
                        }
                    } while (baseStationCursor.moveToNext());
                }
            }
            fixedStationCursor = db.query(DatabaseHelper.fixedStationTable,
                    new String[] {DatabaseHelper.latitude, DatabaseHelper.longitude},
                    DatabaseHelper.mmsi +" = ?",
                    new String[] {String.valueOf(originMMSI)},
                    null, null, null);
            if (fixedStationCursor.getCount() != 1){
                Log.d(TAG, "Error Reading Origin Latitude Longitude");
                return false;
            } else{
                if(fixedStationCursor.moveToFirst()){
                    originLatitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.latitude));
                    originLongitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.longitude));
                }
            }

            betaCursor = db.query(DatabaseHelper.betaTable,
                    new String[]{DatabaseHelper.beta, DatabaseHelper.updateTime},
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
        } catch(SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
            return false;
        }finally {
            if (baseStationCursor != null){
                baseStationCursor.close();
            }
            if (betaCursor != null){
                betaCursor.close();
            }
            if (fixedStationCursor != null){
                fixedStationCursor.close();
            }
        }
    }

    /**
     * onDestroy method, part of service lifecycle
     */
    @Override
    public void onDestroy(){
        super.onDestroy();
        instance = null;
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }
}
