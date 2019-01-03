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
import android.support.v4.app.Fragment;
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

import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.admin.AdminPageActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.FragmentChangeListener;
import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.R;
import de.awi.floenavigation.aismessages.AISDecodingService;

/**
 * A simple {@link Fragment} subclass.
 */
public class AISStationCoordinateFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "AISStationDeployFrag";

    private static final String changeText = "AIS Packet Received from the new Station";
    private int MMSINumber;
    private final Handler handler = new Handler();
    private int originMMSI;
    private double beta;
    private double originLatitude;
    private double originLongitude;
    private double stationLatitude;
    private double stationLongitude;
    private double distance;
    private double stationX;
    private double stationY;
    private double theta;
    private double alpha;
    private static final int checkInterval = 1000;
    private int autoCancelTimer = 0;
    private final static int MAX_TIMER = 300; //5 mins timer
    private boolean isSetupComplete = false;
    private Runnable aisStationRunnable;
    private MenuItem gpsIconItem, aisIconItem, gridSetupIconItem;
    private boolean locationStatus = false;
    private boolean packetStatus = false;
    private final Handler statusHandler = new Handler();
    private BroadcastReceiver aisPacketBroadcastReceiver;
    private BroadcastReceiver broadcastReceiver;


    public AISStationCoordinateFragment() {
        // Required empty public constructor
    }


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
                    SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getActivity());;
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    if (checkForAISPacket(db)) {
                        Log.d(TAG, "AIS Packet Received");
                        if (readParamsFromDatabase(db)) {
                            distance = NavigationFunctions.calculateDifference(originLatitude, originLongitude, stationLatitude, stationLongitude);
                            theta = NavigationFunctions.calculateAngleBeta(originLatitude, originLongitude, stationLatitude, stationLongitude);
                            alpha = Math.abs(theta - beta);
                            stationX = distance * Math.cos(Math.toRadians(alpha));
                            stationY = distance * Math.sin(Math.toRadians(alpha));
                            ContentValues stationUpdate = new ContentValues();
                            stationUpdate.put(DatabaseHelper.alpha, alpha);
                            stationUpdate.put(DatabaseHelper.distance, distance);
                            stationUpdate.put(DatabaseHelper.xPosition, stationX);
                            stationUpdate.put(DatabaseHelper.yPosition, stationY);
                            db.update(DatabaseHelper.fixedStationTable, stationUpdate,
                                    DatabaseHelper.mmsi + " = ?", new String[] {String.valueOf(MMSINumber)});
                            packetReceived();
                        } else{
                            Log.d(TAG, "Error Reading from Database");
                            //Do something here
                            Toast.makeText(getActivity(), "Error in Database. Please Try again", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.d(TAG, "Waiting for AIS Packet");
                        autoCancelTimer++;
                        handler.postDelayed(this, checkInterval);
                        if (autoCancelTimer >= MAX_TIMER){
                            removeMMSIfromDBTable();
                            Toast.makeText(getActivity(), "No relevant packets received", Toast.LENGTH_LONG).show();
                            handler.removeCallbacks(this);
                            StationInstallFragment stationInstallFragment = new StationInstallFragment();
                            FragmentChangeListener fc = (FragmentChangeListener) getActivity();
                            fc.replaceFragment(stationInstallFragment);
                        }

                    }
                } catch (SQLiteException e){
                    e.printStackTrace();
                    Log.d(TAG, "Database Error");
                }
            }
        };
        handler.post(aisStationRunnable);
        setHasOptionsMenu(true);
        return layout;
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
    public void onPause(){
        super.onPause();
        getActivity().unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        getActivity().unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
    }


    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.station_finish:
                onClickFinish();
        }
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

    private void removeMMSIfromDBTable() {
        SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        db.delete(DatabaseHelper.fixedStationTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(MMSINumber)});
        db.delete(DatabaseHelper.stationListTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(MMSINumber)});
        Log.d(TAG, "Deleted MMSI from db tables");

    }

    /**
     *
     */

    private void packetReceived(){
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
     * Method for checking whether a Position Report has been received from an AIS Station as it is being deployed.
     * @return True if packet is received.
     */

    private boolean checkForAISPacket(SQLiteDatabase db){
        boolean success = false;
        int locationReceived;
        try{

            Cursor cursor = db.query(DatabaseHelper.fixedStationTable,
                    new String[]{DatabaseHelper.mmsi, DatabaseHelper.latitude, DatabaseHelper.longitude, DatabaseHelper.isLocationReceived},
                    DatabaseHelper.mmsi + " = ? AND (" + DatabaseHelper.packetType + " = ? OR " + DatabaseHelper.packetType + " = ? )",
                    new String[] {Integer.toString(MMSINumber), Integer.toString(AISDecodingService.POSITION_REPORT_CLASSA_TYPE_1), Integer.toString(AISDecodingService.POSITION_REPORT_CLASSB)},
                    null, null, null);
            if(cursor.moveToFirst()){
                locationReceived = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.isLocationReceived));
                if(locationReceived == DatabaseHelper.IS_LOCATION_RECEIVED) {
                    stationLatitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.latitude));
                    stationLongitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.longitude));
                    success = true;
                    //Toast.makeText(getActivity(), "Success True", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Packet Recieved from AIS Station");
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

    private void onClickFinish(){
        if(isSetupComplete) {
            Toast.makeText(getContext(), "Deployment Complete", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(getActivity(), AdminPageActivity.class);
            getActivity().startActivity(intent);
        } else{
            removeMMSIfromDBTable();
            Log.d(TAG, "AIS Station Installation Cancelled");
            handler.removeCallbacks(aisStationRunnable);
            StationInstallFragment stationInstallFragment = new StationInstallFragment();
            Bundle bundle = new Bundle();
            bundle.putBoolean("stationTypeAIS", true);
            stationInstallFragment.setArguments(bundle);
            FragmentChangeListener fc = (FragmentChangeListener)getActivity();
            if (fc != null) {
                fc.replaceFragment(stationInstallFragment);
            }
        }
    }

    private boolean readParamsFromDatabase(SQLiteDatabase db){
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
                Log.d(TAG, "Error Reading Origin Latitude Longtidue");
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
                    //Log.d(TAG, String.valueOf(beta));
                } else{
                    Log.d(TAG, "Beta Table Move to first failed");
                }

            } else {
                Log.d(TAG,String.valueOf(betaCursor.getCount()));

                Log.d(TAG, "Error in Beta Table");
                return false;
            }
            betaCursor.close();
            baseStationCursor.close();
            fixedStationCursor.close();
            return true;
        } catch (SQLiteException e){
            e.printStackTrace();
            Log.d(TAG, "Error reading Database");
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
