package de.awi.floenavigation.initialsetup;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.FragmentChangeListener;
import de.awi.floenavigation.R;

/**
 * This {@link Fragment} runs on top of the {@link GridSetupActivity} and shows the layout for inserting the first two Fixed/Base Station.
 * This fragment is run twice during the setup process once each for the Origin and x-Axis Marker.
 *
 * @see GridSetupActivity
 * @see de.awi.floenavigation.initialsetup
 */
public class MMSIFragment extends Fragment implements View.OnClickListener{

    /**
     * Valid Length of an MMSI Number. Currently set to 9
     */
    private static final int VALID_MMSI_LENGTH = 9;

    /**
     * {@link SQLiteDatabase} object used for accessing the local Database.
     */
    private SQLiteDatabase db;

    /**
     * Number stations inserted in Database. This helps in deciding whether the current station is Origin or x-Axis Marker.
     */
    private long stationCount;
    private static final String TAG = "MMSI Fragment";

    /**
     * {@link SQLiteOpenHelper} object used for accessing the local database.
     */
    private SQLiteOpenHelper dbHelper;

    /**
     * {@link EditText} of the Station Name field on the screen.
     */
    private EditText aisStationName;

    /**
     * {@link EditText} of the Station MMSI field on the screen.
     */
    private EditText mmsi;

    /**
     * Default empty constructor
     */
    public MMSIFragment() {
        // Required empty public constructor
    }


    /**
     * Default {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}. Checks the number of stations installed by reading
     * the length of the Database table {@link DatabaseHelper#stationListTable}.
     * @param inflater
     * @param container
     * @param savedInstanceState
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout =  inflater.inflate(R.layout.fragment_mmsi, container, false);

        Button confirmButton = layout.findViewById(R.id.confirm_Button);
        confirmButton.setOnClickListener(this);
        dbHelper = DatabaseHelper.getDbInstance(getActivity());
        db = dbHelper.getReadableDatabase();
        stationCount = DatabaseUtils.queryNumEntries(db, DatabaseHelper.stationListTable);
        Log.d(TAG, "StationCount: " + String.valueOf(stationCount));

        return layout;
    }

    /**
     * Called when the Fragment come back from background to foreground. Enables the Up Button.
     */
    @Override
    public void onResume(){
        super.onResume();
        GridSetupActivity activity = (GridSetupActivity)getActivity();
        if(activity != null){
            activity.showUpButton();
        }
    }

    /**
     * Default Handler for the Confirm button on the Screen. It calls the method {@link #onClickConfirm()}
     * @param v
     */
    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.confirm_Button:
                onClickConfirm();
                break;
        }
    }

    /**
     * Checks the validity of the Station Name and MMSI entered and calls {@link #insertStation(String, int)}  to insert the values in
     * the Database. If the station is inserted successfully it changes the Fragment to {@link CoordinateFragment} and passes it the
     * {@link #mmsi} and {@link #aisStationName} in the {@link Bundle}.
     */
    public void onClickConfirm(){

        //mmsi.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        View view = getView();
        aisStationName = view.findViewById(R.id.ais_station_name);
        mmsi = view.findViewById(R.id.mmsi_field);

        String stationName = aisStationName.getText().toString();
        if (TextUtils.isEmpty(aisStationName.getText().toString())){
            Toast.makeText(getActivity(), "Invalid Station Name", Toast.LENGTH_LONG).show();
            return;
        }
        if (validateMMSINumber(mmsi)) {

            int mmsiNumber = Integer.parseInt(mmsi.getText().toString());
            if (insertStation(stationName, mmsiNumber)) {
                //if(stationCount == 0) {
            /*AISMessageReceiver aisMessage = new AISMessageReceiver(GridSetupActivity.dstAddress, GridSetupActivity.dstPort, getActivity().getApplicationContext());
            Thread aisMessageReceiver = new Thread(aisMessage);
            aisMessageReceiver.start();*/
                //}
                CoordinateFragment coordinateFragment = new CoordinateFragment();
                Bundle argument = new Bundle();
                argument.putInt(DatabaseHelper.mmsi, mmsiNumber);
                argument.putString(DatabaseHelper.stationName, stationName);
                coordinateFragment.setArguments(argument);
                FragmentChangeListener fc = (FragmentChangeListener) getActivity();
                if (fc != null) {
                    fc.replaceFragment(coordinateFragment);
                }
            }

        }else {
            Toast.makeText(getActivity(), "MMSI Number does not match the requirements", Toast.LENGTH_LONG).show();
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
     * Inserts a Fixed Station MMSI and other initial values in the Database tables {@link DatabaseHelper#fixedStationTable}, {@link DatabaseHelper#stationListTable} and {@link DatabaseHelper#baseStationTable}.
     * Before inserting in the Database tables it checks if the Station is already installed as a BaseStation and shows a {@link Toast} message to that effect.
     * It then inserts the name, MMSI and other parameters in their respective columns in the database tables.
     * The first station being installed is also marked as the Origin in the Database table {@link DatabaseHelper#baseStationTable}.
     * Depending on whether it's the first station being installed or second different initialization values of x and y position
     * are inserted in the Database tables.
     * If the MMSI being added is a mobile station it removes from the Database table {@link DatabaseHelper#mobileStationTable} and then inserts the name and MMSI of the Fixed Station to the Database
     * tables {@link DatabaseHelper#fixedStationTable}, {@link DatabaseHelper#stationListTable} and {@link DatabaseHelper#baseStationTable}.
     * @see DatabaseHelper
     * @see ContentValues
     * @return <code>true</code> if the station is inserted successfully.
     */
    private boolean insertStation(String AISStationName, int MMSI){
        //DatabaseHelper databaseHelper = new DatabaseHelper(getActivity());
        try{
            //db = databaseHelper.getWritableDatabase();
            dbHelper = DatabaseHelper.getDbInstance(getActivity());
            db = dbHelper.getReadableDatabase();
            if (stationCount == 1 && baseStationRetrievalfromDB(db, MMSI) == MMSI){
                Toast.makeText(getActivity(), "Duplicate MMSI, AIS Station is already existing", Toast.LENGTH_LONG).show();
                return false;
            }

            ContentValues station = new ContentValues();
            station.put(DatabaseHelper.mmsi, MMSI);
            station.put(DatabaseHelper.stationName, AISStationName);
            ContentValues baseStationContent = new ContentValues();
            baseStationContent.put(DatabaseHelper.mmsi, MMSI);
            baseStationContent.put(DatabaseHelper.stationName, AISStationName);
            ContentValues stationData = new ContentValues();
            stationData.put(DatabaseHelper.mmsi, MMSI);
            stationData.put(DatabaseHelper.stationName, AISStationName);
            stationData.put(DatabaseHelper.isLocationReceived, DatabaseHelper.IS_LOCATION_RECEIVED_INITIAL_VALUE);
            if(stationCount == 0){
                stationData.put(DatabaseHelper.xPosition, DatabaseHelper.station1InitialX);
                stationData.put(DatabaseHelper.yPosition, DatabaseHelper.station1InitialY);
                stationData.put(DatabaseHelper.alpha, DatabaseHelper.station1Alpha);
                stationData.put(DatabaseHelper.distance, DatabaseHelper.ORIGIN_DISTANCE);
                baseStationContent.put(DatabaseHelper.isOrigin, DatabaseHelper.ORIGIN);

            } else if(stationCount == 1){
                //stationData.put(DatabaseHelper.xPosition, DatabaseHelper.station2InitialX);
                stationData.put(DatabaseHelper.yPosition, DatabaseHelper.station2InitialY);
                stationData.put(DatabaseHelper.alpha, DatabaseHelper.station2Alpha);
                baseStationContent.put(DatabaseHelper.isOrigin, 0);

            } else{
                Toast.makeText(getActivity(), "Wrong Data", Toast.LENGTH_LONG).show();
                Log.d(TAG, "StationCount Greater than 2");
            }
            if (checkStationInMobileTable(db, MMSI)) {
                db.delete(DatabaseHelper.mobileStationTable, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(MMSI)});
                Log.d(TAG, "Station Removed from Mobile Station Table");
            }

            db.insert(DatabaseHelper.stationListTable, null, station);
            db.insert(DatabaseHelper.baseStationTable, null, baseStationContent);
            db.insert(DatabaseHelper.fixedStationTable, null, stationData);

            //db.close();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Unavailable");
            e.printStackTrace();
            return false;
        }
        return true;

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
            mMobileStationCursor.close();
        }catch (SQLException e){
            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        }finally {
            if(mMobileStationCursor != null){
                mMobileStationCursor.close();
            }
        }
        return isPresent;
    }

    @Override
    public String toString(){
        return "mmsiFragment";
    }

    /**
     * Checks if the MMSI passed to it already exists in the Database table {@link DatabaseHelper#baseStationTable}.
     * @param db an instance of {@link SQLiteDatabase}
     * @param MMSI MMSI number of the Station being installed.
     * @return MMSI number of the Base Station if it exists else returns 0.
     */
    private int baseStationRetrievalfromDB(SQLiteDatabase db, int MMSI){

        Cursor mBaseStnCursor = null;
        try {
            int existingBaseStnMMSI;
            //SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            //SQLiteDatabase db = dbHelper.getReadableDatabase();
            mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi},
                    DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(MMSI)}, null, null, null);


            int index = 0;

            if (mBaseStnCursor.moveToFirst()) {
                existingBaseStnMMSI = mBaseStnCursor.getInt(mBaseStnCursor.getColumnIndex(DatabaseHelper.mmsi));
            }else {
                existingBaseStnMMSI = 0;
            }
            Log.d(TAG, String.valueOf(existingBaseStnMMSI));
            mBaseStnCursor.close();
            return existingBaseStnMMSI;

        }catch (SQLException e){

            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
            return 0;
        }finally {
            if(mBaseStnCursor != null){
                mBaseStnCursor.close();
            }
        }
    }



}
