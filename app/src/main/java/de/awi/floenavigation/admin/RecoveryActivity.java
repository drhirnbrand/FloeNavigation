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

public class RecoveryActivity extends ActionBarActivity {

    private static final String TAG = "RecoveryActivity";
    private boolean aisDeviceCheck = true;
    private int[] baseStnMMSI = new int[DatabaseHelper.INITIALIZATION_SIZE];


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

    private boolean checkEntryInStaticStnTable(SQLiteDatabase db, String stationToBeRemoved){
        boolean isPresent = false;
        try{
            Cursor staticStnCursor = db.query(DatabaseHelper.staticStationListTable, new String[]{DatabaseHelper.staticStationName},
                    DatabaseHelper.staticStationName + " = ?", new String[]{stationToBeRemoved}, null, null, null);
            isPresent = staticStnCursor.moveToFirst();
            staticStnCursor.close();
        }catch (SQLiteException e){
            Log.d(TAG, "Station List Cursor error");
            e.printStackTrace();
        }
        return isPresent;
    }

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

    private void insertIntoBaseStationDeletedTable(SQLiteDatabase db, String mmsi) {
        ContentValues deletedBaseStation = new ContentValues();
        deletedBaseStation.put(DatabaseHelper.mmsi, mmsi);
        deletedBaseStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.baseStationDeletedTable, null, deletedBaseStation);
    }

    private void insertIntoFixedStationDeletedTable(SQLiteDatabase db, String mmsiToBeAdded) {
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.mmsi, Integer.valueOf(mmsiToBeAdded));
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.fixedStationDeletedTable, null, deletedStation);
    }

    private void insertIntoStationListDeletedTable(SQLiteDatabase db, String mmsiToBeAdded){
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.mmsi, Integer.valueOf(mmsiToBeAdded));
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.stationListDeletedTable, null, deletedStation);
    }

    private void insertIntoStaticStationDeletedTable(SQLiteDatabase db, String staticStnName) {
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.staticStationName, staticStnName);
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.staticStationDeletedTable, null, deletedStation);
    }

    private void updataMMSIInDBTables(int mmsi, SQLiteDatabase db, boolean originFlag){
        ContentValues mContentValues = new ContentValues();
        mContentValues.put(DatabaseHelper.mmsi, ((originFlag) ? DatabaseHelper.BASESTN1 : DatabaseHelper.BASESTN2));
        mContentValues.put(DatabaseHelper.stationName, ((originFlag) ? DatabaseHelper.origin : DatabaseHelper.basestn1));
        db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
        db.update(DatabaseHelper.baseStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
    }

    private boolean checkEntryInStationListTable(SQLiteDatabase db, String mmsi){
        boolean isPresent = false;
        try{
            Cursor stationListCursor = db.query(DatabaseHelper.stationListTable, new String[]{DatabaseHelper.mmsi},
                    DatabaseHelper.mmsi + " = ?", new String[]{mmsi}, null, null, null);
            isPresent = stationListCursor.moveToFirst();
            stationListCursor.close();
        }catch (SQLiteException e){
            Log.d(TAG, "Station List Cursor error");
            e.printStackTrace();
        }
        return isPresent;
    }

    private void baseStationsRetrievalfromDB(SQLiteDatabase db){

        try {
            //SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            //SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi},
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
            mBaseStnCursor.close();
        }catch (SQLException e){

            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        }

    }

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
