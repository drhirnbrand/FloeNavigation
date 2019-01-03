package de.awi.floenavigation.helperclasses;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.R;
import gr.net.maroulis.library.EasySplashScreen;

public class EntrySplashScreen extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EasySplashScreen config = new EasySplashScreen(EntrySplashScreen.this)
                .withFullScreen()
                .withTargetActivity(MainActivity.class)
                .withSplashTimeOut(3000)
                .withBackgroundColor(Color.parseColor("#FFFFFF"))
                .withLogo(R.drawable.awi_logo);

        View view = config.create();
        setContentView(view);
    }
}
