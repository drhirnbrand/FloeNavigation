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
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class ValidationService extends IntentService {

    private static final int MAX_NUM_OF_VALID_PACKETS = 3;
    private final Handler mValidationHandler;
    private static final String TAG = "Validation Service: ";
    private static final int VALIDATION_TIME = 3 * 60 * 1000;
    private int[] baseStnMMSI = new int[DatabaseHelper.INITIALIZATION_SIZE];
    public static int ERROR_THRESHOLD_VALUE;
    public static int PREDICTION_ACCURACY_THRESHOLD_VALUE;
    private static int stationMessageCount = 0;
    private static double stationpreviousUpdateTime = 0;


    private Handler uiHandler;
    private Dialog alertDialog;
    Button dialogOkBtn;
    ImageView dialogIcon;
    TextView validationFailedMsg, stationRemovedMsg;

    private BroadcastReceiver broadcastReceiver;
    private long gpsTime;
    private long timeDiff;
    //int[] isOriginMMSI;
    //private Context appContext;

    private static ValidationService instance = null;
    private static boolean stopRunnable = false;

    public ValidationService() {
        super("ValidationService");
        this.mValidationHandler = new Handler();
        uiHandler = new Handler();
        //appContext = con;

    }

    public static boolean isInstanceCreated(){
        return instance != null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        //alertDialog = new Dialog(this);
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

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

            Runnable validationRunnable = new Runnable() {
                @Override
                public void run() {
                    if(!stopRunnable) {
                        try{
                            SQLiteOpenHelper databaseHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                            SQLiteDatabase db = databaseHelper.getReadableDatabase();
                            baseStationsRetrievalfromDB(db);
                            retrieveConfigurationParametersDatafromDB(db);
                            Cursor mFixedStnCursor;
                            double fixedStnrecvdLatitude;
                            double fixedStnrecvdLongitude;
                            double fixedStnLatitude;
                            double fixedStnLongitude;
                            double evaluationDifference;
                            double updateTime;
                            int predictionAccuracy;
                            int mmsi;
                            String stationName;

                            mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.stationName, DatabaseHelper.recvdLatitude, DatabaseHelper.recvdLongitude,
                                    DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.predictionAccuracy, DatabaseHelper.updateTime},null, null, null, null, null);
                            if (mFixedStnCursor.moveToFirst()){
                                do{
                                    mmsi = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                                    fixedStnrecvdLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLatitude));
                                    fixedStnrecvdLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.recvdLongitude));
                                    fixedStnLatitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.latitude));
                                    fixedStnLongitude = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.longitude));
                                    predictionAccuracy = mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.predictionAccuracy));
                                    //stationName = mFixedStnCursor.getString(mFixedStnCursor.getColumnIndex(DatabaseHelper.stationName));
                                    updateTime = mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.updateTime));
                                    if (predictionAccuracy > PREDICTION_ACCURACY_THRESHOLD_VALUE / VALIDATION_TIME){

                                        if (stationMessageCount > MAX_NUM_OF_VALID_PACKETS) {
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

                                    }else {
                                        evaluationDifference = NavigationFunctions.calculateDifference(fixedStnLatitude, fixedStnLongitude, fixedStnrecvdLatitude, fixedStnrecvdLongitude);
                                        Log.d(TAG, "EvalDiff: " + String.valueOf(evaluationDifference) + " predictionAccInDb: " + predictionAccuracy);
                                        if (evaluationDifference > ERROR_THRESHOLD_VALUE) {
                                            getMessageCount(db, updateTime);
                                            ContentValues mContentValues = new ContentValues();
                                            mContentValues.put(DatabaseHelper.predictionAccuracy, ++predictionAccuracy);
                                            Log.d(TAG, "EvaluationDifference > Threshold: predictionAccuracy: " + String.valueOf(predictionAccuracy));
                                            db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
                                        } else {
                                            stationMessageCount = 0;
                                            ContentValues mContentValues = new ContentValues();
                                            mContentValues.put(DatabaseHelper.predictionAccuracy, 0);
                                            //Log.d(TAG, "EvaluationDifference > Threshold: predictionAccuracy: " + String.valueOf(predictionAccuracy));
                                            db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
                                        }
                                    }
                                } while(mFixedStnCursor.moveToNext());
                                mFixedStnCursor.close();
                            }else {
                                Log.d(TAG, "FixedStationTable Cursor Error");
                            }
                            mValidationHandler.postDelayed(this, VALIDATION_TIME);
                        }catch (SQLException e){
                            Log.d(TAG, String.valueOf(e));
                        }
                    }
                    else{
                        mValidationHandler.removeCallbacks(this);
                    }

                }
            };

            mValidationHandler.postDelayed(validationRunnable, VALIDATION_TIME);
        }
    }

    public static void setStopRunnable(boolean stop){
        stopRunnable = stop;
    }

    public static boolean getStopRunnable(){
        return stopRunnable;
    }

    private void updataMMSIInDBTables(int mmsi, SQLiteDatabase db, boolean originFlag){
        ContentValues mContentValues = new ContentValues();
        mContentValues.put(DatabaseHelper.mmsi, ((originFlag) ? DatabaseHelper.BASESTN1 : DatabaseHelper.BASESTN2));
        mContentValues.put(DatabaseHelper.stationName, ((originFlag) ? DatabaseHelper.origin : DatabaseHelper.basestn1));
        db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
        db.update(DatabaseHelper.baseStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
    }

    private void getMessageCount(SQLiteDatabase db, double updateTime) {
        //long numOfStaticStations = DatabaseUtils.queryNumEntries(db, DatabaseHelper.fixedStationTable);
        if (updateTime > stationpreviousUpdateTime){
            stationpreviousUpdateTime = updateTime;
            stationMessageCount++;
        }
    }

    private void dialogBoxDisplay(int failedAttempts, String mmsi) {
        String validationMsg = getResources().getString(R.string.validationFailedMsg, failedAttempts, mmsi);
        String popupMsg = validationMsg + "\n" + getResources().getString(R.string.stationRemovedMsg);
        String title = "Validation Failed";
        Intent dialogIntent = new Intent(this, DialogActivity.class);
        dialogIntent.putExtra(DialogActivity.DIALOG_TITLE, title);
        dialogIntent.putExtra(DialogActivity.DIALOG_MSG, popupMsg);
        dialogIntent.putExtra(DialogActivity.DIALOG_OPTIONS, false);
        dialogIntent.putExtra(DialogActivity.DIALOG_ABOUTUS, false);
        dialogIntent.putExtra(DialogActivity.DIALOG_TABLETID, false);
        dialogIntent.putExtra(DialogActivity.DIALOG_ICON, R.drawable.ic_warning_black_24dp);

        //dialogIntent.putExtras(dialogParams);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);


        /*alertDialog.setContentView(R.layout.dialog_validation_failed);
        dialogIcon = alertDialog.findViewById(R.id.dialogIcon);
        validationFailedMsg = alertDialog.findViewById(R.id.validationFailed);
        stationRemovedMsg = alertDialog.findViewById(R.id.stationRemoved);
        dialogOkBtn = alertDialog.findViewById(R.id.validationDialogOkBtn);

        dialogOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        alertDialog.setOnDismissListener(new Dialog.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                alertDialog.dismiss();
            }
        });

        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();*/
    }

    private void runOnUiThread(Runnable runnable){
        uiHandler.post(runnable);
    }

    private void deleteEntryfromStationListTableinDB(int mmsiToBeRemoved, SQLiteDatabase db){
        db.delete(DatabaseHelper.stationListTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsiToBeRemoved)});
        insertIntoStationListDeletedTable(db, String.valueOf(mmsiToBeRemoved));

    }

    private void deleteEntryfromFixedStationTableinDB(int mmsiToBeRemoved, SQLiteDatabase db){
        db.delete(DatabaseHelper.fixedStationTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsiToBeRemoved)});
        insertIntoFixedStationDeletedTable(db, String.valueOf(mmsiToBeRemoved));
    }

    private void baseStationsRetrievalfromDB(SQLiteDatabase db){

        try {
            //SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            //SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.isOrigin},
                    null, null, null, null, DatabaseHelper.isOrigin + " DESC");

            if (mBaseStnCursor.getCount() == DatabaseHelper.NUM_OF_BASE_STATIONS) {
                int index = 0;

                if (mBaseStnCursor.moveToFirst()) {
                    do {
                        baseStnMMSI[index] = mBaseStnCursor.getInt(mBaseStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                        //isOriginMMSI[index] = mBaseStnCursor.getInt(mBaseStnCursor.getColumnIndex(DatabaseHelper.isOrigin));
                        index++;
                    } while (mBaseStnCursor.moveToNext());
                }else {
                    Log.d(TAG, "Base stn cursor error");
                }

            } else {
                Log.d(TAG, "Error reading from base stn table");
            }
            mBaseStnCursor.close();
        }catch (SQLException e){

            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        }

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

    private void insertIntoFixedStationDeletedTable(SQLiteDatabase db, String mmsiToBeAdded) {
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.mmsi, Integer.valueOf(mmsiToBeAdded));
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - timeDiff));
        db.insert(DatabaseHelper.fixedStationDeletedTable, null, deletedStation);
    }

    private void insertIntoStationListDeletedTable(SQLiteDatabase db, String mmsiToBeAdded){
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.mmsi, Integer.valueOf(mmsiToBeAdded));
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - timeDiff));
        db.insert(DatabaseHelper.stationListDeletedTable, null, deletedStation);
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
