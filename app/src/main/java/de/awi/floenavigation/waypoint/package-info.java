/**
 * This package provides the necessary activity to process Waypoints in the App.
 * {@link de.awi.floenavigation.waypoint.WaypointActivity} is called from the {@link de.awi.floenavigation.dashboard.MainActivity}
 * when the grid initial setup is completed.
 * <p>
 *     Waypoints can be any poinnt of interest on the Ice. Each Waypoint has a unique name which is given by the user. To ensure that
 *     the Waypoint names remain unique throughout the {@link de.awi.floenavigation.helperclasses.DatabaseHelper#tabletId} is appended to
 *     the name provided by the user. The User can delete previously created waypoints. Previously created Waypoints can be viewed by
 *     tapping the View Waypoints button on the {@link de.awi.floenavigation.waypoint.WaypointActivity}. This list is created by using
 *     the {@link de.awi.floenavigation.admin.ListViewActivity} and it displays the name, and x,y position of each waypoint currently
 *     existing in the database.
 *     Waypoints created in other tablets can also viewed after the Synchronization Process.
 * </p>
 *
 * @see de.awi.floenavigation.synchronization.SyncActivity
 * @see de.awi.floenavigation.synchronization.Waypoints
 */

package de.awi.floenavigation.waypoint;