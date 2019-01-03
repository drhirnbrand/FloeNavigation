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

public class ConfigurationParameterSync {
    private static final String TAG = "ConfigurationParamSync";
    private Context mContext;

    private String pushURL = "";
    private String pullURL = "";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    private HashMap<Integer, String> configParameterName = new HashMap<>();
    private HashMap<Integer, String> configParameterValue = new HashMap<>();

    private Cursor configParameterCursor;
    private ConfigurationParameter configurationParameter;
    private ArrayList<ConfigurationParameter> configParamArrayList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    private boolean dataPullCompleted;

    ConfigurationParameterSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }

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
        }
    }

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

    public void setBaseUrl(String baseUrl, String port){
        pushURL = "http://" + baseUrl + ":" + port + "/ConfigurationParameter/pullParameter.php";
        pullURL = "http://" + baseUrl + ":" + port + "/ConfigurationParameter/pushParameter.php";

    }

    public boolean getDataCompleted(){
        return dataPullCompleted;
    }
}

class ConfigurationParameter{

    private static final String TAG = "ConfigurationParameter";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private String parameterName;
    private String parameterValue;
    private Context appContext;
    ContentValues parameter;

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

    private void generateContentValues(){
        parameter = new ContentValues();
        parameter.put(DatabaseHelper.parameterName, this.parameterName);
        parameter.put(DatabaseHelper.parameterValue, this.parameterValue);
    }

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

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String name) {
        this.parameterName = name;
    }
    public String getParameterValue() {
        return parameterValue;
    }

    public void setParameterValue(String value) {
        this.parameterValue = value;
    }

}
