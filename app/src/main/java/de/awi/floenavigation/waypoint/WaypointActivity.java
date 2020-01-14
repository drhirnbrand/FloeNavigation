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
import android.database.SQLException;
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

import de.awi.floenavigation.R;
import de.awi.floenavigation.admin.ListViewActivity;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.services.GPS_Service;

/**
 * {@link WaypointActivity} activity is used to install the waypoints at specific points of interest
 * on the sea ice.
 * The app takes into consideration the tablet's current geographic coordinates to record the
 * location of the waypoint.
 */
public class WaypointActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "WaypointActivity";

    private static final String changeText = "Waypoint Installed";

    /**
     * {@link BroadcastReceiver} is used to receive the latest gps coordinates from the {@link
     * GPS_Service}
     */
    private BroadcastReceiver broadcastReceiver;
    /**
     * Variable used to store the tablet's latitude value
     */
    private Double tabletLat;
    /**
     * Variable used to store the tablet's longitude value
     */
    private Double tabletLon;
    /**
     * Variable stores the latitudinal value of origin fixed station
     */
    private double originLatitude;
    /**
     * Variable stores the longitudinal value of x-axis fixed station
     */
    private double originLongitude;
    /**
     * Variable stores the mmsi value of the origin fixed station
     */
    private int originMMSI;
    /**
     * Variable used to store the value of {@value DatabaseHelper#beta}
     * It is the angle between the x-axis and the geographic longitudinal axis
     */
    private double beta;

    /**
     * distance calculated between the origin fixed station and any waypoint in meters
     */
    private double distance;
    /**
     * Angle calculated between the x-axis and the waypoint
     */
    private double alpha;
    /**
     * Variable used to store the x-axis distance between the origin and the waypoint
     */
    private double xPosition;
    /**
     * Variable used to store the y-axxis distance between the origin and the waypoint
     */
    private double yPosition;
    /**
     * Angle calculated between the axis connecting origin fixed station and the longitudinal axis
     * and the waypoint
     */
    private double theta;
    /**
     * Variable stores the label of the waypoint, formatted in a specific structure
     */
    private String waypointLabel;
    /**
     * Variable used to store the time stamp at which the waypoint was installed
     */
    private String time;
    /**
     * Toggle flag to change the format of geographical coordinates display either in deg.min.sec or
     * in decimal
     * depending on the selection
     */
    private boolean changeFormat;
    /**
     * reads the admin defined number of significant figures from the {@link
     * DatabaseHelper#decimal_number_significant_figures}, used to
     * display the number of digits after the decimal point to be displayed
     */
    private int numOfSignificantFigures;
    /**
     * Variable used to store the gps time received from the {@link #broadcastReceiver}
     */
    private long gpsTime;
    /**
     * It stores the time difference between the gps time and the system clock
     */
    private long timeDiff;
    /**
     * Edit text view of the label id
     */
    private EditText labelId_TV;
    /**
     * Stores the label id
     */
    private String labelId;
    /**
     * Stores the tablet id
     */
    private String tabletID;


    //Action Bar Updates
    /**
     * {@link BroadcastReceiver} to receive {@link GPS_Service#AISPacketStatus}
     */
    private BroadcastReceiver aisPacketBroadcastReceiver;
    /**
     * Stores the gps location status received
     */
    private boolean locationStatus = false;
    /**
     * Stores the packet status received
     */
    private boolean packetStatus = false;
    /**
     * {@link Handler} to run the status bar runnable
     */
    private final Handler statusHandler = new Handler();
    /**
     * Menu Item views for gps icon, ais icon ang grid setup icon
     */
    private MenuItem gpsIconItem, aisIconItem, gridSetupIconItem;

    /**
     * onCreate method to setup the xml layout and
     * to setting up the on click listeners
     *
     * @param savedInstanceState stores the previous instance state
     */
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

    /**
     * onStart method to call method {@link #actionBarUpdatesFunction()}
     */
    @Override
    protected void onStart() {
        super.onStart();
        //Broadcast receiver for tablet location
        actionBarUpdatesFunction();
    }

    /**
     * Function to register the broadcast receivers
     * and implements onReceiver function of broadcast receiver
     * {@link Runnable} to update the status bar icons based on the values received
     * It runs periodically at a rate of {@link ActionBarActivity#UPDATE_TIME}
     */
    private void actionBarUpdatesFunction() {

        /*****************ACTION BAR UPDATES*************************/
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    tabletLat = intent.getExtras().getDouble(GPS_Service.latitude);
                    tabletLon = intent.getExtras().getDouble(GPS_Service.longitude);
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
                    gpsTime =
                            Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());
                    timeDiff = System.currentTimeMillis() - gpsTime;
                    populateTabLocation();
                }
            };
        }

        if (aisPacketBroadcastReceiver == null) {
            aisPacketBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    packetStatus = intent.getExtras().getBoolean(GPS_Service.AISPacketStatus);

                }
            };
        }

        registerReceiver(aisPacketBroadcastReceiver,
                         new IntentFilter(GPS_Service.AISPacketBroadcast));
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

        Runnable gpsLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (locationStatus) {
                    if (gpsIconItem != null) {
                        gpsIconItem.getIcon()
                                .setColorFilter(Color.parseColor(ActionBarActivity.colorGreen),
                                                PorterDuff.Mode.SRC_IN);
                    }
                } else {
                    if (gpsIconItem != null) {
                        gpsIconItem.getIcon()
                                .setColorFilter(Color.parseColor(ActionBarActivity.colorRed),
                                                PorterDuff.Mode.SRC_IN);
                    }
                }
                if (packetStatus) {
                    if (aisIconItem != null) {
                        aisIconItem.getIcon()
                                .setColorFilter(Color.parseColor(ActionBarActivity.colorGreen),
                                                PorterDuff.Mode.SRC_IN);
                    }
                } else {
                    if (aisIconItem != null) {
                        aisIconItem.getIcon()
                                .setColorFilter(Color.parseColor(ActionBarActivity.colorRed),
                                                PorterDuff.Mode.SRC_IN);
                    }
                }

                statusHandler.postDelayed(this, ActionBarActivity.UPDATE_TIME);
            }
        };

        statusHandler.postDelayed(gpsLocationRunnable, ActionBarActivity.UPDATE_TIME);
        /******************************************/
    }

    /**
     * Function used to change the display format of the latitude and longitude values in the views
     */
    private void populateTabLocation() {

        TextView latView = findViewById(R.id.waypointTabletLat);
        TextView lonView = findViewById(R.id.waypointTabletLon);
        String formatString = "%." + String.valueOf(numOfSignificantFigures) + "f";
        if (changeFormat) {
            String[] formattedCoordinates =
                    NavigationFunctions.locationInDegrees(tabletLat, tabletLon);
            latView.setText(formattedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
            lonView.setText(formattedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
        } else {
            latView.setText(String.format(formatString, tabletLat));
            lonView.setText(String.format(formatString, tabletLon));
        }
    }

    /**
     * This function takes care of implementation of status bar icons in the activity
     *
     * @param menu menu
     * @return returns menu to parent function
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

        if (MainActivity.numOfBaseStations >= DatabaseHelper.INITIALIZATION_SIZE) {
            if (gridSetupIconItem != null) {
                gridSetupIconItem.getIcon()
                        .setColorFilter(Color.parseColor(ActionBarActivity.colorGreen),
                                        PorterDuff.Mode.SRC_IN);
            }
        } else {
            if (gridSetupIconItem != null) {
                gridSetupIconItem.getIcon()
                        .setColorFilter(Color.parseColor(ActionBarActivity.colorRed),
                                        PorterDuff.Mode.SRC_IN);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Listener function to handle functions when an item in the menu list is clicked
     *
     * @param menuItem menu item
     * @return returns true
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.changeLatLonFormat:
                changeFormat = !changeFormat;
                populateTabLocation();
                DatabaseHelper.updateCoordinateDisplaySetting(this, changeFormat);
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);

        }
    }

    /**
     * Handles unregistering of the broadcast receivers
     */
    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
    }

    /**
     * OnClick listener to handle view clicks
     *
     * @param v view which was clicked
     */
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.waypoint_confirm:
                onClickConfirm();
                break;

            case R.id.waypoint_finish:
                onClickFinish();
                break;
        }

    }

    /**
     * When the confirm button is pressed, the function checks whether there are duplicate waypoints
     * already existing in the local database,
     * then it checks whether valid and proper gps location is available, if all the conditions are
     * met, it stores the waypoint into the
     * local database.
     * Exception handling and error checks are taken care of by displaying proper toast and log
     * messages
     */
    private void onClickConfirm() {

        TextView wayPointLabel = findViewById(R.id.waypointLabelId);
        if (TextUtils.isEmpty(wayPointLabel.getText().toString())) {
            Toast.makeText(this, "Invalid waypoint label", Toast.LENGTH_LONG).show();
            return;
        }
        DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(this);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        if (!checkWaypointInDBTables(db)) {
            tabletLat = (tabletLat == null) ? 0.0 : tabletLat;
            tabletLon = (tabletLon == null) ? 0.0 : tabletLon;
            if (tabletLat != 0.0 && tabletLon != 0.0) {
                findViewById(R.id.waypointCoordinateView).setVisibility(View.GONE);
                findViewById(R.id.waypointWaitingView).setVisibility(View.VISIBLE);
                if (getOriginCoordinates(db)) {
                    calculateWaypointParameters();
                    createLabel();
                    if (insertInDatabase(db)) {
                        Log.d(TAG, "Waypoint Inserted");
                        ProgressBar progress = findViewById(R.id.waypointProgress);
                        progress.stopNestedScroll();
                        progress.setVisibility(View.GONE);
                        findViewById(R.id.waypoint_finish).setClickable(true);
                        findViewById(R.id.waypoint_finish).setEnabled(true);
                        TextView waitingMsg = findViewById(R.id.waypointWaitingMsg);
                        waitingMsg.setText(changeText);
                    } else {
                        Log.d(TAG, "Error inserting new Waypoint");
                    }
                } else {
                    Log.d(TAG, "Error reading Origin Coordinates");
                }
            } else {
                Log.d(TAG, "Error with GPS Service");
                Toast.makeText(this, "Error reading Device Lat and Long", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Duplicate Waypoint, already exists",
                           Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Inserts the waypoint with all the required and necessary parameters into the database table
     * {@link DatabaseHelper#waypointsTable}
     *
     * @param db SQLiteDatabase object
     * @return <code>true</code> if the data is successfully inserted into the database table
     * <code>false</code> otherwise
     */
    private boolean insertInDatabase(SQLiteDatabase db) {
        ContentValues waypoint = new ContentValues();
        waypoint.put(DatabaseHelper.latitude, tabletLat);
        waypoint.put(DatabaseHelper.longitude, tabletLon);
        waypoint.put(DatabaseHelper.xPosition, xPosition);
        waypoint.put(DatabaseHelper.yPosition, yPosition);
        waypoint.put(DatabaseHelper.updateTime, time);
        waypoint.put(DatabaseHelper.labelID, labelId);
        waypoint.put(DatabaseHelper.label, waypointLabel);
        long result = db.insert(DatabaseHelper.waypointsTable, null, waypoint);
        if (result != -1) {
            Log.d(TAG, "Waypoint Inserted Successfully");
            return true;
        } else {
            Log.d(TAG, "Error Inserting Waypoint");
            return false;
        }
    }

    /**
     * Starts the {@link MainActivity} when the user presses the finish button
     */
    private void onClickFinish() {
        Log.d(TAG, "Activity Finished");
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
    }

    /**
     * calculates waypoint location in the grid
     */
    private void calculateWaypointParameters() {
        final NavigationFunctions.TransformedCoordinates t = NavigationFunctions
                .transform(originLatitude, originLongitude, tabletLat, tabletLon, beta);

        theta = t.getTheta();
        alpha = t.getAlpha();
        distance = t.getDistance();
        xPosition = t.getX();
        yPosition = t.getY();
        Log.d(TAG,
              String.format("Distance: %.4f, Alpha: %.3f (Theta: %.3f) -> x=%.2f,y=%.2f", distance,
                            alpha, theta, xPosition, yPosition));
    }

    /**
     * Create label in a particular format
     */
    private void createLabel() {
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

    /**
     * Checks whether there are duplicate waypoints in the database table
     * based on the label ID
     *
     * @param db SQLiteDatabase object
     * @return <code>true</code> if present; <code>false</code> otherwise
     */
    private boolean checkWaypointInDBTables(SQLiteDatabase db) {
        boolean isPresent = false;
        Cursor mWaypointCursor = null;
        labelId_TV = findViewById(R.id.waypointLabelId);
        labelId = labelId_TV.getText().toString();
        labelId = tabletID + "_" + labelId;
        try {
            mWaypointCursor =
                    db.query(DatabaseHelper.waypointsTable, new String[]{DatabaseHelper.labelID},
                             DatabaseHelper.labelID + " = ?", new String[]{labelId}, null, null,
                             null);

            isPresent = mWaypointCursor.moveToFirst();
        } catch (SQLException e) {
            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        } finally {
            if (mWaypointCursor != null) {
                mWaypointCursor.close();
            }
        }
        return isPresent;
    }

    /**
     * Get origin geographic coordinates and mmsi from the database tables {@link
     * DatabaseHelper#baseStationTable} and {@link DatabaseHelper#fixedStationTable}
     *
     * @param db SQLiteDatabase object
     * @return <code>true</code> if retrieval is successful; <code>false</code> otherwise
     */
    private boolean getOriginCoordinates(SQLiteDatabase db) {

        try {

            Cursor baseStationCursor =
                    db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi},
                             DatabaseHelper.isOrigin + " = ?",
                             new String[]{String.valueOf(DatabaseHelper.ORIGIN)}, null, null, null);
            if (baseStationCursor.getCount() != 1) {
                Log.d(TAG, "Error Reading from BaseStation Table");
                return false;
            } else {
                if (baseStationCursor.moveToFirst()) {
                    originMMSI = baseStationCursor
                            .getInt(baseStationCursor.getColumnIndex(DatabaseHelper.mmsi));
                }
            }
            Cursor fixedStationCursor = db.query(DatabaseHelper.fixedStationTable,
                                                 new String[]{DatabaseHelper.latitude,
                                                              DatabaseHelper.longitude},
                                                 DatabaseHelper.mmsi + " = ?",
                                                 new String[]{String.valueOf(originMMSI)}, null,
                                                 null, null);
            if (fixedStationCursor.getCount() != 1) {
                Log.d(TAG, "Error Reading Origin Latitude Longitude");
                return false;
            } else {
                if (fixedStationCursor.moveToFirst()) {
                    originLatitude = fixedStationCursor
                            .getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.latitude));
                    originLongitude = fixedStationCursor
                            .getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.longitude));
                }
            }

            Cursor betaCursor = db.query(DatabaseHelper.betaTable, new String[]{DatabaseHelper.beta,
                                                                                DatabaseHelper.updateTime},
                                         null, null, null, null, null);
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
        } catch (SQLiteException e) {
            Log.d(TAG, "Database Error");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * On pressing the View Waypoints List button, the activity starts a new activity listing all
     * the waypoints currently
     * stored in the database table
     *
     * @param view view which was clicked
     */
    public void onClickViewWaypoints(View view) {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            long numOfWaypoints = DatabaseUtils.queryNumEntries(db, DatabaseHelper.waypointsTable);

            if (numOfWaypoints > 0) {
                Intent listViewIntent = new Intent(this, ListViewActivity.class);
                listViewIntent.putExtra("GenerateDataOption", "WaypointActivity");
                startActivity(listViewIntent);
            } else {
                Toast.makeText(this, "No waypoints are marked in the grid", Toast.LENGTH_LONG)
                        .show();
            }
        } catch (SQLiteException e) {
            Log.d(TAG, "Error in reading database");
            e.printStackTrace();
        }

    }

    /**
     * On pressing back button, {@link MainActivity} gets started
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent mainActIntent = new Intent(this, MainActivity.class);
        startActivity(mainActIntent);
    }

    /**
     * Async task to read tablet id from the database table
     */
    private class ReadTabletID extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            boolean success = false;
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor paramCursor = db.query(DatabaseHelper.configParametersTable,
                                              new String[]{DatabaseHelper.parameterName,
                                                           DatabaseHelper.parameterValue},
                                              DatabaseHelper.parameterName + " = ?",
                                              new String[]{DatabaseHelper.tabletId}, null, null,
                                              null);
                if (paramCursor.moveToFirst()) {
                    String paramValue = paramCursor.getString(
                            paramCursor.getColumnIndexOrThrow(DatabaseHelper.parameterValue));
                    if (!paramValue.isEmpty()) {
                        success = true;
                        tabletID = paramValue;
                    } else {
                        Log.d(TAG, "Blank TabletID");
                    }
                } else {
                    Log.d(TAG, "TabletID not set");
                }
                paramCursor.close();

            } catch (SQLiteException e) {
                Log.d(TAG, "Error Reading from Database");
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Log.d(TAG, "Waypoint AsyncTask: Database Error");
            }
        }
    }
}
