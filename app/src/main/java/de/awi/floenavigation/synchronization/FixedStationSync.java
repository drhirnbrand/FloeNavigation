package de.awi.floenavigation.synchronization;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

public class FixedStationSync {

    private static final String TAG = "FixedStnSyncActivity";
    private Context mContext;

    //private static final String URL = "http://192.168.137.1:80/FixedStation/pullStations.php";
    //private static final String pullURL = "http://192.168.137.1:80/FixedStation/pushStations.php";
    //private static final String deleteURL = "http://192.168.137.1:80/FixedStation/deleteStations.php";

    private String URL;
    private String pullURL;
    private String deleteURL;

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    private HashMap<Integer, String> stationNameData = new HashMap<>();
    private HashMap<Integer, Double> latitudeData = new HashMap<>();
    private HashMap<Integer, Double> longitudeData = new HashMap<>();
    private HashMap<Integer, Double> recvdLatitudeData = new HashMap<>();
    private HashMap<Integer, Double> recvdLongitudeData = new HashMap<>();
    private HashMap<Integer, Double> alphaData = new HashMap<>();
    private HashMap<Integer, Double> distanceData = new HashMap<>();
    private HashMap<Integer, Double> xPositionData = new HashMap<>();
    private HashMap<Integer, Double> yPositionData = new HashMap<>();
    private HashMap<Integer, String> stationTypeData = new HashMap<>();
    private HashMap<Integer, String> updateTimeData = new HashMap<>();
    private HashMap<Integer, Double> sogData = new HashMap<>();
    private HashMap<Integer, Double> cogData = new HashMap<>();
    private HashMap<Integer, Integer> packetTypeData = new HashMap<>();
    private HashMap<Integer, Integer> isPredictedData = new HashMap<>();
    private HashMap<Integer, Integer> predictionAccuracyData = new HashMap<>();
    private HashMap<Integer, Integer> isLocationReceivedData = new HashMap<>();
    private HashMap<Integer, Integer> mmsiData = new HashMap<>();


    private HashMap<Integer, Integer> deletedFixedStationData = new HashMap<>();
    private Cursor fixedStationCursor;
    private FixedStation fixedStation;
    private ArrayList<FixedStation> fixedStationList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    //private int numOfDeleteRequests = 0;
    private StringRequest pullRequest;

    private boolean dataPullCompleted;
    FixedStationSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }

    public void onClickFixedStationReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            fixedStationCursor = db.query(DatabaseHelper.fixedStationTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(fixedStationCursor.moveToFirst()){
                do{
                    stationNameData.put(i, fixedStationCursor.getString(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.stationName)));
                    latitudeData.put(i, fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.latitude)));
                    longitudeData.put(i, fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.longitude)));
                    recvdLatitudeData.put(i, fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.recvdLatitude)));
                    recvdLongitudeData.put(i, fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.recvdLongitude)));
                    alphaData.put(i, fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.alpha)));
                    distanceData.put(i, fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.distance)));
                    xPositionData.put(i, fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.xPosition)));
                    yPositionData.put(i, fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.yPosition)));
                    stationTypeData.put(i, fixedStationCursor.getString(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.stationType)));
                    updateTimeData.put(i, fixedStationCursor.getString(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.updateTime)));
                    sogData.put(i, fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.sog)));
                    cogData.put(i, fixedStationCursor.getDouble(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.cog)));
                    packetTypeData.put(i, fixedStationCursor.getInt(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.packetType)));
                    isPredictedData.put(i, fixedStationCursor.getInt(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.isPredicted)));
                    predictionAccuracyData.put(i, fixedStationCursor.getInt(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.predictionAccuracy)));
                    isLocationReceivedData.put(i, fixedStationCursor.getInt(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.isLocationReceived)));
                    mmsiData.put(i, fixedStationCursor.getInt(fixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));

                    i++;

                }while (fixedStationCursor.moveToNext());
            }
            fixedStationCursor.close();
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();

        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }

    }

    private String formatUpdateTime(double updateTime) {
        Date stationTime = new Date((long) updateTime);
        return stationTime.toString();
    }

    public void onClickFixedStationSyncButton(){
        for(int i = 0; i < mmsiData.size(); i++){
            final int index = i;
            request = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.names().get(0).equals("success")) {
                            //Toast.makeText(mContext, "SUCCESS " + jsonObject.getString("success"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "SUCCESS: " + jsonObject.getString("success"));
                        } else {
                            Toast.makeText(mContext, "Error" + jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error: " + jsonObject.getString("error"));
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "Error : " + error.getCause());
                }
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String,String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.stationName,(stationNameData.get(index) == null)? "" : stationNameData.get(index));
                    hashMap.put(DatabaseHelper.latitude,(latitudeData.get(index) == null)? "" : latitudeData.get(index).toString());
                    hashMap.put(DatabaseHelper.longitude,(longitudeData.get(index) == null)? "" : longitudeData.get(index).toString());
                    hashMap.put(DatabaseHelper.recvdLatitude,(recvdLatitudeData.get(index) == null)? "" : recvdLatitudeData.get(index).toString());
                    hashMap.put(DatabaseHelper.recvdLongitude,(recvdLongitudeData.get(index) == null)? "" : recvdLongitudeData.get(index).toString());
                    hashMap.put(DatabaseHelper.alpha,(alphaData.get(index) == null)? "" : alphaData.get(index).toString());
                    hashMap.put(DatabaseHelper.distance,(distanceData.get(index) == null)? "" : distanceData.get(index).toString());
                    hashMap.put(DatabaseHelper.xPosition,(xPositionData.get(index) == null)? "" : xPositionData.get(index).toString());
                    hashMap.put(DatabaseHelper.yPosition,(yPositionData.get(index) == null)? "" : yPositionData.get(index).toString());
                    hashMap.put(DatabaseHelper.stationType,(stationTypeData.get(index) == null)? "" : stationTypeData.get(index));
                    hashMap.put(DatabaseHelper.updateTime,(updateTimeData.get(index) == null)? "" : updateTimeData.get(index));
                    hashMap.put(DatabaseHelper.sog,(sogData.get(index) == null)? "" : sogData.get(index).toString());
                    hashMap.put(DatabaseHelper.cog,(cogData.get(index) == null)? "" : cogData.get(index).toString());
                    hashMap.put(DatabaseHelper.packetType,(packetTypeData.get(index) == null)? "" : packetTypeData.get(index).toString());
                    hashMap.put(DatabaseHelper.isPredicted,(isPredictedData.get(index) == null)? "" : isPredictedData.get(index).toString());
                    hashMap.put(DatabaseHelper.predictionAccuracy,(predictionAccuracyData.get(index) == null)? "" : predictionAccuracyData.get(index).toString());
                    hashMap.put(DatabaseHelper.isLocationReceived,(isLocationReceivedData.get(index) == null)? "" : isLocationReceivedData.get(index).toString());
                    hashMap.put(DatabaseHelper.mmsi,(mmsiData.get(index) == null)? "" : mmsiData.get(index).toString());

                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
        sendFSDeleteRequest();

    }

    public void onClickFixedStationPullButton(){
        try {
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            db.execSQL("Delete from " + DatabaseHelper.fixedStationTable);
            db.execSQL("Delete from " + DatabaseHelper.fixedStationDeletedTable);
            pullRequest = new StringRequest(pullURL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        parser.setInput(new StringReader(response));
                        int event = parser.getEventType();
                        String tag = "";
                        String value = "";
                        while (event != XmlPullParser.END_DOCUMENT) {
                            tag = parser.getName();
                            switch (event) {
                                case XmlPullParser.START_TAG:
                                    if (tag.equals(DatabaseHelper.fixedStationTable)) {
                                        fixedStation = new FixedStation(mContext);
                                        fixedStationList.add(fixedStation);
                                    }
                                    break;

                                case XmlPullParser.TEXT:
                                    value = parser.getText();
                                    break;

                                case XmlPullParser.END_TAG:

                                    switch (tag) {

                                        case DatabaseHelper.stationName:
                                            fixedStation.setStationName(value);
                                            break;

                                        case DatabaseHelper.latitude:
                                            fixedStation.setLatitude(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.longitude:
                                            fixedStation.setLongitude(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.recvdLatitude:
                                            fixedStation.setRecvdLatitude(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.recvdLongitude:
                                            fixedStation.setRecvdLongitude(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.alpha:
                                            fixedStation.setAlpha(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.distance:
                                            fixedStation.setDistance(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.xPosition:
                                            fixedStation.setxPosition(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.yPosition:
                                            fixedStation.setyPosition(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.stationType:
                                            fixedStation.setStationType(value);
                                            break;

                                        case DatabaseHelper.updateTime:
                                            fixedStation.setUpdateTime(value);
                                            break;

                                        case DatabaseHelper.sog:
                                            fixedStation.setSog(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.cog:
                                            fixedStation.setCog(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.packetType:
                                            fixedStation.setPacketType(Integer.parseInt(value));
                                            break;

                                        case DatabaseHelper.isPredicted:
                                            fixedStation.setIsPredicted(Integer.parseInt(value));
                                            break;

                                        case DatabaseHelper.predictionAccuracy:
                                            fixedStation.setPredictionAccuracy(Integer.parseInt(value));
                                            break;

                                        case DatabaseHelper.isLocationReceived:
                                            fixedStation.setIsLocationReceived(Integer.parseInt(value));
                                            break;

                                        case DatabaseHelper.mmsi:
                                            fixedStation.setMmsi(Integer.parseInt(value));
                                            break;


                                    }
                                    break;
                            }
                            event = parser.next();
                        }
                        for (FixedStation currentStn : fixedStationList) {
                            currentStn.insertFixedStationInDB();
                        }
                        dataPullCompleted = true;
                        Toast.makeText(mContext, "Data Pulled from Server", Toast.LENGTH_SHORT).show();
                    } catch (XmlPullParserException e) {
                        Log.d(TAG, "Error Parsing XML");
                        e.printStackTrace();
                    } catch (IOException e) {
                        Log.d(TAG, "IOException from Parser");
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });
            requestQueue.add(pullRequest);

        } catch (SQLException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }

    }

    public boolean getDataCompleted(){
        return dataPullCompleted;
    }

    public void setBaseUrl(String baseUrl, String port){
        URL = "http://" + baseUrl + ":" + port + "/FixedStation/pullStations.php";
        pullURL = "http://" + baseUrl + ":" + port + "/FixedStation/pushStations.php";
        deleteURL = "http://" + baseUrl + ":" + port + "/FixedStation/deleteStations.php";

    }

    private void sendFSDeleteRequest(){
        try{
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            Cursor deletedFixedStationCursor = db.query(DatabaseHelper.fixedStationDeletedTable,
                    null,
                    null,
                    null,
                    null, null, null);
            int i = 0;
            if(deletedFixedStationCursor.moveToFirst()){
                do{
                    deletedFixedStationData.put(i, deletedFixedStationCursor.getInt(deletedFixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));
                    Log.d(TAG, "MMSI to be Deleted: " + deletedFixedStationCursor.getInt(deletedFixedStationCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));
                    i++;

                }while (deletedFixedStationCursor.moveToNext());
            }
            deletedFixedStationCursor.close();
            /*
            if(deletedFixedStationData.size() == 0){
                requestQueue.add(pullRequest);
            } else{
                numOfDeleteRequests = deletedFixedStationData.size();
            }*/
        } catch (SQLException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }

        for(int j = 0; j < deletedFixedStationData.size(); j++) {
            final int delIndex = j;
            request = new StringRequest(Request.Method.POST, deleteURL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        //Log.d(TAG, "on receive");
                        if (jsonObject.names().get(0).equals("success")) {
                            //Toast.makeText(mContext, "SUCCESS " + jsonObject.getString("success"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "SUCCESS: " + jsonObject.getString("success"));
                        } else {
                            Toast.makeText(mContext, "Error" + jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error: " + jsonObject.getString("error"));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    /*
                    numOfDeleteRequests--;
                    if(numOfDeleteRequests == 0){
                        requestQueue.add(pullRequest);
                    }*/

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.mmsi, (deletedFixedStationData.get(delIndex) == null) ? "" : deletedFixedStationData.get(delIndex).toString());
                    //Log.d(TAG, "MMSI sent to be Deleted: " + deletedFixedStationData.get(delIndex) + " Index: " + String.valueOf(delIndex));
                    return hashMap;
                }
            };
            requestQueue.add(request);
        }
    }

}
