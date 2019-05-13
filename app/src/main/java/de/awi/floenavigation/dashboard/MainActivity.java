package de.awi.floenavigation.dashboard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import de.awi.floenavigation.R;
import de.awi.floenavigation.admin.LoginPage;
import de.awi.floenavigation.deployment.DeploymentActivity;
import de.awi.floenavigation.grid.GridActivity;
import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.DialogActivity;
import de.awi.floenavigation.network.NetworkService;
import de.awi.floenavigation.sample_measurement.SampleMeasurementActivity;
import de.awi.floenavigation.services.AlphaCalculationService;
import de.awi.floenavigation.services.AngleCalculationService;
import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.services.PredictionService;
import de.awi.floenavigation.services.ValidationService;
import de.awi.floenavigation.waypoint.WaypointActivity;

/**
 * {@link MainActivity} represents the main dashboard of the app.
 * One can navigate to different screens from this dashboard.
 */
public class MainActivity extends ActionBarActivity {

    /**
     * Flag to check whether {@link NetworkService} is enabled
     * <code>true</code> {@link NetworkService} is running
     * <code>false</code> otherwise
     */
    private static boolean networkSetup = false;
    /**
     * code used for requesting permission
     */
    public static final int GPS_REQUEST_CODE = 10;
    /**
     * Stores the number of base stations
     */
    public static long numOfBaseStations;
    /**
     * Stores the number of devices installed.
     * This variable is 0 if the devices are not downloaded from the server
     */
    private static long numOfDeviceList;
    /**
     * Variable to check whether services are running
     * <code>true</code> when the services are enabled it is initialized to true provided the grid initial setup is completed
     * <code>false</code> otherwise
     */
    public static boolean areServicesRunning = false;
    /**
     * for logging purpose
     */
    private static final String TAG = "MainActivity";

    /**
     * {@link NetworkService} and {@link GPS_Service} are started and sets {@link #networkSetup} to true.
     * Also starts other services when grid initial setup is completed.
     * @param savedInstanceState stores previous instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Start Network Monitor Service

        if(!networkSetup){
            Log.d(TAG, "NetworkServicie not Running. Starting NetworkService");
            networkSetup = true;
            Intent networkServiceIntent = new Intent(this, NetworkService.class);
            startService(networkServiceIntent);

        } else{
            Log.d(TAG, "NetworkService Already Running");
        }

        //Start GPS Service
        if (!GPS_Service.isInstanceCreated()) {
            Log.d(TAG, "GPS_SERVICE not Running. Starting GPS_SERVICE");
            checkPermission();
        }else{
            Log.d(TAG, "GPSService Already Running");
        }

        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            numOfBaseStations = DatabaseUtils.queryNumEntries(db, DatabaseHelper.baseStationTable);
            numOfDeviceList = DatabaseUtils.queryNumEntries(db, DatabaseHelper.deviceListTable);
        }catch (SQLiteException e){
            Log.d(TAG, "Error reading database");
            e.printStackTrace();
        }
        if(numOfBaseStations >= DatabaseHelper.INITIALIZATION_SIZE){

            if(!areServicesRunning){
                Log.d(TAG, "AngleCalculationService not Running. Starting AngleCalulationService");
                Intent angleCalculationServiceIntent = new Intent(getApplicationContext(), AngleCalculationService.class);
                startService(angleCalculationServiceIntent);

                Log.d(TAG, "AlphaCalculationService not Running. Starting AlphaCalulationService");
                Intent alphaCalculationServiceIntent = new Intent(getApplicationContext(), AlphaCalculationService.class);
                startService(alphaCalculationServiceIntent);

                Log.d(TAG, "PredictionService not Running. Starting PredictionService");
                Intent predictionServiceIntent = new Intent(getApplicationContext(), PredictionService.class);
                startService(predictionServiceIntent);

                Log.d(TAG, "ValidationService not Running. Starting ValidationService");
                Intent validationServiceIntent = new Intent(getApplicationContext(), ValidationService.class);
                startService(validationServiceIntent);

                areServicesRunning = true;
            } else{
                Log.d(TAG, "AngleCalculationService already Running");
                Log.d(TAG, "AlphaCalculationService already Running");
                Log.d(TAG, "PredictionService already Running");
                Log.d(TAG, "ValidationService already Running");
            }
        }

    }

    /**
     * If an app needs to use resources or information outside of its own sandbox, the app has to request the appropriate
     * permission. The app needs a permission by listing the permission in the app manifest and then
     * requesting that the user approve each permission at runtime.
     * It requests permission using request code {@value #GPS_REQUEST_CODE}
     */
    private void checkPermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.INTERNET},
                                GPS_REQUEST_CODE);
            }
            return;
        }
        Intent intent = new Intent(this, GPS_Service.class);
        startService(intent);
    }

    /**
     * When the user responds to the app's permission request, the system invokes the app's onRequestPermissionsResult() method,
     * passing it the user response. The app has to override that method to find out whether the permission was granted.
     * The callback is passed the same request code {@link #GPS_REQUEST_CODE} that was passed to requestPermissions().
     * @param requestCode request code
     * @param permissions permission
     * @param grantResults results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch (requestCode){
            case GPS_REQUEST_CODE:
                checkPermission();
                break;
            default:
                break;
        }
    }

    /**
     * Used for creating menu list
     * @param menu menu
     * @return parent
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu, 2);
    }

    /**
     * About us menu item
     * Displays the information of the app and the developers
     * @param menuItem menu item
     * @return true
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()){
            case R.id.aboutUs:
                 displayDialogBox();
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    /**
     * Starts the {@link DialogActivity} to display information of the app and
     * the developers
     */
    private void displayDialogBox() {

        Intent dialogIntent = new Intent(this, DialogActivity.class);
        dialogIntent.putExtra(DialogActivity.DIALOG_ABOUTUS, true);
        startActivity(dialogIntent);
    }

    /**
     * Starts the {@link DeploymentActivity} provided grid initial setup is completed
     * @param view view which was clicked
     */
    public void onClickDeploymentBtn(View view){
        if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS) {
            Intent deploymentIntent = new Intent(this, DeploymentActivity.class);
            deploymentIntent.putExtra("DeploymentSelection", false);
            startActivity(deploymentIntent);
        }else {
            Toast.makeText(getApplicationContext(), "Initial configuration is not completed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Starts {@link LoginPage}
     * @param view view which was clicked
     */
    public void onClickAdminBtn(View view){
        Intent intent = new Intent(this, LoginPage.class);
        startActivity(intent);
    }

    /**
     * Starts the {@link SampleMeasurementActivity} provided grid initial setup is completed and devices have been installed {@link #numOfDeviceList}
     * @param view view which was clicked
     */
    public void onClickSampleMeasureBtn(View view) {
        if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS) {
            if (numOfDeviceList != 0) {
                Intent sampleMeasureIntent = new Intent(this, SampleMeasurementActivity.class);
                startActivity(sampleMeasureIntent);
            }else
                Toast.makeText(getApplicationContext(), "No devices installed", Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(getApplicationContext(), "Initial configuration is not completed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Starts the {@link GridActivity} provided grid initial setup is completed
     * @param view view which was clicked
     */
    public void onClickGridButton(View view) {
        if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS) {
            Intent gridActivityIntent = new Intent(this, GridActivity.class);
            startActivity(gridActivityIntent);
        }else {
            Toast.makeText(getApplicationContext(), "Initial configuration is not completed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Starts the {@link WaypointActivity} provided grid initial setup is completed
     * @param view view which was clicked
     */
    public void onClickWaypointBtn(View view) {
        if (numOfBaseStations >= DatabaseHelper.NUM_OF_BASE_STATIONS) {
            Intent waypointIntent = new Intent(this, WaypointActivity.class);
            startActivity(waypointIntent);
        }else {
            Toast.makeText(getApplicationContext(), "Initial configuration is not completed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * On Back button
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

}
