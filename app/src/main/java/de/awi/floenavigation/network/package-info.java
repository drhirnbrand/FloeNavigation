/**
 * This package handles the network connection with the ais transponder over wifi.
 * {@link de.awi.floenavigation.network.NetworkService} is an intent service which runs a thread {@link de.awi.floenavigation.network.NetworkMonitor}.
 * <p>
 *      The functionality of the thread is to continuously ping the server and checks whether the connection between the tablet and the ais transponder is still available.
 *      If the ping is successful, it executes {@link de.awi.floenavigation.aismessages.AISMessageReceiver} on a separate thread which is responsible for creating a
 *      telnet client connection with the server and for checking whether the packet is of AIVDM or AIVDO. Eventually sending the packet to the {@link de.awi.floenavigation.aismessages.AISDecodingService}
 *      for decoding of the packet.
 *      However, if the ping is unsuccessful {@link de.awi.floenavigation.network.NetworkMonitor} sends a disconnect flag to the {@link de.awi.floenavigation.aismessages.AISMessageReceiver}
 *      requesting it to disconnect the telnet connection.
 *      The thread which runs the {@link de.awi.floenavigation.aismessages.AISMessageReceiver} on receival of the disconnect flag, stops decoding the packet.
 *      If the thread does not have any work to do, it eventually dies out.
 *      After every transition from a unsuccessful ping request to successful ping request, a new thread to execute {@link de.awi.floenavigation.aismessages.AISMessageReceiver}
 *      is created. This helps the app to have continuous connection to the AIS transponder.
 * </p>
 */


package de.awi.floenavigation.network;