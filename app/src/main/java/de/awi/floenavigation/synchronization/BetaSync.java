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

public class BetaSync {
    private static final String TAG = "BetaSync";
    private Context mContext;

    private String pushURL = "";
    private String pullURL = "";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    private HashMap<Integer, Double> betaData = new HashMap<>();
    private HashMap<Integer, String> updateTimeData = new HashMap<>();

    private Cursor betaCursor;
    private Beta beta;
    private ArrayList<Beta> betaList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    private boolean dataPullCompleted;

    BetaSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }

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
        }
    }

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

class Beta{

    private static final String TAG = "Beta";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private double beta;
    private String updateTime;
    private Context appContext;
    ContentValues betaValue;

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

    private void generateContentValues(){
        betaValue = new ContentValues();
        betaValue.put(DatabaseHelper.beta, this.beta);
        betaValue.put(DatabaseHelper.updateTime, this.updateTime);
    }

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

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }
    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String value) {
        this.updateTime = value;
    }

}

