package de.awi.floenavigation.helperclasses;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import de.awi.floenavigation.R;
import de.awi.floenavigation.dashboard.MainActivity;
import de.awi.floenavigation.admin.AdminPageActivity;
import de.awi.floenavigation.initialsetup.SetupActivity;
import de.awi.floenavigation.services.GPS_Service;

/**
 * This {@link Activity} creates and displays different type of {@link Dialog}s according to the parameters passed to it in its calling
 * {@link Intent}. Currently it displays four different type of Dialogs
 * <p>
 *     The Validation Dialog is created when the {@link de.awi.floenavigation.services.ValidationService} fails for a Fixed Station and
 *     the Dialog Box contains a simple message and can be canceled by a simple Tap on the screen. When the Dialog is canceled it
 *     starts the {@link MainActivity}.
 * </p>
 * <p>
 *     The About Us Dialog is created from the {@link MainActivity} and contains info about the App. The Dialog Box contains the info
 *     in simple text and the Dialog Box can be canceled by a Simple tap on the screen which returns the App to {@link MainActivity}.
 * </p>
 * <p>
 *     The Tablet ID dialog is created only once when the App is used for the first time. It is created when the {@link AdminPageActivity}
 *     is started. It contains a simple message with an {@link EditText} in which the Tablet ID is entered and an Okay button. On clicking
 *     the Okay button the text (Tablet ID) in the {@link EditText} is inserted in the Database and it returns to {@link AdminPageActivity}.
 * </p>
 * <p>
 *     The Setup Complete Dialog is created when the Next button is clicked in {@link SetupActivity} at the end of the Predictions. The
 *     Dialog Box contains a text message with two Buttons (Confirm and Finish). On clicking Confirm the {@link SetupActivity} is restarted
 *     to re-run the Predictions. On clicking Finish {@link MainActivity} is started and the last calculated Grid Parameters are inserted
 *     in the Database.
 * </p>
 *
 * @see MainActivity
 * @see SetupActivity
 * @see de.awi.floenavigation.services.ValidationService
 * @see AdminPageActivity
 */

public class DialogActivity extends Activity {

    private static final String TAG = "DialogActivity";
    /**
     * The title of the {@link AlertDialog}
     */
    private String dialogTitle;

    /**
     * The message to show with in the {@link AlertDialog}
     */
    private String dialogMsg;

    /**
     * The icon of the {@link AlertDialog}
     */
    private int dialogIcon;

    /**
     * Variable to specify whether to show Options (For example Yes/No buttons) at the bottom of the Dialog Box
     * If <code>true</code> the buttons will be shown.
     */
    private boolean showDialogOptions = false;

    /**
     * <code>true</code> if the current dialog is the TabletID dialog.
     */
    private boolean tabletIdDialog = false;

    /**
     * <code>true</code> if the current dialog is the About Us dialog.
     */
    private boolean aboutUsDialog = false;

    /**
     * <code>true</code> if the current dialog is the Validation Service dialog.
     */
    private boolean validationDialog = false;
    //public static boolean servicesStarted = true;
    public static final String DIALOG_BUNDLE = "dialogBundle";

    /**
     * String specifying the key for getting the {@link #dialogTitle} from the extras of the {@link Bundle} passed.
     */
    public static final String DIALOG_TITLE = "title";

    /**
     * String specifying the key for getting the {@link #dialogMsg} from the extras of the {@link Bundle} passed.
     */
    public static final String DIALOG_MSG = "message";

    /**
     * String specifying the key for getting the {@link #dialogIcon} from the extras of the {@link Bundle} passed.
     */
    public static final String DIALOG_ICON = "icon";

    /**
     * String specifying the key for getting the value of  {@link #showDialogOptions} from the extras of the {@link Bundle} passed.
     */
    public static final String DIALOG_OPTIONS = "options";

    /**
     * String specifying the key for getting the value of  {@link #receivedBeta} from the extras of the {@link Bundle} passed.
     */
    public static final String DIALOG_BETA = "beta";

    /**
     * String specifying the key for getting the value of  {@link #tabletIdDialog} from the extras of the {@link Bundle} passed.
     */
    public static final String DIALOG_TABLETID = "tabletIdDialog";

    /**
     * String specifying the key for getting the value of  {@link #aboutUsDialog} from the extras of the {@link Bundle} passed.
     */
    public static final String DIALOG_ABOUTUS = "aboutUsDialog";

    /**
     * String specifying the key for getting the value of  {@link #validationDialog} from the extras of the {@link Bundle} passed.
     */
    public static final String DIALOG_VALIDATION = "validationDialog";

    /**
     * The actual unique ID of the tablet which is set the first time the App is run on a tablet. This is set if the current
     * Dialog is the Tablet ID dialog when {@link #tabletIdDialog} is <code>true</code>.
     * @see AdminPageActivity
     */
    private String tabletId;

    /**
     * The {@link AlertDialog} object which will be displayed
     */
    private AlertDialog alertDialog;

    /**
     * The value of the angle {@link DatabaseHelper#beta} to insert in the Database table {@link DatabaseHelper#betaTable} on the completion
     * of {@link SetupActivity}. This is used if the current Dialog is the Dialog box shown at the end of the predictions in {@link SetupActivity}.
     * @see SetupActivity
     */
    private double receivedBeta = 0.0;

    /**
     * {@link BroadcastReceiver} for receiving the GPS location broadcast from {@link GPS_Service}
     */
    private BroadcastReceiver broadcastReceiver;

    /**
     * The current UTC time in milliseconds as read from the GPS fix available via {@link GPS_Service}
     */
    private long gpsTime;

    /**
     * The difference in milliseconds between the current time of the tablet and {@link #gpsTime}
     */
    private long timeDiff;

    /**
     * Default {@link Activity#onCreate(Bundle)}. Registers the {@link #broadcastReceiver} and retrieves the values for type of dialog
     * box to display from the {@link Intent} with which this activity was started. Then depending on the type of Dialog Box to display
     * it will create and display the Dialog Box.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent){
                    gpsTime = Long.parseLong(intent.getExtras().get(GPS_Service.GPSTime).toString());
                    timeDiff = System.currentTimeMillis() - gpsTime;

                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter(GPS_Service.GPSBroadcast));


        Intent callingIntent = getIntent();
        if(callingIntent.getExtras().containsKey(DIALOG_ABOUTUS)){
            aboutUsDialog = callingIntent.getExtras().getBoolean(DIALOG_ABOUTUS);
        }
        if(callingIntent.getExtras().containsKey(DIALOG_TITLE)){
            dialogTitle = callingIntent.getExtras().getString(DIALOG_TITLE);
        }
        if(callingIntent.getExtras().containsKey(DIALOG_MSG)){
            dialogMsg = callingIntent.getExtras().getString(DIALOG_MSG);
        }
        if(callingIntent.getExtras().containsKey(DIALOG_ICON)){
            dialogIcon = callingIntent.getExtras().getInt(DIALOG_ICON);
        }
        if(callingIntent.getExtras().containsKey(DIALOG_OPTIONS)){
            showDialogOptions = callingIntent.getExtras().getBoolean(DIALOG_OPTIONS);
        }
        if(callingIntent.getExtras().containsKey(DIALOG_BETA)){
            receivedBeta = callingIntent.getExtras().getDouble(DIALOG_BETA);
        }
        if(callingIntent.getExtras().containsKey(DIALOG_TABLETID)){
            tabletIdDialog = callingIntent.getExtras().getBoolean(DIALOG_TABLETID);
        }
        if(callingIntent.getExtras().containsKey(DIALOG_VALIDATION)){
            validationDialog = callingIntent.getExtras().getBoolean(DIALOG_VALIDATION);
        }

        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);

        if(aboutUsDialog) {
            Dialog aboutUs = new Dialog(this);
            aboutUs.requestWindowFeature(Window.FEATURE_NO_TITLE);
            WindowManager.LayoutParams wmlp = aboutUs.getWindow().getAttributes();
            wmlp.width = WindowManager.LayoutParams.MATCH_PARENT;
            wmlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            aboutUs.setContentView(R.layout.activity_dialog);
            CardView aboutUsView = aboutUs.findViewById(R.id.aboutUs_View);
            CardView normalView = aboutUs.findViewById(R.id.normalDialog_View);
            normalView.setVisibility(View.GONE);
            aboutUsView.setVisibility(View.VISIBLE);
            aboutUs.setTitle("");
            aboutUs.setCancelable(true);
            aboutUs.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
            });
            aboutUs.setCanceledOnTouchOutside(true);

            aboutUs.show();

        } else if(validationDialog) {
            alertBuilder.setIcon(dialogIcon);
            alertBuilder.setIcon(dialogIcon);
            alertBuilder.setTitle(dialogTitle);
            alertBuilder.setMessage(dialogMsg);
            alertDialog = alertBuilder.create();
            alertDialog.setCancelable(true);
            alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
            });
            alertDialog.setCanceledOnTouchOutside(true);
            alertDialog.show();

        } else {

            alertBuilder.setIcon(dialogIcon);
            alertBuilder.setTitle(dialogTitle);
            alertBuilder.setMessage(dialogMsg);


            if (showDialogOptions) {

                alertBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //clearDatabase();
                        Intent intent = new Intent(getApplicationContext(), SetupActivity.class);
                        intent.putExtra(SetupActivity.calledFromCoordinateFragment, false);
                        startActivity(intent);
                    }
                });
                alertBuilder.setNegativeButton("Finish", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.cancel();
                        new InsertBetaOnStartup().execute();
                        showNavigationBar();
                        SetupActivity.runServices(getApplicationContext());
                        //MainActivity.servicesStarted = true;
                        //servicesStarted = false;
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                    }
                });
            }

            if (tabletIdDialog) {
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!TextUtils.isEmpty(s)) {
                            ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }

                    }
                });
                alertBuilder.setView(input);
                alertBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tabletId = input.getText().toString();
                        Log.d(TAG, tabletId);

                        //insert in Db;
                        new SetupTabletID().execute();
                        alertDialog.cancel();
                        Intent intent = new Intent(getApplicationContext(), AdminPageActivity.class);
                        startActivity(intent);

                    }
                });
            }

            Log.d(TAG, dialogTitle);
            Log.d(TAG, String.valueOf(showDialogOptions));
            alertDialog = alertBuilder.create();
            if (showDialogOptions || tabletIdDialog) {
                alertDialog.setCancelable(false);
                alertDialog.setCanceledOnTouchOutside(false);
            }

            if (tabletIdDialog) {
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    }
                });
            }


            alertDialog.show();
        }
    }


    private void clearDatabase(){
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());;
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            db.execSQL("delete from AIS_STATION_LIST");
            db.execSQL("delete from AIS_FIXED_STATION_POSITION");
            db.execSQL("delete from BASE_STATIONS");
        } catch (SQLiteException e){
            Toast.makeText(this, "Database Unavailable", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * An {@link AsyncTask} to insert the tablet ID entered in the Database table {@link DatabaseHelper#configParametersTable}. This
     * task is run only when {@link #tabletIdDialog} is <code>true</code>.
     */
    private class SetupTabletID extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                ContentValues tablet = new ContentValues();
                tablet.put(DatabaseHelper.parameterName, DatabaseHelper.tabletId);
                tablet.put(DatabaseHelper.parameterValue, tabletId);
                if((db.insert(DatabaseHelper.configParametersTable, null, tablet)) != -1){
                    return true;
                } else{
                    return false;
                }
            } catch (SQLiteException e){
                Log.d(TAG, "Error Inserting TabletID");
                e.printStackTrace();
                return false;
            }

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result){
                Log.d(TAG, "SetupTabletID AsyncTask: Database Error");
            }
        }
    }

    /**
     * An {@link AsyncTask} to insert the beta value passed to it ({@link #receivedBeta}) in the Database table
     * {@link DatabaseHelper#betaTable}. This task is run only when {@link #showDialogOptions} is <code>true</code>.
     */
    private class InsertBetaOnStartup extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            DatabaseHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            return updateBetaTable(receivedBeta, db);

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result){
                Log.d(TAG, "InsertBetaOnStartup Async Task: Database Error");
            }
        }
    }

    /**
     * Inserts the Beta value passed to it in the Database table {@link DatabaseHelper#betaTable}.
     * @param recdBeta the value of the angle {@link DatabaseHelper#beta} to insert in the Database.
     * @param db An instance of the local Database
     * @return <code>true</code> if the value is inserted successfully.
     */
    private boolean updateBetaTable(double recdBeta, SQLiteDatabase db){

        try {
            ContentValues beta = new ContentValues();
            beta.put(DatabaseHelper.beta, recdBeta);
            beta.put(DatabaseHelper.updateTime, String.valueOf(System.currentTimeMillis() - timeDiff));
            db.insert(DatabaseHelper.betaTable, null, beta);
            return true;
            /*long test = DatabaseUtils.queryNumEntries(db, DatabaseHelper.betaTable);
            Log.d(TAG, String.valueOf(test));
*/

        } catch(SQLException e){
            Log.d(TAG, "Error Updating Beta Table");
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Enable the Navigation Bar (back button, home button) at the bottom of the screen.
     */
    private void showNavigationBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * Called when the Dialog box is about to be destroyed. It unregisters the GPS {@link BroadcastReceiver}.
     */
    @Override
    public void onDestroy(){
        super.onDestroy();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }
}
