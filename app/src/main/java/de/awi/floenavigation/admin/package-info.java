/**
 * This package provides the classes and Activities for the Admin Dashboard on the App. The Admin Dashboard is special menu in the App which
 * is accessible only to specific users. The list of Admin Users is defined by the Database table
 * {@link de.awi.floenavigation.helperclasses.DatabaseHelper#usersTable}.The activities included in the Admin Dashboard are critical to
 * the smooth running of the App.
 * <p>
 *     The tasks included in the Admin Dashboard are: changing the values of {@link de.awi.floenavigation.helperclasses.DatabaseHelper#configurationParameters},
 *     administration of Admin Users, deploying new Fixed Stations, Recovery of Fixed and Static Stations and running the Synchronization.
 *     Most of these tasks are handled by the Activities in this package. However, the Synchronization process and Deployment of new Fixed
 *     Stations are quite complicated tasks so those tasks are handled in their own packages.
 * </p>
 * <p>
 *     This package also provides the {@link de.awi.floenavigation.admin.ListViewActivity}, which is a generic class that can be used to
 *     display any information from the Database in a List form. This is used to display the Waypoints list in the
 *     {@link de.awi.floenavigation.waypoint.WaypointActivity}, Sample/Measurement in {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity},
 *     Fixed Stations and Static Stations in {@link de.awi.floenavigation.admin.RecoveryActivity}.
 * </p>
 */
package de.awi.floenavigation.admin;