package de.awi.floenavigation.admin;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.awi.floenavigation.helperclasses.ActionBarActivity;
import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.R;

/**
 * This Activity deals with configuring and displaying of the important internal parameters of the system
 * These parameters are available in {@value DatabaseHelper#configParametersTable}
 * The fields are changed for entering the value w.r.t the parameter selected. Different fields are present such as radio buttons
 * for changing lat-lon display format {@value DatabaseHelper#lat_long_view_format}, seek bar view available for {@value DatabaseHelper#error_threshold},
 * edit texts available for {@value DatabaseHelper#tabletId} and so on.
 *
 */
public class ConfigurationActivity extends ActionBarActivity {

    private static final String TAG = "ConfigurationActivity";
    /**
     * View used to enter the value of the parameter selected
     */
    private EditText valueField;
    /**
     * flag used for setting up the display mode of geographical coordinate
     * in deg:min:sec or in decimal format
     */
    private boolean coordinateTypeDegMinSec = false;
    /**
     * flag used to enable the visibility of the {@link #valueField} based on the paramter selected
     */
    private boolean isNormalParam = true;
    /**
     * flag used to display time units when time related parameter is selected
     */
    private boolean isTimeRangeParam = false;
    /**
     * flag used to display distance units when distance related parameter is selected
     */
    private boolean isDistanceRangeParam = false;
    /**
     * Used for accessing the spinner item position
     */
    private int spinnerItem = 0;
    /**
     * Used for adding default value for the seek bar value
     */
    private int MIN_VALUE;
    /**
     * Default value for minimum time
     */
    private int TIME_MIN_VALUE = 10;
    /**
     * Default value for maximum time
     */
    private int TIME_MAX_VALUE = 50;
    /**
     * Default value for minimum distance
     */
    private int DISTANCE_MIN_VALUE = 0;
    /**
     * Default value for maximum distance
     */
    private int DISTANCE_MAX_VALUE = 100;
    /**
     * Default value for minimum digits for {@value DatabaseHelper#decimal_number_significant_figures}
     */
    private int SIGNIFICANT_FIGURES_MIN_VALUE = 0;
    /**
     * Default value for maximum digits for {@value DatabaseHelper#decimal_number_significant_figures}
     */
    private int SIGNIFICANT_FIGURES_MAX_VALUE = 10;
    /**
     * Used for displaying the unit string
     */
    private String units = "";
    /**
     * Default value for the seek bar
     */
    private int progressValue = MIN_VALUE;

    /**
     * This function is responsible for setting up the layout and the default parameter and the value field is visible
     * Based on the parameter selected from a list (Spinner), the value field changes accordingly
     * Visibility of the fields are governed by the spinner position of the selected item
     * The function makes sure that corresponding flags are initialized {@link #isNormalParam}, {@link #isDistanceRangeParam},
     * {@link #isTimeRangeParam} properly
     * @param savedInstanceState Used to store the previous saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        populateParameters();
        final Spinner paramType = findViewById(R.id.parameterSelect);

        final SeekBar initialTimeRange = findViewById(R.id.seekBarInitialTimeRange);
        final TextView progressBarValue = findViewById(R.id.progressBarInitValue);
        initialTimeRange.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue = MIN_VALUE + progress;
                progressBarValue.setText(String.valueOf(progressValue) + " " + units);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        paramType.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //EditText paramValue = findViewById(R.id.parameterValue);
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                //hideSoftKeyboard(getCallingActivity());
                Log.d(TAG, "OnTouch");

                return false;
            }
        });
        paramType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position == 0) {
                    findViewById(R.id.normalParam).setVisibility(View.GONE);
                    findViewById(R.id.normalInitialRangeParam).setVisibility(View.VISIBLE);
                    findViewById(R.id.latLonViewParam).setVisibility(View.GONE);
                    isDistanceRangeParam = true;
                    isNormalParam = false;
                    MIN_VALUE = DISTANCE_MIN_VALUE;
                    initialTimeRange.setMax(DISTANCE_MAX_VALUE);
                    units = "meters";
                    progressBarValue.setText(String.valueOf(DISTANCE_MIN_VALUE) + " " + units);

                } else if(position == 1 || position == 4){
                    findViewById(R.id.normalParam).setVisibility(View.GONE);
                    findViewById(R.id.normalInitialRangeParam).setVisibility(View.VISIBLE);
                    findViewById(R.id.latLonViewParam).setVisibility(View.GONE);
                    isTimeRangeParam = true;
                    isNormalParam = false;
                    MIN_VALUE = TIME_MIN_VALUE;
                    initialTimeRange.setMax(TIME_MAX_VALUE);
                    units = "minutes";
                    progressBarValue.setText(String.valueOf(TIME_MIN_VALUE) + " " + units);
                } else if (position == 2){
                    findViewById(R.id.normalParam).setVisibility(View.GONE);
                    findViewById(R.id.latLonViewParam).setVisibility(View.VISIBLE);
                    findViewById(R.id.normalInitialRangeParam).setVisibility(View.GONE);
                    isNormalParam = false;
                    isTimeRangeParam = false;
                    isDistanceRangeParam = false;
                } else if (position == 3){
                    findViewById(R.id.normalParam).setVisibility(View.GONE);
                    findViewById(R.id.normalInitialRangeParam).setVisibility(View.VISIBLE);
                    findViewById(R.id.latLonViewParam).setVisibility(View.GONE);
                    isTimeRangeParam = true;
                    isNormalParam = false;
                    MIN_VALUE = SIGNIFICANT_FIGURES_MIN_VALUE;
                    initialTimeRange.setMax(SIGNIFICANT_FIGURES_MAX_VALUE);
                    units = " ";
                    progressBarValue.setText(String.valueOf(SIGNIFICANT_FIGURES_MIN_VALUE) + " " + units);
                } else if(position == 6){
                    findViewById(R.id.normalParam).setVisibility(View.VISIBLE);
                    findViewById(R.id.latLonViewParam).setVisibility(View.GONE);
                    findViewById(R.id.normalInitialRangeParam).setVisibility(View.GONE);
                    EditText paramValue = findViewById(R.id.parameterValue);
                    paramValue.setInputType(InputType.TYPE_CLASS_NUMBER);
                    spinnerItem = position;
                    isTimeRangeParam = false;
                    isDistanceRangeParam = false;
                    isNormalParam = true;
                } else {
                    findViewById(R.id.normalParam).setVisibility(View.VISIBLE);
                    findViewById(R.id.latLonViewParam).setVisibility(View.GONE);
                    findViewById(R.id.normalInitialRangeParam).setVisibility(View.GONE);
                    EditText paramValue = findViewById(R.id.parameterValue);
                    paramValue.setInputType(InputType.TYPE_CLASS_TEXT);
                    spinnerItem = position;
                    isTimeRangeParam = false;
                    isDistanceRangeParam = false;
                    isNormalParam = true;
                }
                spinnerItem = position;
                initialTimeRange.setProgress(0);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                return;
            }
        });
    }

    /**
     * Based on the {}
     * @param view The view that has been clicked {@link #isNormalParam}, {@link #isDistanceRangeParam}, {@link #isTimeRangeParam}
     * flags, different operations are undertaken such as validation of the entered data, adding an offset to the data
     * and eventually updating the values to the internal database
     */
    public void onClickConfigurationParamsconfirm(View view) {
        try {
            SQLiteOpenHelper dbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            if(isNormalParam){
                valueField = findViewById(R.id.parameterValue);
                if(validateValueField(valueField)){
                    String paramName = DatabaseHelper.configurationParameters[spinnerItem];
                    String paramValue = valueField.getText().toString();
                    updateDatabaseTable(db, paramName, paramValue);
                    Toast.makeText(getApplicationContext(), "Configuration Saved", Toast.LENGTH_SHORT).show();

                }else {
                    Toast.makeText(this, "Please Enter a Correct Parameter Value", Toast.LENGTH_SHORT).show();
                }
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            } else if (isTimeRangeParam) {
                String paramName = DatabaseHelper.configurationParameters[spinnerItem];
                Log.d(TAG, String.valueOf(progressValue * 60 * 1000));
                updateDatabaseTable(db, paramName, String.valueOf(progressValue * 60 * 1000));
                Toast.makeText(getApplicationContext(), "Configuration Saved", Toast.LENGTH_SHORT).show();

            } else if (isDistanceRangeParam){
                String paramName = DatabaseHelper.configurationParameters[spinnerItem];
                updateDatabaseTable(db, paramName, String.valueOf(progressValue));
                Toast.makeText(getApplicationContext(), "Configuration Saved", Toast.LENGTH_SHORT).show();

            }
            else{

                String paramName = DatabaseHelper.configurationParameters[spinnerItem];
                String paramValue;
                if(coordinateTypeDegMinSec){
                    paramValue = "0";

                } else {
                    paramValue = "1";
                }
                Log.d(TAG, paramName);
                Log.d(TAG, paramValue);
                updateDatabaseTable(db, paramName, paramValue);
                Toast.makeText(getApplicationContext(), "Configuration Saved", Toast.LENGTH_SHORT).show();
            }





        } catch(SQLiteException e){
            Log.d(TAG, "Database Error");
            e.printStackTrace();
        }
    }

    /**
     * Function initializes the spinner list with the parameters present in {@value DatabaseHelper#configParametersTable}
     */
    private void populateParameters(){
        List<String> parameterList = new ArrayList<String>();

        parameterList.addAll(Arrays.asList(DatabaseHelper.configurationParameters));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.spinner_item, parameterList
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner parameterType = findViewById(R.id.parameterSelect);
        parameterType.setAdapter(adapter);
    }

    /**
     * Function used to update the local internal database
     * @param db SQLiteDatabase object
     * @param parameterName name of the parameter
     * @param inputValue corresponding value
     */
    private void updateDatabaseTable(SQLiteDatabase db, String parameterName, String inputValue){
        ContentValues configParamsContents = new ContentValues();
        configParamsContents.put(DatabaseHelper.parameterName, parameterName);
        configParamsContents.put(DatabaseHelper.parameterValue, inputValue);
        db.update(DatabaseHelper.configParametersTable, configParamsContents, DatabaseHelper.parameterName + " = ?",
                new String[] {parameterName});

    }

    /**
     * onClick listener to display the internal parameters with their most updated value
     * Used for verfication purpose
     * The Listener starts {@link ParameterViewActivity} activity when the view is pressed
     * @param view The view that has been clicked
     */
    public void onClickViewConfigurationParams(View view) {
        Intent parameterActivityIntent = new Intent(this, ParameterViewActivity.class);
        startActivity(parameterActivityIntent);
    }

    /**
     * Validate the text field for empty string
     * @param valueField value received as an input argument
     * @return <code>true</code> is valid value present
     */
    private boolean validateValueField(EditText valueField) {
        return !TextUtils.isEmpty(valueField.getText().toString());
    }

    /**
     * Used to update the flag {@link #coordinateTypeDegMinSec} based on the radio button pressed
     * @param view The view that has been clicked
     */
    public void onClickDegreeFraction(View view) {
        coordinateTypeDegMinSec = false;
    }

    /**
     * Used to update the flag {@link #coordinateTypeDegMinSec} based on the radio button pressed
     * @param view The view that has been clicked
     */
    public void onClickDegMinSec(View view) {
        coordinateTypeDegMinSec = true;
    }
}
