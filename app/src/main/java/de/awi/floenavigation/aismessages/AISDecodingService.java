package de.awi.floenavigation.aismessages;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.services.GPS_Service;

import static de.awi.floenavigation.aismessages.AIVDM.strbuildtodec;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class AISDecodingService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String TAG = "AISDecodingService";
    private Handler handler;
    private String packet = null;
    //private Callbacks sCallbacks;
    private AIVDM aivdmObj;
    private PostnReportClassA posObjA;
    private PostnReportClassB posObjB;
    private StaticVoyageData voyageDataObj;
    private StaticDataReport dataReportObj;

    public static final int POSITION_REPORT_CLASSA_TYPE_1 = 1;
    public static final int POSITION_REPORT_CLASSA_TYPE_2 = 2;
    public static final int POSITION_REPORT_CLASSA_TYPE_3 = 3;
    public static final int STATIC_VOYAGE_DATA_CLASSB = 5;
    public static final int POSITION_REPORT_CLASSB = 18;
    public static final int STATIC_DATA_CLASSA = 24;

    //Data to be decoded
    private long recvdMMSI;
    private double recvdLat;
    private double recvdLon;
    private double recvdSpeed;
    private double recvdCourse;
    private String recvdTimeStamp;
    private String recvdStationName;
    private int packetType;
    private BroadcastReceiver wifiReceiver;
    private BroadcastReceiver broadcastReceiver;
    private long gpsTime;
    private long timeDiff;



    public AISDecodingService() {
        super("AISDecodingService");
        aivdmObj = new AIVDM();
        posObjA = new PostnReportClassA(); //1,2,3
        posObjB = new PostnReportClassB(); //18
        voyageDataObj = new StaticVoyageData(); //5
        dataReportObj = new StaticDataReport(); //24

        //System.out.println(sdf.format(date));

    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent){
                    gpsTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());
                    timeDiff = System.currentTimeMillis() - gpsTime;

                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        /*synchronized (this){
            try{
                wait(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }*/
        try
        {
            packet = intent.getExtras().getString("AISPacket");
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());;
            SQLiteDatabase db = dbHelper.getReadableDatabase();


            Cursor mobileCheckCursor = db.rawQuery("Select DISTINCT tbl_name from sqlite_master where tbl_name = '" + DatabaseHelper.mobileStationTable + "'", null);
            //Log.d(TAG, "MobileStationTable: " + mobileCheckCursor.getCount());


            int msgType = 0;

            if(packet != null) {
                Log.d(TAG, packet);
                String[] dataExtr = packet.split(",");
                aivdmObj.setData(dataExtr);
                StringBuilder binary = aivdmObj.decodePayload();
                msgType = (int) strbuildtodec(0, 5, 6, binary, int.class);
                msgDecoding(msgType, binary);
                Log.d(TAG, String.valueOf(recvdMMSI));
            }

            Cursor cursor_stnlist = db.query(DatabaseHelper.stationListTable,
                    new String[] {DatabaseHelper.mmsi, DatabaseHelper.stationName},
                    DatabaseHelper.mmsi + " = ?",
                    new String[] {String.valueOf(recvdMMSI)},
                    null, null, null);
            if(cursor_stnlist.moveToFirst())
            {
                ContentValues decodedValues = new ContentValues();
                if(msgType == STATIC_DATA_CLASSA || msgType == STATIC_VOYAGE_DATA_CLASSB) {
                    decodedValues.put(DatabaseHelper.stationName, recvdStationName);
                }
                decodedValues.put(DatabaseHelper.mmsi, recvdMMSI);
                decodedValues.put(DatabaseHelper.isLocationReceived, 1);
                decodedValues.put(DatabaseHelper.packetType, packetType);
                if ((msgType != STATIC_VOYAGE_DATA_CLASSB) && (msgType != STATIC_DATA_CLASSA)) {
                    decodedValues.put(DatabaseHelper.latitude, recvdLat);
                    decodedValues.put(DatabaseHelper.longitude, recvdLon);
                    decodedValues.put(DatabaseHelper.recvdLatitude, recvdLat);
                    decodedValues.put(DatabaseHelper.recvdLongitude, recvdLon);
                    decodedValues.put(DatabaseHelper.sog, recvdSpeed);
                    decodedValues.put(DatabaseHelper.cog, recvdCourse);
                    decodedValues.put(DatabaseHelper.updateTime, recvdTimeStamp);
                    decodedValues.put(DatabaseHelper.isPredicted, 0);
                }
                Log.d(TAG, "Updated DB " + String.valueOf(recvdMMSI));
                int a = db.update(DatabaseHelper.fixedStationTable, decodedValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(recvdMMSI)});
                //Log.d(TAG, "Update Result: " + recvdTimeStamp);


            } else if (mobileCheckCursor.getCount() == 1){
                ContentValues decodedValues = new ContentValues();
                if(msgType == STATIC_DATA_CLASSA || msgType == STATIC_VOYAGE_DATA_CLASSB) {
                    decodedValues.put(DatabaseHelper.stationName, recvdStationName);
                }
                decodedValues.put(DatabaseHelper.mmsi, recvdMMSI);
                decodedValues.put(DatabaseHelper.packetType, packetType);
                if ((msgType != STATIC_VOYAGE_DATA_CLASSB) && (msgType != STATIC_DATA_CLASSA)) {
                    decodedValues.put(DatabaseHelper.latitude, recvdLat);
                    decodedValues.put(DatabaseHelper.longitude, recvdLon);
                    decodedValues.put(DatabaseHelper.sog, recvdSpeed);
                    decodedValues.put(DatabaseHelper.cog, recvdCourse);
                    decodedValues.put(DatabaseHelper.updateTime, recvdTimeStamp);
                }
                int result = db.update(DatabaseHelper.mobileStationTable, decodedValues, DatabaseHelper.mmsi + " = ?", new String[]{String.valueOf(recvdMMSI)});
                //Log.d(TAG, "Mobile Station Update Result: " + String.valueOf(result));
                //Log.d(TAG, "Mobile Station MMSI: " + String.valueOf(recvdMMSI));

                if(result == 0){
                    long a = db.insert(DatabaseHelper.mobileStationTable, null, decodedValues);
                    //Log.d(TAG, "Mobile Station Insertion Result: " + String.valueOf(a));
                }
                Log.d(TAG, "Mobile Station Table Length: " + String.valueOf(DatabaseUtils.queryNumEntries(db, DatabaseHelper.mobileStationTable)));


            }
            cursor_stnlist.close();
            //cursor_fixedstnlist.close();
            //Uncomment later
            mobileCheckCursor.close();
            //db.close();
        }catch (SQLException e)
        {
            String text = "Database unavailable";
            e.printStackTrace();
            Log.d(TAG, text);
            //showText(text);
        }
    }

    private void msgDecoding(int msgType, StringBuilder binary){


        switch(msgType)
        {
            case POSITION_REPORT_CLASSA_TYPE_1 :
            case POSITION_REPORT_CLASSA_TYPE_2 :
            case POSITION_REPORT_CLASSA_TYPE_3 :
                posObjA.setData(binary);
                recvdMMSI = posObjA.getMMSI();
                recvdLat = posObjA.getLatitude();
                recvdLon = posObjA.getLongitude();
                recvdSpeed = posObjA.getSpeed();
                recvdCourse = posObjA.getCourse();
                //recvdTimeStamp = String.valueOf(SystemClock.elapsedRealtime());//String.valueOf(posObjA.getSeconds());
                recvdTimeStamp = String.valueOf(System.currentTimeMillis() - timeDiff);
                packetType = POSITION_REPORT_CLASSA_TYPE_1;
                break;
            case STATIC_VOYAGE_DATA_CLASSB:
                voyageDataObj.setData(binary);
                recvdMMSI = voyageDataObj.getMMSI();
                recvdStationName = voyageDataObj.getVesselName();
                packetType = STATIC_VOYAGE_DATA_CLASSB;
                break;
            case POSITION_REPORT_CLASSB:
                posObjB.setData(binary);
                recvdMMSI = posObjB.getMMSI();
                recvdLat = posObjB.getLatitude();
                recvdLon = posObjB.getLongitude();
                recvdSpeed = posObjB.getSpeed();
                recvdCourse = posObjB.getCourse();
                //recvdTimeStamp = String.valueOf(SystemClock.elapsedRealtime());//String.valueOf(posObjA.getSeconds());
                recvdTimeStamp = String.valueOf(System.currentTimeMillis() - timeDiff);
                packetType = POSITION_REPORT_CLASSB;
                break;
            case STATIC_DATA_CLASSA:
                dataReportObj.setData(binary);
                recvdMMSI = dataReportObj.getMMSI();
                recvdStationName = dataReportObj.getVesselName();
                packetType = STATIC_DATA_CLASSA;
                break;
            default:
                recvdMMSI = 0;
                recvdLat = 0;
                recvdLon = 0;
                break;

        }


    }

    private void showText(final String text) {

        handler.post(new Runnable(){
            public void run()
            {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });

    }
}
