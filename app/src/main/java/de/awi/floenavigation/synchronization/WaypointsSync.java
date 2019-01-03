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

public class WaypointsSync {

    private static final String TAG = "WaypointsSyncActivity";
    private Context mContext;

    private String URL = "";
    private String pullURL = "";
    private String deleteURL = "";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    private HashMap<Integer, Double> latitudeData = new HashMap<>();
    private HashMap<Integer, Double> longitudeData = new HashMap<>();
    private HashMap<Integer, Double> xPositionData = new HashMap<>();
    private HashMap<Integer, Double> yPositionData = new HashMap<>();
    private HashMap<Integer, String> updateTimeData = new HashMap<>();
    private HashMap<Integer, String> labelIDData = new HashMap<>();
    private HashMap<Integer, String> labelData = new HashMap<>();


    private HashMap<Integer, String> deletedWaypointsData = new HashMap<Integer, String>();
    private Cursor waypointsCursor;
    private Waypoints waypoints;
    private ArrayList<Waypoints> waypointsList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    private boolean dataPullCompleted;

    //private int numOfDeleteRequests = 0;
    private StringRequest pullRequest;

    WaypointsSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }

    public void onClickWaypointsReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            waypointsCursor = db.query(DatabaseHelper.waypointsTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(waypointsCursor.moveToFirst()){
                do{
                    latitudeData.put(i, waypointsCursor.getDouble(waypointsCursor.getColumnIndexOrThrow(DatabaseHelper.latitude)));
                    longitudeData.put(i, waypointsCursor.getDouble(waypointsCursor.getColumnIndexOrThrow(DatabaseHelper.longitude)));
                    xPositionData.put(i, waypointsCursor.getDouble(waypointsCursor.getColumnIndexOrThrow(DatabaseHelper.xPosition)));
                    yPositionData.put(i, waypointsCursor.getDouble(waypointsCursor.getColumnIndexOrThrow(DatabaseHelper.yPosition)));
                    updateTimeData.put(i, waypointsCursor.getString(waypointsCursor.getColumnIndexOrThrow(DatabaseHelper.updateTime)));
                    labelIDData.put(i, waypointsCursor.getString(waypointsCursor.getColumnIndexOrThrow(DatabaseHelper.labelID)));
                    labelData.put(i, waypointsCursor.getString(waypointsCursor.getColumnIndexOrThrow(DatabaseHelper.label)));

                    i++;

                }while (waypointsCursor.moveToNext());
            }
            waypointsCursor.close();
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

    public void onClickWaypointsSyncButton(){
        for(int i = 0; i < labelIDData.size(); i++){
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

                }
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String,String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.latitude,(latitudeData.get(index) == null)? "" : latitudeData.get(index).toString());
                    hashMap.put(DatabaseHelper.longitude,(longitudeData.get(index) == null)? "" : longitudeData.get(index).toString());
                    hashMap.put(DatabaseHelper.xPosition,(xPositionData.get(index) == null)? "" : xPositionData.get(index).toString());
                    hashMap.put(DatabaseHelper.yPosition,(yPositionData.get(index) == null)? "" : yPositionData.get(index).toString());
                    hashMap.put(DatabaseHelper.updateTime,(updateTimeData.get(index) == null)? "" : updateTimeData.get(index));
                    hashMap.put(DatabaseHelper.labelID,(labelIDData.get(index) == null)? "" : labelIDData.get(index));
                    hashMap.put(DatabaseHelper.label,(labelData.get(index) == null)? "" : labelData.get(index));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }

        sendWaypointDeleteRequest();


    }

    public void onClickWaypointsPullButton(){
        try {
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            db.execSQL("Delete from " + DatabaseHelper.waypointsTable);
            db.execSQL("Delete from " + DatabaseHelper.waypointDeletedTable);
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
                                    if (tag.equals(DatabaseHelper.waypointsTable)) {
                                        waypoints = new Waypoints(mContext);
                                        waypointsList.add(waypoints);
                                    }
                                    break;

                                case XmlPullParser.TEXT:
                                    value = parser.getText();
                                    break;

                                case XmlPullParser.END_TAG:

                                    switch (tag) {

                                        case DatabaseHelper.latitude:
                                            waypoints.setLatitude(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.longitude:
                                            waypoints.setLongitude(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.xPosition:
                                            waypoints.setxPosition(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.yPosition:
                                            waypoints.setyPosition(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.updateTime:
                                            waypoints.setUpdateTime(value);
                                            break;

                                        case DatabaseHelper.labelID:
                                            waypoints.setLabelID(value);
                                            break;

                                        case DatabaseHelper.label:
                                            waypoints.setLabel(value);
                                            break;


                                    }
                                    break;
                            }
                            event = parser.next();
                        }
                        for (Waypoints currentWaypoint : waypointsList) {
                            currentWaypoint.insertWaypointsInDB();
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
        URL = "http://" + baseUrl + ":" + port + "/Waypoint/pullWaypoints.php";
        pullURL = "http://" + baseUrl + ":" + port + "/Waypoint/pushWaypoints.php";
        deleteURL = "http://" + baseUrl + ":" + port + "/Waypoint/deleteWaypoints.php";

    }

    private void sendWaypointDeleteRequest(){
        try{
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            Cursor deletedWaypointsCursor = db.query(DatabaseHelper.waypointDeletedTable,
                    null,
                    null,
                    null,
                    null, null, null);
            int i = 0;
            if(deletedWaypointsCursor.moveToFirst()){
                do{
                    deletedWaypointsData.put(i, deletedWaypointsCursor.getString(deletedWaypointsCursor.getColumnIndexOrThrow(DatabaseHelper.labelID)));
                    Log.d(TAG, "Waypoint to be Deleted: " + deletedWaypointsCursor.getString(deletedWaypointsCursor.getColumnIndexOrThrow(DatabaseHelper.labelID)));
                    i++;

                }while (deletedWaypointsCursor.moveToNext());
            }
            deletedWaypointsCursor.close();
            /*
            if(deletedWaypointsData.size() == 0){
                requestQueue.add(pullRequest);
            } else{
                numOfDeleteRequests = deletedWaypointsData.size();
            }*/
        } catch (SQLException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }

        for(int j = 0; j < deletedWaypointsData.size(); j++){
            final int delIndex = j;
            request = new StringRequest(Request.Method.POST, deleteURL, new Response.Listener<String>() {

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
                        /*
                        numOfDeleteRequests--;
                        if(numOfDeleteRequests == 0){
                            requestQueue.add(pullRequest);
                        }*/

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String,String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.labelID,(deletedWaypointsData.get(delIndex) == null)? "" : deletedWaypointsData.get(delIndex));
                    //Log.d(TAG, "Waypoint sent to be Deleted: " + deletedWaypointsData.get(delIndex) + " Index: " + String.valueOf(delIndex));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
    }

}
