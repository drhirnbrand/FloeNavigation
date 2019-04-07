
/**
 * {@link de.awi.floenavigation.dashboard.MainActivity} represents the main dashboard of the app. This is the first screen seen by the user/admin.
 * One can navigate from here to different operations/screens.
 * <p>
 *     Different menus which are available on the screen are
 *     {@link de.awi.floenavigation.deployment.DeploymentActivity} - used for static station deployment
 *     {@link de.awi.floenavigation.synchronization.SampleMeasurement} - used for taking sample and measurement
 *     {@link de.awi.floenavigation.waypoint.WaypointActivity} - used for installing waypoints on the sea ice
 *     {@link de.awi.floenavigation.grid.GridActivity} - used for visual representation of the grid
 *     {@link de.awi.floenavigation.admin.AdminPageActivity} - used for admin related activities such as ais station deployment,
 *     recovery, synchronization etc.
 * </p>
 * <p>
 *     There are 3 icons on the menu bar namely the gps icon, ais status icon and grid initial setup icon.
 *     By default the color of these 3 icons are red.
 *     gps icon will turn green whenever gps location is available for the tablet.
 *     ais status icon turns green whenever the ping request between the tablet and the ais transponder is successful.
 *     grid initial setup icon turns green if the initial grid setup has been completed.
 * @see de.awi.floenavigation.helperclasses.ActionBarActivity
 * </p>
 * <p>
 *     Initially (using the app for the first time), only the {@link de.awi.floenavigation.admin.AdminPageActivity} will be available to the admin.
 *     Only after the grid has been setup all other menus will be available to the user.
 * </p>
 * <p>
 *     {@link de.awi.floenavigation.dashboard.MainActivity} also deals with starting the {@link de.awi.floenavigation.network.NetworkService} which takes care of establishing telnet connection between
 *     the ais transponder and the tablet and starting the {@link de.awi.floenavigation.services.GPS_Service} which takes care of enabling gps service of the tablet and
 *     read gps coordinates at frequent intervals.
 *     These services are started when one opens the app for the first time and remains active throughout the lifetime of the app.
 *     If however these sevices gets cancelled, these are restarted when user/admin navigates to the dashboard {@link de.awi.floenavigation.dashboard.MainActivity}
 * </p>
 * <p>
 *     When the app is started for the first time, the admin will get a pop up asking permission to enable the gps of the tablet.
 *     {@link de.awi.floenavigation.dashboard.MainActivity#checkPermission()}. If the gps is already enabled, it won't ask for permission.
 * </p>
 * <p>
 *     Other services such as {@link de.awi.floenavigation.services.AngleCalculationService}, {@link de.awi.floenavigation.services.AlphaCalculationService}, {@link de.awi.floenavigation.services.PredictionService}
 *     and {@link de.awi.floenavigation.services.ValidationService} are started/restarted if these services are not running.
 *     These services are only enabled when the grid initial setup is completed.
 * </p>
 */
package de.awi.floenavigation.dashboard;