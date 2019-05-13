/**
 * Provides the classes necessary for running the Synchronization process and the classes for every database table which is to be
 * Synchronized with the Sync Server. The Synchronization processes uses {@link com.android.volley.RequestQueue}, {@link com.android.volley.toolbox.StringRequest}
 * {@link org.xmlpull.v1.XmlPullParser} to transfer data between the Sync Server and the App.
 * <p>
 *     For each Database table which is to be Synchronized there are two classes.
 *     A class which contains all the parameters (columns) of each database table to be synchronized and another class which provides
 *     methods for creating the Push and Pull requests and adding those requests to the {@link com.android.volley.RequestQueue}.
 *     An android activity ({@link de.awi.floenavigation.synchronization.SyncActivity}) is used for running the Overall synchronization
 *     process.
 * </p>
 * @see com.android.volley.RequestQueue
 * @see com.android.volley.toolbox.StringRequest
 * @see org.xmlpull.v1.XmlPullParser
 * @see org.xmlpull.v1.XmlPullParserFactory
 */

package de.awi.floenavigation.synchronization;