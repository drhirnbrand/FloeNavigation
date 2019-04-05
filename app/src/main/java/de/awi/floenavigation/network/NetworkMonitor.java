package de.awi.floenavigation.network;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;

import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.aismessages.AISMessageReceiver;
import de.awi.floenavigation.initialsetup.GridSetupActivity;

/**
 * {@link NetworkMonitor} runs on a separate thread.
 * It implements a runnable {@link Runnable} method to periodically execute ping request to the ip address {@link GridSetupActivity#dstAddress} and on
 * {@link GridSetupActivity#dstPort} number.
 * On Successful ping request {@link #success}, it starts the {@link AISMessageReceiver} on a separate thread, also sets the {@link GPS_Service#AISPacketStatus}
 * to true, which is broadcasted to all the activities or fragments which requests it.
 * On unsuccessful ping request, it sends a disconnect flag set to true to {@link AISMessageReceiver} requesting it to stop the decoding of the AIS packet received.
 *
 */
public class NetworkMonitor implements Runnable {
    /**
     * <code>true</code> ping request is successful
     * <code>false</code> otherwise
     */
    boolean success = false;
    /**
     * Context of the activity from where the service is called
     */
    Context appContext;
    /**
     * {@link AISMessageReceiver} object
     */
    AISMessageReceiver aisMessage;
    /**
     * Thread to run {@link AISMessageReceiver}
     */
    Thread aisMessageThread;
    /**
     * String for logging purpose
     */
    private static final String TAG = "NetworkMonitor";

    /**
     * Initializes the thread {@link #aisMessageThread}
     * @param con
     */
    public NetworkMonitor(Context con){
        this.appContext = con;
        aisMessage = new AISMessageReceiver(GridSetupActivity.dstAddress,GridSetupActivity.dstPort, con);
        aisMessageThread = new Thread(aisMessage);

    }

    /**
     * run method to continuously send ping request to the {@link GridSetupActivity#dstAddress} and {@link GridSetupActivity#dstPort}
     * if {@link #success} is true, it starts the {@link AISMessageReceiver} on {@link #aisMessageThread}.
     * Separate thread is created on every transition from unsuccessful ping request to a successful ping request
     */
    public void run(){

        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            success = pingRequest("/system/bin/ping -c 1 " + GridSetupActivity.dstAddress);
            boolean mdisconnectFlag = true;

            Intent intent = new Intent();
            intent.setAction("Reconnect");
            Log.d(TAG, "Success Value: " + String.valueOf(success));
            //Log.d(TAG, "Thread Status: " + String.valueOf(aisMessageThread.isAlive()));
            if(success){
                //Broadcast Service for action bar updates
                Intent broadcastIntent = new Intent(GPS_Service.AISPacketBroadcast);
                broadcastIntent.putExtra(GPS_Service.AISPacketStatus, true);
                appContext.sendBroadcast(broadcastIntent);

                if(!aisMessageThread.isAlive()){
/*                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                    try {
                        aisMessageThread.start();
                        mdisconnectFlag = false;
                        Log.d(TAG, "Connect Flag send");
                        intent.putExtra("mDisconnectFlag", mdisconnectFlag);
                        appContext.sendBroadcast(intent);
                    }catch (IllegalThreadStateException e){
                        Log.d(TAG, "Network Monitor Exception");
                        mdisconnectFlag = true; //to disconnect the client
                        intent.putExtra("mDisconnectFlag", mdisconnectFlag);
                        appContext.sendBroadcast(intent);
                        aisMessage = new AISMessageReceiver(GridSetupActivity.dstAddress,GridSetupActivity.dstPort, appContext);
                        aisMessageThread = new Thread(aisMessage);
                        e.printStackTrace();
                        mdisconnectFlag = true; //to disconnect the client
                        intent.putExtra("mDisconnectFlag", mdisconnectFlag);
                        appContext.sendBroadcast(intent);


                        aisMessage = new AISMessageReceiver(GridSetupActivity.dstAddress,GridSetupActivity.dstPort, appContext);
                        aisMessageThread = new Thread(aisMessage);
                    }
                }
            } else {
                Log.d(TAG, "Ping Failed");
                //Broadcast Service for action bar updates
                Intent broadcastIntent = new Intent(GPS_Service.AISPacketBroadcast);
                broadcastIntent.putExtra(GPS_Service.AISPacketStatus, false);
                appContext.sendBroadcast(broadcastIntent);
                /*try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                if(aisMessageThread.isAlive()) {
                    mdisconnectFlag = true; //to disconnect the client
                    intent.putExtra("mDisconnectFlag", mdisconnectFlag);
                    appContext.sendBroadcast(intent);


                    aisMessage = new AISMessageReceiver(GridSetupActivity.dstAddress,GridSetupActivity.dstPort, appContext);
                    aisMessageThread = new Thread(aisMessage);
                    //aisMessageThread.start();
                    //aisMessageThread.interrupt();
                }
            }


        }
    }

    /**
     * Responsible for ping request
     * @param Instr command
     * @return <code>true</code> if the {@link GridSetupActivity#dstAddress} is reachable
     *         <code>false</code> otherwise
     */
    private boolean pingRequest(String Instr){

        boolean mExitValue = false;


        try {
            mExitValue = InetAddress.getByName(GridSetupActivity.dstAddress).isReachable(1000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mExitValue;
    }

}
