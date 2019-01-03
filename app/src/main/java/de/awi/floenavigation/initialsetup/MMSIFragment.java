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
 * A simple {@link Fragment} subclass.
 */
public class MMSIFragment extends Fragment implements View.OnClickListener{


    private static final int VALID_MMSI_LENGTH = 9;
    private SQLiteDatabase db;
    private long stationCount;
    private static final String TAG = "MMSI Fragment";
    private SQLiteOpenHelper dbHelper;
    private EditText aisStationName;
    private EditText mmsi;



    public MMSIFragment() {
        // Required empty public constructor
    }


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

    @Override
    public void onResume(){
        super.onResume();
        GridSetupActivity activity = (GridSetupActivity)getActivity();
        if(activity != null){
            activity.showUpButton();
        }
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.confirm_Button:
                onClickConfirm();
                break;
        }
    }


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

    private boolean validateMMSINumber(EditText mmsi) {
        return mmsi.getText().length() == VALID_MMSI_LENGTH && !TextUtils.isEmpty(mmsi.getText().toString()) && TextUtils.isDigitsOnly(mmsi.getText().toString());
    }


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

    private boolean checkStationInMobileTable(SQLiteDatabase db, int MMSI){
        boolean isPresent = false;
        try{

            Cursor mMobileStationCursor;
            mMobileStationCursor = db.query(DatabaseHelper.mobileStationTable, new String[]{DatabaseHelper.mmsi}, DatabaseHelper.mmsi + " = ?",
                    new String[]{String.valueOf(MMSI)}, null, null, null);
            isPresent = mMobileStationCursor.moveToFirst();
            mMobileStationCursor.close();
        }catch (SQLException e){
            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        }
        return isPresent;
    }

    @Override
    public String toString(){
        return "mmsiFragment";
    }

    private int baseStationRetrievalfromDB(SQLiteDatabase db, int MMSI){

        try {
            int existingBaseStnMMSI;
            //SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            //SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi},
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
        }
    }



}
