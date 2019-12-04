package de.awi.floenavigation.admin;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.R;

/**
 * Class deals with activity related to logging to the admin dashboard
 * Username and pwd entered are verified with internal local database {@value DatabaseHelper#usersTable}
 * On successful login, the admin is redirected to the admin dashboard {@link AdminPageActivity}
 */
public class LoginPage extends ActionBarActivity {

    /**
     * Creating a new database helper to access the local internal database
     */
    DatabaseHelper databaseHelper = new DatabaseHelper(this);

    /**
     * Layout variable
     */
    RelativeLayout username_pwd;
    /**
     * Edittexts present in Relative layout
     */
    EditText username, password;

    /**
     * onCreate function to initialize the edit texts parameters and to add animation for the relative layout
     * @param savedInstanceState helps the activity to store the previous values of some parameters before the app is minimized
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);

        username_pwd = findViewById(R.id.username_pwd);
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                username_pwd.setVisibility(View.VISIBLE);
            }
        };
        handler.postDelayed(runnable, 1000);

    }

    /**
     * Verification of the entered username and the password with the internal local database and thereby redirect to {@link AdminPageActivity} activity
     * @param view The view that was clicked
     */
    public void onClickLogin(View view){
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        String usernameSubmitted = username.getText().toString();
        String passwordSubmitted = password.getText().toString();

        String pass = databaseHelper.searchPassword(usernameSubmitted, getApplicationContext());

        if (passwordSubmitted.equals(pass)){
            Intent intent = new Intent(this, AdminPageActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Username and Password do not match", Toast.LENGTH_LONG).show();
        }


    }
}
