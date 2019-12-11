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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.scalified.fab.ActionButton;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import de.awi.floenavigation.R;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.util.LocationUtils;


/**
 * {@link GridActivity} class takes care of creating and initializing async tasks to
 * read {x,y} values from all the database tables of interest.
 * Separate {@link AsyncTask} tasks are created to handle each database table.
 * It runs on a background thread.
 * It periodically runs at a rate of {@value #ASYNC_TASK_TIMER_PERIOD}.
 * The values extracted from the database tables are stored in the corresponding {@link HashMap}
 * which are send to {@link MapView} using setters.
 * It also takes care of handling of status bar icons and tablet location updates.
 */
public class GridActivity extends Activity implements View.OnClickListener {

    /**
     * Time period of the async task
     */
    private static final int ASYNC_TASK_TIMER_PERIOD = 8 * 1000;
    /**
     * Time delay before the start of asyn task
     */
    private static final int ASYNC_TASK_TIMER_DELAY = 0;

    /**
     * Used for logging purpose
     */
    private static final String TAG = "GridActivity";

    /**
     * {@link BroadcastReceiver} to receive gps coordinates of the tablet
     */
    private BroadcastReceiver gpsBroadcastReceiver;
    /**
     * Variable stores the latitude value of the tablet
     */
    private double tabletLat;
    /**
     * Variable stores the longitude value of the tablet
     */
    private double tabletLon;
    /**
     * Variable stores the origin fixed station latitude value
     */
    private double originLatitude;
    /**
     * Variable stores the origin fixed station longitude value
     */
    private double originLongitude;
    /**
     * Variable stores the origin x value in meters on the sea ice
     */
    private double originX;
    /**
     * Variable stores the origin y value in meters on the sea ice
     */
    private double originY;
    /**
     * Stores the origin fixed station mmsi value
     */
    private int originMMSI;
    /**
     * Variable used to store the value of {@value DatabaseHelper#beta}
     * It is the angle between the x-axis and the geographic longitudinal axis
     */
    private double beta;
    /**
     * Variable stores the tablet x value in meters on the sea ice w.r.t to the origin fixed station
     */
    private double tabletX;
    /**
     * Variable stores the tablet y value in meters on the sea ice w.r.t to the origin fixed station
     */
    private double tabletY;
    /**
     * Variable stores the tablet distance in meters from the origin
     */
    private double tabletDistance;
    /**
     * Angle between the axis connecting origin along the longitudinal axis and the tablet
     * geographic coordinate
     */
    private double tabletTheta;
    /**
     * Angle between the x-axis and the tablet location
     */
    private double tabletAlpha;

    /**
     * if <code>true</code> checks the checkbox on the menubar list and displays the fixed stations
     * on the grid
     * <code>false</code> otherwise
     */
    public static boolean showFixedStation = true;
    /**
     * if <code>true</code> checks the checkbox on the menubar list and displays the mobile stations
     * on the grid
     * <code>false</code> otherwise
     */
    public static boolean showMobileStation = true;
    /**
     * if <code>true</code> checks the checkbox on the menubar list and displays the static stations
     * on the grid
     * <code>false</code> otherwise
     */
    public static boolean showStaticStation = true;
    /**
     * if <code>true</code> checks the checkbox on the menubar list and displays the waypoints on
     * the grid
     * <code>false</code> otherwise
     */
    public static boolean showWaypointStation = true;
    /**
     * Hashmaps to store index and x position of fixed stations in a key-value pair
     */
    public static HashMap<Integer, Double> mFixedStationXs;
    /**
     * Hashmaps to store index and y position of fixed stations in a key-value pair
     */
    public static HashMap<Integer, Double> mFixedStationYs;
    /**
     * Hashmaps to store index and mmsi's of fixed stations in a key-value pair
     */
    public static HashMap<Integer, Integer> mFixedStationMMSIs;
    /**
     * Hashmaps to store index and fixed station names in a key-value pair
     */
    public static HashMap<Integer, String> mFixedStationNames;
    /**
     * Hashmaps to store index and x position of mobile stations in a key-value pair
     */
    public static HashMap<Integer, Double> mMobileStationXs;
    /**
     * Hashmaps to store index and y position of mmobile stations in a key-value pair
     */
    public static HashMap<Integer, Double> mMobileStationYs;
    /*
     * Hashmaps to store index and mmsi's of mobile stations in a key-value pair
     */
    public static HashMap<Integer, Integer> mMobileStationMMSIs;
    /**
     * Hashmaps to store index and mobile station names in a key-value pair
     */
    public static HashMap<Integer, String> mMobileStationNames;
    /**
     * Hashmaps to store index and x position of static stations in a key-value pair
     */
    public static HashMap<Integer, Double> mStaticStationXs;
    /**
     * Hashmaps to store index and y position of static stations in a key-value pair
     */
    public static HashMap<Integer, Double> mStaticStationYs;
    /**
     * Hashmaps to store index and static station names in a key-value pair
     */
    public static HashMap<Integer, String> mStaticStationNames;
    /**
     * Hashmaps to store index and x position of waypoints in a key-value pair
     */
    public static HashMap<Integer, Double> mWaypointsXs;
    /**
     * Hashmaps to store index and y position of waypoints in a key-value pair
     */
    public static HashMap<Integer, Double> mWaypointsYs;
    /**
     * Hashmaps to store index and labels of waypoints in a key-value pair
     */
    public static HashMap<Integer, String> mWaypointsLabels;

    /**
     * Timer to run the async task
     */
    private Timer asyncTaskTimer;


    /**
     * {@link BroadcastReceiver} is used to receive ais status update
     */
    private BroadcastReceiver aisPacketBroadcastReceiver;
    /**
     * <code>true</code> location status is available
     * <code>false</code> otherwise
     */
    private boolean locationStatus = false;
    /**
     * <code>true</code> ais packets are available from the ais transponder
     * <code>false</code> otherwise
     */
    private boolean packetStatus = false;
    /**
     * Handler to run the runnable for status updates
     */
    private final Handler statusHandler = new Handler();
    /**
     * Menu items views
     */
    private MenuItem gpsIconItem, aisIconItem, emptyIcon, gridSetupIconItem;
    /**
     * Mapview object
     */
    private MapView myGridView;
    /**
     * view object to store the grid view
     */
    private View myView;
    /**
     * Focus button
     */
    private ActionButton buttonView;

    /**
     * Initializes the views on the activity
     *
     * @param savedInstanceState stores the saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid);

        myGridView = (MapView) findViewById(R.id.GridView);
        myView = this.findViewById(R.id.GridView);
        buttonView = this.findViewById(R.id.action_button);
        buttonView.setOnClickListener(this);

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.myLayout);
        BubbleDrawable myBubble = new BubbleDrawable(BubbleDrawable.CENTER);
        myBubble.setCornerRadius(20);
        myBubble.setPointerAlignment(BubbleDrawable.CENTER);
        myBubble.setPadding(25, 25, 25, 25);
        myGridView.setBubbleLayout(linearLayout, myBubble);

    }

    /**
     * Initializes the hashmaps
     * and schedules async tasks
     */
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

    /**
     * OnResume method is called when the app gets back its focus
     * So it was required to restart the {@link #asyncTaskTimer}
     * to run the async tasks
     */
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

    /**
     * onPause method
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "LifeCycle OnPause");

    }

    /**
     * onRestart method
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "LifeCycle OnRestart");
    }

    /**
     * onStop method handles the unregistering of the broadcast receivers
     */
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

    /**
     * onDestroy handles the cancelling of the
     * {@link #asyncTaskTimer} timer and refresh timer of the map view
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Lifecycle OnDestroy");
        asyncTaskTimer.cancel();
        MapView.refreshScreenTimer.cancel();
    }

    /**
     * Used to specify the options menu for the activity
     * and also keeps default checks to the items in the list
     *
     * @param menu type of the parameter menu
     * @return <code>true</code> for the menu to be displayed; <code>false</code> otherwise
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

    /**
     * It starts the {@link MainActivity} when the back button on the screen is pressed
     */
    @Override
    public void onBackPressed() {
        Log.d(TAG, "BackPressed");
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);

    }

    /**
     * This function is called whenever an item in the option menu is selected
     * Whenever the item is selected, the ticks on the checkboxes are toggled
     * This will result in displaying only the items selected on the grid
     * For example, if fixed station is selected, only fixed stations will be available on the grid
     * These selections are not available for tablet position and the position of the mothership
     * It will be present on the grid by default all the time
     *
     * @param item The menu item that was selected
     * @return <code>true</code> to consume it here; <code>false</code> otherwise
     */
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


    /**
     * This function handles the functionality of receiving the tablet coordinates and translating
     * it
     * to (x, y) coordinates.
     * If the tablet positions are not available from the {@link GPS_Service}, the last known
     * positions are calculated
     * and stored in the variables {@link #tabletLat} and {@link #tabletLon}
     * The calculated parameters are then send to the {@link MapView} for displaying purpose
     */
    private void calculateTabletGridCoordinates() {
        if (tabletLat == 0.0) {
            try {
                if (getLastKnownLocation() != null) {
                    tabletLat = getLastKnownLocation().getLatitude();
                }
            } catch (SecurityException e) {
                Toast.makeText(this, "Location Service Problem", Toast.LENGTH_LONG).show();
            }
        }
        if (tabletLon == 0.0) {
            try {
                if (getLastKnownLocation() != null) {
                    tabletLon = getLastKnownLocation().getLongitude();
                }
            } catch (SecurityException e) {
                Toast.makeText(this, "Location Service Problem", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Location Service Not Available");
                e.printStackTrace();
            }
        }
        tabletDistance = NavigationFunctions
                .calculateDifference(tabletLat, tabletLon, originLatitude, originLongitude);
        //Log.d(TAG + "TabletParam", "TabletLat: " + String.valueOf(tabletLat)+ " TabletLon: "+
        // String.valueOf(tabletLon));
        //Log.d(TAG + "TabletParam", "OriginLat: " + String.valueOf(originLatitude)+ " OriginLon:
        // " + String.valueOf(originLongitude));
        //tabletTheta = NavigationFunctions.calculateAngleBeta(tabletLat, tabletLon,
        // originLatitude, originLongitude);
        tabletTheta = NavigationFunctions
                .calculateAngleBeta(originLatitude, originLongitude, tabletLat, tabletLon);
        //Log.d(TAG + "TabletParam", "TabletDistance: " + String.valueOf(tabletDistance));
        //tabletAlpha = Math.abs(tabletTheta - beta);
        tabletAlpha = tabletTheta - beta;
        tabletX = tabletDistance * Math.cos(Math.toRadians(tabletAlpha));
        tabletY = tabletDistance * Math.sin(Math.toRadians(tabletAlpha));
        Log.d(TAG, "tabletX " + tabletX);
        myGridView.setTabletX(tabletX);
        myGridView.setTabletY(tabletY);
        myGridView.setTabletLat(tabletLat);
        myGridView.setTabletLon(tabletLon);
    }

    /**
     * Handles the registration of broadcast receiver and implementation of onReceive function of
     * the gps broadcast receiver
     * Whenever an updated location it available from the {@link GPS_Service}, the onReceive
     * function gets triggered and the new coordinates are stored in these 2 variables {@link
     * #tabletLat}
     * and {@link #tabletLon}.
     * Also, the color of the status bar icons are toggled based on the values received
     */
    private void actionBarUpdatesFunction() {

        ///*****************ACTION BAR UPDATES*************************/
        if (gpsBroadcastReceiver == null) {
            gpsBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //Log.d(TAG, "BroadCast Received");
                    /*String coordinateString = intent.getExtras().get("coordinates").toString();
                    String[] coordinates = coordinateString.split(",");*/
                    tabletLat = intent.getExtras().getDouble(GPS_Service.latitude);
                    tabletLon = intent.getExtras().getDouble(GPS_Service.longitude);
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
                    //tabletTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime)
                    // .toString());

                    //Log.d(TAG, "Tablet Lat: " + String.valueOf(tabletLat));
                    //Log.d(TAG, "Tablet Lon: " + String.valueOf(tabletLon));
                    //Toast.makeText(getActivity(),"Received Broadcast", Toast.LENGTH_LONG).show();
                    //populateTabLocation();
                    calculateTabletGridCoordinates();
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
        registerReceiver(gpsBroadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

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

        statusHandler.post(gpsLocationRunnable);
        ///******************************************/
    }

    public void onClickCenterButton(View view) {

        Log.d(TAG, "Pressed Detected onClick Function");
        //myGridView.onClickOriginButton(myView);

    }

    /**
     * On pressing on the focus button, the grid size gets changed
     *
     * @param v view which was clicked
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_button:
                Log.d(TAG, "Button Click");
                myGridView.resetContentRect();
                break;

        }

    }

    /**
     * Async task runs in the background thread periodically.
     * This class handles the retrieval of origin fixed station parameters from the database table.
     * The latest values are available from the database whenever exceuted.
     * Initially, it obtains the origin mmsi from the {@link DatabaseHelper#baseStationTable},
     * then it obtains the origin geographic coordinates and the (x, y) positions from the {@link
     * DatabaseHelper#fixedStationTable},
     * then it obtains the {@link DatabaseHelper#beta} from the {@link DatabaseHelper#betaTable}.
     * {@link DatabaseHelper#beta} is used to calculate the tablet position {@link
     * #calculateTabletGridCoordinates()}.
     * Origin coordinates are
     * After the retrieval the each respective cursors are closed.
     */
    private class ReadOriginFromDB extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            Cursor baseStationCursor = null;
            Cursor fixedStationCursor = null;
            Cursor betaCursor = null;
            try {
                int i = 0;
                DatabaseHelper databaseHelper =
                        DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                baseStationCursor =
                        db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi},
                                 DatabaseHelper.isOrigin + " = ?",
                                 new String[]{String.valueOf(DatabaseHelper.ORIGIN)}, null, null,
                                 null);
                Log.d(TAG, String.valueOf(baseStationCursor.getCount()));
                if (baseStationCursor.getCount() != 1) {
                    Log.d(TAG, "Error Reading from BaseStation Table");
                    return false;
                } else {
                    if (baseStationCursor.moveToFirst()) {
                        originMMSI = baseStationCursor
                                .getInt(baseStationCursor.getColumnIndex(DatabaseHelper.mmsi));
                    }
                }


                fixedStationCursor = db.query(DatabaseHelper.fixedStationTable,
                                              new String[]{DatabaseHelper.latitude,
                                                           DatabaseHelper.longitude,
                                                           DatabaseHelper.xPosition,
                                                           DatabaseHelper.yPosition},
                                              DatabaseHelper.mmsi + " = ?",
                                              new String[]{String.valueOf(originMMSI)}, null, null,
                                              null);
                if (fixedStationCursor.getCount() != 1) {
                    Log.d(TAG, "Error Reading Origin Latitude Longitude");
                    return false;
                } else {
                    if (fixedStationCursor.moveToFirst()) {
                        originLatitude = fixedStationCursor.getDouble(
                                fixedStationCursor.getColumnIndex(DatabaseHelper.latitude));
                        originLongitude = fixedStationCursor.getDouble(
                                fixedStationCursor.getColumnIndex(DatabaseHelper.longitude));
                        originX = fixedStationCursor.getDouble(
                                fixedStationCursor.getColumnIndex(DatabaseHelper.xPosition));
                        originY = fixedStationCursor.getDouble(
                                fixedStationCursor.getColumnIndex(DatabaseHelper.yPosition));

                        final double _originX = originX;
                        final double _originY = originY;

                        runOnUiThread(() -> {
                            myGridView.setOriginX(_originX);
                            myGridView.setOriginY(_originY);
                        });
                        //                        myGridView.setOriginX(originX);
                        //                        myGridView.setOriginY(originY);
                    }
                }

                betaCursor = db.query(DatabaseHelper.betaTable,
                                      new String[]{DatabaseHelper.beta, DatabaseHelper.updateTime},
                                      null, null, null, null, null);
                if (betaCursor.getCount() == 1) {
                    if (betaCursor.moveToFirst()) {
                        beta = betaCursor.getDouble(betaCursor.getColumnIndex(DatabaseHelper.beta));
                    }

                } else {
                    Log.d(TAG, "Error in Beta Table");
                    return false;
                }
                return true;
            } catch (SQLiteException e) {
                Log.d(TAG, "Database Error");
                e.printStackTrace();
                return false;
            } finally {
                if (betaCursor != null) {
                    betaCursor.close();
                }
                if (baseStationCursor != null) {
                    baseStationCursor.close();
                }
                if (fixedStationCursor != null) {
                    fixedStationCursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Log.d(TAG, "ReadOriginFromDB AsyncTask Error");
            }
        }

    }

    /**
     * Async task runs in the background thread periodically.
     * MMSIs, X, Y and Names of the fixed stations are obtained from the fixed station database
     * table {@link DatabaseHelper#fixedStationTable}.
     * The latest values are available from the database whenever exceuted.
     * One by one these values are stored in the respective hashmaps in a key-value pair format.
     * These hashmaps are used by the {@link MapView} to display all the fixed stations on the
     * grid.
     * The onPostExecute method implements the call to setter functions of the {@link MapView}
     */
    private class ReadFixedStationsFromDB extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            Cursor mFixedStnCursor = null;
            try {
                SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                //double xPosition, yPosition;
                //int mmsi;

                mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable,
                                           new String[]{DatabaseHelper.mmsi,
                                                        DatabaseHelper.xPosition,
                                                        DatabaseHelper.yPosition,
                                                        DatabaseHelper.stationName}, null, null,
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
                        mFixedStationMMSIs.put(i, mFixedStnCursor
                                .getInt(mFixedStnCursor.getColumnIndex(DatabaseHelper.mmsi)));
                        mFixedStationXs.put(i, mFixedStnCursor.getDouble(
                                mFixedStnCursor.getColumnIndex(DatabaseHelper.xPosition)));
                        mFixedStationYs.put(i, mFixedStnCursor.getDouble(
                                mFixedStnCursor.getColumnIndex(DatabaseHelper.yPosition)));
                        mFixedStationNames.put(i, mFixedStnCursor.getString(
                                mFixedStnCursor.getColumnIndex(DatabaseHelper.stationName)));
                        Log.d(TAG, "Fixed Station MMSI: " + mFixedStationMMSIs.get(i) + " xPos: " +
                                mFixedStationXs.get(i) + " yPos: " + mFixedStationYs.get(i));
                        mFixedStnCursor.moveToNext();
                    }
                    return true;
                } else {

                    Log.d(TAG, "FixedStationTable Cursor Error");
                    return false;
                }
            } catch (SQLiteException e) {
                Log.d(TAG, "Error reading database");
                e.printStackTrace();
                return false;
            } finally {
                if (mFixedStnCursor != null) {
                    mFixedStnCursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Log.d(TAG, "ReadFixedStationParams AsyncTask Error");
            } else {
                myGridView.setmFixedStationMMSIs(mFixedStationMMSIs);
                myGridView.setmFixedStationXs(mFixedStationXs);
                myGridView.setmFixedStationYs(mFixedStationYs);
                myGridView.setmFixedStationNamess(mFixedStationNames);
            }
        }
    }

    /**
     * Async task runs in the background thread periodically.
     * MMSIs, X, Y and Names of the fixed stations are obtained from the mobile station database
     * table {@link DatabaseHelper#mobileStationTable}.
     * The latest values are available from the database whenever exceuted.
     * One by one these values are stored in the respective hashmaps in a key-value pair format.
     * These hashmaps are used by the {@link MapView} to display all the fixed stations on the
     * grid.
     * The onPostExecute method implements the call to setter functions of the {@link MapView}
     */
    private class ReadMobileStationsFromDB extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            Cursor mMobileStnCursor = null;
            try {
                SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                //double xPosition, yPosition;
                //int mmsi;

                mMobileStnCursor = db.query(DatabaseHelper.mobileStationTable,
                                            new String[]{DatabaseHelper.mmsi,
                                                         DatabaseHelper.stationName,
                                                         DatabaseHelper.xPosition,
                                                         DatabaseHelper.yPosition,
                                                         DatabaseHelper.isCalculated},
                                            DatabaseHelper.isCalculated + " = ?", new String[]{
                                Integer.toString(DatabaseHelper.MOBILE_STATION_IS_CALCULATED)},
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
                        mMobileStationMMSIs.put(i, mMobileStnCursor
                                .getInt(mMobileStnCursor.getColumnIndex(DatabaseHelper.mmsi)));
                        mMobileStationXs.put(i, mMobileStnCursor.getDouble(
                                mMobileStnCursor.getColumnIndex(DatabaseHelper.xPosition)));
                        mMobileStationYs.put(i, mMobileStnCursor.getDouble(
                                mMobileStnCursor.getColumnIndex(DatabaseHelper.yPosition)));
                        mMobileStationNames.put(i, mMobileStnCursor.getString(
                                mMobileStnCursor.getColumnIndex(DatabaseHelper.stationName)));
                        //Log.d(TAG, "Mobile Station MMSI: " + mMobileStationMMSIs.get(i) + " X:
                        // " + mMobileStationXs.get(i) + " Y: " + mMobileStationYs.get(i));
                        mMobileStnCursor.moveToNext();
                    }
                    return true;
                } else {

                    Log.d(TAG, "MobileStation Cursor Error");
                    return false;
                }
            } catch (SQLiteException e) {
                Log.d(TAG, "Error reading database");
                e.printStackTrace();
                return false;
            } finally {
                if (mMobileStnCursor != null) {
                    mMobileStnCursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Log.d(TAG, "ReadMobileStationFromDB AsyncTask Error");
            } else {
                myGridView.setmMobileStationMMSIs(mMobileStationMMSIs);
                myGridView.setmMobileStationXs(mMobileStationXs);
                myGridView.setmMobileStationYs(mMobileStationYs);
                myGridView.setmMobileStationNamess(mMobileStationNames);
            }
        }
    }

    /**
     * Async task runs in the background thread periodically.
     * X, Y and Names of the static stations are obtained from the static station database table
     * {@link DatabaseHelper#staticStationListTable}.
     * The latest values are available from the database whenever exceuted.
     * One by one these values are stored in the respective hashmaps in a key-value pair format.
     * These hashmaps are used by the {@link MapView} to display all the fixed stations on the
     * grid.
     * The onPostExecute method implements the call to setter functions of the {@link MapView}
     */
    private class ReadStaticStationsFromDB extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            Cursor mStaticStationCursor = null;
            try {
                SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                //double xPosition, yPosition;
                //int mmsi;

                mStaticStationCursor = db.query(DatabaseHelper.staticStationListTable,
                                                new String[]{DatabaseHelper.staticStationName,
                                                             DatabaseHelper.xPosition,
                                                             DatabaseHelper.yPosition}, null, null,
                                                null, null, null, null);
                //mStaticStationXs = new double[mStaticStationCursor.getCount()];
                //mStaticStationYs = new double[mStaticStationCursor.getCount()];
                //mStaticStationNames = new String[mStaticStationCursor.getCount()];
                mStaticStationNames.clear();
                mStaticStationXs.clear();
                mStaticStationYs.clear();
                MapView.clearStaticStationHashTables();
                if (mStaticStationCursor.moveToFirst()) {
                    for (int i = 0; i < mStaticStationCursor.getCount(); i++) {
                        mStaticStationNames.put(i, mStaticStationCursor.getString(
                                mStaticStationCursor
                                        .getColumnIndex(DatabaseHelper.staticStationName)));
                        mStaticStationXs.put(i, mStaticStationCursor.getDouble(
                                mStaticStationCursor.getColumnIndex(DatabaseHelper.xPosition)));
                        mStaticStationYs.put(i, mStaticStationCursor.getDouble(
                                mStaticStationCursor.getColumnIndex(DatabaseHelper.yPosition)));
                        mStaticStationCursor.moveToNext();
                    }
                    return true;
                } else {
                    Log.d(TAG, "StaticStation Cursor Error");
                    return false;
                }

            } catch (SQLiteException e) {
                Log.d(TAG, "Error reading database");
                e.printStackTrace();
                return false;
            } finally {
                if (mStaticStationCursor != null) {
                    mStaticStationCursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Log.d(TAG, "ReadStaticStationFromDB AsyncTask Error");
            } else {
                myGridView.setmStaticStationNamess(mStaticStationNames);
                myGridView.setmStaticStationXs(mStaticStationXs);
                myGridView.setmStaticStationYs(mStaticStationYs);
            }
        }
    }

    /**
     * Async task runs in the background thread periodically.
     * Labels, X, Y and Names of the waypoints are obtained from the waypoint database table {@link
     * DatabaseHelper#waypointsTable}.
     * The latest values are available from the database whenever exceuted.
     * One by one these values are stored in the respective hashmaps in a key-value pair format.
     * These hashmaps are used by the {@link MapView} to display all the fixed stations on the
     * grid.
     * The onPostExecute method implements the call to setter functions of the {@link MapView}
     */
    private class ReadWaypointsFromDB extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            Cursor mWaypointsCursor = null;
            try {
                SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                //double xPosition, yPosition;
                //int mmsi;

                mWaypointsCursor = db.query(DatabaseHelper.waypointsTable,
                                            new String[]{DatabaseHelper.labelID,
                                                         DatabaseHelper.xPosition,
                                                         DatabaseHelper.yPosition}, null, null,
                                            null, null, null, null);
                //mWaypointsXs = new double[mWaypointsCursor.getCount()];
                //mWaypointsYs = new double[mWaypointsCursor.getCount()];
                //mWaypointsLabels = new String[mWaypointsCursor.getCount()];
                mWaypointsLabels.clear();
                mWaypointsXs.clear();
                mWaypointsYs.clear();
                MapView.clearWaypointHashTables();
                if (mWaypointsCursor.moveToFirst()) {
                    for (int i = 0; i < mWaypointsCursor.getCount(); i++) {
                        mWaypointsLabels.put(i, mWaypointsCursor.getString(
                                mWaypointsCursor.getColumnIndex(DatabaseHelper.labelID)));
                        mWaypointsXs.put(i, mWaypointsCursor.getDouble(
                                mWaypointsCursor.getColumnIndex(DatabaseHelper.xPosition)));
                        mWaypointsYs.put(i, mWaypointsCursor.getDouble(
                                mWaypointsCursor.getColumnIndex(DatabaseHelper.yPosition)));
                        mWaypointsCursor.moveToNext();
                    }
                    return true;
                } else {
                    Log.d(TAG, "Waypoints Cursor Error");
                    return false;
                }
            } catch (SQLiteException e) {
                Log.d(TAG, "Error reading database");
                e.printStackTrace();
                return false;
            } finally {
                if (mWaypointsCursor != null) {
                    mWaypointsCursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Log.d(TAG, "ReadWaypointsFromDB AsyncTask Error");
            } else {
                myGridView.setmWapointLabels(mWaypointsLabels);
                myGridView.setmWaypointsXs(mWaypointsXs);
                myGridView.setmWapointsYs(mWaypointsYs);
            }
        }
    }

    /**
     * Intializes the location service and obtains the last known location of the tablet
     *
     * @return the last location
     */
    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        return LocationUtils.getLastKnownLocation(this);
    }


}
