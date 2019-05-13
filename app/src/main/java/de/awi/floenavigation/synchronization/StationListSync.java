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
 * Pushes AIS Station table parameters in the Local Database to the Server.
 * Reads all the parameters of {@link DatabaseHelper#stationListTable} from the Database and stores it in {@link HashMap}s.
 * Creates {@link StringRequest}s and inserts it in to a {@link RequestQueue} to push and pull Data from the Server.
 * Clears the AIS Station List Table before inserting Data that was pulled from the Server.
 *
 * <p>
 * Uses {@link StationList} to create a new AIS Station and insert into local Database, the parameters that is pulled from the Server.
 *</p>
 * @see DatabaseHelper#stationListTable
 * @see SyncActivity
 * @see StationList
 * @see de.awi.floenavigation.synchronization
 */

public class StationListSync {

    private static final String TAG = "StnListSyncActivity";
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
     * Stores {@link StationList#stationName} of all AIS Stations
     */
    private HashMap<Integer, String> stationNameData = new HashMap<>();

    /**
     * Stores {@link StationList#mmsi} of all AIS Stations
     */
    private HashMap<Integer, Integer> mmsiData = new HashMap<>();

    /**
     * Stores {@link StationList#stationName} of all the AIS Stations that are to be deleted.
     * Reads {@link StationList#mmsi} from {@value DatabaseHelper#stationListDeletedTable}
     */
    private HashMap<Integer, Integer> deletedStationListData = new HashMap<>();
    private Cursor stationListCursor = null;
    private StationList stationList;
    private ArrayList<StationList> stationArrayList = new ArrayList<>();
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
    StationListSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }

    /**
     * Reads the {@value DatabaseHelper#stationListTable} Table and inserts the data from all the Columns of the
     * {@value DatabaseHelper#stationListTable} Table in to their respective {@link HashMap}.
     * @throws SQLiteException In case of error in reading database
     * @see #stationListCursor
     */
    public void onClickStationListReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            stationListCursor = db.query(DatabaseHelper.stationListTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(stationListCursor.moveToFirst()){
                do{
                    stationNameData.put(i, stationListCursor.getString(stationListCursor.getColumnIndexOrThrow(DatabaseHelper.stationName)));
                    mmsiData.put(i, stationListCursor.getInt(stationListCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));

                    i++;

                }while (stationListCursor.moveToNext());
            }
            stationListCursor.close();
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }finally {
            if (stationListCursor != null){
                stationListCursor.close();
            }
        }

    }

    /**
     * Creates {@link StringRequest}s as per the size of {@link #mmsiData} data extracted from the local database and inserts all the requests in the {@link RequestQueue}
     * A Stringrequest {@link #request} for pushing the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     */
    public void onClickStationListSyncButton(){
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

                }
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String,String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.stationName,(stationNameData.get(index) == null)? "" : stationNameData.get(index));
                    hashMap.put(DatabaseHelper.mmsi,(mmsiData.get(index) == null)? "" : mmsiData.get(index).toString());

                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
        sendSLDeleteRequest();
    }

    /**
     * Function is used to pull data from internal database to the server
     * A Stringrequest {@link #request} for pulling the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     * On pulling the data from the server internal database tables {@link DatabaseHelper#stationListTable} and
     * {@link DatabaseHelper#stationListDeletedTable} are cleared.
     *
     * <p>
     * The server sends the data in .xml format, therefore it has to extract the data based on the tags
     * Inside {@link Response.Listener#onResponse(Object)} it loops through the entire xml file till it reaches the end of document.
     * Based on the {@link XmlPullParser#START_TAG}, {@link XmlPullParser#TEXT}, {@link XmlPullParser#END_TAG} it adds the values received to
     * the corresponding columns of the {@link DatabaseHelper#stationListTable}
     * Each {@link #stationList} is added to the {@link #stationArrayList} which is individually taken and added to the internal database.
     * </p>
     */
    public void onClickStationListPullButton(){
        try {
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            db.execSQL("Delete from " + DatabaseHelper.stationListTable);
            db.execSQL("Delete from " + DatabaseHelper.stationListDeletedTable);
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
                                    if (tag.equals(DatabaseHelper.stationListTable)) {
                                        stationList = new StationList(mContext);
                                        stationArrayList.add(stationList);
                                    }
                                    break;

                                case XmlPullParser.TEXT:
                                    value = parser.getText();
                                    break;

                                case XmlPullParser.END_TAG:

                                    switch (tag) {

                                        case DatabaseHelper.stationName:
                                            stationList.setStationName(value);
                                            break;

                                        case DatabaseHelper.mmsi:
                                            stationList.setMmsi(Integer.parseInt(value));
                                            break;


                                    }
                                    break;
                            }
                            event = parser.next();
                        }
                        for (StationList currentStn : stationArrayList) {
                            currentStn.insertStationInDB();
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
        URL = "http://" + baseUrl + ":" + port + "/StationList/pullStations.php";
        pullURL = "http://" + baseUrl + ":" + port + "/StationList/pushStations.php";
        deleteURL = "http://" + baseUrl + ":" + port + "/StationList/deleteStations.php";

    }

    /**
     * Reads the {@value DatabaseHelper#stationListDeletedTable} Table and inserts the data from all the Columns of the
     * {@value DatabaseHelper#stationListDeletedTable} Table in to their respective {@link HashMap}.
     * @throws SQLiteException In case of error in reading database
     * After reading the data, it creates string requests to forward the data to the server
     * This facilitates the server to know which MMSI's should be marked for deletion since these stations are no longer used.
     */
    private void sendSLDeleteRequest(){
        Cursor deletedStationListCursor = null;
        try{
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            deletedStationListCursor = db.query(DatabaseHelper.stationListDeletedTable,
                    null,
                    null,
                    null,
                    null, null, null);
            int i = 0;
            if(deletedStationListCursor.moveToFirst()){
                do{
                    deletedStationListData.put(i, deletedStationListCursor.getInt(deletedStationListCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));
                    Log.d(TAG, "MMSI to be Deleted: " + deletedStationListCursor.getInt(deletedStationListCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));
                    i++;

                }while (deletedStationListCursor.moveToNext());
            }
            deletedStationListCursor.close();
            /*
            if(deletedStationListData.size() == 0){
                requestQueue.add(pullRequest);
            } else{
                numOfDeleteRequests = deletedStationListData.size();
            }*/
        } catch (SQLException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }finally {
            if (deletedStationListCursor != null){
                deletedStationListCursor.close();
            }
        }

        for(int j = 0; j < deletedStationListData.size(); j++){
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
                    hashMap.put(DatabaseHelper.mmsi,(deletedStationListData.get(delIndex) == null)? "" : deletedStationListData.get(delIndex).toString());
                    //Log.d(TAG, "MMSI sent to be Deleted: " + deletedStationListData.get(delIndex) + " Index: " + String.valueOf(delIndex));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }

    }

}
