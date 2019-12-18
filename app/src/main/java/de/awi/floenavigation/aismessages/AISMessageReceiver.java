package de.awi.floenavigation.aismessages;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.util.Constants;

/**
 * It runs on a separate thread. It is called from the
 * {@link de.awi.floenavigation.network.NetworkMonitor}
 * when the ping request between the tablet and the AIS Server is successful.
 * It takes care of establishing socket connection with the Wifi Network of the AIS transponder
 */
public class AISMessageReceiver implements Runnable {

    private static final String TAG = "AISMessageReceiver";

    /**
     * Socket timeout in milliseconds. Default for TCP is 20 min. Should probably reduced to detect
     * hanging connections faster.
     */
    private static final int SO_TIMEOUT = 20 * 60 * 1000;

    // Threads cannot be restarted, therefore not final.
    private Thread thread;

    /**
     * This variable stores the ip address of the AIS Transponder to which the tablet is connected
     * over Wifi
     * You can change the value of the variable in
     * {@link Constants#DST_ADDRESS}
     */
    private String destAddress;
    /**
     * This variable stores the port number of the AIS transponder
     * You can change the value of the variable in
     * {@link Constants#DST_PORT}
     */
    private int destPort;

    /**
     * Flag to store the status of client connection
     */
    boolean isConnected = false;
    /**
     * Stores the context of the application
     */
    private Context context;

    /**
     * Broadcast receiver to receive intent packets from
     * {@link de.awi.floenavigation.network.NetworkMonitor}
     */
    private BroadcastReceiver reconnectReceiver;
    /**
     * Flag to stop decoding AIS packets, set from
     * {@link de.awi.floenavigation.synchronization.SyncActivity}
     * Triggered when Synchronization with the server is in progress.
     */
    private static boolean stopDecoding = false;
    private boolean disconnectFlag;

    private enum State {
        INIT,
        OPEN,
        RECEIVING,
        ERROR,
        CLOSED;
    }

    private State state;

    /**
     * Default constructor to initialize broadcast receiver
     * Object created from {@link de.awi.floenavigation.network.NetworkMonitor}
     *
     * @param destAddress Sets the ip address of the AIS transponder
     * @param port        Sets the port number of the AIS transponder
     * @param context     Sets the context of the Activity from which this object is initialized
     */
    private AISMessageReceiver(final String destAddress, final int port, final Context context) {
        this.context = context;
        this.thread = new Thread(this);

        this.destAddress = destAddress;
        this.destPort = port;

        this.disconnectFlag = false;

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    public static AISMessageReceiver newInstance(final String destAddress, final int port,
                                                 final Context context) {
        final AISMessageReceiver instance = new AISMessageReceiver(destAddress, port, context);
        instance.init();
        return instance;
    }

    private void init() {
        reconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getExtras() != null) {
                    Log.d(TAG, String.format("BroadCastReceived '{}' received", intent.getAction(),
                                             intent.getExtras().getBoolean("mDisconnectFlag")));
                    restart();
                    disconnectFlag = intent.getExtras().getBoolean("mDisconnectFlag");
                }
            }
        };
        context.registerReceiver(reconnectReceiver, new IntentFilter("Reconnect"));
        this.state = State.INIT;
        start();
    }


    public synchronized void start() {
        if (this.state != State.INIT) {
            Log.e(TAG, "Cannot be started once past initialization");
        }
        if (thread.isAlive()) {
            return;
        }
        try {
            thread.start();
        } catch (IllegalThreadStateException e) {
            Log.i(TAG, "Thread was probably started and is finished already, make a new one");
            thread = new Thread(this);
            thread.start();
        }
    }

    public synchronized void restart() {
        if (!((state == State.ERROR) || (state == State.CLOSED))) {
            return;
        }

        if (thread.isAlive()) {
            Log.d(TAG, "Receiver thread still alive!");
            return;
        }

        setState(State.INIT);
        Log.i(TAG, "Restarting thread");
        thread = new Thread(this);
        thread.start();
    }

    /**
     * Overides the run method of the runnable, which is continuously executed that helps in
     * setting up the client connection and reading the input stream from the buffered reader and
     * thereby filtering
     * on the basis of AIVDM and AIVDO packets and calling the {@link AISDecodingService} to decode
     * the ASCII packet
     */
    @Override
    public void run() {
        Looper.prepare();
        setState(State.OPEN);

        try (Socket socket = new Socket(destAddress, destPort)) {
            configure(socket);
            isConnected = true;
            setState(State.RECEIVING);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))) {

                String message;
                do {
                    message = reader.readLine();

                    final Intent broadcastIntent = new Intent(GPS_Service.AISPacketBroadcast);
                    broadcastIntent.putExtra(GPS_Service.AISPacketStatus, true);
                    context.sendBroadcast(broadcastIntent);

                    if (!disconnectFlag) {

                        if (message != null) {

                            if (message.contains("AIVDM") || message.toString().contains("AIVDO")) {

                                if (!stopDecoding) {
                                    Intent serviceIntent =
                                            new Intent(context, AISDecodingService.class);
                                    serviceIntent.putExtra("AISPacket", message);
                                    context.startService(serviceIntent);
                                }
                                //                            final AISRawMessage packet =
                                //                            AISRawMessage.newInstance(message);
                                //
                                //                            if (packet.isValid()) {
                                //                                notifyOnline();
                                //
                                //                                Intent serviceIntent =
                                //                                        new Intent(context,
                                //                                        AISDecodingService.class);
                                //                                serviceIntent
                                //                                        .putExtra(Constants
                                //                                        .EXTRA_AIS_PACKET_KEY,
                                //                                        packet.getData());
                                //                                context.startService
                                //                                (serviceIntent);
                                //                            }
                            }
                        }
                    }
                } while (message != null);
            } catch (IOException e) {
                setState(State.ERROR);
                Log.e(TAG, "Connection ended!");
            }

        } catch (UnknownHostException e) {
            Toast.makeText(context, "My AIS transponder was not found in the network",
                           Toast.LENGTH_LONG).show();
            Log.e(TAG, "AIS transponder host not found!", e);
            setState(State.ERROR);
        } catch (IOException e) {
            Toast.makeText(context, "Connection to my AIS transponder has failed",
                           Toast.LENGTH_LONG).show();
            Log.e(TAG, "AIS transponder I/O error", e);
            setState(State.ERROR);
        }

        notifyOffline();

        // Stop the decoding service.
        Intent serviceIntent = new Intent(context, AISDecodingService.class);
        context.stopService(serviceIntent);


            /*Intent serviceIntent = new Intent(context, AISDecodingService.class);
            context.startService(serviceIntent);*/
    }

    private void notifyOnline() {
        Log.i(TAG, "AIS message receiver is online");
        Intent broadcastIntent = new Intent(GPS_Service.AISPacketBroadcast);
        broadcastIntent.putExtra(GPS_Service.AISPacketStatus, true);
        context.sendBroadcast(broadcastIntent);
    }

    private void notifyOffline() {
        Log.i(TAG, "AIS message receiver is offline");
        Intent broadcastIntent = new Intent(GPS_Service.AISPacketBroadcast);
        broadcastIntent.putExtra(GPS_Service.AISPacketStatus, false);
        context.sendBroadcast(broadcastIntent);
        if (state != State.ERROR) {
            state = State.CLOSED;
        }
    }

    private void configure(final Socket socket) {
        try {
            socket.setSoTimeout(SO_TIMEOUT);
        } catch (SocketException e) {
            Log.d(TAG, "Unable to configure socket timeouts");
        }
    }


    /**
     * Function for the setting the value
     *
     * @param stopAISDecoding flag received from
     *                        {@link de.awi.floenavigation.synchronization.SyncActivity}
     *                        to stop the
     *                        decoding when the sync activity is in progress
     */
    public static void setStopDecoding(final boolean stopAISDecoding) {
        stopDecoding = stopAISDecoding;
    }

    public synchronized void setState(final State state) {
        this.state = state;
    }

    public synchronized boolean isConnected() {
        if (state == State.ERROR) {
            return false;
        }
        if (state == State.CLOSED) {
            return false;
        }
        if (state == State.INIT) {
            return false;
        }
        return true;
    }
}
