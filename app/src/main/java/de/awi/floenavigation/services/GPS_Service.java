package de.awi.floenavigation.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

/**
 * A Service is an application component that can perform long-running operations in the background,
 * and it doesn't provide a user interface.
 * <p>
 *     {@link GPS_Service} is used to get GPS location for the tablet using the internal GPS
 *
 *
 * </p>
 */
public class GPS_Service extends Service {

    /**
     * LocationManager is the main class through which your application can access location services on Android.
     * A reference can be obtained from calling the getSystemService() method.
     */
    private LocationManager locationManager;
    /**
     * Used for receiving notifications from the LocationManager when the location has changed.
     * These methods are called if the LocationListener has been registered with the location manager service
     */
    private LocationListener listener;
    private static final String TAG = "GPS_SERVICE";
    /**
     * Intent filter to send location updates
     */
    public static final String GPSBroadcast = "GPSLocationUpdates";
    /**
     * Intent filter to send ais packet receive updates
     */
    public static final String AISPacketBroadcast = "AISPacketUpdates";
    /**
     * The name of the extra data
     */
    public static final String AISPacketStatus = "AISPacketReceived";
    /**
     * The name of the extra data for sending latitude value
     */
    public static final String latitude = "LATITUDE";
    /**
     * The name of the extra data for sending longitude value
     */
    public static final String longitude = "LONGITUDE";
    /**
     * The name of the extra data for sending gps time
     */
    public static final String GPSTime = "TIME";
    /**
     * The name of the extra data for sending location status
     */
    public static final String locationStatus = "CURRENT_LOCATION_AVAILABLE";
    /**
     * Update interval for the runnable
     */
    private static final int updateInterval = 5 * 1000;
    /**
     * Gps listener location update time
     */
    private static final int LOCATION_UPDATE_TIME = 10 * 1000;//30 * 1000;
    /**
     * Gps listener location distance in meters
     */
    private static final int LOCATION_UPDATE_DISTANCE = 1;//5;
    /**
     * Location update runnable to put intent extra
     */
    LocationUpdates locationUpdates = new LocationUpdates();
    /**
     * Location provide
     */
    Location lastLocation;
    /**
     * Last location time used to eliminate the error between current system time and the gps time
     */
    private long mLastLocationTimeMillis;
    /**
     * <code>true</code> GPSFix is available
     * <code>false</code> Otherwise
     */
    private boolean isGPSFix = false;
    /**
     * GPS listener to broadcast location updates
     */
    private GPSListener gpsListener;

    /**
     * Not used
     */
    private static GPS_Service instance = null;


    /**
     * Default constructor
     */
    public GPS_Service() {
        //super(name);
        //Toast.makeText(getApplicationContext(), "startService", Toast.LENGTH_LONG).show();

    }

    public static boolean isInstanceCreated(){
        return instance != null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    /**
     * OnCreate method is used to register location manager {@link #locationManager}
     * Add Gps listener {@link #gpsListener} and {@link #listener} to the location manager
     *
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(){
        Log.d(TAG, "GPS Service Started");
        instance = this;
        listener =  new Listener(LocationManager.GPS_PROVIDER);
        gpsListener = new GPSListener();
        locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(true);
        criteria.setSpeedRequired(true);
        criteria.setCostAllowed(true);
        criteria.setBearingRequired(true);

        //API level 9 and up
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setBearingAccuracy(Criteria.ACCURACY_LOW);
        criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);
        locationManager.getBestProvider(criteria, true);
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        try {
            locationManager.addGpsStatusListener(gpsListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_TIME, LOCATION_UPDATE_DISTANCE, listener);
        } catch (SecurityException ex){
            Log.d(TAG, "Fail to Request Location updates");
        } catch (IllegalArgumentException ex){
            Log.d(TAG, "GPS Provider does not exist");
            ex.printStackTrace();
        }
        new Thread(locationUpdates).start();

    }


    /**
     * onDestroy method to unregister listener {@link #listener}
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(locationManager != null)
        {
            locationManager.removeUpdates(listener);
            //locationManager = null;
        }
        instance = null;
    }

    /**
     * Location Listener activated by Location manager whenever there location is updated
     * Locatoin listener sets and updates values of latitude and longitude
     */
    private class Listener implements LocationListener{



        public Listener(String provider){
            Log.d(TAG, "LocationListener " + provider);
            lastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location){
            if (location == null) return;
            Log.d(TAG, "GPS Status on location Change: " + isGPSFix);
            mLastLocationTimeMillis = SystemClock.elapsedRealtime();
            locationUpdates.setLatitude(location.getLatitude());
            locationUpdates.setLongitude(location.getLongitude());
            locationUpdates.setTime(location.getTime());
            lastLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        }
    }

    /**
     * GPS listener to set/update GPS location status
     * to check whether {@link #isGPSFix} has fix
     */
    private class GPSListener implements GpsStatus.Listener {
        @Override
        public void onGpsStatusChanged(int event){
            switch (event){
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    if(lastLocation != null){
                        isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationTimeMillis) < 2 * LOCATION_UPDATE_TIME;
                    }
                    //Log.d(TAG, "GPS Status: " + isGPSFix);
                    locationUpdates.setLocationStatus(isGPSFix);
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    //Log.d(TAG, "GPS Status: " + isGPSFix);
                    isGPSFix = true;
                    locationUpdates.setLocationStatus(isGPSFix);
                    break;
            }
        }
    }

    /**
     * Runnable thread to send intent broadcast to all the activities and fragments
     * the location updates and the location status
     * Runs every {@link #updateInterval}
     */
    private class LocationUpdates implements Runnable {
        private double lat = 0.0;
        private double lon = 0.0;
        private long time = 0;
        private boolean locStatus = false;


        public void setLatitude(double lat) {
            this.lat = lat;

        }

        public void setLongitude(double lon) {
            this.lon = lon;
        }

        public void setTime(long gpsTime){
            this.time = gpsTime;
        }

        public void setLocationStatus(boolean status){
            this.locStatus = status;
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(updateInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Thread Interrupted");
                }


                Intent broadcastIntent = new Intent(GPSBroadcast);
                broadcastIntent.putExtra(latitude, lat);
                broadcastIntent.putExtra(longitude, lon);
                broadcastIntent.putExtra(GPSTime, time);

                broadcastIntent.putExtra(locationStatus, locStatus);
                //Log.d(TAG, "BroadCast sent");
                Log.d(TAG, "Tablet Location: " + String.valueOf(lat) + " " +  String.valueOf(lon));
                Log.d(TAG, "Tablet Time: " + String.valueOf(time));
                Log.d(TAG, "LocStatus: " + String.valueOf(locStatus));
                //Toast.makeText(getApplicationContext(),"Broadcast Sent", Toast.LENGTH_LONG).show();
                sendBroadcast(broadcastIntent);
                //locStatus = false;
            }
        }
    }


}

