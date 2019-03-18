package de.awi.floenavigation.aismessages;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.StrictMode;
import android.util.Log;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * It runs on a separate thread. It is called from the {@link de.awi.floenavigation.network.NetworkMonitor}
 * when the ping request between the tablet and the AIS Server is successful.
 * It takes care of establishing socket connection with the Wifi Network of the AIS transponder
 */
public class AISMessageReceiver implements Runnable {

    private static final String TAG = "AISMessageReceiver";

    /**
     *This variable stores the ip address of the AIS Transponder to which the tablet is connected over Wifi
     * You can change the value of the variable in {@link de.awi.floenavigation.initialsetup.GridSetupActivity#dstAddress}
     */
    private String dstAddress;
    /**
     *This variable stores the port number of the AIS transponder
     * You can change the value of the variable in {@link de.awi.floenavigation.initialsetup.GridSetupActivity#dstPort}
     */
    private int dstPort;
    /**
     * Used to establish telnet client connection
     */
    private TelnetClient client;
    /**
     * Stores the packet received from the buffered reader on the basis of AIVDM and AIVDO
     */
    private String packet;
    /**
     * To initialize input stream reader
     */
    private BufferedReader bufferedReader;
    /**
     * Used to store the read lines from the buffered reader
     */
    StringBuilder responseString;
    /**
     * Flag to store the status of client connection
     */
    boolean isConnected = false;
    /**
     * Stores the context of the application
     */
    private Context context;
    /**
     * The flag receives its value from {@link de.awi.floenavigation.network.NetworkMonitor}
     * Used to disconnect the client connection when the ping request is unsuccessful with the AIS transponder
     */
    private boolean mDisconnectFlag = false;
    /**
     * Broadcast receiver to receive intent packets from {@link de.awi.floenavigation.network.NetworkMonitor}
     */
    private BroadcastReceiver reconnectReceiver;
    /**
     * Flag to stop decoding AIS packets, set from {@link de.awi.floenavigation.synchronization.SyncActivity}
     * Triggered when Synchronization with the server is in progress.
     */
    private static boolean stopDecoding = false;

    /**
     * Default constructor to initialize broadcast receiver
     * Object created from {@link de.awi.floenavigation.network.NetworkMonitor}
     * @param addr Sets the ip address of the AIS transponder
     * @param port Sets the port number of the AIS transponder
     * @param con Sets the context of the Activity from which this object is initialized
     */
    public AISMessageReceiver(String addr, int port, Context con){
        this.dstAddress = addr;
        this.dstPort = port;
        this.context = con;
        reconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getExtras()!= null) {
                    Log.d(TAG, "BraodCastReceived: " + String.valueOf( intent.getExtras().getBoolean("mDisconnectFlag")));
                    mDisconnectFlag = intent.getExtras().getBoolean("mDisconnectFlag");
                }
            }
        };
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

    }

    /**
     * Overides the run method of the runnable, which is continuously executed that helps in
     * setting up the client connection and reading the input stream from the buffered reader and thereby filtering
     * on the basis of AIVDM and AIVDO packets and calling the {@link AISDecodingService} to decode the ASCII packet
     */
    @Override
    public void run(){
        context.registerReceiver(reconnectReceiver, new IntentFilter("Reconnect"));
        responseString = new StringBuilder();
        try {
            //System.setProperty("http.keepAlive", "false");
            client = new TelnetClient();

            client.connect(dstAddress, dstPort);

            //client.setKeepAlive(false);
            InputStreamReader clientStream = new InputStreamReader(client.getInputStream());
            bufferedReader = new BufferedReader(clientStream);
            /*Intent serviceIntent = new Intent(context, AISDecodingService.class);
            context.startService(serviceIntent);*/

            do{

                if (client != null) {
                    isConnected =  client.isConnected();
                }

                if(mDisconnectFlag){
                    if (client != null) {

                        clientStream.close();
                        bufferedReader.close();
                        client.disconnect();

                        Intent serviceIntent = new Intent(context, AISDecodingService.class);
                        //serviceIntent.putExtra("AISPacket", packet);
                        context.stopService(serviceIntent);
                        client = null;
                        //Log.d(TAG, "DisconnectFlag: " + String.valueOf(client.isConnected()));
                        break;
                    }
                }

                while(bufferedReader.read() != -1) {
                    //Log.d(TAG, "ConnectionStatus: " + String.valueOf(client.isConnected()));
                    //Log.d(TAG, "DisconnectFlag Value: " + String.valueOf(mDisconnectFlag));
                    responseString.append(bufferedReader.readLine());
                    if (responseString.toString().contains("AIVDM") || responseString.toString().contains("AIVDO")) {
                        packet = responseString.toString();
                        //responseString.setLength(0);

                        if(!stopDecoding) {
                            Intent serviceIntent = new Intent(context, AISDecodingService.class);
                            serviceIntent.putExtra("AISPacket", packet);
                            context.startService(serviceIntent);
                        }
                    }
                    responseString.setLength(0);
                    /*Intent intent = new Intent("RECEIVED_PACKET");
                    intent.putExtra("AISPACKET", packet);
                    this.context.sendBroadcast(intent);*/
                    //Log.d(TAG, packet);


                }
            } while (isConnected);


        } catch (IOException e) {
            e.printStackTrace();
            client = null;
        }


    }

    /**
     * Function for the setting the value
     * @param stopAISDecoding flag received from {@link de.awi.floenavigation.synchronization.SyncActivity} to stop the
     *                        decoding when the sync activity is in progress
     */
    public static void setStopDecoding(boolean stopAISDecoding){
        stopDecoding = stopAISDecoding;
    }

}
