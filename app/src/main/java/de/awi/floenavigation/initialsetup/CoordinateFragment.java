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
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.aismessages.AISDecodingService;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.FragmentChangeListener;
import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class CoordinateFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "CoordinateFragment";


    public CoordinateFragment() {
        // Required empty public constructor
    }


    private int MMSINumber;
    private double latitude;
    private double longitude;
    private String stationName;
    private LocationManager locationManager;
    private LocationListener listener;
    private BroadcastReceiver broadcastReceiver;
    private final Handler handler = new Handler();
    private Runnable fragRunnable;
    private String tabletLat;
    private String tabletLon;
    /*private long tabletTime;*/
    private boolean isConfigDone;
    private long countAIS;
    private static final int checkInterval = 1000;
    private boolean changeFormat;
    private int numOfSignificantFigures;
    private int autoCancelTimer = 0;
    private final static int MAX_TIMER = 300; //5 mins timer



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
        if (savedInstanceState != null){
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
                if (checkForCoordinates()){

                    isConfigDone = true;
                    //show the packet received
                    changeLayout();
                    populateTabLocation();
                } else {
                    //Toast.makeText(getActivity(), "In Coordinate Fragment", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Waiting for AIS Packet");
                    autoCancelTimer++;
                    handler.postDelayed(this, checkInterval);

                    if (autoCancelTimer >= MAX_TIMER){
                        removeMMSIfromDBTable();
                        callMMSIFragment();
                        Toast.makeText(getActivity(), "No relevant packets received", Toast.LENGTH_LONG).show();
                        handler.removeCallbacks(this);
                    }
                }
            }
        };
       return layout;
    }

    private void callMMSIFragment(){
        MMSIFragment mmsiFragment = new MMSIFragment();
        FragmentChangeListener fc = (FragmentChangeListener) getActivity();
        fc.replaceFragment(mmsiFragment);
    }

    @Override
    public void onResume(){
        super.onResume();
        GridSetupActivity activity = (GridSetupActivity)getActivity();
        if(activity != null){
            activity.hideUpButton();
        }
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

    @Override
    public void onStart(){
        super.onStart();
        if (isConfigDone) {
            changeLayout();
            populateTabLocation();
        } else{
            handler.post(fragRunnable);
        }
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent){
                    //Log.d(TAG, "BroadCast Received");
                    /*String coordinateString = intent.getExtras().get("coordinates").toString();
                    String[] coordinates = coordinateString.split(",");*/
                    tabletLat = intent.getExtras().get(GPS_Service.latitude).toString();
                    tabletLon = intent.getExtras().get(GPS_Service.longitude).toString();
                    //tabletTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());

                    //Log.d(TAG, "Tablet Loc: " + tabletLat);
                    //Toast.makeText(getActivity(),"Received Broadcast", Toast.LENGTH_LONG).show();
                    populateTabLocation();
                }
            };
        }
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));
    }


    @Override
    public void onPause(){
        super.onPause();
        if(broadcastReceiver != null){
            getActivity().unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putBoolean("isConfigDone", isConfigDone);
    }

    private boolean checkForCoordinates(){
        int index;
        boolean success = false;
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getActivity());;
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            countAIS = DatabaseUtils.queryNumEntries(db, DatabaseHelper.stationListTable);
            Log.d(TAG, "countAIS:" + String.valueOf(countAIS));
            Cursor cursor = db.query(DatabaseHelper.fixedStationTable,
                    new String[]{DatabaseHelper.stationName, DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.isLocationReceived},
                    DatabaseHelper.mmsi + " = ? AND (" + DatabaseHelper.packetType + " = ? OR " + DatabaseHelper.packetType + " = ? )",
                    new String[] {Integer.toString(MMSINumber), Integer.toString(AISDecodingService.POSITION_REPORT_CLASSA_TYPE_1), Integer.toString(AISDecodingService.POSITION_REPORT_CLASSB)},
                    null, null, null);
            if(cursor.moveToFirst()){
                //stationName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.stationName));
                //index = cursor.getColumnIndexOrThrow(DatabaseHelper.latitude);
                latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.latitude));

                longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.longitude));
                int locationReceived = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.isLocationReceived));
                //Toast.makeText(getActivity(), "LocationReceived: " + String.valueOf(locationReceived), Toast.LENGTH_LONG).show();
                if(locationReceived == DatabaseHelper.IS_LOCATION_RECEIVED) {
                    success = true;
                    //Toast.makeText(getActivity(), "Success True", Toast.LENGTH_LONG).show();
                }
            }
            cursor.close();
            //db.close();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Unavailable");
            Toast.makeText(getActivity(), "Database Unavailable", Toast.LENGTH_LONG).show();
        }
        return success;
    }

    private void changeLayout(){
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
            if(countAIS == 2){
                Button confirmButton = v.findViewById(R.id.confirm_Coordinates);
                confirmButton.setText(R.string.startSetup);
            }
        }else {
            Log.d(TAG, "view is null");
        }

    }

    private void populateTabLocation(){
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
        }else {
            Log.d(TAG, "view is null");
        }
        //tabletLat = (tabletLat == null) ? "0.0" : tabletLat;
        //tabletLon = (tabletLon == null) ? "0.0" : tabletLat;
        if (tabletLat == null || tabletLat.isEmpty()){
            try {
                //locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

                //assert locationManager != null;
                if (getLastKnownLocation() != null) {
                    tabletLat = String.valueOf(getLastKnownLocation().getLatitude());
                } /*else {
                    tabletLat = "0.0";
                }*/


            } catch (SecurityException e){
                Toast.makeText(getActivity(),"Location Service Problem", Toast.LENGTH_LONG).show();
            }
        }
        if (tabletLon == null || tabletLon.isEmpty()){
            try {
                //locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                if(getLastKnownLocation() != null) {
                    tabletLon = String.valueOf(getLastKnownLocation().getLongitude());
                } /*else{
                    tabletLon = "0.0";
                }*/

            } catch (SecurityException e){
                Toast.makeText(getActivity(),"Location Service Problem", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Location Service Not Available");
                e.printStackTrace();
            }
        }
        String formatString = "%."+String.valueOf(numOfSignificantFigures)+"f";
        String[] tabletFormattedCoordinates = NavigationFunctions.locationInDegrees(Double.valueOf(tabletLat), Double.valueOf(tabletLon));
        String[] stationFormattedCoordinates = NavigationFunctions.locationInDegrees(latitude, longitude);
        if (tabLat != null) {
            tabLat.setEnabled(true);
            if(changeFormat){
                tabLat.setText(tabletFormattedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
            } else {
                Double tabletLatitude = Double.valueOf(tabletLat);
                tabLat.setText(String.format(formatString, tabletLatitude));
            }
            tabLat.setEnabled(false);
        }


        if (tabLon != null) {
            tabLon.setEnabled(true);
            if(changeFormat){
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
            if (changeFormat){
                aisLat.setText(stationFormattedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
            } else {
                aisLat.setText(String.format(formatString, latitude));
            }
            aisLat.setEnabled(false);
        }

        if (aisLon != null) {
            aisLon.setEnabled(true);
            if (changeFormat){
                aisLon.setText(stationFormattedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
            } else {
                aisLon.setText(String.format(formatString, longitude));
            }
            aisLon.setEnabled(false);
        }

    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
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



    private void removeMMSIfromDBTable() {
        SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        db.delete(DatabaseHelper.baseStationTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(MMSINumber)});
        db.delete(DatabaseHelper.fixedStationTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(MMSINumber)});
        db.delete(DatabaseHelper.stationListTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(MMSINumber)});
        Log.d(TAG, "Deleted MMSI from db tables");

    }

    private void onClickBtn(){
        if(countAIS < 2) {
            callMMSIFragment();
        } else{
            Intent intent = new Intent(getActivity(), SetupActivity.class);
            intent.putExtra(SetupActivity.calledFromCoordinateFragment, true);
            startActivity(intent);
        }
    }

    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
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
