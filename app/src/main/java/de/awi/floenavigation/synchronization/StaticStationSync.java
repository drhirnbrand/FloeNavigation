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
import java.util.HashMap;
import java.util.Map;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

/**
 * Synchronizes all the Static Stations in the Local Database with the Server.
 * Reads all the {@link StaticStation} Data from the Database and stores it in {@link HashMap}s.
 * Creates {@link StringRequest}s and inserts it in to a {@link RequestQueue} to push and pull Data from the Server.
 * Clears the Static Station table before inserting Data that was pulled from the Server.
 * <p>
 * Uses {@link StaticStation} to create a new Static Station Object  and insert it in Database, for each Static Station that is pulled from the Server.
 *</p>
 * @see DatabaseHelper#staticStationListTable
 * @see DatabaseHelper#staticStationDeletedTable
 * @see SyncActivity
 * @see StaticStation
 * @see de.awi.floenavigation.synchronization
 */

public class StaticStationSync {

    private static final String TAG = "StaticStnSyncActivity";
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
     * Stores {@link StaticStation#stationName} of all Static Station
     */
    private HashMap<Integer, String> stationNameData = new HashMap<>();

    /**
     * Stores {@link StaticStation#alpha} of all Static Station
     */
    private HashMap<Integer, Double> alphaData = new HashMap<>();

    /**
     * Stores {@link StaticStation#distance} of all Static Station
     */
    private HashMap<Integer, Double> distanceData = new HashMap<>();

    /**
     * Stores {@link StaticStation#xPosition} of all Static Station
     */
    private HashMap<Integer, Double> xPositionData = new HashMap<>();

    /**
     * Stores {@link StaticStation#yPosition} of all Static Station
     */
    private HashMap<Integer, Double> yPositionData = new HashMap<>();

    /**
     * Stores {@link StaticStation#stationType} of all Static Station
     */
    private HashMap<Integer, String> stationTypeData = new HashMap<>();

    /**
     * Stores {@link StaticStation#stationName} of all the Static Station that are to be deleted.
     * Reads {@link StaticStation#stationName} from {@value DatabaseHelper#staticStationDeletedTable}
     */
    private HashMap<Integer, String> deletedStaticStationData = new HashMap<Integer, String>();
    private Cursor staticStationCursor;
    private StaticStation staticStation;
    private ArrayList<StaticStation> staticStationList = new ArrayList<>();


    private RequestQueue requestQueue;
    private XmlPullParser parser;

    //private int numOfDeleteRequests = 0;
    private StringRequest pullRequest;

    /**
     * <code>true</code> if all Static Stations are pulled from the server and inserted in to the local Database
     */
    private boolean dataPullCompleted;


    StaticStationSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }

    /**
     * Reads the {@value DatabaseHelper#staticStationListTable} Table and inserts the data from all the Columns of the
     * {@value DatabaseHelper#staticStationListTable} Table in to their respective {@link HashMap}.
     *
     */
    public void onClickStaticStationReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            staticStationCursor = db.query(DatabaseHelper.staticStationListTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(staticStationCursor.moveToFirst()){
                do{
                    stationNameData.put(i, staticStationCursor.getString(staticStationCursor.getColumnIndexOrThrow(DatabaseHelper.staticStationName)));
                    alphaData.put(i, staticStationCursor.getDouble(staticStationCursor.getColumnIndexOrThrow(DatabaseHelper.alpha)));
                    distanceData.put(i, staticStationCursor.getDouble(staticStationCursor.getColumnIndexOrThrow(DatabaseHelper.distance)));
                    xPositionData.put(i, staticStationCursor.getDouble(staticStationCursor.getColumnIndexOrThrow(DatabaseHelper.xPosition)));
                    yPositionData.put(i, staticStationCursor.getDouble(staticStationCursor.getColumnIndexOrThrow(DatabaseHelper.yPosition)));
                    stationTypeData.put(i, staticStationCursor.getString(staticStationCursor.getColumnIndexOrThrow(DatabaseHelper.stationType)));
                    i++;

                }while (staticStationCursor.moveToNext());
            }
            staticStationCursor.close();
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();

        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }

    }

    /**
     * Creates {@link StringRequest}s for each Static Station and inserts all the requests in the {@link RequestQueue}
     *
     * {@link @value DatabaseHelper#staticStationListTable} Table in to their respective {@link HashMap}.
     *
     */
    public void onClickStaticStationSyncButton(){
        for(int i = 0; i < stationNameData.size(); i++){
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
                    hashMap.put(DatabaseHelper.staticStationName,(stationNameData.get(index) == null)? "" : stationNameData.get(index));
                    hashMap.put(DatabaseHelper.alpha,(alphaData.get(index) == null)? "" : alphaData.get(index).toString());
                    hashMap.put(DatabaseHelper.distance,(distanceData.get(index) == null)? "" : distanceData.get(index).toString());
                    hashMap.put(DatabaseHelper.xPosition,(xPositionData.get(index) == null)? "" : xPositionData.get(index).toString());
                    hashMap.put(DatabaseHelper.yPosition,(yPositionData.get(index) == null)? "" : yPositionData.get(index).toString());
                    hashMap.put(DatabaseHelper.stationType,(stationTypeData.get(index) == null)? "" : stationTypeData.get(index));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
        sendSSDeleteRequest();
    }

    public void onClickStaticStationPullButton(){
        try {
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            db.execSQL("Delete from " + DatabaseHelper.staticStationListTable);
            db.execSQL("Delete from " + DatabaseHelper.staticStationDeletedTable);
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
                                    if (tag.equals(DatabaseHelper.staticStationListTable)) {
                                        staticStation = new StaticStation(mContext);
                                        staticStationList.add(staticStation);
                                    }
                                    break;

                                case XmlPullParser.TEXT:
                                    value = parser.getText();
                                    break;

                                case XmlPullParser.END_TAG:

                                    switch (tag) {

                                        case DatabaseHelper.staticStationName:
                                            staticStation.setStationName(value);
                                            Log.d(TAG, "Value: " + value);
                                            break;

                                        case DatabaseHelper.alpha:
                                            staticStation.setAlpha(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.distance:
                                            staticStation.setDistance(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.xPosition:
                                            staticStation.setxPosition(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.yPosition:
                                            staticStation.setyPosition(Double.valueOf(value));
                                            break;

                                        case DatabaseHelper.stationType:
                                            staticStation.setStationType(value);
                                            break;

                                    }
                                    break;
                            }
                            event = parser.next();
                        }
                        for (StaticStation currentStn : staticStationList) {
                            currentStn.insertStaticStationInDB();
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

    public void setBaseUrl(String baseUrl, String port){
        URL = "http://" + baseUrl + ":" + port + "/StaticStation/pullStations.php";
        pullURL = "http://" + baseUrl + ":" + port + "/StaticStation/pushStations.php";
        deleteURL = "http://" + baseUrl + ":" + port + "/StaticStation/deleteStations.php";

    }

    public boolean getDataCompleted(){
        return dataPullCompleted;
    }

    private void sendSSDeleteRequest(){
        try {
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            Cursor deletedStaticStationCursor = db.query(DatabaseHelper.staticStationDeletedTable,
                    null,
                    null,
                    null,
                    null, null, null);
            int i = 0;
            if (deletedStaticStationCursor.moveToFirst()) {
                do {
                    deletedStaticStationData.put(i, deletedStaticStationCursor.getString(deletedStaticStationCursor.getColumnIndexOrThrow(DatabaseHelper.staticStationName)));
                    Log.d(TAG, "Static Station to be Deleted: " + deletedStaticStationCursor.getString(deletedStaticStationCursor.getColumnIndexOrThrow(DatabaseHelper.staticStationName)));
                    i++;

                } while (deletedStaticStationCursor.moveToNext());
            }
            deletedStaticStationCursor.close();
            /*
            if(deletedStaticStationData.size() == 0){
                requestQueue.add(pullRequest);
            } else{
                numOfDeleteRequests = deletedStaticStationData.size();
            }*/
        } catch (SQLException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }

        for(int j = 0; j < deletedStaticStationData.size(); j++){
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
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String,String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.staticStationName, (deletedStaticStationData.get(delIndex) == null)? "" : deletedStaticStationData.get(delIndex));
                    //Log.d(TAG, "Static Station sent to be Deleted: " + deletedStaticStationData.get(delIndex) + " Index: " + String.valueOf(delIndex));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
    }

}
