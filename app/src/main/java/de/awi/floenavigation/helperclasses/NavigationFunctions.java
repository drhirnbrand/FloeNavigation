package de.awi.floenavigation.helperclasses;

import android.content.Context;

import java.text.DecimalFormat;

/**
 * This Class provides methods to calculate different Navigation parameters used by the App. The methods here are used by the background
 * services and different activities to calculate parameters of the stations. These methods are based on the Haversine formula.
 *
 * @see <a href="https://www.movable-type.co.uk/scripts/latlong.html">Navigation Functions</a>
 */
public class NavigationFunctions {
    private static final String TAG = "Navigation Functions";

    /**
     * Given the current Latitude, Longitude, Speed and Course, this method will calculate and return the expected latitude and longitude
     * in 10 seconds time using the Haversine formula. Depending on the latitude the Earth's radius may need to be adjusted in the method.
     * This method assumes a linear and constant speed.
     * @param lat Current Latitude of the Station/Ship
     * @param lon Current Longitude of the Station/Ship
     * @param speed Current Speed Over Ground of the Station/Ship
     * @param bearing Current Course Over Ground of the Station/Ship
     * @return an array of double containing the expected latitude and longitude in 10 seconds time, with the latitude at 0 index.
     */
    public static double[] calculateNewPosition(double lat, double lon, double speed, double bearing){

        final double r = 6364.348 * 1000; // Earth Radius in m
        double distance = speed * 10 * 0.51444;

        double lat2 = Math.asin(Math.sin(Math.toRadians(lat)) * Math.cos(distance / r)
                + Math.cos(Math.toRadians(lat)) * Math.sin(distance / r) * Math.cos(Math.toRadians(bearing)));
        double lon2 = Math.toRadians(lon)
                + Math.atan2(Math.sin(Math.toRadians(bearing)) * Math.sin(distance / r) * Math.cos(Math.toRadians(lat)), Math.cos(distance / r)
                - Math.sin(Math.toRadians(lat)) * Math.sin(lat2));
        lat2 = Math.toDegrees( lat2);
        lon2 = Math.toDegrees(lon2);
        return new double[]{lat2, lon2};
    }

    /**
     * Given two sets of coordinates this method will calculate the distance between the two points in meters using the Haversine formula.
     * Depending on the latitude the Earth's radius may need to be adjusted in the method.
     * @param lat1 Latitude of the first point
     * @param lon1 Longitude of the first point
     * @param lat2 Latitude of the second point
     * @param lon2 Longitude of the second point
     * @return The distance between the two sets of latitude and longitude in meters.
     */
    public static double calculateDifference(double lat1, double lon1, double lat2, double lon2){

        final double R = 6364.348; // Radius of the earth change this

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        distance = Math.pow(distance, 2);

        return Math.sqrt(distance);
    }

    /*
    public static double[] calculateCoordinatePosition(double lat, double lon, Context context){
        double[] referencePointsCoordinates = new DatabaseHelper(context).readBaseCoordinatePointsLatLon(context);
        //referencePointsCoordinates[0] = originLatitutde;
        //referencePointsCoordinates[1] = originLongitude;
        //referencePointsCoordinates[2] = xAxisReferenceLatitude;
        //referencePointsCoordinates[3] = xAxisReferenceLongitude;
        double distance1 = calculateDifference(referencePointsCoordinates[0], referencePointsCoordinates[1], lat, lon);
        double distance2 = calculateDifference(referencePointsCoordinates[2], referencePointsCoordinates[3], lat, lon);
        double x = ((distance1 * distance1) - (distance2 * distance2) + (DatabaseHelper.station2InitialX * DatabaseHelper.station2InitialX))
                / (2 * DatabaseHelper.station2InitialX);
        double y = 0;
        if(x > 0){
            y = Math.sqrt((distance1 * distance1) - (x * x));
        }
        double xAxisBearing = calculateBearing(referencePointsCoordinates[0], referencePointsCoordinates[1], referencePointsCoordinates[2], referencePointsCoordinates[3]);
        double pointBearing = calculateBearing(referencePointsCoordinates[0], referencePointsCoordinates[1], lat, lon);
        if(pointBearing < xAxisBearing){
            y = -1 * y;
        } else if(xAxisBearing < 90 && pointBearing > 270){
            if((pointBearing - 270) < xAxisBearing){
                y = -1 * y;
            }
        }
        return new double[] {x, y};

    }
    */

    /**
     * Given two sets of points this method will calculate the bearing from the first coordinates to the second coordinates.
     * @param lat1 Latitude of the first point
     * @param lon1 Longitude of the first point
     * @param lat2 Latitude of the second point
     * @param lon2 Longitude of the second point
     * @return The bearing from the first point to the second point in Degrees
     */
    public static double calculateBearing(double lat1, double lon1, double lat2, double lon2){
        double y = Math.sin(Math.toRadians(lon2 - lon1)) * Math.cos(Math.toRadians(lat2));
        double x = (Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))) -
                (Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(lon2 - lon1)));
        return Math.toDegrees(Math.atan2(y,x));
    }

    /**
     * Converts the Geographic coordinate (latitude/longitude) passed to it from Decimal form (Degree.xxx) to Degree Minute Second form.
     * @param decCoord the coordinate to be converted.
     * @return The Converted coordinate in Degree Minutes Seconds as a String.
     */
    public static String convertToDegMinSec(double decCoord){

        String degMinSec;
        DecimalFormat df = new DecimalFormat("#.0000");

        double decCoordinate = Math.abs(decCoord);
        int deg = (int)decCoordinate;
        double temp = (decCoordinate - deg) * 60;
        int min = (int)temp;
        double sec = ((temp - min) * 60);

        degMinSec = String.valueOf(deg) + "°" + String.valueOf(min) + "'" + String.valueOf(df.format(sec))+ "\"";

        return degMinSec;
    }

    /**
     * This method calculates the angle between two points from the longitudinal axis in degrees.
     * @param lat1 Latitude of the first point
     * @param lon1 Longitude of the first point
     * @param lat2 Latitude of the second point
     * @param lon2 Longitude of the second point
     * @return The angle between the two points from the longitudinal axis in degrees.
     */
    public static double calculateAngleBeta(double lat1, double lon1, double lat2, double lon2){

        //double fixedLat = lat1;
        //double fixedLon = lon2;

        //double bearing = calculateBearing(lat1, lon1, lat2, lon2);
        //Log.d(TAG, "Bearing: " + String.valueOf(bearing));

        /*if(bearing >= 0 && bearing <= 180){
            bearing -= 90;
        }
        else if(bearing > 180 && bearing <= 360){
            bearing -= 270;
        }*/
        double bearing;
        double deltaY = lat2 - lat1;
        double deltaX = lon2 - lon1;
        bearing = Math.toDegrees(Math.atan2(deltaY, deltaX));
        bearing = (bearing + 360) % 360;
        //bearing = 360 - bearing;


        //double hypDistance = calculateDifference(lat1, lon1, lat2, lon2);
        //double leg1Distance = calculateDifference(fixedLat, fixedLon, lat2, lon2);
        //double leg2Distance = calculateDifference(lat1, lon1, fixedLat, fixedLon);


        //double angle = Math.toDegrees(Math.atan(leg1Distance/leg2Distance));

        //double firstangle = Math.atan2(lon1 - fixedLon, lat1 - fixedLat);
        //double secondangle = Math.atan2(lon2 - fixedLon, lat2 - fixedLat);

        //return Math.abs(bearing);
        return bearing;
    }

    /**
     * This method formats a given set of coordinates in decimal form (Degree.xxx) to Degree Minute Second along with Direction.
     * @param latitude Latitude in Decimal form
     * @param longitude Longitude in Decimal
     * @return The formatted coordinates in a String Array with latitude first and longitude second.
     */
    public static String[] locationInDegrees(double latitude, double longitude){

        int MAX_SIZE = 2;
        String[] coordinatesInDegree = new String[MAX_SIZE];
        String latDirection = "N";
        String lonDirection = "E";

        if(latitude < 0){
            latDirection = "S";
        }
        if(longitude < 0){
            lonDirection = "W";
        }

        String latitudeInDeg = convertToDegMinSec(latitude) + latDirection;
        String longitudeInDeg = convertToDegMinSec(longitude) + lonDirection;

        coordinatesInDegree[0] = latitudeInDeg;
        coordinatesInDegree[1] = longitudeInDeg;

        return coordinatesInDegree;
    }


}
