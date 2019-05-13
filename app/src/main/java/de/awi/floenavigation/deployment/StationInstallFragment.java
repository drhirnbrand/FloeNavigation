package de.awi.floenavigation.deployment;


import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.FragmentChangeListener;
import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.R;


/**
 * This {@link Fragment} runs on top of the {@link DeploymentActivity} and shows the layout for deployment of a Static Station or a Fixed Station
 * depending on the value of {@link DeploymentActivity#aisDeployment}. The Layout for deployment of Fixed Station shows an extra MMSI field whereas
 * if a Static Station is being deployed the MMSI field is hidden and the tablet's Latitude and Longitude are shown.
 *
 * @see BroadcastReceiver
 * @see DeploymentActivity
 * @see Runnable
 */
public class StationInstallFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "StationInstallFragment";

    /**
     * Valid Length of an MMSI Number. Currently set to 9
     */
    private static final int VALID_MMSI_LENGTH = 9;

    /**
     * Default empty constructor
     */
    public StationInstallFragment() {
        // Required empty public constructor
    }

    /**
     * Main View of the Fragment which allows access to its members
     */
    View activityView;
    /**
     * <code>true</code> when the station type being deployed is Fixed Station
     */
    private boolean stationTypeAIS;

    /**
     * {@link BroadcastReceiver} for receiving the GPS location broadcast from {@link GPS_Service}
     */
    private BroadcastReceiver broadcastReceiver;

    /**
     * Current Latitude of the tablet read from the tablet's built-in GPS
     */
    private Double tabletLat;

    /**
     * Current Longitude of the tablet read from the tablet's built-in GPS
     */
    private Double tabletLon;
    /**
     * Sets the format for display of Geographic Coordinates on the Screen.
     * If <code>true</code> the coordinates will be displayed as degree, minutes, seconds
     */
    private boolean changeFormat;
    /**
     * Sets the number of significant figures to show after a decimal point on the screen. This does not affect the calculations.
     * The value is read from {@link DatabaseHelper#decimal_number_significant_figures}
     */
    private int numOfSignificantFigures;
    /**
     * The Icons to show on the Action Bar on the screen.
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
     * A {@link Handler} which is runs a {@link Runnable} object which changes the Action Bar icons colors according to {@link #packetStatus}
     * and {@link #locationStatus}.
     */
    private final Handler statusHandler = new Handler();

    /**
     * {@link BroadcastReceiver} for checking the WiFi connection to an AIS Transponder which is broadcast from {@link de.awi.floenavigation.network.NetworkMonitor}.
     */
    private BroadcastReceiver aisPacketBroadcastReceiver;


    /**
     * Default {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}. Checks the type of station being deployed by reading the arguments
     * passed to it from {@link DeploymentActivity}. If the Fixed Station is being deployed it shows the MMSI field and hides the Tablet's latitude and longitude field
     * If a Static Station is being deployed it hides the MMSI field, shows the Tablet's latitude and longitude and reads the {@link DatabaseHelper#configParametersTable}
     * to set {@link #numOfSignificantFigures} and {@link #changeFormat}.
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_station_install, container, false);

        stationTypeAIS = getArguments().getBoolean("stationTypeAIS");
        layout.findViewById(R.id.station_confirm).setOnClickListener(this);
        if (stationTypeAIS) {
            layout.findViewById(R.id.stationMMSI).setVisibility(View.VISIBLE);
            layout.findViewById(R.id.station_mmsi).setEnabled(true);
            layout.findViewById(R.id.station_type).setVisibility(View.VISIBLE);
            layout.findViewById(R.id.station_type).setEnabled(true);
            layout.findViewById(R.id.staticStationTabletLat).setVisibility(View.GONE);
            layout.findViewById(R.id.staticStationTabletLon).setVisibility(View.GONE);
            layout.findViewById(R.id.station_confirm).setClickable(true);
            layout.findViewById(R.id.station_confirm).setEnabled(true);
            //setHasOptionsMenu(false);
        }else {
            layout.findViewById(R.id.staticStationTabletLat).setVisibility(View.VISIBLE);
            layout.findViewById(R.id.staticStationTabletLon).setVisibility(View.VISIBLE);
            layout.findViewById(R.id.stationMMSI).setVisibility(View.GONE);
            layout.findViewById(R.id.station_type).setVisibility(View.GONE);
            layout.findViewById(R.id.station_confirm).setClickable(true);
            layout.findViewById(R.id.station_confirm).setEnabled(true);
            changeFormat = DatabaseHelper.readCoordinateDisplaySetting(getActivity());
            numOfSignificantFigures = DatabaseHelper.readSiginificantDigitsSetting(getActivity());

        }
        setHasOptionsMenu(true);
        populateStationType(layout);
        return layout;
    }

    /**
     * Default Handler for the Confirm button on the Screen. It checks the Station type being deployed and calls the specific function
     * for the deployment of that type.
     * @param v
     */
    @Override
    public void onClick(View v){
        activityView = getView();
        switch (v.getId()){
            case R.id.station_confirm:
                if(stationTypeAIS) {
                    insertAISStation();
                } else{
                    insertStaticStation();
                }
                break;
        }
    }

    /**
     * Called when Fragment is no longer in foreground. It unregisters the AIS and GPS {@link BroadcastReceiver}s.
     */
    @Override
    public void onPause(){
        super.onPause();
        getActivity().unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        getActivity().unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
    }

    /**
     * Creates the Action Bar icons on top of the screen. For a static station deployment it shows the Option Menu of Changing Lat/Long View format.
     * By default it shows the GPS icon, AIS Connectivity icon and the Grid Setup icon.
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        MenuItem latLonFormat = menu.findItem(R.id.changeLatLonFormat);
        if(!stationTypeAIS){
            latLonFormat.setVisible(true);
           /* */
        } else{
            latLonFormat.setVisible(false);
        }

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

        super.onCreateOptionsMenu(menu,inflater);
    }

    /**
     * Default handler for the Action Bar Option Menu of Change Lat/Lon View format. Only called when a Static Station is deployed.
     * It inverts the value of {@link #changeFormat} and updates its value in {@link DatabaseHelper#configParametersTable} so that the
     * same format is shown throughout the App.
     * @param menuItem
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()){
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
     * Registers and implements the {@link BroadcastReceiver}s for the AIS Connectivity and GPS location broadcasts; which are sent from
     * {@link de.awi.floenavigation.network.NetworkMonitor} and {@link GPS_Service} respectively.
     * The GPS Broadcast receiver sets the value of {@link #locationStatus}, {@link #tabletLat} and {@link #tabletLon} to the values from the {@link GPS_Service}.
     * The AIS Connectivity broadcast receiver sets the boolean {@link #packetStatus}.
     * This also registers {@link Runnable} which runs at a regular interval specified by {@link ActionBarActivity#UPDATE_TIME}  and
     * it checks the booleans {@link #locationStatus} and {@link #packetStatus} and changes the Action Bar icons for GPS and AIS Connectivity
     * accrodingly.
     *
     * @see Runnable
     * @see Handler
     * @see BroadcastReceiver
     * @see ActionBarActivity
     */
    private void actionBarUpdatesFunction() {

        //***************ACTION BAR UPDATES*************************/
        if (broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
                    tabletLat = intent.getExtras().getDouble(GPS_Service.latitude);
                    tabletLon = intent.getExtras().getDouble(GPS_Service.longitude);
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

        getActivity().registerReceiver(aisPacketBroadcastReceiver, new IntentFilter(GPS_Service.AISPacketBroadcast));
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

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
        //****************************************/
    }

    /**
     * Reads the values of {@link #tabletLat} and {@link #tabletLon} and populates it on the Screen formatting the coordinates according
     * as specified by {@link #changeFormat}. Uses the {@link NavigationFunctions#locationInDegrees(double, double)}  to convert the format
     * of the coordinates.
     * @see NavigationFunctions#locationInDegrees(double, double)
     * @see String#format(String, Object...)
     */
    private void populateTabLocation(){
        View v = getView();
        TextView latView = v.findViewById(R.id.staticStationCurrentLat);
        TextView lonView = v.findViewById(R.id.staticStationCurrentLon);
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

    /**
     * @param mmsi the {@link EditText} for the MMSI field on the Layout
     * @return  <code>true</code> if the MMSI field is not empty, contains only digits and has a valid length (specified by {@link #VALID_MMSI_LENGTH})
     */
    private boolean validateMMSINumber(EditText mmsi) {
        return mmsi.getText().length() == VALID_MMSI_LENGTH && !TextUtils.isEmpty(mmsi.getText().toString()) && TextUtils.isDigitsOnly(mmsi.getText().toString());
    }

    /**
     * Inserts a Fixed Station MMSI in the Database tables {@link DatabaseHelper#fixedStationTable} and {@link DatabaseHelper#stationListTable}.
     * Only inserts the Fixed Station if the MMSI and Station name are valid. Before inserting in the Database tables it checks if the Station already
     * exists as a Mobile Station or is already in the Fixed Station Deleted tables. If the MMSI being added is a mobile station it removes from
     * the Database table {@link DatabaseHelper#mobileStationTable} and then inserts the name and MMSI of the Fixed Station to the Database
     * tables {@link DatabaseHelper#fixedStationTable} and {@link DatabaseHelper#stationListTable}. It replaces the current
     * fragment with {@link AISStationCoordinateFragment} to calculate and insert the parameters such as angle Alpha and x, y coordinates
     * of the station. It passes the MMSI number of the Fixed Station to {@link AISStationCoordinateFragment}.
     * @see AISStationCoordinateFragment
     * @see DatabaseHelper
     * @see ContentValues
     */
    private void insertAISStation(){

        EditText mmsi_TV = activityView.findViewById(R.id.station_mmsi);
        EditText stationName_TV = activityView.findViewById(R.id.station_name);

        Spinner stationTypeOption = activityView.findViewById(R.id.stationType);
        String stationType = stationTypeOption.getSelectedItem().toString();
        String stationName = stationName_TV.getText().toString();

        if (TextUtils.isEmpty(stationName_TV.getText().toString())){
            Toast.makeText(getActivity(), "Invalid Station Name", Toast.LENGTH_LONG).show();
            return;
        }

        if (!validateMMSINumber(mmsi_TV)) {
            Toast.makeText(getActivity(), "MMSI Number does not match the requirements", Toast.LENGTH_LONG).show();
            return;
        }

        int mmsi = Integer.parseInt(mmsi_TV.getText().toString());


        try {
            DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            if (checkStationInDBTables(db, mmsi)){
                Toast.makeText(getActivity(), "Duplicate MMSI, AIS Station already exists", Toast.LENGTH_LONG).show();
                return;
            }

            ContentValues station = new ContentValues();
            ContentValues fixedStation = new ContentValues();
            station.put(DatabaseHelper.mmsi, mmsi);
            station.put(DatabaseHelper.stationName, stationName);
            fixedStation.put(DatabaseHelper.mmsi, mmsi);
            fixedStation.put(DatabaseHelper.stationName, stationName);
            fixedStation.put(DatabaseHelper.stationType, stationType);
            fixedStation.put(DatabaseHelper.isLocationReceived, DatabaseHelper.IS_LOCATION_RECEIVED_INITIAL_VALUE);
            //Synchronize Delete from Mobile Station Table and Insertion in Station List Table so that
            //Decoding Service would not create the MMSI in Mobile Station Table again.
            synchronized (this) {
                if (checkStationInMobileTable(db, mmsi)) {
                    db.delete(DatabaseHelper.mobileStationTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
                    Log.d(TAG, "Station Removed from Mobile Station Table");
                }

                if (checkStationInFixedDeleteTable(db, mmsi)) {
                    db.delete(DatabaseHelper.fixedStationDeletedTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
                    Log.d(TAG, "Station Removed from Fixed Station Delete Table");
                }

                if (checkStationInStationListDeleteTable(db, mmsi)) {
                    db.delete(DatabaseHelper.stationListDeletedTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
                    Log.d(TAG, "Station Removed from Station List Delete Table");
                }
                db.insert(DatabaseHelper.stationListTable, null, station);
                db.insert(DatabaseHelper.fixedStationTable, null, fixedStation);
            }
            AISStationCoordinateFragment aisFragment = new AISStationCoordinateFragment();
            Bundle argument = new Bundle();
            argument.putInt(DatabaseHelper.mmsi, mmsi);
            aisFragment.setArguments(argument);
            FragmentChangeListener fc = (FragmentChangeListener) getActivity();
            if (fc != null) {
                fc.replaceFragment(aisFragment);
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
            Log.d(TAG, "Database Unavailable");
        }


    }

    /**
     * Checks the validity of the Station Name of the station and if the current tablet location is available.
     * Checks if the Station already exists if so it shows a {@link Toast} message to that effect.
     * Otherwise it replaces the current fragment with {@link StaticStationFragment} to insert the static station in the Database.
     * It passes the current tablet location and the station name and type to {@link StaticStationFragment}.
     * @see StaticStationFragment
     * @see DatabaseHelper
     * @see ContentValues
     * @see Toast
     */
    private void insertStaticStation(){

        EditText stationName_TV = activityView.findViewById(R.id.station_name);
        String stationName = stationName_TV.getText().toString();
        Spinner stationTypeOption = activityView.findViewById(R.id.stationType);
        String stationType = stationTypeOption.getSelectedItem().toString();
        tabletLat = (tabletLat == null) ? 0.0 : tabletLat;
        tabletLon = (tabletLon == null) ? 0.0 : tabletLon;
        if(tabletLat != 0.0 && tabletLon != 0.0) {
            if (!TextUtils.isEmpty(stationName_TV.getText().toString())) {

                DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (checkStaticStationInDBTables(db, stationName)) {
                    Toast.makeText(getActivity(), "Duplicate Static Station, Station already exists", Toast.LENGTH_LONG).show();
                    return;
                }
                StaticStationFragment stationFragment = new StaticStationFragment();
                Bundle arguments = new Bundle();
                arguments.putString(DatabaseHelper.staticStationName, stationName);
                arguments.putString(DatabaseHelper.stationType, stationType);
                arguments.putDouble(GPS_Service.latitude, tabletLat);
                arguments.putDouble(GPS_Service.longitude, tabletLon);
                stationFragment.setArguments(arguments);
                FragmentChangeListener fc = (FragmentChangeListener) getActivity();
                fc.replaceFragment(stationFragment);

            } else {
                Toast.makeText(getActivity(), "Invalid Station Name", Toast.LENGTH_LONG).show();
            }
        }else{
            Log.d(TAG, "Error with GPS Service");
            Toast.makeText(getActivity(), "Error reading Device Lat and Long", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Called when the Fragment come back from background to foreground. Enables the Up Button and calls the
     * {@link #actionBarUpdatesFunction()} to set the correct icon colors in the Action Bar.
     */
    @Override
    public void onResume(){
        super.onResume();
        DeploymentActivity activity = (DeploymentActivity)getActivity();
        if(activity != null){
            activity.showUpButton();
        }
        actionBarUpdatesFunction();
    }

    /**
     * Populates the {@link Spinner} with values of Station Type from {@link DatabaseHelper#stationTypes}.
     * Static Stations and Fixed Stations both have the same list of Station Types.
     * @param v
     */
    private void populateStationType(View v){
        List<String> stationList = new ArrayList<String>();
        /*for(int i = 0; i < DatabaseHelper.stationTypes.length; i++){
            stationList.add(DatabaseHelper.stationTypes[i]);
        }*/
        stationList.addAll(Arrays.asList(DatabaseHelper.stationTypes));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(), R.layout.spinner_item, stationList
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner stationType = v.findViewById(R.id.stationType);
        stationType.setAdapter(adapter);
    }

    /**
     * Checks if the given MMSI already exists in {@link DatabaseHelper#fixedStationTable} and {@link DatabaseHelper#stationListTable}
     * @param db An instance {@link SQLiteDatabase}
     * @param MMSI The MMSI number to be checked
     * @return <code>true</code> if the given MMSI exists in {@link DatabaseHelper#fixedStationTable} and {@link DatabaseHelper#stationListTable}
     */
    private boolean checkStationInDBTables(SQLiteDatabase db, int MMSI){
        boolean isPresent = false;
        Cursor mStationListCursor = null;
        Cursor mFixedStnCursor = null;
        try{
            mStationListCursor = db.query(DatabaseHelper.stationListTable, new String[]{DatabaseHelper.mmsi}, DatabaseHelper.mmsi + " = ?",
                    new String[]{String.valueOf(MMSI)}, null, null, null);
            mFixedStnCursor = db.query(DatabaseHelper.fixedStationTable, new String[]{DatabaseHelper.mmsi}, DatabaseHelper.mmsi + " = ?",
                    new String[]{String.valueOf(MMSI)}, null, null, null);
            isPresent = mStationListCursor.moveToFirst() && mFixedStnCursor.moveToFirst();
            return isPresent;
        }catch (SQLException e){
            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
            return isPresent;
        } finally {
            if (mStationListCursor != null){
                mStationListCursor.close();
            }
            if (mFixedStnCursor != null){
                mFixedStnCursor.close();
            }
        }
    }

    /**
     * Checks if the given MMSI already exists in {@link DatabaseHelper#mobileStationTable}
     * @param db An instance {@link SQLiteDatabase}
     * @param MMSI The MMSI number to be checked
     * @return <code>true</code> if the given MMSI exists in {@link DatabaseHelper#mobileStationTable}
     */
    private boolean checkStationInMobileTable(SQLiteDatabase db, int MMSI){
        boolean isPresent = false;
        Cursor mMobileStationCursor = null;
        try{
            mMobileStationCursor = db.query(DatabaseHelper.mobileStationTable, new String[]{DatabaseHelper.mmsi}, DatabaseHelper.mmsi + " = ?",
                    new String[]{String.valueOf(MMSI)}, null, null, null);
            isPresent = mMobileStationCursor.moveToFirst();
        }catch (SQLException e){
            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        } finally {
            if (mMobileStationCursor != null){
                mMobileStationCursor.close();
            }
        }
        return isPresent;
    }

    /**
     * Checks if the given MMSI already exists in {@link DatabaseHelper#fixedStationDeletedTable}
     * @param db An instance {@link SQLiteDatabase}
     * @param MMSI The MMSI number to be checked
     * @return <code>true</code> if the given MMSI exists in {@link DatabaseHelper#fixedStationDeletedTable}
     */
    private boolean checkStationInFixedDeleteTable(SQLiteDatabase db, int MMSI){
        boolean isPresent = false;
        Cursor mFixedStationDeleteCursor = null;
        try{
            mFixedStationDeleteCursor = db.query(DatabaseHelper.fixedStationDeletedTable, new String[]{DatabaseHelper.mmsi}, DatabaseHelper.mmsi + " = ?",
                    new String[]{String.valueOf(MMSI)}, null, null, null);
            isPresent = mFixedStationDeleteCursor.moveToFirst();
        }catch (SQLException e){
            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        } finally {
            if (mFixedStationDeleteCursor != null){
                mFixedStationDeleteCursor.close();
            }
        }

        return isPresent;
    }

    /**
     * Checks if the given MMSI already exists in {@link DatabaseHelper#stationListDeletedTable}
     * @param db An instance {@link SQLiteDatabase}
     * @param MMSI The MMSI number to be checked
     * @return <code>true</code> if the given MMSI exists in {@link DatabaseHelper#stationListDeletedTable}
     */
    private boolean checkStationInStationListDeleteTable(SQLiteDatabase db, int MMSI){
        boolean isPresent = false;
        Cursor mStationListDeleteCursor = null;
        try{
            mStationListDeleteCursor = db.query(DatabaseHelper.stationListDeletedTable, new String[]{DatabaseHelper.mmsi}, DatabaseHelper.mmsi + " = ?",
                    new String[]{String.valueOf(MMSI)}, null, null, null);

            isPresent =  mStationListDeleteCursor.moveToFirst();
        }catch (SQLException e){
            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        } finally {
            if (mStationListDeleteCursor != null){
                mStationListDeleteCursor.close();
            }
        }
        return isPresent;
    }

    /**
     * Checks if the given MMSI already exists in {@link DatabaseHelper#staticStationListTable}
     * @param db An instance {@link SQLiteDatabase}
     * @param stationName The name of the Station to be checked
     * @return <code>true</code> if the given Station name exists in {@link DatabaseHelper#staticStationListTable}
     */
    private boolean checkStaticStationInDBTables(SQLiteDatabase db, String stationName){
        boolean isPresent = false;
        Cursor mStaticStationListCursor = null;
        try{
            mStaticStationListCursor = db.query(DatabaseHelper.staticStationListTable, new String[]{DatabaseHelper.staticStationName}, DatabaseHelper.staticStationName + " = ?",
                    new String[]{stationName}, null, null, null);

            isPresent =  mStaticStationListCursor.moveToFirst();
        }catch (SQLException e){
            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        } finally {
            if(mStaticStationListCursor != null){
                mStaticStationListCursor.close();
            }
        }
        return isPresent;
    }


}
