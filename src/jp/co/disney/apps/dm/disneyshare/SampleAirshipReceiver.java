/* Copyright 2016 Urban Airship and Contributors */

package jp.co.disney.apps.dm.disneyshare;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.urbanairship.AirshipReceiver;
import com.urbanairship.UAirship;
import com.urbanairship.push.PushMessage;
import com.urbanairship.richpush.RichPushMessage;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.Set;

public class SampleAirshipReceiver extends AirshipReceiver {
//public class SampleAirshipReceiver extends AirshipReceiver implements PushNotificationBuilder{
//public class SampleAirshipReceiver extends BasicPushNotificationBuilder {


    private static final String TAG = "SampleAirshipReceiver";
    
    /**
     * Intent action sent as a local broadcast to update the channel.
     */
    public static final String ACTION_UPDATE_CHANNEL = "ACTION_UPDATE_CHANNEL";

    @Override
    protected void onChannelCreated(@NonNull Context context, @NonNull String channelId) {
        Log.i(TAG, "Channel created. Channel Id:" + channelId + ".");

        // Broadcast that the channel was created. Used to refresh the channel ID on the home fragment
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_UPDATE_CHANNEL));
    }

    @Override
    protected void onChannelUpdated(@NonNull Context context, @NonNull String channelId) {
        Log.i(TAG, "Channel updated. Channel Id:" + channelId + ".");

        // Broadcast that the channel was updated. Used to refresh the channel ID on the home fragment
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_UPDATE_CHANNEL));
    }

    @Override
    protected void onChannelRegistrationFailed(Context context) {
        Log.i(TAG, "Channel registration failed.");
    }

    @Override
    protected void onPushReceived(@NonNull Context context, @NonNull PushMessage message, boolean notificationPosted) {
//        Log.i(TAG, "Received push message. Alert: " + message.getAlert() + ". posted notification: " + notificationPosted);
        Log.i(TAG, "Received push message. Alert: " + message.getAlert()
               + "_1 "+ message.getRichPushMessageId()
                + "_2 "+ message.getAlert()
                + "_3 "+ message.getRichPushMessageId()
                + "_4 "+ message.getCanonicalPushId()
                + "_5  "+ message.getCategory()
                + "_6 "+ message.getInteractiveActionsPayload()
                + "_7 "+ message.getInteractiveNotificationType()
                + "_8 "+ message.getMetadata()
                + "_9 "+ message.getPublicNotificationPayload()
                + "_10 "+ message.getSendId()
                + "_11 "+ message.getStylePayload()
                + "_12 "+ message.getSummary()
                + "_13 "+ message.getCategory()
                + "_14 "+ message.getTitle()
                + "_15 "+ message.getWearablePayload()
                + "_16 "+ message.toString()
                + "_17 "+ message.describeContents()
                + "_18 "+ message.getSendId()
                + "_19 "+ message.getInAppMessage()
                + "_20 "+ message.getPriority()
                + "_21 "+ message.getVisibility()
        );

//        Bundle bundle = getResultExtras(true);
//        extras.putString("key", "value");
//        setResultExtras(extras);

        Bundle bundle = message.getPushBundle();
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
//            Log.e(LOG_TAG,"Dumping Intent start");
            while (it.hasNext()) {
                String key = it.next();
                Log.i(TAG,"[" + key + "=" + bundle.get(key)+"]");
//                Log.e(LOG_TAG,"[" + key + "=" + bundle.get(key)+"]");
            }
//            Log.e(LOG_TAG,"Dumping Intent end");
        }

        String mType =  (bundle.get("messageType")).toString();
        String ContentText ="";
                if(mType.equals("1")){
                    ContentText = "友達がグループに参加しました。";
                }else{
                    ContentText = "ディズニーシェアの更新情報を確認しましょう";
                }


        Intent ni = new Intent(Intent.ACTION_MAIN);
        ni.addCategory(Intent.CATEGORY_LAUNCHER);
        ni.setClassName(context.getPackageName(), MainActivity.class.getName());
        ni.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 0, ni, PendingIntent.FLAG_CANCEL_CURRENT);

        //Notification bar のprogress bar①
        final NotificationManager notifMgr = (NotificationManager)context.getSystemService(context.NOTIFICATION_SERVICE);
        final Notification.Builder notifBuilder = new Notification.Builder(context); // API Level 11
        long when = System.currentTimeMillis(); // 時刻
        // 取得した値をセット
        notifBuilder.setSmallIcon(R.drawable.ic_launcher);// アイコン
        // LargeIcon の Bitmap を生成
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        notifBuilder.setLargeIcon(largeIcon);
//        notifBuilder.setTicker(getString(R.string.Downloading));// テキスト(ステータスバー)//OS4.4以下のみ
        notifBuilder.setTicker("ディズニーシェア");// テキスト(ステータスバー)//OS4.4以下のみ
        notifBuilder.setWhen(when);
//        notifBuilder.setContentTitle(getString(R.string.app_name));// タイトル
        notifBuilder.setContentTitle("ディズニーシェア");// タイトル
//        notifBuilder.setContentText(getString(R.string.Downloading));
//        notifBuilder.setContentText("ディズニーシェアの更新情報を確認しましょう");
        notifBuilder.setContentText(ContentText);

        //Notification bar のprogress bar③
//        notifBuilder.setProgress(0, 0, false); // API Level 14
//        notifBuilder.setContentText(getString(R.string.Downloaded));
        notifBuilder.setAutoCancel(true); // 通知をクリックした時に自動的に通知を消すように設定
        notifBuilder.setContentIntent(pi); // 通知が選択された時に起動するIntentを設定
        notifMgr.notify(R.string.app_name, notifBuilder.getNotification());



    }

    @Override
    protected void onNotificationPosted(@NonNull Context context, @NonNull NotificationInfo notificationInfo) {
        Log.i(TAG, "Notification posted. Alert: " + notificationInfo.getMessage().getAlert() + ". NotificationId: " + notificationInfo.getNotificationId());
    }

    @Override
    protected boolean onNotificationOpened(@NonNull Context context, @NonNull NotificationInfo notificationInfo) {
        Log.i(TAG, "Notification opened. Alert: " + notificationInfo.getMessage().getAlert() + ". NotificationId: " + notificationInfo.getNotificationId());

        // Return false here to allow Urban Airship to auto launch the launcher activity
        return false;
    }

    @Override
    protected boolean onNotificationOpened(@NonNull Context context, @NonNull NotificationInfo notificationInfo, @NonNull ActionButtonInfo actionButtonInfo) {
        Log.i(TAG, "Notification action button opened. Button ID: " + actionButtonInfo.getButtonId() + ". NotificationId: " + notificationInfo.getNotificationId());

        // Return false here to allow Urban Airship to auto launch the launcher
        // activity for foreground notification action buttons
        return false;
    }

    @Override
    protected void onNotificationDismissed(@NonNull Context context, @NonNull NotificationInfo notificationInfo) {
        Log.i(TAG, "Notification dismissed. Alert: " + notificationInfo.getMessage().getAlert() + ". Notification ID: " + notificationInfo.getNotificationId());
    }
}
