/**
 * This package handles the processing of AIS packets received from the ais transponder.
 * <p>
 *     {@link de.awi.floenavigation.aismessages.AISMessageReceiver} - It is a thread enabled from {@link de.awi.floenavigation.network.NetworkMonitor}
 *     which establishes telnet connection between the tablet and the ais transponder.
 *     It also segragates packets based on AIVDM and AIVDO messages and forwards it to {@link de.awi.floenavigation.aismessages.AISDecodingService}
 *     {@link de.awi.floenavigation.aismessages.AISDecodingService} - It decodes the AIS packets and based on the packet type sends to the respective
 *     functions for further decoding.
 *     It receives the decoded values from the position report and static data classes and stores in the corresponding
 *     database tables.
 *     {@link de.awi.floenavigation.aismessages.AIVDM} - splits the AIS packet on the basis of comma and sends only the payloads to {@link de.awi.floenavigation.aismessages.PostnReportClassA}/
 *     {@link de.awi.floenavigation.aismessages.PostnReportClassB}/{@link de.awi.floenavigation.aismessages.StaticDataReport}/{@link de.awi.floenavigation.aismessages.StaticVoyageData}.
 *     {@link de.awi.floenavigation.aismessages.PostnReportClassA} - Decoding of packets from class A transponders.
 *     {@link de.awi.floenavigation.aismessages.PostnReportClassB} - Decoding of packets from class B transponders.
 *     {@link de.awi.floenavigation.aismessages.StaticDataReport} - Decoding of static data packets from class B transponders
 *     {@link de.awi.floenavigation.aismessages.StaticVoyageData} - Decoding of static data packets from class A transponders
 *
 * </p>
 */

package de.awi.floenavigation.aismessages;