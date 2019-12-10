package de.awi.floenavigation.admin;

import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import androidx.cardview.widget.CardView;
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

/**
 * Activity is responsible for presenting all the menus accessible for the admin
 * On pressing any of the menu, the admin is redirected to the corresponding activity associated with that menu
 * On first login, the admin has an added task to enter the tablet id {@value DatabaseHelper#tabletId} which would be stored in
 * {@link DatabaseHelper#configParametersTable} table.
 * This class handles animation of the menus to be visible on the activity in a precalculated delay
 */
public class AdminPageActivity extends ActionBarActivity {
    private static final String TAG = "AdminPageActivity";

    /**
     * Cardview objects for different menus
     */
    CardView gridConfigOption, SyncOption, adminPrivilegesOption, configParamsOption,
            recoverycardOption, deploymentcardOption;
    /**
     * Handler object to execute the runnable
     */
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

    /**
     * onCreate function is responsible for initializing and associating a handler {@link #handler} with a runnable object with the views {@link #gridConfigOption},
     * {@link #SyncOption} etc., of the .xml file
     * @param savedInstanceState Used to store previous information of paramters before the app is minimized
     */
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

    /**
     * onClick listener to start {@link GridSetupActivity} activity
     * It is called when a view has been clicked
     * The {@link GridSetupActivity} activity is only called when the initial configuration is done for the first time.
     * If the grid is setup, this menu will be disabled.
     * @param view The view that was clicked
     */
    public void onClickGridConfiguration(View view){
       if(!isSetupComplete()){
           Intent intent = new Intent(this, GridSetupActivity.class);
           startActivity(intent);
       } else{
           Toast.makeText(this, "Grid is already Setup", Toast.LENGTH_LONG).show();
       }
    }

    /**
     * Function to check whether the initial grid setup has been completed.
     * It accesses the local internal database {@value DatabaseHelper#stationListTable} to check the number of entries
     * @return It returns <code>true</code> if the grid initial setup is complete.
     */
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

    /**
     * Function to verify whether the tablet name has been entered by the admin for the first login
     * If the tablet name is initialized and its present in its internal local database, the return value makes sure that the dialog box to enter the tabletName is not
     * visible for successive logins and the admin is only presented directly with all the menus of the admin dashboard.
     * @return <code>true</code> if the tablet name is setup
     */
    private boolean isTabletNameSetup(){
        boolean success = false;
        Cursor paramCursor = null;
        try{
            DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            paramCursor = db.query(DatabaseHelper.configParametersTable,
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

        } catch(SQLiteException e){
            Log.d(TAG, "Error Reading from Database");
        } finally {
            if(paramCursor != null) {
                paramCursor.close();
            }
        }
        return success;
    }

    /**
     * onClick listener to start {@link ConfigurationActivity} activity
     * It is called when a view has been clicked
     * @param view The view that was clicked
     */
    public void onClickConfigurationParams(View view) {
        Intent configActivityIntent = new Intent(this, ConfigurationActivity.class);
        startActivity(configActivityIntent);
    }

    /**
     * onClick listener to start {@link AdminUserPwdActivity} activity
     * It is called when a view has been clicked
     * @param view The view that was clicked
     */
    public void onClickAdminPrivilegesListener(View view) {
        Intent adminUserPwdActIntent = new Intent(this, AdminUserPwdActivity.class);
        startActivity(adminUserPwdActIntent);
    }

    /**
     * onClick listener to start {@link RecoveryActivity} activity
     * It is called when a view has been clicked
     * The activity is only called when there are 2 AIS stations available on the grid.
     * @param view The view that was clicked
     */
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

    /**
     * Function to start {@link DialogActivity} activity
     * Used to display dialog box pop up
     */
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

    /**
     * Overridden the back pressed function of android, to start {@link MainActivity} activity on back button press
     * when present in {@link AdminPageActivity}
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
    }

    /**
     * onClick listener to start {@link DeploymentActivity} activity
     * It is called when a view has been clicked
     * The activity is only called when there are 2 AIS stations available on the grid
     * @param view The view that was clicked
     */
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

    /**
     * onClick listener to start {@link SyncActivity} activity
     * It is called when a view has been clicked
     * @param view The view that was clicked
     */
    public void onClickSyncListener(View view) {
        Intent syncIntent = new Intent(this, SyncActivity.class);
        startActivity(syncIntent);
    }
}
