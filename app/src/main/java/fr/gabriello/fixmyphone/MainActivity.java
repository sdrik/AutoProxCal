package fr.gabriello.fixmyphone;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private ToggleButton mTestButton;

    public static class PrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        TextView value = (TextView)findViewById(R.id.sensorNameValue);
        value.setText(mSensor.getName());
        value = (TextView)findViewById(R.id.sensorVendorValue);
        value.setText(mSensor.getVendor());

        mTestButton = (ToggleButton)findViewById(R.id.testButton);
        mTestButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    enableProximitySensor();
                } else {
                    disableProximitySensor();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTestButton.isChecked()) {
            enableProximitySensor();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableProximitySensor();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            View layout = findViewById(R.id.mainLayout);
            Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
            if (event.values[0] == 0) {
                layout.setBackgroundColor(getResources().getColor(R.color.colorNear));
                vibrator.vibrate(999999);
            } else {
                layout.setBackgroundColor(Color.WHITE);
                vibrator.cancel();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onClickCalibrateButton(View view) {
        startService(new Intent(this, ProximityCalibrateService.class));
    }

    private void enableProximitySensor() {
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void disableProximitySensor() {
        mSensorManager.unregisterListener(this);
        Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        vibrator.cancel();
        View layout = findViewById(R.id.mainLayout);
        layout.setBackgroundColor(Color.WHITE);
    }

}
