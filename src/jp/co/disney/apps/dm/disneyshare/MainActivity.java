package jp.co.disney.apps.dm.disneyshare;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jp.co.disney.apps.dm.disneyshare.spp.AdBannerLayout;
import jp.co.disney.apps.dm.disneyshare.spp.BannerWeightComparator;
import jp.co.disney.apps.dm.disneyshare.spp.SPPUtility;
import jp.co.disney.apps.dm.disneyshare.spp.SppCheckUserStatusAsyncTask;
import jp.co.disney.apps.dm.disneyshare.spp.SppCheckUserStatusTaskCallback;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.urbanairship.UAirship;

import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkSettings;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkActivity;


public class MainActivity extends Activity implements SppCheckUserStatusTaskCallback, ActivityCompat.OnRequestPermissionsResultCallback {//ListView.OnItemClickListener, View.OnClickListener,
//public class MainActivity extends XWalkActivity implements SppCheckUserStatusTaskCallback, ActivityCompat.OnRequestPermissionsResultCallback {//ListView.OnItemClickListener, View.OnClickListener,
//public class MainActivity extends AppCompatActivity implements SppCheckUserStatusTaskCallback, ActivityCompat.OnRequestPermissionsResultCallback {//ListView.OnItemClickListener, View.OnClickListener,

	//    private static final String MENU_PREV = "menu_prev";
//    private static final String MENU_NEXT = "menu_next";
//    private static final String MENU_REFRESH = "menu_refresh";
	private static final String TYPE_IMAGE = "image/*";
	private static final int FILECHOOSER_REQUEST_CODE = 1;

//    private String NAV_HOME;
//    private String NAV_HELP;
//    private String NAV_QUIT;

	//    private DrawerLayout mDrawerLayout;
	private FrameLayout mContentLayout;
	private ProgressBar mProgressBar;
	//    private WebView mWv;
//	private XWalkView xWalkWebView;
	private XWalkView mWv;
//	private XWalkView mWev;

//    private ListView mDrawerList;
//    private LinearLayout mDrawer;

	private AlertDialog mAppFinishDialog;

	private boolean mLoadingPage = false;

	private ValueCallback<Uri> mUploadMessage;
	private ValueCallback<Uri[]> mFilePathCallback;

	private final int GET_VERSION_INFO_CODE = 210;
	private final int GET_AUTH_INFO_CODE = 214;
	private final int GET_AUTH_INFO_CODE_FROM_WEB = 2141;
	private final int GET_SPP_REGIST_CODE = 212;
	private final int GET_AD_INFO_CODE = 202;

	private boolean nowGetVersionInfo = false;
	private boolean isTrackingHelperStarted = false;

	private Runnable runnableForGetVersionInfo, runnableForGetAdInfo, runnableForGetAuthInfo, runnableForGetAuthInfoFromWeb, runnableForRegistSPP;
	private final Handler handler = new Handler();

	private final String[] Permissions = {Manifest.permission.READ_PHONE_STATE};
	private final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 101;
	private boolean isPermissionCheckNG = false;
	private Runnable runnableForPermission;

	private userInfo myInfo = null;
	private IntentInfo nowIntentInfo = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//パーミッションチェック
		if (Build.VERSION.SDK_INT >= 23) {//OSバージョン
			//パーミッションチェック
			for (int i = 0; i < Permissions.length; i++) {

				if (PermissionChecker.checkSelfPermission(this, Permissions[i])
						!= PermissionChecker.PERMISSION_GRANTED) {
					DebugLog.instance.outputLog("checkSelfPermission", "onCreate_checkSelfPermission_" + Permissions[i] + "_!= PERMISSION_GRANTED");
					isPermissionCheckNG = true;

					// パーミッションをリクエストする
					int MY_PERMISSION_REQUEST = 0;
					if (Permissions[i].equals(Permissions[1])) {
						MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_READ_PHONE_STATE;
					}

					//パーミッションリクエスト
					final int index = i;
					final int PERMISSION_REQUEST = MY_PERMISSION_REQUEST;
					runnableForPermission = new Runnable() {
						@Override
						public void run() {
							ActivityCompat.requestPermissions(MainActivity.this, new String[]{Permissions[index]}, PERMISSION_REQUEST);
						}
					};
					handler.post(runnableForPermission);
					break;

				} else {
					DebugLog.instance.outputLog("checkSelfPermission", "onCreate_checkSelfPermission_" + Permissions[i] + "_== PERMISSION_GRANTED");
				}
			}

		} else {
			isPermissionCheckNG = false;
		}

		if (!isPermissionCheckNG) onCreateAfterPermissionGranted();

	}

	private void onCreateAfterPermissionGranted() {

		nowIntentInfo = new IntentInfo();
		nowIntentInfo.getIntentInfo(getIntent());

//        //http://wp.developapp.net/?p=3319
//        //http://d.hatena.ne.jp/spitfire_tree/20160118/1453101520
//        CookieManager cManager = CookieManager.getInstance();
//        cManager.setAcceptCookie(true);

//        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mContentLayout = (FrameLayout) findViewById(R.id.content_frame);
//        mDrawer = (LinearLayout) findViewById(R.id.left_drawer);
//        mDrawerList = (ListView) findViewById(R.id.drawer_list);
		mProgressBar = (ProgressBar) findViewById(R.id.prg_bar);

		// WebViewはXMLで定義しない (破棄処理を行うため。)
//        mWv = new WebView(this);
		mWv = new XWalkView(this);

		mContentLayout.addView(mWv, 0, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

//        //WebViewのUserAgentを変更する
//        String userAgentString = mWv.getSettings().getUserAgentString();
//        mWv.getSettings().setUserAgentString(userAgentString + "@RGlzbmV5U2hhcmVGcm9tQVBQ");
//        userAgentString = mWv.getSettings().getUserAgentString();
//        DebugLog.instance.outputLog("value", "UserAgent:" + userAgentString);

		layout = new RelativeLayout(this);
		layout.setBackgroundColor(0xffe6e6e6);
//        layout.addView(initializeForView(launcherlistener, config));

//		final int MP = ViewGroup.LayoutParams.MATCH_PARENT;
//		addContentView(layout,new RelativeLayout.LayoutParams(MP, MP));

		initWebview();

		initRunnable();

		myInfo = new userInfo();

		// 画面サイズを取得
//		int viewWidth = this.getResources().getDisplayMetrics().widthPixels;
//		dispScaleperBase = (float)viewWidth/1080f;

		handler.post(runnableForGetVersionInfo);

		String channelId = UAirship.shared().getPushManager().getChannelId();
		DebugLog.instance.outputLog("pushtes", "MainActivity_My Application Channel ID: " + channelId);

	}

	@Override
	protected void onRestart() {
		super.onRestart();

		if (!nowGetVersionInfo) {
			TrackingHelper.resume();
		}
	}



	enum IntentState {
		NORMAL,
		DO_GETAUTHINFO,
		FROMINVIURL,
		FROMWEB_FORINVIURL,
		FROMWEBVIEW_FORWEATHER,
		FROMWEBVIEW_FORVOICE,
		FROMWEBVIEW_FORGREETING,
		FROMWEBVIEW_FORPROFILE,
		FROM_BASEAPP_D_CONTENTS;
	}


	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		//起動中に受け取るパタンを想定
		//NORMAL,FROMINVIURL,FROMWEB_FORINVIURL,FROMWEBVIEW_FORWEATHER,FROMWEBVIEW_FORVOICE,FROMWEBVIEW_FORGREETING

		DebugLog.instance.outputLog("value", "MainActivity_onNewIntent_getAction----------" + intent.getAction());
		DebugLog.instance.outputLog("value", "MainActivity_onNewIntent_" + intent.getDataString());

		if (intent.getAction().equals(Intent.ACTION_MAIN) && intent.getDataString() == null) {
			return;
		} else if (intent.getDataString() != null && intent.getDataString().equals("disneyshare://sp-share.disney.co.jp/")) {
			//起動中にWebから「通常起動」で呼ばれたら状態に変化なしの想定
			return;
		}

		nowIntentInfo.getIntentInfo(intent);

		selectAction();
	}

	private void selectAction() {
		if (nowIntentInfo.intent_state == IntentState.NORMAL) {
			goSharePage("", "");

		} else if (nowIntentInfo.intent_state == IntentState.DO_GETAUTHINFO) {
			//GetAuthInfo
			handler.post(runnableForGetAuthInfoFromWeb);

		} else if (nowIntentInfo.intent_state == IntentState.FROMINVIURL) {
			//"ドメイン/room/?"を消す。
			String inviWaitRoom = nowIntentInfo.intent_data.replace(getDomain() + "room/?", "");
			goSharePage(inviWaitRoom, "");

		} else if (nowIntentInfo.intent_state == IntentState.FROMWEB_FORINVIURL) {
			//招待URLのパラメータ取得
			String inviWaitRoom = nowIntentInfo.intent_data.replace("disneyshare://sp-share.disney.co.jp/roomparam=", "");

			DebugLog.instance.outputLog("value", "Webから起動：参加待ち画面へ：" + getDomain() + "_" + inviWaitRoom);
			goSharePage(inviWaitRoom, "");

		} else if (nowIntentInfo.intent_state == IntentState.FROMWEBVIEW_FORWEATHER) {
			startOtherApp("jp.co.disney.apps.managed.weatherapp", "DisneyWeatherSplash", "90020");

		} else if (nowIntentInfo.intent_state == IntentState.FROMWEBVIEW_FORVOICE) {
			startOtherApp("http://sp-voicemessage.disney.co.jp", "", "");

		} else if (nowIntentInfo.intent_state == IntentState.FROMWEBVIEW_FORGREETING) {
			startOtherApp("jp.co.disney.apps.managed.gcapp", "TopActivity", "90018");

		} else if (nowIntentInfo.intent_state == IntentState.FROMWEBVIEW_FORPROFILE) {
			startOtherApp("jp.co.disney.apps.dm.disneyprofilemaker", "StartupActivity", "1000005536");

		} else if (nowIntentInfo.intent_state == IntentState.FROM_BASEAPP_D_CONTENTS) {
			//詳細画面にアクセス
			String assetId = nowIntentInfo.intent_data.replace("dmarketinternal://jp.co.disney.apps.dm.disneyshare/MainActivity?aid=", "");
			DebugLog.instance.outputLog("value", "BaseAppから起動：コンテンツ詳細画面へ：" + getDomain() + "_" + assetId);
			goSharePage("", assetId);
		}

	}

	private void startOtherApp(String hontai, String act, String assetid) {

		//起動アクティビティが設定されていなかったらhontaiをuriとしてブラウザへintent
		if (act.equals("")) {

			//主にボイスメッセージ
			Uri uri = Uri.parse(hontai);
			Intent i = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(i);

		} else {

			if (SPPUtility.isAppInstalled(getApplicationContext(), hontai)) {
				Intent intentForW = new Intent(Intent.ACTION_MAIN);
				intentForW.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intentForW.setClassName(hontai, hontai + "." + act);
				startActivity(intentForW);
			} else {
				//各アプリがインストールされていなかったらDisneyMarketの該当アプリの詳細画面起動
				//DisneyMarketもDLされていなかったらDisneyMarketをDL

				if (SPPUtility.isAppInstalled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")) {
					if (SPPUtility.isAppEnabled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")) {
						Intent intentForDM = new Intent();
						intentForDM.setAction(Intent.ACTION_VIEW);
						intentForDM.setData(Uri.parse("dmarket://moveassetinfo/?assetID=" + assetid));
						startActivity(intentForDM);

					} else {
						Intent intentForSet = new Intent();
						intentForSet.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						intentForSet.setData(Uri.parse("package:jp.co.disney.apps.base.disneymarketapp"));
						startActivity(intentForSet);
						if (SPPUtility.getCarrierID(getApplicationContext()).equals(SPPUtility.AUTH_CARRIER_AU)) {
							Toast.makeText(getApplicationContext(), "DisneyPassを有効にしてください", Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(getApplicationContext(), "DisneyMarketを有効にしてください", Toast.LENGTH_LONG).show();
						}
					}
				} else {
					//インストール処理？
					if (SPPUtility.getCarrierID(getApplicationContext()).equals(SPPUtility.AUTH_CARRIER_AU)) {
						//auマーケットが入っていたら
						if (SPPUtility.isAppInstalled(getApplicationContext(), "com.kddi.market") && SPPUtility.isAppEnabled(getApplicationContext(), "com.kddi.market")) {
							//
							Toast.makeText(getApplicationContext(), "Disney passをインストールして下さい。", Toast.LENGTH_LONG).show();
							Intent intent = new Intent(Intent.ACTION_VIEW,
									Uri.parse("auonemkt://details?id=8588000000001"));
							startActivity(intent);
							finish();
							return;

						}
					}

					//ダウンロード開始用Activity
					Intent intent = new Intent();
					intent.setClassName(getPackageName(), "jp.co.disney.apps.managed.kisekaeapp.spp.BaseAppDownloadActivity");
					startActivity(intent);
					finish();
				}

			}

		}

	}

	enum AppIndexParam {
		xmid,
		status_code,
		age_band,
		birthday,
		access_token,
		id_token,
		is_premium,
		room_id,
		asset_id;
	}

	enum UserStatusCode {
		standardCPK(210),
		standardCPS(211),
		standardOnD(912),
		standardOnS(913),
		singleCPK(200),
		singleCPS(201);

		private int value;

		UserStatusCode(int v) {
			this.value = v;
		}

		public int getValue() {
			return value;
		}

	}


	private String getDomain() {
		String[] param = SPPUtility.getConnectEnv(getApplicationContext());

		//強制でstaging環境　TODO
//		return "http://dev.sp-share.disney.co.jp/";
		return "http://staging.sp-share.disney.co.jp/";

//		if(param[0].equals("liv")){
//			return "https://sp-share.disney.co.jp/";
//		}else if(param[0].equals("stg")){
//			return "http://staging.sp-share.disney.co.jp/";
//		}else if(param[0].equals("dev")){
//			return "http://dev.sp-share.disney.co.jp/app_index.php";
//		}else{
//			return "https://sp-share.disney.co.jp/";
//		}

	}

	private boolean goSharePage(String roomid, String assetid) {
		DebugLog.instance.outputLog("value", "goSharePage_" + nowIntentInfo.intent_state.name());
		DebugLog.instance.outputLog("value", "goSharePage_roomid_" + roomid);
		DebugLog.instance.outputLog("value", "goSharePage_assetid_" + assetid);

		String connectURL = getDomain() + "app_index.php";

		//誕生日
		SharedPreferences userP = getSharedPreferences(SPPUtility.USER_INFO_PREF, Context.MODE_PRIVATE);
		String userBirth = "";
		userBirth = userP.getString(SPPUtility.USER_BIRTHDAY, "");
		if (myInfo.is_standard && userBirth.equals("")) return false;

		//ageband
		String userAgeBand = "";
		userAgeBand = userP.getString(SPPUtility.USER_AGEBAND, "");
		if (myInfo.is_standard && userAgeBand.equals("")) return false;


//		String postData = "postvar=value&postvar2=value2";
		String postData = AppIndexParam.xmid.toString() + "=" + myInfo.xmid
				+ "&" +
				AppIndexParam.status_code.toString() + "=" + myInfo.status_code.getValue()
				+ "&" +
				AppIndexParam.age_band.toString() + "=" + userAgeBand
				+ "&" +
				AppIndexParam.birthday.toString() + "=" + userBirth
				+ "&" +
				AppIndexParam.is_premium.toString() + "=" + myInfo.is_premium
				+ "&" +
				AppIndexParam.access_token.toString() + "=" + myInfo.accessToken
				+ "&" +
				AppIndexParam.id_token.toString() + "=" + myInfo.idToken;
		if (!roomid.equals("") && myInfo.is_standard) {
			postData = postData + "&" + AppIndexParam.room_id.toString() + "=" + roomid;
		}
		if (!assetid.equals("")) {
			postData = postData + "&" + AppIndexParam.asset_id.toString() + "=" + assetid;
		}

		DebugLog.instance.outputLog("value", "url__" + connectURL);
		DebugLog.instance.outputLog("value", "postdata__" + postData);

		try {
			byte[] data = postData.getBytes("UTF-8");
			String base64 = Base64.encodeToString(data, Base64.DEFAULT);

			byte[] encodedBytes;
			encodedBytes = Base64.encode(postData.getBytes(), 0);

////			mWv.postUrl(connectURL, base64.getBytes("UTF-8"));
////			mWv.postUrl(connectURL, encodedBytes);
//			mWv.postUrl(connectURL, data);
////			mWv.postUrl(connectURL, EncodingUtils.getBytes(postData, "base64"));
////			mWv.postUrl(connectURL, postData.getBytes("base64"));
//			mWv.load(connectURL, null);//tes
//			mWv.evaluateJavascript(connectURL, data)//×

			String connectURLwithData = connectURL + "?" + postData;//×
 			mWv.load(connectURLwithData, null);//tes;//×
//			mWv.load(connectURL, new String(data, "UTF-8"));//tes;//×
//			mWv.load("https://www.websocket.org/echo.html", null);//tes;//×
//			mWv.load("http://dshare.fieldsystem.co.jp/app/home.php", null);//tes;//×


//			mWv.load(connectURL, postData);//tes;//×
//			mWv.load(connectURL, base64);//tes;

			return true;
		} catch (UnsupportedEncodingException e) {
			DebugLog.instance.outputLog("value", "UnsupportedEncodingException_" + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showDialogFragment(APP_FINISH_ACTION);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onPause() {
		super.onPause();
//        isPausing = true;
		if (mLoadingPage) {
			if (mWv != null) {
				mWv.stopLoading();
			}
		}
	}

	@Override
	public void onDestroy() {
		if (isTrackingHelperStarted) TrackingHelper.shutdown();
		super.onDestroy();

		if (mWv != null) {
			mContentLayout.removeView(mWv);

			mWv.removeAllViews();
			mWv.clearCache(true);
//            mWv.destroy();
			mWv.onDestroy();
			mWv = null;
		}

		if (mAppFinishDialog != null) {
			mAppFinishDialog.dismiss();
			mAppFinishDialog = null;
		}

		handler.removeCallbacks(runnableForPermission);
		handler.removeCallbacks(runnableForGetVersionInfo);
		handler.removeCallbacks(runnableForGetAdInfo);
		handler.removeCallbacks(runnableForGetAuthInfo);
		handler.removeCallbacks(runnableForGetAuthInfoFromWeb);
		handler.removeCallbacks(runnableForRegistSPP);

	}

	private void initWebview() {

//        mWv.setWebViewClient(new WebViewClient() {
		mWv.setResourceClient(new XWalkResourceClient(mWv){
            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
			public boolean shouldOverrideUrlLoading(XWalkView view, String url) {
            	// TODO: ドメインで限定する
                if (!url.startsWith("http:") && !url.startsWith("https:")) {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    MainActivity.this.startActivity(intent);
                    return true;
                }

                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
//            public void onPageStarted (WebView view, String url, Bitmap favicon) {
			public void onLoadStarted (XWalkView view, String url) {
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(0);
                mLoadingPage = true;
            }

            @Override
//            public void onPageFinished(WebView view, String url) {
			public void onLoadFinished(XWalkView view, String url) {
                mProgressBar.setVisibility(View.INVISIBLE);
                mLoadingPage = false;
            }
        });
//		mWv.Client(new WebViewClient() {
//
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//            	// TODO: ドメインで限定する
//                if (!url.startsWith("http:") && !url.startsWith("https:")) {
//                    Uri uri = Uri.parse(url);
//                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                    MainActivity.this.startActivity(intent);
//                    return true;
//                }
//
//                return super.shouldOverrideUrlLoading(view, url);
//            }
//
//            @Override
//            public void onPageStarted (WebView view, String url, Bitmap favicon) {
//                mProgressBar.setVisibility(View.VISIBLE);
//                mProgressBar.setProgress(0);
//                mLoadingPage = true;
//            }
//
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                mProgressBar.setVisibility(View.INVISIBLE);
//                mLoadingPage = false;
//            }
//        });

//        WebSettings settings = mWv.getSettings();
		XWalkSettings settings = mWv.getSettings();
//        settings.setJavaScriptEnabled(true);//JavaScriptエンジン（Crosswalk）をアプリ内に内包してるから不要のはず。
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
//        settings.setLoadWithOverviewMode(true);//次の段で実装済。
		mWv.getSettings().setInitialPageScale(100);

//		settings.setAppCacheEnabled(true);//?有効か無効化不明。


		mWv.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);

//        wv.requestFocus(View.FOCUS_DOWN);
//        wv.requestFocusFromTouch();

		mWv.setUIClient(new MyUIClient(mWv));

//        mWv.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public void onProgressChanged(WebView view, int progress) {
//                mProgressBar.setProgress(progress);
//            }
//
//            //------- File Upload用 START----------------------------------------------------------------
//            // For Android < 3.0
//            public void openFileChooser(ValueCallback<Uri> uploadFile) {
//                openFileChooser(uploadFile, "");
//            }
//
//            // For 3.0 <= Android < 4.1
//            public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType) {
//                openFileChooser(uploadFile, acceptType, "");
//            }
//
//            // For 4.1 <= Android < 5.0
//            public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
//                if (mUploadMessage != null) {
//                    mUploadMessage.onReceiveValue(null);
//                }
//                mUploadMessage = uploadFile;
//
//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                intent.setType(TYPE_IMAGE);
//
//                startActivityForResult(intent, FILECHOOSER_REQUEST_CODE);
//            }
//
//            // For Android 5.0+
//            @Override
//            public boolean onShowFileChooser(WebView webView,
//                    ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
//                if (mFilePathCallback != null) {
//                    mFilePathCallback.onReceiveValue(null);
//                }
//                mFilePathCallback = filePathCallback;
//
//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                intent.setType(TYPE_IMAGE);
//                startActivityForResult(intent, FILECHOOSER_REQUEST_CODE);
//
//                return true;
//            }
//          //------- File Upload用 END ----------------------------------------------------------------
//        });
//		mWv.Client(new WebChromeClient() {
//			@Override
//			public void onProgressChanged(WebView view, int progress) {
//				mProgressBar.setProgress(progress);
//			}
//
//			//------- File Upload用 START----------------------------------------------------------------
//			// For Android < 3.0
//			public void openFileChooser(ValueCallback<Uri> uploadFile) {
//				openFileChooser(uploadFile, "");
//			}
//
//			// For 3.0 <= Android < 4.1
//			public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType) {
//				openFileChooser(uploadFile, acceptType, "");
//			}
//
//			// For 4.1 <= Android < 5.0
//			public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
//				if (mUploadMessage != null) {
//					mUploadMessage.onReceiveValue(null);
//				}
//				mUploadMessage = uploadFile;
//
//				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//				intent.addCategory(Intent.CATEGORY_OPENABLE);
//				intent.setType(TYPE_IMAGE);
//
//				startActivityForResult(intent, FILECHOOSER_REQUEST_CODE);
//			}
//
//			// For Android 5.0+
//			@Override
//			public boolean onShowFileChooser(WebView webView,
//											 ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
//				if (mFilePathCallback != null) {
//					mFilePathCallback.onReceiveValue(null);
//				}
//				mFilePathCallback = filePathCallback;
//
//				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//				intent.addCategory(Intent.CATEGORY_OPENABLE);
//				intent.setType(TYPE_IMAGE);
//				startActivityForResult(intent, FILECHOOSER_REQUEST_CODE);
//
//				return true;
//			}
//			//------- File Upload用 END ----------------------------------------------------------------
//		});
	}

//    private void initNavigationDrawer() {
//
//        final Resources res = getResources();
//        NAV_HOME = res.getString(R.string.nav_home);
//        NAV_HELP = res.getString(R.string.nav_help);
//        NAV_QUIT = res.getString(R.string.nav_quit);
//
//        final String[] items  = { NAV_HOME, NAV_HELP, NAV_QUIT };
//
//        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, items));
//        mDrawerList.setOnItemClickListener(this);
//    }

//    private void initMenu() {
//
//        MenuItemImageView menuPrev = (MenuItemImageView) findViewById(R.id.menu_prev);
//        menuPrev.setTag(MENU_PREV);
//        menuPrev.setOnClickListener(this);
//
//        MenuItemImageView menuNext = (MenuItemImageView) findViewById(R.id.menu_next);
//        menuNext.setTag(MENU_NEXT);
//        menuNext.setOnClickListener(this);
//
//        MenuItemImageView menuRefresh = (MenuItemImageView) findViewById(R.id.menu_refresh);
//        menuRefresh.setTag(MENU_REFRESH);
//        menuRefresh.setOnClickListener(this);
//    }

//    @Override
//    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
////        mDrawerLayout.closeDrawer(mDrawer);
//
//        ListView listView = (ListView) parent;
//
//        String item = (String) listView.getItemAtPosition(position);
//
//        if (NAV_HOME.equals(item)) {
//            if (mWv != null) goSharePage("");
//        } else if (NAV_HELP.equals(item)) {
//
//        } else if (NAV_QUIT.equals(item)) {
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    showAppFinishDialog();
//                }
//            }, 100);
//        }
//    }

//    @Override
//    public void onClick(View v) {
//
//        String tag = (String) v.getTag();
//
//        if (MENU_PREV.equals(tag)) {
//
//            if (mWv != null && mWv.canGoBack()) {
//                mWv.goBack();
//            }
//
//        } else if (MENU_NEXT.equals(tag)) {
//
//            if (mWv != null && mWv.canGoForward()) {
//                mWv.goForward();
//            }
//
//        } else if (MENU_REFRESH.equals(tag)) {
//            mWv.reload();
//        }
//    }

	//---------------------
	//---------------------SPP関連処理
	//---------------------





	class MyUIClient extends XWalkUIClient {
		MyUIClient(XWalkView view) {
			super(view);
		}

		@Override
		public void openFileChooser(XWalkView view, ValueCallback<Uri> uploadFile, String acceptType, String capture) {
			Log.d(MainActivity.class.getSimpleName(), "openFileChooser");
			DebugLog.instance.outputLog("value", "openFileChooser_");

//			super.openFileChooser(view, uploadFile, acceptType, capture);
//			mUploadMessage = uploadFile;
			if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                }
                mUploadMessage = uploadFile;

			DebugLog.instance.outputLog("checkSelfPermission", "openFileChooser_");
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType(TYPE_IMAGE);
			startActivityForResult(intent, FILECHOOSER_REQUEST_CODE);

		}

//		// For Android 5.0+
//		@Override
//		public boolean onShowFileChooser(WebView webView,
//										 ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
//			if (mFilePathCallback != null) {
//				mFilePathCallback.onReceiveValue(null);
//			}
//			mFilePathCallback = filePathCallback;
//
//			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//			intent.addCategory(Intent.CATEGORY_OPENABLE);
//			intent.setType(TYPE_IMAGE);
//			startActivityForResult(intent, FILECHOOSER_REQUEST_CODE);
//
//			return true;
//		}
	}



	//---------------------SPP関連処理(onActivityResult)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

		DebugLog.instance.outputLog("value", "onActivityResult__requestCode:" + requestCode + "/resultCode:" + resultCode);

		if (requestCode == FILECHOOSER_REQUEST_CODE) {

//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//				DebugLog.instance.outputLog("value", "onActivityResult_if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {_");
//				if (mFilePathCallback == null) {
//					super.onActivityResult(requestCode, resultCode, intent);
//					return;
//				}
//				Uri[] results = null;
//
//				if (resultCode == RESULT_OK) {
//					String dataString = intent.getDataString();
//					if (dataString != null) {
//						results = new Uri[]{Uri.parse(dataString)};
//					}
//				}
//
//				mFilePathCallback.onReceiveValue(results);
//				mFilePathCallback = null;
//			} else {
				DebugLog.instance.outputLog("value", "onActivityResult_else_if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {_mUploadMessage_" + mUploadMessage);

				if (mUploadMessage == null) {
					super.onActivityResult(requestCode, resultCode, intent);
					return;
				}

				Uri result = null;

				if (resultCode == RESULT_OK) {
					if (intent != null) {
						result = intent.getData();
					}
				}

				mUploadMessage.onReceiveValue(result);
				mUploadMessage = null;
//			}
			return;
		}

		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode == RESULT_OK) {
			//成功

			//GetVersionInfo,GetAdInfo,GetAuthinfo
			if (requestCode == GET_VERSION_INFO_CODE) {
				nowGetVersionInfo = false;

				if (intent == null || (intent.getExtras().getString("resultXML") != null && intent.getExtras().getString("resultXML").length() == 0)) {
					showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);

				} else {
					HashMap<String, String> map = parseResultXML(intent.getStringExtra("resultXML"), "GetVersionInfo");

					if (map == null) {
						//解析不正
						showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);

					} else {
						//Errorがあったら終了
						Iterator entries = map.entrySet().iterator();
						while (entries.hasNext()) {
							Map.Entry entry = (Map.Entry) entries.next();
							String keyName = (String) entry.getKey();
							DebugLog.instance.outputLog("value", "key:" + keyName);
							if (keyName.equals("Error")) {
								finish();
								return;
							}
						}

						String userStatus = map.get("Status");
						if (userStatus == null) userStatus = "false";

						//エラー対策
						int resultVersion;
						if (map.get("Result") == null || map.get("Result").equals(""))
							resultVersion = 100;
						else resultVersion = Integer.parseInt(map.get("Result"));

						if (map.get("Carrier").equals("4") || map.get("Carrier").equals("5")) {
							isDMfromGVI = true;
						} else {
							isDMfromGVI = false;
						}

						DebugLog.instance.outputLog("value", "GVI_userStatus::::::::::::::" + userStatus);
						DebugLog.instance.outputLog("value", "GVI_resultVersion::::::::::::::" + resultVersion);
						DebugLog.instance.outputLog("value", "GVI_isDM::::::::::::::" + isDMfromGVI);

						if (Boolean.parseBoolean(userStatus) && resultVersion == 0) {//会員登録済みの正常終了

							TrackingHelper.start_top(this, "", SPPUtility.isDebugFlag);
							isTrackingHelperStarted = true;
							DebugLog.instance.outputLog("value", "Tracking helper start !!!!");

							if (SPPUtility.getCarrierID(getApplicationContext()).equals(SPPUtility.AUTH_CARRIER_CONPAS)
									|| SPPUtility.getCarrierID(getApplicationContext()).equals(SPPUtility.AUTH_CARRIER_AU)) {
								String userProfile = "";
								userProfile = map.get("UserProfile");
								if (!userProfile.equals("")) {
									DebugLog.instance.outputLog("value2", "user_profile_" + userProfile);
									TrackingHelper.setExtProfile(userProfile);
								}
							}

							//GVI→GAuthI
							handler.post(runnableForGetAuthInfo);
						} else {
							DebugLog.instance.outputLog("value", "result:" + resultVersion);
							if (!Boolean.parseBoolean(userStatus)) {
//                    			if((!Boolean.parseBoolean(userStatus) && resultVersion == 0) || (!Boolean.parseBoolean(userStatus) && resultVersion == 1)){
								showDialogFragment(SPP_FAILED_REASON_NOT_MEMBER);
//                				isDMfromGVI = false;
							} else {
								finish();
							}
						}
					}
				}

			} else if (requestCode == GET_AUTH_INFO_CODE || requestCode == GET_AUTH_INFO_CODE_FROM_WEB) {

				if (intent == null || (intent.getExtras().getString("resultXML") != null && intent.getExtras().getString("resultXML").length() == 0)) {
					showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);

				} else {

					String result = intent.getExtras().getString("resultXML");
					DebugLog.instance.outputLog("value", "result:" + result);
					if (TextUtils.isEmpty(result)) {
						showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
					}
					try {
						final XmlPullParserFactory factory = XmlPullParserFactory
								.newInstance();
						factory.setNamespaceAware(true);
						final XmlPullParser parser = factory.newPullParser();

						parser.setInput(new StringReader(result));
						int eventType = parser.getEventType();
						while (eventType != XmlPullParser.END_DOCUMENT) {
							if (eventType == XmlPullParser.START_TAG) {
								final String tag = parser.getName().toLowerCase();
								if ("result".equals(tag)) {

									if (!"0".equals(parser.nextText().toLowerCase())) {
										showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
									}

								} else if ("accesstoken".equals(tag)) {
									myInfo.accessToken = parser.nextText();

								} else if ("idtoken".equals(tag)) {
									myInfo.idToken = parser.nextText();
								}
							} else if (eventType == XmlPullParser.END_TAG) {
							}
							eventType = parser.next();
						}

						//Tokenが両方取得できたら
						if (!myInfo.idToken.equals("") && !myInfo.accessToken.equals("")) {
							//prefへの保存処理
							SharedPreferences preferences = getSharedPreferences(SPPUtility.GET_TOKEN_PREF, Context.MODE_PRIVATE);
							SharedPreferences.Editor e = preferences.edit();
							Date date = new Date(System.currentTimeMillis());
							//現在時刻
							long nowTimeLong = date.getTime() - (2 * 60 * 1000);
							e.putLong(SPPUtility.TOKEN_GET_LASTTIME, nowTimeLong);

							e.putString(SPPUtility.PREF_KEY_ID_TOKEN, myInfo.idToken);
							e.putString(SPPUtility.PREF_KEY_ACCESS_TOKEN, myInfo.accessToken);
							e.commit();

							if (requestCode == GET_AUTH_INFO_CODE) {
								new SppCheckUserStatusAsyncTask(this).execute("");

							} else if (requestCode == GET_AUTH_INFO_CODE_FROM_WEB) {
								DebugLog.instance.outputLog("value", "トークン再取得のGAI成功、app_index.phpをcall");
								goSharePage("", "");
							}
						} else {
							//tokenの取得失敗
							showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
						}

					} catch (final Exception e) {
						showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
					}

				}

			} else if (requestCode == GET_AD_INFO_CODE) {

				layout.setVisibility(View.VISIBLE);
				DebugLog.instance.outputLog("value", "GetAdInfoの戻り");
				//バナー情報をget
				//取得失敗してもアプリは終了させない→次の処理へ移行
				if (intent == null || (intent.getExtras().getString("resultXML") != null && intent.getExtras().getString("resultXML").length() == 0)) {
					selectAction();

				} else {
					DebugLog.instance.outputLog("value", "GetAdInfoの戻り" + intent.getExtras().getString("resultXML"));

					bannerLayout = parseResultADXML(intent.getStringExtra("resultXML"));
					if (bannerLayout != null) {
						DebugLog.instance.outputLog("value", "GetAdInfo_if(bannerLayout != null){1");
						if (bannerLayout.size() > 0) {
							DebugLog.instance.outputLog("value", "GetAdInfo_if(bannerLayout.size() > 0){2");
							Collections.sort(bannerLayout, new BannerWeightComparator());//TODO ソートが出来ているか要確認
							if (!bannerLayout.get(0).imageURL.equals("")) {//画像URL取得成功
								DebugLog.instance.outputLog("value", "GetAdInfo_if(!bannerLayout.get(0).imageURL.equals()){//画像URL取得成功3");
								//それぞれのバナーの情報が帰ってきている筈。一枚も無かったらスルー（あと、複数枚ある想定なのでlayoutもそれにあわせておく。）
								startDisplayBanner();
								selectAction();//バナーを取得しつつ接続も開始
							} else {
								selectAction();
							}
						} else {
							selectAction();
						}
					} else {
						selectAction();//接続開始
					}
				}

			} else if (requestCode == GET_SPP_REGIST_CODE) {
				if (intent == null || (intent.getExtras().getString("resultXML") != null && intent.getExtras().getString("resultXML").length() == 0)) {
				} else {
					DebugLog.instance.outputLog("value", "戻り:" + intent.getExtras().getString("resultXML"));

					String xml = intent.getExtras().getString("resultXML");
					//xml内部の&を特殊文字として置き換え
					xml = xml.replaceAll("&", "&amp;");

					DebugLog.instance.outputLog("value", "RegistSPP_response XML_startTag_" + xml);

					int adNum = 0;
					boolean isResult = false;
					XmlPullParser xmlPullParser = Xml.newPullParser();
					try {
						String name = "";
						xmlPullParser.setInput(new StringReader(xml));
						for (int eventType = xmlPullParser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = xmlPullParser.next()) {
							DebugLog.instance.outputLog("value", "adxml_eventType_" + eventType + "/" + xmlPullParser.getName());
							if (eventType == XmlPullParser.START_TAG) {
								name = xmlPullParser.getName();
								if (name != null) {

									if (name.equals("Result")) {
										DebugLog.instance.outputLog("value", "_RegistSPP start!!!!!!!!!!!!");
										isResult = true;
									}
								}
							}
							//item tag
							else if (eventType == XmlPullParser.TEXT) {//<Version><VersionUpComment>のところでエラーになる対応

								String val = xmlPullParser.getText();

								DebugLog.instance.outputLog("value", name + "_text_" + val);
								if (isResult) {
									if (val.equals("0")) {
										//成功
										selectAction();
									} else if (val.equals("1")) {
										//失敗
										showDialogFragment(SPP_FAILED_REASON_NOT_REGULAR_MEMBER);
									}
								}

								name = "";
							}
							//tes
							else if (eventType == XmlPullParser.END_TAG) {
//          	                  DebugLog.instance.outputLog("value", "}else if(eventType == XmlPullParser.END_TAG){_");
								name = xmlPullParser.getName();
								if (name != null) {
									if (name.equals(AD_INFO_ITEM_TAG)) {
										isResult = false;
									}
								}
							}

						}
					} catch (Exception e) {
						showDialogFragment(SPP_FAILED_REASON_NOT_REGULAR_MEMBER);
					}

				}
			}

		} else {
			//失敗
			//GetVersionInfo,GetAdInfo,GetAuthinfo
			if (requestCode == GET_VERSION_INFO_CODE) {
				nowGetVersionInfo = false;
				showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
			} else if (requestCode == GET_AUTH_INFO_CODE) {
				showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
			} else if (requestCode == GET_AD_INFO_CODE) {
				selectAction();
			} else if (requestCode == GET_SPP_REGIST_CODE) {
				showDialogFragment(SPP_FAILED_REASON_NOT_REGULAR_MEMBER);
			}


		}

	}

	//---------------------SPP関連処理(ゲートバナー)
	private ArrayList<AdBannerLayout> bannerLayout = null;
	private RelativeLayout layout = null;
	private float dispScaleperBase = 1f;//FHD(1080x1920)と比較しての倍率

	private final String AD_BLOCK_SHARED_PREF = "ad_banner_blockable", AD_BLOCK_BANNER_ID = "block_banner_id_", AD_BLOCK_LAST_DISPLAYDATE = "ad_last_display_date_";
	private final int ISBLOCK = -1, NOTBLOCK = 0, LIMIT24H = 1;
	private int barHeight = 50;

	private final int gateAreaCode = 15;
	private final String AD_INFO_ITEM_TAG = "Item";
	private final String AD_INFO_BANNER_AREA_TAG = "AreaNo";
	private final String AD_INFO_BANNER_ID_TAG = "BannerID";
	private final String AD_INFO_WEIGHT_TAG = "Weight";
	private final String AD_INFO_IMAGE_TAG = "Image";
	private final String AD_INFO_LINK_TAG = "Link";
	private final String AD_INFO_BLOCKABLE_TAG = "Blockable";

	private void startDisplayBanner() {
		DebugLog.instance.outputLog("value", "banner startDisplayBanner_bannerLayout.size()" + bannerLayout.size());
		//広告表示中の広告画像以外のタッチ抑止、再表示抑止不可の広告ではチェックボックスを非表示に

		if (bannerLayout.size() <= 0) return;

		OnTouchListener bannerTouchListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						switch (view.getId()) {
							case R.id.cancel_area:
								DebugLog.instance.outputLog("value", "banner tap cancel area!");
								setAdBlock();
								removeBanner();
								break;
							case R.id.ad_banner_image:
								DebugLog.instance.outputLog("value", "banner tap image!");
								setAdBlock();
//						moveToAdPage();//taihi
								removeBanner();//tes
								break;
							default:
								break;
						}
						break;
				}
				return true;
			}
		};

		LinearLayout.LayoutParams adParentMargin = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

		RelativeLayout.LayoutParams adImageMargin = new RelativeLayout.LayoutParams((int) (980 * dispScaleperBase), (int) (1326 * dispScaleperBase));
		adImageMargin.setMargins((int) (50 * dispScaleperBase), (int) ((225 * dispScaleperBase) - barHeight), 0, 0);

		RelativeLayout.LayoutParams cancelMargin = new RelativeLayout.LayoutParams((int) (152 * dispScaleperBase), (int) (152 * dispScaleperBase));
		cancelMargin.setMargins((int) (875 * dispScaleperBase), (int) ((225 * dispScaleperBase) - barHeight), 0, 0);

		RelativeLayout.LayoutParams checkMargin = new RelativeLayout.LayoutParams((int) (952 * dispScaleperBase), (int) (80 * dispScaleperBase));
		checkMargin.setMargins((int) (70 * dispScaleperBase), (int) ((1590 * dispScaleperBase) - barHeight), 0, 0);

		RelativeLayout.LayoutParams checkboxMargin = new RelativeLayout.LayoutParams((int) (74 * dispScaleperBase), (int) (74 * dispScaleperBase));
		checkboxMargin.setMargins(2, 2, 0, 0);

		RelativeLayout.LayoutParams checkmessageMargin = new RelativeLayout.LayoutParams((int) (727 * dispScaleperBase), (int) (60 * dispScaleperBase));
		checkmessageMargin.setMargins((int) ((175 - 70) * dispScaleperBase), (int) ((1597 - 1590) * dispScaleperBase), 0, 0);

		for (int i = 0; i < bannerLayout.size(); i++) {

			LayoutInflater inflater = getLayoutInflater();
//			bannerLayout[i] = new AdBannerLayout();
			bannerLayout.get(i).adParent = (LinearLayout) inflater.inflate(R.layout.ad_banner, null);
			bannerLayout.get(i).adParent.setLayoutParams(adParentMargin);

			// 広告表示中のタッチイベント浸透抑止
			bannerLayout.get(i).adParent.setOnTouchListener(bannerTouchListener);
			bannerLayout.get(i).checkAreaLayout = (RelativeLayout) (bannerLayout.get(i).adParent.findViewById(R.id.check_area));
			bannerLayout.get(i).cancelAreaView = (View) (bannerLayout.get(i).adParent.findViewById(R.id.cancel_area));
			bannerLayout.get(i).blockCheckBox = (CheckBox) (bannerLayout.get(i).adParent.findViewById(R.id.check_block));
			bannerLayout.get(i).bannerImageView = (ImageView) (bannerLayout.get(i).adParent.findViewById(R.id.ad_banner_image));
			bannerLayout.get(i).checkMessageView = (ImageView) (bannerLayout.get(i).adParent.findViewById(R.id.check_message));
			//抑止可・不可にあわせて表示非表示を設定
			if (!bannerLayout.get(i).isBlockable) {
				DebugLog.instance.outputLog("value", "isBlockable_" + bannerLayout.get(i).isBlockable);
				bannerLayout.get(i).checkAreaLayout.setVisibility(View.INVISIBLE);
			}

			bannerLayout.get(i).bannerImageView.setOnTouchListener(bannerTouchListener);
			bannerLayout.get(i).cancelAreaView.setOnTouchListener(bannerTouchListener);
			bannerLayout.get(i).bannerImageView.setLayoutParams(adImageMargin);
			bannerLayout.get(i).cancelAreaView.setLayoutParams(cancelMargin);
			bannerLayout.get(i).checkAreaLayout.setLayoutParams(checkMargin);
			bannerLayout.get(i).blockCheckBox.setLayoutParams(checkboxMargin);
			bannerLayout.get(i).checkMessageView.setLayoutParams(checkmessageMargin);
		}

		//非表示のバナーがあったらArrayから削除
		SharedPreferences pref = getSharedPreferences(AD_BLOCK_SHARED_PREF, MODE_PRIVATE);
		for (int i = 0; i < bannerLayout.size(); i++) {
			int bannerState = pref.getInt(AD_BLOCK_BANNER_ID + bannerLayout.get(i).bannerId, NOTBLOCK);
			if (bannerState == LIMIT24H) {
				DebugLog.instance.outputLog("value", bannerLayout.get(0).bannerId + "は24時間以内非表示");
				long blockLimitTime = pref.getLong(AD_BLOCK_LAST_DISPLAYDATE + bannerLayout.get(i).bannerId, 0) + 24 * 60 * 60 * 1000;
				if (blockLimitTime > System.currentTimeMillis()) {
					DebugLog.instance.outputLog("value", bannerLayout.get(0).bannerId + "前回表示してから24時間過ぎてないので非表示");
					//前回表示してから24時間過ぎてないので非表示
					bannerLayout.remove(i);
					//インデックスをひとつ戻す
					i--;
				}
			} else if (bannerState == ISBLOCK) {
				DebugLog.instance.outputLog("value", bannerLayout.get(0).bannerId + "ブロック設定されてたので非表示");
				bannerLayout.remove(i);
				//インデックスをひとつ戻す
				i--;
			}
		}


//		if(!bannerLayout.get(i).imageURL.equals("")){
		try {
			AdImgDLTask adImgDLTask = new AdImgDLTask();
			adImgDLTask.execute(bannerLayout.size());
		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "createBitmapDrawable_error_" + e.toString());
			e.printStackTrace();
		}
//		}

	}

	private void setAdBlock() {

		/*
ユーザが「今後このお知らせを表示しない」のチェックを入れたら再表示させない
1回表示させたら、その日のうち(24時間以内)は再表示させない(1日1回だけ表示)
		 */


//		SharedPreferences pref = getSharedPreferences("GetAdInfoBlock", MODE_PRIVATE);

//		if (((CheckBox) findViewById(R.id.check_block)).isChecked()) {
//		for(AdBannerLayout banner : bannerLayout)
		if (!bannerLayout.get(0).blockCheckBox.isChecked()) {
			//24時間以内は再表示させない
			DebugLog.instance.outputLog("value", bannerLayout.get(0).bannerId + "非ブロック");
			SharedPreferences pref = getSharedPreferences(AD_BLOCK_SHARED_PREF, MODE_PRIVATE);
			SharedPreferences.Editor edit = pref.edit();
			edit.putInt(AD_BLOCK_BANNER_ID + bannerLayout.get(0).bannerId, LIMIT24H);
			edit.putLong(AD_BLOCK_LAST_DISPLAYDATE + bannerLayout.get(0).bannerId, System.currentTimeMillis());
			edit.commit();
		} else {
			//今後一切再表示させない
			DebugLog.instance.outputLog("value", bannerLayout.get(0).bannerId + "ブロック");
			SharedPreferences pref = getSharedPreferences(AD_BLOCK_SHARED_PREF, MODE_PRIVATE);
			SharedPreferences.Editor edit = pref.edit();
			edit.putInt(AD_BLOCK_BANNER_ID + bannerLayout.get(0).bannerId, ISBLOCK);
			edit.commit();

		}

	}


	private void removeBanner() {
		if (bannerLayout != null && bannerLayout.size() > 0) {
			//現在表示しているバナーを消して、arrayから消す、次のやつが表示対象だったら表示する
			bannerLayout.get(0).goneLayout();
			bannerLayout.remove(0);
			if (bannerLayout.size() > 0) {
//				bannerLayout.sort(new BannerWeightComparator());　TODO　ソート出来ているか要確認
				Collections.sort(bannerLayout, new BannerWeightComparator());

				layout.addView(bannerLayout.get(0).adParent);
				bannerLayout.get(0).adParent.setVisibility(View.VISIBLE);
			}

			mContentLayout.removeViewAt(1);
		}
	}

	private void moveToAdPage() {
		//リンク先
		if (!bannerLayout.get(0).linkURL.equals("")) {
			DebugLog.instance.outputLog("value", "linkURL_" + bannerLayout.get(0).linkURL);
			if (bannerLayout.get(0).linkURL.indexOf("dmarket://") != -1) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(bannerLayout.get(0).linkURL));
				startActivity(intent);

			} else {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(bannerLayout.get(0).linkURL.replaceAll(" ", "")));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		} else {
			return;
		}
		removeBanner();
	}


	private class AdImgDLTask extends AsyncTask<Integer, Void, Boolean> {//news画像取得
		BitmapDrawable[] adBd = null;

		/**
		 * バックグランドで行う処理
		 */
		@Override
		protected Boolean doInBackground(Integer... params) {
			DebugLog.instance.outputLog("value", "バナー数" + bannerLayout.size());

			if (params.length < 1) {
				return false;
			} else {
				adBd = new BitmapDrawable[bannerLayout.size()];
			}

			try {
				for (int i = 0; i < bannerLayout.size(); i++) {
					DebugLog.instance.outputLog("value", "createBitmapDrawable_" + bannerLayout.get(i).imageURL);
					URL url = new URL(bannerLayout.get(i).imageURL);
					InputStream is = (InputStream) url.getContent();
					adBd[i] = (BitmapDrawable) BitmapDrawable.createFromStream(is, "src" + i);

					if (adBd[i].getBitmap().getDensity() == Bitmap.DENSITY_NONE) {
						adBd[i].setTargetDensity(getResources().getDisplayMetrics());
//			    		adBd[i].setTargetDensity(getResources().getDisplayMetrics().densityDpi);
					}

				}

			} catch (Exception e) {
				DebugLog.instance.outputLog("value", e.toString());
				return false;
			}

			return true;
		}

		/**
		 * バックグランド処理が完了
		 */
		@Override
		protected void onPostExecute(Boolean result) {
			DebugLog.instance.outputLog("value", "バナー_onPostExecute");

//			ImageView iv2 = new ImageView(getApplicationContext());
//			final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
//	        final int MP = ViewGroup.LayoutParams.MATCH_PARENT;
//			iv2.setLayoutParams(new LinearLayout.LayoutParams(MP, WC));

			if (result) {
				DebugLog.instance.outputLog("value", "バナー_if(result){");
//				for(int i = 0; i < bannerLayout.size(); i++) bannerLayout.get(i).bannerImageView.setImageDrawable(adBd[i]);DebugLog.instance.outputLog("value", "バナー_if(result){_i_" + i );
				for (int i = 0; i < bannerLayout.size(); i++) {
					bannerLayout.get(i).bannerImageView.setImageDrawable(adBd[i]);
//					iv2.setImageDrawable(adBd[i]);
					DebugLog.instance.outputLog("value", "バナー_if(result){_i_" + i);
				}
			}

			DebugLog.instance.outputLog("value", "バナー_aft_if(result){");


			//ここは一番上（weightで判断）のものだけadd
			if (bannerLayout != null && bannerLayout.size() >= 1) {
				DebugLog.instance.outputLog("value", "バナー_if(bannerLayout != null && bannerLayout.size() >= 1){_bannerLayout.get(0).adParent_" + bannerLayout.get(0).adParent);
////				layout.addView(bannerLayout.get(0).adParent);
				layout.addView(bannerLayout.get(0).adParent);
				bannerLayout.get(0).adParent.setVisibility(View.VISIBLE);
//				mContentLayout.addView(layout, 1, new FrameLayout.LayoutParams(
//		                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				mContentLayout.addView(layout, 1, new RelativeLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

//				layout.addView(iv2);

			}

//			((CatalogScreen)launcherlistener).onFinishBannerLoading();
			selectAction();//taihi
			DebugLog.instance.outputLog("value", "バナー_selectAction();//taihi_");

			super.onPostExecute(result);
		}


	}


	private ArrayList<AdBannerLayout> parseResultADXML(String xml) {
		//xml内部の&を特殊文字として置き換え
		xml = xml.replaceAll("&", "&amp;");

		DebugLog.instance.outputLog("value", "response XML_startTag_" + xml);

//	      Array<AdBannerLayout> itemArray = new Array<AdBannerLayout>();
		ArrayList<AdBannerLayout> itemArray = new ArrayList<AdBannerLayout>();

		int adNum = 0;
		boolean isItemTag = false;
		XmlPullParser xmlPullParser = Xml.newPullParser();
//	      boolean targetTag = false;
		try {
			String name = "";
			xmlPullParser.setInput(new StringReader(xml));
			for (int eventType = xmlPullParser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = xmlPullParser.next()) {
				DebugLog.instance.outputLog("value", "adxml_eventType_" + eventType + "/" + xmlPullParser.getName());
				if (eventType == XmlPullParser.START_TAG) {
//	                  DebugLog.instance.outputLog("value", "response XML_if(eventType == XmlPullParser.START_TAG){_" + eventType+"_"+XmlPullParser.START_TAG);
					name = xmlPullParser.getName();
					if (name != null) {

//	                      if(startTag.equals(xmlPullParser.getName())){
//	                          targetTag = true;
//	                      }
						if (name.equals(AD_INFO_ITEM_TAG)) {
							DebugLog.instance.outputLog("value", "_item start!!!!!!!!!!!!");
							isItemTag = true;
							itemArray.add(new AdBannerLayout());
						}
//	                      DebugLog.instance.outputLog("value", "response XML_xmlPullParser.getName()_startTag_targetTag_" + name +"_"+ AD_INFO_ITEM_TAG +"_"+ isItemTag);
					}
				}
				//item tag
				else if (eventType == XmlPullParser.TEXT) {//<Version><VersionUpComment>のところでエラーになる対応
//	                  DebugLog.instance.outputLog("value", "response XML_else if(targetTag&& eventType == XmlPullParser.TEXT){_");
//	                  DebugLog.instance.outputLog("value", "response XML_}else if(targetTag){_xmlPullParser.getName()_" + name);

					String val = xmlPullParser.getText();

					DebugLog.instance.outputLog("value", "adxml_text_" + val);
					if (isItemTag) {
						if (name.equals(AD_INFO_BANNER_AREA_TAG)) {
							itemArray.get(itemArray.size() - 1).area = Integer.parseInt(val);
						} else if (name.equals(AD_INFO_BANNER_ID_TAG)) {
							itemArray.get(itemArray.size() - 1).bannerId = val;
						} else if (name.equals(AD_INFO_WEIGHT_TAG)) {
							itemArray.get(itemArray.size() - 1).weight = Integer.parseInt(val);
						} else if (name.equals(AD_INFO_IMAGE_TAG)) {
							itemArray.get(itemArray.size() - 1).imageURL = val;
//	                     	DebugLog.instance.outputLog("value", (itemArray.size - 1) + "_imageURL" +"_"+ val);
						} else if (name.equals(AD_INFO_LINK_TAG)) {
							itemArray.get(itemArray.size() - 1).linkURL = val;
						} else if (name.equals(AD_INFO_BLOCKABLE_TAG)) {
							itemArray.get(itemArray.size() - 1).isBlockable = Boolean.parseBoolean(val);
						}

					} else {
						if (name.equals("Result")) {
							if (!val.equals("0")) {
								return null;
							}
						} else if (name.equals("NumberOfResult")) {
							adNum = Integer.parseInt(val);
						}
					}

					name = "";
				}
				//tes
				else if (eventType == XmlPullParser.END_TAG) {
//	                  DebugLog.instance.outputLog("value", "}else if(eventType == XmlPullParser.END_TAG){_");
//	                  String name = xmlPullParser.getName();
					name = xmlPullParser.getName();
//	                  if(name != null){
//	                      if(startTag.equals(xmlPullParser.getName())){
//	                          targetTag = false;
//	                      }
//	                  }
					if (name != null) {
						if (name.equals(AD_INFO_ITEM_TAG)) {
							isItemTag = false;
						}
					}
				}

			}
		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "response XML_} catch (Exception e) {_" + e.toString());
			return null;
		}

		if (itemArray.size() != adNum) return null;

		for (int i = 0; i < itemArray.size(); i++) {
			if (itemArray.get(i).area != gateAreaCode) {
//	    		  itemArray.removeIndex(i);
				itemArray.remove(i);
				i--;
			}
		}
		return itemArray;
	}


	public final int APP_FINISH_ACTION = 0;
	public final int SPP_FAILED_REASON_AUTH_ERROR = 1;
	public final int SPP_FAILED_REASON_NETWORK_ERROR = 2;
	public final int SPP_FAILED_REASON_BASE_APP_INVALID = 3;
	public final int SPP_FAILED_REASON_NOT_REGULAR_MEMBER = 4;
	public final int SPP_FAILED_REASON_NOT_MEMBER = 5;

	private void initRunnable() {
		runnableForGetVersionInfo = new Runnable() {
			@Override
			public void run() {
//				 此処でDisneyマーケットアプリの有無確認
				//Disney Market or Disney passが入ってるかチェック
				if (!SPPUtility.isAppInstalled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")) {

					if (SPPUtility.getCarrierID(getApplicationContext()).equals(SPPUtility.AUTH_CARRIER_AU)) {
						//auマーケットが入っていたら
						if (SPPUtility.isAppInstalled(getApplicationContext(), "com.kddi.market") && SPPUtility.isAppEnabled(getApplicationContext(), "com.kddi.market")) {
							//
							Toast.makeText(getApplicationContext(), "Disney passをインストールして下さい。", Toast.LENGTH_LONG).show();
							Intent intent = new Intent(Intent.ACTION_VIEW,
									Uri.parse("auonemkt://details?id=8588000000001"));
							startActivity(intent);
							finish();
							return;

						}
					}

					//ダウンロード開始用Activity
					Intent intent = new Intent();
					intent.setClassName(getPackageName(), "jp.co.disney.apps.managed.kisekaeapp.spp.BaseAppDownloadActivity");
					startActivity(intent);
					finish();
					return;
					//ここで終了------------------------------------------------------------------------------------
				}

				if (!SPPUtility.isAppEnabled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")) {
					showDialogFragment(SPP_FAILED_REASON_BASE_APP_INVALID);
				} else {
					DebugLog.instance.outputLog("value", "GetVersionInfo　start");
					//SPP認証	ここから
					Intent sppIntent = new Intent(Intent.ACTION_MAIN);
					ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
					sppIntent.setComponent(compo);

					ApplicationInfo appliInfo = null;
					try {
						appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
						sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
						sppIntent.putExtra("argFunc", 210);
						sppIntent.putExtra("argPackagename", getPackageName());
						sppIntent.putExtra("argRegistCheck", "0");

						sppIntent.putExtra("argMemberCheck", "1");//taihi
//						sppIntent.putExtra("argMemberCheck", "0");//tes×
//						sppIntent.putExtra("argMemberCheck", "2");//tes×

						sppIntent.putExtra("argAvailableCheck", "0");
						sppIntent.putExtra("argCarrierCheck", "1");//TODO
						sppIntent.putExtra("argUserProfile", "1");

						nowGetVersionInfo = true;
						startActivityForResult(sppIntent, GET_VERSION_INFO_CODE);
					} catch (NameNotFoundException e) {
						showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
					}
					//	ここまで
				}


			}
		};

		runnableForGetAuthInfo = new Runnable() {

			@Override
			public void run() {
				if (!SPPUtility.isAppEnabled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")) {
					showDialogFragment(SPP_FAILED_REASON_BASE_APP_INVALID);
				} else {
					DebugLog.instance.outputLog("value", "GetAuthInfo　start");

					//GetAuthInfo
					Intent sppIntent = new Intent(Intent.ACTION_MAIN);
					ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
					sppIntent.setComponent(compo);

					ApplicationInfo appliInfo = null;
					try {
						appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
						sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
						sppIntent.putExtra("argFunc", 214);
						startActivityForResult(sppIntent, GET_AUTH_INFO_CODE);
					} catch (NameNotFoundException e) {
					}
				}

			}
		};


		runnableForGetAuthInfoFromWeb = new Runnable() {

			@Override
			public void run() {
				if (!SPPUtility.isAppEnabled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")) {
					showDialogFragment(SPP_FAILED_REASON_BASE_APP_INVALID);
				} else {
					DebugLog.instance.outputLog("value", "GetAuthInfo　start");

					//GetAuthInfo
					Intent sppIntent = new Intent(Intent.ACTION_MAIN);
					ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
					sppIntent.setComponent(compo);

					ApplicationInfo appliInfo = null;
					try {
						appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
						sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
						sppIntent.putExtra("argFunc", 214);
						startActivityForResult(sppIntent, GET_AUTH_INFO_CODE_FROM_WEB);
					} catch (NameNotFoundException e) {
					}
				}

			}
		};


		runnableForGetAdInfo = new Runnable() {

			@Override
			public void run() {
				//GetAdInfo
				//初回起動時はバナーは表示しない
//   				SharedPreferences prefs = getSharedPreferences(CatalogScreen.SCREEN_PREFERENCE_NAME, MODE_PRIVATE);
//   				if(!prefs.getBoolean("finishTutrial", false)){
//   					((CatalogScreen)launcherlistener).onFinishBannerLoading();
//   					startDownloadAllData();
//   				}else{
				if (SPPUtility.checkNetwork(getApplicationContext())) {
					DebugLog.instance.outputLog("value", "GetAdInfo Start");

					layout.setVisibility(View.GONE);

					Intent sppIntent = new Intent(Intent.ACTION_MAIN);
					ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
					sppIntent.setComponent(compo);

					ApplicationInfo appliInfo = null;
					try {
						appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
						sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
						sppIntent.putExtra("argFunc", 202);
						sppIntent.putExtra("argAreaCode", "15");
						startActivityForResult(sppIntent, GET_AD_INFO_CODE);
					} catch (NameNotFoundException e) {
						selectAction();
					}

				} else {
					showDialogFragment(SPP_FAILED_REASON_NETWORK_ERROR);
				}

			}
		};

		runnableForRegistSPP = new Runnable() {
			@Override
			public void run() {
				if (!SPPUtility.isAppEnabled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")) {
					showDialogFragment(SPP_FAILED_REASON_BASE_APP_INVALID);
				} else {
					DebugLog.instance.outputLog("value", "RegistSPP　start");

					//GetAuthInfo
					Intent sppIntent = new Intent(Intent.ACTION_MAIN);
					ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
					sppIntent.setComponent(compo);

					ApplicationInfo appliInfo = null;
					try {
						appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
						sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
						sppIntent.putExtra("argFunc", 212);
//			            sppIntent.putExtra("argRedirectUri", encodeParameter("dshareregist://sp-share.disney.co.jp"));
						startActivityForResult(sppIntent, GET_SPP_REGIST_CODE);
					} catch (NameNotFoundException e) {
					}
				}

			}
		};

	}

	public void FinishApp() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						finish();
//		        	exit();
					}
				});
			}
		}).start();
	}

	public String encodeParameter(String p) {
		try {
			return URLEncoder.encode(p, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}

	private void showDialogFragment(int reason) {

		Bundle args = new Bundle();
		args.putInt("finish_reason", reason);

		FragmentManager fm = getFragmentManager();
		MainFragmentDialog dialog = new MainFragmentDialog();
		dialog.setArguments(args);
		if (reason != 0) dialog.setCancelable(false);
		dialog.show(fm, String.valueOf(reason));
	}

	public class MainFragmentDialog extends DialogFragment {
		//static class MainFragmentDialog extends DialogFragment {
//
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
//		 static Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle args = getArguments();
			if (args == null) return null;

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setView(null);

			int reason = args.getInt("finish_reason", 0);
			if (reason == APP_FINISH_ACTION) {
				builder.setMessage("アプリを終了しますか？");

				builder.setNegativeButton("いいえ", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				});
				builder.setPositiveButton("はい", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						FinishApp();
					}
				});

			} else {
				switch (reason) {
					default:
					case SPP_FAILED_REASON_AUTH_ERROR:
						builder.setMessage(R.string.invalid_app_fig2);
						break;
					case SPP_FAILED_REASON_NETWORK_ERROR:
						builder.setMessage(R.string.invalid_app_fig1);
						break;
					case SPP_FAILED_REASON_BASE_APP_INVALID:
						builder.setMessage(R.string.invalid_app_fig3);
						break;
					case SPP_FAILED_REASON_NOT_REGULAR_MEMBER:
						builder.setMessage(R.string.invalid_app_fig4);
						break;
					case SPP_FAILED_REASON_NOT_MEMBER:
						if (isDMfromGVI) {
							//DMの場合は非会員以外の理由の可能性アリのため通常の認証エラー表示
							reason = SPP_FAILED_REASON_AUTH_ERROR;
							builder.setMessage(R.string.invalid_app_fig2);
						} else {
							builder.setMessage(R.string.invalid_app_fig5);
						}
						break;
//					case -1:
//						reason = SPP_FAILED_REASON_NOT_REGULAR_MEMBER;
//						builder.setMessage(R.string.invalid_app_fig4);
//						break;
				}

				if (reason == SPP_FAILED_REASON_NOT_REGULAR_MEMBER) {
					//登録
					builder.setPositiveButton(R.string.for_invitation_dialog_btn1, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							handler.post(runnableForRegistSPP);
						}
					});

//						//ログイン
//						builder.setNeutralButton(R.string.for_invitation_dialog_btn2, new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								FinishApp();
//							}
//						});

					//招待を受けない
					builder.setNegativeButton(R.string.for_invitation_dialog_btn3, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//ダイアログを閉じる
							if (dialog != null) dismiss();
							//招待ではなくNORMAL
							nowIntentInfo.intent_state = IntentState.NORMAL;
							nowIntentInfo.intent_data = "";
							selectAction();
						}
					});

				} else {
					builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							FinishApp();
						}
					});

				}

				builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_BACK:
								DebugLog.instance.outputLog("value", "back key pressed!");
								FinishApp();
								return true;
							default:
								return false;
						}
					}
				});

			}

			AlertDialog dialog = builder.create();

			if (reason != 0) dialog.setCancelable(false);

			return dialog;
		}
	}


	@Override
	public void onFailedCheckUserStatus(int reason) {
		if (reason == SppCheckUserStatusAsyncTask.LOGIN_INVALID) {
			showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
		} else {
			showDialogFragment(SPP_FAILED_REASON_NETWORK_ERROR);
		}

	}

	@Override
	public void onFinishedCheckUserStatus(String xmid, boolean isregular, int businessDomain, boolean ispremium) {
		myInfo.is_standard = isregular;
		myInfo.xmid = xmid;

		if (myInfo.xmid.equals("")) {
			//見込みと判断
			myInfo.is_standard = false;
		}

		if (ispremium) {
			myInfo.is_premium = 1;
		} else {
			myInfo.is_premium = 0;
		}
		DebugLog.instance.outputLog("value", "isPremium = " + myInfo.is_premium);

		//ビジネスドメイン確認
		switch (businessDomain) {
			default:
			case 0:
				DebugLog.instance.outputLog("value", "Not Member");
				showDialogFragment(SPP_FAILED_REASON_NETWORK_ERROR);
				break;
			case 2:
				DebugLog.instance.outputLog("value", "DisneyMobileOnDocomo");

				myInfo.status_code = UserStatusCode.standardOnD;

				break;
			case 3:
				DebugLog.instance.outputLog("value", "DisneyMobileOnSoftBank");

				myInfo.status_code = UserStatusCode.standardOnS;

				break;
			case 4:
				DebugLog.instance.outputLog("value", "Kddi");

				if (myInfo.is_standard) {
					myInfo.status_code = UserStatusCode.standardCPK;
				} else {
					DebugLog.instance.outputLog("value", "Kddi_見込み");
					myInfo.status_code = UserStatusCode.singleCPK;
				}
				break;
			case 5:
				DebugLog.instance.outputLog("value", "SoftBankMobile");

				if (myInfo.is_standard) {
					myInfo.status_code = UserStatusCode.standardCPS;
				} else {
					DebugLog.instance.outputLog("value", "SoftBankMobile_見込み");
					myInfo.status_code = UserStatusCode.singleCPS;
				}
				break;
		}

		//招待からの時に見込み会員だったらダイアログ表示、それ以外の場合は次のGetAdInfoに進む
		if (!myInfo.is_standard) {
//			if( nowIntentInfo.intent_data != null && !nowIntentInfo.intent_data.equals("") ){
//				if(nowIntentInfo.intent_data.indexOf("/room/") != -1 && nowIntentInfo.intent_data.indexOf("sp-share.disney.co.jp") != -1){
//					showDialogFragment(SPP_FAILED_REASON_NOT_REGULAR_MEMBER);
//					return;
//				}
//			}

			if (nowIntentInfo.intent_state == IntentState.FROMINVIURL
					|| nowIntentInfo.intent_state == IntentState.FROMWEB_FORINVIURL) {
				showDialogFragment(SPP_FAILED_REASON_NOT_REGULAR_MEMBER);
				return;
			}
		}

		if (businessDomain != 0) handler.post(runnableForGetAdInfo);

	}

	private boolean isDMfromGVI = false;

	public static HashMap<String, String> parseResultXML(String xml, String startTag) {
//      DebugLog.instance.outputLog("value", "response XML:" + xml);
		DebugLog.instance.outputLog("value", "response XML_startTag_" + xml + "_" + startTag);
		HashMap<String, String> map = new HashMap<String, String>();
		XmlPullParser xmlPullParser = Xml.newPullParser();
		boolean targetTag = false;
		try {
			String name = "";
			xmlPullParser.setInput(new StringReader(xml));
			for (int eventType = xmlPullParser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = xmlPullParser.next()) {
				DebugLog.instance.outputLog("value", "xmlPullParser_eventType_" + eventType);
				if (eventType == XmlPullParser.START_TAG) {
					DebugLog.instance.outputLog("value", "response XML_if(eventType == XmlPullParser.START_TAG){_" + eventType + "_" + XmlPullParser.START_TAG);
					name = xmlPullParser.getName();
					if (name != null) {
						DebugLog.instance.outputLog("value", "response XML_xmlPullParser.getName()_startTag_targetTag_" + name + "_" + startTag + "_" + targetTag);

						if (startTag.equals(xmlPullParser.getName())) {
							targetTag = true;
						}
					}
				}
				//tes
				else if (targetTag && eventType == XmlPullParser.TEXT) {//<Version><VersionUpComment>のところでエラーになる対応
					DebugLog.instance.outputLog("value", "response XML_else if(targetTag&& eventType == XmlPullParser.TEXT){_");
					DebugLog.instance.outputLog("value", "response XML_}else if(targetTag){_xmlPullParser.getName()_" + name);
					String val = xmlPullParser.getText();
					map.put(name, val);
					DebugLog.instance.outputLog("value", "response XML_map.put(name, val);_name_val_" + name + "_" + val);
					name = "";
				}
				//tes
				else if (eventType == XmlPullParser.END_TAG) {
					DebugLog.instance.outputLog("value", "}else if(eventType == XmlPullParser.END_TAG){_");
					name = xmlPullParser.getName();
					if (name != null) {
						if (startTag.equals(xmlPullParser.getName())) {
							targetTag = false;
						}
					}
				}

			}
		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "response XML_} catch (Exception e) {_");
			return null;
		}
		return map;
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		DebugLog.instance.outputLog("checkSelfPermission", "onRequestPermissionsResult_");

		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_READ_PHONE_STATE:
				if (grantResults.length > 0 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
					// パーミッションが必要な処理
					if (PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE)
							!= PackageManager.PERMISSION_GRANTED) {
						DebugLog.instance.outputLog("checkSelfPermission", "READ_PHONE_STATE != PackageManager.PERMISSION_GRANTED");
						MainActivity.this.finish();
					} else {
						DebugLog.instance.outputLog("checkSelfPermission", "READ_PHONE_STATE == PackageManager.PERMISSION_GRANTED");
					}

				}
				break;
		}

	}


	//Shareサーバに送るユーザ情報
	private class userInfo {
		//他、birthdayはsharedprefrenceに保存
		String xmid;
		UserStatusCode status_code;
		boolean is_standard = false;
		String accessToken = "";
		String idToken = "";
		int is_premium = 0;
	}

	//受け取ったintentの情報
	private class IntentInfo {
		IntentState intent_state = null;
		String intent_data = "";


		void getIntentInfo(Intent i) {
			DebugLog.instance.outputLog("value", "-----------getIntentInfo-------------");
			nowIntentInfo.intent_data = i.getDataString();

			if (nowIntentInfo.intent_data != null && !nowIntentInfo.intent_data.equals("")) {
				DebugLog.instance.outputLog("value", "MainActivity_intent_data_extra_" + nowIntentInfo.intent_data);
				nowIntentInfo.intent_state = setIntentState(nowIntentInfo.intent_data);

			} else {
				if (i.getStringExtra("data") != null && !i.getStringExtra("data").equals("")) {
					nowIntentInfo.intent_data = i.getStringExtra("data");
					DebugLog.instance.outputLog("value", "MainActivity_intent_data_extra_" + nowIntentInfo.intent_data);
					nowIntentInfo.intent_state = setIntentState(nowIntentInfo.intent_data);

				} else {
					nowIntentInfo.intent_data = "";
					nowIntentInfo.intent_state = IntentState.NORMAL;
				}
			}
//			nowIntentState = setIntentState(mStartupData);

		}

		/**
		 * 受け取ったintentからアプリ起動の意図を判別
		 *
		 * @param scheme
		 * @return
		 */
		private IntentState setIntentState(String scheme) {
			if (scheme == null || scheme.equals("")) {
				return IntentState.NORMAL;
			}

			if (scheme.startsWith("http") && scheme.indexOf("/room/") != -1) {
				//招待URL直
				return IntentState.FROMINVIURL;
			} else if (scheme.startsWith("disneyshare://sp-share.disney.co.jp/")) {
				if (scheme.indexOf("roomparam=") != -1) {
					//招待URLでブラウザに行ったフローからの起動
					return IntentState.FROMWEB_FORINVIURL;

				} else if (scheme.indexOf("startweather") != -1) {
					//天気予報アプリ起動
					return IntentState.FROMWEBVIEW_FORWEATHER;

				} else if (scheme.indexOf("startvoicem") != -1) {
					//ボイスメッセージ起動
					return IntentState.FROMWEBVIEW_FORVOICE;

				} else if (scheme.indexOf("startgreeting") != -1) {
					//グリーティングカード起動
					return IntentState.FROMWEBVIEW_FORGREETING;

				} else if (scheme.indexOf("startprofilemake") != -1) {
					//プロフィールメーカー起動
					return IntentState.FROMWEBVIEW_FORPROFILE;

				} else if (scheme.indexOf("startgettoken") != -1) {
					//Token再取得
					return IntentState.DO_GETAUTHINFO;

				} else {
					return IntentState.NORMAL;

				}
			} else if (scheme.startsWith("dmarketinternal://" + getPackageName())) {
				//BaseAppからのDisneyコンテンツ詳細画面表示
				return IntentState.FROM_BASEAPP_D_CONTENTS;

			} else {
				return IntentState.NORMAL;
			}
		}

	}
}
