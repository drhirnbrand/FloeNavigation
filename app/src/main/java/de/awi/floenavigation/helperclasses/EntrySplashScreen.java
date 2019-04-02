package de.awi.floenavigation.helperclasses;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.R;
import gr.net.maroulis.library.EasySplashScreen;

/**
 * This {@link Activity} is the first activity to run whenever the App is launched on the tablet. Its a simple activity which just
 * shows the AWI logo for a specified amount of time and then starts the {@link MainActivity}.
 *
 * @see EasySplashScreen
 */
public class EntrySplashScreen extends Activity {

    /**
     * Default {@link Activity#onCreate(Bundle)}. Creates an {@link EasySplashScreen} which shows the AWI logo for a specified amount of
     * time and then starts the {@link MainActivity}.
     * @param savedInstanceState
     */
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
