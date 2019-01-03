package de.awi.floenavigation.grid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.scalified.fab.ActionButton;

import de.awi.floenavigation.aismessages.AISDecodingService;
import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.R;


public class GridActivity extends Activity implements View.OnClickListener{

    private static final int ASYNC_TASK_TIMER_PERIOD = 8 * 1000;
    private static final int ASYNC_TASK_TIMER_DELAY = 0;
    private static final int SCREEN_REFRESH_TIMER_PERIOD = 10 * 1000;
    private static final int SCREEN_REFRESH_TIMER_DELAY = 0;
    private static final String TAG = "GridActivity";


    private BroadcastReceiver gpsBroadcastReceiver;
    private double tabletLat;
    private double tabletLon;
    private double originLatitude;
    private double originLongitude;
    private double originX;
    private double originY;
    private int originMMSI;
    private double beta;
    private double tabletX;
    private double tabletY;
    private double tabletDistance;
    private double tabletTheta;
    private double tabletAlpha;

    public static boolean showFixedStation = true;
    public static boolean showMobileStation = true;
    public static boolean showStaticStation = true;
    public static boolean showWaypointStation = true;
    //private double[] mFixedStationXs;
    public static HashMap<Integer, Double> mFixedStationXs;
    //private double[] mFixedStationYs;
    public static HashMap<Integer, Double> mFixedStationYs;
    //private int[] mFixedStationMMSIs;
    public static HashMap<Integer, Integer> mFixedStationMMSIs;
    public static HashMap<Integer, String> mFixedStationNames;
    //private double[] mMobileStationXs;
    public static HashMap<Integer, Double> mMobileStationXs;
    //private double[] mMobileStationYs;
    public static HashMap<Integer, Double> mMobileStationYs;
    //private int[] mMobileStationMMSIs;
    public static HashMap<Integer, Integer> mMobileStationMMSIs;
    public static HashMap<Integer, String> mMobileStationNames;
    //private double[] mStaticStationXs;
    public static HashMap<Integer, Double> mStaticStationXs;
    //private double[] mStaticStationYs;
    public static HashMap<Integer, Double> mStaticStationYs;
    //private String[] mStaticStationNames;
    public static HashMap<Integer, String> mStaticStationNames;
    //private double[] mWaypointsXs;
    public static HashMap<Integer, Double> mWaypointsXs;
    //private double[] mWaypointsYs;
    public static HashMap<Integer, Double> mWaypointsYs;
    //private String[] mWaypointsLabels;
    public static HashMap<Integer, String> mWaypointsLabels;

    private static final double scale = 500;
    private LocationManager locationManager;
    private Timer asyncTaskTimer;


    //Action Bar Updates
    private BroadcastReceiver aisPacketBroadcastReceiver;
    private boolean locationStatus = false;
    private boolean packetStatus = false;
    private final Handler statusHandler = new Handler();
    private MenuItem gpsIconItem, aisIconItem, emptyIcon, gridSetupIconItem;
    private MapView myGridView;
    private View myView;
    private ActionButton buttonView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid);

        myGridView = (MapView) findViewById(R.id.GridView);
        myView = this.findViewById(R.id.GridView);
        buttonView = this.findViewById(R.id.action_button);
        buttonView.setOnClickListener(this);

        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.myLayout);
        BubbleDrawable myBubble = new BubbleDrawable(BubbleDrawable.CENTER);
        myBubble.setCornerRadius(20);
        myBubble.setPointerAlignment(BubbleDrawable.CENTER);
        myBubble.setPadding(25, 25, 25, 25);
        myGridView.setBubbleLayout(linearLayout, myBubble);

    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "LifeCycle OnStart");
        asyncTaskTimer = new Timer();
        mFixedStationXs = new HashMap<>();
        mFixedStationYs = new HashMap<>();
        mFixedStationMMSIs = new HashMap<>();
        mFixedStationNames = new HashMap<>();
        mMobileStationXs = new HashMap<>();
        mMobileStationYs = new HashMap<>();
        mMobileStationMMSIs = new HashMap<>();
        mMobileStationNames = new HashMap<>();
        mStaticStationXs = new HashMap<>();
        mStaticStationYs = new HashMap<>();
        mStaticStationNames = new HashMap<>();
        mWaypointsXs = new HashMap<>();
        mWaypointsYs = new HashMap<>();
        mWaypointsLabels = new HashMap<>();
        myView.postInvalidateOnAnimation();

        //Broadcast receiver for tablet location
        myGridView.initRefreshTimer();
        //initializeArrayList();
        actionBarUpdatesFunction();
        new ReadStaticStationsFromDB().execute();
        new ReadWaypointsFromDB().execute();
        new ReadOriginFromDB().execute();
        new ReadFixedStationsFromDB().execute();
        new ReadMobileStationsFromDB().execute();

        asyncTaskTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                new ReadOriginFromDB().execute();
                new ReadFixedStationsFromDB().execute();
                new ReadMobileStationsFromDB().execute();

            }
        }, ASYNC_TASK_TIMER_DELAY, ASYNC_TASK_TIMER_PERIOD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "LifeCycle OnResume");
        myView.postInvalidateOnAnimation();

        //Broadcast receiver for tablet location
        myGridView.initRefreshTimer();
        //initializeArrayList();

        asyncTaskTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                new ReadOriginFromDB().execute();
                new ReadFixedStationsFromDB().execute();
                new ReadMobileStationsFromDB().execute();

            }
        }, ASYNC_TASK_TIMER_DELAY, ASYNC_TASK_TIMER_PERIOD);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "LifeCycle OnPause");

    }

    /*private void initializeArrayList() {
        DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(getApplicationContext());
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        long numOfEntries = DatabaseUtils.queryNumEntries(db, DatabaseHelper.fixedStationTable);
        for(int i = 0; i < (int)numOfEntries; i++){
            Log.d(TAG, "FixedStnIndex " + String.valueOf(i));
            mFixedStationMMSIs.add(i, 0);
            mFixedStationXs.add(i, (float) 0.0);
            mFixedStationYs.add(i, (float) 0.0);
        }
    }*/


    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "LifeCycle OnRestart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "LifeCycle OnStop");
        unregisterReceiver(gpsBroadcastReceiver);
        gpsBroadcastReceiver = null;
        unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
        MapView.refreshScreenTimer.cancel();
        asyncTaskTimer.cancel();


    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "Lifecycle OnDestroy");
        asyncTaskTimer.cancel();
        MapView.refreshScreenTimer.cancel();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu, menu);
        getMenuInflater().inflate(R.menu.grid_menu, menu);

        int[] iconItems = {R.id.gridSetupComplete, R.id.currentLocationAvail, R.id.aisPacketAvail};
        gridSetupIconItem = menu.findItem(iconItems[0]);
        gridSetupIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        gpsIconItem = menu.findItem(iconItems[1]);
        gpsIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        aisIconItem = menu.findItem(iconItems[2]);
        aisIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        emptyIcon = menu.findItem(R.id.empty);
        emptyIcon.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);

        if(MainActivity.numOfBaseStations >= DatabaseHelper.INITIALIZATION_SIZE) {
            if (gridSetupIconItem != null) {
                gridSetupIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorGreen), PorterDuff.Mode.SRC_IN);
            }
        } else{
            if (gridSetupIconItem != null) {
                gridSetupIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorRed), PorterDuff.Mode.SRC_IN);
            }
        }

        MenuItem fixedStationItem = menu.findItem(R.id.FixedStation);
        fixedStationItem.setChecked(showFixedStation);
        MenuItem mobileStationItem = menu.findItem(R.id.MobileStation);
        mobileStationItem.setChecked(showMobileStation);
        MenuItem staticStationItem = menu.findItem(R.id.StaticStation);
        staticStationItem.setChecked(showStaticStation);
        MenuItem waypointsStationItem = menu.findItem(R.id.Waypoint);
        waypointsStationItem.setChecked(showWaypointStation);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onBackPressed(){
        Log.d(TAG, "BackPressed");
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       switch (item.getItemId()) {
           case android.R.id.home:
                onBackPressed();
                return true;

           case R.id.FixedStation:
               showFixedStation = !showFixedStation;
               item.setChecked(showFixedStation);
               myView.postInvalidateOnAnimation();
               //ViewCompat.postInvalidateOnAnimation(myGridView);
               return true;

           case R.id.MobileStation:
               showMobileStation = !showMobileStation;
               item.setChecked(showMobileStation);
               myView.postInvalidateOnAnimation();
               //ViewCompat.postInvalidateOnAnimation(myGridView);
               return true;

           case R.id.StaticStation:
               showStaticStation = !showStaticStation;
               item.setChecked(showStaticStation);
               myView.postInvalidateOnAnimation();
               //ViewCompat.postInvalidateOnAnimation(myGridView);
               return true;

           case R.id.Waypoint:
               showWaypointStation = !showWaypointStation;
               item.setChecked(showWaypointStation);
               myView.postInvalidateOnAnimation();
               //ViewCompat.postInvalidateOnAnimation(myGridView);
               return true;



        }
        return super.onOptionsItemSelected(item);
    }



    private void calculateTabletGridCoordinates(){
        if (tabletLat == 0.0){
            try {
                if (getLastKnownLocation() != null) {
                    tabletLat = getLastKnownLocation().getLatitude();
                }
            } catch (SecurityException e){
                Toast.makeText(this,"Location Service Problem", Toast.LENGTH_LONG).show();
            }
        }
        if (tabletLon == 0.0){
            try {
                if(getLastKnownLocation() != null) {
                    tabletLon = getLastKnownLocation().getLongitude();
                }
            } catch (SecurityException e){
                Toast.makeText(this,"Location Service Problem", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Location Service Not Available");
                e.printStackTrace();
            }
        }
        tabletDistance = NavigationFunctions.calculateDifference(tabletLat, tabletLon, originLatitude, originLongitude);
        //Log.d(TAG + "TabletParam", "TabletLat: " + String.valueOf(tabletLat)+ " TabletLon: "+ String.valueOf(tabletLon));
        //Log.d(TAG + "TabletParam", "OriginLat: " + String.valueOf(originLatitude)+ " OriginLon: " + String.valueOf(originLongitude));
        tabletTheta = NavigationFunctions.calculateAngleBeta(tabletLat, tabletLon, originLatitude, originLongitude);
        //Log.d(TAG + "TabletParam", "TabletDistance: " + String.valueOf(tabletDistance));
        tabletAlpha = Math.abs(tabletTheta - beta);
        tabletX = tabletDistance * Math.cos(Math.toRadians(tabletAlpha));
        tabletY = tabletDistance * Math.sin(Math.toRadians(tabletAlpha));
        Log.d(TAG, "tabletX " + tabletX);
        myGridView.setTabletX(tabletX);
        myGridView.setTabletY(tabletY);
        myGridView.setTabletLat(tabletLat);
        myGridView.setTabletLon(tabletLon);
    }



    private float translateCoord(double coordinate){
        float result = (float) (coordinate / scale);
        return result;
    }

    private void actionBarUpdatesFunction() {

        ///*****************ACTION BAR UPDATES*************************/
        if(gpsBroadcastReceiver == null){
            gpsBroadcastReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent){
                    //Log.d(TAG, "BroadCast Received");
                    /*String coordinateString = intent.getExtras().get("coordinates").toString();
                    String[] coordinates = coordinateString.split(",");*/
                    tabletLat = intent.getExtras().getDouble(GPS_Service.latitude);
                    tabletLon = intent.getExtras().getDouble(GPS_Service.longitude);
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
                    //tabletTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());

                    //Log.d(TAG, "Tablet Lat: " + String.valueOf(tabletLat));
                    //Log.d(TAG, "Tablet Lon: " + String.valueOf(tabletLon));
                    //Toast.makeText(getActivity(),"Received Broadcast", Toast.LENGTH_LONG).show();
                    //populateTabLocation();
                    calculateTabletGridCoordinates();
                }
            };
        }


        if (aisPacketBroadcastReceiver == null){
            aisPacketBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    packetStatus = intent.getExtras().getBoolean(GPS_Service.AISPacketStatus);
                }
            };
        }

        registerReceiver(aisPacketBroadcastReceiver, new IntentFilter(GPS_Service.AISPacketBroadcast));
        registerReceiver(gpsBroadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

        Runnable gpsLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (locationStatus){
                    if (gpsIconItem != null)
                        gpsIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorGreen), PorterDuff.Mode.SRC_IN);
                }
                else {
                    if (gpsIconItem != null)
                        gpsIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorRed), PorterDuff.Mode.SRC_IN);
                }
                if (packetStatus){
                    if (aisIconItem != null)
                        aisIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorGreen), PorterDuff.Mode.SRC_IN);
                }else {
                    if (aisIconItem != null)
                        aisIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorRed), PorterDuff.Mode.SRC_IN);
                }

                statusHandler.postDelayed(this, ActionBarActivity.UPDATE_TIME);
            }
        };

        statusHandler.postDelayed(gpsLocationRunnable, ActionBarActivity.UPDATE_TIME);
        ///******************************************/
    }

    public void onClickCenterButton(View view) {

        Log.d(TAG, "Pressed Detected onClick Function");
        //myGridView.onClickOriginButton(myView);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.action_button:
                Log.d(TAG,"Button Click");
                myGridView.resetContentRect();
                break;

        }

    }

    private class ReadOriginFromDB extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                int i = 0;
                DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor baseStationCursor = db.query(DatabaseHelper.baseStationTable,
                        new String[] {DatabaseHelper.mmsi},
                        DatabaseHelper.isOrigin + " = ?",
                        new String[]{String.valueOf(DatabaseHelper.ORIGIN)},
                        null, null, null);
                Log.d(TAG, String.valueOf(baseStationCursor.getCount()));
                if (baseStationCursor.getCount() != 1){
                    Log.d(TAG, "Error Reading from BaseStation Table");
                    return false;
                } else{
                    if(baseStationCursor.moveToFirst()){
                        originMMSI = baseStationCursor.getInt(baseStationCursor.getColumnIndex(DatabaseHelper.mmsi));
                    }
                }

                /*if(baseStationCursor.moveToFirst()) {
                    do {
                        int mmsi = baseStationCursor.getInt(baseStationCursor.getColumnIndex(DatabaseHelper.mmsi));

                        Log.d(TA    G, "MMSIs: " + String.valueOf(i) + " " + String.valueOf(mmsi));
                        i++;
                    } while (baseStationCursor.moveToNext());
                }*/

                Cursor fixedStationCursor = db.query(DatabaseHelper.fixedStationTable,
                        new String[] {DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
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
                        originX = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.xPosition));
                        originY = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.yPosition));
                        myGridView.setOriginX(originX);
                        myGridView.setOriginY(originY);
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
        protected void onPostExecute(Boolean result){
            if(!result){
                Log.d(TAG, "ReadOriginFromDB AsyncTask Error");
            }
        }

    }

    private class ReadFixedStationsFromDB extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor mFixedStnCursor;
                //double xPosition, yPosition;
                //int mmsi;

                mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.xPosition, DatabaseHelper.yPosition, DatabaseHelper.stationName},
                        null,
                        null,
                        null, null, null, null);
                //mFixedStationMMSIs = new int[mFixedStnCursor.getCount()];
                //mFixedStationXs = new double[mFixedStnCursor.getCount()];
                //mFixedStationYs = new double[mFixedStnCursor.getCount()];
                mFixedStationMMSIs.clear();
                mFixedStationXs.clear();
                mFixedStationYs.clear();
                mFixedStationNames.clear();
                MapView.clearFixedStationHashTables();
                if (mFixedStnCursor.moveToFirst()) {
                    for (int i = 0; i < mFixedStnCursor.getCount(); i++) {
                        Log.d(TAG, "FixedStnIndex " + String.valueOf(i));
                        mFixedStationMMSIs.put(i, mFixedStnCursor.getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi)));
                        mFixedStationXs.put(i, mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.xPosition)));
                        mFixedStationYs.put(i, mFixedStnCursor.getDouble(mFixedStnCursor.getColumnIndex(DatabaseHelper.yPosition)));
                        mFixedStationNames.put(i, mFixedStnCursor.getString(mFixedStnCursor.getColumnIndex(DatabaseHelper.stationName)));
                        mFixedStnCursor.moveToNext();
                    }
                    mFixedStnCursor.close();
                    return true;
                } else {
                    mFixedStnCursor.close();
                    Log.d(TAG, "FixedStationTable Cursor Error");
                    return false;
                }
            } catch (SQLiteException e) {
                Log.d(TAG, "Error reading database");
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result){
            if(!result){
                Log.d(TAG, "ReadFixedStationParams AsyncTask Error");
            }
            else{
                myGridView.setmFixedStationMMSIs(mFixedStationMMSIs);
                myGridView.setmFixedStationXs(mFixedStationXs);
                myGridView.setmFixedStationYs(mFixedStationYs);
                myGridView.setmFixedStationNamess(mFixedStationNames);
            }
        }
    }

    private class ReadMobileStationsFromDB extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor mMobileStnCursor;
                //double xPosition, yPosition;
                //int mmsi;

                mMobileStnCursor = db.query(DatabaseHelper.mobileStationTable,
                        new String[]{DatabaseHelper.mmsi, DatabaseHelper.stationName, DatabaseHelper.xPosition, DatabaseHelper.yPosition, DatabaseHelper.isCalculated},
                        DatabaseHelper.isCalculated + " = ?",
                        new String[] {Integer.toString(DatabaseHelper.MOBILE_STATION_IS_CALCULATED)},
                        null, null, null, null);
                //mMobileStationXs = new double[mMobileStnCursor.getCount()];
                //mMobileStationYs = new double[mMobileStnCursor.getCount()];
                //mMobileStationMMSIs = new int[mMobileStnCursor.getCount()];
                mMobileStationMMSIs.clear();
                mMobileStationXs.clear();
                mMobileStationYs.clear();
                mMobileStationNames.clear();
                MapView.clearMobileStationHashTables();
                if (mMobileStnCursor.moveToFirst()) {
                    for (int i = 0; i < mMobileStnCursor.getCount(); i++) {
                        mMobileStationMMSIs.put(i, mMobileStnCursor.getInt(mMobileStnCursor.getColumnIndex(DatabaseHelper.mmsi)));
                        mMobileStationXs.put(i, mMobileStnCursor.getDouble(mMobileStnCursor.getColumnIndex(DatabaseHelper.xPosition)));
                        mMobileStationYs.put(i, mMobileStnCursor.getDouble(mMobileStnCursor.getColumnIndex(DatabaseHelper.yPosition)));
                        mMobileStationNames.put(i, mMobileStnCursor.getString(mMobileStnCursor.getColumnIndex(DatabaseHelper.stationName)));
                        //Log.d(TAG, "Mobile Station MMSI: " + mMobileStationMMSIs.get(i) + " X: " + mMobileStationXs.get(i) + " Y: " + mMobileStationYs.get(i));
                        mMobileStnCursor.moveToNext();
                    }
                    mMobileStnCursor.close();
                    return true;
                } else {
                    mMobileStnCursor.close();
                    Log.d(TAG, "MobileStation Cursor Error");
                    return false;
                }
            } catch (SQLiteException e) {
                Log.d(TAG, "Error reading database");
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result){
            if(!result){
                Log.d(TAG, "ReadMobileStationFromDB AsyncTask Error");
            } else{
                myGridView.setmMobileStationMMSIs(mMobileStationMMSIs);
                myGridView.setmMobileStationXs(mMobileStationXs);
                myGridView.setmMobileStationYs(mMobileStationYs);
                myGridView.setmMobileStationNamess(mMobileStationNames);
            }
        }
    }

    private class ReadStaticStationsFromDB extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor mStaticStationCursor;
                //double xPosition, yPosition;
                //int mmsi;

                mStaticStationCursor = db.query(DatabaseHelper.staticStationListTable, new String[]{DatabaseHelper.staticStationName, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
                        null,
                        null,
                        null, null, null, null);
                //mStaticStationXs = new double[mStaticStationCursor.getCount()];
                //mStaticStationYs = new double[mStaticStationCursor.getCount()];
                //mStaticStationNames = new String[mStaticStationCursor.getCount()];
                mStaticStationNames.clear();
                mStaticStationXs.clear();
                mStaticStationYs.clear();
                MapView.clearStaticStationHashTables();
                if (mStaticStationCursor.moveToFirst()) {
                    for(int i = 0; i < mStaticStationCursor.getCount(); i++){
                        mStaticStationNames.put(i, mStaticStationCursor.getString(mStaticStationCursor.getColumnIndex(DatabaseHelper.staticStationName)));
                        mStaticStationXs.put(i, mStaticStationCursor.getDouble(mStaticStationCursor.getColumnIndex(DatabaseHelper.xPosition)));
                        mStaticStationYs.put(i, mStaticStationCursor.getDouble(mStaticStationCursor.getColumnIndex(DatabaseHelper.yPosition)));
                        mStaticStationCursor.moveToNext();
                    }
                    mStaticStationCursor.close();
                    return true;
                }
                else {
                    mStaticStationCursor.close();
                    Log.d(TAG, "StaticStation Cursor Error");
                    return false;
                }

            } catch (SQLiteException e) {
                Log.d(TAG, "Error reading database");
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result){
            if(!result){
                Log.d(TAG, "ReadStaticStationFromDB AsyncTask Error");
            } else{
                myGridView.setmStaticStationNamess(mStaticStationNames);
                myGridView.setmStaticStationXs(mStaticStationXs);
                myGridView.setmStaticStationYs(mStaticStationYs);
            }
        }
    }

    private class ReadWaypointsFromDB extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor mWaypointsCursor;
                //double xPosition, yPosition;
                //int mmsi;

                mWaypointsCursor = db.query(DatabaseHelper.waypointsTable, new String[]{DatabaseHelper.labelID, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
                        null,
                        null,
                        null, null, null, null);
                //mWaypointsXs = new double[mWaypointsCursor.getCount()];
                //mWaypointsYs = new double[mWaypointsCursor.getCount()];
                //mWaypointsLabels = new String[mWaypointsCursor.getCount()];
                mWaypointsLabels.clear();
                mWaypointsXs.clear();
                mWaypointsYs.clear();
                MapView.clearWaypointHashTables();
                if (mWaypointsCursor.moveToFirst()) {
                    for(int i = 0; i < mWaypointsCursor.getCount(); i++){
                        mWaypointsLabels.put(i, mWaypointsCursor.getString(mWaypointsCursor.getColumnIndex(DatabaseHelper.labelID)));
                        mWaypointsXs.put(i, mWaypointsCursor.getDouble(mWaypointsCursor.getColumnIndex(DatabaseHelper.xPosition)));
                        mWaypointsYs.put(i, mWaypointsCursor.getDouble(mWaypointsCursor.getColumnIndex(DatabaseHelper.yPosition)));
                        mWaypointsCursor.moveToNext();
                    }
                    mWaypointsCursor.close();
                    return true;
                }
                else {
                    mWaypointsCursor.close();
                    Log.d(TAG, "Waypoints Cursor Error");
                    return false;
                }
            } catch (SQLiteException e) {
                Log.d(TAG, "Error reading database");
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result){
            if(!result){
                Log.d(TAG, "ReadWaypointsFromDB AsyncTask Error");
            } else{
                myGridView.setmWapointLabels(mWaypointsLabels);
                myGridView.setmWaypointsXs(mWaypointsXs);
                myGridView.setmWapointsYs(mWaypointsYs);
            }
        }
    }


    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }



}
