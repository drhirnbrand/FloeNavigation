package de.awi.floenavigation.deployment;


import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import de.awi.floenavigation.R;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.services.GPS_Service;

/**
 * This {@link Fragment} runs on top of the {@link DeploymentActivity} and inserts the Static
 * Station which is being deployed in the Database.
 * <p>
 * The Fragment calculates the x,y coordinates of the Static Station from current GPS location of
 * the tablet and inserts the new station
 * in the Database table {@link DatabaseHelper#staticStationListTable}.
 * </p>
 *
 * @see DeploymentActivity
 * @see ContentValues
 * @see Runnable
 * @see NavigationFunctions
 */
public class StaticStationFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "StaticStationDeployFrag";

    /**
     * Text which is displayed when the station's parameters are calculated and inserted in to the
     * Database successfully.
     */
    private static final String changeText = "Station Installed";

    /**
     * {@link DatabaseHelper#staticStationName} of the Static Station to be deployed
     */
    private String stationName;

    /**
     * {@link DatabaseHelper#stationType} of the Static Station to be deployed
     */
    private String stationType;

    /**
     * {@link BroadcastReceiver} for receiving the GPS location broadcast from {@link GPS_Service}
     */
    private BroadcastReceiver broadcastReceiver;

    /**
     * Current Latitude of the tablet read from the tablet's built-in GPS
     */
    private double tabletLat;

    /**
     * Current Longitude of the tablet read from the tablet's built-in GPS
     */
    private double tabletLon;

    /**
     * Current Latitude of the origin
     */
    private double originLatitude;

    /**
     * Current longitude of the origin
     */
    private double originLongitude;

    /**
     * MMSI of the origin Base/Fixed Station
     */
    private int originMMSI;

    /**
     * Current Beta of the Coordinate System
     */
    private double beta;

    /**
     * Distance of the Static Station from the Origin. Calculated between {@link #originLatitude},
     * {@link #originLatitude} and {@link #tabletLat}, {@link #tabletLon}
     * using the Haversine formula
     */
    private double distance;

    /**
     * The Angle alpha of the Fixed Station which it makes with the x-Axis of the Floe's Coordinate
     * System
     */
    private double alpha;

    /**
     * x coordinate of the Static Station on the Floe's Coordinate System
     */
    private double xPosition;

    /**
     * y coordinate of the Static Station on the Floe's Coordinate System
     */
    private double yPosition;

    /**
     * The Angle theta of the Static Station which it makes with the Geographical Longitudinal Axis
     */
    private double theta;

    /**
     * {@link MenuItem}s shown on the Action Bar
     */
    private MenuItem gpsIconItem, aisIconItem, gridSetupIconItem;

    /**
     * <code>true</code> when the tablet has a GPS Connection
     */
    private boolean locationStatus = false;

    /**
     * <code>true</code> when the tablet is connected to the WiFi network of an AIS Transponder
     */
    private boolean packetStatus = false;

    /**
     * A {@link Handler} which is runs a {@link Runnable} object which changes the Action Bar icons
     * colors according to {@link #packetStatus}
     * and {@link #locationStatus}.
     */
    private final Handler statusHandler = new Handler();

    /**
     * {@link BroadcastReceiver} for checking the WiFi connection to an AIS Transponder which is
     * broadcast from {@link de.awi.floenavigation.network.NetworkMonitor}.
     */
    private BroadcastReceiver aisPacketBroadcastReceiver;

    /**
     * Default empty constructor
     */
    public StaticStationFragment() {
        // Required empty public constructor
    }

    /**
     * Default {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}. Reads the name and
     * type of the Static Station passed to it by {@link StationInstallFragment}.
     * The layout shows a {@link ProgressBar} with a waiting message and a cancel button.
     * <p>
     * The fragment reads the {@link #stationName}, {@link #stationType}, {@link #tabletLat}, {@link
     * #tabletLon} passed to it in the {@link Bundle}.
     * It then reads the Origin coordinates and calculates and inserts the new station in the
     * Database. It then changes the Layout to show
     * a successful insertion message ({@link #changeText}) and a Finish button.
     * </p>
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_static_station, container, false);

        layout.findViewById(R.id.static_station_finish).setOnClickListener(this);
        layout.findViewById(R.id.static_station_finish).setEnabled(false);
        layout.findViewById(R.id.static_station_finish).setClickable(false);
        stationName = getArguments().getString(DatabaseHelper.staticStationName);
        stationType = getArguments().getString(DatabaseHelper.stationType);
        tabletLat = getArguments().getDouble(GPS_Service.latitude);
        tabletLon = getArguments().getDouble(GPS_Service.longitude);
        if (getOriginCoordinates()) {
            calculateStaticStationParameters();
            insertStaticStation();
            ProgressBar progress = layout.findViewById(R.id.staticStationProgress);
            progress.stopNestedScroll();
            progress.setVisibility(View.GONE);
            TextView msg = layout.findViewById(R.id.staticStationFragMsg);
            msg.setText(changeText);
            layout.findViewById(R.id.static_station_finish).setEnabled(true);
            layout.findViewById(R.id.static_station_finish).setClickable(true);
            Log.d(TAG, "Station Installed");
            Toast.makeText(getContext(), "Station Installed", Toast.LENGTH_LONG).show();

        } else {
            Log.d(TAG, "Error Inserting new Station");
        }
        setHasOptionsMenu(true);
        return layout;
    }

    /**
     * Default Handler for the Finish button on the Screen. {@link MainActivity} is started when
     * deployment is complete.
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.static_station_finish:
                Intent mainIntent = new Intent(getActivity(), MainActivity.class);
                getActivity().startActivity(mainIntent);

        }
    }

    /**
     * Called when the Fragment is no longer in foreground. It unregisters the AIS and GPS {@link
     * BroadcastReceiver}s.
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        getActivity().unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
    }

    /**
     * Called when the Fragment come back from background to foreground. Disables the Up Button and
     * calls the
     * {@link #actionBarUpdatesFunction()} to set the correct icon colors in the Action Bar.
     */
    @Override
    public void onResume() {
        super.onResume();
        DeploymentActivity activity = (DeploymentActivity) getActivity();
        if (activity != null) {
            activity.hideUpButton();
        }
        actionBarUpdatesFunction();

    }

    /**
     * Creates the Action Bar icons on top of the screen. By default it shows the GPS icon, AIS
     * Connectivity icon and the Grid Setup icon.
     *
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        MenuItem latLonFormat = menu.findItem(R.id.changeLatLonFormat);
        latLonFormat.setVisible(false);

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

        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Calculates the location parameters of the Static Station. It calculates and sets {@link
     * #distance}, {@link #theta}, {@link #alpha}, {@link #xPosition}, {@link #yPosition}.
     */
    private void calculateStaticStationParameters() {

        final NavigationFunctions.TransformedCoordinates t = NavigationFunctions
                .transform(originLatitude, originLongitude, tabletLat, tabletLon, beta);

        //theta = NavigationFunctions.calculateAngleBeta(tabletLat, tabletLon, originLatitude,
        // originLongitude);
        //alpha = Math.abs(theta - beta);
        theta = t.getTheta();
        alpha = t.getAlpha();
        distance = t.getDistance();

        Log.d(TAG, "Theta: " + theta + " Alpha: " + alpha);
        Log.d(TAG, "StationDistance: " + String.valueOf(distance));
        Log.d(TAG, "OriginLat: " + String.valueOf(originLatitude) + " OriginLon: " +
                String.valueOf(originLongitude));
        Log.d(TAG, "TabletLat: " + String.valueOf(tabletLat) + " TabletLon: " +
                String.valueOf(tabletLon));

        xPosition = t.getX();
        yPosition = t.getY();

    }

    /**
     * Inserts the new Static Station and its location paramters into the database table {@link
     * DatabaseHelper#staticStationListTable}.
     *
     * @see ContentValues
     */
    private void insertStaticStation() {
        DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(getActivity());
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        ContentValues staticStation = new ContentValues();
        staticStation.put(DatabaseHelper.staticStationName, stationName);
        staticStation.put(DatabaseHelper.stationType, stationType);
        staticStation.put(DatabaseHelper.xPosition, xPosition);
        staticStation.put(DatabaseHelper.yPosition, yPosition);

        staticStation.put(DatabaseHelper.distance, distance);
        staticStation.put(DatabaseHelper.alpha, alpha);
        db.insert(DatabaseHelper.staticStationListTable, null, staticStation);
    }

    /**
     * Reads the basic parameters of the Floe's Coordinate system from the Database. Reads the
     * values of {@link #originMMSI}, {@link #originLatitude}, {@link #originLongitude},
     * {@link #beta} from their respective tables.
     *
     * @return <code>true</code> if all the values are read successfully.
     */
    private boolean getOriginCoordinates() {
        Cursor baseStationCursor = null;
        Cursor fixedStationCursor = null;
        Cursor betaCursor = null;

        try {
            DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(getActivity());
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            baseStationCursor =
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
            fixedStationCursor = db.query(DatabaseHelper.fixedStationTable,
                                          new String[]{DatabaseHelper.latitude,
                                                       DatabaseHelper.longitude},
                                          DatabaseHelper.mmsi + " = ?",
                                          new String[]{String.valueOf(originMMSI)}, null, null,
                                          null);
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


    /**
     * Registers and implements the {@link BroadcastReceiver}s for the AIS Connectivity and GPS
     * location broadcasts; which are sent from
     * {@link de.awi.floenavigation.network.NetworkMonitor} and {@link GPS_Service} respectively.
     * The GPS Broadcast receiver sets the value of {@link #locationStatus} to the value from the
     * {@link GPS_Service}.
     * The AIS Connectivity broadcast receiver sets the boolean {@link #packetStatus}.
     * This also registers {@link Runnable} with the {@link Handler} {@link #statusHandler} which
     * runs at a regular interval specified by {@link ActionBarActivity#UPDATE_TIME}  and
     * it checks the booleans {@link #locationStatus} and {@link #packetStatus} and changes the
     * Action Bar icons for GPS and AIS Connectivity
     * accrodingly.
     *
     * @see Runnable
     * @see Handler
     * @see BroadcastReceiver
     * @see ActionBarActivity
     */
    private void actionBarUpdatesFunction() {

        //***************ACTION BAR UPDATES*************************/
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
                    //setTabletLat(intent.getExtras().getDouble(GPS_Service.latitude));
                    //setTabletLon(intent.getExtras().getDouble(GPS_Service.longitude));

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

        getActivity().registerReceiver(aisPacketBroadcastReceiver,
                                       new IntentFilter(GPS_Service.AISPacketBroadcast));
        getActivity()
                .registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

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
        //****************************************/
    }


}
