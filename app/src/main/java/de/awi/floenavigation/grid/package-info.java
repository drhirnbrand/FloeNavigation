/**
 * This package provides a graphical representation of the grid, by which the user/admin can see the positions of all the stations, waypoints,
 * mothership on the sea ice. The grid refreshes itself at particular time steps enabling the user/admin to view the points of interest almost at real
 * time.
 * <p>
 *     Provides {@link de.awi.floenavigation.grid.GridActivity} which is responsible of running several async tasks periodically {@link android.os.AsyncTask}
 *     to read grid positions of fixed stations {@link de.awi.floenavigation.helperclasses.DatabaseHelper#fixedStationTable}, mobile stations {@link de.awi.floenavigation.helperclasses.DatabaseHelper#mobileStationTable},
 *     static stations {@link de.awi.floenavigation.helperclasses.DatabaseHelper#staticStationListTable} and waypoints {@link de.awi.floenavigation.helperclasses.DatabaseHelper#waypointsTable},
 *     from the local database. It also reads tablet location {x,y}.
 *     {@link de.awi.floenavigation.grid.GridActivity} sends these values to the {@link de.awi.floenavigation.grid.MapView} which is responsible for drawing of the grid on the screen
 *     and actual display of all these points on the grid.
 * </p>
 * <p>
 *     {@link de.awi.floenavigation.grid.BubbleDrawable} takes care of displaying a dialog box when the user/admin clicks on a station or a waypoint.
 *     The dialog box provides information w.r.t to the point which is clicked.
 * </p>
 * <p>
 *     {@link de.awi.floenavigation.grid.Zoomer} takes care of handling zoom in and zoom out, pan features on the grid.
 * </p>
 */

package de.awi.floenavigation.grid;