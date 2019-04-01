package de.awi.floenavigation.initialsetup;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.services.AlphaCalculationService;
import de.awi.floenavigation.services.AngleCalculationService;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.DialogActivity;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.services.PredictionService;
import de.awi.floenavigation.R;
import de.awi.floenavigation.services.ValidationService;


public class SetupActivity extends ActionBarActivity {

    private static final String TAG = "SetupActivity";
    private static final int JOB_ID = 100;

    /**
     * Specifies how the total amount of time the Predictions will run for.
     * Read from the Configuration Parameter {@link DatabaseHelper#initial_setup_time}
     * Specifies the delay for {@link #parentTimer}.
     */
    private static int PREDICTION_TIME;// = 2 * 60 * 1000; //30 * 60 * 1000;

    /**
     * Specifies the interval for each prediction. Currently set to 10 seconds.
     * Specifies the time period for {@link #timer}.
     */
    private static final int PREDICATION_TIME_PERIOD = 10 * 1000;

    /**
     * Specifies the minimum number of times the Prediction timer ({@link #timer}) has to run before it can be ended.
     * It describes the absolute minimum number predictions that have to occur before the Setup Activity can end.
     */
    private static final int MAX_TIMER_COUNT = 3;

    /**
     * {@link Toast} message which is displayed in case back button is pressed. Back Button is displayed in this Activity.
     */
    private static final String toastMsg = "Please wait while Coordinate System is being Setup";

    /**
     * String which specifies the key in the {@link Bundle} for whether the activity was started from {@link CoordinateFragment}
     */
    public static final String calledFromCoordinateFragment = "calledFromFragment";

    /**
     * {@link Timer} object which monitors the overall time for the Predictions. It starts with a Delay of {@link #PREDICTION_TIME}
     * and checks every 500 milliseconds if {@link #MAX_TIMER_COUNT} number of Predictions have been done. If so, it stops both itself and
     * the prediction timer {@link #timer}, stops the circular progess bar and displays the Next Button on the screen.
     */
    Timer parentTimer = new Timer();

    /**
     * {@link Timer} object which runs every {@link #PREDICATION_TIME_PERIOD}, it reads the current location of both the Origin and x-Axis
     * marker, predicts new location of the stations, displays the received location, predicted location and their difference on the screen
     * and updates the {@link ProgressBar} percentage.
     * <p>
     *     It runs until it is cancelled by {@link #parentTimer}.
     * </p>
     */
    Timer timer = new Timer();

    /**
     * Counts the number of times the {@link #timer} is run. As each run of {@link #timer} is a new prediction this is effectively a count
     * of the number of predictions.
     */
    private int timerCounter = 0;

    /**
     * Array holding the MMSIs of the Origin and x-Axis Marker.
     */
    private int[] stationMMSI = new int[DatabaseHelper.INITIALIZATION_SIZE];

    /**
     * Array holding the received latitudes of the Origin and x-Axis Marker.
     */
    private double[] stationLatitude = new double[DatabaseHelper.INITIALIZATION_SIZE];

    /**
     * Array holding the received longitudes of the Origin and x-Axis Marker.
     */
    private double[] stationLongitude = new double[DatabaseHelper.INITIALIZATION_SIZE];

    /**
     * Array holding the Speed Over Ground (SOG) of the Origin and x-Axis Marker.
     */
    private double[] stationSOG = new double[DatabaseHelper.INITIALIZATION_SIZE];

    /**
     * Array holding the Course Over Ground (COG) of the Origin and x-Axis Marker.
     */
    private double[] stationCOG = new double[DatabaseHelper.INITIALIZATION_SIZE];

    /**
     * Array holding the predicted latitudes of the Origin and x-Axis Marker.
     */
    private double[] predictedLatitude = new double[DatabaseHelper.INITIALIZATION_SIZE];

    /**
     * Array holding the predicted longitudes of the Origin and x-Axis Marker.
     */
    private double[] predictedLongitude = new double[DatabaseHelper.INITIALIZATION_SIZE];

    /**
     * Array holding the difference in meters between the received location and the predicted location
     * of both the Origin and the x-Axis Marker.
     */
    private double[] distanceDiff = new double[DatabaseHelper.INITIALIZATION_SIZE];

    /**
     * Array holding the time of last received packet of the Origin and the x-Axis Marker.
     */
    private Date[] stationTime = new Date[DatabaseHelper.INITIALIZATION_SIZE];

    /**
     * Angle {@link DatabaseHelper#beta} calculated from Predicted coordinates ({@link #predictedLatitude}, {@link #predictedLongitude})
     */
    private double predictedBeta = 0.0;

    /**
     * Angle {@link DatabaseHelper#beta} calculated from received coordinates ({@link #stationLatitude}, {@link #stationLongitude})
     */
    private double receivedBeta = 0.0;

    /**
     * Difference in degrees between the {@link #predictedBeta} and {@link #receivedBeta}
     */
    private double betaDifference = 0.0;

    /**
     * Distance between the Origin and the x-Axis marker station.
     */
    private double xAxisDistance = 0.0;

    /**
     * Sets the format for display of Geographic Coordinates on the Screen.
     * If <code>true</code> the coordinates will be displayed as degree, minutes, seconds
     */
    private boolean changeFormat;

    /**
     * Counter to keep track of the progress shown in percentage inside the circulate {@link ProgressBar}.
     */
    private int timerIndex = 0;

    /**
     * The Circular {@link ProgressBar} shown on screen which shows the number of times predictions has run as a percentage of
     * {@link #PREDICTION_TIME}
     */
    private ProgressBar timerProgress;

    /**
     * The percentage value shown inside {@link #timerProgress}
     */
    private int timerPercentage;

    /**
     * {@link TextView} of the {@link ProgressBar}
     */
    private TextView progressBarValue;

    /**
     * Number of messages received from the Origin while {@link #timer} is running.
     */
    private static int firstStationMessageCount = 0;

    /**
     * Number of messages received from the x-Axis Marker while {@link #timer} is running.
     */
    private static int secondStationMessageCount = 0;

    /**
     * <code>true</code> when Back Button is enabled.
     */
    private boolean backButtonEnabled = false;

    /**
     * UTC Time in Milliseconds of last received AIS packet of the origin
     */
    private static double firstStationpreviousUpdateTime = 0;

    /**
     * UTC Time in Milliseconds of last received AIS packet of the x-Axis marker
     */
    private static double secondStationpreviousUpdateTime = 0;

    /**
     * Sets the number of significant figures to show after a decimal point on the screen. This does not affect the calculations.
     * The value is read from {@link DatabaseHelper#decimal_number_significant_figures}
     */
    private int numOfSignificantFigures;
    private boolean isLock = false;

    /**
     * {@link SimpleDateFormat} to convert the update time {@link #firstStationpreviousUpdateTime} and {@link #secondStationpreviousUpdateTime}
     * to normal Date Time format.
     */
    private SimpleDateFormat sdf;

    /**
     * <code>true</code> when the activity is started from {@link CoordinateFragment}
     */
    private boolean isCalledFromCoordinateFragment = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setContentView(R.layout.activity_setup);
        hideNavigationBar();
        sdf = new SimpleDateFormat("HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        retrievePredictionTimefromDB();

        if(getIntent().getExtras().containsKey(calledFromCoordinateFragment)){
            isCalledFromCoordinateFragment = getIntent().getExtras().getBoolean(calledFromCoordinateFragment);
        }

        progressBarValue = findViewById(R.id.progressBarText);
        progressBarValue.setEnabled(true);

        timerProgress = findViewById(R.id.progressBar);
        timerProgress.setEnabled(true);
        timerPercentage = 0;
        changeFormat = DatabaseHelper.readCoordinateDisplaySetting(this);
        numOfSignificantFigures = DatabaseHelper.readSiginificantDigitsSetting(this);

        //Populate Screen with Initial Values from DB
        new ReadParamsFromDB().execute();
        try{
            Thread.sleep(50);
        } catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }

        receivedBeta = NavigationFunctions.calculateAngleBeta(stationLatitude[DatabaseHelper.firstStationIndex],
                stationLongitude[DatabaseHelper.firstStationIndex], stationLatitude[DatabaseHelper.secondStationIndex], stationLongitude[DatabaseHelper.secondStationIndex]);
        if(!isCalledFromCoordinateFragment){
            predictedBeta = NavigationFunctions.calculateAngleBeta(predictedLatitude[DatabaseHelper.firstStationIndex],
                    predictedLongitude[DatabaseHelper.firstStationIndex], predictedLatitude[DatabaseHelper.secondStationIndex], predictedLongitude[DatabaseHelper.secondStationIndex]);
            for(int i = 0; i < DatabaseHelper.INITIALIZATION_SIZE; i++) {
                distanceDiff[i] = NavigationFunctions.calculateDifference(stationLatitude[i], stationLongitude[i], predictedLatitude[i], predictedLongitude[i]);

            }
            betaDifference = Math.abs(predictedBeta - receivedBeta);
        }

        refreshScreen();


        xAxisDistance = NavigationFunctions.calculateDifference(stationLatitude[DatabaseHelper.firstStationIndex], stationLongitude[DatabaseHelper.firstStationIndex],
                stationLatitude[DatabaseHelper.secondStationIndex], stationLongitude[DatabaseHelper.secondStationIndex]);
        new InsertXAxisDistance().execute();


        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "Predicting New Values");
                timerCounter++;


                new ReadParamsFromDB().execute();
                for(int i = 0; i < DatabaseHelper.INITIALIZATION_SIZE; i++){
                    double[] predictedCoordinates = NavigationFunctions.calculateNewPosition(stationLatitude[i], stationLongitude[i], stationSOG[i], stationCOG[i]);
                    predictedLatitude[i] = predictedCoordinates[DatabaseHelper.LATITUDE_INDEX];
                    predictedLongitude[i] = predictedCoordinates[DatabaseHelper.LONGITUDE_INDEX];
                    distanceDiff[i] = NavigationFunctions.calculateDifference(stationLatitude[i], stationLongitude[i], predictedLatitude[i], predictedLongitude[i]);
                }

                predictedBeta = NavigationFunctions.calculateAngleBeta(predictedLatitude[DatabaseHelper.firstStationIndex], predictedLongitude[DatabaseHelper.firstStationIndex], predictedLatitude[DatabaseHelper.secondStationIndex], predictedLongitude[DatabaseHelper.secondStationIndex]);
                receivedBeta = NavigationFunctions.calculateAngleBeta(stationLatitude[DatabaseHelper.firstStationIndex], stationLongitude[DatabaseHelper.firstStationIndex], stationLatitude[DatabaseHelper.secondStationIndex], stationLongitude[DatabaseHelper.secondStationIndex]);
                betaDifference = Math.abs(predictedBeta - receivedBeta);

                refreshScreen();
                progressBarValueUpdates();
            }
        }, 0, PREDICATION_TIME_PERIOD);


        parentTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "StartupComplete");
                if (timerCounter >= MAX_TIMER_COUNT)
                {
                    timer.cancel();
                    Log.d(TAG, "Completed");
                    if(insertPredictedValuesInDB()){
                        Log.d(TAG, "Values Inserted Successfully");
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Setup Complete", Toast.LENGTH_LONG).show();
                        }
                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.setup_finish).setVisibility(View.VISIBLE);
                            timerProgress.setVisibility(View.GONE);
                            findViewById(R.id.progressBarText).setVisibility(View.GONE);
                        }
                    });


                    //------//
                    backButtonEnabled = true;
                    timer.cancel();
                    parentTimer.cancel();



                }
                            }
        }, PREDICTION_TIME, 500);

    }

    /**
     * Hides the Navigation Bar (containing the Back Button, Home Button etc) on the bottom of the screen
     */
    private void hideNavigationBar() {

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * Inserts the Predicted Latitude and Longitude of both the Origin and x-Axis marker in to the Database table {@link DatabaseHelper#fixedStationTable}
     * @return <code>true</code> if inserted successfully.
     */
    private boolean insertPredictedValuesInDB(){
        try{
            DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            for (int i = 0; i < DatabaseHelper.INITIALIZATION_SIZE; i++) {
                ContentValues fixedStation = new ContentValues();
                fixedStation.put(DatabaseHelper.latitude, predictedLatitude[i]);
                fixedStation.put(DatabaseHelper.longitude, predictedLongitude[i]);
                db.update(DatabaseHelper.fixedStationTable, fixedStation, DatabaseHelper.mmsi + " = ?", new String[] {String.valueOf(stationMMSI[i])});
            }

            return true;

        } catch(SQLiteException e){
            Log.d(TAG, "Error Updating Database with Predicted Values");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves the total amount of time the overall Prediction timer has to run for from the Database table {@link DatabaseHelper#configParametersTable}
     * and sets it to {@link #PREDICTION_TIME}
     */
    private void retrievePredictionTimefromDB(){
        Cursor configParamCursor = null;
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            configParamCursor = db.query(DatabaseHelper.configParametersTable, null, null,
                    null, null, null, null);
            String parameterName;
            int parameterValue = 0;

            if (configParamCursor.moveToFirst()){
                do{
                    parameterName = configParamCursor.getString(configParamCursor.getColumnIndex(DatabaseHelper.parameterName));
                    parameterValue = configParamCursor.getInt(configParamCursor.getColumnIndex(DatabaseHelper.parameterValue));

                    switch (parameterName) {
                        case DatabaseHelper.initial_setup_time:
                            PREDICTION_TIME = parameterValue;
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
            if(configParamCursor != null){
                configParamCursor.close();
            }
        }
    }

    /**
     * Updates the Percentage value shown inside the Circular {@link ProgressBar}
     */
    private void progressBarValueUpdates(){
        //Progress Bar Value update

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBarValue.setText(String.format("%s%%", String.valueOf(timerPercentage)));
                timerIndex++;
                timerPercentage = (int)timerIndex * 100 / (PREDICTION_TIME / PREDICATION_TIME_PERIOD);
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu, 1);
    }

    private void dialogBoxDisplay() {

        String popupMsg = "Do you wish to rerun the initial setup?, then press Confirm!";
        String title = "Initial Setup Completed";
        Intent dialogIntent = new Intent(this, DialogActivity.class);
        dialogIntent.putExtra(DialogActivity.DIALOG_TITLE, title);
        dialogIntent.putExtra(DialogActivity.DIALOG_MSG, popupMsg);
        dialogIntent.putExtra(DialogActivity.DIALOG_ICON, R.drawable.ic_done_all_black_24dp);
        dialogIntent.putExtra(DialogActivity.DIALOG_BETA, receivedBeta);
        dialogIntent.putExtra(DialogActivity.DIALOG_OPTIONS, true);
        dialogIntent.putExtra(DialogActivity.DIALOG_TABLETID, false);
        dialogIntent.putExtra(DialogActivity.DIALOG_ABOUTUS, false);
        startActivity(dialogIntent);
    }

    public static void runServices(Context mContext){
        AngleCalculationService.setStopRunnable(false);
        AlphaCalculationService.stopTimer(false);
        PredictionService.setStopRunnable(false);
        ValidationService.setStopRunnable(false);
        Intent angleCalcServiceIntent = new Intent(mContext, AngleCalculationService.class);
        Intent alphaCalcServiceIntent = new Intent (mContext, AlphaCalculationService.class);
        Intent predictionServiceIntent = new Intent(mContext, PredictionService.class);
        Intent validationServiceIntent = new Intent(mContext, ValidationService.class);
        mContext.startService(angleCalcServiceIntent);
        mContext.startService(alphaCalcServiceIntent);
        mContext.startService(predictionServiceIntent);
        mContext.startService(validationServiceIntent);
        MainActivity.areServicesRunning = true;
    }

    private void refreshScreen(){
        final EditText ais1MMSI = findViewById(R.id.first_station_MMSI);
        final EditText ais1UpdateTime = findViewById(R.id.first_station_updateTime);
        final EditText ais1RcvLatitude = findViewById(R.id.first_station_received_Latitude);
        final EditText ais1RcvLongitude = findViewById(R.id.first_station_received_Longitude);
        final EditText ais1PrdLatitude = findViewById(R.id.first_station_predicted_Latitude);
        final EditText ais1PrdLongitude = findViewById(R.id.first_station_predicted_Longitude);
        final EditText ais1Difference = findViewById(R.id.first_station_diff_distance);
        final EditText ais2RcvLatitude = findViewById(R.id.second_station_received_latitude);
        final EditText ais2RcvLongitude = findViewById(R.id.second_station_received_longitude);
        final EditText ais2PrdLatitude = findViewById(R.id.second_station_predicted_latitude);
        final EditText ais2PrdLongitude = findViewById(R.id.second_station_predicted_longitude);
        final EditText ais2Difference = findViewById(R.id.second_station_diff_distance);
        final EditText ais2MMSI = findViewById(R.id.second_station_MMSI);
        final EditText ais2UpdateTime = findViewById(R.id.second_station_updateTime);
        final EditText calculatedBeta = findViewById(R.id.calculatedBeta);
        final EditText rcvBeta = findViewById(R.id.receivedBeta);
        final EditText betaDiff = findViewById(R.id.betaDifference);
        final EditText ais1StationMsgCount = findViewById(R.id.first_station_msgCount);
        final EditText ais2StationMsgCount = findViewById(R.id.second_station_msgCount);
        final String formatString = "%."+String.valueOf(numOfSignificantFigures)+"f";



        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ais1MMSI.setEnabled(true);
                ais1UpdateTime.setEnabled(true);
                ais1Difference.setEnabled(true);
                ais1PrdLatitude.setEnabled(true);
                ais1PrdLongitude.setEnabled(true);
                ais1RcvLatitude.setEnabled(true);
                ais1RcvLongitude.setEnabled(true);
                ais1StationMsgCount.setEnabled(true);
                ais2Difference.setEnabled(true);
                ais2PrdLatitude.setEnabled(true);
                ais2PrdLongitude.setEnabled(true);
                ais2RcvLatitude.setEnabled(true);
                ais2RcvLongitude.setEnabled(true);
                ais2MMSI.setEnabled(true);
                ais2UpdateTime.setEnabled(true);
                ais2StationMsgCount.setEnabled(true);
                calculatedBeta.setEnabled(true);
                rcvBeta.setEnabled(true);
                betaDiff.setEnabled(true);
                ais1MMSI.setText(String.valueOf(stationMMSI[DatabaseHelper.firstStationIndex]));
                //ais1UpdateTime.setText(String.valueOf(stationUpdateTime[DatabaseHelper.firstStationIndex]));
                ais1UpdateTime.setText(sdf.format(stationTime[DatabaseHelper.firstStationIndex]));

                //ais1Difference.setText(String.valueOf(distanceDiff[DatabaseHelper.firstStationIndex]));
                ais1Difference.setText(String.format(formatString, distanceDiff[DatabaseHelper.firstStationIndex]));



                if(changeFormat){
                    String[] ais1FormattedPredictedCoordinates = NavigationFunctions.locationInDegrees(predictedLatitude[DatabaseHelper.firstStationIndex],
                                                                predictedLongitude[DatabaseHelper.firstStationIndex]);
                    ais1PrdLatitude.setText(ais1FormattedPredictedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
                    ais1PrdLongitude.setText(ais1FormattedPredictedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);

                    String[] ais1FormattedReceivedCoordinates = NavigationFunctions.locationInDegrees(stationLatitude[DatabaseHelper.firstStationIndex],
                                                                stationLongitude[DatabaseHelper.firstStationIndex]);
                    ais1RcvLatitude.setText(ais1FormattedReceivedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
                    ais1RcvLongitude.setText(ais1FormattedReceivedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
                } else {
                    ais1PrdLatitude.setText(String.format(formatString, predictedLatitude[DatabaseHelper.firstStationIndex]));
                    ais1PrdLongitude.setText(String.format(formatString, predictedLongitude[DatabaseHelper.firstStationIndex]));
                    ais1RcvLatitude.setText(String.format(formatString, stationLatitude[DatabaseHelper.firstStationIndex]));
                    ais1RcvLongitude.setText(String.format(formatString, stationLongitude[DatabaseHelper.firstStationIndex]));
                }

                ais2MMSI.setText(String.valueOf(stationMMSI[DatabaseHelper.secondStationIndex]));
                //ais2UpdateTime.setText(String.valueOf(stationUpdateTime[DatabaseHelper.secondStationIndex]));
                ais2UpdateTime.setText(sdf.format(stationTime[DatabaseHelper.secondStationIndex]));
                ais2Difference.setText(String.format(formatString, distanceDiff[DatabaseHelper.secondStationIndex]));

                if(changeFormat){
                    String[] ais2FormattedPredictedCoordinates = NavigationFunctions.locationInDegrees(predictedLatitude[DatabaseHelper.secondStationIndex],
                                                    predictedLongitude[DatabaseHelper.secondStationIndex]);
                    ais2PrdLatitude.setText(ais2FormattedPredictedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
                    ais2PrdLongitude.setText(ais2FormattedPredictedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);

                    String[] ais2FormattedReceivedCoordinates = NavigationFunctions.locationInDegrees(stationLatitude[DatabaseHelper.secondStationIndex],
                                                            stationLongitude[DatabaseHelper.secondStationIndex]);
                    ais2RcvLatitude.setText(ais2FormattedReceivedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
                    ais2RcvLongitude.setText(ais2FormattedReceivedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
                } else {
                    ais2PrdLatitude.setText(String.format(formatString, predictedLatitude[DatabaseHelper.secondStationIndex]));
                    ais2PrdLongitude.setText(String.format(formatString, predictedLongitude[DatabaseHelper.secondStationIndex]));
                    ais2RcvLatitude.setText(String.format(formatString, stationLatitude[DatabaseHelper.secondStationIndex]));
                    ais2RcvLongitude.setText(String.format(formatString, stationLongitude[DatabaseHelper.secondStationIndex]));
                }

                ais1StationMsgCount.setText(String.valueOf(firstStationMessageCount));
                ais2StationMsgCount.setText(String.valueOf(secondStationMessageCount));
                //calculatedBeta.setText(String.valueOf(predictedBeta));
                //rcvBeta.setText(String.valueOf(receivedBeta));
                //betaDiff.setText(String.valueOf(betaDifference));
                calculatedBeta.setText(String.format(formatString, predictedBeta));
                rcvBeta.setText(String.format(formatString, receivedBeta));
                betaDiff.setText(String.format(formatString, betaDifference));
                ais1MMSI.setEnabled(false);
                ais1UpdateTime.setEnabled(false);
                ais1Difference.setEnabled(false);
                ais1PrdLatitude.setEnabled(false);
                ais1PrdLongitude.setEnabled(false);
                ais1RcvLatitude.setEnabled(false);
                ais1RcvLongitude.setEnabled(false);
                ais2Difference.setEnabled(false);
                ais2PrdLatitude.setEnabled(false);
                ais2PrdLongitude.setEnabled(false);
                ais2RcvLatitude.setEnabled(false);
                ais2RcvLongitude.setEnabled(false);
                ais2MMSI.setEnabled(false);
                ais2UpdateTime.setEnabled(false);
                calculatedBeta.setEnabled(false);
                rcvBeta.setEnabled(false);
                betaDiff.setEnabled(false);
                ais1StationMsgCount.setEnabled(false);
                ais2StationMsgCount.setEnabled(false);
            }
        });
    }



    public void onClickNext(View view) {
        //Intent mainIntent = new Intent(this, MainActivity.class);
        //startActivity(mainIntent);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialogBoxDisplay();
            }
        });
    }

    @Override
    public void onBackPressed(){

        if (backButtonEnabled){
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
                }
            });

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()){
            case R.id.changeLatLonFormat:
                changeFormat = !changeFormat;
                refreshScreen();
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);

        }
    }



    private class ReadParamsFromDB extends AsyncTask<Void,Void,Boolean> {

        int[] mmsi;

        double[] updateTime;


        @Override
        protected void onPreExecute(){


        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Cursor cursor = null;
            mmsi = new int[DatabaseHelper.INITIALIZATION_SIZE];
            updateTime = new double[DatabaseHelper.INITIALIZATION_SIZE];
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());

            try{
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                cursor = db.query(DatabaseHelper.fixedStationTable,
                        new String[] {DatabaseHelper.mmsi, DatabaseHelper.recvdLatitude, DatabaseHelper.recvdLongitude, DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.sog, DatabaseHelper.cog, DatabaseHelper.updateTime},
                        null, null, null, null, null);
                long stationCount = DatabaseUtils.queryNumEntries(db, DatabaseHelper.stationListTable);
                if (stationCount == DatabaseHelper.INITIALIZATION_SIZE) {
                    if (cursor.moveToFirst()) {
                        Log.d(TAG, "Row Count: " + String.valueOf(cursor.getCount()));
                        int i = 0;
                        do {
                            stationMMSI[i] = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.mmsi));
                            //latitude[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.recvdLatitude));
                            /*Log.d(TAG, String.valueOf(i) + " " + String.valueOf(latitude[i]));
                            Log.d(TAG, "MMSIs: " + String.valueOf(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.mmsi))));*/
                            //longitude[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.recvdLongitude));
                            stationLatitude[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.recvdLatitude));
                            stationLongitude[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.recvdLongitude));
                            if(!isCalledFromCoordinateFragment){
                                predictedLatitude[i] = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.latitude));
                                predictedLongitude[i] = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.longitude));
                            }
                            stationSOG[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.sog));
                            stationCOG[i] = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.cog));
                            if (i == 0 && (cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.updateTime)) > firstStationpreviousUpdateTime)){
                                firstStationpreviousUpdateTime = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.updateTime));
                                firstStationMessageCount++;
                            }else if (i == 1 && (cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.updateTime)) > secondStationpreviousUpdateTime)){
                                secondStationpreviousUpdateTime = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.updateTime));
                                secondStationMessageCount++;
                            }
                            //updateTime[i] = SystemClock.elapsedRealtime() - cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.updateTime));
                            updateTime[i] = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.updateTime));
                            stationTime[i] = new Date((long) updateTime[i]);

                            i++;
                        } while (cursor.moveToNext());
                        cursor.close();
                        //db.close();

                    }

                    return true;
                } else{
                    Log.d(TAG, "Invalid Number of Entries in Station List Table");
                    return false;
                }
            } catch (SQLiteException e){
                return false;
            }finally {
                if (cursor != null){
                    cursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result){
                Log.d(TAG, "ReadParamsFromDB Async Task: Database Unavailable");
            }
        }
    }



    private class InsertXAxisDistance extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                ContentValues stationData = new ContentValues();
                stationData.put(DatabaseHelper.distance, xAxisDistance);
                stationData.put(DatabaseHelper.xPosition, xAxisDistance);
                db.update(DatabaseHelper.fixedStationTable, stationData,
                        DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(stationMMSI[DatabaseHelper.secondStationIndex])});
                return true;
            } catch (SQLiteException e){
                e.printStackTrace();
                Log.d(TAG, "Error Updating Distance in Fixed Station Table");
                return false;
            }

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result){
                Log.d(TAG, "InsertXAxisDistance Async Task: Database Error");
            }
        }
    }
}


