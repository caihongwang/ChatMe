package com.csipsimple.wizards.impl;

import android.content.ContentUris;
import android.content.Context;
import android.text.TextUtils;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.db.DBProvider;
import com.csipsimple.utils.PreferencesWrapper;

/**
 * 将账户设置到数据库中
 * @author caihongwang
 *
 */
public class cp_Default {

	private Context context;
	
	private SipProfile account = null;

	/**
	 * 构造Advanced account
	 * @param context  当前的Context
	 * @param CallerID 缺省为空
	 * @param DisplayName 显示名称    如:1019
	 * @param AccUserName 帐号，SIP服务端要求的帐号  如：1019
	 * @param AuthId  服务器验证ID,留空则与AccUserName相同
	 * @param Password 密码  
	 * @param SipServer server的IP或者域名，可不包含端口     "42.96.162.184:5060"
	 * @param UseTcp 是tcp还是udp ,false是udp
	 * @param Proxy 代理服务器地址，IP:Port格式
	 */
	public cp_Default(Context context,
					String CallerID,
					String DisplayName,	String AccUserName,String AuthId,String Password,
					String SipServer,String Proxy,boolean UseTcp){
		
		this.context = context;
		
		//新建时，就是 SipProfile.INVALID_ID
		long accountId = SipProfile.INVALID_ID;
		//初始化一个SipPorfile
		account = SipProfile.getProfileFromDbId(context, accountId, DBProvider.ACCOUNT_FULL_PROJECTION);
		
		//开始赋值
		account.wizard = "BASIC" ;		//设置选择运营商
		account.display_name = DisplayName;
		String[] serverParts = SipServer.split(":");
		account.acc_id = "<sip:" + SipUri.encodeUser(AccUserName) + "@" + serverParts[0].trim() + ">";
		
		account.reg_uri = "sip:" + SipServer;

		account.realm = "*";
		
        account.username = CallerID;
        if (TextUtils.isEmpty(account.username)) {
            account.username = AccUserName;
        }
        
		account.data = Password;
		account.scheme = SipProfile.CRED_SCHEME_DIGEST;
		account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;

		account.transport = UseTcp ? SipProfile.TRANSPORT_UDP : SipProfile.TRANSPORT_AUTO;
		
		if (Proxy.length() > 0) {
			account.proxies = new String[] { "sip:" + Proxy };
		} else {
			account.proxies = null;
		}
	}
	/**
	 * 保存  account
	 */
	public void SaveAccount(){

		if (account.id == SipProfile.INVALID_ID) { //新建
			//清除表内现有帐户，表内只允许有一个帐户
			context.getContentResolver().delete(SipProfile.ACCOUNT_URI, null, null);
			//插入一个。如果账号或密码有一个为空，则不插入，相当于清空了帐户。
			if(!TextUtils.isEmpty(account.username) && !TextUtils.isEmpty(account.data)){
				context.getContentResolver().insert(SipProfile.ACCOUNT_URI, account.getDbContentValues());	
				SipConfigManager.setPreferenceBooleanValue(context, PreferencesWrapper.HAS_ALREADY_SETUP, true);
			}else{
				SipConfigManager.setPreferenceBooleanValue(context, PreferencesWrapper.HAS_ALREADY_SETUP, false);
			}
		} else { //更新

			context.getContentResolver().update(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, account.id), account.getDbContentValues(), null, null);
		}
		//全局配置
		//SavePrefs();
	}
	
	private void SavePrefs(){
		//这个表明是否已经设置了帐号
		SipConfigManager.setPreferenceBooleanValue(context, PreferencesWrapper.HAS_ALREADY_SETUP, true);
		// About integration
		SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.INTEGRATE_WITH_DIALER, true);
		SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.INTEGRATE_WITH_CALLLOGS, true);
		
		// About out/in mode	
	    SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.USE_3G_IN, true);
		SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.USE_3G_OUT, true);
		
		SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.USE_GPRS_IN, true);
		SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.USE_GPRS_OUT, true);
		
		SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.USE_EDGE_IN, true);
		SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.USE_EDGE_OUT, true);
		
		SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.USE_WIFI_IN, true);
		SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.USE_WIFI_OUT, true);
		
		SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.USE_OTHER_IN, true);
		SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.USE_OTHER_OUT, true);
		
		//是否仅用wifi
		SipConfigManager.setPreferenceBooleanValue(context, SipConfigManager.LOCK_WIFI, false);
	
	}
}
