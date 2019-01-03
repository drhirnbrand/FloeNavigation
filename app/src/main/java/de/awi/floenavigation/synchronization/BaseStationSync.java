package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
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
 * This class is used for handling synchronization of base station tables in database.
 * Operations done are reading database tables, pushing the data to the server and parsing the data received from the server
 *
 */
public class BaseStationSync {
    private static final String TAG = "BaseStationSync";
    private Context mContext;

    private String pushURL = "";
    private String pullURL = "";
    private String deleteURL = "";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    /**
     * Hashtables for storing base station names, corresponding mmsi's and to check whether
     * the corresponding base station is origin or not
     */
    private HashMap<Integer, String> baseStationName = new HashMap<>();
    private HashMap<Integer, Integer> mmsiData = new HashMap<>();
    private HashMap<Integer, Integer> isOriginData = new HashMap<>();

    /**
     * Cursor is used to loop through the rows of {@link DatabaseHelper#baseStationTable}
     * based on the selection in the Cursor query
     */
    private Cursor baseStationCursor;
    private BaseStation baseStationList;
    private ArrayList<BaseStation> baseStationArrayList = new ArrayList<>();
    /**
     * Hashtable for storing base station deleted mmsi's
     */
    private HashMap<Integer, Integer> deletedBaseStationData = new HashMap<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;
    private boolean dataPullCompleted;

    BaseStationSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }

    /**
     * Function is used to read base station table from internal db
     * It is called from SyncActivity.java file
     * @throws SQLiteException In case of error in reading database
     * @see #baseStationCursor
     * @see #baseStationName
     * @see #mmsiData
     * @see #isOriginData
     */
    public void onClickBaseStationReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            baseStationCursor = db.query(DatabaseHelper.baseStationTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(baseStationCursor.moveToFirst()){
                do{
                    baseStationName.put(i, baseStationCursor.getString(baseStationCursor.getColumnIndexOrThrow(DatabaseHelper.stationName)));
                    mmsiData.put(i, baseStationCursor.getInt(baseStationCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));
                    isOriginData.put(i, baseStationCursor.getInt(baseStationCursor.getColumnIndexOrThrow(DatabaseHelper.isOrigin)));

                    i++;

                }while (baseStationCursor.moveToNext());
            }
            baseStationCursor.close();
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
    }

    /**
     * Function is used to push data from internal database to the server
     * A Stringrequest {@link #request} for pushing the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     */
    public void onClickBaseStationSyncButton() {
        for (int i = 0; i < mmsiData.size(); i++) {
            final int index = i;
            request = new StringRequest(Request.Method.POST, pushURL, new Response.Listener<String>() {

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


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
                /**
                 *
                 * @return The Hashmap with {@link BaseStationSync#baseStationName}, {@link BaseStationSync#mmsiData} and {@link BaseStationSync#isOriginData}
                 * is posted to the server on the {@link BaseStationSync#pushURL}
                 *
                 */
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.stationName, (baseStationName.get(index) == null) ? "" : baseStationName.get(index));
                    hashMap.put(DatabaseHelper.mmsi, (mmsiData.get(index) == null) ? "" : mmsiData.get(index).toString());
                    hashMap.put(DatabaseHelper.isOrigin, (isOriginData.get(index) == null) ? "" : isOriginData.get(index).toString());

                    return hashMap;
                }
            };
            requestQueue.add(request);

        }

        sendBSDeleteRequest();
    }

    /**
     * Function is used to pull data from internal database to the server
     * A Stringrequest {@link #request} for pulling the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     * On pulling the data from the server internal database tables {@link DatabaseHelper#baseStationTable} and {@link DatabaseHelper#baseStationDeletedTable}
     * are cleared.
     * <p>
     * The server sends the data in .xml format, therefore it has to extract the data based on the tags
     * Inside {@link Response.Listener#onResponse(Object)} it loops through the entire xml file till it reaches end of document.
     * Based on the {@link XmlPullParser#START_TAG}, {@link XmlPullParser#TEXT}, {@link XmlPullParser#END_TAG} it adds the values received to
     * the corresponding {@link BaseStation#setStationName(String)}, {@link BaseStation#setMmsi(int)}, {@link BaseStation#setOrigin(int)}
     * Each {@link #baseStationList} is added to the {@link #baseStationArrayList} which is individually taken and added to the internal database.
     * </p>
     */

    public void onClickBaseStationPullButton(){
        dbHelper = DatabaseHelper.getDbInstance(mContext);
        db = dbHelper.getReadableDatabase();
        db.execSQL("Delete from " + DatabaseHelper.baseStationTable);
        db.execSQL("DELETE from " + DatabaseHelper.baseStationDeletedTable);
        StringRequest pullRequest = new StringRequest(pullURL, new Response.Listener<String>() {
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
                                if (tag.equals(DatabaseHelper.baseStationTable)) {
                                    baseStationList = new BaseStation(mContext);
                                    baseStationArrayList.add(baseStationList);
                                }
                                break;

                            case XmlPullParser.TEXT:
                                value = parser.getText();
                                break;

                            case XmlPullParser.END_TAG:

                                switch (tag) {

                                    case DatabaseHelper.stationName:
                                        baseStationList.setStationName(value);
                                        break;

                                    case DatabaseHelper.mmsi:
                                        baseStationList.setMmsi(Integer.parseInt(value));
                                        break;

                                    case DatabaseHelper.isOrigin:
                                        baseStationList.setOrigin(Integer.parseInt(value));


                                }
                                break;
                        }
                        event = parser.next();
                    }
                    for(BaseStation currentStn : baseStationArrayList){
                        currentStn.insertBaseStationInDB();
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

    }

    /**
     *
     * @param baseUrl Server URL configured by the admin
     * @param port    Port number configured by the admin
     * @see de.awi.floenavigation.admin.ParameterViewActivity
     */
    public void setBaseUrl(String baseUrl, String port){
        pushURL = "http://" + baseUrl + ":" + port + "/BaseStation/pullStations.php";
        pullURL = "http://" + baseUrl + ":" + port + "/BaseStation/pushStations.php";
        deleteURL = "http://" + baseUrl + ":" + port + "/BaseStation/deleteStations.php";

    }

    /**
     *
     * @return It returns the flag to check whether the data pull from server is completed or not
     * If completed {@link #dataPullCompleted} returns true.
     */
    public boolean getDataCompleted(){
        return dataPullCompleted;
    }

    /**
     * After the post request is send to the server, this function is called to send {@link DatabaseHelper#baseStationDeletedTable}
     * to the server.
     * Initially the data is extracted from the internal database and then post request is send to the server.
     * A Stringrequest {@link #request} for pushing the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     */

    private void sendBSDeleteRequest(){

        try{
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            Cursor deletedStationListCursor = db.query(DatabaseHelper.baseStationDeletedTable,
                    null,
                    null,
                    null,
                    null, null, null);
            int i = 0;
            if(deletedStationListCursor.moveToFirst()){
                do{
                    deletedBaseStationData.put(i, deletedStationListCursor.getInt(deletedStationListCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));
                    Log.d(TAG, "MMSI to be Deleted: " + deletedStationListCursor.getInt(deletedStationListCursor.getColumnIndexOrThrow(DatabaseHelper.mmsi)));
                    i++;

                }while (deletedStationListCursor.moveToNext());
            }
            deletedStationListCursor.close();

        } catch (SQLException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }

        for(int j = 0; j < deletedBaseStationData.size(); j++){
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

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String,String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.mmsi,(deletedBaseStationData.get(delIndex) == null)? "" : deletedBaseStationData.get(delIndex).toString());
                    //Log.d(TAG, "MMSI sent to be Deleted: " + deletedStationListData.get(delIndex) + " Index: " + String.valueOf(delIndex));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }

    }

}

/**
 * This class is used to store the data received/pulled from main server.
 * Contents are updated based on the corresponding mmsi's if already present in internal db
 * else a new entry is added with all the values.
 */

class BaseStation{

    private static final String TAG = "BaseStation";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private int mmsi;
    private int isOrigin;
    private Context appContext;
    ContentValues baseStation;
    private String stationName;

    public BaseStation(Context context){
            appContext = context;
            try {
            dbHelper = DatabaseHelper.getDbInstance(appContext);
            db = dbHelper.getReadableDatabase();
            } catch(SQLException e){
            Log.d(TAG, "Database Exception");
            e.printStackTrace();
            }
    }

    private void generateContentValues(){
        baseStation = new ContentValues();
        baseStation.put(DatabaseHelper.stationName, this.stationName);
        baseStation.put(DatabaseHelper.isOrigin, this.isOrigin);
        baseStation.put(DatabaseHelper.mmsi, this.mmsi);
    }

    public void insertBaseStationInDB(){
        generateContentValues();
        int result = db.update(DatabaseHelper.baseStationTable, baseStation, DatabaseHelper.mmsi + " = ?", new String[] {String.valueOf(mmsi)});
        if(result == 0){
            db.insert(DatabaseHelper.baseStationTable, null, baseStation);
            Log.d(TAG, "Station Added");
        } else{
            Log.d(TAG, "Station Updated");
        }
    }

    public int getIsOrigin() {
        return isOrigin;
    }

    public void setOrigin(int origin) {
        this.isOrigin = origin;
    }
    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public int getMmsi() {
        return mmsi;
    }

    public void setMmsi(int mmsi) {
        this.mmsi = mmsi;
    }


}
