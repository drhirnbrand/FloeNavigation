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


public class DeploymentActivity extends FragmentActivity implements FragmentChangeListener {

    private static final String TAG = "DeploymentActivity";
    //Action Bar Updates



    private boolean aisDeployment;

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

    public void showUpButton(){
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void hideUpButton(){
        getActionBar().setDisplayHomeAsUpEnabled(false);
    }



    @Override
    public void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frag_container, fragment, fragment.toString());
        fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.commit();
    }

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
