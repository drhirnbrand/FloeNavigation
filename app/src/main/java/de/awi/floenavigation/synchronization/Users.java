package de.awi.floenavigation.synchronization;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.awi.floenavigation.helperclasses.DatabaseHelper;

public class Users {

    private static final String TAG = "USERS";

    private String userName;
    private String password;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private Context appContext;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
