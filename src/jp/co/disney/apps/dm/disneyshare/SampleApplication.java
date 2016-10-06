package jp.co.disney.apps.dm.disneyshare;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.urbanairship.UAirship;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DebugLog.instance.outputLog("pushtes", "SampleApplication1");

        UAirship.takeOff(this, new UAirship.OnReadyCallback() {
            @Override
            public void onAirshipReady(UAirship airship) {
                DebugLog.instance.outputLog("pushtes", "SampleApplication2");
                // Enable user notifications
//                airship.getPushManager().setUserNotificationsEnabled(true);
                airship.getPushManager().setUserNotificationsEnabled(false);

            }
        });

        String channelId = UAirship.shared().getPushManager().getChannelId();
        DebugLog.instance.outputLog("pushtes", "SampleApplication3_My Application Channel ID: " + channelId);
    }

//    private static final String FIRST_RUN_KEY = "first_run";
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        DebugLog.instance.outputLog("pushtes", "SampleApplication");
//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        final boolean isFirstRun = preferences.getBoolean(FIRST_RUN_KEY, true);
//        if (isFirstRun) {
//            preferences.edit().putBoolean(FIRST_RUN_KEY, false).apply();
//        }
//
//        /*
//          Optionally, customize your config at runtime:
//             AirshipConfigOptions options = new AirshipConfigOptions.Builder()
//                    .setInProduction(!BuildConfig.DEBUG)
//                    .setDevelopmentAppKey("Your Development App Key")
//                    .setDevelopmentAppSecret("Your Development App Secret")
//                    .setProductionAppKey("Your Production App Key")
//                    .setProductionAppSecret("Your Production App Secret")
//                    .setNotificationAccentColor(ContextCompat.getColor(this, R.color.color_accent))
//                    .setNotificationIcon(R.drawable.ic_notification)
//                    .build();
//            UAirship.takeOff(this, options);
//         */
//
//
//        UAirship.takeOff(this, new UAirship.OnReadyCallback() {
//            @Override
//            public void onAirshipReady(UAirship airship) {
//                if (isFirstRun) {
//                    airship.getPushManager().setUserNotificationsEnabled(true);
//                }
//            }
//        });
//    }
}