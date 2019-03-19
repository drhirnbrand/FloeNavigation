package de.awi.floenavigation.admin;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.R;

/**
 * {@link RecoveryActivity} activity is responsible for recovery of static and fixed stations from the system
 * Only a admin has the privilege to do this task
 * This class sets up the layout screen for the recovery activity and initializes various fields of the layout
 * Radio button is presented with yes/no option for selection between fixed and static station
 */
public class RecoveryActivity extends ActionBarActivity {

    private static final String TAG = "RecoveryActivity";
    private boolean aisDeviceCheck = true;
    /**
     * Array to store the base stations
     */
    private int[] baseStnMMSI = new int[DatabaseHelper.INITIALIZATION_SIZE];


    /**
     * onCreate function to set the corresponding layout and initialize all the views corresponding to the layout
     * @param savedInstanceState Used to store previous saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery);

        final RadioButton withAISButton = findViewById(R.id.withAIS);
        final RadioButton withoutAISButton = findViewById(R.id.withoutAIS);
        final TextView devicesOptionSelection = findViewById(R.id.devicesOptionSelection);
        final TextView AISdeviceSelectedView = findViewById(R.id.AISdeviceSelected);
        final TextView StaticStationSelectedView = findViewById(R.id.StaticStationSelected);
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.RadioSelect);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                if (withAISButton.equals(findViewById(checkedId))){
                    AISdeviceSelectedView.setVisibility(View.VISIBLE);
                    StaticStationSelectedView.setVisibility(View.GONE);
                    devicesOptionSelection.setText(R.string.mmsi);
                    aisDeviceCheck = true;
                }else{
                    AISdeviceSelectedView.setVisibility(View.GONE);
                    StaticStationSelectedView.setVisibility(View.VISIBLE);
                    devicesOptionSelection.setText(R.string.staticstn);
                    aisDeviceCheck = false;
                }

            }
        });
    }

    /**
     * Based on the radio button selected, the mmsi for the fixed station or the station name for the static station is validated
     * and checked whether it is present in internal local database.
     * Validation of the field also involves check for empty string
     * @param view The view that has been clicked
     */
    public void onClickRecoveryListenerconfirm(View view) {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            baseStationsRetrievalfromDB(db);
            TextView AISdeviceSelectedView = findViewById(R.id.AISdeviceSelected);
            TextView StaticStationSelectedView = findViewById(R.id.StaticStationSelected);
            if (aisDeviceCheck) {
                if (TextUtils.isEmpty(AISdeviceSelectedView.getText().toString())){
                    Toast.makeText(this, "Please enter a valid mmsi", Toast.LENGTH_SHORT).show();
                    return;
                }
                int mmsiValue = Integer.parseInt(AISdeviceSelectedView.getText().toString());
                deleteEntryfromDBTables(String.valueOf(mmsiValue));
            } else {
                String staticStnName = StaticStationSelectedView.getText().toString();
                if (TextUtils.isEmpty(staticStnName)){
                    Toast.makeText(this, "Please enter a valid station name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (checkEntryInStaticStnTable(db, staticStnName)) {
                    db.delete(DatabaseHelper.staticStationListTable, DatabaseHelper.staticStationName + " = ?", new String[]{staticStnName});
                    insertIntoStaticStationDeletedTable(db, staticStnName);
                    Toast.makeText(getApplicationContext(), "Removed from static station table", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this, "No Entry in DB", Toast.LENGTH_SHORT).show();
                }
            }


        }catch(SQLiteException e) {
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
    }

    /**
     * Function to check the presence of station name entered by the admin in the local internal database {@link DatabaseHelper#staticStationListTable}
     * @param db SQLiteDatabase object
     * @param stationToBeRemoved station to be removed
     * @return <code>true</code> if the station is present
     */
    private boolean checkEntryInStaticStnTable(SQLiteDatabase db, String stationToBeRemoved){
        boolean isPresent = false;
        Cursor staticStnCursor = null;
        try{
            staticStnCursor = db.query(DatabaseHelper.staticStationListTable, new String[]{DatabaseHelper.staticStationName},
                    DatabaseHelper.staticStationName + " = ?", new String[]{stationToBeRemoved}, null, null, null);
            isPresent = staticStnCursor.moveToFirst();
        }catch (SQLiteException e){
            Log.d(TAG, "Station List Cursor error");
            e.printStackTrace();
        } finally {
            if (staticStnCursor != null){
                staticStnCursor.close();
            }
        }
        return isPresent;
    }

    /**
     * @return The number of entries in the {@link DatabaseHelper#staticStationListTable}
     */
    private long getNumOfAISStation() {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            return DatabaseUtils.queryNumEntries(db, DatabaseHelper.stationListTable);

        }catch (SQLiteException e){
            Log.d(TAG, "Error in reading database");
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * The function is responsible for deleting the mmsi of the fixed station from the internal database tables
     * {@value DatabaseHelper#stationListTable}, {@value DatabaseHelper#fixedStationTable}, {@value DatabaseHelper#baseStationTable}
     * Also insert the mmsi into the deleted tables {@value DatabaseHelper#fixedStationDeletedTable}, {@value DatabaseHelper#stationListDeletedTable},
     * {@value DatabaseHelper#baseStationDeletedTable} for synchronization purpose
     * @param mmsiToBeRemoved mmsi to be recovered
     */
    private void deleteEntryfromDBTables(String mmsiToBeRemoved){
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            baseStationsRetrievalfromDB(db);
            int numOfStations = (int) getNumOfAISStation();
            if (checkEntryInStationListTable(db, mmsiToBeRemoved)) {
                if (numOfStations <= DatabaseHelper.NUM_OF_BASE_STATIONS) {
                    Toast.makeText(getApplicationContext(), "Cannot be removed from DB tables, only 2 base stations available", Toast.LENGTH_SHORT).show();
                } else {
                    if (Integer.parseInt(mmsiToBeRemoved) == baseStnMMSI[DatabaseHelper.firstStationIndex]
                            || Integer.parseInt(mmsiToBeRemoved) == baseStnMMSI[DatabaseHelper.secondStationIndex]) {

                        db.delete(DatabaseHelper.stationListTable, DatabaseHelper.mmsi + " = ?", new String[]{mmsiToBeRemoved});
                        insertIntoStationListDeletedTable(db, mmsiToBeRemoved);
                        insertIntoFixedStationDeletedTable(db, mmsiToBeRemoved);
                        insertIntoBaseStationDeletedTable(db, mmsiToBeRemoved);
                        updataMMSIInDBTables(Integer.parseInt(mmsiToBeRemoved), db, (Integer.parseInt(mmsiToBeRemoved) == baseStnMMSI[DatabaseHelper.firstStationIndex]));

                    } else {
                        db.delete(DatabaseHelper.stationListTable, DatabaseHelper.mmsi + " = ?", new String[]{mmsiToBeRemoved});
                        db.delete(DatabaseHelper.fixedStationTable, DatabaseHelper.mmsi + " = ?", new String[]{mmsiToBeRemoved});
                        insertIntoFixedStationDeletedTable(db, mmsiToBeRemoved);
                        insertIntoStationListDeletedTable(db, mmsiToBeRemoved);
                    }
                    Toast.makeText(getApplicationContext(), "Removed from DB tables", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(), "Device Recovered", Toast.LENGTH_SHORT).show();
                }
            }else {
                Toast.makeText(this, "No Entry in DB", Toast.LENGTH_LONG).show();
            }
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        }
    }

    /**
     * Function is used to add the mmsi into the {@value DatabaseHelper#baseStationDeletedTable} table
     * @param db SQLiteDatabase object
     * @param mmsi mmsi recovered
     */
    private void insertIntoBaseStationDeletedTable(SQLiteDatabase db, String mmsi) {
        ContentValues deletedBaseStation = new ContentValues();
        deletedBaseStation.put(DatabaseHelper.mmsi, mmsi);
        deletedBaseStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.baseStationDeletedTable, null, deletedBaseStation);
    }

    /**
     * Function is used to add the mmsi into the {@value DatabaseHelper#fixedStationDeletedTable} table
     * @param db SQLiteDatabase object
     * @param mmsiToBeAdded mmsi recovered
     */
    private void insertIntoFixedStationDeletedTable(SQLiteDatabase db, String mmsiToBeAdded) {
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.mmsi, Integer.valueOf(mmsiToBeAdded));
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.fixedStationDeletedTable, null, deletedStation);
    }

    /**
     * Function is used to add the mmsi into the {@value DatabaseHelper#stationListDeletedTable} table
     * @param db SQLiteDatabase object
     * @param mmsiToBeAdded mmsi recovered
     */
    private void insertIntoStationListDeletedTable(SQLiteDatabase db, String mmsiToBeAdded){
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.mmsi, Integer.valueOf(mmsiToBeAdded));
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.stationListDeletedTable, null, deletedStation);
    }

    /**
     * Function is used to add the recovered static station into the {@value DatabaseHelper#staticStationDeletedTable} table
     * @param db SQLiteDatabase object
     * @param staticStnName mmsi recovered
     */
    private void insertIntoStaticStationDeletedTable(SQLiteDatabase db, String staticStnName) {
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.staticStationName, staticStnName);
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.staticStationDeletedTable, null, deletedStation);
    }

    /**
     * If the recovered fixed stations are part of the original base stations which were used to setup the initial grid
     * then the mmsi's for those stations are assigned {@value DatabaseHelper#BASESTN1} or {@value DatabaseHelper#BASESTN2} values such that the predictions for these
     * stations are in progress and can be redeployed at a different point in the grid even though these are recovered
     * @param mmsi mmsi to be recovered
     * @param db SQLiteDatabase instance
     * @param originFlag <code>true</code> if the mmsi is of the origin base station
     */
    private void updataMMSIInDBTables(int mmsi, SQLiteDatabase db, boolean originFlag){
        ContentValues mContentValues = new ContentValues();
        mContentValues.put(DatabaseHelper.mmsi, ((originFlag) ? DatabaseHelper.BASESTN1 : DatabaseHelper.BASESTN2));
        mContentValues.put(DatabaseHelper.stationName, ((originFlag) ? DatabaseHelper.origin : DatabaseHelper.basestn1));
        db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
        db.update(DatabaseHelper.baseStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
    }

    /**
     * Function to check whether mmsi is present in the internal local database table
     * @param db SQLiteDatabase instance
     * @param mmsi mmsi to be recovered
     * @return <code>true</code> if the station is present in {@value DatabaseHelper#stationListTable}
     */
    private boolean checkEntryInStationListTable(SQLiteDatabase db, String mmsi){
        boolean isPresent = false;
        Cursor stationListCursor = null;
        try{
            stationListCursor = db.query(DatabaseHelper.stationListTable, new String[]{DatabaseHelper.mmsi},
                    DatabaseHelper.mmsi + " = ?", new String[]{mmsi}, null, null, null);
            isPresent = stationListCursor.moveToFirst();
        }catch (SQLiteException e){
            Log.d(TAG, "Station List Cursor error");
            e.printStackTrace();
        } finally {
            if (stationListCursor != null){
                stationListCursor.close();
            }
        }
        return isPresent;
    }

    /**
     * Function used to store the base stations from the {@value DatabaseHelper#baseStationTable} into the {@link #baseStnMMSI}
     * @param db SQLiteDatabase instance
     */
    private void baseStationsRetrievalfromDB(SQLiteDatabase db){
        Cursor mBaseStnCursor = null;
        try {
            //SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            //SQLiteDatabase db = dbHelper.getReadableDatabase();
            mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi},
                    null, null, null, null, null);

            if (mBaseStnCursor.getCount() == DatabaseHelper.NUM_OF_BASE_STATIONS) {
                int index = 0;

                if (mBaseStnCursor.moveToFirst()) {
                    do {
                        baseStnMMSI[index] = mBaseStnCursor.getInt(mBaseStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                        index++;
                    } while (mBaseStnCursor.moveToNext());
                }else {
                    Log.d(TAG, "Base stn cursor error");
                }

            } else {
                Log.d(TAG, "Error reading from base stn table");
            }
        }catch (SQLException e){

            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        } finally {
            if (mBaseStnCursor != null){
                mBaseStnCursor.close();
            }
        }

    }

    /**
     * Function starts the {@link ListViewActivity} to display the list of Fixed stations or the Static Stations stored inside the internal database table
     * @param view View that has been clicked
     */
    public void onClickViewDeployedStations(View view) {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            long numOfStaticStations = DatabaseUtils.queryNumEntries(db, DatabaseHelper.staticStationListTable);
            Intent listViewIntent = new Intent(this, ListViewActivity.class);
            if (aisDeviceCheck)
                listViewIntent.putExtra("GenerateDataOption", "AISRecoverActivity");
            else {
                if (numOfStaticStations > 0) {
                    listViewIntent.putExtra("GenerateDataOption", "StaticStationRecoverActivity");
                }else {
                    Toast.makeText(this, "No static station are deployed in the grid", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            startActivity(listViewIntent);

        }catch (SQLiteException e){
            Log.d(TAG, "Error in reading database");
            e.printStackTrace();
        }

    }

}
