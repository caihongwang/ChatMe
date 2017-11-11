package com.csipsimple.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.Context;
import android.util.Log;

/**
 * 对管理员账户进行监控，主要是让用户打不打开设置功能进、进入程序后台
 * 当没有手机卡的时候，获取默认账户
 * @author caihongwang
 *
 */
public class ConfigInfoUtil {
	
	private static final String THIS_FILE = "AdminConfigUtil";
	private static final String defaultAccountFilt = "defaultAccount.properties";
	
	/**
	 * 获取默认账户名称
	 * @return
	 */
	public static String getDefaultAccountName(Context context) {
		
		Properties props = new Properties();
		try {
			InputStream in = context.getAssets().open(defaultAccountFilt);
			props.load(in);
		} catch (IOException e) {
			Log.i("THIS_FILE", "从配置文件中获取默认账户名称读取失败...");
			e.printStackTrace();
		}
		String defaultAccountName = props.getProperty("defaultAccountName");
		Log.d("THIS_FILE", "从配置文件中获取默认账户名称  defaultAccountName = " + defaultAccountName) ;
		return defaultAccountName;
	}

	/**
	 * 获取默认账户账号
	 * @return
	 */
	public static String getDefaultAccountNumber(Context context) {
		
		Properties props = new Properties();
		try {
			InputStream in = context.getAssets().open(defaultAccountFilt);
			props.load(in);
		} catch (IOException e) {
			Log.i("THIS_FILE", "从配置文件中获取默认账户账号读取失败...");
			e.printStackTrace();
		}
		String defaultAccountNumber = props.getProperty("defaultAccountNumber");
		Log.d("THIS_FILE", "从配置文件中获取默认账户账号  defaultAccountName = " + defaultAccountNumber) ;
		return defaultAccountNumber;
	}

	/**
	 * 获取默认账户密码
	 * @return
	 */
	public static String getDefaultAccountPassword(Context context) {
		
		Properties props = new Properties();
		try {
			InputStream in = context.getAssets().open(defaultAccountFilt);
			props.load(in);
		} catch (IOException e) {
			Log.i("THIS_FILE", "从配置文件中获取默认账户密码读取失败...");
			e.printStackTrace();
		}
		String defaultAccountPassword = props.getProperty("defaultAccountPassword");
		Log.d("THIS_FILE", "从配置文件中获取默认账户密码  defaultAccountPassword = " + defaultAccountPassword) ;
		return defaultAccountPassword;
	}	
	
	/**
	 * 获取默认服务器IP和端口号
	 * @return
	 */
	public static String getDefaultServerIpAndPort(Context context) {
		
		Properties props = new Properties();
		try {
			InputStream in = context.getAssets().open(defaultAccountFilt);
			props.load(in);
		} catch (IOException e) {
			Log.i("THIS_FILE", "从配置文件中获取默认服务器IP和端口号读取失败...");
			e.printStackTrace();
		}
		String defaultServerIpAndPort = props.getProperty("defaultServerIpAndPort");
		Log.d("THIS_FILE", "从配置文件中获取默认服务器IP和端口号  defaultServerIpAndPort = " + defaultServerIpAndPort) ;
		return defaultServerIpAndPort;
	}

	
	/**
	 * 获取服务器IP和端口号
	 * @return
	 */
	public static String getServerIpAndPort(Context context) {
		
		Properties props = new Properties();
		try {
			InputStream in = context.getAssets().open("adminConfig.properties");
			props.load(in);
		} catch (IOException e) {
			Log.i("THIS_FILE", "从配置文件中服务器IP和端口号读取失败...");
			e.printStackTrace();
		}
		String ServerIpAndPort = props.getProperty("ServerIpAndPort");
		Log.d("THIS_FILE", "从配置文件中获取服务器IP和端口号  SipUserName = " + ServerIpAndPort) ;
		return ServerIpAndPort;
	}
	
	/**
	 * 获取管理员账号
	 * @return
	 */
	public static String getAdminSipUserName(Context context) {
		
		Properties props = new Properties();
		try {
			InputStream in = context.getAssets().open("adminConfig.properties");
			props.load(in);
		} catch (IOException e) {
			Log.i("THIS_FILE", "从配置文件中管理员账号读取失败...");
			e.printStackTrace();
		}
		String SipUserName = props.getProperty("SipUserName");
		Log.d("THIS_FILE", "从配置文件中获取管理员账号  SipUserName = " + SipUserName) ;
		return SipUserName;
	}
}
