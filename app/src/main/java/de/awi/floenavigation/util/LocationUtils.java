package de.awi.floenavigation.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import java.util.List;

public class LocationUtils {

    /**
     * Intializes the location service and obtains the last known location of the tablet
     *
     * @return the last location
     */
    @SuppressLint("MissingPermission")
    public static Location getLastKnownLocation(Activity context) {
        final LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }

        if (bestLocation == null) {
            Log.e("GetLastKnownLocation", "Tablet location unknown!");
            Location location = new Location("TheLocationUtilities");
            return location;
        }

        return bestLocation;
    }

}
