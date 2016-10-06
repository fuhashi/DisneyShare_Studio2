package jp.co.disney.apps.dm.disneyshare.spp;

public interface SppCheckUserStatusTaskCallback {

	  void onFailedCheckUserStatus(int reason);
	  
	  void onFinishedCheckUserStatus(String xmid, boolean isregular, int bussinessDomain, boolean ispremium);
}
