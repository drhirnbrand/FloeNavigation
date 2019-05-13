package de.awi.floenavigation.synchronization;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.awi.floenavigation.admin.AdminPageActivity;
import de.awi.floenavigation.services.AlphaCalculationService;
import de.awi.floenavigation.services.AngleCalculationService;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.services.PredictionService;
import de.awi.floenavigation.R;
import de.awi.floenavigation.services.ValidationService;
import de.awi.floenavigation.aismessages.AISMessageReceiver;
import de.awi.floenavigation.initialsetup.SetupActivity;

/**
 * This Activity runs the Synchronization Process between the App and the Sync Server. The Activity uses a separate Sync Class for each
 * object which is to be synchronized. A Java {@link RequestQueue} is created and {@link StringRequest}s are added to the Request Queue
 * to push and Pull the Data from the Server. The Synchronization process is Asynchronous which means that each Sync Class's Push and Pull method run
 * asynchronously and the Synchronization is not done in a specific order.
 * The Activity is configured to read the data from the Database tables and push the data to the Sync Server on pressing the Start button on
 * the UI and then clearing the local Database tables and pulling fresh data from the Sync Server on Pressing the Pull Button on the UI.
 * <p>
 *     The Activity uses the Configuration Parameters {@link DatabaseHelper#sync_server_hostname} and {@link DatabaseHelper#sync_server_port}
 *     to connect to the Sync Server.
 * </p>
 * @see RequestQueue
 * @see XmlPullParser
 * @see XmlPullParserFactory
 * @see StringRequest
 */
public class SyncActivity extends Activity {

    private static final String TAG = "SyncActivity";
    private static final String toastMsg = "Please wait until Sync Finishes";


    /**
     * The Common RequestQueue which is shared by the Sync Classes to run the Synchronization Process.
     */
    private RequestQueue requestQueue;

    /**
     * Default XML Parser Factory which is used to create the XML Parser.
     */
    private XmlPullParserFactory factory;

    /**
     * Default XML parser which is used by the Sync Classes to read the XML Tags in the XML coming from the Sync Server and insert it
     * in to the local database accordingly.
     */
    private XmlPullParser parser;

    /**
     * Sync Class used for Synchronizing {@link Users}s.
     * @see UsersSync
     */
    private UsersSync usersSync;

    /**
     * Sync Class used for Synchronizing {@link FixedStation}s.
     * @see FixedStationSync
     */
    private FixedStationSync fixedStationSync;

    /**
     * Sync Class used for Synchronizing {@link StationList}s.
     * @see StationListSync
     */
    private StationListSync stationListSync;

    /**
     * Sync Class used for Synchronizing {@link Waypoints}s.
     * @see WaypointsSync
     */
    private WaypointsSync waypointsSync;

    /**
     * Sync Class used for Synchronizing {@link BaseStation}s.
     * @see BaseStationSync
     */
    private BaseStationSync baseStationSync;

    /**
     * Sync Class used for Synchronizing {@link ConfigurationParameter}s.
     * @see ConfigurationParameterSync
     */
    private ConfigurationParameterSync parameterSync;

    /**
     * Sync Class used for Synchronizing {@link Beta}.
     * @see BetaSync
     */
    private BetaSync betaSync;

    /**
     * Sync Class used for Synchronizing {@link SampleMeasurement}s.
     * @see SampleMeasurementSync
     */
    private SampleMeasurementSync sampleSync;

    /**
     * Sync Class used for Synchronizing {@link StaticStation}s.
     * @see StaticStationSync
     */
    private StaticStationSync staticStationSync;


    private DatabaseHelper dbHelper;

    /**
     * Default Database object used for reading and inserting the data in to the local database.
     * @see DatabaseHelper
     */
    private SQLiteDatabase db;

    /**
     * The Synchronization process must not be interrupted, so the back button on the tablet is disabled.
     * Local variable used to disable the back button.
     * <code>true</code> when backButton is enabled.
     */
    private boolean backButtonEnabled = true;

    /**
     * The hostname/IP address of the Sync Server. It is set to the value of {@link DatabaseHelper#sync_server_hostname}.
     */
    private String hostname;

    /**
     * The port used on the Sync Server. It is set to the value of {@link DatabaseHelper#sync_server_port}.
     */
    private String port;

    /**
     * <code>true</code> when the Push to the Sync Server is completed.
     */
    private boolean isPushCompleted = false;
    private String msg = "";


    private TextView waitingMsg ;

    /**
     * <code>true</code> when Pull from the Sync Server is completed.
     */
    boolean isPullCompleted = false;

    public long numOfBaseStations;

    /**
     * Stores {@link DatabaseHelper#stationName} of all {@link DatabaseHelper#mobileStationTable}.
     * Use only when Mobile Stations are also synchronized.
     */
    private HashMap<Integer, String> stationNameData = new HashMap<>();

    /**
     * Stores {@link DatabaseHelper#mmsi} of all {@link DatabaseHelper#mobileStationTable}.
     * Use only when Mobile Stations are also synchronized.
     */
    private HashMap<Integer, Integer> mmsiData = new HashMap<>();

    /**
     * Current Application Context
     */
    Context mContext;

    /**
     * Default onCreate method of the Activity. Creates the {@link RequestQueue} and {@link XmlPullParser}.
     * Reads the {@link DatabaseHelper#sync_server_hostname} and {@link DatabaseHelper#sync_server_port} from the local Database.
     * Initializes the Sync Objects by passing them the created {@link RequestQueue}  and {@link XmlPullParser} and then waits for User
     * to push the Start Synchronization button.
     * @param savedInstanceState
     * @throws XmlPullParserException
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        requestQueue = Volley.newRequestQueue(this);
        try {
            factory = XmlPullParserFactory.newInstance();
            parser = factory.newPullParser();
        } catch (XmlPullParserException e){
            Log.d(TAG, "Error create XML Parser");
            e.printStackTrace();
        }
        setContentView(R.layout.activity_sync);
        waitingMsg = findViewById(R.id.syncWaitingMsg);
        readParamsfromDatabase();
        usersSync = new UsersSync(this, requestQueue, parser);
        fixedStationSync = new FixedStationSync(this, requestQueue, parser);
        stationListSync = new StationListSync(this, requestQueue, parser);
        waypointsSync = new WaypointsSync(this, requestQueue, parser);
        baseStationSync = new BaseStationSync(this, requestQueue, parser);
        parameterSync = new ConfigurationParameterSync(this, requestQueue, parser);
        betaSync = new BetaSync(this, requestQueue, parser);
        sampleSync = new SampleMeasurementSync(this, requestQueue, parser);
        staticStationSync = new StaticStationSync(this, requestQueue, parser);
    }

    /**
     * Callback function for Start Sync Button.
     * Reads the Sync Server parameters from the database and tries to ping the Sync Server. If ping fails it displays an error message and
     * and the Synchronization process is not run. If Sync Server is available it changes the Screen layout, stops the background {@link de.awi.floenavigation.services}s,
     * and checks if the coordinate system is initialized.
     *
     * The NavigationBar at the bottom of the screen and the hard back button are disabled during this process.
     * <p>
     *     If the Coordinate System is already setup the {@link DatabaseHelper#mobileStationTable} is cleared and the data for the other tables
     *     is read from the local database and pushed in to the {@link RequestQueue} using the individual Sync Objects for each table.
     *     The Screen Layout is changed to show a waiting view with a Pull Button and {@link #isPushCompleted} is set to <code>true</code> and {@link #isPullCompleted} is set to <code>false</code>.
     *
     *     If the Coordinate System is not setup, the Push process is skipped and the App tries to pull the data from the Sync Server. The Screen Layout is changed
     *     to show a Finish button and {@link #isPushCompleted} is set to <code>false</code> and {@link #isPullCompleted} is set to <code>true</code>.
     * </p>
     *
     * @param view
     */
    public void onClickStartSync(View view) {
        if(readParamsfromDatabase()) {
            hideNavigationBar();
            backButtonEnabled = false;
            dbHelper = DatabaseHelper.getDbInstance(this);
            db = dbHelper.getReadableDatabase();
            numOfBaseStations = DatabaseUtils.queryNumEntries(db, DatabaseHelper.baseStationTable);
            if (pingRequest(hostname)) {
                findViewById(R.id.syncWelcomeScreen).setVisibility(View.GONE);
                findViewById(R.id.syncWaitingView).setVisibility(View.VISIBLE);
                msg = "Contacting Server....";
                waitingMsg.setText(msg);
                msg = "Stopping Services and Clearing Metadata";
                waitingMsg.setText(msg);
                //clearMobileStationTable();
                setBaseUrl(hostname, port);
                stopServices();
                if (numOfBaseStations == 2) {
                    readMobileStations();
                    sendMobileStations();
                    clearMobileStationTable();
                    msg = "Reading Fixed Stations from Database and Pushing it to the Server";
                    waitingMsg.setText(msg);
                    fixedStationSync.onClickFixedStationReadButton();
                    fixedStationSync.onClickFixedStationSyncButton();

                    msg = "Reading Base Stations from Database and Pushing it to the Server";
                    waitingMsg.setText(msg);
                    baseStationSync.onClickBaseStationReadButton();
                    baseStationSync.onClickBaseStationSyncButton();

                    msg = "Reading AIS Station List from Database and Pushing it to the Server";
                    waitingMsg.setText(msg);
                    stationListSync.onClickStationListReadButton();
                    stationListSync.onClickStationListSyncButton();

                    msg = "Reading Beta Table from Database and Pushing it to the Server";
                    waitingMsg.setText(msg);
                    betaSync.onClickBetaReadButton();
                    betaSync.onClickBetaSyncButton();

                    msg = "Reading Users from Database and Pushing it to the Server";
                    waitingMsg.setText(msg);
                    usersSync.setBaseUrl(hostname, port);
                    usersSync.onClickUserReadButton();
                    usersSync.onClickUserSyncButton();

                    msg = "Reading Samples from Database and Pushing it to the Server";
                    waitingMsg.setText(msg);
                    sampleSync.onClickSampleReadButton();
                    sampleSync.onClickSampleSyncButton();

                    msg = "Reading Static Stations from Database and Pushing it to the Server";
                    waitingMsg.setText(msg);
                    staticStationSync.onClickStaticStationReadButton();
                    staticStationSync.onClickStaticStationSyncButton();

                    msg = "Reading Waypoints from Database and Pushing it to the Server";
                    waitingMsg.setText(msg);
                    waypointsSync.onClickWaypointsReadButton();
                    waypointsSync.onClickWaypointsSyncButton();

                    msg = "Reading Configuration Parameters from Database and Pushing it to the Server";
                    waitingMsg.setText(msg);
                    parameterSync.onClickParameterReadButton();
                    parameterSync.onClickParameterSyncButton();

                    //findViewById(R.id.syncProgressBar).setVisibility(View.GONE);
                    msg = "Push to Server Completed. Press Pull from Server only after Pushing Data from all tablets to the Server";
                    waitingMsg.setText(msg);
                    Button confirmBtn = findViewById(R.id.syncFinishBtn);
                    confirmBtn.setVisibility(View.VISIBLE);
                    confirmBtn.setClickable(true);
                    confirmBtn.setEnabled(true);
                    isPushCompleted = true;
                    isPullCompleted = false;
                } else {
                    //Pull Request only
                    pullDatafromServer();
                    isPullCompleted = true;
                    isPushCompleted = false;
                    Button confirmBtn = findViewById(R.id.syncFinishBtn);
                    confirmBtn.setVisibility(View.VISIBLE);
                    confirmBtn.setClickable(true);
                    confirmBtn.setEnabled(true);
                    confirmBtn.setText(R.string.syncFinish);
                    msg = "Sync Completed";
                    waitingMsg.setText(msg);
                    findViewById(R.id.syncProgressBar).setVisibility(View.GONE);
                }

            } else {
                findViewById(R.id.syncWelcomeScreen).setVisibility(View.VISIBLE);
                findViewById(R.id.syncWaitingView).setVisibility(View.GONE);
                Toast.makeText(this, "Could not contact the Server. Please check the Connection", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            findViewById(R.id.syncWelcomeScreen).setVisibility(View.VISIBLE);
            findViewById(R.id.syncWaitingView).setVisibility(View.GONE);
            Toast.makeText(this, "Error Reading Server Details from Database", Toast.LENGTH_SHORT).show();
        }



    }

    /**
     * Function to stop the background services and sets {@link MainActivity#areServicesRunning} to false.
     */
    private void stopServices(){
        AlphaCalculationService.stopTimer(true);
        AISMessageReceiver.setStopDecoding(true);
        AngleCalculationService.setStopRunnable(true);
        PredictionService.setStopRunnable(true);
        ValidationService.setStopRunnable(true);
        MainActivity.areServicesRunning = false;
    }

    /**
     * Clears the {@link DatabaseHelper#mobileStationTable} Table.
     */
    private void clearMobileStationTable(){
        try {
            dbHelper = DatabaseHelper.getDbInstance(this);
            db = dbHelper.getReadableDatabase();
            db.execSQL("Delete from " + DatabaseHelper.mobileStationTable);
        } catch (SQLException e){
            Log.d(TAG, "Error Clearing Mobile Station Database");
            Toast.makeText(this, "Error Clearing Mobile Station Database", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Sets the Base URLs for pushing, pulling and deleting data on the Sync Server.
     * @param host the hostname/IP of the Sync Serer
     * @param port the port used by the Sync Server
     */
    private void setBaseUrl(String host, String port){
        fixedStationSync.setBaseUrl(host, port);
        baseStationSync.setBaseUrl(host, port);
        stationListSync.setBaseUrl(host, port);
        betaSync.setBaseUrl(host, port);
        usersSync.setBaseUrl(host, port);
        waypointsSync.setBaseUrl(host, port);
        staticStationSync.setBaseUrl(host, port);
        sampleSync.setBaseUrl(host, port);
        parameterSync.setBaseUrl(host, port);
    }

    /**
     * Checks if the Sync Server is reachable on the network.
     * @param host the hostname/IP of the Sync Server
     * @return <code>true</code> if the Sync Server is reachable
     */
    private boolean pingRequest(String host){

        boolean mExitValue = false;
        Log.d(TAG, "Hostname: " + host);

        try {
            mExitValue = InetAddress.getByName(host).isReachable(1000);
            Log.d(TAG, "Ping Result: " + mExitValue);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mExitValue;

    }

    /**
     * Overrides the normal back button behavior. If the {@link #backButtonEnabled} is <code>true</code> it will return to {@link MainActivity}
     * otherwise it will just show a {@link Toast}.
     */
    @Override
    public void onBackPressed(){

        if (backButtonEnabled){
            Intent mainIntent = new Intent(this, AdminPageActivity.class);
            startActivity(mainIntent);
        }else {
            Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Hides the hard Navigation buttons at the bottom of the tablet during Synchronization process
     */
    private void hideNavigationBar() {

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     *  Reads the {@link DatabaseHelper#sync_server_hostname} and {@link DatabaseHelper#sync_server_port} from the local Database.
     */
    private boolean readParamsfromDatabase(){
        Cursor parameterCursor = null;
        try {
            dbHelper = DatabaseHelper.getDbInstance(this);
            db = dbHelper.getReadableDatabase();
            String parameterName;
            parameterCursor = db.query(DatabaseHelper.configParametersTable,
                    new String[] {DatabaseHelper.parameterName, DatabaseHelper.parameterValue},
                    DatabaseHelper.parameterName + " = ? OR " + DatabaseHelper.parameterName + " = ?" ,
                    new String[] {DatabaseHelper.sync_server_hostname, DatabaseHelper.sync_server_port},
                    null, null, null);

                if(parameterCursor.moveToFirst()){
                    do {
                        parameterName = parameterCursor.getString(parameterCursor.getColumnIndexOrThrow(DatabaseHelper.parameterName));
                        switch (parameterName) {
                            case DatabaseHelper.sync_server_hostname:
                                hostname = parameterCursor.getString(parameterCursor.getColumnIndexOrThrow(DatabaseHelper.parameterValue));
                                break;

                            case DatabaseHelper.sync_server_port:
                                port = parameterCursor.getString(parameterCursor.getColumnIndexOrThrow(DatabaseHelper.parameterValue));
                                break;
                        }
                    } while (parameterCursor.moveToNext());
                }
                parameterCursor.close();
                return true;
        } catch(SQLException e){
            Log.d(TAG, "Error Reading Server Details from Database");

            e.printStackTrace();
            return false;
        }finally {
            if (parameterCursor != null){
                parameterCursor.close();
            }
        }
    }

    /**
     * Callback function for Start Pull from Server Button.
     * If data has already been push to the RequestQueue, it starts adding the Pull requests to the RequestQueue and changes the layout to display the finish
     * button.
     * If Pull Requests have already been added to the the RequestQueue, that means the synchronization process is complete and it restarts the
     * background Services and returns to the {@link AdminPageActivity}.
     * @param view
     */
    public void onClickProgressBarButton(View view) {

        if(isPushCompleted){
            pullDatafromServer();
            Button finishBtn = findViewById(R.id.syncFinishBtn);
            finishBtn.setText(R.string.syncFinish);
            msg = "Sync Completed";
            waitingMsg.setText(msg);
            findViewById(R.id.syncProgressBar).setVisibility(View.GONE);
            isPullCompleted = true;
            isPushCompleted = false;
        }

        if(isPullCompleted){

            isPullCompleted = false;
            new RestartServices().execute();
            Intent configActivity = new Intent(this, AdminPageActivity.class);
            startActivity(configActivity);
        }
    }

    /**
     * Clears the local database and starts adding the Pull requests to the {@link #requestQueue} for each database table which is to be synced.
     */
    private void pullDatafromServer(){

        msg = "Clearing Database and Pulling Fixed Stations from the Server";
        waitingMsg.setText(msg);
        fixedStationSync.onClickFixedStationPullButton();


        msg = "Clearing Database and Pulling Base Stations from the Server";
        waitingMsg.setText(msg);
        baseStationSync.onClickBaseStationPullButton();


        msg = "Clearing Database and Pulling AIS Station List from the Server";
        waitingMsg.setText(msg);
        stationListSync.onClickStationListPullButton();


        msg = "Clearing Database and Pulling Beta from the Server";
        waitingMsg.setText(msg);
        betaSync.onClickBetaPullButton();



        msg = "Clearing Database and Pulling Users from the Server";
        waitingMsg.setText(msg);
        usersSync.onClickUserPullButton();



        msg = "Clearing Database and Pulling Device List from the Server";
        waitingMsg.setText(msg);
        sampleSync.onClickDeviceListPullButton();


        msg = "Clearing Database and Pulling Static Stations from the Server";
        waitingMsg.setText(msg);
        staticStationSync.onClickStaticStationPullButton();


        msg = "Clearing Database and Pulling Waypoints from the Server";
        waitingMsg.setText(msg);
        waypointsSync.onClickWaypointsPullButton();


        msg = "Clearing Database and Pulling Configuration Parameters from the Server";
        waitingMsg.setText(msg);
        parameterSync.onClickParameterPullButton(numOfBaseStations);
    }

    /**
     * Reads the {@value DatabaseHelper#mobileStationTable} Table and inserts the data from all the Columns of the
     * {@value DatabaseHelper#mobileStationTable} Table in to their respective {@link HashMap}.
     * @throws SQLiteException In case of error in reading database.
     */
    public void readMobileStations(){
        Cursor mobileStationCursor = null;
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            mobileStationCursor = db.query(DatabaseHelper.mobileStationTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(mobileStationCursor.moveToFirst()){
                do{
                    stationNameData.put(i, mobileStationCursor.getString(mobileStationCursor.getColumnIndexOrThrow(DatabaseHelper.stationName)));
                    mmsiData.put(i, mobileStationCursor.getInt(mobileStationCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));

                    i++;

                }while (mobileStationCursor.moveToNext());
            }
            mobileStationCursor.close();
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }finally {
            if (mobileStationCursor != null){
                mobileStationCursor.close();
            }
        }

    }

    /**
     * Creates {@link StringRequest}s as per the size of {@link #mmsiData} data extracted from the local database and inserts all the requests in the {@link RequestQueue}
     * A Stringrequest for pushing the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     */
    public void sendMobileStations(){
        for(int i = 0; i < mmsiData.size(); i++){
            final int index = i;
            StringRequest request;
            String URL = "http://192.168.137.1:80/pullMobileStation.php";
            request = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.names().get(0).equals("success")) {
                            //Toast.makeText(mContext, "SUCCESS " + jsonObject.getString("success"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "SUCCESS: " + jsonObject.getString("success"));
                        } else {
                            Toast.makeText(mContext, "Error" + jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error: " + jsonObject.getString("error"));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String,String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.stationName,(stationNameData.get(index) == null)? "" : stationNameData.get(index));
                    hashMap.put(DatabaseHelper.mmsi,(mmsiData.get(index) == null)? "" : mmsiData.get(index).toString());

                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
        //sendSLDeleteRequest();
    }

    /**
     * Async Task for restarting the Background services. It checks if the Pull requests have been added to the {@link #requestQueue} before starting the Services.
     */
    private class RestartServices extends AsyncTask<Void,Void,Void> {

        Timer timer = new Timer();
        private final int TIMER_PERIOD = 10 * 1000;
        private final int TIMER_DELAY = 0;


        @Override
        protected void onPreExecute(){

        }

        @Override
        protected Void doInBackground(Void... voids) {

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(baseStationSync.getDataCompleted() && fixedStationSync.getDataCompleted() && betaSync.getDataCompleted() && stationListSync.getDataCompleted() ){
                        Log.d(TAG, "Pull Requests Completed. Starting Services");
                        AISMessageReceiver.setStopDecoding(false);
                        SetupActivity.runServices(mContext);

                        timer.cancel();

                    } else{
                        Log.d(TAG, "Pull not Completed yet.");
                    }
                }
            }, TIMER_DELAY, TIMER_PERIOD);

            return null;

        }

    }
}
