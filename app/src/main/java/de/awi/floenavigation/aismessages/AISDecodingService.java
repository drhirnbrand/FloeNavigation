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
import de.awi.floenavigation.initialsetup.CoordinateFragment;
import de.awi.floenavigation.services.GPS_Service;

import static de.awi.floenavigation.aismessages.AIVDM.strbuildtodec;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * This class is used to handle decoding of AIS messages received from the {@link AISMessageReceiver}
 * and on the basis of AIS Message types the packet data is segregated and stored in the internal local database
 *
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class AISDecodingService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String TAG = "AISDecodingService";
    private Handler handler;
    /**
     * packet in the form of String received from the {@link AISMessageReceiver}
     * Initially it is initialized to null value
     */
    private String packet = null;

    /**
     * The received packet is splitted on the basis of comma and stored it in corresponding aivdm/aivdo parameters
     * by the use of AIVDM class object
     */
    private AIVDM aivdmObj;
    /**
     * The payload present in the packet after separated from the packet is decoded and segregated
     * by the class {@link PostnReportClassA} for message types {@link #POSITION_REPORT_CLASSA_TYPE_1}, {@link #POSITION_REPORT_CLASSA_TYPE_2}
     * and {@link #POSITION_REPORT_CLASSA_TYPE_3}
     */
    private PostnReportClassA posObjA;
    /**
     * The payload present in the packet after separated from the packet is decoded and segregated
     * by the class {@link PostnReportClassB} for message type {@link #POSITION_REPORT_CLASSB}
     */
    private PostnReportClassB posObjB;
    /**
     * The payload present in the packet after separated from the packet is decoded and segregated
     * by the class {@link StaticVoyageData} for message type {@link #STATIC_VOYAGE_DATA_CLASSB}
     */
    private StaticVoyageData voyageDataObj;
    /**
     * The payload present in the packet after separated from the packet is decoded and segregated
     * by the class {@link StaticDataReport} for message type {@link #STATIC_DATA_CLASSA}
     */
    private StaticDataReport dataReportObj;

    /**
     * Message type of AIS packet, this corresponds to class A AIS transponders
     * The payload containing this message type contains position related information like lat, lon, sog, cog
     * related to the station fitted with the class A transponder
     * The first 6 bits of the payload are the message type
     */
    public static final int POSITION_REPORT_CLASSA_TYPE_1 = 1;
    /**
     * Message type of AIS packet, this corresponds to class A AIS transponders
     * The payload containing this message type contains position related information like lat, lon, sog, cog
     * related to the station fitted with the class A transponder
     * The first 6 bits of the payload are the message type
     */
    public static final int POSITION_REPORT_CLASSA_TYPE_2 = 2;
    /**
     * Message type of AIS packet, this corresponds to class A AIS transponders
     * The payload containing this message type contains position related information like lat, lon, sog, cog
     * related to the station fitted with the class A transponder
     * The first 6 bits of the payload are the message type
     */
    public static final int POSITION_REPORT_CLASSA_TYPE_3 = 3;
    /**
     * Message type of AIS packet, this corresponds to class A AIS transponders
     * The payload containing this message type contains static information such as vessel name, call sign, part number
     * related to the station fitted with the class A transponder
     */
    public static final int STATIC_DATA_CLASSA = 24;
    /**
     * Message type of AIS packet, this corresponds to class B AIS transponders
     * The payload containing this message type contains position related information like lat, lon, sog, cog
     * related to the station fitted with the class B transponder
     * The first 6 bits of the payload are the message type
     */
    public static final int POSITION_REPORT_CLASSB = 18;
    /**
     * Message type of AIS packet, this corresponds to class B AIS transponders
     * The payload containing this message type contains static information such as vessel name, call sign
     * related to the station fitted with the class B transponder
     */
    public static final int STATIC_VOYAGE_DATA_CLASSB = 5;

    /**
     * This is the decoded MMSI number decoded from the payload received
     */
    private long recvdMMSI;
    /**
     * This is the decoded latitudinal value of the station from the payload received
     */
    private double recvdLat;
    /**
     * This is the decoded longitudinal value of the station from the payload received
     */
    private double recvdLon;
    /**
     * This is the decoded speed over ground of the station from the payload received
     */
    private double recvdSpeed;
    /**
     * This is the decoded course over ground of the station from the payload received
     */
    private double recvdCourse;
    /**
     * This is the decoded timestamp from the payload received
     */
    private String recvdTimeStamp;
    /**
     * This is the decoded station name from the payload received
     */
    private String recvdStationName;
    /**
     * Message type is stored in the internal database, so that it can be used during initial setup of the grid.
     * During the initial setup after the MMSI number of the station to be installed is entered, the screen transitions
     * to the coordinate fragment only when the positional data report of the AIS station with the entered MMSI is received.
     * The coordinate fragment realizes that a valid positional packet is decoded by the AISDecodingService when it checks and evaluates the
     * packetType which is here stored.
     * @see de.awi.floenavigation.initialsetup.MMSIFragment
     * @see de.awi.floenavigation.initialsetup.CoordinateFragment
     * @see CoordinateFragment#checkForCoordinates()
     */
    private int packetType;

    /**
     * It is used to receive the broadcasted gps time
     */
    private BroadcastReceiver broadcastReceiver;
    /**
     * Stores the received gps time from the {@link GPS_Service}
     */
    private long gpsTime;
    /**
     * Stores the difference between the system time in milliseconds and the received gps time
     */
    private long timeDiff;

    /**
     * Default Constructor.
     * Used to initialize {@link #aivdmObj}, {@link #posObjA}, {@link #posObjB}, {@link #voyageDataObj}, {@link #dataReportObj}
     */
    public AISDecodingService() {
        super("AISDecodingService");
        aivdmObj = new AIVDM();
        posObjA = new PostnReportClassA(); //1,2,3
        posObjB = new PostnReportClassB(); //18
        voyageDataObj = new StaticVoyageData(); //5
        dataReportObj = new StaticDataReport(); //24
    }

    /**
     * Initializes and registers broadcast receiver and implements on onReceive
     * onReceive calculates the time difference between the system time and the gps time
     * This is required since the gps time is received after certain interval periodically, this helps in synchronizing the
     * time stamp of the received packets with the gps time
     */
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

    /**
     * Broadcast receiver is unregistered in this callback function
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    /**
     * This function splits the received packet on the basis of comma and sends the data to be decoded to the {@link AIVDM} class
     * After the payload is decoded the required parameters are stored into the corrsponding tables of the internal local database
     * If the received mmsi is present in the {@link DatabaseHelper#stationListTable} the decoded payload along with the mmsi is stored in {@link DatabaseHelper#fixedStationTable}
     * else it is stored in {@link DatabaseHelper#mobileStationTable}
     * If there is error in any SQLite Database instructions, the exceptions are handled using try catch exception handlers and appropriate
     * log messages are added.
     * @param intent It is used to extract the packet send as an intent extras
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Cursor mobileCheckCursor = null;
        Cursor cursor_stnlist = null;
        try
        {
            packet = intent.getExtras().getString("AISPacket");
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());;
            SQLiteDatabase db = dbHelper.getReadableDatabase();


            mobileCheckCursor = db.rawQuery("Select DISTINCT tbl_name from sqlite_master where tbl_name = '" + DatabaseHelper.mobileStationTable + "'", null);
            //Log.d(TAG, "MobileStationTable: " + mobileCheckCursor.getCount());


            int msgType = 0;

            if(packet != null) {
                Log.d(TAG, packet);
                String[] dataExtr = packet.split(",");
                aivdmObj.setData(dataExtr);
                StringBuilder binary = aivdmObj.decodePayload();
                msgType = (int) strbuildtodec(0, 5, 6, binary, int.class, false);
                msgDecoding(msgType, binary);
                Log.d(TAG, String.valueOf(recvdMMSI));
            }

            cursor_stnlist = db.query(DatabaseHelper.stationListTable,
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
                    //decodedValues.put(DatabaseHelper.latitude, recvdLat);
                    //decodedValues.put(DatabaseHelper.longitude, recvdLon);
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
                    //decodedValues.put(DatabaseHelper.recvdLatitude, recvdLat);
                    //decodedValues.put(DatabaseHelper.recvdLongitude, recvdLon);
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
            //db.close();
        }catch (SQLException e)
        {
            String text = "Database unavailable";
            e.printStackTrace();
            Log.d(TAG, text);
            //showText(text);
        } finally {
            if (mobileCheckCursor != null){
                //Uncomment later
                mobileCheckCursor.close();
            }
            if(cursor_stnlist != null){

                cursor_stnlist.close();
            }
        }
    }

    /**
     * This function is called from {@link #onHandleIntent(Intent)}
     * Based on the message type corresponding classes are called to decode the payload
     * Once decoded, local variables are initialized with those values
     * @param msgType message type of the AIS message
     * @param binary payload from the packet received in binary format from {@link AIVDM#decodePayload()}
     * @see #recvdLat
     * @see #recvdLon
     * @see #recvdSpeed
     * @see #recvdCourse
     * @see #recvdMMSI
     *
     */
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


}
