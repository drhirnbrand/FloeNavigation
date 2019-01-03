package de.awi.floenavigation.waypoint;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.admin.ListViewActivity;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.R;

public class WaypointActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "WaypointActivity";

    private static final String changeText = "Waypoint Installed";

    private BroadcastReceiver broadcastReceiver;
    private Double tabletLat;
    private Double tabletLon;
    private double originLatitude;
    private double originLongitude;
    private int originMMSI;
    private double beta;
    private double distance;
    private double alpha;
    private double xPosition;
    private double yPosition;
    private double theta;
    private String waypointLabel;
    private String time;
    private boolean changeFormat;
    private int numOfSignificantFigures;
    private long gpsTime;
    private long timeDiff;
    private EditText labelId_TV;
    private String labelId;
    private String tabletID;


    //Action Bar Updates
    private BroadcastReceiver aisPacketBroadcastReceiver;
    private boolean locationStatus = false;
    private boolean packetStatus = false;
    private final Handler statusHandler = new Handler();
    private MenuItem gpsIconItem, aisIconItem, gridSetupIconItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waypoint);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        new ReadTabletID().execute();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        changeFormat = DatabaseHelper.readCoordinateDisplaySetting(this);
        numOfSignificantFigures = DatabaseHelper.readSiginificantDigitsSetting(this);
        findViewById(R.id.waypoint_confirm).setOnClickListener(this);
        findViewById(R.id.waypoint_finish).setOnClickListener(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        //Broadcast receiver for tablet location
        actionBarUpdatesFunction();
    }

    private void actionBarUpdatesFunction() {

        /*****************ACTION BAR UPDATES*************************/
        if (broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    tabletLat = intent.getExtras().getDouble(GPS_Service.latitude);
                    tabletLon = intent.getExtras().getDouble(GPS_Service.longitude);
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
                    gpsTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());
                    timeDiff = System.currentTimeMillis() - gpsTime;
                    populateTabLocation();
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
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

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
        /******************************************/
    }

    private void populateTabLocation(){

        TextView latView = findViewById(R.id.waypointTabletLat);
        TextView lonView = findViewById(R.id.waypointTabletLon);
        String formatString = "%." + String.valueOf(numOfSignificantFigures) + "f";
        if(changeFormat){
            String[] formattedCoordinates = NavigationFunctions.locationInDegrees(tabletLat, tabletLon);
            latView.setText(formattedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
            lonView.setText(formattedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
        } else {
            latView.setText(String.format(formatString, tabletLat));
            lonView.setText(String.format(formatString, tabletLon));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem latLonFormat = menu.findItem(R.id.changeLatLonFormat);
        latLonFormat.setVisible(true);
        int[] iconItems = {R.id.gridSetupComplete, R.id.currentLocationAvail, R.id.aisPacketAvail};
        gridSetupIconItem = menu.findItem(iconItems[0]);
        gridSetupIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        gpsIconItem = menu.findItem(iconItems[1]);
        gpsIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        aisIconItem = menu.findItem(iconItems[2]);
        aisIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);

        if(MainActivity.numOfBaseStations >= DatabaseHelper.INITIALIZATION_SIZE) {
            if (gridSetupIconItem != null) {
                gridSetupIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorGreen), PorterDuff.Mode.SRC_IN);
            }
        } else{
            if (gridSetupIconItem != null) {
                gridSetupIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorRed), PorterDuff.Mode.SRC_IN);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()){
            case R.id.changeLatLonFormat:
                changeFormat = !changeFormat;
                populateTabLocation();
                DatabaseHelper.updateCoordinateDisplaySetting(this, changeFormat);
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
    }

    public void onClick(View v){

        switch (v.getId()){
            case R.id.waypoint_confirm:
                onClickConfirm();
                break;

            case R.id.waypoint_finish:
                onClickFinish();
                break;
        }

    }

    private void onClickConfirm(){

        TextView wayPointLabel = findViewById(R.id.waypointLabelId);
        if (TextUtils.isEmpty(wayPointLabel.getText().toString())) {
            Toast.makeText(this, "Invalid waypoint label", Toast.LENGTH_LONG).show();
            return;
        }
        tabletLat = (tabletLat == null) ? 0.0 : tabletLat;
        tabletLon = (tabletLon == null) ? 0.0 : tabletLon;
        if(tabletLat != 0.0 && tabletLon != 0.0) {
            DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            findViewById(R.id.waypointCoordinateView).setVisibility(View.GONE);
            findViewById(R.id.waypointWaitingView).setVisibility(View.VISIBLE);
            if (getOriginCoordinates(db)){
                calculateWaypointParameters();
                createLabel();
                if(insertInDatabase(db)){
                    Log.d(TAG, "Waypoint Inserted");
                    ProgressBar progress = findViewById(R.id.waypointProgress);
                    progress.stopNestedScroll();
                    progress.setVisibility(View.GONE);
                    findViewById(R.id.waypoint_finish).setClickable(true);
                    findViewById(R.id.waypoint_finish).setEnabled(true);
                    TextView waitingMsg = findViewById(R.id.waypointWaitingMsg);
                    waitingMsg.setText(changeText);
                }else {
                    Log.d(TAG, "Error inserting new Waypoint");
                }
            } else{
                Log.d(TAG, "Error reading Origin Coordinates");
            }
        } else{
            Log.d(TAG, "Error with GPS Service");
            Toast.makeText(this, "Error reading Device Lat and Long", Toast.LENGTH_LONG).show();
        }
    }

    private boolean insertInDatabase(SQLiteDatabase db){
        ContentValues waypoint = new ContentValues();
        waypoint.put(DatabaseHelper.latitude, tabletLat);
        waypoint.put(DatabaseHelper.longitude, tabletLon);
        waypoint.put(DatabaseHelper.xPosition, xPosition);
        waypoint.put(DatabaseHelper.yPosition, yPosition);
        waypoint.put(DatabaseHelper.updateTime, time);
        waypoint.put(DatabaseHelper.labelID, labelId);
        waypoint.put(DatabaseHelper.label, waypointLabel);
        long result = db.insert(DatabaseHelper.waypointsTable, null, waypoint);
        if(result != -1){
            Log.d(TAG, "Waypoint Inserted Successfully");
            return true;
        } else{
            Log.d(TAG, "Error Inserting Waypoint");
            return false;
        }
    }

    private void onClickFinish(){
        Log.d(TAG, "Activity Finished");
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
    }

    private void calculateWaypointParameters(){
        distance = NavigationFunctions.calculateDifference(tabletLat, tabletLon, originLatitude, originLongitude);
        theta = NavigationFunctions.calculateAngleBeta(tabletLat, tabletLon, originLatitude, originLongitude);
        alpha = Math.abs(theta - beta);
        xPosition = distance * Math.cos(Math.toRadians(alpha));
        yPosition = distance * Math.sin(Math.toRadians(alpha));
    }

    private void createLabel(){
        Date date = new Date(System.currentTimeMillis() - timeDiff);
        SimpleDateFormat displayFormat = new SimpleDateFormat("yyyyMMdd'D'HHmmss");
        displayFormat.setTimeZone(TimeZone.getTimeZone("gmt"));
        time = displayFormat.format(date);
        labelId_TV = findViewById(R.id.waypointLabelId);
        labelId = labelId_TV.getText().toString();
        labelId = tabletID + "_" + labelId;
        List<String> labelElements = new ArrayList<String>();
        labelElements.add(time);
        labelElements.add(String.valueOf(tabletLat));
        labelElements.add(String.valueOf(tabletLon));
        labelElements.add(String.valueOf(xPosition));
        labelElements.add(String.valueOf(yPosition));
        labelElements.add(String.valueOf(0.0));
        labelElements.add(labelId);
        waypointLabel = TextUtils.join(",", labelElements);
        Log.d(TAG, "Label: " + waypointLabel);
    }

    private boolean getOriginCoordinates(SQLiteDatabase db){

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

    public void onClickViewWaypoints(View view) {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            long numOfWaypoints = DatabaseUtils.queryNumEntries(db, DatabaseHelper.waypointsTable);

            if (numOfWaypoints > 0){
                Intent listViewIntent = new Intent(this, ListViewActivity.class);
                listViewIntent.putExtra("GenerateDataOption", "WaypointActivity");
                startActivity(listViewIntent);
            }else {
                Toast.makeText(this, "No waypoints are marked in the grid", Toast.LENGTH_LONG).show();
            }
        }catch (SQLiteException e){
            Log.d(TAG, "Error in reading database");
            e.printStackTrace();
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent mainActIntent = new Intent(this, MainActivity.class);
        startActivity(mainActIntent);
    }

    private class ReadTabletID extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            boolean success = false;
            try{
                DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor paramCursor = db.query(DatabaseHelper.configParametersTable,
                        new String[] {DatabaseHelper.parameterName, DatabaseHelper.parameterValue},
                        DatabaseHelper.parameterName +" = ?",
                        new String[] {DatabaseHelper.tabletId},
                        null, null, null);
                if (paramCursor.moveToFirst()){
                    String paramValue = paramCursor.getString(paramCursor.getColumnIndexOrThrow(DatabaseHelper.parameterValue));
                    if(!paramValue.isEmpty()){
                        success = true;
                        tabletID = paramValue;
                    } else{
                        Log.d(TAG, "Blank TabletID");
                    }
                } else{
                    Log.d(TAG, "TabletID not set");
                }
                paramCursor.close();

            } catch(SQLiteException e){
                Log.d(TAG, "Error Reading from Database");
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result){
                Log.d(TAG, "Waypoint AsyncTask: Database Error");
            }
        }
    }
}
