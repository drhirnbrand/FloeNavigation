package de.awi.floenavigation.admin;

import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.widget.Toast;

import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.DialogActivity;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.R;
import de.awi.floenavigation.deployment.DeploymentActivity;
import de.awi.floenavigation.initialsetup.GridSetupActivity;
import de.awi.floenavigation.synchronization.SyncActivity;

public class AdminPageActivity extends ActionBarActivity {
    private static final String TAG = "AdminPageActivity";

    CardView gridConfigOption, SyncOption, adminPrivilegesOption, configParamsOption,
            recoverycardOption, deploymentcardOption;
    Handler handler = new Handler();
    Runnable gridConfigRunnable = new Runnable() {
        @Override
        public void run() {
            gridConfigOption.setVisibility(View.VISIBLE);
        }
    };
    Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            SyncOption.setVisibility(View.VISIBLE);
        }
    };
    Runnable adminPrivilegesRunnable = new Runnable() {
        @Override
        public void run() {
            adminPrivilegesOption.setVisibility(View.VISIBLE);
        }
    };

    Runnable configParamsRunnable = new Runnable() {
        @Override
        public void run() {
            configParamsOption.setVisibility(View.VISIBLE);
        }
    };

    Runnable recoveryRunnable = new Runnable() {
        @Override
        public void run() {
            recoverycardOption.setVisibility(View.VISIBLE);
        }
    };

    Runnable deploymentRunnable = new Runnable() {
        @Override
        public void run() {
            deploymentcardOption.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_page);
        if(!isTabletNameSetup()) {
            dialogBoxDisplay();
        }
        gridConfigOption = (CardView) findViewById(R.id.gridconfigcardView);
        handler.postDelayed(gridConfigRunnable, 100);
        SyncOption = (CardView) findViewById(R.id.synccardView);
        handler.postDelayed(syncRunnable, 300);
        adminPrivilegesOption = (CardView) findViewById(R.id.admincardView);
        handler.postDelayed(adminPrivilegesRunnable, 500);
        configParamsOption = (CardView) findViewById(R.id.configparamcardView);
        handler.postDelayed(configParamsRunnable, 700);
        recoverycardOption = (CardView) findViewById(R.id.recoverycardView);
        handler.postDelayed(recoveryRunnable, 900);
        deploymentcardOption = (CardView) findViewById(R.id.deploymentcardView);
        handler.postDelayed(deploymentRunnable, 1100);
    }

    public void onClickGridConfiguration(View view){
       if(!isSetupComplete()){
           Intent intent = new Intent(this, GridSetupActivity.class);
           startActivity(intent);
       } else{
           Toast.makeText(this, "Grid is already Setup", Toast.LENGTH_LONG).show();
           //For testing purposes has to be removed later.
           //clearDatabase();
       }
    }


    private boolean isSetupComplete(){
        long count = 0;
        boolean success = true;
        try{

            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());;
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //db = databaseHelper.getReadableDatabase();
            count = DatabaseUtils.queryNumEntries(db, DatabaseHelper.stationListTable);
            //db.close();
        } catch (SQLiteException e){
            Toast.makeText(this, "Database Unavailable", Toast.LENGTH_LONG).show();
        }

        if (count < 2){
            success =  false;
        }
        return success;
    }

    private boolean isTabletNameSetup(){
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
                    Log.d(TAG, "TabletID is: " + paramValue);
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

    private void clearDatabase(){
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());;
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            db.execSQL("delete from AIS_STATION_LIST");
            db.execSQL("delete from AIS_FIXED_STATION_POSITION");
            db.execSQL("delete from BASE_STATIONS");
        } catch (SQLiteException e){
            Toast.makeText(this, "Database Unavailable", Toast.LENGTH_LONG).show();
        }
    }

    public void onClickConfigurationParams(View view) {
        Intent configActivityIntent = new Intent(this, ConfigurationActivity.class);
        startActivity(configActivityIntent);
    }

    public void onClickAdminPrivilegesListener(View view) {
        Intent adminUserPwdActIntent = new Intent(this, AdminUserPwdActivity.class);
        startActivity(adminUserPwdActIntent);
    }

    public void onClickRecoveryListener(View view) {

        long numOfBaseStations = 0;
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            numOfBaseStations = DatabaseUtils.queryNumEntries(db, DatabaseHelper.baseStationTable);
            Log.d(TAG, String.valueOf(numOfBaseStations));
            if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS) {
                Intent recoveryActivityIntent = new Intent(this, RecoveryActivity.class);
                startActivity(recoveryActivityIntent);
            }else {
                Toast.makeText(getApplicationContext(), "Initial configuration is not completed", Toast.LENGTH_SHORT).show();
            }
        }catch (SQLiteException e){
            Log.d(TAG, "Error reading database");
            e.printStackTrace();
        }
    }

    private void dialogBoxDisplay() {

        String popupMsg = "Please Give a Unique ID to this Tablet: ";
        String title = "Setup Tablet ID";
        Intent dialogIntent = new Intent(this, DialogActivity.class);
        dialogIntent.putExtra(DialogActivity.DIALOG_TITLE, title);
        dialogIntent.putExtra(DialogActivity.DIALOG_MSG, popupMsg);
        dialogIntent.putExtra(DialogActivity.DIALOG_ICON, R.drawable.ic_done_all_black_24dp);
        dialogIntent.putExtra(DialogActivity.DIALOG_OPTIONS, false);
        dialogIntent.putExtra(DialogActivity.DIALOG_TABLETID, true);
        dialogIntent.putExtra(DialogActivity.DIALOG_ABOUTUS, false);
        startActivity(dialogIntent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
    }

    public void onClickDeploymentListener(View view) {
        long numOfBaseStations = 0;
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            numOfBaseStations = DatabaseUtils.queryNumEntries(db, DatabaseHelper.baseStationTable);
            if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS) {
                Intent deploymentIntent = new Intent(this, DeploymentActivity.class);
                deploymentIntent.putExtra("DeploymentSelection", true);
                startActivity(deploymentIntent);
            }else {
                Toast.makeText(getApplicationContext(), "Initial configuration is not completed", Toast.LENGTH_SHORT).show();
            }
        }catch (SQLiteException e){
            Log.d(TAG, "Error reading database");
            e.printStackTrace();
        }


    }

    public void onClickSyncListener(View view) {
        Intent syncIntent = new Intent(this, SyncActivity.class);
        startActivity(syncIntent);
    }
}
