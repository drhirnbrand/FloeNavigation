package de.awi.floenavigation.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

public class GPS_Service extends Service {

    private LocationManager locationManager;
    private LocationListener listener;
    private double lat = 0.0;
    private double lon = 0.0;
    private static final String TAG = "GPS_SERVICE";
    public static final String GPSBroadcast = "GPSLocationUpdates";
    public static final String AISPacketBroadcast = "AISPacketUpdates";
    public static final String AISPacketStatus = "AISPacketReceived";
    public static final String latitude = "LATITUDE";
    public static final String longitude = "LONGITUDE";
    public static final String GPSTime = "TIME";
    public static final String locationStatus = "CURRENT_LOCATION_AVAILABLE";
    private static final int updateInterval = 1000;
    LocationUpdates locationUpdates = new LocationUpdates();

    private static GPS_Service instance = null;


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

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(){
        Log.d(TAG, "GPS Service Started");
        instance = this;
        listener =  new Listener(LocationManager.GPS_PROVIDER);
        locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        } catch (SecurityException ex){
            Log.d(TAG, "Fail to Request Location updates");
        } catch (IllegalArgumentException ex){
            Log.d(TAG, "GPS Provider does not exist");
            ex.printStackTrace();
        }
        new Thread(locationUpdates).start();

    }


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

    private class Listener implements LocationListener{

        Location lastLocation;

        public Listener(String provider){
            Log.d(TAG, "LocationListener " + provider);
            lastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location){

                locationUpdates.setLatitude(location.getLatitude());
                locationUpdates.setLongitude(location.getLongitude());
                locationUpdates.setTime(location.getTime());
                locationUpdates.setLocationStatus(true);
               /* Log.d(TAG, "Location: " + String.valueOf(location.getLatitude()) + " " +  String.valueOf(location.getLongitude()));
                Log.d(TAG, "Location Time: " + String.valueOf(new Date(location.getTime())));*/
                /*Date dateTime = new Date(location.getTime());*/
                //Log.d(TAG, "Formatted TIme: " + dateTime.toString());
            /*lastLocation.set(location);
            Intent broadcastIntent = new Intent(GPSBroadcast);
            broadcastIntent.putExtra(latitude, location.getLatitude());
            broadcastIntent.putExtra(longitude, location.getLongitude());
            //Log.d(TAG, "BroadCast sent");
            Log.d(TAG, "Tablet Location: " + String.valueOf(location.getLatitude()) + " " +  String.valueOf(location.getLongitude()));
            //Toast.makeText(getApplicationContext(),"Broadcast Sent", Toast.LENGTH_LONG).show();
            sendBroadcast(broadcastIntent);*/
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


                /*if (lat != 0.0 && lon != 0.0){
                    broadcastIntent.putExtra(locationStatus, true);
                }else
                    broadcastIntent.putExtra(locationStatus, false);*/
                broadcastIntent.putExtra(locationStatus, locStatus);
                //Log.d(TAG, "BroadCast sent");
                Log.d(TAG, "Tablet Location: " + String.valueOf(lat) + " " +  String.valueOf(lon));
                Log.d(TAG, "Tablet Time: " + String.valueOf(time));
                Log.d(TAG, "LocStatus: " + String.valueOf(locStatus));
                //Toast.makeText(getApplicationContext(),"Broadcast Sent", Toast.LENGTH_LONG).show();
                sendBroadcast(broadcastIntent);
                locStatus = false;
            }
        }
    }


}

