package de.awi.floenavigation.network;

import java.io.IOException;
import java.net.InetAddress;

import de.awi.floenavigation.util.Constants;

abstract class NetworkUtils {

    /**
     * Responsible for ping request
     *
     * @param dstAddress
     * @return <code>true</code> if the {@link Constants#DST_ADDRESS} is reachable
     * <code>false</code> otherwise
     */
    static boolean isDestinationReachable(final String dstAddress) {

        boolean mExitValue = false;


        try {
            mExitValue = InetAddress.getByName(dstAddress).isReachable(1000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mExitValue;
    }

}
