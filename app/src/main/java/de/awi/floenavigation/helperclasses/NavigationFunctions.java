package de.awi.floenavigation.helperclasses;

import android.content.Context;

import java.text.DecimalFormat;

public class NavigationFunctions {
    private static final String TAG = "Navigation Functions";

    public static double[] calculateNewPosition(double lat, double lon, double speed, double bearing){

        final double r = 6371 * 1000; // Earth Radius in m
        double distance = speed * 10;

        double lat2 = Math.asin(Math.sin(Math.toRadians(lat)) * Math.cos(distance / r)
                + Math.cos(Math.toRadians(lat)) * Math.sin(distance / r) * Math.cos(Math.toRadians(bearing)));
        double lon2 = Math.toRadians(lon)
                + Math.atan2(Math.sin(Math.toRadians(bearing)) * Math.sin(distance / r) * Math.cos(Math.toRadians(lat)), Math.cos(distance / r)
                - Math.sin(Math.toRadians(lat)) * Math.sin(lat2));
        lat2 = Math.toDegrees( lat2);
        lon2 = Math.toDegrees(lon2);
        return new double[]{lat2, lon2};
    }

    public static double calculateDifference(double lat1, double lon1, double lat2, double lon2){

        final int R = 6371; // Radius of the earth change this

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

    public static double[] calculateCoordinatePosition(double lat, double lon, Context context){
        double[] referencePointsCoordinates = new DatabaseHelper(context).readBaseCoordinatePointsLatLon(context);
        /*referencePointsCoordinates[0] = originLatitutde;
        referencePointsCoordinates[1] = originLongitude;
        referencePointsCoordinates[2] = xAxisReferenceLatitude;
        referencePointsCoordinates[3] = xAxisReferenceLongitude;*/
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

    public static double calculateBearing(double lat1, double lon1, double lat2, double lon2){
        double y = Math.sin(Math.toRadians(lon2 - lon1)) * Math.cos(Math.toRadians(lat2));
        double x = (Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))) -
                (Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(lon2 - lon1)));
        return Math.toDegrees(Math.atan2(y,x));
    }

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

    public static double calculateAngleBeta(double lat1, double lon1, double lat2, double lon2){

        //double fixedLat = lat1;
        //double fixedLon = lon2;

        double bearing = calculateBearing(lat1, lon1, lat2, lon2);
        //Log.d(TAG, "Bearing: " + String.valueOf(bearing));

        if(bearing >= 0 && bearing <= 180){
            bearing -= 90;
        }
        else if(bearing > 180 && bearing <= 360){
            bearing -= 270;
        }


        //double hypDistance = calculateDifference(lat1, lon1, lat2, lon2);
        //double leg1Distance = calculateDifference(fixedLat, fixedLon, lat2, lon2);
        //double leg2Distance = calculateDifference(lat1, lon1, fixedLat, fixedLon);


        //double angle = Math.toDegrees(Math.atan(leg1Distance/leg2Distance));

        //double firstangle = Math.atan2(lon1 - fixedLon, lat1 - fixedLat);
        //double secondangle = Math.atan2(lon2 - fixedLon, lat2 - fixedLat);

        return Math.abs(bearing);
    }

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
