package de.awi.floenavigation.admin;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.R;

/**
 * {@link ParameterViewActivity} is responsible for displaying a list of configuration parameters extracted from the local database
 * {@value DatabaseHelper#configParametersTable} table to display as a list when called from {@link ConfigurationActivity#onClickViewConfigurationParams(View)}
 */
public class ParameterViewActivity extends ListActivity {

    private static final String TAG = "ParameterViewActivity";

    /**
     * DatabaseHelper object
     */
    private DatabaseHelper dbHelper;
    /**
     * SQLiteDatabase object
     */
    private SQLiteDatabase db;
    /**
     * Array adapter used by {@link #setListAdapter(ListAdapter)}
     * to display the contents
     */
    private ArrayAdapter arrayAdapter;
    /**
     * array of {@link ParameterObject} objects
     */
    private ArrayList<ParameterObject> parameterObjects = new ArrayList<>();

    /**
     * onCreate method to initialize {@link #arrayAdapter} and set {@link #setListAdapter(ListAdapter)}
     * @param savedInstanceState used to save previous instance variables
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        arrayAdapter = new ParameterListAdapter(this, generateData());
        setListAdapter(arrayAdapter);

    }

    /**
     * Generate data to populate the list
     * Extracted from the local database table {@link DatabaseHelper#configParametersTable}
     * @return {@link ParameterObject} object with all the required contents
     */
    private ArrayList<ParameterObject> generateData(){
        Cursor paramsCursor = null;
        try{
            dbHelper = DatabaseHelper.getDbInstance(this);
            db = dbHelper.getReadableDatabase();
            paramsCursor = db.query(DatabaseHelper.configParametersTable,
                    new String[] {DatabaseHelper.parameterName, DatabaseHelper.parameterValue},
                    null,
                    null,
                    null, null, null);
            while(paramsCursor.moveToNext()){
               String paramName = paramsCursor.getString(paramsCursor.getColumnIndexOrThrow(DatabaseHelper.parameterName));
               String paramValue = String.valueOf(paramsCursor.getString(paramsCursor.getColumnIndexOrThrow(DatabaseHelper.parameterValue)));
                Log.d(TAG, paramName);
               Log.d(TAG, paramValue);
               if(paramName.equals(DatabaseHelper.lat_long_view_format)){
                   if(paramValue.equals("0")){
                       paramValue = getResources().getString(R.string.latLonDegMinSec);
                   } else if(paramValue.equals("1")){
                       paramValue = getResources().getString(R.string.latLonFraction);
                   }
               }
               if (paramName.equals(DatabaseHelper.initial_setup_time) || paramName.equals(DatabaseHelper.prediction_accuracy_threshold)
                    || paramName.equals(DatabaseHelper.packet_threshold_time)) {
                   paramValue = String.valueOf(Integer.parseInt(paramValue) / 60000);
                   paramValue = paramValue + " mins";
               } else if(paramName.equals(DatabaseHelper.error_threshold)){
                   paramValue = paramValue + " meters";
               }


               parameterObjects.add(new ParameterObject(paramName, paramValue));

            }
        } catch (SQLException e){
            Log.d(TAG, "Error Reading from Database");
        } finally {
            if (paramsCursor != null){
                paramsCursor.close();
            }
        }
        return parameterObjects;
        //arrayAdapter.notifyDataSetChanged();

    }
}

/**
 * Defines the list of information required from the {@value DatabaseHelper#configParametersTable}
 *
 */
class ParameterObject{
    private String parameterName;
    private String parameterValue;

    /**
     * Constructor to initialize the parameters
     * @param name {@value DatabaseHelper#parameterName}
     * @param value {@value DatabaseHelper#parameterValue}
     */
    ParameterObject(String name, String value){
        this.parameterName = name;
        this.parameterValue = value;
    }

    /**
     *
     * @return returns parameter name
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     *
     * @return returns parameter value
     */
    public String getParameterValue() {
        return parameterValue;
    }
}

/**
 * Defines the array adapter
 */
class ParameterListAdapter extends ArrayAdapter<ParameterObject>{

    private ArrayList<ParameterObject> parameters;
    private Context context;

    /**
     * Default constructor
     * @param con context of the activity
     * @param params parameter list object
     */
    public ParameterListAdapter(Context con, ArrayList<ParameterObject> params){
        super(con, R.layout.parameter_list_item, params);
        this.context = con;
        this.parameters = params;
    }

    /**
     * sets the view type
     * @param position position in the list
     * @param convertView not used
     * @param parent parent view
     * @return returns the view name
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.parameter_list_item, parent, false);

        TextView paramName = (TextView)rowView.findViewById(R.id.column1);
        TextView paramValue = rowView.findViewById(R.id.column2);
        RelativeLayout item = rowView.findViewById(R.id.item);

        paramName.setText(parameters.get(position).getParameterName());
        paramValue.setText(parameters.get(position).getParameterValue());

        return rowView;
    }
}
