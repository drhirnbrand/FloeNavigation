package de.awi.floenavigation.admin;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.R;

public class LoginPage extends ActionBarActivity {

    DatabaseHelper databaseHelper = new DatabaseHelper(this);

    RelativeLayout username_pwd;
    EditText username, password;

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
