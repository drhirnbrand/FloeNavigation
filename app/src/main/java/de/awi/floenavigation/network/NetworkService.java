package de.awi.floenavigation.network;

import android.app.IntentService;
import android.content.Intent;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class NetworkService extends IntentService {

    private static final String TAG = "NetworkService";

    private static NetworkService instance = null;


    private NetworkMonitor monitor;
    Thread networkMonitorThread;


    public NetworkService() {

        super("NetworkService");
        monitor = new NetworkMonitor(this);
        networkMonitorThread = new Thread(monitor);
        networkMonitorThread.start();


    }

    @Override
    public void onCreate(){
        super.onCreate();
        instance = this;
    }

    public static boolean isInstanceCreated(){
        return instance != null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        instance = null;
    }




}
