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
 * Synchronizes configuration paramters (internal parameters) in the Local Database with the Server.
 * Reads all the parameters of {@link DatabaseHelper#configParametersTable} from the Database and stores it in {@link HashMap}s.
 * Creates {@link StringRequest}s and inserts it into a {@link RequestQueue} to push and pull Data from the Server.
 * Clears the {@link DatabaseHelper#configParametersTable} table before inserting Data that was pulled from the Server.
 * <p>
 * Uses {@link ConfigurationParameter} to create a new Object and insert it into local Database, for configurations parameters that is pulled from the Server.
 *</p>
 * @see DatabaseHelper#configParametersTable
 * @see SyncActivity
 * @see ConfigurationParameter
 * @see de.awi.floenavigation.synchronization
 */
public class ConfigurationParameterSync {
    private static final String TAG = "ConfigurationParamSync";
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
     * Stores {@link ConfigurationParameter#parameterName} values in HashMaps
     */
    private HashMap<Integer, String> configParameterName = new HashMap<>();
    /**
     * Stores {@link ConfigurationParameter#parameterValue} values in HashMaps
     */
    private HashMap<Integer, String> configParameterValue = new HashMap<>();
    /**
     * Cursor used to loop through the database entries
     */
    private Cursor configParameterCursor = null;
    private ConfigurationParameter configurationParameter;
    private ArrayList<ConfigurationParameter> configParamArrayList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;
    /**
     * <code>true</code> if all config parameters are pulled from the server and inserted into the local Database
     * Default value is <code>false</code> which is initialized in the constructor
     */
    private boolean dataPullCompleted;
    /**
     * Default Constructor.
     * @param context Used to create a {@link DatabaseHelper} object.
     */
    ConfigurationParameterSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }
    /**
     * Reads the {@value DatabaseHelper#configParametersTable} Table and inserts the data from all the Columns of the
     * {@value DatabaseHelper#configParametersTable} Table in to their respective {@link HashMap}.
     * @throws SQLiteException In case of error in reading database
     */
    public void onClickParameterReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            configParameterCursor = db.query(DatabaseHelper.configParametersTable,
                    null,
                    DatabaseHelper.parameterName + " != ?",
                    new String[]{"TABLET_ID"},
                    null, null, null);
            if(configParameterCursor.moveToFirst()){
                do{
                    configParameterName.put(i, configParameterCursor.getString(configParameterCursor.getColumnIndexOrThrow(DatabaseHelper.parameterName)));
                    configParameterValue.put(i, configParameterCursor.getString(configParameterCursor.getColumnIndexOrThrow(DatabaseHelper.parameterValue)));
                    i++;

                }while (configParameterCursor.moveToNext());
            }
            configParameterCursor.close();
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }finally {
            if (configParameterCursor != null){
                configParameterCursor.close();
            }
        }
    }

    /**
     * Creates {@link StringRequest}s as per the size of {@link #configParameterName} data extracted from the local database and inserts all the requests in the {@link RequestQueue}
     * A Stringrequest {@link #request} for pushing the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     */
    public void onClickParameterSyncButton() {
        for (int i = 0; i < configParameterName.size(); i++) {
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
                    hashMap.put(DatabaseHelper.parameterName, (configParameterName.get(index) == null) ? "" : configParameterName.get(index));
                    hashMap.put(DatabaseHelper.parameterValue, (configParameterValue.get(index) == null) ? "" : configParameterValue.get(index).toString());

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
     * On pulling the data from the server internal database tables {@link DatabaseHelper#configParametersTable} are cleared.
     *
     * <p>
     * The server sends the data in .xml format, therefore it has to extract the data based on the tags
     * Inside {@link Response.Listener#onResponse(Object)} it loops through the entire xml file till it reaches the end of document.
     * Based on the {@link XmlPullParser#START_TAG}, {@link XmlPullParser#TEXT}, {@link XmlPullParser#END_TAG} it adds the values received to
     * the corresponding {@link ConfigurationParameter#setParameterName(String)}, {@link ConfigurationParameter#setParameterValue(String)}
     * Each {@link #configurationParameter} is added to the {@link #configParamArrayList} which is individually taken and added to the internal database.
     * </p>
     */
    public void onClickParameterPullButton(long baseStations){
        dbHelper = DatabaseHelper.getDbInstance(mContext);
        db = dbHelper.getReadableDatabase();
        if(baseStations == 2) {
            db.execSQL("Delete from " + DatabaseHelper.configParametersTable + " Where " + DatabaseHelper.parameterName + " NOT IN ('TABLET_ID', 'SYNC_SERVER_HOSTNAME', 'SYNC_SERVER_PORT')");
        }
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
                                if (tag.equals(DatabaseHelper.configParametersTable)) {
                                    configurationParameter = new ConfigurationParameter(mContext);
                                    configParamArrayList.add(configurationParameter);
                                }
                                break;

                            case XmlPullParser.TEXT:
                                value = parser.getText();
                                break;

                            case XmlPullParser.END_TAG:

                                switch (tag) {

                                    case DatabaseHelper.parameterName:
                                        configurationParameter.setParameterName(value);
                                        break;

                                    case DatabaseHelper.parameterValue:
                                        configurationParameter.setParameterValue(value);
                                        break;
                                }
                                break;
                        }
                        event = parser.next();
                    }
                    for(ConfigurationParameter param : configParamArrayList){
                        param.insertParameterInDB();
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
     * Used to initialize {@link #pushURL} and {@link #pullURL}
     * @param baseUrl Url set by the administrator, which is stored in the local database
     * @param port port number set by the administrator, which is stored in the local database (default value is 80)
     */
    public void setBaseUrl(String baseUrl, String port){
        pushURL = "http://" + baseUrl + ":" + port + "/ConfigurationParameter/pullParameter.php";
        pullURL = "http://" + baseUrl + ":" + port + "/ConfigurationParameter/pushParameter.php";

    }

    public boolean getDataCompleted(){
        return dataPullCompleted;
    }
}

/**
 * Creates a ConfigurationParameter object with getters and setters for all the parameters of a {@link DatabaseHelper#configParametersTable} in Database.
 * Used by {@link ConfigurationParameterSync} to create a new ConfigurationParameter Object to be inserted into the Database.
 *
 * @see SyncActivity
 * @see ConfigurationParameterSync
 * @see de.awi.floenavigation.synchronization
 */
class ConfigurationParameter{

    private static final String TAG = "ConfigurationParameter";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    /**
     * Parameter name retrieved or to be inserted into database
     */
    private String parameterName;
    /**
     * Corresponding parameter value for the parameter name retrieved or to be inserted into database
     */
    private String parameterValue;
    private Context appContext;
    /**
     * Local variable. {@link ContentValues} object which will be inserted into the {@link DatabaseHelper#configParametersTable}.
     */
    ContentValues parameter;

    /**
     * Default Constructor.
     * @param context Used to create a {@link DatabaseHelper} object.
     */
    public ConfigurationParameter(Context context){
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
     * Inserts the values of the config parameters into {@link #parameter}
     */
    private void generateContentValues(){
        parameter = new ContentValues();
        parameter.put(DatabaseHelper.parameterName, this.parameterName);
        parameter.put(DatabaseHelper.parameterValue, this.parameterValue);
    }

    /**
     * Inserts the config parameters {@link #parameter} created from pulling Data from the Server into the local Database.
     */
    public void insertParameterInDB(){
        generateContentValues();
        int result = db.update(DatabaseHelper.configParametersTable, parameter, DatabaseHelper.parameterName + " = ?", new String[] {this.parameterName});
        if(result == 0){
            db.insert(DatabaseHelper.configParametersTable, null, parameter);
            Log.d(TAG, "Station Added");
        } else{
            Log.d(TAG, "Station Updated");
        }
    }

    /**
     * Get the parameter name
     * @return {@link #parameterName}
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Set the parameter name
     * @param name
     */
    public void setParameterName(String name) {
        this.parameterName = name;
    }

    /**
     * Get the parameter value
     * @return {@link #parameterValue}
     */
    public String getParameterValue() {
        return parameterValue;
    }

    /**
     * Set the parameter value
     * @param value
     */
    public void setParameterValue(String value) {
        this.parameterValue = value;
    }

}
