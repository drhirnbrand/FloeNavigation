package de.awi.floenavigation.admin;

import android.content.ContentValues;
import android.content.Context;
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
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.R;
import de.awi.floenavigation.waypoint.WaypointActivity;

public class ListViewActivity extends ActionBarActivity {

    private static final String TAG = "ListViewActivity";
    private ArrayList<ParameterListObject> parameterObjects = new ArrayList<ParameterListObject>();
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Intent intentOnExit;
    private int[] baseStnMMSI = new int[DatabaseHelper.INITIALIZATION_SIZE];


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
            }
        }
        return callingActivityString;
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

    private void insertIntoUsersDeletedTable(SQLiteDatabase db, String user) {
        ContentValues deletedUser = new ContentValues();
        deletedUser.put(DatabaseHelper.userName, user);
        deletedUser.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.userDeletedTable, null, deletedUser);
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

    private void insertIntoWaypointsDeletedTable(SQLiteDatabase db, String labelID) {
        ContentValues deletedWaypoint = new ContentValues();
        deletedWaypoint.put(DatabaseHelper.labelID, labelID);
        deletedWaypoint.put(DatabaseHelper.deleteTime, String.valueOf(System.currentTimeMillis() - super.timeDiff));
        db.insert(DatabaseHelper.waypointDeletedTable, null, deletedWaypoint);
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

    private boolean checkEntryInWaypointsTable(SQLiteDatabase db, String waypointToBeRemoved){
        boolean isPresent = false;
        try{
            Cursor waypointCursor = db.query(DatabaseHelper.waypointsTable, new String[]{DatabaseHelper.labelID},
                    DatabaseHelper.labelID + " = ?", new String[]{waypointToBeRemoved}, null, null, null);
            isPresent = waypointCursor.moveToFirst();
            waypointCursor.close();
        }catch (SQLiteException e){
            Log.d(TAG, "Station List Cursor error");
            e.printStackTrace();
        }
        return isPresent;
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

    private void baseStationsRetrievalfromDB(SQLiteDatabase db){

        try {
            //SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            //SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor mBaseStnCursor = db.query(DatabaseHelper.baseStationTable, new String[]{DatabaseHelper.mmsi, DatabaseHelper.isOrigin},
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
            mBaseStnCursor.close();
        }catch (SQLException e){

            Log.d(TAG, "SQLiteException");
            e.printStackTrace();
        }

    }


    private ArrayList<ParameterListObject> generateDataFromWaypointsTable(){
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor waypointsCursor = db.query(DatabaseHelper.waypointsTable,
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
            waypointsCursor.close();
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        }
        return parameterObjects;
        //arrayAdapter.notifyDataSetChanged();

    }

    private ArrayList<ParameterListObject> generateDataFromFixedStnTable(){
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //Cursor fixedStnCursor = db.query(DatabaseHelper.fixedStationTable,
            //        new String[] {DatabaseHelper.stationName, DatabaseHelper.mmsi, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
            //        null,
            //        null,
            //        null, null, null);
            Cursor fixedStnCursor = db.rawQuery("Select " + DatabaseHelper.stationName + ", " + DatabaseHelper.mmsi + ", " + DatabaseHelper.xPosition +
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
            fixedStnCursor.close();
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
            e.printStackTrace();
        }
        return parameterObjects;
        //arrayAdapter.notifyDataSetChanged();

    }

    private ArrayList<ParameterListObject> generateDataFromStaticStnTable(){
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor staticStnCursor = db.query(DatabaseHelper.staticStationListTable,
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
            staticStnCursor.close();
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        }
        return parameterObjects;
        //arrayAdapter.notifyDataSetChanged();

    }

    private ArrayList<ParameterListObject> generateDataUsersTable(){
        try{
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //Cursor fixedStnCursor = db.query(DatabaseHelper.fixedStationTable,
            //        new String[] {DatabaseHelper.stationName, DatabaseHelper.mmsi, DatabaseHelper.xPosition, DatabaseHelper.yPosition},
            //        null,
            //        null,
            //        null, null, null);
            Cursor usersCursor = db.query(DatabaseHelper.usersTable,
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
            usersCursor.close();
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
            e.printStackTrace();
        }
        return parameterObjects;
        //arrayAdapter.notifyDataSetChanged();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }


}

class ParameterListObject{

    private String labelID;
    private double xValue;
    private double yValue;
    private String userName;
    private boolean isUserActivity = false;

    ParameterListObject(String labelID, double xValue, double yValue){
        this.labelID = labelID;
        this.xValue = xValue;
        this.yValue = yValue;
        this.isUserActivity = false;
    }

    ParameterListObject(String userName){
        this.userName = userName;
        this.isUserActivity = true;
    }

    public String getLabelID() {
        return labelID;
    }

    public String getxValue() {
        return String.valueOf(xValue);
    }

    public String getyValue() {
        return String.valueOf(yValue);
    }

    public String getUserName() {
        return userName;
    }
    public boolean getIsUserActivity(){ return isUserActivity; }
}

class ListViewAdapter extends RecyclerView.Adapter<ListViewAdapter.ViewHolder> {

    private static final String TAG = "ListViewActivity";
    private ArrayList<ParameterListObject> parameters;
    private Context context;
    private View view;

    ListViewAdapter(ListViewActivity listViewActivity, ArrayList<ParameterListObject> parameterListObjects) {
        this.parameters = parameterListObjects;
    }

    @NonNull
    @Override
    public ListViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

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


    @Override
    public int getItemCount() {
        return parameters.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView labelIDView;
        TextView xPosValue;
        TextView yPosValue;
        TextView xPosLabel;
        TextView yPosLabel;

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
