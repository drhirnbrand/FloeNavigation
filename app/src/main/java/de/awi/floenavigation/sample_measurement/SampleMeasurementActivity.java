package de.awi.floenavigation.sample_measurement;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import de.awi.floenavigation.admin.ListViewActivity;
import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.helperclasses.NavigationFunctions;
import de.awi.floenavigation.R;

/**
 * {@link SampleMeasurementActivity} can be used to take sample or measurement on the sea ice.
 * The devices are available on the view in the form of a drop down list with advanced search feature.
 * This class handles sample/measurement selection, device selection, subsequent data is automatically populated,
 * ais and gps status icon updates, view samples feature.
 */
public class SampleMeasurementActivity extends Activity {

    /**
     * For logging purpose
     */
    private static final String TAG = "SampleMeasureActivity";
    /**
     * {@link BroadcastReceiver} used to receive gps location
     */
    private BroadcastReceiver broadcastReceiver;
    /**
     * Stores tablet latitude value
     */
    private Double tabletLat;
    /**
     * Stores tablet longitude value
     */
    private Double tabletLon;
    /**
     * Array list to store device name, device id and device type
     */
    private ArrayList<String> selectedDeviceAttributes;
    /**
     * Selected device name
     */
    private String deviceSelectedName;
    /**
     * Index of the {@link #selectedDeviceAttributes} for device ID
     */
    private final int deviceIDIndex = 0;
    /**
     * Index of the {@link #selectedDeviceAttributes} for device full name
     */
    private final int deviceFullNameIndex = 1;
    /**
     * Index of the {@link #selectedDeviceAttributes} for device type
     */
    private final int deviceTypeIndex = 2;
    /**
     * Stores origin latitude
     */
    private double originLatitude;
    /**
     * Stores origin longitude
     */
    private double originLongitude;
    /**
     * Stores origin mmsi
     */
    private int originMMSI;
    /**
     * Variable used to store the value of {@value DatabaseHelper#beta}
     * It is the angle between the x-axis and the geographic longitudinal axis
     */
    private double beta;
    /**
     * distance calculated between the origin fixed station and the sample/measurement in meters
     */
    private double distance;
    /**
     * Angle calculated between the x-axis and the sample/measurement
     */
    private double alpha;
    /**
     * X axis value in meters of the sample/measurement
     */
    private double xPosition;
    /**
     * Y axis value in meters of the sample/measurement
     */
    private double yPosition;
    /**
     * Angle calculated between the axis connecting origin fixed station along the longitudinal axis
     * and the sample/meaurement
     */
    private double theta;
    /**
     * Spinner for listing sample or measurement
     */
    //private Spinner operation;
    /**
     * to change the display format of the geographic coordinates
     */
    private boolean changeFormat;
    /**
     * Number of figures to display after the decimal point
     */
    private int numOfSignificantFigures;
    /**
     * stores the label of the sample/measurement
     */
    private String label;
    /**
     * time
     */
    private String time;
    /**
     * gps time
     */
    private long gpsTime;
    /**
     * time difference between the gps time and the system clock
     */
    private long timeDiff;
    /**
     * label id
     */
    private String labelId;
    /**
     * comment for the sample/measurement taken
     */
    private String comment;

    //Action Bar Updates
    /**
     * {@link BroadcastReceiver} to receive ais status updates
     */
    private BroadcastReceiver aisPacketBroadcastReceiver;
    /**
     * <code>true</code> when the tablet has a GPS Connection
     */
    private boolean locationStatus = false;
    /**
     * <code>true</code> when the tablet is connected to the WiFi network of an AIS Transponder
     */
    private boolean packetStatus = false;
    /**
     * Handler to run the runnable
     */
    private final Handler statusHandler = new Handler();
    /**
     * Menu items
     */
    private MenuItem gpsIconItem, aisIconItem, gridSetupIconItem;

    /**
     * Time at which the position was last predicted
     */
    private double predictionTime;

    /**
     * Time at which the AIS packet was last received
     */
    private double updateTime;

    /**
     * Stores the tablet id
     */
    private String tabletID;
    /**
     * Edit text view of the label id
     */
    private EditText labelId_TV;

    /**
     * interval
     */
    private static final int waitInterval = 1000;
    /**
     * A {@link Handler} which is runs the {@link Runnable} {@link #waitRunnable} which periodically checks for the Position Report.
     */
    private final Handler handler = new Handler();
    /**
     * {@link Runnable} which checks periodically (as specified by {@link #waitInterval})
     */
    private Runnable waitRunnable;
    private static final int WAIT_COUNTER = 3;
    private int autoCancelTimer = 0;

    /**
     * Intializes all the views
     * and registers listeners for the corresponding views
     * @param savedInstanceState stores previous instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_measurement);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        changeFormat = DatabaseHelper.readCoordinateDisplaySetting(this);
        numOfSignificantFigures = DatabaseHelper.readSiginificantDigitsSetting(this);

        //Set text for label ID
        if (getTabletID()) {
            labelId_TV = findViewById(R.id.sampleMeasurementLabelId);
            labelId_TV.setText(String.format("%s_%s", tabletID, Integer.toString(DatabaseHelper.SAMPLE_ID_COUNTER)));
        }

        //Advanced Search Feature
        DatabaseHelper.loadDeviceList(getApplicationContext());
        //setSpinnerValues();
        final AutoCompleteTextView deviceNameTextView = findViewById(R.id.deviceshortname);
        ArrayAdapter<String> adapter = DatabaseHelper.advancedSearchTextView(getApplicationContext());
        deviceNameTextView.setDropDownBackgroundResource(R.color.backgroundGradStart);
        deviceNameTextView.setThreshold(0);
        deviceNameTextView.setAdapter(adapter);
        deviceNameTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    deviceNameTextView.showDropDown();
                }
            }
        });
        deviceNameTextView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(final View view){
                deviceNameTextView.showDropDown();
            }


        });

        //on Click listener for device name
        deviceNameTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                deviceSelectedName = (String)parent.getItemAtPosition(position);
                selectedDeviceAttributes = DatabaseHelper.getDeviceAttributes((String)parent.getItemAtPosition(position));
                populateDeviceAttributes();
            }
        });

        //on Click listener for confirmbutton
        Button confirmButton = findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (populateDatabaseTable()) {
                    findViewById(R.id.sampleCoordinateView).setVisibility(View.GONE);
                    findViewById(R.id.sampleWaitingView).setVisibility(View.VISIBLE);
                    //Toast.makeText(getApplicationContext(), "Data Sample Confirmed", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Data Sample Confirmed");
                    waitRunnable = new Runnable() {
                        @Override
                        public void run() {
                            handler.postDelayed(this, waitInterval);
                            autoCancelTimer++;
                            if (autoCancelTimer >= WAIT_COUNTER){
                                handler.removeCallbacks(this);
                                onClickFinish();
                            }
                        }
                    };
                    handler.post(waitRunnable);


                }
            }
        });
    }

    /**
     * Starts the {@link MainActivity} when the user presses the finish button
     */
    private void onClickFinish(){
        Log.d(TAG, "Activity Finished");
        Intent sampleActivityIntent = new Intent(getApplicationContext(), SampleMeasurementActivity.class);
        startActivity(sampleActivityIntent);
    }

    /**
     * Function to read tablet id from the database table
     */
    private boolean getTabletID() {
        boolean success = false;
        try{
            DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor paramCursor = db.query(DatabaseHelper.configParametersTable,
                    new String[] {DatabaseHelper.parameterName, DatabaseHelper.parameterValue},
                    DatabaseHelper.parameterName +" = ?",
                    new String[] {DatabaseHelper.tabletId},
                    null, null, null);
            if (paramCursor.moveToFirst()){
                String paramValue = paramCursor.getString(paramCursor.getColumnIndexOrThrow(DatabaseHelper.parameterValue));
                if(!paramValue.isEmpty()){
                    success = true;
                    tabletID = paramValue;
                } else{
                    Log.d(TAG, "Blank TabletID");
                }
            } else{
                Log.d(TAG, "TabletID not set");
            }
            paramCursor.close();

        } catch(SQLiteException e){
            Log.d(TAG, "Error Reading from Database");
        }
        return success;
    }

    /**
     * Registers {@link BroadcastReceiver}s for gps location and ais packet status updates.
     * Handler {@link #statusHandler} to run the runnable, which takes care of changing the color of the icons based
     * on the received values from the broadcast receivers
     */
    private void actionBarUpdatesFunction() {

        /*****************ACTION BAR UPDATES*************************/
        if (broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    tabletLat = intent.getExtras().getDouble(GPS_Service.latitude);
                    tabletLon = intent.getExtras().getDouble(GPS_Service.longitude);
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
                    gpsTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());
                    timeDiff = System.currentTimeMillis() - gpsTime;
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

        registerReceiver(aisPacketBroadcastReceiver, new IntentFilter(GPS_Service.AISPacketBroadcast));
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

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
        /******************************************/
    }

    /**
     * {@link #operation} spinner value shows a drop down list to select between sample
     * and measurement
     */
    /**
    private void setSpinnerValues(){
        operation = findViewById(R.id.operationspinner);
        String[] contents = new String[]{"Sample", "Measurement"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, contents);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        operation.setAdapter(adapter);
    }**/

    /**
     * onStart method to call {@link #actionBarUpdatesFunction()}
     */
    @Override
    protected void onStart() {
        super.onStart();
        //Broadcast receiver for tablet location
        actionBarUpdatesFunction();

    }

    /**
     * onStop method unregisters the {@link BroadcastReceiver}s
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
     * This method initializes and sets the menu list
     * @param menu menu
     * @return parent method
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
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
        if(MainActivity.numOfBaseStations >= DatabaseHelper.INITIALIZATION_SIZE) {
            if (gridSetupIconItem != null) {
                gridSetupIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorGreen), PorterDuff.Mode.SRC_IN);
            }
        } else{
            if (gridSetupIconItem != null) {
                gridSetupIconItem.getIcon().setColorFilter(Color.parseColor(ActionBarActivity.colorRed), PorterDuff.Mode.SRC_IN);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Listener for the menu item clicked
     * Changes the display format of the geographical coordinates in the screen
     * @param menuItem menu
     * @return true
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()){
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
     * Invokes {@link ListViewActivity} when the button is pressed, which lists all the samples currently stored in the
     * database table
     * @param view view which was clicked
     */
    public void OnClickViewSamples(View view) {
        try{
            DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            long numOfSamples = DatabaseUtils.queryNumEntries(db, DatabaseHelper.sampleMeasurementTable);

            if(numOfSamples > 0){
                Intent listViewIntent = new Intent(this, ListViewActivity.class);
                listViewIntent.putExtra("GenerateDataOption", "SampleMeasurementActivity");
                startActivity(listViewIntent);
            } else{
                Toast.makeText(this, "No samples have been recorded since the last Sync", Toast.LENGTH_SHORT).show();
            }
        } catch (SQLiteException e){
            Log.d(TAG, "Error Reading from Database");
            e.printStackTrace();
        }
    }

    /**
     * Populates the views based on the flag {@link #changeFormat} set
     */
    private void populateTabLocation(){

        TextView latView = findViewById(R.id.tabLat);
        TextView lonView = findViewById(R.id.tabLon);
        String formatString = "%."+String.valueOf(numOfSignificantFigures)+"f";

        if (changeFormat){
            String[] formattedCoordinates = NavigationFunctions.locationInDegrees(tabletLat, tabletLon);
            latView.setText(formattedCoordinates[DatabaseHelper.LATITUDE_INDEX]);
            lonView.setText(formattedCoordinates[DatabaseHelper.LONGITUDE_INDEX]);
        } else{
            latView.setText(String.format(formatString, tabletLat));
            lonView.setText(String.format(formatString, tabletLon));
        }

    }

    /**
     * Populates the views with all the device information based on the device selection
     */
    private void populateDeviceAttributes(){

        TextView deviceFullNameView = findViewById(R.id.devicefullname);
        TextView deviceIDView = findViewById(R.id.deviceid);
        TextView deviceTypeView = findViewById(R.id.devicetype);

        deviceIDView.setText(selectedDeviceAttributes.get(deviceIDIndex));
        deviceFullNameView.setText(selectedDeviceAttributes.get(deviceFullNameIndex));
        deviceTypeView.setText(selectedDeviceAttributes.get(deviceTypeIndex));
        InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * Called from the listener of the confirm button
     * If valid gps location is available all the sample/measurement fields are stored into the database table {@link DatabaseHelper#sampleMeasurementTable}
     * @return
     */
    private boolean populateDatabaseTable(){

        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            tabletLat = (tabletLat == null) ? 0.0 : tabletLat;
            tabletLon = (tabletLon == null) ? 0.0 : tabletLon;
            if (tabletLat == 0.0 && tabletLon == 0.0){
                Toast.makeText(getApplicationContext(), "Error reading Device Lat and Long", Toast.LENGTH_LONG).show();
                return false;
            }
            EditText labelView = findViewById(R.id.sampleMeasurementLabelId);
            if (TextUtils.isEmpty(labelView.getText().toString())){
                Toast.makeText(getApplicationContext(), "Error reading Label ID", Toast.LENGTH_LONG).show();
                return false;
            }
            /**
            EditText commentView = findViewById(R.id.sampleMeasurementComment);
            if(TextUtils.isEmpty(commentView.getText().toString())){
                Toast.makeText(getApplicationContext(), "Error Reading Comment", Toast.LENGTH_SHORT).show();
                return false;
            }**/

            if (getOriginCoordinates()) {
                calculateSampledLocationParameters();
                createLabel();
                ContentValues mContentValues = new ContentValues();
                mContentValues.put(DatabaseHelper.deviceID, selectedDeviceAttributes.get(deviceIDIndex));
                mContentValues.put(DatabaseHelper.deviceName, selectedDeviceAttributes.get(deviceFullNameIndex));
                mContentValues.put(DatabaseHelper.deviceShortName, deviceSelectedName);
                //mContentValues.put(DatabaseHelper.operation, operation.getSelectedItem().toString());
                mContentValues.put(DatabaseHelper.deviceType, selectedDeviceAttributes.get(deviceTypeIndex));
                mContentValues.put(DatabaseHelper.latitude, tabletLat);
                mContentValues.put(DatabaseHelper.longitude, tabletLon);
                mContentValues.put(DatabaseHelper.xPosition, xPosition);
                mContentValues.put(DatabaseHelper.yPosition, yPosition);
                mContentValues.put(DatabaseHelper.labelID, labelId);
                mContentValues.put(DatabaseHelper.comment, comment);
                mContentValues.put(DatabaseHelper.label, label);
                mContentValues.put(DatabaseHelper.updateTime, time);
                db.insert(DatabaseHelper.sampleMeasurementTable, null, mContentValues);
                return true;
            } else {
                Log.d(TAG, "Error Inserting new data");
            }
        }catch(SQLiteException e) {
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * {@link #label} is formed as per a specified format with all the necessary information
     */
    private void createLabel(){
        Date date = new Date(System.currentTimeMillis() - timeDiff);
        SimpleDateFormat displayFormat = new SimpleDateFormat("yyyyMMdd'D'HHmmss");
        displayFormat.setTimeZone(TimeZone.getTimeZone("gmt"));
        time = displayFormat.format(date);
        labelId = labelId_TV.getText().toString();
        if (labelId.contains(tabletID))
            DatabaseHelper.SAMPLE_ID_COUNTER += 1;
        EditText comment_TV = findViewById(R.id.sampleMeasurementComment);
        comment = comment_TV.getText().toString();
        List<String> labelElements = new ArrayList<String>();
        labelElements.add(time);
        labelElements.add(String.valueOf(tabletLat));
        labelElements.add(String.valueOf(tabletLon));
        labelElements.add(String.valueOf(xPosition));
        labelElements.add(String.valueOf(yPosition));
        labelElements.add(labelId);
        //labelElements.add(operation.getSelectedItem().toString());
        labelElements.add(comment);
        labelElements.add(selectedDeviceAttributes.get(deviceIDIndex));
        label = TextUtils.join(",", labelElements);
        Log.d(TAG, "Label: " + label);

    }

    /**
     * Calculates sample/measurement location on the grid
     * @see #distance
     * @see #theta
     * @see #alpha
     * @see #beta
     * @see #xPosition
     * @see #yPosition
     */
    private void calculateSampledLocationParameters(){
        distance = NavigationFunctions.calculateDifference(tabletLat, tabletLon, originLatitude, originLongitude);
        //theta = NavigationFunctions.calculateAngleBeta(tabletLat, tabletLon, originLatitude, originLongitude);
        theta = NavigationFunctions.calculateAngleBeta(originLatitude, originLongitude, tabletLat, tabletLon);
        //alpha = Math.abs(theta - beta);
        alpha = theta - beta;
        xPosition = distance * Math.cos(Math.toRadians(alpha));
        yPosition = distance * Math.sin(Math.toRadians(alpha));
    }

    /**
     * Retrieves origin mmsi/location and beta value from {@link DatabaseHelper#fixedStationTable} and {@link DatabaseHelper#betaTable}
     * respectively.
     * It is used to calculate location parameters {@link #calculateSampledLocationParameters()}
     * @return
     */
    private boolean getOriginCoordinates(){
        Cursor baseStationCursor = null;
        Cursor fixedStationCursor = null;
        Cursor betaCursor = null;
        try {
            DatabaseHelper databaseHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            baseStationCursor = db.query(DatabaseHelper.baseStationTable,
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
            fixedStationCursor = db.query(DatabaseHelper.fixedStationTable,
                    new String[] {DatabaseHelper.latitude, DatabaseHelper.longitude,
                            DatabaseHelper.recvdLatitude, DatabaseHelper.recvdLongitude, DatabaseHelper.predictionTime, DatabaseHelper.updateTime},
                    DatabaseHelper.mmsi +" = ? AND " + DatabaseHelper.isLocationReceived + " = ?",
                    new String[] {String.valueOf(originMMSI), String.valueOf(DatabaseHelper.LOCATIONRECEIVED)},
                    null, null, null);
            if (fixedStationCursor.getCount() != 1){
                Log.d(TAG, "Error Reading Origin Latitude Longitude");
                return false;
            } else{
                if(fixedStationCursor.moveToFirst()){
                    updateTime = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.updateTime));
                    predictionTime = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.predictionTime));
                    if (updateTime >= predictionTime) {
                        originLatitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.recvdLatitude));
                        originLongitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.recvdLongitude));
                    }else {
                        originLatitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.latitude));
                        originLongitude = fixedStationCursor.getDouble(fixedStationCursor.getColumnIndex(DatabaseHelper.longitude));
                    }
                }
            }

            betaCursor = db.query(DatabaseHelper.betaTable,
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
        }finally {
            if (baseStationCursor != null){
                baseStationCursor.close();
            }
            if (fixedStationCursor != null){
                fixedStationCursor.close();
            }
            if (betaCursor != null){
                betaCursor.close();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);

    }
}
