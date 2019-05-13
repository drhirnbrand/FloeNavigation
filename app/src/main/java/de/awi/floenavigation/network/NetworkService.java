package de.awi.floenavigation.network;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 *      The service gets called from the {@link de.awi.floenavigation.dashboard.MainActivity#onCreate(Bundle)} function.
 *      It gets called everytime if the network service is not running.
 * </p>
 * <p>
 *     It starts {@link NetworkMonitor} on a separate thread for execution.
 * </p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class NetworkService extends IntentService {

    /**
     * TAG for logging purpose
     */
    private static final String TAG = "NetworkService";


    /**
     * {@link NetworkMonitor} object
     */
    private NetworkMonitor monitor;
    /**
     * Thread to execute {@link NetworkMonitor}
     */
    Thread networkMonitorThread;

    /**
     * Constructor to start the thread
     */
    public NetworkService() {

        super("NetworkService");
        monitor = new NetworkMonitor(this);
        networkMonitorThread = new Thread(monitor);
        networkMonitorThread.start();


    }

    /**
     * onCreate method
     */
    @Override
    public void onCreate(){
        super.onCreate();

    }


    @Override
    protected void onHandleIntent(Intent intent) {

    }

    /**
     * onDestroy method
     */
    @Override
    public void onDestroy(){
        super.onDestroy();

    }




}
