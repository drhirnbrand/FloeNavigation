package de.awi.floenavigation.deployment;


import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import de.awi.floenavigation.R;
import de.awi.floenavigation.admin.AdminPageActivity;
import de.awi.floenavigation.aismessages.AISDecodingService;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.FragmentChangeListener;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.services.GPS_Service;

/**
 * This {@link Fragment} runs on top of the {@link DeploymentActivity} and calculates the location
 * parameters of a Fixed Station which is being deployed.
 * <p>
 * The {@link StationInstallFragment} inserts the MMSI number in to the {@link
 * DatabaseHelper#fixedStationTable} and {@link DatabaseHelper#stationListTable}.
 * This fragment waits and checks the {@link DatabaseHelper#fixedStationTable} to see if a Position
 * Report has been received from the given MMSI number.
 * The Position Report {@link de.awi.floenavigation.aismessages.PostnReportClassA} and {@link
 * de.awi.floenavigation.aismessages.PostnReportClassB} are received, decoded
 * and inserted in to the Database by {@link AISDecodingService}. If a position report is received
 * the fragment uses the location data (Latitude and Longitude) from the Position to calculate the
 * angle Alpha and x and y coordinates of
 * the Fixed Station and insert these values in {@link DatabaseHelper#fixedStationTable}. If the
 * position report is not received for a specified time
 * the {@link StationInstallFragment} is called again to re-enter MMSI number.
 * The Layout of this fragment shows a {@link ProgressBar} with a waiting message when waiting for
 * the Position Report and once it receives the position report
 * it shows a simple successful message with a button.
 * </p>
 *
 * @see AISDecodingService
 * @see DeploymentActivity
 * @see Runnable
 * @see de.awi.floenavigation.aismessages.AISMessageReceiver
 * @see de.awi.floenavigation.aismessages.PostnReportClassA
 * @see de.awi.floenavigation.aismessages.PostnReportClassB
 * @see de.awi.floenavigation.initialsetup.SetupActivity
 * @see NavigationFunctions
 */
public class AISStationCoordinateFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "AISStationDeployFrag";

    /**
     * Default value to insert in the column {@link DatabaseHelper#predictionTime} when a new {@link
     * DatabaseHelper#fixedStationTable} is
     * deployed.
     */
    private static final double DEFAULT_PREDICTION_TIME = 0.0;

    /**
     * Text which is displayed when a Position report is received, location parameters are
     * calculated and inserted in to the Database successfully.
     */
    private static final String changeText = "AIS Packet Received from the new Station";

    /**
     * MMSI number of the Fixed Station
     */
    private int MMSINumber;

    /**
     * A {@link Handler} which is runs the {@link Runnable} {@link #aisStationRunnable} which
     * periodically checks for the Position Report.
     */
    private final Handler handler = new Handler();

    /**
     * MMSI of the origin Base/Fixed Station
     */
    private int originMMSI;

    /**
     * Current Beta of the Coordinate System
     */
    private double beta;

    /**
     * Current Latitude of the origin
     */
    private double originLatitude;

    /**
     * Current longitude of the origin
     */
    private double originLongitude;

    /**
     * Latitude of the Fixed Station as received from the Position Report
     */
    private double stationLatitude;

    /**
     * Longitude of the Fixed Station as received from the Postion Report
     */
    private double stationLongitude;

    /**
     * Distance of the Fixed Station from the Origin. Calculated between {@link #originLatitude},
     * {@link #originLatitude} and {@link #stationLatitude}, {@link #stationLongitude}
     * using the Haversine formula
     */
    private double distance;

    /**
     * x coordinate of the Fixed Station on the Floe's Coordinate System
     */
    private double stationX;

    /**
     * y coordinate of the Fixed Station on the Floe's Coordinate System
     */
    private double stationY;

    /**
     * The Angle theta of the Fixed Station which it makes with the Geographical Longitudinal Axis
     */
    private double theta;

    /**
     * The Angle alpha of the Fixed Station which it makes with the x-Axis of the Floe's Coordinate
     * System
     */
    private double alpha;

    /**
     * The interval at which the Fragment checks the Database table {@link
     * DatabaseHelper#fixedStationTable} for insertion of Position Report by
     * {@link AISDecodingService}
     */
    private static final int checkInterval = 1000;

    /**
     * Variable for keeping track of the time the fragment waits for the Position Report. It is
     * increment every time the {@link #aisStationRunnable} runs.
     */
    private int autoCancelTimer = 0;

    /**
     * A constant value specifying how long the Fragment will wait for a Position Report. Current
     * value is 300 seconds or 5 minutes.
     */
    private final static int MAX_TIMER = 300; //5 mins timer

    /**
     * A boolean variable which is <code>true</code> when the Position report has been received and
     * the Station is insert in to the Database.
     */
    private boolean isSetupComplete = false;

    /**
     * {@link Runnable} which checks periodically (as specified by {@link #checkInterval}) for the
     * Position report data in {@link DatabaseHelper#fixedStationTable}.
     */
    private Runnable aisStationRunnable;

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
     * {@link BroadcastReceiver} for receiving the GPS location broadcast from {@link GPS_Service}
     */
    private BroadcastReceiver broadcastReceiver;

    /**
     * Default empty constructor
     */
    public AISStationCoordinateFragment() {
        // Required empty public constructor
    }

    /**
     * Default {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}. Reads the MMSI
     * number of the Fixed Station passed to it by {@link StationInstallFragment}.
     * The layout shows a {@link ProgressBar} with a waiting message and a cancel button.
     * <p>
     * The fragment runs the {@link #aisStationRunnable} to check the {@link
     * DatabaseHelper#fixedStationTable} if the position data has been inserted by the {@link
     * AISDecodingService}.
     * If the data is inserted the Fragment calculates the {@link #distance}, {@link #alpha}, {@link
     * #stationX} and {@link #stationY} of the Fixed Station and updates
     * the {@link DatabaseHelper#fixedStationTable} and changes the Layout to show a Finish button
     * pressing which will start the {@link AdminPageActivity}.
     * If the data is not yet inserted the {@link #aisStationRunnable} increments {@link
     * #autoCancelTimer} and re-posts the {@link #aisStationRunnable} with a delay of {@link
     * #checkInterval}.
     * If the {@link #autoCancelTimer} values goes above {@link #MAX_TIMER} then the MMSI is removed
     * from the {@link DatabaseHelper#fixedStationTable} and {@link DatabaseHelper#stationListTable}
     * the {@link #aisStationRunnable} is cancelled and control is returned to {@link
     * StationInstallFragment} to re-enter a valid MMSI number.
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
        View layout = inflater.inflate(R.layout.fragment_station_coordinate, container, false);
        Button button = layout.findViewById(R.id.station_finish);
        button.setOnClickListener(this);

        //layout.findViewById(R.id.station_finish).setClickable(false);
        button.setText(R.string.aisStationCancel);
        MMSINumber = getArguments().getInt(DatabaseHelper.mmsi);
        TextView msg = layout.findViewById(R.id.aisStationFragMsg);
        String waitingMsg = getResources().getString(R.string.stationWaitingMsg, MMSINumber);
        msg.setText(waitingMsg);
        //Log.d(TAG, "Test Message");
        aisStationRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getActivity());
                    ;
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    if (checkForAISPacket(db)) {
                        Log.d(TAG, "AIS Packet Received");
                        if (readParamsFromDatabase(db)) {
                            distance = NavigationFunctions
                                    .calculateDifference(originLatitude, originLongitude,
                                                         stationLatitude, stationLongitude);
                            //                            theta = NavigationFunctions
                            //                            .calculateAngleBeta(originLatitude,
                            //                            originLongitude, stationLatitude,
                            //                            stationLongitude);
                            theta = getTheta(originLatitude, originLongitude, stationLatitude,
                                             stationLongitude);
                            //alpha = Math.abs(theta - beta);
                            alpha = theta - beta;
                            stationX = distance * Math.cos(Math.toRadians(alpha));
                            stationY = distance * Math.sin(Math.toRadians(alpha));
                            ContentValues stationUpdate = new ContentValues();
                            stationUpdate
                                    .put(DatabaseHelper.predictionTime, DEFAULT_PREDICTION_TIME);
                            stationUpdate.put(DatabaseHelper.alpha, alpha);
                            stationUpdate.put(DatabaseHelper.distance, distance);
                            stationUpdate.put(DatabaseHelper.xPosition, stationX);
                            stationUpdate.put(DatabaseHelper.yPosition, stationY);
                            db.update(DatabaseHelper.fixedStationTable, stationUpdate,
                                      DatabaseHelper.mmsi + " = ?",
                                      new String[]{String.valueOf(MMSINumber)});
                            packetReceived();
                        } else {
                            Log.d(TAG, "Error Reading from Database");
                            //Do something here
                            Toast.makeText(getActivity(), "Error in Database. Please Try again",
                                           Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.d(TAG, "Waiting for AIS Packet");
                        autoCancelTimer++;
                        handler.postDelayed(this, checkInterval);
                        if (autoCancelTimer >= MAX_TIMER) {
                            removeMMSIfromDBTable();
                            Toast.makeText(getActivity(), "No relevant packets received",
                                           Toast.LENGTH_LONG).show();
                            handler.removeCallbacks(this);
                            StationInstallFragment stationInstallFragment =
                                    new StationInstallFragment();
                            FragmentChangeListener fc = (FragmentChangeListener) getActivity();
                            fc.replaceFragment(stationInstallFragment);
                        }

                    }
                } catch (SQLiteException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Database Error");
                }
            }

        };
        handler.post(aisStationRunnable);
        setHasOptionsMenu(true);
        return layout;
    }

    private double getTheta(final double originLatitude, final double originLongitude,
                            final double stationLatitude, final double stationLongitude) {
        theta = NavigationFunctions
                .calculateAngleBeta(originLatitude, originLongitude, stationLatitude,
                                    stationLongitude);
        return theta;
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
     * Default Handler for the Finish button on the Screen. It calls the {@link #onClickFinish()}
     * method.
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.station_finish:
                onClickFinish();
        }
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
     * Removes the {@link #MMSINumber} from the Database tables {@link
     * DatabaseHelper#fixedStationTable} and {@link DatabaseHelper#stationListTable}.
     * Called when a position report is not received within the specified time interval or when the
     * cancel button is pressed.
     */
    private void removeMMSIfromDBTable() {
        SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        db.delete(DatabaseHelper.fixedStationTable, DatabaseHelper.mmsi + " = ?",
                  new String[]{String.valueOf(MMSINumber)});
        db.delete(DatabaseHelper.stationListTable, DatabaseHelper.mmsi + " = ?",
                  new String[]{String.valueOf(MMSINumber)});
        Log.d(TAG, "Deleted MMSI from db tables");

    }

    /**
     * Called when an AIS Packet (Position Report) is received from the Fixed Station. It changes
     * the Layout of the fragment.
     * It hides the {@link ProgressBar} and the waiting message and replaces it with a new
     * successful insertion message {@link #changeText}
     * and changes the text of the Cancel button to Finish.
     */
    private void packetReceived() {
        View v = getView();
        ProgressBar progress = v.findViewById(R.id.aisStationProgress);
        progress.stopNestedScroll();
        progress.setVisibility(View.GONE);
        TextView msg = v.findViewById(R.id.aisStationFragMsg);
        msg.setText(changeText);
        //v.findViewById(R.id.station_finish).setClickable(true);
        //v.findViewById(R.id.station_finish).setEnabled(true);
        Button finishButton = v.findViewById(R.id.station_finish);
        finishButton.setText(R.string.stationFinishBtn);
        isSetupComplete = true;

    }

    /**
     * Checks the Database table {@link DatabaseHelper#fixedStationTable} if the Position data of
     * the Fixed Station has been inserted by
     * the {@link AISDecodingService}. If the data is inserted it sets {@link #stationLatitude} and
     * {@link #stationLongitude} to the received values.
     *
     * @return <code>true</code> if packet has been received.
     */
    private boolean checkForAISPacket(SQLiteDatabase db) {
        boolean success = false;
        int locationReceived;
        Cursor cursor = null;
        try {

            cursor = db.query(DatabaseHelper.fixedStationTable,
                              new String[]{DatabaseHelper.mmsi, DatabaseHelper.latitude,
                                           DatabaseHelper.longitude,
                                           DatabaseHelper.isLocationReceived},
                              DatabaseHelper.mmsi + " = ? AND (" + DatabaseHelper.packetType +
                                      " = ? OR " + DatabaseHelper.packetType + " = ? )",
                              new String[]{Integer.toString(MMSINumber), Integer.toString(
                                      AISDecodingService.POSITION_REPORT_CLASSA_TYPE_1),
                                           Integer.toString(
                                                   AISDecodingService.POSITION_REPORT_CLASSB)},
                              null, null, null);
            if (cursor.moveToFirst()) {
                locationReceived = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DatabaseHelper.isLocationReceived));
                if (locationReceived == DatabaseHelper.IS_LOCATION_RECEIVED) {
                    stationLatitude =
                            cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.latitude));
                    stationLongitude = cursor.getDouble(
                            cursor.getColumnIndexOrThrow(DatabaseHelper.longitude));
                    success = true;
                    //Toast.makeText(getActivity(), "Success True", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Packet Recieved from AIS Station");
                }
            }
            //db.close();
        } catch (SQLiteException e) {
            Log.d(TAG, "Database Unavailable");
            Toast.makeText(getActivity(), "Database Unavailable", Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return success;
    }

    /**
     * Handler for the Finish/Cancel Button. If the Fixed Station Deployment is complete meaning its
     * position is received, parameters are calculated
     * and inserted in to the database then the Fragment is finished and {@link AdminPageActivity}
     * is started.
     * If the Deployment is not yet complete that means the current deployment needs to be canceled,
     * the {@link #MMSINumber} is removed
     * from the Database tables and {@link #handler} callbacks are removed and {@link
     * StationInstallFragment} is started to start a new deployment.
     */
    private void onClickFinish() {
        if (isSetupComplete) {
            Toast.makeText(getContext(), "Deployment Complete", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(getActivity(), AdminPageActivity.class);
            getActivity().startActivity(intent);
        } else {
            removeMMSIfromDBTable();
            Log.d(TAG, "AIS Station Installation Cancelled");
            handler.removeCallbacks(aisStationRunnable);
            StationInstallFragment stationInstallFragment = new StationInstallFragment();
            Bundle bundle = new Bundle();
            bundle.putBoolean("stationTypeAIS", true);
            stationInstallFragment.setArguments(bundle);
            FragmentChangeListener fc = (FragmentChangeListener) getActivity();
            if (fc != null) {
                fc.replaceFragment(stationInstallFragment);
            }
        }
    }

    /**
     * Reads the basic parameters of the Floe's Coordinate system from the Database. Reads the
     * values of {@link #originMMSI}, {@link #originLatitude}, {@link #originLongitude},
     * {@link #beta} from their respective tables.
     *
     * @param db An instance {@link SQLiteDatabase}
     * @return <code>true</code> if all the values are read successfully.
     */
    private boolean readParamsFromDatabase(SQLiteDatabase db) {
        Cursor baseStationCursor = null;
        Cursor fixedStationCursor = null;
        Cursor betaCursor = null;
        try {
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
                Log.d(TAG, "Error Reading Origin Latitude Longtidue");
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
                    //Log.d(TAG, String.valueOf(beta));
                } else {
                    Log.d(TAG, "Beta Table Move to first failed");
                }

            } else {
                Log.d(TAG, String.valueOf(betaCursor.getCount()));

                Log.d(TAG, "Error in Beta Table");
                return false;
            }
            return true;
        } catch (SQLiteException e) {
            e.printStackTrace();
            Log.d(TAG, "Error reading Database");
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
     * accordingly.
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
