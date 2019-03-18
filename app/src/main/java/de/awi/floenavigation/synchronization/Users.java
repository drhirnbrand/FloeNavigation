package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

/**
 * Creates an Admin User object with getters and setters for  all the parameters of a {@link DatabaseHelper#usersTable} Table in Database.
 * Used by {@link UsersSync} to create a new User Object to be inserted in to the Database.
 *
 * @see SyncActivity
 * @see UsersSync
 * @see de.awi.floenavigation.synchronization
 */
public class Users {

    private static final String TAG = "USERS";

    /**
     * Name of the Admin User
     */
    private String userName;

    /**
     * Password for the Admin User
     */
    private String password;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private Context appContext;

    /**
     * Default Constructor.
     * @param context Used to create a {@link DatabaseHelper} object.
     */
    public Users(Context context){
        appContext = context;
        try {
            dbHelper = DatabaseHelper.getDbInstance(appContext);
            db = dbHelper.getReadableDatabase();
        } catch(SQLException e){
            Log.d(TAG, "Database Exception");
            e.printStackTrace();
        }
    }

    /**
     * Inserts the User parameters in to a {@link ContentValues} which is then inserted in to {@link DatabaseHelper#usersTable} Table.
     */
    public void insertUserInDB(){
        ContentValues user = new ContentValues();
        user.put(DatabaseHelper.userName, this.userName);
        user.put(DatabaseHelper.password, this.password);
        int result = db.update(DatabaseHelper.usersTable, user, DatabaseHelper.userName + " = ?", new String[] {this.userName});
        if(result == 0){
            db.insert(DatabaseHelper.usersTable, null, user);
            Log.d(TAG, "User Created");
        } else{
            Log.d(TAG, "User Updated");
        }
    }

    /**
     * Get the User Password
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the User Password
     * @param password AIS Station Name
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get the User Name
     * @return User Name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Set the UserName
     * @param userName User Name
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }
}
