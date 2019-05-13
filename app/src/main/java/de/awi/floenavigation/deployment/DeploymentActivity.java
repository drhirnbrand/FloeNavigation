package de.awi.floenavigation.deployment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import de.awi.floenavigation.admin.AdminPageActivity;
import de.awi.floenavigation.helperclasses.FragmentChangeListener;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.R;

/**
 * This is the primary {@link FragmentActivity} for installation of a Static Station or a Fixed Station. The activity's own layout is empty containing only
 * a single {@link android.widget.FrameLayout}. There are multiple Fragments running on top of this activity which handle the different type
 * of stations to be installed separately. The primary fragment which is running by default is {@link StationInstallFragment}.
 * <p>
     * The icons shown on the Action Bar and the Up button on the screen depend on the running fragment. The Backbutton, Action Bar icons and
     * Up Button are disabled in the fragments {@link StaticStationFragment} and {@link AISStationCoordinateFragment} while are enabled in
     * {@link StationInstallFragment}.
 * </p>
 */

public class DeploymentActivity extends FragmentActivity implements FragmentChangeListener {


    private static final String TAG = "DeploymentActivity";
    //Action Bar Updates


    /**
     * <code>true</code>When the activity is called from {@link AdminPageActivity} which means a Fixed Station is being deployed.
     */
    private boolean aisDeployment;

    /**
     * Default implementation of {@link #onCreate(Bundle)}. Read a boolean from the calling Intent and sets the value of {@link #aisDeployment}
     * accordingly. Runs the fragment {@link StationInstallFragment}.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deployment);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        aisDeployment = getIntent().getExtras().getBoolean("DeploymentSelection");
        Bundle bundle = new Bundle();
        bundle.putBoolean("stationTypeAIS", aisDeployment);
        StationInstallFragment deviceFragment = new StationInstallFragment();
        deviceFragment.setArguments(bundle);
        this.replaceFragment(deviceFragment);

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    /**
     * Shows the Up button in the ActionBar on the tablet Screen.
     */
    public void showUpButton(){
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Hides the Up button in the ActionBar on the tablet Screen.
     */
    public void hideUpButton(){
        getActionBar().setDisplayHomeAsUpEnabled(false);
    }


    /**
     * Replaces the currently running Fragment with a new fragment using the {@link FragmentManager} and creating a {@link FragmentTransaction}.
     * @inheritDoc
     * @param fragment The {@link Fragment} to replace the currently running Fragment with.
     */
    @Override
    public void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frag_container, fragment, fragment.toString());
        fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.commit();
    }

    /**
     * Overrides the Default Back button behavior of Android. Checks the value of {@link #aisDeployment} to return the App to the activity
     * from which it came originally.
     */
    @Override
    public void onBackPressed(){
        Fragment frag = this.getSupportFragmentManager().findFragmentById(R.id.frag_container);
        Intent intent;
        if (frag instanceof StationInstallFragment){
            if (aisDeployment) {
                intent = new Intent(this, AdminPageActivity.class);
            }else
                intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else{
            Toast.makeText(this, "Please finish Setup of the Station", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (aisDeployment) {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
