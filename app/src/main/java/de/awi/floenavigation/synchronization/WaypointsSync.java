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

/**
 * Pushes Waypoint table parameters in the Local Database to the Server.
 * Reads all the parameters of {@link DatabaseHelper#waypointsTable} from the Database and stores it in {@link HashMap}s.
 * Creates {@link StringRequest}s and inserts it in to a {@link RequestQueue} to push and pull Data from the Server.
 * Clears the Waypoint Table before inserting Data that was pulled from the Server.
 *
 * <p>
 * Uses {@link Waypoints} to create a new Waypoint and insert into local Database, the parameters that is pulled from the Server.
 *</p>
 * @see DatabaseHelper#waypointsTable
 * @see SyncActivity
 * @see Waypoints
 * @see de.awi.floenavigation.synchronization
 */

public class WaypointsSync {

    private static final String TAG = "WaypointsSyncActivity";
    private Context mContext;

    /**
     * URL to use for Pushing Data to the Server
     * @see #setBaseUrl(String, String)
     */
    private String URL = "";

    /**
     * URL to use for Pulling Data from the Server
     * @see #setBaseUrl(String, String)
     */
    private String pullURL = "";

    /**
     * URL to use for sending Delete Request to the Server
     * @see #setBaseUrl(String, String)
     */
    private String deleteURL = "";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    /**
     * Stores {@link Waypoints#latitude} of all Waypoints
     */
    private HashMap<Integer, Double> latitudeData = new HashMap<>();

    /**
     * Stores {@link Waypoints#longitude} of all Waypoints
     */
    private HashMap<Integer, Double> longitudeData = new HashMap<>();

    /**
     * Stores {@link Waypoints#xPosition} of all Waypoints
     */
    private HashMap<Integer, Double> xPositionData = new HashMap<>();

    /**
     * Stores {@link Waypoints#yPosition} of all Waypoints
     */
    private HashMap<Integer, Double> yPositionData = new HashMap<>();

    /**
     * Stores {@link Waypoints#updateTime} of all Waypoints
     */
    private HashMap<Integer, String> updateTimeData = new HashMap<>();

    /**
     * Stores {@link Waypoints#labelID} of all Waypoints
     */
    private HashMap<Integer, String> labelIDData = new HashMap<>();

    /**
     * Stores {@link Waypoints#label} of all Waypoints
     */
    private HashMap<Integer, String> labelData = new HashMap<>();

    /**
     * Stores {@link Waypoints#label} of all the Waypoints that are to be deleted.
     * Reads {@link Waypoints#label} from {@value DatabaseHelper#waypointDeletedTable}
     */
    private HashMap<Integer, String> deletedWaypointsData = new HashMap<Integer, String>();
    private Cursor waypointsCursor = null;
    private Waypoints waypoints;
    private ArrayList<Waypoints> waypointsList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    /**
     * <code>true</code> if all Static Stations are pulled from the server and inserted in to the local Database
     */
    private boolean dataPullCompleted;

    //private int numOfDeleteRequests = 0;
    private StringRequest pullRequest;

    /**
     * Default Constructor.
     * @param context Used to create a {@link DatabaseHelper} object.
     */
    WaypointsSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }

    /**
     * Reads the {@value DatabaseHelper#waypointsTable} Table and inserts the data from all the Columns of the
     * {@value DatabaseHelper#waypointDeletedTable} Table in to their respective {@link HashMap}.
     * @throws SQLiteException In case of error in reading database
     * @see #waypointsCursor
     */
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
        }finally {
            if (waypointsCursor != null){
                waypointsCursor.close();
            }
        }

    }

    private String formatUpdateTime(double updateTime) {
        Date stationTime = new Date((long) updateTime);
        return stationTime.toString();
    }

    /**
     * Creates {@link StringRequest}s as per the size of {@link #labelIDData} data extracted from the local database and inserts all the requests in the {@link RequestQueue}
     * A Stringrequest {@link #request} for pushing the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     */
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

    /**
     * Function is used to pull data from internal database to the server
     * A Stringrequest {@link #request} for pulling the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     * On pulling the data from the server internal database tables {@link DatabaseHelper#waypointsTable} and
     * {@link DatabaseHelper#waypointDeletedTable} are cleared.
     *
     * <p>
     * The server sends the data in .xml format, therefore it has to extract the data based on the tags
     * Inside {@link Response.Listener#onResponse(Object)} it loops through the entire xml file till it reaches the end of document.
     * Based on the {@link XmlPullParser#START_TAG}, {@link XmlPullParser#TEXT}, {@link XmlPullParser#END_TAG} it adds the values received to
     * the corresponding columns of the {@link DatabaseHelper#waypointsTable}
     * Each {@link #waypoints} is added to the {@link #waypointsList} which is individually taken and added to the internal database.
     * </p>
     */
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

    /**
     * Used to initialize {@link #URL}, {@link #pullURL} and {@link #deleteURL}
     * @param baseUrl Url set by the administrator, which is stored in the local database
     * @param port port number set by the administrator, which is stored in the local database (default value is 80)
     */
    public void setBaseUrl(String baseUrl, String port){
        URL = "http://" + baseUrl + ":" + port + "/Waypoint/pullWaypoints.php";
        pullURL = "http://" + baseUrl + ":" + port + "/Waypoint/pushWaypoints.php";
        deleteURL = "http://" + baseUrl + ":" + port + "/Waypoint/deleteWaypoints.php";

    }

    /**
     * Reads the {@value DatabaseHelper#waypointDeletedTable} Table and inserts the data from all the Columns of the
     * {@value DatabaseHelper#waypointDeletedTable} Table in to their respective {@link HashMap}.
     * @throws SQLiteException In case of error in reading database
     * After reading the data, it creates string requests to forward the data to the server
     * This facilitates the server to know which LabelIDs should be marked for deletion since these Waypoint are no longer used.
     */
    private void sendWaypointDeleteRequest(){
        Cursor deletedWaypointsCursor = null;
        try{
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            deletedWaypointsCursor = db.query(DatabaseHelper.waypointDeletedTable,
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
        }finally {
            if (deletedWaypointsCursor != null){
                deletedWaypointsCursor.close();
            }
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
