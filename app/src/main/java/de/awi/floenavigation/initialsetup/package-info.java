/**
 * Provides the necessary Activities and Fragments required for setting up the Floe's Coordinate System. The Grid is setup by adding two AIS Stations
 * as Fixed/Base Stations. The Grid Initial Setup starts with the Activity {@link de.awi.floenavigation.initialsetup.GridSetupActivity}
 * on which two Fragments are run.
 * <p>
 *     The Fragment {@link de.awi.floenavigation.initialsetup.MMSIFragment} is used to add the Station Name and MMSI to the Database tables
 *     {@link de.awi.floenavigation.helperclasses.DatabaseHelper#fixedStationTable} and {@link de.awi.floenavigation.helperclasses.DatabaseHelper#stationListTable}.
 *     It then loads the Fragment {@link de.awi.floenavigation.initialsetup.CoordinateFragment}.
 * </p>
 * <p>
 *     The Fragment {@link de.awi.floenavigation.initialsetup.CoordinateFragment} then waits for the {@link de.awi.floenavigation.aismessages.AISDecodingService}
 *     to receive an AIS Position report from these Stations; while waiting the {@link de.awi.floenavigation.initialsetup.CoordinateFragment} layout
 *     shows a waiting screen. Once the AIS Position report is received the layout displays the Geographical coordinates received and the coordinates
 *     of the tablet itself. It then loads {@link de.awi.floenavigation.initialsetup.MMSIFragment} again to add a second Fixed/Base Station in the same manner.
 *     When the second station is added and the position report is received and displayed, then the {@link de.awi.floenavigation.initialsetup.CoordinateFragment}
 *     starts the {@link de.awi.floenavigation.initialsetup.SetupActivity}.
 * </p>
 * <p>
 *     The {@link de.awi.floenavigation.initialsetup.SetupActivity} runs the setup process. This activity displays the the MMSI and received coordinates
 *     of both Fixed/Base Stations. It also predicts the next position of each station using the Speed Over Ground (SOG) and Course Over Ground (COG) information
 *     which is received in the AIS Position Report. It then compares the predicted position with the received position using the {@link de.awi.floenavigation.helperclasses.NavigationFunctions#calculateNewPosition(double, double, double, double)}
 *     and {@link de.awi.floenavigation.helperclasses.NavigationFunctions#calculateDifference(double, double, double, double)}. It also calculates
 *     the Angle {@link de.awi.floenavigation.helperclasses.DatabaseHelper#beta} of the coordinate system with the received coordinates and the
 *     predicted coordinates. This Activity runs this comparison for the time specified by the Configuration Parameter {@link de.awi.floenavigation.helperclasses.DatabaseHelper#initial_setup_time};
 *     at the end of which the user can choose to re-run the comparison or exit to {@link de.awi.floenavigation.dashboard.MainActivity}.
 * </p>
 * @see de.awi.floenavigation.helperclasses.DatabaseHelper
 * @see de.awi.floenavigation.helperclasses.NavigationFunctions
 */

package de.awi.floenavigation.initialsetup;