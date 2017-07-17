package fr.gabriello.fixmyphone;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;

@android.support.annotation.RequiresApi(api = Build.VERSION_CODES.N)
public class ResetRadioTileService extends TileService {

    public final static String ACTION_UPDATE_TILE = "fr.gabriello.fixmyphone.ACTION_UPDATE_TILE";
    private BroadcastReceiver mReceiver = null;
    private boolean mResetting = false;

    public static class ResetRadioService extends IntentService {

        private PhoneStateListener mListener = null;
        private boolean mDown = false;
        private boolean mUp = false;

        public ResetRadioService() {
            super("ResetRadioService");
        }

        @Override
        public void onCreate() {
            Log.d("ResetRadioService", "onCreate()");
            super.onCreate();

            mDown = false;
            mUp = false;

            final Context context = this;
            final TelephonyManager telephony = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

            mListener = new PhoneStateListener() {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    Log.d("PhoneStateListener", "onServiceStateChanged("+String.valueOf(serviceState.getState())+")");
                    super.onServiceStateChanged(serviceState);
                    switch (serviceState.getState()) {
                        case ServiceState.STATE_EMERGENCY_ONLY:
                        case ServiceState.STATE_IN_SERVICE:
                            if (mDown)
                                mUp = true;
                            break;
                        case ServiceState.STATE_OUT_OF_SERVICE:
                        case ServiceState.STATE_POWER_OFF:
                            mDown = true;
                            break;
                    }
                }
            };
        }

        @Override
        public void onDestroy() {
            Log.d("ResetRadioService", "onDestroy()");
            super.onDestroy();

            if (mListener != null) {
                final TelephonyManager telephony = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                telephony.listen(mListener, PhoneStateListener.LISTEN_NONE);
                mListener = null;
            }
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            Log.d("ResetRadioService", "onHandleIntent()");
            if (intent == null) return;

            ResetRadioTileService.setResetting(this, true);

            boolean success = false;
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream STDIN = new DataOutputStream(process.getOutputStream());
                STDIN.write("pkill -f /system/bin/cbd; exit $?\n".getBytes());
                STDIN.flush();
                success = (process.waitFor() == 0);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            if (success) {
                Log.d("ResetRadioService", "Reset radio done. Waiting for phone service to recover...");
                TelephonyManager telephony = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                telephony.listen(mListener, PhoneStateListener.LISTEN_SERVICE_STATE);
                while (!mDown || !mUp) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                Log.d("ResetRadioService", "Stopped waiting.");
                telephony.listen(mListener, PhoneStateListener.LISTEN_NONE);
                mListener = null;
            } else {
                Log.d("ResetRadioService", "Reset radio failed.");
            }
            ResetRadioTileService.setResetting(this, false);
        }
    }

    @Override
    public void onCreate() {
        Log.d("ResetRadioTileService", "onCreate()");
        super.onCreate();
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mResetting = intent.getBooleanExtra("resetting", false);
                Log.d("BroadcastReceiver", "onReceive("+String.valueOf(mResetting)+")");
                TileService.requestListeningState(context, new ComponentName(context, ResetRadioTileService.class));
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(ACTION_UPDATE_TILE));
        setResetting(this, false);
    }

    @Override
    public void onDestroy() {
        Log.d("ResetRadioTileService", "onDestroy()");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        mReceiver = null;
        super.onDestroy();
    }

    /*
    @Override
    public void onTileAdded() {
        Log.d("ResetRadioTileService", "onTileAdded()");
        super.onTileAdded();
    }
    */

    /*
    @Override
    public void onTileRemoved() {
        Log.d("ResetRadioTileService", "onTileRemoved()");
        super.onTileRemoved();
    }
    */

    @Override
    public void onStartListening() {
        Log.d("ResetRadioTileService", "onStartListening("+String.valueOf(mResetting)+")");
        super.onStartListening();
        Tile tile = getQsTile();
        tile.setState(mResetting ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onStopListening() {
        Log.d("ResetRadioTileService", "onStopListening()");
        super.onStopListening();
    }

    @Override
    public void onClick() {
        Log.d("ResetRadioTileService", "onClick()");
        super.onClick();

        if (mResetting)
            return;

        Context context = getApplicationContext();
        context.startService(new Intent(context, ResetRadioService.class));
    }

    public static void setResetting(Context context, boolean resetting) {
        Log.d("ResetRadioTileService", "setResetting("+String.valueOf(resetting)+")");
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_UPDATE_TILE).putExtra("resetting", resetting));
    }

}