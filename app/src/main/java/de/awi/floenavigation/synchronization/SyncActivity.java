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

public class SyncActivity extends Activity {

    private static final String TAG = "SyncActivity";
    private static final String toastMsg = "Please wait until Sync Finishes";



    private RequestQueue requestQueue;
    private XmlPullParserFactory factory;
    private XmlPullParser parser;

    private UsersSync usersSync;
    private FixedStationSync fixedStationSync;
    private StationListSync stationListSync;
    private WaypointsSync waypointsSync;
    private BaseStationSync baseStationSync;
    private ConfigurationParameterSync parameterSync;
    private BetaSync betaSync;
    private SampleMeasurementSync sampleSync;
    private StaticStationSync staticStationSync;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private boolean backButtonEnabled = true;
    private String hostname;
    private String port;
    private boolean isPushCompleted = false;
    private String msg = "";
    private TextView waitingMsg ;
    boolean isPullCompleted = false;

    public long numOfBaseStations;

    private HashMap<Integer, String> stationNameData = new HashMap<>();
    private HashMap<Integer, Integer> mmsiData = new HashMap<>();

    Context mContext;

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



    private void stopServices(){
        AlphaCalculationService.stopTimer(true);
        AISMessageReceiver.setStopDecoding(true);
        AngleCalculationService.setStopRunnable(true);
        PredictionService.setStopRunnable(true);
        ValidationService.setStopRunnable(true);
        MainActivity.areServicesRunning = false;
    }

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


    @Override
    public void onBackPressed(){

        if (backButtonEnabled){
            Intent mainIntent = new Intent(this, AdminPageActivity.class);
            startActivity(mainIntent);
        }else {
            Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
        }
    }

    private void hideNavigationBar() {

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private boolean readParamsfromDatabase(){
        try {
            dbHelper = DatabaseHelper.getDbInstance(this);
            db = dbHelper.getReadableDatabase();
            String parameterName;
            Cursor parameterCursor = db.query(DatabaseHelper.configParametersTable,
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
        }
    }

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

    public void readMobileStations(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            Cursor mobileStationCursor = db.query(DatabaseHelper.mobileStationTable,
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
        }

    }

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
