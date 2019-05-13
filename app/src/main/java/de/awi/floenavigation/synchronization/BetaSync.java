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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

/**
 * Synchronizes beta value in the Local Database with the Server.
 * Reads all the parameters of {@link DatabaseHelper#betaTable} from the Database and stores it in {@link HashMap}s.
 * Creates {@link StringRequest}s and inserts it in to a {@link RequestQueue} to push and pull Data from the Server.
 * Clears the Beta table before inserting Data that was pulled from the Server.
 * <p>
 * Uses {@link Beta} to create a new Beta Object and insert it in Database, for Beta parameters that is pulled from the Server.
 *</p>
 * @see DatabaseHelper#betaTable
 * @see SyncActivity
 * @see Beta
 * @see de.awi.floenavigation.synchronization
 */

public class BetaSync {
    private static final String TAG = "BetaSync";
    private Context mContext;

    /**
     * URL to use for Pushing Data to the Server
     * @see #setBaseUrl(String, String)
     */
    private String pushURL = "";
    /**
     * URL to use for Pulling Data from the Server
     * @see #setBaseUrl(String, String)
     */
    private String pullURL = "";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    /**
     * Stores {@link Beta#beta} values in HashMaps
     */
    private HashMap<Integer, Double> betaData = new HashMap<>();
    /**
     * Stores {@link Beta#updateTime} values in HashMaps
     */
    private HashMap<Integer, String> updateTimeData = new HashMap<>();

    /**
     * Cursor used to loop through the database entries
     */
    private Cursor betaCursor = null;
    private Beta beta;
    private ArrayList<Beta> betaList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    /**
     * <code>true</code> if all Beta parameters are pulled from the server and inserted into the local Database
     * Default value is <code>false</code> which is initialized in the constructor
     */
    private boolean dataPullCompleted;

    /**
     * Default Constructor.
     * @param context Used to create a {@link DatabaseHelper} object.
     */
    BetaSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }

    /**
     * Reads the {@value DatabaseHelper#betaTable} Table and inserts the data from all the Columns of the
     * {@value DatabaseHelper#betaTable} Table in to their respective {@link HashMap}.
     * @see #betaData
     * @see #updateTimeData
     * @see #betaCursor
     */
    public void onClickBetaReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            betaCursor = db.query(DatabaseHelper.betaTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(betaCursor.moveToFirst()){
                do{
                    betaData.put(i, betaCursor.getDouble(betaCursor.getColumnIndexOrThrow(DatabaseHelper.beta)));
                    updateTimeData.put(i, betaCursor.getString(betaCursor.getColumnIndexOrThrow(DatabaseHelper.updateTime)));
                    i++;

                }while (betaCursor.moveToNext());
            }
            betaCursor.close();
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }finally {
            if (betaCursor != null){
                betaCursor.close();
            }
        }
    }

    /**
     * Creates {@link StringRequest}s as per the rows of beta data extracted from the local database (in our case its only '1') and inserts all the requests in the {@link RequestQueue}
     * {@link DatabaseHelper#betaTable} Table in to their respective {@link HashMap}.
     *
     */
    public void onClickBetaSyncButton() {
        for (int i = 0; i < betaData.size(); i++) {
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
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.beta, (betaData.get(index) == null) ? "" : betaData.get(index).toString());
                    hashMap.put(DatabaseHelper.updateTime, (updateTimeData.get(index) == null) ? "" : updateTimeData.get(index));

                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
    }

    /**
     * Function is used to pull data from internal database to the server
     * A Stringrequest {@link #request} for pulling the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     * On pulling the data from the server internal database tables {@link DatabaseHelper#betaTable} are cleared.
     *
     * <p>
     * The server sends the data in .xml format, therefore it has to extract the data based on the tags
     * Inside {@link Response.Listener#onResponse(Object)} it loops through the entire xml file till it reaches the end of document.
     * Based on the {@link XmlPullParser#START_TAG}, {@link XmlPullParser#TEXT}, {@link XmlPullParser#END_TAG} it adds the values received to
     * the corresponding {@link Beta#setBeta(double)}, {@link Beta#setUpdateTime(String)}
     * Each {@link #beta} is added to the {@link #betaList} which is individually taken and added to the internal database.
     * </p>
     */
    public void onClickBetaPullButton(){
        dbHelper = DatabaseHelper.getDbInstance(mContext);
        db = dbHelper.getReadableDatabase();
        db.execSQL("Delete from " + DatabaseHelper.betaTable);
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
                                if (tag.equals(DatabaseHelper.betaTable)) {
                                    beta = new Beta(mContext);
                                    betaList.add(beta);
                                }
                                break;

                            case XmlPullParser.TEXT:
                                value = parser.getText();
                                break;

                            case XmlPullParser.END_TAG:

                                switch (tag) {

                                    case DatabaseHelper.beta:
                                        beta.setBeta(Double.valueOf(value));
                                        break;

                                    case DatabaseHelper.updateTime:
                                        beta.setUpdateTime(value);
                                        break;
                                }
                                break;
                        }
                        event = parser.next();
                    }
                    for(Beta newBeta : betaList){
                        newBeta.insertBetaInDB();
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

    private String formatUpdateTime(double updateTime) {
        Date stationTime = new Date((long) updateTime);
        return stationTime.toString();
    }

    public void setBaseUrl(String baseUrl, String port){
        pushURL = "http://" + baseUrl + ":" + port + "/Beta/pullBeta.php";
        pullURL = "http://" + baseUrl + ":" + port + "/Beta/pushBeta.php";

    }

    public boolean getDataCompleted(){
        return dataPullCompleted;
    }
}

/**
 * Creates a Beta object with getters and setters for all the parameters of a Beta Table in Database.
 * Used by {@link BetaSync} to create a new Beta Object to be inserted into the Database.
 *
 * @see SyncActivity
 * @see BetaSync
 * @see de.awi.floenavigation.synchronization
 */
class Beta{

    private static final String TAG = "Beta";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    /**
     * Beta value retrieved or to be inserted into database
     */
    private double beta;
    /**
     * updateTime retrieved or to be inserted into database
     */
    private String updateTime;
    private Context appContext;
    /**
     * Local variable. {@link ContentValues} object which will be inserted into the Beta Table.
     */
    ContentValues betaValue;

    /**
     * Default Constructor.
     * @param context Used to create a {@link DatabaseHelper} object.
     */
    public Beta(Context context){
        appContext = context;
        try {
            dbHelper = DatabaseHelper.getDbInstance(appContext);
            db = dbHelper.getReadableDatabase();
        } catch(SQLException e){
            Log.d(TAG, "Database Exception");
            e.printStackTrace();
        }
    }

    /**
     * Inserts the values of the Beta parameters into {@link #betaValue}
     */
    private void generateContentValues(){
        betaValue = new ContentValues();
        betaValue.put(DatabaseHelper.beta, this.beta);
        betaValue.put(DatabaseHelper.updateTime, this.updateTime);
    }

    /**
     * Inserts the Beta created from pulling Data from the Server into the local Database.
     */
    public void insertBetaInDB(){
        generateContentValues();
        int result = db.update(DatabaseHelper.betaTable, betaValue, null, null);
        if(result == 0){
            db.insert(DatabaseHelper.betaTable, null, betaValue);
            Log.d(TAG, "Station Added");
        } else{
            Log.d(TAG, "Station Updated");
        }
    }

    /**
     * Get the Beta value
     * @return {@link #beta}
     */
    public double getBeta() {
        return beta;
    }

    /**
     * Set the Beta value
     * @param beta
     */
    public void setBeta(double beta) {
        this.beta = beta;
    }

    /**
     * Get the time at which beta value is updated
     * @return {@link #updateTime}
     */
    public String getUpdateTime() {
        return updateTime;
    }

    /**
     * Set the update Time
     * @param value
     */

    public void setUpdateTime(String value) {
        this.updateTime = value;
    }

}

