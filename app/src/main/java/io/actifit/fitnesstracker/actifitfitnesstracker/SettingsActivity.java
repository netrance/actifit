package io.actifit.fitnesstracker.actifitfitnesstracker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import static io.actifit.fitnesstracker.actifitfitnesstracker.MainActivity.isStepSensorPresent;

public class SettingsActivity extends AppCompatActivity {

    private NumberPicker hourOptions, minOptions;
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private String fullSPPay = "full_SP_Pay";
    private String sbdSPPay = "50_50_SBD_SP_Pay";

    /*@Bind(R.id.main_toolbar)
    Toolbar toolbar;*/


    /*public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        case "shared_network":
        if (sharedPreferences.getBoolean(key, false) == true) {
            com.exerpic.si.aar.Activity.create(this);
        } else {
            com.exerpic.si.aar.Activity.cancel(this);
        }

        break;
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        //oxylabs preferences
        /*SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.edit().putBoolean("shared_network", com.exerpic.si.aar.Activity.isEnabled()).apply();
        PreferenceManager.addPreferencesFromResource(R.xml.prefs);*/

        //display version number
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            TextView version_info = findViewById(R.id.version_info);
            version_info.setText(getString(R.string.app_version_string) + " :" + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //grab instances of settings components
        final RadioButton metricSysRadioBtn = findViewById(R.id.metric_system);
        final RadioButton usSystemRadioBtn = findViewById(R.id.us_system);

        final CheckBox aggBgTrackingChckBox = findViewById(R.id.background_tracking);

        final CheckBox donateCharityChckBox = findViewById(R.id.donate_charity);

        final CheckBox reminderSetChckBox = findViewById(R.id.reminder_settings);

        final RadioButton deviceSensorsBtn = findViewById(R.id.device_sensors);
        final RadioButton fitbitBtn = findViewById(R.id.fitbit);
        final LinearLayout aggModeSection = findViewById(R.id.background_tracking_section);

        final LinearLayout fitbitSettingsSection = findViewById(R.id.fitbit_settings_section);
        final CheckBox fitbitMeasurementsChckBox = findViewById(R.id.fitbit_measurements);

        final RadioButton fullSPayRadioBtn = findViewById(R.id.full_sp_pay);
        final RadioButton sbdSPPayRadioBtn = findViewById(R.id.sbd_sp_pay);


        Spinner charitySelected = findViewById(R.id.charity_options);

        //retrieving prior settings if already saved before
        SharedPreferences sharedPreferences = getSharedPreferences("actifitSets",MODE_PRIVATE);

        String currentSystem = (sharedPreferences.getString("activeSystem",""));

        //check which is the current active system
        //if the setting is manually set as US System or default Metric value (else)
        if (currentSystem.equals(getString(R.string.us_system))){
            usSystemRadioBtn.setChecked(true);
        }else{
            metricSysRadioBtn.setChecked(true);
        }

        //check which pay mode for reports to be used
        String reportPayMode = sharedPreferences.getString("reportSTEEMPayMode",sbdSPPay);
        if (reportPayMode.equals(fullSPPay)){
            fullSPayRadioBtn.setChecked(true);
        }else{
            sbdSPPayRadioBtn.setChecked(true);
        }


        //check which data source is active now

        String dataTrackingSystem = sharedPreferences.getString("dataTrackingSystem","");
        if (dataTrackingSystem.equals(getString(R.string.fitbit_tracking))){
            fitbitBtn.setChecked(true);

            //also hide aggressive mode if fitbit is on, and show fitbit configuration
            aggModeSection.setVisibility(View.INVISIBLE);
            fitbitSettingsSection.setVisibility(View.VISIBLE);
        }else{
            deviceSensorsBtn.setChecked(true);
            //alternatively hide fitbit settings and show aggressive mode settings
            aggModeSection.setVisibility(View.VISIBLE);
            fitbitSettingsSection.setVisibility(View.INVISIBLE);
        }

        RadioGroup trackingModeRadiogroup = findViewById(R.id.tracking_mode_radiogroup);

        //capture change event for radiobutton group to reflect on user available options
        trackingModeRadiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if (deviceSensorsBtn.isChecked()){
                    aggModeSection.setVisibility(View.VISIBLE);
                    fitbitSettingsSection.setVisibility(View.INVISIBLE);
                }else{
                    aggModeSection.setVisibility(View.INVISIBLE);
                    fitbitSettingsSection.setVisibility(View.VISIBLE);
                }
            }
        });

        //grab aggressive mode setting and update checkbox accordingly
        String aggModeEnabled = sharedPreferences.getString("aggressiveBackgroundTracking",getString(R.string.aggr_back_tracking_off));
        Log.d(MainActivity.TAG,">>>>[Actifit] Agg Mode:"+aggModeEnabled);
        Log.d(MainActivity.TAG,">>>>[Actifit] Agg Mode Test:"+aggModeEnabled.equals(getString(R.string.aggr_back_tracking_on)));

        aggBgTrackingChckBox.setChecked(aggModeEnabled.equals(getString(R.string.aggr_back_tracking_on)));

        //grab fitbit setting and update checkbox accordingly
        String fitbitMeasurements = sharedPreferences.getString("fitbitMeasurements",getString(R.string.fitbit_measurements_on));
        fitbitMeasurementsChckBox.setChecked(fitbitMeasurements.equals(getString(R.string.fitbit_measurements_on)));


        final Activity currentActivity = this;

        //need to update the info based on charity selection
        charitySelected.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            TextView charityInfo = findViewById(R.id.charity_info);
            Spinner charitySelected = findViewById(R.id.charity_options);

            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String fullUrl = getString(R.string.steemit_url)+'@'+((Charity)charitySelected.getSelectedItem()).getCharityName();
                charityInfo.setText(fullUrl);
                charityInfo.setMovementMethod(LinkMovementMethod.getInstance());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                charityInfo.setText("");
            }
        });


        Button BtnSaveSettings = findViewById(R.id.btn_save_settings);
        BtnSaveSettings.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View arg0) {
                //need to adjust the selection of the sensors and store it

                //store as new preferences
                SharedPreferences sharedPreferences = getSharedPreferences("actifitSets",MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                //test for which option the user has set
                if (metricSysRadioBtn.isChecked()) {
                    editor.putString("activeSystem", getString(R.string.metric_system));
                }else{
                    editor.putString("activeSystem", getString(R.string.us_system));
                }

                //store selected STEEM pay mode

                //check which pay mode for reports to be used and store it
                if (fullSPayRadioBtn.isChecked()){
                    editor.putString("reportSTEEMPayMode", fullSPPay);
                }else{
                    editor.putString("reportSTEEMPayMode", sbdSPPay);
                }

                //store selected tracking system
                if (fitbitBtn.isChecked()) {
                    editor.putString("dataTrackingSystem", getString(R.string.fitbit_tracking));

                    //also deactivate running sensors if any instance is running
                    try {
                        ActivityMonitorService mSensorService = MainActivity.getmSensorService();
                        if (mSensorService != null) {
                            stopService(MainActivity.getmServiceIntent());
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }else{
                    editor.putString("dataTrackingSystem", getString(R.string.device_tracking));
                }


                //PowerManager pm = ActivityMonitorService.getPowerManagerInstance();
                PowerManager.WakeLock  wl = ActivityMonitorService.getWakeLockInstance();

                //we need to enable aggressive checking only if device sensors are functioning,
                //otherwise it's pointless
                if (aggBgTrackingChckBox.isChecked()){
                    editor.putString("aggressiveBackgroundTracking", getString(R.string.aggr_back_tracking_on));

                }else{
                    editor.putString("aggressiveBackgroundTracking", getString(R.string.aggr_back_tracking_off));
                    //enable wake lock to ensure tracking functions in the background
                    if (wl!=null && wl.isHeld()) {
                        Log.d(MainActivity.TAG,">>>>[Actifit]Settings AGG MODE OFF");
                        wl.release();
                    }
                }

                //reset value first
                editor.putString("selectedCharity", "");

                //check if charity mode is on and a charity has been selected
                if (donateCharityChckBox.isChecked()){
                    Spinner charitySelected = findViewById(R.id.charity_options);
                    if (charitySelected.getSelectedItem() !=null){
                        editor.putString("selectedCharity", ((Charity)charitySelected.getSelectedItem()).getCharityName());
                        editor.putString("selectedCharityDisplayName", charitySelected.getSelectedItem().toString());
                    }
                }

                //unset alarm and the need to restart Actifit notification reminder after reboot
                alarmManager = (AlarmManager) getApplicationContext()
                        .getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(getApplicationContext(), ReminderNotificationService.class);
                alarmIntent = PendingIntent.getService(getApplicationContext()
                        , ReminderNotificationService.NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                //unset any existing alarms first
                alarmManager.cancel(alarmIntent);

                //check if reminder setting is on
                if (reminderSetChckBox.isChecked()) {
                    editor.putString("selectedReminderHour", "" + hourOptions.getValue());
                    editor.putString("selectedReminderMin", "" + minOptions.getValue());

                    //set the alarm at user defined value
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    calendar.set(Calendar.HOUR_OF_DAY, hourOptions.getValue());
                    calendar.set(Calendar.MINUTE, minOptions.getValue());

                    //PendingIntent.getService(currentActivity, ReminderNotificationService.NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    intent.putExtra("NOTIFICATION_ID", ReminderNotificationService.NOTIFICATION_ID);

                    Log.d(MainActivity.TAG,">>>>[Actifit]: set alarm manager"+hourOptions.getValue()+" "+minOptions.getValue());

                    alarmIntent = PendingIntent.getBroadcast(getApplicationContext()
                            , 0, intent, 0);

                    alarmManager = (AlarmManager) getApplicationContext()
                            .getSystemService(Context.ALARM_SERVICE);

                    //specify alarm interval to be every 24 hours at user defined slot
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                            1000 * 60 * 60 * 24, alarmIntent);
                }

                //store fitbit setting to see if user wants to grab measurements too
                CheckBox fitbitMeasurements = findViewById(R.id.fitbit_measurements);
                if (fitbitMeasurements.isChecked()){
                    editor.putString("fitbitMeasurements", getString(R.string.fitbit_measurements_on));
                }else{
                    editor.putString("fitbitMeasurements", getString(R.string.fitbit_measurements_off));
                }

                editor.apply();

                currentActivity.finish();

            }
        });

        //grab charity list
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        // This holds the url to connect to the API and grab the balance.
        String charityUrl = getString(R.string.charity_list_api_url);

        JsonArrayRequest charitiesRequest = new JsonArrayRequest(Request.Method.GET,
                charityUrl, null, new Response.Listener<JSONArray>(){

            @Override
            public void onResponse(JSONArray transactionListArray) {

                ArrayList<Charity> transactionList = new ArrayList<Charity>();
                Spinner charityOptions = findViewById(R.id.charity_options);
                // Handle the result
                try {

                    for (int i = 0; i < transactionListArray.length(); i++) {
                        // Retrieve each JSON object within the JSON array
                        JSONObject jsonObject = transactionListArray.getJSONObject(i);

                        // Adds strings from the current object to the data string
                        transactionList.add(new Charity(jsonObject.getString("charity_name"), jsonObject.getString("display_name")));
                    }
                    // convert content to adapter display, and render it
                    ArrayAdapter<Charity> arrayAdapter  =
                            new ArrayAdapter<Charity>(getApplicationContext(),android.R.layout.simple_list_item_1, transactionList ){
                                @NonNull
                                @Override
                                public View getView(int position, View convertView, @NonNull ViewGroup parent){
                                    // Get the Item from ListView
                                    View view = super.getView(position, convertView, parent);

                                    // Initialize a TextView for ListView each Item
                                    TextView tv = view.findViewById(android.R.id.text1);

                                    // Set the text color of TextView (ListView Item)
                                    tv.setTextColor(Color.BLACK);
                                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);

                                    // Generate ListView Item using TextView
                                    return view;
                                }
                            };

                    charityOptions.setAdapter(arrayAdapter);

                    //choose a charity if one is already selected before

                    SharedPreferences sharedPreferences = getSharedPreferences("actifitSets",MODE_PRIVATE);

                    String currentCharity = (sharedPreferences.getString("selectedCharity",""));
                    String currentCharityDisplayName = (sharedPreferences.getString("selectedCharityDisplayName",""));

                    if (!currentCharity.equals("")){
                        Spinner charitySelected = findViewById(R.id.charity_options);
                        TextView charityInfo = findViewById(R.id.charity_info);

                        donateCharityChckBox.setChecked(true);
                        charitySelected.setSelection(arrayAdapter.getPosition(new Charity(currentCharity,currentCharityDisplayName)), false);
                        String fullUrl = getString(R.string.steemit_url)+'@'+currentCharity;
                        charityInfo.setText(fullUrl);
                        charityInfo.setMovementMethod(LinkMovementMethod.getInstance());
                    }

                    //actifitTransactions.setText("Response is: "+ response);
                }catch (Exception e) {
                    Log.d(MainActivity.TAG,">>>>[Actifit]: Volley error"+e.getMessage());
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(MainActivity.TAG,">>>>[Actifit]: Volley response error"+error.getMessage());
                error.printStackTrace();
            }
        });

        // Add charities request to be processed
        queue.add(charitiesRequest);


        //set proper reminder times

        hourOptions = findViewById(R.id.reminder_hour_options);

        hourOptions.setMinValue(0);
        hourOptions.setMaxValue(23);
        //hourOptions.setWrapSelectorWheel(false);

        minOptions = findViewById(R.id.reminder_min_options);

        minOptions.setMinValue(0);
        minOptions.setMaxValue(59);
        //minOptions.setWrapSelectorWheel(false);

        //formatting display of reminder times to add extra left zeros (hours and mins)
        NumberPicker.Formatter formatter = new NumberPicker.Formatter(){
            @Override
            public String format(int i) {
                if (i<10){
                    return "0"+i;
                }
                return ""+i;
            }
        };

        hourOptions.setFormatter(formatter);
        minOptions.setFormatter(formatter);

        //get pre-saved values for reminder setting
        String reminderHour = (sharedPreferences.getString("selectedReminderHour",""));
        String reminderMin = (sharedPreferences.getString("selectedReminderMin",""));

        //check which is the current active system
        //if the setting is manually set as US System or default Metric value (else)
        if (!reminderHour.equals("") && !reminderMin.equals("")){
            try {
                hourOptions.setValue(Integer.parseInt(reminderHour));
                minOptions.setValue(Integer.parseInt(reminderMin));
                //we were able to grab proper values, set as checked
                reminderSetChckBox.setChecked(true);
            }catch(Exception e){
                e.printStackTrace();
            }
        }else{
            metricSysRadioBtn.setChecked(true);
        }

    }


}
