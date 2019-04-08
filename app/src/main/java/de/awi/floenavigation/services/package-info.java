/**
 * This package contains the essential and crucial services that runs on background for the proper functioning of the
 * app. These services are enabled from the {@link de.awi.floenavigation.dashboard.MainActivity}.
 * These services run through out the lifetime of the app. However if these services gets disabled, it gets restarted when
 * the user/admin navigates to the {@link de.awi.floenavigation.dashboard.MainActivity}.
 * <p>
 *     {@link de.awi.floenavigation.services.GPS_Service} - This service runs periodically and updates the app with
 *     gps location.
 *     {@link de.awi.floenavigation.services.AlphaCalculationService} - This service runs periodically to calculate alpha angle
 *     to determine the position of mobile station.
 *     {@link de.awi.floenavigation.services.AngleCalculationService} - This service periodically calculates angle beta.
 *     It calculates beta angle w.r.t to all the fixed stations, and stores the average beta value in the database.
 *     {@link de.awi.floenavigation.services.PredictionService} - It predicts future positions of all the fixed stations based
 *     on received coordinates, SOG and COG at fixed intervals.
 *     {@link de.awi.floenavigation.services.ValidationService} - It handles the comparison between predicted coordinates
 *     and the received coordinates and based on certain threshold values, notifies whether the sea ice floe has broken off.
 * </p>
 */

package de.awi.floenavigation.services;