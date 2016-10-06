package jp.co.disney.apps.dm.disneyshare.spp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import jp.co.disney.apps.dm.disneyshare.DebugLog;
import jp.co.disney.apps.dm.disneyshare.R;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class SppCheckUserStatusAsyncTask extends
		AsyncTask<String, Integer, Boolean>{


	private ProgressDialog dialog = null;
	Context myContext;
	private SppCheckUserStatusTaskCallback callback;

	private Timer mTimer = null;
	private long startTime = 0;

	public SppCheckUserStatusAsyncTask(Context context) {
		super();
		myContext = context;
		callback = (SppCheckUserStatusTaskCallback) context;
	}

//	public SppCheckDisneyStyleAsyncTask(Context context, SPPCheckDisneyStyleTaskCallback c) {
//		super();
//		myContext = context;
//		callback = c;
//	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		DebugLog.instance.outputLog("value", "sppcheck start!");
		dialog = new ProgressDialog(myContext, R.style.MyDialogTheme);
//		dialog = new ProgressDialog(myContext);
		// スタイルを設定
//		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setProgressStyle(android.R.style.Widget_ProgressBar);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();

		//開始時刻を取得
		startTime = System.currentTimeMillis();

		mTimer = new java.util.Timer(true);
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if((System.currentTimeMillis() - startTime) >= 10 * 1000){//計10秒
					DebugLog.instance.outputLog("value", "timeout!!!!!");
					mTimer.cancel();
					mTimer = null;
					callCancel();
				}
				DebugLog.instance.outputLog("value", "time:" + (System.currentTimeMillis() - startTime));
			}

		}, 100, 100);


	}

	private void callCancel(){
		cancel(true);
	}

	
	public static final int regular = 1, mikomi = 2, AUTH_ERROR = -1, OFFLINE = -2, LOGIN_INVALID = -3;

	/**
	 * 課金会員としての状況を確認する
	 * returnは成否、状況の詳細は別途フィールド変数に格納する
	 */
	@Override
	protected Boolean doInBackground(String... arg0) {
		//保存動作。dismiss時には止める
		DebugLog.instance.outputLog("value", "Disney有料会員チェック");

//		try {
//			Thread.sleep(4000);
//		} catch (Exception e) {
//		}
		
		
		InputStream in = null;
		HttpURLConnection conn = null;
		try {
				if(isCancelled()){
					finishReason = OFFLINE;
					return false;
				}

				//オフラインチェック
//				AppDataSaved app = (AppDataSaved) ((SplashActivity)myContext).getApplication();
				if(!SPPUtility.checkNetwork(myContext)){
					DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:オフライン");

//					if(app.getDisneyStyleStatus().equals("")){//前回値が存在しないので初回起動とみなす
//						finishReason = SPPUtility.OFFLINE30;
//						return false;
//					}else{
//						if(app.getDisneyStyleStatus().equals("true")){
//							//連続30日以内をチェック
//							if(app.checkOfflineDate() >= 30){//30日越えていたら使えない
//								//使えない
//								finishReason = SPPUtility.OFFLINE30;
//								return false;
//							}else{
//								//チェックせずに使える。
//								return true;
//							}
//						}else if(app.getDisneyStyleStatus().equals("false")){
//							finishReason = SPPUtility.OFFLINE30;
//							return false;
//						}
//					}
					
					finishReason = OFFLINE;
					return false;

				}

				DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:オフラインチェック完了");

				if(isCancelled()){
					finishReason = OFFLINE;
					return false;
				}

				//チェック
				String[] param = SPPUtility.getConnectEnv(myContext.getApplicationContext());
				String domain = "ssapi.disney.co.jp";
				if(param[0].equals("liv")){
					domain = "ssapi.disney.co.jp";
				}else if(param[0].equals("stg")){
					domain = "staging.ssapi.disney.co.jp";
				}

				URL url = new URL("https://" + domain + "/webapi/v1/SPPJudgment");
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-type", "application/json");
				conn.setRequestProperty("Accept","application/json");
				conn.setDoOutput(true);
				
				/*
https://www.google.co.jp/?client=firefox-b#q=android+httpurlconnection+post+body&gfe_rd=cr
http://stackoverflow.com/questions/20020902/android-httpurlconnection-how-to-set-post-data-in-http-body
http://osa030.hatenablog.com/entry/2015/05/22/181155
http://yukimura1227.blog.fc2.com/blog-entry-36.html
				 */

//				HttpPost httpPost = new HttpPost("https://" + domain + "/webapi/v1/SPPJudgment");
//				httpPost.setHeader("Content-type", "application/json");
//				httpPost.setHeader("Accept","application/json");
//				HttpClient httpClient = new DefaultHttpClient();
//				//http://hc.apache.org/httpclient-3.x/preference-api.html
//				httpClient.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_1);

				if(isCancelled()){
					finishReason = OFFLINE;
					return false;
				}

				DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:indentifier追加前");
				
				TelephonyManager manager = (TelephonyManager) myContext.getSystemService(Context.TELEPHONY_SERVICE);
				String id = manager.getSimSerialNumber();
				if(id == null || id.equals("")){
					finishReason = AUTH_ERROR;
					return false;
				}


				//body部に追加
				//http://stackoverflow.com/questions/18188041/write-in-body-request-with-httpclient
				String postDataBody = SPPUtility.createDeviceIdentifier(myContext);//ここに本文を入れる。
				
				byte[] outputInBytes = postDataBody.getBytes("UTF-8");
				OutputStream os = conn.getOutputStream();
				os.write( outputInBytes );    
				os.close();
				
//				HttpEntity entity = null;
//				try {
//					entity = new ByteArrayEntity(postDataBody.getBytes("UTF-8"));
//				} catch (UnsupportedEncodingException e1) {
//					e1.printStackTrace();
//				}
//				httpPost.setEntity(entity);
				
				
//				DebugLog.instance.outputLog("value", "GetUserProfileIntentService::postDataBody" + postDataBody);
				DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:接続直前_" + url.toString());


				conn.connect();
				
				in = conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
 
                // InputStreamからbyteデータを取得するための変数
                StringBuffer bufStr = new StringBuffer();
                String temp = null;
 
                // InputStreamからのデータを文字列として取得する
                while((temp = br.readLine()) != null) {
                    bufStr.append(temp);
                }
 
                String responseBody = null;
                responseBody = bufStr.toString();
				
				
//				TelephonyManager manager = (TelephonyManager) myContext.getSystemService(Context.TELEPHONY_SERVICE);
//				String id = manager.getSimSerialNumber();
//				if(id!=null&&!id.equals("")){
//				try {
//					responseBody = httpClient.execute(httpPost, new ResponseHandler<String>() {
//						@Override
//						public String handleResponse(HttpResponse response)
//								throws ClientProtocolException, IOException {
////							DebugLog.instance.outputLog("value", "GetUserProfileIntentService::getStatusCode:" + response.getStatusLine().getStatusCode());
//							if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode()){
//								return EntityUtils.toString(response.getEntity(), "UTF-8");
//							}
//							return null;
////							return "";
//						}
//					});
//				} catch (ClientProtocolException e) {
//					e.printStackTrace();
//					finishReason = AUTH_ERROR;
//					return false;
//				} catch (IOException e) {
//					e.printStackTrace();
//					finishReason = AUTH_ERROR;
//					return false;
//				} finally {
//					httpClient.getConnectionManager().shutdown();
//				}
//
//				}else{
//					responseBody ="";
//				}

				if(responseBody != null) parseResponse(responseBody);

				//この時、sharedprefにユーザの誕生日が保存されていなかったら取得して保存する必要アリ。
				if(isRegularMember){//見込み会員はSPP登録がないので誕生日取得は行わない
					SharedPreferences userP = myContext.getSharedPreferences(SPPUtility.USER_INFO_PREF, Context.MODE_PRIVATE);
//					if(userP.getString(SPPUtility.USER_BIRTHDAY, "").equals("") || userP.getString(SPPUtility.USER_AGEBAND, "").equals("")){
						if(!getUserBirthday(myContext)){
							return false;
						}
//					}
				}
				
				return true;

		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "CatchException in doInBackground:" + e.getMessage());
			finishReason = AUTH_ERROR;
			return false;

		} finally {
			try {
                if(conn != null) conn.disconnect();
                if(in != null) in.close();

            } catch (IOException ioe ) {
                ioe.printStackTrace();
            }
		}
	}
	
	boolean isRegularMember = false;
	String xmid = "";
	int bd = 0;
	boolean isPremium = false;

	/**
	 * json解析
	 */
	private void parseResponse(String jsonStr){
		//レスポンスを解析してuser_profileを保存
		//あとここで入会退会判定の結果も取得しておく
		DebugLog.instance.outputLog("value", "SppCheckDisneyStyle//////////////////response:" + jsonStr);

		//price planが"disney style"
//		String price_plan = "";
//		String xcid = "";

		//解析
		try {
			JSONObject rootObj = new JSONObject(jsonStr);
			JSONObject parseObj = rootObj.getJSONObject("spp_judgment");
			
			//加入ビジネスドメインがDisneyPassやDisneyMarket（S)やDineyStyleだった場合は「見込み」状態が存在するので見込みが登録済みかをチェック
//			String bd = parseObj.getString("business_domain_code");
			if(parseObj.getBoolean("is_spp3_registered")){
				isRegularMember = true;
			}

			bd = Integer.parseInt(parseObj.getString("business_domain_code"));
			if(bd == 1){
				if(jsonStr.indexOf("BDM=BlackMarket") != -1){//ビジネスドメインが1で、BlackMarketだったらonSと見なす
					bd = 3;
				}
			}
			
			isPremium = Boolean.parseBoolean(parseObj.getString("is_premium"));

			xmid = parseObj.getString("xmid");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		//nullだったら仮で入れておく
//		if(price_plan == null || price_plan.equals("null")) price_plan = "";
		if(xmid == null || xmid.equals("null")) xmid = "";

//		if(price_plan.equals("disney style")){
//			//入会中
//
//			//XCIDをDBに格納
//			DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:xcid:" + xcid);
//
//		}else{
//			//非入会
//		}
	}
	
	
	
	//IDトークンを使用してユーザの生年月日を取得する
	public boolean getUserBirthday(Context context) throws JSONException{
		String birthday = "", ageBand = "";
		
		SharedPreferences preferences = context.getSharedPreferences(SPPUtility.GET_TOKEN_PREF, Context.MODE_PRIVATE);
		String idToken = preferences.getString(SPPUtility.PREF_KEY_ID_TOKEN, "");
		String accessToken = preferences.getString(SPPUtility.PREF_KEY_ACCESS_TOKEN, "");

		InputStream in = null;
		HttpURLConnection conn = null;

		//チェック
		String[] param = SPPUtility.getConnectEnv(myContext.getApplicationContext());
		String domain = "ssapi.disney.co.jp";
		
		if(param[0].equals("liv")){
			domain = "ssapi.disney.co.jp";
		}else if(param[0].equals("stg")){
			domain = "staging.ssapi.disney.co.jp";
		}else if(param[0].equals("dev")){
			domain = "dev.ssapi.disney.co.jp";
		}
		
		DebugLog.instance.outputLog("value", "environment:" + param[0]);
		
		URL url;
		try {
			url = new URL("https://" + domain + "/webapi/v2/SPPInformation?id_token=" + idToken + "&did=1");
			
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Authorization", "Bearer " + accessToken);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setDoInput(true);

			if(isCancelled()){
				finishReason = OFFLINE;
				return false;
			}

			DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:connect直前" + url.toString());

			conn.connect();
			
//			DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:connect直後");
//			DebugLog.instance.outputLog("value", "getResponseCode:" + conn.getResponseCode());

			in = conn.getInputStream();
			
//			DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:getInputStream直後");
			
			
	        BufferedReader br = new BufferedReader(new InputStreamReader(in));

	        // InputStreamからbyteデータを取得するための変数
	        StringBuffer bufStr = new StringBuffer();
	        String temp = null;

	        // InputStreamからのデータを文字列として取得する
	        while((temp = br.readLine()) != null) {
	            bufStr.append(temp);
	        }

	        String responseBody = null;
	        responseBody = bufStr.toString();
	        
	        DebugLog.instance.outputLog("value", "responseBody:" + responseBody);

			if(responseBody != null && !responseBody.equals("")){
				//解析
				JSONObject root = new JSONObject(responseBody);
				JSONObject member = root.getJSONObject("spp_member_details");
				birthday = member.getString("date_of_birth");
				ageBand = member.getString("age_band");
			}

			DebugLog.instance.outputLog("value", "getUserBirthday:birthday" + birthday);
			
			//sharedprefに保存する
			SharedPreferences userPref = myContext.getSharedPreferences(SPPUtility.USER_INFO_PREF, Context.MODE_PRIVATE);
			SharedPreferences.Editor userEditor = userPref.edit();
			
			userEditor.putString(SPPUtility.USER_BIRTHDAY, birthday);
			userEditor.putString(SPPUtility.USER_AGEBAND, ageBand);
			
			userEditor.commit();
			
			return true;

		} catch (MalformedURLException e) {
			e.printStackTrace();
			DebugLog.instance.outputLog("value", "getUserBirthday:birthday:error:" + e.toString());
			DebugLog.instance.outputLog("value", "getUserBirthday:birthday:error:" + e.getMessage());
			finishReason = AUTH_ERROR;
			return false;
		} catch (IOException e2) {
			DebugLog.instance.outputLog("value", "getUserBirthday:birthday:error:" + e2.toString());
			DebugLog.instance.outputLog("value", "getUserBirthday:birthday:error:" + e2.getMessage());
			
			try {
				BufferedReader reader = reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
				StringBuilder sb = new StringBuilder();
				char[] b = new char[1024];
				int line;
				while (0 <= (line = reader.read(b))) {
				    sb.append(b, 0, line);
				}
				DebugLog.instance.outputLog("value", "failed!!!!!!!!!_" + sb.toString());
				
				//TODO "code":"010107"だったらログインを促す。
				JSONObject root = new JSONObject(sb.toString());
				JSONObject err = root.getJSONObject("error");
				String code = err.getString("code");
				if(code.equals("010107")){
					finishReason = LOGIN_INVALID;
				}

			} catch (IOException e) {
				e.printStackTrace();
			}



			e2.printStackTrace();
			if(finishReason != LOGIN_INVALID) finishReason = AUTH_ERROR;
			return false;
		}

		
//		String response = "";
//		try {
//			response = httpClient.execute(httpGet, new ResponseHandler<String>() {
//				@Override
//				public String handleResponse(HttpResponse response)
//						throws ClientProtocolException, IOException {
//					DebugLog.instance.outputLog("value", "getUserBirthday::getStatusLine:" + response.getStatusLine());
//					DebugLog.instance.outputLog("value", "getUserBirthday::getStatusCode:" + response.getStatusLine().getStatusCode());
//					DebugLog.instance.outputLog("value", "getUserBirthday::getProtocolVersion:" + response.getStatusLine().getProtocolVersion());
//					return EntityUtils.toString(response.getEntity(), "UTF-8");
//				}
//			});
//		} catch (ClientProtocolException e) {
//			e.printStackTrace();
//			response = e.getMessage();
//		} catch (IOException e) {
//			e.printStackTrace();
//			response = e.getMessage();
//		} finally {
//			httpClient.getConnectionManager().shutdown();
//		}
//
//		DebugLog.instance.outputLog("value", "getUserBirthday:responseBody" + response);

	}

	

	@Override
	protected void onProgressUpdate(Integer... values) {
		//super.onProgressUpdate(values);
//		callback.onProgressDownloadThumbs(values[0]);
	}

	@Override
	protected void onCancelled() {
		dialog.dismiss();

		DebugLog.instance.outputLog("value", "DisneyStyle会員チェックonCancelled");
		callback.onFailedCheckUserStatus(finishReason);
		super.onCancelled();
	}

	private int finishReason = 0;
	@Override
	protected void onPostExecute(Boolean result) {
		dialog.dismiss();
		//super.onPostExecute(result);

        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }

		if(result){
			DebugLog.instance.outputLog("value", "User Status Check success!:");
			callback.onFinishedCheckUserStatus(xmid, isRegularMember, bd, isPremium);
		}else{
			DebugLog.instance.outputLog("value", "User Status Check failed!:");
			//ダイアログを出す
			callback.onFailedCheckUserStatus(finishReason);
		}
	}


}
