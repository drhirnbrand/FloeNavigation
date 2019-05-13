package de.awi.floenavigation.admin;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Pattern;

import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.R;

/**
 * This Activity is responsible for adding and deleting administrators of the system
 */
public class AdminUserPwdActivity extends ActionBarActivity {

    private static final String TAG = "AdminUserPwdActivity";
    /**
     * Views pertaining for entering new username and password
     */
    private EditText newusernameView, newpwdView;
    /**
     * Used for validation check of the username {@link #validateNewUserCredentials(String, String)}
     */
    private static final int MAX_CHARS = 3;

    /**
     * Adds the layout and initializes the views {@link #newusernameView}, {@link #newpwdView}
     * @param savedInstanceState used for previous saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_pwd);

        newusernameView = findViewById(R.id.newusername_id);
        newpwdView = findViewById(R.id.newpassword_id);
    }

    /**
     * onClick Listener responsible for reading the new username and password and validating the same as per general guidelines
     * Once validation is done, the admin is redirected to the {@link AdminPageActivity} activity
     * @param view The view was clicked
     */
    public void onClickNewUserConfirm(View view) {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            String newUserName = newusernameView.getText().toString();
            String newPassword = newpwdView.getText().toString();

            if (validateNewUserCredentials(newUserName, newPassword)){
                DatabaseHelper.insertUser(db, newUserName, newPassword);
                Toast.makeText(getApplicationContext(), "New User Credentials Saved", Toast.LENGTH_LONG).show();

                Intent adminPageActivityIntent = new Intent(this, AdminPageActivity.class);
                startActivity(adminPageActivityIntent);
            }



        } catch(SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
    }

    /**
     * Function responsible for validating the username and the password
     * As a first step, it checks whether the entered username is already present in its local internal database
     * Then it checks for the number of characters of the entered username
     * @param newUserName username - input argument
     * @param newPassword password - input argument
     * @return <code>true</code> If the validation is successful
     */
    private boolean validateNewUserCredentials(String newUserName, String newPassword) {
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        String receivedPassword = databaseHelper.searchPassword(newUserName, getApplicationContext());

        if (!receivedPassword.equals("Not Found")){
            Toast.makeText(getApplicationContext(), "User Name already present", Toast.LENGTH_LONG).show();
            return false;
        }

        if (newUserName.length() < MAX_CHARS){
            Toast.makeText(getApplicationContext(), "User name must be atleast 3 chars", Toast.LENGTH_LONG).show();
            return false;
        }
        Pattern pattern = Pattern.compile("[A-Za-z0-9_]+");
        boolean validUserName = (newUserName != null) && pattern.matcher(newUserName).matches();
        boolean validPassword = (newPassword != null) && pattern.matcher(newPassword).matches();
        if (!validUserName){
            Toast.makeText(getApplicationContext(), "Invalid user name", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!validPassword){
            Toast.makeText(getApplicationContext(), "Invalid Password", Toast.LENGTH_LONG).show();
            return false;
        }


        return true;
    }

    /**
     * onClick listener to display the list of administrators already present in the system
     * It starts the {@link ListViewActivity} activity to display a list of admins
     * @param view The view that was clicked
     */
    public void onClickViewUsers(View view) {
        Intent listViewIntent = new Intent(this, ListViewActivity.class);
        listViewIntent.putExtra("GenerateDataOption", "UsersPwdActivity");
        startActivity(listViewIntent);
    }
}
