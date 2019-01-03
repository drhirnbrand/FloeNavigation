package de.awi.floenavigation.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
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
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class PredictionService extends IntentService {

    private final static String TAG = "PREDICTION_SERVICE: ";
    private final Handler mPredictionHandler;
    private final int PREDICTION_TIME = 10 * 1000;
    private static final int VALIDATION_TIME = 3 * 60 * 1000;
    public static int ERROR_THRESHOLD_VALUE;
    public static int PREDICTION_ACCURACY_THRESHOLD_VALUE;

    private double distance;
    private double xPosition;
    private double yPosition;
    private double alpha;
    private double theta;
    private double originLatitude;
    private double originLongitude;
    private int originMMSI;
    private int xAxisBaseStationMMSI;
    private double beta;

    private static PredictionService instance = null;

    private static boolean stopRunnable = false;

    public PredictionService() {
        super("PredictionService");
        mPredictionHandler = new Handler();
    }

    @Override
    public void onCreate(){
        super.onCreate();
        instance = this;
    }

    public static boolean isInstanceCreated(){
        return instance != null;
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

                Runnable predictionRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if(!stopRunnable) {
                            try{
                                DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                                SQLiteDatabase db = dbHelper.getReadableDatabase();
                                if(getOriginCoordinates(db)) {
                                    Cursor mFixedStnCursor;
                                    double stationLatitude, stationLongitude, stationSOG, stationCOG;
                                    double[] predictedCoordinate;
                                    int mmsi;
                                    int predictionAccuracy;
                                    retrieveConfigurationParametersDatafromDB(db);

                                    mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.latitude, DatabaseHelper.longitude,
                                            DatabaseHelper.recvdLatitude, DatabaseHelper.recvdLongitude, DatabaseHelper.sog, DatabaseHelper.cog, DatabaseHelper.predictionAccuracy}, null, null, null, null, null);
                                    if (mFixedStnCursor.moveToFirst()) {
                                        do {
                                            mmsi = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                                            predictionAccuracy = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.predictionAccuracy));
                                            if (predictionAccuracy > PREDICTION_ACCURACY_THRESHOLD_VALUE / VALIDATION_TIME) {
                                                stationLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                                                stationLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                                            } else {
                                                stationLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLatitude));
                                                stationLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLongitude));
                                            }
                                            stationSOG = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.sog));
                                            stationCOG = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.cog));
                                            predictedCoordinate = NavigationFunctions.calculateNewPosition(stationLatitude, stationLongitude, stationSOG, stationCOG);
                                            calculateNewParams(mmsi, predictedCoordinate[DatabaseHelper.LATITUDE_INDEX], predictedCoordinate[DatabaseHelper.LONGITUDE_INDEX]);
                                            ContentValues mContentValues = new ContentValues();
                                            mContentValues.put(DatabaseHelper.latitude, predictedCoordinate[DatabaseHelper.LATITUDE_INDEX]);
                                            mContentValues.put(DatabaseHelper.longitude, predictedCoordinate[DatabaseHelper.LONGITUDE_INDEX]);
                                            mContentValues.put(DatabaseHelper.xPosition, xPosition);
                                            mContentValues.put(DatabaseHelper.yPosition, yPosition);
                                            mContentValues.put(DatabaseHelper.distance, distance);
                                            mContentValues.put(DatabaseHelper.alpha, alpha);
                                            mContentValues.put(DatabaseHelper.isPredicted, 1);
                                            db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
                                            Log.d(TAG, "Lat: " + stationLatitude + " Lon: " + stationLongitude);
                                            Log.d(TAG, "PredLat: " + predictedCoordinate[0] + "PredLon: " + predictedCoordinate[1]);
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
                            }
                        } else{
                            mPredictionHandler.removeCallbacks(this);
                        }
                    }

                };
            mPredictionHandler.post(predictionRunnable);
        }
    }

    public static void setStopRunnable(boolean stop){
        stopRunnable = stop;
    }

    public static boolean getStopRunnable(){
        return stopRunnable;
    }

    private void retrieveConfigurationParametersDatafromDB(SQLiteDatabase db){
        try{
            Cursor configParamCursor = db.query(DatabaseHelper.configParametersTable, null, null,
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
        }
    }

    private void calculateNewParams(int mmsi, double latitude, double longitude ){
        if(mmsi == originMMSI){
            xPosition = 0.0;
            yPosition = 0.0;
            alpha = 0.0;
            distance = 0.0;
        } else if(mmsi == xAxisBaseStationMMSI){
            xPosition = NavigationFunctions.calculateDifference(originLatitude, originLongitude, latitude, longitude);
            yPosition = 0.0;
            alpha = 0.0;
            distance = xPosition;
        } else {
            distance = NavigationFunctions.calculateDifference(originLatitude, originLongitude, latitude, longitude);
            theta = NavigationFunctions.calculateAngleBeta(originLatitude, originLongitude, latitude, longitude);
            alpha = Math.abs(theta - beta);
            xPosition = distance * Math.cos(Math.toRadians(alpha));
            yPosition = distance * Math.sin(Math.toRadians(alpha));
        }
    }

    private boolean getOriginCoordinates(SQLiteDatabase db){

        try {

            Cursor baseStationCursor = db.query(DatabaseHelper.baseStationTable,
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
            Cursor fixedStationCursor = db.query(DatabaseHelper.fixedStationTable,
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

            Cursor betaCursor = db.query(DatabaseHelper.betaTable,
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
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        instance = null;
    }
}
