package de.awi.floenavigation.admin;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.R;
import de.awi.floenavigation.sample_measurement.SampleMeasurementActivity;
import de.awi.floenavigation.waypoint.WaypointActivity;

/**
 * {@link ListViewActivity} is responsible for displaying a list of static stations and fixed stations depending on the
 * intent extra received from the calling activity {@link RecoveryActivity#onClickViewDeployedStations(View)}
 * Added feature of swipping right on the entry present in the list will recover/delete the corresponding station from the internal
 * local database tables
 */
public class ListViewActivity extends ActionBarActivity {

    private static final String TAG = "ListViewActivity";
    /**
     * Object is used to store different parameters in case of fixed station recovery
     * the name of the station and the x, y positions of the station in the grid
     * This helps in displaying the contents on the list
     */
    private ArrayList<ParameterListObject> parameterObjects = new ArrayList<ParameterListObject>();
    /**
     * Recycler view used to display scrolling list of elements
     */
    private RecyclerView mRecyclerView;
    /**
     * List view adapter to be used by {@link #mRecyclerView}
     * Used for notifying the position of the item removed from the list
     */
    private RecyclerView.Adapter mAdapter;
    /**
     * Grid layout manager to be used by {@link #mRecyclerView}
     */
    private RecyclerView.LayoutManager mLayoutManager;
    /**
     * Intent variable used to store the intent activity which is to be started when the
     * station/user is removed
     */
    private Intent intentOnExit;
    /**
     * Array to store the base stations used to initially set up the grid
     */
    private int[] baseStnMMSI = new int[DatabaseHelper.INITIALIZATION_SIZE];


    /**
     * Initializes the layout and the recycler view {@link #mRecyclerView}
     * Listener initialization for the swipe event in case of removal
     * Paint method added for the animation and the layout of the element in the list
     * @param savedInstanceState stores the previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);
        final String callingActivity = getCallingActivityName();
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mRecyclerView = findViewById(R.id.parametersListRecyclerView);
        mLayoutManager = new GridLayoutManager(this, 1);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.setAdapter(mAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                mAdapter.notifyDataSetChanged();
                deleteEntry(callingActivity, position, getNumOfAISStation());
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                final int position = viewHolder.getAdapterPosition();
                Bitmap icon;
                Paint p = new Paint();
                if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE){

                    View itemView = viewHolder.itemView;
                    float height = (float) itemView.getBottom() - (float) itemView.getTop();
                    float width = height / 3;

                    if(dX > 0){
                        p.setColor(Color.parseColor("#D32F2F"));
                        RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX,(float) itemView.getBottom());
                        c.drawRect(background,p);
                        icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_delete);
                        RectF icon_dest = new RectF((float) itemView.getLeft() + width ,(float) itemView.getTop() + width,(float) itemView.getLeft()+ 2*width,(float)itemView.getBottom() - width);
                        c.drawBitmap(icon,null,icon_dest,p);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });

        itemTouchHelper.attachToRecyclerView(mRecyclerView);

    }

    /**
     * This method is called from {@link androidx.recyclerview.widget.ItemTouchHelper} callback in {@link #onCreate(Bundle)}
     * The Listener is called when the admin or the user swipes the element in the list
     * Depending on the calling activity, the local variable flag isRemoved is initialized
     * If the flag is set to true, the entry is removed from the list
     * @param callingActivityString the name of the calling activity
     * @param position position of the entry in the list
     * @param numOfStations Only required for logging purpose
     */
    private void deleteEntry(String callingActivityString, int position, long numOfStations){

        boolean isRemoved = false;

        switch (callingActivityString){
            case "WaypointActivity":
                isRemoved = deleteEntryfromWaypointsTableinDB(parameterObjects.get(position).getLabelID());
                break;
            case "AISRecoverActivity":
                Log.d(TAG, "Num of entries: " + numOfStations);
                isRemoved = deleteEntryfromDBTables(parameterObjects.get(position).getLabelID().split(" ")[0]);
                break;
            case "StaticStationRecoverActivity":
                isRemoved = deleteEntryfromStaticStnTableinDB(parameterObjects.get(position).getLabelID());
                break;

            case "UsersPwdActivity":
                isRemoved = deleteEntryfromUsersTableinDB(parameterObjects.get(position).getUserName());
                break;

            case "SampleMeasurementActivity":
                isRemoved = false;
                Toast.makeText(getApplicationContext(), "Cannot Delete Samples", Toast.LENGTH_SHORT).show();
                break;
        }

        if (isRemoved) {
            parameterObjects.remove(position);
            mAdapter.notifyItemRemoved(position);
            mAdapter.notifyItemRangeChanged(position, parameterObjects.size());
            if (parameterObjects.size() == 0) {
                startActivity(intentOnExit);
            }
        }
    }

    /**
     * Function to determine the name of the activity from which the list view is called
     * Depending on the name of the activity {@link #mAdapter} and {@link #intentOnExit} are initialized
     * @return returns the name of the calling activity
     */
    private String getCallingActivityName(){

        Intent intent = getIntent();
        String callingActivityString = intent.getExtras().getString("GenerateDataOption");
        if (callingActivityString != null) {
            switch (callingActivityString){
                case "WaypointActivity":
                    mAdapter = new ListViewAdapter(this, generateDataFromWaypointsTable());
                    intentOnExit = new Intent(getApplicationContext(), WaypointActivity.class);
                    break;
                case "AISRecoverActivity":
                    mAdapter = new ListViewAdapter(this, generateDataFromFixedStnTable());
                    intentOnExit = new Intent(getApplicationContext(), RecoveryActivity.class);
                    break;
                case "StaticStationRecoverActivity":
                    mAdapter = new ListViewAdapter(this, generateDataFromStaticStnTable());
                    intentOnExit = new Intent(getApplicationContext(), RecoveryActivity.class);
                    break;

                case "UsersPwdActivity":
                    mAdapter = new ListViewAdapter(this, generateDataUsersTable());
                    intentOnExit = new Intent(getApplicationContext(), AdminUserPwdActivity.class);
                    break;

                case "SampleMeasurementActivity":
                    mAdapter = new ListViewAdapter(this, generateDataFromSamplesTable());
                    intentOnExit = new Intent(getApplicationContext(), SampleMeasurementActivity.class);
                    break;
            }
        }
        return callingActivityString;
    }

    /**
     * Internal local database access to determine number of fixed ais stations installed
     * @return the number of fixed ais stations
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
     * This function is called from {@link #deleteEntry(String, int, long)} (String)}
     * Based on the waypoint label ID, its entry from the local internal database is removed
     * Also inserts the waypoint name into the waypoint deleted table {@link #insertIntoWaypointsDeletedTable(SQLiteDatabase, String)}, which is later used for synchronization purpose
     * @param waypointToBeRemoved waypoint label ID
     * @return <code>true</code> if successful in deleting the entry from the database
     *         <code>false</code> otherwise
     */
    private boolean deleteEntryfromWaypointsTableinDB(String waypointToBeRemoved){
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            if (checkEntryInWaypointsTable(db, waypointToBeRemoved)) {
                db.delete(DatabaseHelper.waypointsTable, DatabaseHelper.labelID + " = ?", new String[]{waypointToBeRemoved});
                insertIntoWaypointsDeletedTable(db, waypointToBeRemoved);
                Toast.makeText(getApplicationContext(), "Removed from waypoints table", Toast.LENGTH_SHORT).show();
                return true;
            }else {
                Toast.makeText(this, "No Entry in DB", Toast.LENGTH_SHORT).show();
                return false;
            }

        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        }
        return false;
    }

    /**
     * This function is called from {@link #deleteEntry(String, int, long)} (String)}
     * Based on the static station name, its entry from the local internal database is removed
     * Also inserts the static station name into the static station deleted table {@link #insertIntoStaticStationDeletedTable(SQLiteDatabase, String)}, which is later used for synchronization purpose
     * @param stationToBeRemoved static station name to be removed
     * @return <code>true</code> if successful in deleting the entry from the database
     *         <code>false</code> otherwise
     */
    private boolean deleteEntryfromStaticStnTableinDB(String stationToBeRemoved){
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            if (checkEntryInStaticStnTable(db, stationToBeRemoved)) {
                db.delete(DatabaseHelper.staticStationListTable, DatabaseHelper.staticStationName + " = ?", new String[]{stationToBeRemoved});
                insertIntoStaticStationDeletedTable(db, stationToBeRemoved);
                Toast.makeText(getApplicationContext(), "Removed from static station table", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "No Entry in DB", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        }
        return false;
    }

    /**
     * This function is called from {@link #deleteEntry(String, int, long)} (String)}
     * Based on the mmsi of the station, its entry from the local internal database is removed
     * Also inserts the mmsi into the deleted tables {@link #insertIntoFixedStationDeletedTable(SQLiteDatabase, String)},
     * {@link #insertIntoBaseStationDeletedTable(SQLiteDatabase, String)}}
     * {@link #insertIntoStationListDeletedTable(SQLiteDatabase, String)}
     * which is later used for synchronization purpose
     * @param mmsiToBeRemoved mmsi to be removed
     * @return <code>true</code> if successful in deleting the entry from the database
     *         <code>false</code> otherwise
     */
    private boolean deleteEntryfromDBTables(String mmsiToBeRemoved){
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            baseStationsRetrievalfromDB(db);
            int numOfStations = (int) getNumOfAISStation();
            if (checkEntryInStationListTable(db, mmsiToBeRemoved)) {
                if (numOfStations <= DatabaseHelper.NUM_OF_BASE_STATIONS) {
                    Toast.makeText(getApplicationContext(), "Cannot be removed from DB tables, only 2 base stations available", Toast.LENGTH_SHORT).show();
                    return false;
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
                        insertIntoStationListDeletedTable(db, mmsiToBeRemoved);
                        insertIntoFixedStationDeletedTable(db, mmsiToBeRemoved);
                    }
                    Toast.makeText(getApplicationContext(), "Removed from DB tables", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }else {
                Toast.makeText(this, "No Entry in DB", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        }
        return false;
    }

    /**
     * This function is called from {@link #deleteEntry(String, int, long)} (String)}
     * Based on the user name, its entry from the local internal database is removed
     * Also inserts the user name into the deleted tables {@link #insertIntoUsersDeletedTable(SQLiteDatabase, String)}, which is later used for synchronization purpose
     * @param name user name  to be removed
     * @return <code>true</code> if successful in deleting the entry from the database
     *         <code>false</code> otherwise
     */
    private boolean deleteEntryfromUsersTableinDB(String name){
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            long numOfUsers = DatabaseUtils.queryNumEntries(db, DatabaseHelper.usersTable);
            if(numOfUsers > 1) {
                db.delete(DatabaseHelper.usersTable, DatabaseHelper.userName + " = ?", new String[]{name});
                insertIntoUsersDeletedTable(db, name);
                Toast.makeText(getApplicationContext(), "User Removed", Toast.LENGTH_SHORT).show();
                return true;
            } else{
                Toast.makeText(getApplicationContext(), "Atleast one Administrator is Required", Toast.LENGTH_SHORT).show();
                return false;
            }

        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        }
        return false;
    }

    /**
     * Inserts the user name to be removed into the {@link DatabaseHelper#userDeletedTable}
     * @param db SQLiteDatabase object
     * @param user user name to be removed
     */
    private void insertIntoUsersDeletedTable(SQLiteDatabase db, String user) {
        ContentValues deletedUser = new ContentValues();
        deletedUser.put(DatabaseHelper.userName, user);
        deletedUser.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.userDeletedTable, null, deletedUser);
    }

    /**
     * Inserts the mmsi to be removed into the {@link DatabaseHelper#baseStationDeletedTable}
     * @param db SQLiteDatabase object
     * @param mmsi mmsi to be removed
     */
    private void insertIntoBaseStationDeletedTable(SQLiteDatabase db, String mmsi) {
        ContentValues deletedBaseStation = new ContentValues();
        deletedBaseStation.put(DatabaseHelper.mmsi, mmsi);
        deletedBaseStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.baseStationDeletedTable, null, deletedBaseStation);
    }

    /**
     * Inserts the mmsi to be removed into the {@link DatabaseHelper#fixedStationDeletedTable}
     * @param db SQLiteDatabase object
     * @param mmsiToBeAdded mmsi to be removed
     */
    private void insertIntoFixedStationDeletedTable(SQLiteDatabase db, String mmsiToBeAdded) {
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.mmsi, Integer.valueOf(mmsiToBeAdded));
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.fixedStationDeletedTable, null, deletedStation);
    }

    /**
     * Inserts the mmsi to be removed into the {@link DatabaseHelper#stationListDeletedTable}
     * @param db SQLiteDatabase object
     * @param mmsiToBeAdded mmsi to be removed
     */
    private void insertIntoStationListDeletedTable(SQLiteDatabase db, String mmsiToBeAdded){
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.mmsi, Integer.valueOf(mmsiToBeAdded));
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.stationListDeletedTable, null, deletedStation);
    }

    /**
     * Inserts the static station name to be removed into the {@link DatabaseHelper#staticStationDeletedTable}
     * @param db SQLiteDatabase object
     * @param staticStnName static station name to be removed
     */
    private void insertIntoStaticStationDeletedTable(SQLiteDatabase db, String staticStnName) {
        ContentValues deletedStation = new ContentValues();
        deletedStation.put(DatabaseHelper.staticStationName, staticStnName);
        deletedStation.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.staticStationDeletedTable, null, deletedStation);
    }

    /**
     * Inserts the waypoint label ID to be removed into the {@link DatabaseHelper#waypointDeletedTable}
     * @param db SQLiteDatabase object
     * @param labelID waypoint label ID to be removed
     */
    private void insertIntoWaypointsDeletedTable(SQLiteDatabase db, String labelID) {
        ContentValues deletedWaypoint = new ContentValues();
        deletedWaypoint.put(DatabaseHelper.labelID, labelID);
        deletedWaypoint.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.waypointDeletedTable, null, deletedWaypoint);
    }

    /**
     * If the removed station is one of the base station that was used for initial grid setup
     * Change the mmsi number to either {@link DatabaseHelper#BASESTN1} or {@link DatabaseHelper#BASESTN2} based on whether the station is origin or x-axis
     * base station.
     * This is done so that {@link de.awi.floenavigation.services.PredictionService} can keep on predicting the positions of these stations
     * and the grid/coordinate system remains intact
     * Also these mmsi's could be used for redeployment at a different point on the grid
     * @param mmsi mmsi
     * @param db SQLiteDatabase object
     * @param originFlag <code>true</code> if the mmsi is of origin base station
     *                   <code>false</code> otherwise, if the mmsi is of x-axis base station
     */
    private void updataMMSIInDBTables(int mmsi, SQLiteDatabase db, boolean originFlag){
        ContentValues mContentValues = new ContentValues();
        mContentValues.put(DatabaseHelper.mmsi, ((originFlag) ? DatabaseHelper.BASESTN1 : DatabaseHelper.BASESTN2));
        mContentValues.put(DatabaseHelper.stationName, ((originFlag) ? DatabaseHelper.origin : DatabaseHelper.basestn1));
        db.update(DatabaseHelper.fixedStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
        db.update(DatabaseHelper.baseStationTable, mContentValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(mmsi)});
    }

    /**
     * Checks whether the mmsi is part of the stations installed on the grid
     * @param db SQLiteDatabase object
     * @param mmsi mmsi
     * @return <code>true</code> if the mmsi is present in the internal local database
     *         <code>false</code> otherwise
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
            if(stationListCursor != null){
                stationListCursor.close();
            }
        }
        return isPresent;
    }

    /**
     * Checks whether the waypoint is part of the waypoints installed on the grid
     * @param db SQLiteDatabase object
     * @param waypointToBeRemoved waypoint label ID
     * @return <code>true</code> if the waypoint is present in the internal local database
     *         <code>false</code> otherwise
     */
    private boolean checkEntryInWaypointsTable(SQLiteDatabase db, String waypointToBeRemoved){
        boolean isPresent = false;
        Cursor waypointCursor = null;
        try{
            waypointCursor = db.query(DatabaseHelper.waypointsTable, new String[]{DatabaseHelper.labelID},
                    DatabaseHelper.labelID + " = ?", new String[]{waypointToBeRemoved}, null, null, null);
            isPresent = waypointCursor.moveToFirst();
        }catch (SQLiteException e){
            Log.d(TAG, "Station List Cursor error");
            e.printStackTrace();
        }finally {
            if (waypointCursor != null){
                waypointCursor.close();
            }
        }
        return isPresent;
    }

    /**
     * Checks whether the static station name is part of the stations installed on the grid
     * @param db SQLiteDatabase object
     * @param stationToBeRemoved static station name
     * @return <code>true</code> if the static station is present in the internal local database
     *         <code>false</code> otherwise
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
     * Returns the base stations used for initial grid setup
     * @param db SQLiteDatabase object
     */
    private void baseStationsRetrievalfromDB(SQLiteDatabase db){

        Cursor mBaseStnCursor = null;
        try {
            //SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            //SQLiteDatabase db = dbHelper.getReadableDatabase();
            mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.isOrigin},
                    null, null, null, null, DatabaseHelper.isOrigin + " DESC");

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
     * Used to populate each entry in the list from the data extracted from the database
     * @return Returns {@link DatabaseHelper#labelID}, {@link DatabaseHelper#xPosition}, {@link DatabaseHelper#yPosition} of the waypoint
     */
    private ArrayList<ParameterListObject> generateDataFromWaypointsTable(){
        Cursor waypointsCursor = null;
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            waypointsCursor = db.query(DatabaseHelper.waypointsTable,
                    new String[] {DatabaseHelper.labelID, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
                    null,
                    null,
                    null, null, null);
            if (waypointsCursor.moveToFirst()) {
                do {
                    String labelID = waypointsCursor.getString(waypointsCursor.getColumnIndex(DatabaseHelper.labelID));
                    double xPosition = waypointsCursor.getDouble(waypointsCursor.getColumnIndex(DatabaseHelper.xPosition));
                    double yPosition = waypointsCursor.getDouble(waypointsCursor.getColumnIndex(DatabaseHelper.yPosition));

                    parameterObjects.add(new ParameterListObject(labelID, xPosition, yPosition));

                } while (waypointsCursor.moveToNext());
            }else {
                Log.d(TAG, "Error reading from waypointstable stn table");
            }
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        }finally {
            if (waypointsCursor != null){
                waypointsCursor.close();
            }
        }
        return parameterObjects;
        //arrayAdapter.notifyDataSetChanged();

    }

    /**
     * Used to populate each entry in the list from the data extracted from the database
     * @return Returns {@link DatabaseHelper#labelID}, {@link DatabaseHelper#updateTime}, {@link DatabaseHelper#xPosition},
     * {@link DatabaseHelper#yPosition} of the sample or the measurement taken
     */
    private ArrayList<ParameterListObject> generateDataFromSamplesTable(){
        String displayTime = "";
        Cursor samplesCursor = null;
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            samplesCursor = db.query(DatabaseHelper.sampleMeasurementTable,
                    new String[] {DatabaseHelper.labelID, DatabaseHelper.updateTime, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
                    null,
                    null,
                    null, null, null);
            if (samplesCursor.moveToFirst()) {
                do {
                    String labelID = samplesCursor.getString(samplesCursor.getColumnIndex(DatabaseHelper.labelID));
                    String time = samplesCursor.getString(samplesCursor.getColumnIndexOrThrow(DatabaseHelper.updateTime));
                    double xPosition = samplesCursor.getDouble(samplesCursor.getColumnIndex(DatabaseHelper.xPosition));
                    double yPosition = samplesCursor.getDouble(samplesCursor.getColumnIndex(DatabaseHelper.yPosition));
                    SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'D'HHmmss");
                    displayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    try {
                        displayTime = displayFormat.format(sdf.parse(time));
                    } catch (ParseException e){
                        displayTime = time;
                        Log.d(TAG, "Could not Parse the Date");
                    }
                    parameterObjects.add(new ParameterListObject(labelID + " \t" + displayTime, xPosition, yPosition));

                } while (samplesCursor.moveToNext());
            }else {
                Log.d(TAG, "Error reading from waypointstable stn table");
            }
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        } finally {
          if (samplesCursor != null){
              samplesCursor.close();
          }
        }
        return parameterObjects;
        //arrayAdapter.notifyDataSetChanged();

    }

    /**
     * Used to populate each entry in the list from the data extracted from the database
     * @return Returns {@link DatabaseHelper#mmsi}, {@link DatabaseHelper#stationName}, {@link DatabaseHelper#xPosition},
     * {@link DatabaseHelper#yPosition} of the fixed station
     */
    private ArrayList<ParameterListObject> generateDataFromFixedStnTable(){
        Cursor fixedStnCursor = null;
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            fixedStnCursor = db.rawQuery("Select " + DatabaseHelper.stationName + ", " + DatabaseHelper.mmsi + ", " + DatabaseHelper.xPosition +
                    ", " + DatabaseHelper.yPosition + " from " + DatabaseHelper.fixedStationTable
                    + " where " + DatabaseHelper.mmsi + " in (Select " + DatabaseHelper.mmsi + " from " + DatabaseHelper.stationListTable + ")", null);

            if (fixedStnCursor.moveToFirst()) {
                do {
                    int mmsi = fixedStnCursor.getInt(fixedStnCursor.getColumnIndex(DatabaseHelper.mmsi));
                    String stationName = fixedStnCursor.getString(fixedStnCursor.getColumnIndex(DatabaseHelper.stationName));
                    if (stationName == null)
                        stationName = "";
                    double xPosition = fixedStnCursor.getDouble(fixedStnCursor.getColumnIndex(DatabaseHelper.xPosition));
                    double yPosition = fixedStnCursor.getDouble(fixedStnCursor.getColumnIndex(DatabaseHelper.yPosition));

                    parameterObjects.add(new ParameterListObject(String.valueOf(mmsi + " " + stationName), xPosition, yPosition));

                } while (fixedStnCursor.moveToNext());
            }else {
                Log.d(TAG, "Error reading from fixed stn table");
            }
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
            e.printStackTrace();
        } finally {
            if (fixedStnCursor != null){
                fixedStnCursor.close();
            }
        }
        return parameterObjects;

    }

    /**
     * Used to populate each entry in the list from the data extracted from the database
     * @return Returns {@link DatabaseHelper#staticStationName}, {@link DatabaseHelper#xPosition},
     * {@link DatabaseHelper#yPosition} of the static station
     */
    private ArrayList<ParameterListObject> generateDataFromStaticStnTable(){
        Cursor staticStnCursor = null;
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            staticStnCursor = db.query(DatabaseHelper.staticStationListTable,
                    new String[] {DatabaseHelper.staticStationName, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
                    null,
                    null,
                    null, null, null);
            if (staticStnCursor.moveToFirst()) {
                do {
                    String stationName = staticStnCursor.getString(staticStnCursor.getColumnIndex(DatabaseHelper.staticStationName));
                    double xPosition = staticStnCursor.getDouble(staticStnCursor.getColumnIndex(DatabaseHelper.xPosition));
                    double yPosition = staticStnCursor.getDouble(staticStnCursor.getColumnIndex(DatabaseHelper.yPosition));

                    parameterObjects.add(new ParameterListObject(stationName, xPosition, yPosition));

                } while (staticStnCursor.moveToNext());
            }else {
                Log.d(TAG, "Error reading from static stn table");
            }
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        } finally {
            if (staticStnCursor != null){
                staticStnCursor.close();
            }
        }
        return parameterObjects;
        //arrayAdapter.notifyDataSetChanged();

    }

    /**
     * Used to populate each entry in the list from the data extracted from the database
     * @return Returns {@link DatabaseHelper#userName} of the user
     */
    private ArrayList<ParameterListObject> generateDataUsersTable(){
        Cursor usersCursor = null;
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            usersCursor = db.query(DatabaseHelper.usersTable,
                    new String[] {DatabaseHelper.userName},
                    null,
                    null,
                    null, null, null);

            if (usersCursor.moveToFirst()) {
                do {
                    String userName = usersCursor.getString(usersCursor.getColumnIndexOrThrow(DatabaseHelper.userName));
                    parameterObjects.add(new ParameterListObject(userName));

                } while (usersCursor.moveToNext());
            }else {
                Log.d(TAG, "Error reading from fixed stn table");
            }
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
            e.printStackTrace();
        } finally {
            if (usersCursor != null){
                usersCursor.close();
            }
        }
        return parameterObjects;
        //arrayAdapter.notifyDataSetChanged();

    }

    /**
     * Return back to the calling activity
     * @param item menu item clicked
     * @return <code>true</code> if home button on the top menu bar is clicked
     *         <code>false</code> otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }


}


/**
 * Data object to define the parameters which would be available on the entry in the list
 */
class ParameterListObject{

    /**
     * Used for waypoint
     */
    private String labelID;
    /**
     * Used for xPosition
     */
    private double xValue;
    /**
     * Used for yPosition
     */
    private double yValue;
    /**
     * Used for user
     */
    private String userName;
    /**
     * To determine whether the calling activity is adminitrator privileges (user) activity
     */
    private boolean isUserActivity = false;

    /**
     * Constructor to initialize the parameters
     * @param labelID {@link #labelID}
     * @param xValue  {@link #xValue}
     * @param yValue  {@link #yValue}
     */
    ParameterListObject(String labelID, double xValue, double yValue){
        this.labelID = labelID;
        this.xValue = xValue;
        this.yValue = yValue;
        this.isUserActivity = false;
    }

    /**
     * Constructor to initialize for user entry list
     * @param userName
     */
    ParameterListObject(String userName){
        this.userName = userName;
        this.isUserActivity = true;
    }

    /**
     *
     * @return returns {@link #labelID}
     */
    public String getLabelID() {
        return labelID;
    }

    /**
     *
     * @return returns {@link #xValue}
     */
    public String getxValue() {
        return String.valueOf(xValue);
    }

    /**
     *
     * @return returns {@link #yValue}
     */
    public String getyValue() {
        return String.valueOf(yValue);
    }

    /**
     *
     * @return returns {@link #userName}
     */
    public String getUserName() {
        return userName;
    }

    /**
     *
     * @return <code>true</code> if the calling activity is user activity
     *         <code>false</code> otherwise
     */
    public boolean getIsUserActivity(){ return isUserActivity; }
}

/**
 * Class to generate the {@link RecyclerView.Adapter}
 * which will be used as an input to {@link RecyclerView} to initialize the layout
 */
class ListViewAdapter extends RecyclerView.Adapter<ListViewAdapter.ViewHolder> {

    private static final String TAG = "ListViewActivity";
    /**
     * array of parameter list objects {@link ParameterListObject}
     */
    private ArrayList<ParameterListObject> parameters;
    /**
     * the view name
     */
    private View view;

    /**
     * constructor to initialize the parameters
     * @param listViewActivity the current activity
     * @param parameterListObjects the array of {@link ParameterListObject} objects
     */
    ListViewAdapter(ListViewActivity listViewActivity, ArrayList<ParameterListObject> parameterListObjects) {
        this.parameters = parameterListObjects;
    }

    /**
     * to create the view holder
     * @param parent parent view group
     * @param viewType not used
     * @return returns the view holder
     */
    @NonNull
    @Override
    public ListViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds the view holder
     * @param holder holder to accommodate the each parameters
     * @param position position value of the entry
     */
    @Override
    public void onBindViewHolder(@NonNull ListViewAdapter.ViewHolder holder, int position) {
        Log.d(TAG, "Parameter label: " + parameters.get(position).getLabelID());
        if(!parameters.get(position).getIsUserActivity()) {
            holder.labelIDView.setText(parameters.get(position).getLabelID());
            holder.xPosValue.setVisibility(View.VISIBLE);
            holder.yPosValue.setVisibility(View.VISIBLE);
            holder.xPosLabel.setVisibility(View.VISIBLE);
            holder.yPosLabel.setVisibility(View.VISIBLE);
            holder.xPosValue.setText(parameters.get(position).getxValue());
            holder.yPosValue.setText(parameters.get(position).getyValue());

        } else {
            holder.labelIDView.setText(parameters.get(position).getUserName());
            holder.xPosValue.setVisibility(View.GONE);
            holder.yPosValue.setVisibility(View.GONE);
            holder.xPosLabel.setVisibility(View.GONE);
            holder.yPosLabel.setVisibility(View.GONE);
        }
    }


    /**
     *
     * @return returns the size of parameters
     */
    @Override
    public int getItemCount() {
        return parameters.size();
    }

    /**
     * View holder definition of all the available parameters in the entry
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        TextView labelIDView;
        TextView xPosValue;
        TextView yPosValue;
        TextView xPosLabel;
        TextView yPosLabel;

        /**
         * Default constructor
         * find the view by ID
         * @param view view name
         */
        ViewHolder(View view) {
            super(view);
            Log.d(TAG, "Parameter label: " + labelIDView);
            labelIDView = view.findViewById(R.id.labelIDView);
            xPosValue = view.findViewById(R.id.xPosView);
            yPosValue = view.findViewById(R.id.yPosView);
            xPosLabel = view.findViewById(R.id.xPosLabel);
            yPosLabel = view.findViewById(R.id.yPosLabel);

        }



    }


}
