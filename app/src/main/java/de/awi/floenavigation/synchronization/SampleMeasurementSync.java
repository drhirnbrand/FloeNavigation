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

/**
 * Pushes sample and measurement table parameters in the Local Database to the Server.
 * Reads all the parameters of {@link DatabaseHelper#sampleMeasurementTable} from the Database and stores it in {@link HashMap}s.
 * Creates {@link StringRequest}s and inserts it in to a {@link RequestQueue} to push and pull Data from the Server.
 *
 * <p>
 * During the pull operation the implementation is such that it clears the sample and measurement table and pulls only the device list which is
 * required for taking sample and measurement readings.
 * Uses {@link DeviceList} to create a new Object and insert into local Database, the parameters that is pulled from the Server.
 *</p>
 * @see DatabaseHelper#sampleMeasurementTable
 * @see SyncActivity
 * @see DeviceList
 * @see de.awi.floenavigation.synchronization
 */
public class SampleMeasurementSync {
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
    private String pullDeviceListURL = "";


    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    /**
     * Hashtables for storing different parameters of {@link DatabaseHelper#sampleMeasurementTable}
     */
    private HashMap<Integer, String> deviceIDData = new HashMap<>();
    private HashMap<Integer, String> deviceNameData = new HashMap<>();
    private HashMap<Integer, String> deviceShortNameData = new HashMap<>();
    //private HashMap<Integer, String> operationData = new HashMap<>();
    private HashMap<Integer, String> deviceTypeData = new HashMap<>();
    private HashMap<Integer, Double> latitudeData = new HashMap<>();
    private HashMap<Integer, Double> longitudeData = new HashMap<>();
    private HashMap<Integer, Double> xPositionData = new HashMap<>();
    private HashMap<Integer, Double> yPositionData = new HashMap<>();
    private HashMap<Integer, String> updateTimeData = new HashMap<>();
    private HashMap<Integer, String> labelIDData = new HashMap<>();
    private HashMap<Integer, String> commentData = new HashMap<>();
    private HashMap<Integer, String> labelData = new HashMap<>();

    private Cursor sampleCursor = null;
    private SampleMeasurement sampleMeasurement;
    private DeviceList deviceList;
    private ArrayList<SampleMeasurement> sampleArrayList = new ArrayList<>();
    private ArrayList<DeviceList> devicesArrayList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    /**
     * <code>true</code> if all fixed station parameters are pulled from the server and inserted into the local Database
     * Default value is <code>false</code> which is initialized in the constructor
     */
    private boolean dataPullCompleted;

    SampleMeasurementSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }

    /**
     * Reads the {@value DatabaseHelper#sampleMeasurementTable} Table and inserts the data from all the Columns of the
     * {@value DatabaseHelper#sampleMeasurementTable} Table in to their respective {@link HashMap}.
     * @throws SQLiteException In case of error in reading database
     * @see #sampleCursor
     */
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
                    //operationData.put(i, sampleCursor.getString(sampleCursor.getColumnIndexOrThrow(DatabaseHelper.operation)));
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
        }finally {
            if (sampleCursor != null){
                sampleCursor.close();
            }
        }
    }

    /**
     * Creates {@link StringRequest}s as per the size of {@link #labelIDData} data extracted from the local database and inserts all the requests in the {@link RequestQueue}
     * A Stringrequest {@link #request} for pushing the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     */
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
                    //hashMap.put(DatabaseHelper.operation, (operationData.get(index) == null) ? "" : operationData.get(index));
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

    /**
     * Function is used to pull data from internal database to the server
     * A Stringrequest {@link #request} for pulling the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     * On pulling the data from the server internal database tables {@link DatabaseHelper#deviceListTable} are cleared.
     *
     * <p>
     * The server sends the data in .xml format, therefore it has to extract the data based on the tags
     * Inside {@link Response.Listener#onResponse(Object)} it loops through the entire xml file till it reaches the end of document.
     * Based on the {@link XmlPullParser#START_TAG}, {@link XmlPullParser#TEXT}, {@link XmlPullParser#END_TAG} it adds the values received to
     * the corresponding columns of the {@link DatabaseHelper#deviceListTable}
     * Each {@link #deviceList} is added to the {@link #devicesArrayList} which is individually taken and added to the internal database.
     * </p>
     */
    public void onClickDeviceListPullButton(){
        dbHelper = DatabaseHelper.getDbInstance(mContext);
        db = dbHelper.getReadableDatabase();
        db.execSQL("Delete from " + DatabaseHelper.deviceListTable);
        db.execSQL("Delete from " + DatabaseHelper.sampleMeasurementTable);
        //Counter reinitialized
        DatabaseHelper.SAMPLE_ID_COUNTER = 1;
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

    /**
     * Used to initialize {@link #pushURL} and {@link #pullDeviceListURL}
     * @param baseUrl Url set by the administrator, which is stored in the local database
     * @param port port number set by the administrator, which is stored in the local database (default value is 80)
     */
    public void setBaseUrl(String baseUrl, String port){
        pushURL = "http://" + baseUrl + ":" + port + "/SampleMeasurement/pullSamples.php";
        pullDeviceListURL = "http://" + baseUrl + ":" + port + "/SampleMeasurement/pushDevices.php";

    }
}



