/**
 * Provides the necessary Activity and Fragments required for installing a new Station. The Station to be added can be a Fixed Station or a
 * Static Station; since the installation of both type of stations is similar there is one main Activity {@link de.awi.floenavigation.deployment.DeploymentActivity}
 * which displays a common Fragment {@link de.awi.floenavigation.deployment.StationInstallFragment}. The StationInstallFragment layout is
 * changed based on from where the DeploymentActivity was called. If the DeploymentActivity is called from the {@link de.awi.floenavigation.dashboard.MainActivity}
 * then the Layout for installation of {@link de.awi.floenavigation.helperclasses.DatabaseHelper#staticStationName} is shown otherwise if the
 * DeploymentActivity is called from the {@link de.awi.floenavigation.admin.AdminPageActivity} then the Layout for installation of {@link de.awi.floenavigation.helperclasses.DatabaseHelper#fixedStationTable}
 * is shown.
 * <p>
 *     There are two different Fragments for handling of {@link de.awi.floenavigation.helperclasses.DatabaseHelper#fixedStationTable} and
 *     {@link de.awi.floenavigation.helperclasses.DatabaseHelper#staticStationListTable}:
 *     <p>{@link de.awi.floenavigation.deployment.AISStationCoordinateFragment} handles the installation of Fixed Stations</p>
 *
 *     <p>{@link de.awi.floenavigation.deployment.StaticStationFragment} handles the installation of Static Stations</p>
 * </p>
 * @see de.awi.floenavigation.helperclasses.DatabaseHelper
 */

package de.awi.floenavigation.deployment;