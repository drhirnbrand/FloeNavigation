package de.awi.floenavigation.initialsetup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.admin.AdminPageActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.helperclasses.FragmentChangeListener;
import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.R;

/**
 * This is the primary {@link FragmentActivity} for the Initial Setup of the Coordinate System. To setup the Floe's Coordinate System two
 * Fixed/Base Stations need to installed. For details on the Coordinate System and how it's setup check the Floe Navigation System Guides.
 * <p>
 * The activity's own layout is empty containing only a single {@link android.widget.FrameLayout}. The Fragments {@link MMSIFragment} and {@link CoordinateFragment}
 * run on top of this Activity to install the first two Fixed/Base Stations on which the Coordinate System is setup.
 * </p>
 *
 * <p>
 * The icons shown on the Action Bar and the Up button on the screen depend on the running fragment. The Backbutton, Action Bar icons and
 * Up Button are disabled in the fragment {@link CoordinateFragment} and are enabled in {@link MMSIFragment}.
 * </p>
 * @see DatabaseHelper
 * @see SetupActivity
 * @see de.awi.floenavigation.initialsetup
 */
public class GridSetupActivity extends FragmentActivity implements FragmentChangeListener {

    /**
     * Variable to check which step of the Initial Setup Configuration is running.
     * <code>true</code> when {@link MMSIFragment} has run and now {@link CoordinateFragment} has to be loaded.
     * Not used completely in this version.
     */
    private boolean configSetupStep;

    private static final String TAG = "GridSetupActivity";

    /**
     * Default IP Address/Hostname of the AIS Transponder with which the App will try to make a Telnet Connection to read the incoming
     * AIS Data Stream. Used by {@link de.awi.floenavigation.network.NetworkMonitor} and {@link de.awi.floenavigation.aismessages.AISMessageReceiver} to
     * create a Telnet Connection.
     */
    public static final String dstAddress = "192.168.0.1";

    /**
     * Default Port on the AIS Transponder with which the App will try to make a Telnet Connection to read the incoming
     * AIS Data Stream. Used by {@link de.awi.floenavigation.network.NetworkMonitor} and {@link de.awi.floenavigation.aismessages.AISMessageReceiver} to
     * create a Telnet Connection.
     */
    public static final int dstPort = 2000;

    /**
     * {@link BroadcastReceiver} for receiving the GPS location broadcast from {@link GPS_Service}
     */
    private BroadcastReceiver broadcastReceiver;

    /**
     * {@link BroadcastReceiver} for checking the WiFi connection to an AIS Transponder which is broadcast from {@link de.awi.floenavigation.network.NetworkMonitor}.
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
     * A {@link Handler} which is runs a {@link Runnable} object which changes the Action Bar icons colors according to {@link #packetStatus}
     * and {@link #locationStatus}.
     */
    private final Handler statusHandler = new Handler();

    /**
     * {@link MenuItem}s shown on the Action Bar
     */
    private MenuItem gpsIconItem, aisIconItem, gridSetupIconItem;


    /**
     * Default implementation of {@link FragmentActivity#onCreate(Bundle)}. Loads the {@link MMSIFragment} to start the Grid Initial Configuration Process.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid_setup);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if (savedInstanceState != null){
            configSetupStep = savedInstanceState.getBoolean("SetupStep");
        }

        if (!configSetupStep) {
            configSetupStep = true;
            MMSIFragment mmsiFragment = new MMSIFragment();
            this.replaceFragment(mmsiFragment);
        } //else if (configSetupStep == 1){
           // CoordinateFragment coordinateFragment = new CoordinateFragment();
           // this.replaceFragment(coordinateFragment);
        //}

    }

    /**
     * Called when the activity is about to start, it sets ActionBar icons on the screen by calling the {@link #actionBarUpdatesFunction()}.
     */
    @Override
    protected void onStart() {
        super.onStart();
        actionBarUpdatesFunction();
    }

    /**
     * Registers and implements the {@link BroadcastReceiver}s for the AIS Connectivity and GPS location broadcasts; which are sent from
     * {@link de.awi.floenavigation.network.NetworkMonitor} and {@link GPS_Service} respectively.
     * The GPS Broadcast receiver sets the value of {@link #locationStatus} to the value from the {@link GPS_Service}.
     * The AIS Connectivity broadcast receiver sets the boolean {@link #packetStatus}.
     * This also registers {@link Runnable} with the {@link Handler} {@link #statusHandler} which runs at a regular interval specified by {@link ActionBarActivity#UPDATE_TIME}  and
     * it checks the booleans {@link #locationStatus} and {@link #packetStatus} and changes the Action Bar icons for GPS and AIS Connectivity
     * accordingly.
     *
     * @see Runnable
     * @see Handler
     * @see BroadcastReceiver
     * @see ActionBarActivity
     */
    private void actionBarUpdatesFunction() {

        /*****************ACTION BAR UPDATES*************************/
        if (broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
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
     * @inheritDoc
     */
    @Override
    public void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frag_container, fragment, fragment.toString());
        fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.commit();
    }

    /**
     * Called before {@link FragmentActivity#onStop()}. Saves the value of {@link #configSetupStep} in a {@link Bundle} so that if the App is run again
     * it will load the correct Fragment.
     * @param savedInstanceState
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("SetupStep", configSetupStep);
    }

    /**
     * Shows the Up button in the ActionBar on the tablet Screen.
     */
    public void showUpButton(){
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Hides the Up button in the ActionBar on the tablet Screen.
     */
    public void hideUpButton(){
        getActionBar().setDisplayHomeAsUpEnabled(false);
    }

    /**
     * Overrides the Default Back button behavior of Android. Disables Back button if current Fragment is {@link CoordinateFragment} else
     * starts the {@link AdminPageActivity}
     */
    @Override
    public void onBackPressed(){
        Fragment frag = this.getSupportFragmentManager().findFragmentById(R.id.frag_container);
        if (frag instanceof MMSIFragment){
            Intent intent = new Intent(this, AdminPageActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else{
            Toast.makeText(this, "Please finish Setup", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Creates the Action Bar icons on top of the screen. By default it shows the GPS icon, AIS Connectivity icon and the Grid Setup icon.
     * @param menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
     * Called when the Activity is no longer Visible on the screen. It unregisters the AIS and GPS {@link BroadcastReceiver}s.
     */
    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
    }
    /******************************************/


}
