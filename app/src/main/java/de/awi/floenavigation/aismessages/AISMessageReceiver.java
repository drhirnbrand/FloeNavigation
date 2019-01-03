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

public class AISMessageReceiver implements Runnable {

    private static final String TAG = "AISMessageReceiver";

    private String dstAddress;
    private int dstPort;
    private TelnetClient client;
    private String packet;
    private BufferedReader bufferedReader;
    StringBuilder responseString;
    boolean isConnected = false;
   // private NetworkMonitor monitor;
    private Context context;
    private boolean mDisconnectFlag = false;
    private BroadcastReceiver reconnectReceiver;
    private static boolean stopDecoding = false;

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


    public boolean isConnected(){
        return client.isConnected();
    }

    public static void setStopDecoding(boolean stopAISDecoding){
        stopDecoding = stopAISDecoding;
    }

}
