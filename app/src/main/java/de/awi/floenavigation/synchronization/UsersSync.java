package de.awi.floenavigation.synchronization;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

/**
 * Pushes User table parameters in the Local Database to the Server.
 * Reads all the parameters of {@link DatabaseHelper#usersTable} from the Database and stores it in {@link HashMap}s.
 * Creates {@link StringRequest}s and inserts it in to a {@link RequestQueue} to push and pull Data from the Server.
 * Clears the User Table before inserting Data that was pulled from the Server.
 *
 * <p>
 * Uses {@link Users} to create a new User and insert into local Database, the parameters that is pulled from the Server.
 *</p>
 * @see DatabaseHelper#usersTable
 * @see SyncActivity
 * @see Users
 * @see de.awi.floenavigation.synchronization
 */
public class UsersSync {

    private static final String TAG = "UsersSyncActivity";
    private Context mContext;

    /**
     * URL to use for Pushing Data to the Server
     * @see #setBaseUrl(String, String)
     */
    private String URL = "";

    /**
     * URL to use for Pulling Data from the Server
     * @see #setBaseUrl(String, String)
     */
    private String pullURL = "";

    /**
     * URL to use for sending Delete Request to the Server
     * @see #setBaseUrl(String, String)
     */
    private String deleteUserURL = "";

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private StringRequest request;

    /**
     * Stores {@link Users#userName} of all Users
     */
    private HashMap<Integer, String> userNameData = new HashMap<>();

    /**
     * Stores {@link Users#password} of all Users
     */
    private HashMap<Integer, String> userPasswordData = new HashMap<>();

    /**
     * Stores {@link Users#userName} of all the Users that are to be deleted.
     * Reads {@link Users#userName} from {@value DatabaseHelper#userDeletedTable}
     */
    private HashMap<Integer, String> deletedUserData = new HashMap<>();
    private Cursor userCursor = null;
    private Users user;
    private ArrayList<Users> usersList = new ArrayList<>();
    private RequestQueue requestQueue;
    private XmlPullParser parser;

    //private int numOfDeleteRequests = 0;

    private StringRequest pullRequest;

    /**
     * <code>true</code> if all Static Stations are pulled from the server and inserted in to the local Database
     */
    private boolean dataPullCompleted;

    /**
     * Default Constructor.
     * @param context Used to create a {@link DatabaseHelper} object.
     */
    UsersSync(Context context, RequestQueue requestQueue, XmlPullParser xmlPullParser){
        this.mContext = context;
        this.requestQueue = requestQueue;
        this.parser = xmlPullParser;
        dataPullCompleted = false;
    }

    /**
     * Reads the {@value DatabaseHelper#usersTable} Table and inserts the data from all the Columns of the
     * {@value DatabaseHelper#usersTable} Table in to their respective {@link HashMap}.
     * @throws SQLiteException In case of error in reading database
     * @see #userCursor
     */
    public void onClickUserReadButton(){
        try{
            int i = 0;
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            userCursor = db.query(DatabaseHelper.usersTable,
                    null,
                    null,
                    null,
                    null, null, null);
            if(userCursor.moveToFirst()){
                do{
                    userNameData.put(i, userCursor.getString(userCursor.getColumnIndexOrThrow(DatabaseHelper.userName)));
                    userPasswordData.put(i, userCursor.getString(userCursor.getColumnIndexOrThrow(DatabaseHelper.password)));
                    i++;

                }while (userCursor.moveToNext());
            }
            userCursor.close();
            Toast.makeText(mContext, "Read Completed from DB", Toast.LENGTH_SHORT).show();
        } catch (SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }finally {
            if (userCursor != null){
                userCursor.close();
            }
        }

    }

    /**
     * Creates {@link StringRequest}s as per the size of {@link #userNameData} data extracted from the local database and inserts all the requests in the {@link RequestQueue}
     * A Stringrequest {@link #request} for pushing the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     */
    public void onClickUserSyncButton(){
        for(int i = 0; i < userNameData.size(); i++){
            final int index = i;
            request = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.names().get(0).equals("success")) {
                            //Toast.makeText(mContext, "SUCCESS " + jsonObject.getString("success"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "SUCCESS: " + jsonObject.getString("success"));
                        } else {
                            Toast.makeText(mContext, "Error" + jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error: " + jsonObject.getString("error"));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String,String> hashMap = new HashMap<String, String>();
                    hashMap.put(DatabaseHelper.userName,(userNameData.get(index) == null)? "" : userNameData.get(index));
                    hashMap.put(DatabaseHelper.password,(userPasswordData.get(index) == null)? "" : userPasswordData.get(index));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
        sendUsersDeleteRequest();


    }

    /**
     * Function is used to pull data from internal database to the server
     * A Stringrequest {@link #request} for pulling the data is registered and added to the {@link #requestQueue}.
     * callback function {@link Response.Listener#onResponse(Object)} notifies whether the request was successful or not
     * If it is unsuccessful or the connection is not established {@link Response.Listener#error(VolleyError)} gets called
     * On pulling the data from the server internal database tables {@link DatabaseHelper#usersTable} and
     * {@link DatabaseHelper#userDeletedTable} are cleared.
     *
     * <p>
     * The server sends the data in .xml format, therefore it has to extract the data based on the tags
     * Inside {@link Response.Listener#onResponse(Object)} it loops through the entire xml file till it reaches the end of document.
     * Based on the {@link XmlPullParser#START_TAG}, {@link XmlPullParser#TEXT}, {@link XmlPullParser#END_TAG} it adds the values received to
     * the corresponding columns of the {@link DatabaseHelper#usersTable}
     * Each {@link #user} is added to the {@link #usersList} which is individually taken and added to the internal database.
     * </p>
     */
    public void onClickUserPullButton(){
        try {
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            db.execSQL("Delete from " + DatabaseHelper.usersTable);
            db.execSQL("Delete from " + DatabaseHelper.userDeletedTable);
            pullRequest = new StringRequest(pullURL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        parser.setInput(new StringReader(response));
                        int event = parser.getEventType();
                        String tag = "";
                        String value = "";
                        while (event != XmlPullParser.END_DOCUMENT) {
                            tag = parser.getName();
                            switch (event) {
                                case XmlPullParser.START_TAG:
                                    if (tag.equals(DatabaseHelper.usersTable)) {
                                        user = new Users(mContext);
                                        usersList.add(user);
                                    }
                                    break;

                                case XmlPullParser.TEXT:
                                    value = parser.getText();
                                    break;

                                case XmlPullParser.END_TAG:

                                    switch (tag) {

                                        case DatabaseHelper.userName:
                                            user.setUserName(value);
                                            break;

                                        case DatabaseHelper.password:
                                            user.setPassword(value);
                                            break;
                                    }
                                    break;
                            }
                            event = parser.next();
                        }
                        for (Users currentUser : usersList) {
                            currentUser.insertUserInDB();
                        }
                        dataPullCompleted = true;
                        Toast.makeText(mContext, "Data Pulled from Server", Toast.LENGTH_SHORT).show();
                    } catch (XmlPullParserException e) {
                        Log.d(TAG, "Error Parsing XML");
                        e.printStackTrace();
                    } catch (IOException e) {
                        Log.d(TAG, "IOException from Parser");
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });

            requestQueue.add(pullRequest);

        } catch (SQLException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
            Toast.makeText(mContext, "Database Error User Pull", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Used to initialize {@link #URL}, {@link #pullURL} and {@link #deleteUserURL}
     * @param baseUrl Url set by the administrator, which is stored in the local database
     * @param port port number set by the administrator, which is stored in the local database (default value is 80)
     */
    public void setBaseUrl(String baseUrl, String port){
        URL = "http://" + baseUrl + ":" + port + "/Users/pullUsers.php";
        pullURL = "http://" + baseUrl + ":" + port + "/Users/pushUsers.php";
        deleteUserURL = "http://" + baseUrl + ":" + port + "/Users/deleteUsers.php";

    }

    public boolean getDataCompleted(){
        return dataPullCompleted;
    }

    /**
     * Reads the {@value DatabaseHelper#userDeletedTable} Table and inserts the data from all the Columns of the
     * {@value DatabaseHelper#userDeletedTable} Table in to their respective {@link HashMap}.
     * @throws SQLiteException In case of error in reading database
     * After reading the data, it creates string requests to forward the data to the server
     * This facilitates the server to know which Users should be marked for deletion.
     */
    private void sendUsersDeleteRequest(){
        Cursor deletedUserCursor = null;
        try{
            dbHelper = DatabaseHelper.getDbInstance(mContext);
            db = dbHelper.getReadableDatabase();
            deletedUserCursor = db.query(DatabaseHelper.userDeletedTable,
                    null,
                    null,
                    null,
                    null, null, null);
            int i = 0;
            if(deletedUserCursor.moveToFirst()){
                do{
                    deletedUserData.put(i, deletedUserCursor.getString(deletedUserCursor.getColumnIndexOrThrow(DatabaseHelper.userName)));
                    Log.d(TAG, "User to be Deleted: " + deletedUserCursor.getString(deletedUserCursor.getColumnIndexOrThrow(DatabaseHelper.userName)));
                    i++;

                }while (deletedUserCursor.moveToNext());
            }
            deletedUserCursor.close();
            /*
            if(deletedUserData.size() == 0){
                requestQueue.add(pullRequest);
            } else{
                numOfDeleteRequests = deletedUserData.size();
            }*/

        } catch (SQLException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }finally {
            if (deletedUserCursor != null){
                deletedUserCursor.close();
            }
        }

        for(int j = 0; j < deletedUserData.size(); j++){
            final int delIndex = j;
            request = new StringRequest(Request.Method.POST, deleteUserURL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.names().get(0).equals("success")) {
                            //Toast.makeText(mContext, "SUCCESS " + jsonObject.getString("success"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "SUCCESS: " + jsonObject.getString("success"));
                        } else {
                            Toast.makeText(mContext, "Error" + jsonObject.getString("error"), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error: " + jsonObject.getString("error"));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    /*
                    numOfDeleteRequests--;
                    if(numOfDeleteRequests == 0){
                        requestQueue.add(pullRequest);
                    }*/

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    HashMap<String,String> hashMap = new HashMap<String, String>();
                    hashMap.put("username",(deletedUserData.get(delIndex) == null)? "" : deletedUserData.get(delIndex));
                    //Log.d(TAG, "User sent to be Deleted: " + deletedUserData.get(delIndex) + " Index: " + String.valueOf(delIndex));
                    return hashMap;
                }
            };
            requestQueue.add(request);

        }
    }

}
