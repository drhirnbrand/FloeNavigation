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
import android.support.v4.app.Fragment;
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

import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class StaticStationFragment extends Fragment implements View.OnClickListener{

    private static final String TAG = "StaticStationDeployFrag";
    private static final String changeText = "Station Installed";

    private String stationName;
    private String stationType;
    private BroadcastReceiver broadcastReceiver;
    private double tabletLat;
    private double tabletLon;
    private double originLatitude;
    private double originLongitude;
    private int originMMSI;
    private double beta;
    private double distance;
    private double alpha;
    private double xPosition;
    private double yPosition;
    private double theta;
    private MenuItem gpsIconItem, aisIconItem, gridSetupIconItem;
    private boolean locationStatus = false;
    private boolean packetStatus = false;
    private final Handler statusHandler = new Handler();
    private BroadcastReceiver aisPacketBroadcastReceiver;

    public StaticStationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout =  inflater.inflate(R.layout.fragment_static_station, container, false);

        layout.findViewById(R.id.static_station_finish).setOnClickListener(this);
        layout.findViewById(R.id.static_station_finish).setEnabled(false);
        layout.findViewById(R.id.static_station_finish).setClickable(false);
        stationName = getArguments().getString(DatabaseHelper.staticStationName);
        stationType = getArguments().getString(DatabaseHelper.stationType);
        tabletLat = getArguments().getDouble(GPS_Service.latitude);
        tabletLon = getArguments().getDouble(GPS_Service.longitude);
        if(getOriginCoordinates()) {
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

        } else{
            Log.d(TAG, "Error Inserting new Station");
        }
        setHasOptionsMenu(true);
        return layout;
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.static_station_finish:
                Intent mainIntent = new Intent(getActivity(), MainActivity.class);
                getActivity().startActivity(mainIntent);

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
    public void onResume(){
        super.onResume();
        DeploymentActivity activity = (DeploymentActivity) getActivity();
        if(activity != null){
            activity.hideUpButton();
        }
        actionBarUpdatesFunction();

    }

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

    private void calculateStaticStationParameters(){
        distance = NavigationFunctions.calculateDifference(tabletLat, tabletLon, originLatitude, originLongitude);
        theta = NavigationFunctions.calculateAngleBeta(tabletLat, tabletLon, originLatitude, originLongitude);
        alpha = Math.abs(theta - beta);
        Log.d(TAG, "StationDistance: " + String.valueOf(distance));
        Log.d(TAG, "OriginLat: " + String.valueOf(originLatitude) + " OriginLon: " + String.valueOf(originLongitude));
        Log.d(TAG, "TabletLat: " + String.valueOf(tabletLat) + " TabletLon: " + String.valueOf(tabletLon));
        xPosition = distance * Math.cos(Math.toRadians(alpha));
        yPosition = distance * Math.sin(Math.toRadians(alpha));

    }

    private void insertStaticStation(){
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

    private boolean getOriginCoordinates(){

        try {
            DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(getActivity());
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
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



    private void actionBarUpdatesFunction() {

        //***************ACTION BAR UPDATES*************************/
        if (broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
                    //setTabletLat(intent.getExtras().getDouble(GPS_Service.latitude));
                    //setTabletLon(intent.getExtras().getDouble(GPS_Service.longitude));

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


}
