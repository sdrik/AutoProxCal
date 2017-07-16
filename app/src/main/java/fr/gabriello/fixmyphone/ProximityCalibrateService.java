package fr.gabriello.fixmyphone;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;

public class ProximityCalibrateService extends IntentService {

    public ProximityCalibrateService() {
        super("ProximityCalibrateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;

        SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        SensorEventListener listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {}
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        boolean success = false;
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream STDIN = new DataOutputStream(process.getOutputStream());
            STDIN.write("echo 0 > /sys/devices/virtual/sensors/proximity_sensor/prox_cal && echo 1 > /sys/devices/virtual/sensors/proximity_sensor/prox_cal; exit $?\n".getBytes());
            STDIN.flush();
            success = (process.waitFor() == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        sensorManager.unregisterListener(listener);

        if (success) {
            Application.toast(this, R.string.calibration_success, Toast.LENGTH_SHORT);
        } else {
            Application.toast(this, R.string.calibration_failure, Toast.LENGTH_LONG);
        }
    }

    public static class BootCompletedReceiver extends BroadcastReceiver {
        private static final String PREFS_CALIBRATE_ON_BOOT = "calibrate_on_boot";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (context == null) return;
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            if ((settings != null) && settings.getBoolean(PREFS_CALIBRATE_ON_BOOT, false)) {
                context.startService(new Intent(context, ProximityCalibrateService.class));
            }
        }
    }

}
