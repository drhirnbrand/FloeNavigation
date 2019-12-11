package de.awi.floenavigation.initialsetup;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.List;

import de.awi.floenavigation.R;
import de.awi.floenavigation.aismessages.AISDecodingService;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.FragmentChangeListener;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.util.LocationUtils;

/**
 * This {@link Fragment} runs on top of the {@link GridSetupActivity} and displays the location of
 * the Fixed Station which is being deployed.
 * <p>
 * The {@link MMSIFragment} inserts the name and MMSI number in to the tables {@link
 * DatabaseHelper#baseStationTable}, {@link DatabaseHelper#fixedStationTable}
 * and {@link DatabaseHelper#stationListTable}. This fragment waits and checks the {@link
 * DatabaseHelper#fixedStationTable}
 * to see if a Position Report has been received from the given MMSI number.
 * The Position Report {@link de.awi.floenavigation.aismessages.PostnReportClassA} and {@link
 * de.awi.floenavigation.aismessages.PostnReportClassB} are received, decoded
 * and inserted in to the Database by {@link AISDecodingService}.
 * If a position report is received the fragment displays the location data (Latitude and Longitude)
 * on screen along with the tablet's own location.
 * If the position report is not received for a specified time the {@link MMSIFragment} is called
 * again to re-enter MMSI number.
 * The Layout of this fragment shows a {@link ProgressBar} with a waiting message when waiting for
 * the Position Report
 * and once it receives the position report it shows the location along with a button.
 * It also checks the length of {@link DatabaseHelper#stationListTable} in the button callback, if
 * the length is less than 2 it will start {@link MMSIFragment}
 * and if the length is 2 {@link SetupActivity} will be started.
 * </p>
 *
 * @see AISDecodingService
 * @see GridSetupActivity
 * @see Runnable
 * @see de.awi.floenavigation.aismessages.AISMessageReceiver
 * @see de.awi.floenavigation.aismessages.PostnReportClassA
 * @see de.awi.floenavigation.aismessages.PostnReportClassB
 * @see de.awi.floenavigation.initialsetup.SetupActivity
 * @see NavigationFunctions
 */
public class CoordinateFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "CoordinateFragment";

    /**
     * Default empty constructor
     */
    public CoordinateFragment() {
        // Required empty public constructor
    }

    /**
     * MMSI number of the Fixed Station
     */
    private int MMSINumber;

    /**
     * Latitude of the Fixed Station as received from the Position Report
     */
    private double latitude;

    /**
     * Longitude of the Fixed Station as received from the Postion Report
     */
    private double longitude;

    /**
     * Station Name of the Fixed Station
     */
    private String stationName;

    /**
     * {@link BroadcastReceiver} for receiving the GPS location broadcast from {@link GPS_Service}
     */
    private BroadcastReceiver broadcastReceiver;

    /**
     * A {@link Handler} which is runs the {@link Runnable} {@link #fragRunnable} which periodically
     * checks for the Position Report.
     */
    private final Handler handler = new Handler();

    /**
     * {@link Runnable} which checks periodically (as specified by {@link #checkInterval}) for the
     * Position report data in {@link DatabaseHelper#fixedStationTable}.
     */
    private Runnable fragRunnable;

    /**
     * Current Latitude of the tablet read from the tablet's built-in GPS
     */
    private String tabletLat;

    /**
     * Current Longitude of the tablet read from the tablet's built-in GPS
     */
    private String tabletLon;
    /*private long tabletTime;*/
    /**
     * Variable to check if position report has been received.
     * <code>true</code> when a position report has been received and now layout of the Fragment has
     * to be changed.
     */
    private boolean isConfigDone;

    /**
     * Number stations inserted in Database. This helps in deciding whether the current station is
     * Origin or x-Axis Marker.
     */
    private long countAIS;

    /**
     * The interval at which the Fragment checks the Database table {@link
     * DatabaseHelper#fixedStationTable} for insertion of Position Report by
     * {@link AISDecodingService}
     */
    private static final int checkInterval = 1000;

    /**
     * Sets the format for display of Geographic Coordinates on the Screen.
     * If <code>true</code> the coordinates will be displayed as degree, minutes, seconds
     */
    private boolean changeFormat;

    /**
     * Sets the number of significant figures to show after a decimal point on the screen. This does
     * not affect the calculations.
     * The value is read from {@link DatabaseHelper#decimal_number_significant_figures}
     */
    private int numOfSignificantFigures;
    /**
     * Variable for keeping track of the time the fragment waits for the Position Report. It is
     * increment every time the {@link #fragRunnable} runs.
     */
    private int autoCancelTimer = 0;

    /**
     * A constant value specifying how long the Fragment will wait for a Position Report. Current
     * value is 300 seconds or 5 minutes.
     */
    private final static int MAX_TIMER = 300; //5 mins timer


    /**
     * Default {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}. Reads the MMSI
     * number and station name of the Fixed Station passed to it by {@link MMSIFragment}.
     * The default layout shows a {@link ProgressBar} with a waiting message and a cancel button.
     * <p>
     * The fragment runs the {@link #fragRunnable} to check the {@link
     * DatabaseHelper#fixedStationTable} if the position data has been inserted by the {@link
     * AISDecodingService}.
     * If the data is inserted the Fragment changes its layout to display the received location and
     * the current tablet location.
     * If the data is not yet inserted the {@link #fragRunnable} increments {@link #autoCancelTimer}
     * and re-posts the {@link #fragRunnable} with a delay of {@link #checkInterval}.
     * If the {@link #autoCancelTimer} values goes above {@link #MAX_TIMER} then the MMSI is removed
     * from the
     * {@link DatabaseHelper#baseStationTable}, {@link DatabaseHelper#fixedStationTable} and {@link
     * DatabaseHelper#stationListTable},
     * the {@link #fragRunnable} is cancelled and control is returned to {@link MMSIFragment} to
     * re-enter a valid MMSI number.
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
        View layout = inflater.inflate(R.layout.fragment_coordinate, container, false);
        Button confirmButtonCoordinates = layout.findViewById(R.id.confirm_Coordinates);
        confirmButtonCoordinates.setOnClickListener(this);
        Button progressCancelButton = layout.findViewById(R.id.progressCancelBtn);
        progressCancelButton.setOnClickListener(this);

        changeFormat = DatabaseHelper.readCoordinateDisplaySetting(getActivity());
        numOfSignificantFigures = DatabaseHelper.readSiginificantDigitsSetting(getActivity());
        //configureTabLocation();
        if (savedInstanceState != null) {
            isConfigDone = savedInstanceState.getBoolean("isConfigDone");
        }
        MMSINumber = getArguments().getInt(DatabaseHelper.mmsi);
        stationName = getArguments().getString(DatabaseHelper.stationName);
        String progressMsg = getResources().getString(R.string.waitingMsg, MMSINumber);
        TextView waitingView = layout.findViewById(R.id.waitingMsgID);
        waitingView.setText(progressMsg);
        setHasOptionsMenu(true);
        fragRunnable = new Runnable() {
            @Override
            public void run() {
                if (checkForCoordinates()) {

                    isConfigDone = true;
                    //show the packet received
                    changeLayout();
                    populateTabLocation();
                } else {
                    //Toast.makeText(getActivity(), "In Coordinate Fragment", Toast.LENGTH_LONG)
                    // .show();
                    Log.d(TAG, "Waiting for AIS Packet");
                    autoCancelTimer++;
                    handler.postDelayed(this, checkInterval);

                    if (autoCancelTimer >= MAX_TIMER) {
                        removeMMSIfromDBTable();
                        callMMSIFragment();
                        Toast.makeText(getActivity(), "No relevant packets received",
                                       Toast.LENGTH_LONG).show();
                        handler.removeCallbacks(this);
                    }
                }
            }
        };
        return layout;
    }

    /**
     * Changes the Fragment to {@link MMSIFragment}
     */
    private void callMMSIFragment() {
        MMSIFragment mmsiFragment = new MMSIFragment();
        FragmentChangeListener fc = (FragmentChangeListener) getActivity();
        fc.replaceFragment(mmsiFragment);
    }

    /**
     * Called when the Fragment come back from background to foreground. Disables the Up Button.
     */
    @Override
    public void onResume() {
        super.onResume();
        GridSetupActivity activity = (GridSetupActivity) getActivity();
        if (activity != null) {
            activity.hideUpButton();
        }
    }

    /**
     * Default handler for the Action Bar Option Menu of Change Lat/Lon View format.
     * It inverts the value of {@link #changeFormat} and updates its value in {@link
     * DatabaseHelper#configParametersTable} so that the
     * same format is shown throughout the App.
     *
     * @param menuItem
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.changeLatLonFormat:
                changeFormat = !changeFormat;
                populateTabLocation();
                DatabaseHelper.updateCoordinateDisplaySetting(getActivity(), changeFormat);
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    /**
     * Checks {@link #isConfigDone} to see which layout to display. If it is <code>true</code>
     * meaning the AIS Position Report has been
     * received the fragment shows the layout displaying the coordinates of the AIS Station and
     * Tablet. Else it displays the waiting view.
     * Registers and implements the {@link BroadcastReceiver} for the GPS location broadcasts; which
     * is sent from {@link GPS_Service}.
     * The GPS Broadcast receiver sets the value of {@link #tabletLat} and {@link #tabletLat} to the
     * value from the {@link GPS_Service}.
     */
    @Override
    public void onStart() {
        super.onStart();
        if (isConfigDone) {
            changeLayout();
            populateTabLocation();
        } else {
            handler.post(fragRunnable);
        }
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //Log.d(TAG, "BroadCast Received");
                    /*String coordinateString = intent.getExtras().get("coordinates").toString();
                    String[] coordinates = coordinateString.split(",");*/
                    tabletLat = intent.getExtras().get(GPS_Service.latitude).toString();
                    tabletLon = intent.getExtras().get(GPS_Service.longitude).toString();
                    //tabletTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime)
                    // .toString());

                    //Log.d(TAG, "Tablet Loc: " + tabletLat);
                    //Toast.makeText(getActivity(),"Received Broadcast", Toast.LENGTH_LONG).show();
                    populateTabLocation();
                }
            };
        }
        getActivity()
                .registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));
    }

    /**
     * Called when the Fragment is no longer in foreground. It unregisters the GPS {@link
     * BroadcastReceiver}.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (broadcastReceiver != null) {
            getActivity().unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    /**
     * Called before {@link Fragment#onStop()}. Saves the value of {@link #isConfigDone} in a {@link
     * Bundle} so that if the App is run again
     * it will load the correct layout.
     *
     * @param savedInstanceState
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("isConfigDone", isConfigDone);
    }

    /**
     * Checks the Database table {@link DatabaseHelper#fixedStationTable} if the Position data of
     * the Fixed Station has been inserted by
     * the {@link AISDecodingService}. If the data is inserted it sets {@link #latitude} and {@link
     * #longitude} to the received values.
     *
     * @return <code>true</code> if packet has been received.
     */
    private boolean checkForCoordinates() {
        int index;
        boolean success = false;
        Cursor cursor = null;
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getActivity());
            ;
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            countAIS = DatabaseUtils.queryNumEntries(db, DatabaseHelper.stationListTable);
            Log.d(TAG, "countAIS:" + String.valueOf(countAIS));
            cursor = db.query(DatabaseHelper.fixedStationTable,
                              new String[]{DatabaseHelper.stationName, DatabaseHelper.recvdLatitude,
                                           DatabaseHelper.recvdLongitude,
                                           DatabaseHelper.isLocationReceived},
                              DatabaseHelper.mmsi + " = ? AND (" + DatabaseHelper.packetType +
                                      " = ? OR " + DatabaseHelper.packetType + " = ? )",
                              new String[]{Integer.toString(MMSINumber), Integer.toString(
                                      AISDecodingService.POSITION_REPORT_CLASSA_TYPE_1),
                                           Integer.toString(
                                                   AISDecodingService.POSITION_REPORT_CLASSB)},
                              null, null, null);
            if (cursor.moveToFirst()) {
                //stationName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper
                // .stationName));
                //index = cursor.getColumnIndexOrThrow(DatabaseHelper.latitude);
                latitude = cursor.getDouble(
                        cursor.getColumnIndexOrThrow(DatabaseHelper.recvdLatitude));

                longitude = cursor.getDouble(
                        cursor.getColumnIndexOrThrow(DatabaseHelper.recvdLongitude));
                int locationReceived = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DatabaseHelper.isLocationReceived));
                //Toast.makeText(getActivity(), "LocationReceived: " + String.valueOf
                // (locationReceived), Toast.LENGTH_LONG).show();
                if (locationReceived == DatabaseHelper.IS_LOCATION_RECEIVED) {
                    success = true;
                    //Toast.makeText(getActivity(), "Success True", Toast.LENGTH_LONG).show();
                }
            }
            cursor.close();
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
     * Called when an AIS Packet (Position Report) is received from the Fixed Station. It changes
     * the Layout of the fragment.
     * It hides the {@link ProgressBar} and the waiting message and replaces it with a new layout
     * which shows the Latitude and Longitude
     * of the AIS Station and Tablet.
     * If this is the second station being deployed (meaning it is the x-Axis marker) it changes the
     * text of the Next button to 'Start Setup'.
     */
    private void changeLayout() {
        View v = getView();
        LinearLayout waitingLayout = null;
        if (v != null) {
            waitingLayout = v.findViewById(R.id.waitingView);
            waitingLayout.setVisibility(View.GONE);
            LinearLayout coordinateLayout = v.findViewById(R.id.coordinateView);
            coordinateLayout.setVisibility(View.VISIBLE);
            v.findViewById(R.id.ais_station).setEnabled(false);
            v.findViewById(R.id.ais_station_latitude).setEnabled(false);
            v.findViewById(R.id.ais_station_longitude).setEnabled(false);
            v.findViewById(R.id.tablet_latitude).setEnabled(false);
            v.findViewById(R.id.tablet_longitude).setEnabled(false);
            if (countAIS == 2) {
                Button confirmButton = v.findViewById(R.id.confirm_Coordinates);
                confirmButton.setText(R.string.startSetup);
            }
        } else {
            Log.d(TAG, "view is null");
        }

    }

    /**
     * Populates the current latitude and longitude of the tablet on their respective fields on the
     * layout.
     * If the current latitude and longitude are not available it will populate the last know
     * location from
     * {@link #getLastKnownLocation()}.
     */
    private void populateTabLocation() {
        View v = getView();
        TextView tabLat = null;
        TextView tabLon = null;
        TextView aisLat = null;
        TextView aisLon = null;
        TextView aisName = null;
        if (v != null) {
            tabLat = v.findViewById(R.id.tablet_latitude);
            tabLon = v.findViewById(R.id.tablet_longitude);
            aisLat = v.findViewById(R.id.ais_station_latitude);
            aisLon = v.findViewById(R.id.ais_station_longitude);
            aisName = v.findViewById(R.id.ais_station);
        } else {
            Log.d(TAG, "view is null");
        }
        //tabletLat = (tabletLat == null) ? "0.0" : tabletLat;
        //tabletLon = (tabletLon == null) ? "0.0" : tabletLat;
        if (tabletLat == null || tabletLat.isEmpty()) {
            try {
                //locationManager = (LocationManager) getActivity().getSystemService(Context
                // .LOCATION_SERVICE);

                //assert locationManager != null;
                if (getLastKnownLocation() != null) {
                    tabletLat = String.valueOf(getLastKnownLocation().getLatitude());
                } /*else {
                    tabletLat = "0.0";
                }*/


            } catch (SecurityException e) {
                Toast.makeText(getActivity(), "Location Service Problem", Toast.LENGTH_LONG).show();
            }
        }
        if (tabletLon == null || tabletLon.isEmpty()) {
            try {
                //locationManager = (LocationManager) getActivity().getSystemService(Context
                // .LOCATION_SERVICE);
                if (getLastKnownLocation() != null) {
                    tabletLon = String.valueOf(getLastKnownLocation().getLongitude());
                } /*else{
                    tabletLon = "0.0";
                }*/

            } catch (SecurityException e) {
                Toast.makeText(getActivity(), "Location Service Problem", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Location Service Not Available");
                e.printStackTrace();
            }
        }
        String formatString = "%." + String.valueOf(numOfSignificantFigures) + "f";
        String[] tabletFormattedCoordinates = NavigationFunctions
                .locationInDegrees(Double.valueOf(tabletLat), Double.valueOf(tabletLon));
        String[] stationFormattedCoordinates =
                NavigationFunctions.locationInDegrees(latitude, longitude);
        if (tabLat != null) {
            tabLat.setEnabled(true);
            if (changeFormat) {
                tabLat.setText(tabletFormattedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
            } else {
                Double tabletLatitude = Double.valueOf(tabletLat);
                tabLat.setText(String.format(formatString, tabletLatitude));
            }
            tabLat.setEnabled(false);
        }


        if (tabLon != null) {
            tabLon.setEnabled(true);
            if (changeFormat) {
                tabLon.setText(tabletFormattedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
            } else {
                Double tabletLongitude = Double.valueOf(tabletLon);
                tabLon.setText(String.format(formatString, tabletLongitude));
            }
            tabLon.setEnabled(false);
        }

        if (aisName != null) {
            aisName.setEnabled(true);
            aisName.setText(stationName);
            aisName.setEnabled(false);
        }

        if (aisLat != null) {
            aisLat.setEnabled(true);
            if (changeFormat) {
                aisLat.setText(stationFormattedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
            } else {
                aisLat.setText(String.format(formatString, latitude));
            }
            aisLat.setEnabled(false);
        }

        if (aisLon != null) {
            aisLon.setEnabled(true);
            if (changeFormat) {
                aisLon.setText(stationFormattedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
            } else {
                aisLon.setText(String.format(formatString, longitude));
            }
            aisLon.setEnabled(false);
        }

    }

    /**
     * Default callback method for the Buttons on the screen. Depending on the button tapped calls
     * their respective callback functions.
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.progressCancelBtn:
                removeMMSIfromDBTable();
                handler.removeCallbacks(fragRunnable);
                callMMSIFragment();
                break;
            case R.id.confirm_Coordinates:
                onClickBtn();
                break;
        }
    }

    /**
     * Removes the {@link #MMSINumber} from the Database tables {@link
     * DatabaseHelper#fixedStationTable}, {@link DatabaseHelper#baseStationTable} and
     * {@link DatabaseHelper#stationListTable}.
     * Called when a position report is not received within the specified time interval or when the
     * cancel button is pressed.
     */
    private void removeMMSIfromDBTable() {
        SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        db.delete(DatabaseHelper.baseStationTable, DatabaseHelper.mmsi + " = ?",
                  new String[]{String.valueOf(MMSINumber)});
        db.delete(DatabaseHelper.fixedStationTable, DatabaseHelper.mmsi + " = ?",
                  new String[]{String.valueOf(MMSINumber)});
        db.delete(DatabaseHelper.stationListTable, DatabaseHelper.mmsi + " = ?",
                  new String[]{String.valueOf(MMSINumber)});
        Log.d(TAG, "Deleted MMSI from db tables");

    }

    /**
     * Callback function for Next/Start Setup button on the Screen. Checks the number of Fixed/Base
     * Stations installed. If two stations
     * (meaning both Origin and x-Axis Marker) have been deployed it starts the {@link
     * SetupActivity}. If only one station is installed
     * (meaning only Origin) it will call the {@link MMSIFragment} to insert the second station
     * (x-Axis marker).
     */
    private void onClickBtn() {
        if (countAIS < 2) {
            callMMSIFragment();
        } else {
            Intent intent = new Intent(getActivity(), SetupActivity.class);
            intent.putExtra(SetupActivity.calledFromCoordinateFragment, true);
            startActivity(intent);
        }
    }

    /**
     * Return the last known location by reading all last location from all {@link
     * android.location.LocationProvider}s.
     *
     * @return the most accurate last known {@link Location}
     */
    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        return LocationUtils.getLastKnownLocation(getActivity());
    }


}
