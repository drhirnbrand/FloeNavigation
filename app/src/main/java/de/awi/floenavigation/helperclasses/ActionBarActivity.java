package de.awi.floenavigation.helperclasses;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import de.awi.floenavigation.R;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.services.GPS_Service;

/**
 * This {@link Activity} creates and maintains the icons on the ActionBar at the top of the screen. This activity does not run on its
 * own rather maintains the ActionBar icons for its child classes/activities.
 * <p>
 *     This activity checks if the tablet is connected to the WiFi network of an AIS transponder, if the tablet has a GPS Fix and if
 *     the length the database table {@link DatabaseHelper#stationListTable} is greater than 2, then it turns their respective icons
 *     green (or the color specified by {@link #colorGreen}) otherwise the icons will be red (or the color specified by {@link #colorRed}).
 *     It also populates the MenuItems Change Lat/Lon format and About Us according to the parameters passed to it.
 * </p>
 */
public abstract class ActionBarActivity extends Activity {

    private static final String TAG = "ActionBarActivity";

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
     * The Icons to show on the Action Bar on the screen.
     */
    private MenuItem gpsIconItem, aisIconItem, gridSetupIconItem;

    /**
     * Time in seconds which specify the update interval of the icons on the Action Bar
     */
    public static final int UPDATE_TIME = 2 * 1000;

    /**
     * Defines the color for the icons when their status is true. For example when GPS Fix is available, it icon {@link #gpsIconItem}
     * will turn to this color.
     */
    public static final String colorGreen = "#00bfa5";

    /**
     * Defines the color for the icons when their status is not true. For example when GPS Fix is not available, it icon
     * {@link #gpsIconItem} will turn to this color.
     */
    public static final String colorRed = "#d32f2f";

    /**
     * The current UTC time in milliseconds as read from the GPS fix available via {@link GPS_Service}
     */
    private long gpsTime;

    /**
     * The difference in milliseconds between the current time of the tablet and {@link #gpsTime}
     */
    public long timeDiff;

    /**
     * Default {@link Activity#onCreate(Bundle)}. It registers a {@link Runnable} with the {@link Handler} {@link #statusHandler} which
     * runs at a regular interval specified by {@link #UPDATE_TIME} and it checks the booleans {@link #locationStatus} and
     * {@link #packetStatus} and changes the Action Bar icons for GPS and AIS Connectivity accrodingly.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Runnable gpsLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (locationStatus){
                    if (gpsIconItem != null)
                        gpsIconItem.getIcon().setColorFilter(Color.parseColor(colorGreen), PorterDuff.Mode.SRC_IN);
                }
                else {
                    if (gpsIconItem != null)
                        gpsIconItem.getIcon().setColorFilter(Color.parseColor(colorRed), PorterDuff.Mode.SRC_IN);
                }
                if (packetStatus){
                    if (aisIconItem != null)
                        aisIconItem.getIcon().setColorFilter(Color.parseColor(colorGreen), PorterDuff.Mode.SRC_IN);
                }else {
                    if (aisIconItem != null)
                        aisIconItem.getIcon().setColorFilter(Color.parseColor(colorRed), PorterDuff.Mode.SRC_IN);
                }

                statusHandler.postDelayed(this, UPDATE_TIME);
            }
        };

        statusHandler.postDelayed(gpsLocationRunnable, UPDATE_TIME);
    }

    /**
     * Creates the Action Bar icons on top of the screen. This method is overloaded with {@link #onCreateOptionsMenu(Menu)}.
     * This method shows the GPS icon, AIS Connectivity icon and the Grid Setup icon on Action Bar and depending
     * on the value passed to it, the method will either show the Change Lat/Lon format or About Us menu item.
     * menu item as well
     * @param showExtraParams if this is 1 it will show the Change Lat/Long format Menu Item. If it 2 it will show the About Us Menu Item.
     */
    public boolean onCreateOptionsMenu(Menu menu, int showExtraParams) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        int[] iconItems = {R.id.gridSetupComplete, R.id.currentLocationAvail, R.id.aisPacketAvail};
        gridSetupIconItem = menu.findItem(iconItems[0]);
        gridSetupIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        gpsIconItem = menu.findItem(iconItems[1]);
        gpsIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        aisIconItem = menu.findItem(iconItems[2]);
        aisIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);

        if(MainActivity.numOfBaseStations >= DatabaseHelper.INITIALIZATION_SIZE) {
            if (gridSetupIconItem != null) {
                gridSetupIconItem.getIcon().setColorFilter(Color.parseColor(colorGreen), PorterDuff.Mode.SRC_IN);
            }
        } else{
            if (gridSetupIconItem != null) {
                gridSetupIconItem.getIcon().setColorFilter(Color.parseColor(colorRed), PorterDuff.Mode.SRC_IN);
            }
        }
        if (showExtraParams == 1){
            MenuItem latLonFormat = menu.findItem(R.id.changeLatLonFormat);
            latLonFormat.setVisible(true);
        } else if(showExtraParams == 2){
            MenuItem aboutUs = menu.findItem(R.id.aboutUs);
            aboutUs.setVisible(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Creates the Action Bar icons on top of the screen. This method is overloaded with {@link #onCreateOptionsMenu(Menu, int)}.
     * This method shows the GPS icon, AIS Connectivity icon and the Grid Setup icon.
     * @param menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        int[] iconItems = {R.id.gridSetupComplete, R.id.currentLocationAvail, R.id.aisPacketAvail};
        gridSetupIconItem = menu.findItem(iconItems[0]);
        gridSetupIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        gpsIconItem = menu.findItem(iconItems[1]);
        gpsIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        aisIconItem = menu.findItem(iconItems[2]);
        aisIconItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);


        if(MainActivity.numOfBaseStations >= DatabaseHelper.INITIALIZATION_SIZE) {
            if (gridSetupIconItem != null) {
                gridSetupIconItem.getIcon().setColorFilter(Color.parseColor(colorGreen), PorterDuff.Mode.SRC_IN);
            }
        } else{
            if (gridSetupIconItem != null) {
                gridSetupIconItem.getIcon().setColorFilter(Color.parseColor(colorRed), PorterDuff.Mode.SRC_IN);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Registers and implements the {@link BroadcastReceiver}s for the AIS Connectivity and GPS location broadcasts; which are sent from
     * {@link de.awi.floenavigation.network.NetworkMonitor} and {@link GPS_Service} respectively.
     * The GPS Broadcast receiver sets the value of {@link #locationStatus} and {@link #gpsTime} to the values from the {@link GPS_Service}.
     * The AIS Connectivity broadcast receiver sets the boolean {@link #packetStatus}.
     *
     * @see Runnable
     * @see Handler
     * @see BroadcastReceiver
     */
    @Override
    protected void onStart() {
        super.onStart();

        if (broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    locationStatus = intent.getExtras().getBoolean(GPS_Service.locationStatus);
                    Log.d(TAG, "Location Status: " + String.valueOf(locationStatus));
                    gpsTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());
                    timeDiff = System.currentTimeMillis() - gpsTime;
                }
            };

        }

        if (aisPacketBroadcastReceiver == null){
            aisPacketBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    packetStatus = intent.getExtras().getBoolean(GPS_Service.AISPacketStatus);
                    Log.d(TAG, String.valueOf(packetStatus));
                }
            };
        }

        registerReceiver(aisPacketBroadcastReceiver, new IntentFilter(GPS_Service.AISPacketBroadcast));
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));

    }

    /**
     * Called when the Activity is about to be destroyed. It unregisters the AIS and GPS {@link BroadcastReceiver}s.
     */
    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        unregisterReceiver(aisPacketBroadcastReceiver);
        aisPacketBroadcastReceiver = null;
    }


}
