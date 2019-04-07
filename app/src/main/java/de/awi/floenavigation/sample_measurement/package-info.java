/**
 * {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity} is called from the {@link de.awi.floenavigation.dashboard.MainActivity}
 * when the grid initial setup is completed and the device list has been imported from the server during synchronization.
 * <p>
 *     This activity provides the user of the app to take samples or measurements on the sea ice floe.
 *     Depending on the use case, the user can select between sample and measurement using a drop down list.
 *     An advanced search for selection of devices based on initial 2 characters has been implemented using {@link android.widget.AutoCompleteTextView}.
 *     When the device has been selected, all other information such as {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity#deviceFullNameIndex},
 *     {@link de.awi.floenavigation.sample_measurement.SampleMeasurementActivity#deviceIDIndex} will be automatically populated on the views.
 *     If the gps location is available, the user can press the confirm button to store the sample in the internal database table.
 *     The user can also view all the samples currently stored in the database by pressing view samples, the user will be presented with
 *     a list of samples with information such as sample name, comment, x and y position on the grid {@link de.awi.floenavigation.admin.ListViewActivity}.
 * </p>
 *
 * @see de.awi.floenavigation.synchronization.SyncActivity
 * @see de.awi.floenavigation.synchronization.SampleMeasurement
 */

package de.awi.floenavigation.sample_measurement;