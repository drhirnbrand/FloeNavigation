package de.awi.floenavigation.synchronization;

import android.content.Context;
import android.database.Cursor;
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

public class SampleMeasurementSync {
    private static final String TAG = "ConfigurationParamSync";
    private Context mContext;

    private String pushURL = "";
    private String pullDeviceListURL = "";


    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    private HashMap<Integer, String> deviceIDData = new HashMap<>();
    private HashMap<Integer, String> deviceNameData = new HashMap<>();
    private HashMap<Integer, String> deviceShortNameData = new HashMap<>();
    private HashMap<Integer, String> operationData = new HashMap<>();
    private HashMap<Integer, String> deviceTypeData = new HashMap<>();
    private HashMap<Integer, Double> latitudeData = new HashMap<>();
    private HashMap<Integer, Double> longitudeData = new HashMap<>();
    private HashMap<Integer, Double> xPositionData = new HashMap<>();
    private HashMap<Integer, Double> yPositionData = new HashMap<>();
    private HashMap<Integer, String> updateTimeData = new HashMap<>();
    private HashMap<Integer, String> labelIDData = new HashMap<>();
    private HashMap<Integer, String> commentData = new HashMap<>();
    private HashMap<Integer, String> labelData = new HashMap<>();

    private Cursor sampleCursor;
    private SampleMeasurement sampleMeasurement;
    private DeviceList deviceList;
    private ArrayList<SampleMeasurement> sampleArrayList = new ArrayList<>();
    private ArrayList<DeviceList> devicesArrayList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    private boolean dataPullCompleted;

    SampleMeasurementSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }

    public void onClickSampleReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            sampleCursor = db.query(DatabaseHelper.sampleMeasurementTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(sampleCursor.moveToFirst()){
                do{
                    deviceIDData.put(i, sampleCursor.getString(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.deviceID)));
                    deviceNameData.put(i, sampleCursor.getString(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.deviceName)));
                    deviceShortNameData.put(i, sampleCursor.getString(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.deviceShortName)));
                    operationData.put(i, sampleCursor.getString(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.operation)));
                    deviceTypeData.put(i, sampleCursor.getString(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.deviceType)));
                    latitudeData.put(i, sampleCursor.getDouble(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.latitude)));
                    longitudeData.put(i, sampleCursor.getDouble(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.longitude)));
                    xPositionData.put(i, sampleCursor.getDouble(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.xPosition)));
                    yPositionData.put(i, sampleCursor.getDouble(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.yPosition)));
                    updateTimeData.put(i, sampleCursor.getString(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.updateTime)));
                    labelIDData.put(i, sampleCursor.getString(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.labelID)));
                    commentData.put(i, sampleCursor.getString(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.comment)));
                    labelData.put(i, sampleCursor.getString(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.label)));

                    i++;

                }while (sampleCursor.moveToNext());
            }
            sampleCursor.close();
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
    }

    public void onClickSampleSyncButton() {
        for (int i = 0; i < labelIDData.size(); i++) {
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
                    hashMap.put(DatabaseHelper.deviceID, (deviceIDData.get(index) == null) ? "" : deviceIDData.get(index));
                    hashMap.put(DatabaseHelper.deviceName, (deviceNameData.get(index) == null) ? "" : deviceNameData.get(index));
                    hashMap.put(DatabaseHelper.deviceShortName, (deviceShortNameData.get(index) == null) ? "" : deviceShortNameData.get(index));
                    hashMap.put(DatabaseHelper.operation, (operationData.get(index) == null) ? "" : operationData.get(index));
                    hashMap.put(DatabaseHelper.deviceType, (deviceTypeData.get(index) == null) ? "" : deviceTypeData.get(index));
                    hashMap.put(DatabaseHelper.latitude, (latitudeData.get(index) == null) ? "" : latitudeData.get(index).toString());
                    hashMap.put(DatabaseHelper.longitude, (longitudeData.get(index) == null) ? "" : longitudeData.get(index).toString());
                    hashMap.put(DatabaseHelper.xPosition, (xPositionData.get(index) == null) ? "" : xPositionData.get(index).toString());
                    hashMap.put(DatabaseHelper.yPosition, (yPositionData.get(index) == null) ? "" : yPositionData.get(index).toString());
                    hashMap.put(DatabaseHelper.updateTime, (updateTimeData.get(index) == null) ? "" : updateTimeData.get(index));
                    hashMap.put(DatabaseHelper.labelID, (labelIDData.get(index) == null) ? "" : labelIDData.get(index));
                    hashMap.put(DatabaseHelper.comment, (commentData.get(index) == null) ? "" : commentData.get(index));
                    hashMap.put(DatabaseHelper.label, (labelData.get(index) == null) ? "" : labelData.get(index));

                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
    }

    private String formatUpdateTime(double updateTime) {
        Date stationTime = new Date((long) updateTime);
        return stationTime.toString();
    }

    public void onClickDeviceListPullButton(){
        dbHelper = DatabaseHelper.getDbInstance(mContext);
        db = dbHelper.getReadableDatabase();
        db.execSQL("Delete from " + DatabaseHelper.deviceListTable);
        StringRequest pullRequest = new StringRequest(pullDeviceListURL, new Response.Listener<String>() {
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
                                if (tag.equals(DatabaseHelper.deviceListTable)) {
                                    deviceList = new DeviceList(mContext);
                                    devicesArrayList.add(deviceList);
                                }
                                break;

                            case XmlPullParser.TEXT:
                                value = parser.getText();
                                break;

                            case XmlPullParser.END_TAG:

                                switch (tag) {

                                    case DatabaseHelper.deviceID:
                                        deviceList.setDeviceID(value);
                                        break;

                                    case DatabaseHelper.deviceName:
                                        deviceList.setDeviceName(value);
                                        break;

                                    case DatabaseHelper.deviceShortName:
                                        deviceList.setDeviceShortName(value);
                                        break;

                                    case DatabaseHelper.deviceType:
                                        deviceList.setDeviceType(value);
                                        break;

                                }
                                break;
                        }
                        event = parser.next();
                    }
                    for(DeviceList deviceList : devicesArrayList){
                        deviceList.insertDeviceListInDB();
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

    public boolean getDataCompleted(){
        return dataPullCompleted;
    }

    public void setBaseUrl(String baseUrl, String port){
        pushURL = "http://" + baseUrl + ":" + port + "/SampleMeasurement/pullSamples.php";
        pullDeviceListURL = "http://" + baseUrl + ":" + port + "/SampleMeasurement/pushDevices.php";

    }
}



