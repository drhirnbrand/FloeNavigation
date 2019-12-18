package de.awi.floenavigation.network;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.awi.floenavigation.aismessages.AISMessageReceiver;
import de.awi.floenavigation.services.GPS_Service;
import de.awi.floenavigation.util.Constants;

/**
 * {@link NetworkMonitor} runs on a separate thread.
 * It implements a runnable {@link Runnable} method to periodically execute ping request to the ip
 * address {@link Constants#DST_ADDRESS} and on
 * {@link Constants#DST_PORT} number.
 * On Successful ping request, it starts the {@link AISMessageReceiver} on a
 * separate thread, also sets the {@link GPS_Service#AISPacketStatus}
 * to true, which is broadcasted to all the activities or fragments which requests it.
 * On unsuccessful ping request, it sends a disconnect flag set to true to {@link
 * AISMessageReceiver} requesting it to stop the decoding of the AIS packet received.
 */
public class NetworkMonitor implements Runnable {

    /**
     * Context of the activity from where the service is called
     */
    private final Context context;

    /**
     * String for logging purpose
     */
    private static final String TAG = NetworkMonitor.class.getSimpleName();

    private Thread thread;

    /**
     * Initializes the thread.
     *
     * @param context
     */
    public NetworkMonitor(Context context) {
        this.context = context;
    }

    /**
     * run method to continuously send ping request to the {@link Constants#DST_ADDRESS} and
     * {@link Constants#DST_PORT}
     * <p>
     * Separate thread is created on every transition from unsuccessful ping request to a successful
     * ping request
     */
    @Override
    public void run() {
        Log.d(TAG, "Repeatedly pinging the remote host...");

        final AISMessageReceiver aisMessageReceiver =
                AISMessageReceiver.newInstance(Constants.DST_ADDRESS, Constants.DST_PORT, context);

        // TODO: Loop forever?
        while (true) {

            final boolean success = NetworkUtils.isDestinationReachable(Constants.DST_ADDRESS);

            Log.d(TAG,
                  String.format("AIS mobile station/relay reachable: %s", String.valueOf(success)));

            if (success) {
                if (!aisMessageReceiver.isConnected()) {
                    aisMessageReceiver.restart();
                }
                backOff();
            } else {
                Log.d(TAG, String.format("AIS mobile station/relay is not reachable at %s",
                                         Constants.DST_ADDRESS));

                notifyOffline();
                retryTimeout();
            }
        }
    }

    private void notifyOffline() {
        //Broadcast Service for action bar updates
        final Intent broadcastIntent = new Intent(GPS_Service.AISPacketBroadcast);
        broadcastIntent.putExtra(GPS_Service.AISPacketStatus, false);
        context.sendBroadcast(broadcastIntent);
    }

    private void retryTimeout() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void backOff() {
        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted!", e);
        }
    }

    void start() {
        if (thread == null) {
            thread = new Thread(this);
        }
        thread.start();
    }
}
