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
 * A simple {@link Fragment} subclass.
 */
public class StationInstallFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "StationInstallFragment";
    private static final int VALID_MMSI_LENGTH = 9;

    public StationInstallFragment() {
        // Required empty public constructor
    }

    View activityView;
    private boolean stationTypeAIS;
    private BroadcastReceiver broadcastReceiver;
    private Double tabletLat;
    private Double tabletLon;
    private boolean changeFormat;
    private int numOfSignificantFigures;
    private MenuItem gpsIconItem, aisIconItem, gridSetupIconItem;
    private boolean locationStatus = false;
    private boolean packetStatus = false;
    private final Handler statusHandler = new Handler();
    private BroadcastReceiver aisPacketBroadcastReceiver;


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
            layout.findViewById(R.id.staticStationTabletLat).setVisibility(View.GONE);
            layout.findViewById(R.id.staticStationTabletLon).setVisibility(View.GONE);
            layout.findViewById(R.id.station_confirm).setClickable(true);
            layout.findViewById(R.id.station_confirm).setEnabled(true);
            //setHasOptionsMenu(false);
        }else {
            layout.findViewById(R.id.staticStationTabletLat).setVisibility(View.VISIBLE);
            layout.findViewById(R.id.staticStationTabletLon).setVisibility(View.VISIBLE);
            layout.findViewById(R.id.stationMMSI).setVisibility(View.GONE);
            layout.findViewById(R.id.station_confirm).setClickable(true);
            layout.findViewById(R.id.station_confirm).setEnabled(true);
            changeFormat = DatabaseHelper.readCoordinateDisplaySetting(getActivity());
            numOfSignificantFigures = DatabaseHelper.readSiginificantDigitsSetting(getActivity());

        }
        setHasOptionsMenu(true);
        populateStationType(layout);
        return layout;
    }

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

    @Override
    public void onPause(){
        super.onPause();
        getActivity().unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        getActivity().unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
    }

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

    private boolean validateMMSINumber(EditText mmsi) {
        return mmsi.getText().length() == VALID_MMSI_LENGTH && !TextUtils.isEmpty(mmsi.getText().toString()) && TextUtils.isDigitsOnly(mmsi.getText().toString());
    }

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

    @Override
    public void onResume(){
        super.onResume();
        DeploymentActivity activity = (DeploymentActivity)getActivity();
        if(activity != null){
            activity.showUpButton();
        }
        actionBarUpdatesFunction();
    }

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
