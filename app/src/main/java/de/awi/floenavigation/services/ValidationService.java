package de.awi.floenavigation.services;

import android.app.Dialog;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.DialogActivity;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.R;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * {@link ValidationService} service is used for handling algorithm to detect sea ice break.
 * The algorithm works by checking/comparing received positions {@link DatabaseHelper#recvdLatitude}, {@link DatabaseHelper#recvdLongitude}
 * and predicted positions {@link DatabaseHelper#latitude} and {@link DatabaseHelper#longitude} of the fixed stations {@link DatabaseHelper#fixedStationTable}.
 * If the distance is greater than {@link PredictionService#ERROR_THRESHOLD_VALUE} for {@link PredictionService#PREDICTION_ACCURACY_THRESHOLD_VALUE},
 * then the algorithm states that the fixed station has been broken off and its mmsi is removed from the database table. For this to happen at least {@value #MAX_NUM_OF_VALID_PACKETS}
 * valid ais packets should be available.
 * However there is a caveat to it, if the broken fixed station is origin or the x-axis fixed station although it is removed from the {@link DatabaseHelper#stationListTable}
 * the {@link PredictionService} service predicts its values, by providing it a new mmsi value, this is done so that the grid remains intact
 *
 * </p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class ValidationService extends IntentService {

    /**
     * Maximum number of valid ais packets to be received during the interval after the error in distance has been detected
     */
    private static final int MAX_NUM_OF_VALID_PACKETS = 3;
    /**
     * Handler to run the runnable
     */
    private final Handler mValidationHandler;
    private static final String TAG = "Validation Service: ";
    /**
     * Validation service periodic time interval
     */
    private static final int VALIDATION_TIME = 3 * 60 * 1000;
    /**
     * Array to store the base station mmsi's
     */
    private int[] baseStnMMSI = new int[DatabaseHelper.INITIALIZATION_SIZE];
    /**
     * The threshold distance/difference between the received and the predicted location
     */
    public static int ERROR_THRESHOLD_VALUE;
    /**
     * The threshold time after the distance goes beyond {@link #ERROR_THRESHOLD_VALUE}
     */
    public static int PREDICTION_ACCURACY_THRESHOLD_VALUE;

    /**
     * handler to display dialog box
     */
    private Handler uiHandler;

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
     * Not used
     */
    private static ValidationService instance = null;
    /**
     * <code>true</code> to stop the runnable
     * <code>false</code> otherwise
     */
    private static boolean stopRunnable = false;

    /**
     * Initializing the handlers
     */
    public ValidationService() {
        super("ValidationService");
        this.mValidationHandler = new Handler();
        uiHandler = new Handler();
        //appContext = con;

    }


    /**
     * OnCreate method to register the broadcast receiver {@link #broadcastReceiver}
     */
    @Override
    public void onCreate() {
        super.onCreate();
        //alertDialog = new Dialog(this);
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
     * Runnable is set to periodically run at {@link #VALIDATION_TIME} msecs.
     * The algorithm works in the following manner, necessary parameter values for each fixed station is retrieved from the database table {@link DatabaseHelper#fixedStationTable},
     * then the difference between the received and the predicted values are compared, if the error crosses beyond {@link #ERROR_THRESHOLD_VALUE},
     * <p>
     *     - {@link DatabaseHelper#incorrectMessageCount} is incremented based on the {@link DatabaseHelper#updateTime} and the {@link DatabaseHelper#validationCheckTime}
     *     - Also {@link DatabaseHelper#predictionAccuracy} is incremented
     *     - if the {@link DatabaseHelper#predictionAccuracy} crosses {@link #PREDICTION_ACCURACY_THRESHOLD_VALUE} / {@link #VALIDATION_TIME}
     *       and the {@link DatabaseHelper#incorrectMessageCount} is more than or equal to {@link #MAX_NUM_OF_VALID_PACKETS} then a dialog box is displayed and the
     *       fixed station enry is removed from the database
     * </p>
     * if {@link #stopRunnable} is true, the runnable is stopped until {@link #setStopRunnable(boolean)} with false value is not received
     * from {@link de.awi.floenavigation.synchronization.SyncActivity#stopServices} and {@link de.awi.floenavigation.initialsetup.SetupActivity#runServices}
     * @param intent Intent
     *
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

            Runnable validationRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!stopRunnable) {
                        Cursor mFixedStnCursor = null;
                        try {
                            SQLiteOpenHelper databaseHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                            SQLiteDatabase db = databaseHelper.getReadableDatabase();
                            baseStationsRetrievalfromDB(db);
                            retrieveConfigurationParametersDatafromDB(db);

                            double fixedStnrecvdLatitude;
                            double fixedStnrecvdLongitude;
                            double fixedStnLatitude;
                            double fixedStnLongitude;
                            double evaluationDifference;
                            double updateTime;
                            double validationCheckTime;
                            int predictionAccuracy;
                            int mmsi;
                            int stationMessageCount;
                            String stationName;

                            mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.stationName, DatabaseHelper.recvdLatitude, DatabaseHelper.recvdLongitude,
                                            DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.predictionAccuracy, DatabaseHelper.incorrectMessageCount, DatabaseHelper.validationCheckTime, DatabaseHelper.updateTime},
                                    null,
                                    null,
                                    null, null, null);
                            if (mFixedStnCursor.moveToFirst()) {
                                do {
                                    mmsi = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                                    fixedStnrecvdLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLatitude));
                                    fixedStnrecvdLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLongitude));
                                    fixedStnLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                                    fixedStnLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                                    predictionAccuracy = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.predictionAccuracy));
                                    stationMessageCount = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndexOrThrow(DatabaseHelper.incorrectMessageCount));
                                    //stationName = mFixedStnCursor.getString(mFixedStnCursor.getColumnIndex(DatabaseHelper.stationName));
                                    updateTime = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.updateTime));
                                    validationCheckTime = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndexOrThrow(DatabaseHelper.validationCheckTime));
                                    if (predictionAccuracy > PREDICTION_ACCURACY_THRESHOLD_VALUE / VALIDATION_TIME) {
                                        Log.d(TAG, "Packets = " + stationMessageCount);

                                        if (stationMessageCount >= MAX_NUM_OF_VALID_PACKETS) {
                                            stationMessageCount = 0;
                                            final int faildPredictionTime = PREDICTION_ACCURACY_THRESHOLD_VALUE / (60 * 1000);
                                            final String MMSI = String.valueOf(mmsi);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    dialogBoxDisplay(faildPredictionTime, MMSI);
                                                }
                                            });

                                            if (mmsi == baseStnMMSI[DatabaseHelper.firstStationIndex] || mmsi == baseStnMMSI[DatabaseHelper.secondStationIndex]) {
                                                deleteEntryfromStationListTableinDB(mmsi, db);
                                                updataMMSIInDBTables(mmsi, db, (mmsi == baseStnMMSI[DatabaseHelper.firstStationIndex]));
                                            } else {
                                                deleteEntryfromStationListTableinDB(mmsi, db);
                                                deleteEntryfromFixedStationTableinDB(mmsi, db);
                                            }
                                        }

                                    } else {
                                        evaluationDifference = NavigationFunctions.calculateDifference(fixedStnLatitude, fixedStnLongitude, fixedStnrecvdLatitude, fixedStnrecvdLongitude);
                                        Log.d(TAG, "Coordinates: " + fixedStnLatitude + ", " + fixedStnLongitude);
                                        Log.d(TAG, "Received Coordinate: " + fixedStnrecvdLatitude + ", " + fixedStnrecvdLongitude);
                                        Log.d(TAG, "EvalDiff: " + String.valueOf(evaluationDifference) + " predictionAccInDb: " + predictionAccuracy);
                                        if (evaluationDifference > ERROR_THRESHOLD_VALUE) {
                                            //getMessageCount(db, updateTime);
                                            if (updateTime > validationCheckTime) {
                                                stationMessageCount++;
                                                validationCheckTime = System.currentTimeMillis() - timeDiff;
                                            }
                                            ContentValues mContentValues = new ContentValues();
                                            mContentValues.put(DatabaseHelper.predictionAccuracy, predictionAccuracy);
                                            mContentValues.put(DatabaseHelper.predictionAccuracy, ++predictionAccuracy);
                                            mContentValues.put(DatabaseHelper.incorrectMessageCount, stationMessageCount);
                                            mContentValues.put(DatabaseHelper.validationCheckTime, validationCheckTime);
                                            Log.d(TAG, "EvaluationDifference > Threshold: predictionAccuracy: " + String.valueOf(predictionAccuracy));
                                            db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
                                        } else {
                                            stationMessageCount = 0;
                                            validationCheckTime = System.currentTimeMillis() - timeDiff;
                                            ContentValues mContentValues = new ContentValues();
                                            mContentValues.put(DatabaseHelper.incorrectMessageCount, stationMessageCount);
                                            mContentValues.put(DatabaseHelper.predictionAccuracy, 0);
                                            mContentValues.put(DatabaseHelper.validationCheckTime, validationCheckTime);
                                            //Log.d(TAG, "EvaluationDifference > Threshold: predictionAccuracy: " + String.valueOf(predictionAccuracy));
                                            db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
                                        }
                                    }
                                } while (mFixedStnCursor.moveToNext());
                                mFixedStnCursor.close();
                            } else {
                                Log.d(TAG, "FixedStationTable Cursor Error");
                            }
                            mValidationHandler.postDelayed(this, VALIDATION_TIME);
                        } catch (SQLException e) {
                            Log.d(TAG, String.valueOf(e));
                        } finally {
                            if (mFixedStnCursor != null) {
                                mFixedStnCursor.close();
                            }
                        }
                    } else {
                        mValidationHandler.removeCallbacks(this);
                    }

                }
            };

            mValidationHandler.postDelayed(validationRunnable, VALIDATION_TIME);
        }
    }

    /**
     * To set the value of {@link #stopRunnable}
     * @param stop stop flag
     */
    public static void setStopRunnable(boolean stop) {
        stopRunnable = stop;
    }

    /**
     * Getter function
     * @return returns the value of {@link #stopRunnable}
     */
    public static boolean getStopRunnable() {
        return stopRunnable;
    }

    /**
     * If the recovered fixed stations are part of the original base stations which were used to setup the initial grid
     * then the mmsi's for those stations are assigned {@value DatabaseHelper#BASESTN1} or {@value DatabaseHelper#BASESTN2} values such that the predictions for these
     * stations are in progress and can be redeployed at a different point in the grid even though these are recovered
     * @param mmsi mmsi to be recovered
     * @param db SQLiteDatabase instance
     * @param originFlag <code>true</code> if the mmsi is of the origin base station
     */
    private void updataMMSIInDBTables(int mmsi, SQLiteDatabase db, boolean originFlag) {
        ContentValues mContentValues = new ContentValues();
        mContentValues.put(DatabaseHelper.mmsi, ((originFlag) ? DatabaseHelper.BASESTN1 : DatabaseHelper.BASESTN2));
        mContentValues.put(DatabaseHelper.stationName, ((originFlag) ? DatabaseHelper.origin : DatabaseHelper.basestn1));
        db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
        db.update(DatabaseHelper.baseStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
    }

    /**
     * {@link DialogActivity} is called when the fixed station is broken off
     * The user/admin will get a pop-up on the screen
     * @param failedAttempts minutes
     * @param mmsi mmsi value
     */
    private void dialogBoxDisplay(int failedAttempts, String mmsi) {
        String validationMsg = getResources().getString(R.string.validationFailedMsg, failedAttempts, mmsi);
        String popupMsg = validationMsg + "\n" + getResources().getString(R.string.stationRemovedMsg);
        String title = "Validation Failed";
        Intent dialogIntent = new Intent(this, DialogActivity.class);
        dialogIntent.putExtra(DialogActivity.DIALOG_TITLE, title);
        dialogIntent.putExtra(DialogActivity.DIALOG_MSG, popupMsg);
        dialogIntent.putExtra(DialogActivity.DIALOG_VALIDATION, true);
        dialogIntent.putExtra(DialogActivity.DIALOG_OPTIONS, false);
        dialogIntent.putExtra(DialogActivity.DIALOG_ABOUTUS, false);
        dialogIntent.putExtra(DialogActivity.DIALOG_TABLETID, false);
        dialogIntent.putExtra(DialogActivity.DIALOG_ICON, R.drawable.ic_warning_black_24dp);

        //dialogIntent.putExtras(dialogParams);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);

    }

    /**
     * To display the dialog box
     * @param runnable Runnable object
     */
    private void runOnUiThread(Runnable runnable) {
        uiHandler.post(runnable);
    }

    /**
     * Function to delete mmsi from the {@link DatabaseHelper#stationListTable}
     * @param mmsiToBeRemoved mmsi
     * @param db SQLiteDatabase object
     */
    private void deleteEntryfromStationListTableinDB(int mmsiToBeRemoved, SQLiteDatabase db) {
        db.delete(DatabaseHelper.stationListTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsiToBeRemoved)});
        insertIntoStationListDeletedTable(db, String.valueOf(mmsiToBeRemoved));

    }

    /**
     * Function to delete mmsi from the {@link DatabaseHelper#fixedStationTable}
     * @param mmsiToBeRemoved mmsi
     * @param db SQLiteDatabase object
     */
    private void deleteEntryfromFixedStationTableinDB(int mmsiToBeRemoved, SQLiteDatabase db) {
        db.delete(DatabaseHelper.fixedStationTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsiToBeRemoved)});
        insertIntoFixedStationDeletedTable(db, String.valueOf(mmsiToBeRemoved));
    }

    /**
     * Function to retrieve the base stations from the {@link DatabaseHelper#baseStationTable} table
     * @param db SQLiteDatabase object
     */
    private void baseStationsRetrievalfromDB(SQLiteDatabase db) {
        Cursor mBaseStnCursor = null;
        try {
            //SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            //SQLiteDatabase db = dbHelper.getReadableDatabase();
            mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.isOrigin},
                    null, null, null, null, DatabaseHelper.isOrigin + " DESC");

            if (mBaseStnCursor.getCount() == DatabaseHelper.NUM_OF_BASE_STATIONS) {
                int index = 0;

                if (mBaseStnCursor.moveToFirst()) {
                    do {
                        baseStnMMSI[index] = mBaseStnCursor.getInt(mBaseStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                        //isOriginMMSI[index] = mBaseStnCursor.getInt(mBaseStnCursor.getColumnIndex(DatabaseHelper.isOrigin));
                        index++;
                    } while (mBaseStnCursor.moveToNext());
                } else {
                    Log.d(TAG, "Base stn cursor error");
                }

            } else {
                Log.d(TAG, "Error reading from base stn table");
            }
            mBaseStnCursor.close();
        } catch (SQLException e) {

            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        } finally {
            if (mBaseStnCursor != null) {
                mBaseStnCursor.close();
            }
        }

    }

    /**
     * Function to retrieve configuration parameters {@link DatabaseHelper#configParametersTable}
     * @param db SQLiteDatabase object
     */
    private void retrieveConfigurationParametersDatafromDB(SQLiteDatabase db) {
        Cursor configParamCursor = null;
        try {
            configParamCursor = db.query(DatabaseHelper.configParametersTable, null, null,
                    null, null, null, null);
            String parameterName = null;
            int parameterValue = 0;

            if (configParamCursor.moveToFirst()) {
                do {
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
                } while (configParamCursor.moveToNext());
            } else {
                Log.d(TAG, "Config Parameter table cursor error");
            }
            configParamCursor.close();
        } catch (SQLException e) {

            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        } finally {
            if (configParamCursor != null) {
                configParamCursor.close();
            }
        }
    }

    /**
     * Insert the mmsi to be deleted into the {@link DatabaseHelper#fixedStationDeletedTable}, to be used
     * for synchronization purpose
     * @param db SQLiteDatabase object
     * @param mmsiToBeAdded mmsi
     */
    private void insertIntoFixedStationDeletedTable(SQLiteDatabase db, String mmsiToBeAdded) {
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.mmsi, Integer.valueOf(mmsiToBeAdded));
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - timeDiff));
        db.insert(DatabaseHelper.fixedStationDeletedTable, null, deletedStation);
    }

    /**
     * Insert the mmsi to be deleted into the {@link DatabaseHelper#stationListDeletedTable}, to be used
     * for synchronization purpose
     * @param db SQLiteDatabase object
     * @param mmsiToBeAdded mmsi
     */
    private void insertIntoStationListDeletedTable(SQLiteDatabase db, String mmsiToBeAdded) {
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.mmsi, Integer.valueOf(mmsiToBeAdded));
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - timeDiff));
        db.insert(DatabaseHelper.stationListDeletedTable, null, deletedStation);
    }

    /**
     * onDestroy to unregister the {@link #broadcastReceiver} receiver
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

}
