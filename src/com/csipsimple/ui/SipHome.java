package com.csipsimple.ui;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator;
import com.actionbarsherlock.internal.utils.UtilityWrapper;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.chatme.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBProvider;
import com.csipsimple.ui.account.AccountsEditList;
import com.csipsimple.ui.calllog.CallLogListFragment;
import com.csipsimple.ui.dialpad.DialerFragment;
import com.csipsimple.ui.favorites.FavListFragment;
import com.csipsimple.ui.help.Help;
import com.csipsimple.ui.messages.ConversationsListFragment;
import com.csipsimple.ui.warnings.WarningFragment;
import com.csipsimple.ui.warnings.WarningUtils;
import com.csipsimple.ui.warnings.WarningUtils.OnWarningChanged;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.ConfigInfoUtil;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.utils.GloableUtil;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.NightlyUpdater;
import com.csipsimple.utils.NightlyUpdater.UpdaterPopupLauncher;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.utils.Theme;
import com.csipsimple.utils.UriUtils;
import com.csipsimple.utils.backup.BackupWrapper;
import com.csipsimple.wizards.BasePrefsWizard;
import com.csipsimple.wizards.WizardUtils.WizardInfo;
import com.csipsimple.wizards.impl.cp_Default;

import java.util.ArrayList;
import java.util.List;

public class SipHome extends SherlockFragmentActivity implements OnWarningChanged {
    public static final int ACCOUNTS_MENU = Menu.FIRST + 1;
    public static final int PARAMS_MENU = Menu.FIRST + 2;
    public static final int CLOSE_MENU = Menu.FIRST + 3;
    public static final int HELP_MENU = Menu.FIRST + 4;
    public static final int DISTRIB_ACCOUNT_MENU = Menu.FIRST + 5;


    private static final String THIS_FILE = "SIP_HOME";

    private final static int TAB_ID_DIALER = 0;
    private final static int TAB_ID_CALL_LOG = 1;
    private final static int TAB_ID_FAVORITES = 2;
    private final static int TAB_ID_MESSAGES = 3;
    private final static int TAB_ID_WARNING = 4;

    // protected static final int PICKUP_PHONE = 0;
    private static final int REQUEST_EDIT_DISTRIBUTION_ACCOUNT = 0;

    //private PreferencesWrapper prefWrapper;
    private PreferencesProviderWrapper prefProviderWrapper;

    private boolean hasTriedOnceActivateAcc = false;
    // private ImageButton pickupContact;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private boolean mDualPane;
    private Thread asyncSanityChecker;
    private Tab warningTab;
    private ObjectAnimator warningTabfadeAnim;
    
    /**
     * Listener interface for Fragments accommodated in {@link ViewPager}
     * enabling them to know when it becomes visible or invisible inside the
     * ViewPager.
     */
    public interface ViewPagerVisibilityListener {
        void onVisibilityChanged(boolean visible);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //prefWrapper = new PreferencesWrapper(this);
        prefProviderWrapper = new PreferencesProviderWrapper(this);
        
        //change by chw 20150914 加入对 是否进入设置 的权限
        prefProviderWrapper.setPreferenceBooleanValue("isEnterSetting", false);
        //end change by chw 20150914 加入对 是否进入设置 的权限
        
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sip_home);

        final ActionBar ab = getSupportActionBar();
        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        // showAbTitle = Compatibility.hasPermanentMenuKey

        
        //拨号Tab
        Tab dialerTab = ab.newTab()
                 .setContentDescription(R.string.dial_tab_name_text)
                .setIcon(R.drawable.ic_ab_dialer_holo_dark);
        //拨号历史Tab(即通话记录)
        Tab callLogTab = ab.newTab()
                 .setContentDescription(R.string.calllog_tab_name_text)
                .setIcon(R.drawable.ic_ab_history_holo_dark);
        Tab favoritesTab = null;
        if(CustomDistribution.supportFavorites()) {
            favoritesTab = ab.newTab()
                    .setContentDescription(R.string.favorites_tab_name_text)
                    .setIcon(R.drawable.ic_ab_favourites_holo_dark);
        }
        Tab messagingTab = null;
        if (CustomDistribution.supportMessaging()) {
            messagingTab = ab.newTab()
                    .setContentDescription(R.string.messages_tab_name_text)
                    .setIcon(R.drawable.ic_ab_text_holo_dark);
        }
        
        warningTab = ab.newTab().setIcon(android.R.drawable.ic_dialog_alert);
        warningTabfadeAnim = ObjectAnimator.ofInt(warningTab.getIcon(), "alpha", 255, 100);
        warningTabfadeAnim.setDuration(1500);
        warningTabfadeAnim.setRepeatCount(ValueAnimator.INFINITE);
        warningTabfadeAnim.setRepeatMode(ValueAnimator.REVERSE);
        
        mDualPane = getResources().getBoolean(R.bool.use_dual_panes);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, getSupportActionBar(), mViewPager);
        mTabsAdapter.addTab(dialerTab, DialerFragment.class, TAB_ID_DIALER);
        mTabsAdapter.addTab(callLogTab, CallLogListFragment.class, TAB_ID_CALL_LOG);
        if(favoritesTab != null) {	//最爱Tab
        	//change by chw 20150914 隐藏最爱Tab
            //mTabsAdapter.addTab(favoritesTab, FavListFragment.class, TAB_ID_FAVORITES);
        }
        if (messagingTab != null) {	//信息Tab
            mTabsAdapter.addTab(messagingTab, ConversationsListFragment.class, TAB_ID_MESSAGES);
        }

        hasTriedOnceActivateAcc = false;

        if (!prefProviderWrapper.getPreferenceBooleanValue(SipConfigManager.PREVENT_SCREEN_ROTATION)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        selectTabWithAction(getIntent());
        Log.setLogLevel(prefProviderWrapper.getLogLevel());
        

        // 异步检测更新
        asyncSanityChecker = new Thread() {
            public void run() {
                asyncSanityCheck();
            };
        };
        asyncSanityChecker.start();
        
    }

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost. It relies on a
     * trick. Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show. This is not sufficient for switching
     * between pages. So instead we make the content part of the tab host 0dp
     * high (it is not shown) and the TabsAdapter supplies its own dummy view to
     * show as the tab content. It listens to changes in tabs, and takes care of
     * switch to the correct paged in the ViewPager whenever the selected tab
     * changes.
     */
    private class TabsAdapter extends FragmentPagerAdapter implements
            ViewPager.OnPageChangeListener, ActionBar.TabListener {
        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final List<String> mTabs = new ArrayList<String>();
        private final List<Integer> mTabsId = new ArrayList<Integer>();
        private boolean hasClearedDetails = false;
        

        private int mCurrentPosition = -1;
        /**
         * Used during page migration, to remember the next position
         * {@link #onPageSelected(int)} specified.
         */
        private int mNextPosition = -1;

        public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = actionBar;
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, int tabId) {
            mTabs.add(clss.getName());
            mTabsId.add(tabId);
            mActionBar.addTab(tab.setTabListener(this));
            notifyDataSetChanged();
        }
        
        public void removeTabAt(int location) {
            mTabs.remove(location);
            mTabsId.remove(location);
            mActionBar.removeTabAt(location);
            notifyDataSetChanged();
        }
        
        public Integer getIdForPosition(int position) {
            if(position >= 0 && position < mTabsId.size()) {
                return mTabsId.get(position);
            }
            return null;
        }
        
        public Integer getPositionForId(int id) {
            int fPos = mTabsId.indexOf(id);
            if(fPos >= 0) {
                return fPos;
            }
            return null;
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            return Fragment.instantiate(mContext, mTabs.get(position), new Bundle());
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            clearDetails();
            if (mViewPager.getCurrentItem() != tab.getPosition()) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
            }
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);

            if (mCurrentPosition == position) {
                Log.w(THIS_FILE, "Previous position and next position became same (" + position
                        + ")");
            }

            mNextPosition = position;
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // Nothing to do
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            // Nothing to do
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Nothing to do
        }

        /*
         * public void setCurrentPosition(int position) { mCurrentPosition =
         * position; }
         */

        @Override
        public void onPageScrollStateChanged(int state) {
            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE: {
                    if (mCurrentPosition >= 0) {
                        sendFragmentVisibilityChange(mCurrentPosition, false);
                    }
                    if (mNextPosition >= 0) {
                        sendFragmentVisibilityChange(mNextPosition, true);
                    }
                    supportInvalidateOptionsMenu();

                    mCurrentPosition = mNextPosition;
                    break;
                }
                case ViewPager.SCROLL_STATE_DRAGGING:
                    clearDetails();
                    hasClearedDetails = true;
                    break;
                case ViewPager.SCROLL_STATE_SETTLING:
                    hasClearedDetails = false;
                    break;
                default:
                    break;
            }
        }

        private void clearDetails() {
            if (mDualPane && !hasClearedDetails) {
                FragmentTransaction ft = SipHome.this.getSupportFragmentManager()
                        .beginTransaction();
                ft.replace(R.id.details, new Fragment(), null);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
    }

    private DialerFragment mDialpadFragment;
    private CallLogListFragment mCallLogFragment;
    private ConversationsListFragment mMessagesFragment;
    private FavListFragment mPhoneFavoriteFragment;
    private WarningFragment mWarningFragment;

    private Fragment getFragmentAt(int position) {
        Integer id = mTabsAdapter.getIdForPosition(position);
        if(id != null) {
            if (id == TAB_ID_DIALER) {
                return mDialpadFragment;
            } else if (id == TAB_ID_CALL_LOG) {
                return mCallLogFragment;
            } else if (id == TAB_ID_MESSAGES) {
                return mMessagesFragment;
            } else if (id == TAB_ID_FAVORITES) {
                return mPhoneFavoriteFragment;
            } else if (id == TAB_ID_WARNING) {
                return mWarningFragment;
            }
        }
        throw new IllegalStateException("Unknown fragment index: " + position);
    }

    public Fragment getCurrentFragment() {
        if (mViewPager != null) {
            return getFragmentAt(mViewPager.getCurrentItem());
        }
        return null;
    }

    private void sendFragmentVisibilityChange(int position, boolean visibility) {
        try {
            final Fragment fragment = getFragmentAt(position);
            if (fragment instanceof ViewPagerVisibilityListener) {
                ((ViewPagerVisibilityListener) fragment).onVisibilityChanged(visibility);
            }
        }catch(IllegalStateException e) {
            Log.e(THIS_FILE, "Fragment not anymore managed");
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        // This method can be called before onCreate(), at which point we cannot
        // rely on ViewPager.
        // In that case, we will setup the "current position" soon after the
        // ViewPager is ready.
        final int currentPosition = mViewPager != null ? mViewPager.getCurrentItem() : -1;
        Integer tabId = null; 
        if(mTabsAdapter != null) {
            tabId = mTabsAdapter.getIdForPosition(currentPosition);
        }
        if (fragment instanceof DialerFragment) {
            mDialpadFragment = (DialerFragment) fragment;
            if (initTabId == tabId && tabId != null && tabId == TAB_ID_DIALER) {
                mDialpadFragment.onVisibilityChanged(true);
                initTabId = null;
            }
            if(initDialerWithText != null) {
                mDialpadFragment.setTextDialing(true);
                mDialpadFragment.setTextFieldValue(initDialerWithText);
                initDialerWithText = null;
            }
        } else if (fragment instanceof CallLogListFragment) {
            mCallLogFragment = (CallLogListFragment) fragment;
            if (initTabId == tabId && tabId != null && tabId == TAB_ID_CALL_LOG) {
                mCallLogFragment.onVisibilityChanged(true);
                initTabId = null;
            }
        } else if (fragment instanceof ConversationsListFragment) {
            mMessagesFragment = (ConversationsListFragment) fragment;
            if (initTabId == tabId && tabId != null && tabId == TAB_ID_MESSAGES) {
                mMessagesFragment.onVisibilityChanged(true);
                initTabId = null;
            }
        } else if (fragment instanceof FavListFragment) {
            mPhoneFavoriteFragment = (FavListFragment) fragment;
            if (initTabId == tabId && tabId != null && tabId == TAB_ID_FAVORITES) {
                mPhoneFavoriteFragment.onVisibilityChanged(true);
                initTabId = null;
            }
        } else if (fragment instanceof WarningFragment) {
            mWarningFragment = (WarningFragment) fragment;
            synchronized (warningList) {
                mWarningFragment.setWarningList(warningList);
                mWarningFragment.setOnWarningChangedListener(this);
            }
            
        }

    }


    /**
     * 自动进行异步检测更新
     */
    private void asyncSanityCheck() {
        // if(Compatibility.isCompatible(9)) {
        // // We check now if something is wrong with the gingerbread dialer
        // integration
        // Compatibility.getDialerIntegrationState(SipHome.this);
        // }
        
        // 夜间自动检查更新软件
        if(NightlyUpdater.isNightlyBuild(this)) {
            Log.d(THIS_FILE, "正常自动更新检查 : 在夜间进行自动更新软件操作...");
            ConnectivityManager connectivityService = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo ni = connectivityService.getActiveNetworkInfo();
            // 只有在WIFI的情况下才允许自动更新操作
            if (ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI) {
                // 如果我们没有取消自动动心这个任务，这个操作将被强制执行
                NightlyUpdater nu = new NightlyUpdater(this);

                if (!nu.ignoreCheckByUser()) {
                    long lastCheck = nu.lastCheck();
                    long current = System.currentTimeMillis();
                    long oneDay = 43200000; // 12 hours  12个小时以后进行自动更新
                    if (current - oneDay > lastCheck) {
                        if (onForeground) {
                            // 开始启动自动更新线程
                            UpdaterPopupLauncher ru = nu.getUpdaterPopup(false);
                            if (ru != null && asyncSanityChecker != null) {
                                runOnUiThread(ru);
                            }
                        }
                    }
                }
            }
        }
        
        //启动WARN进行对系统检查，判断系统是否支持STUN,VPN_ICS,SCRAD,PRIVILEGED_INTENT
        //change by chw 20160131 将这个warning屏蔽掉
        //applyWarning(WarningUtils.WARNING_PRIVILEGED_INTENT, WarningUtils.shouldWarnPrivilegedIntent(this, prefProviderWrapper));
        //applyWarning(WarningUtils.WARNING_NO_STUN, WarningUtils.shouldWarnNoStun(prefProviderWrapper));
        //applyWarning(WarningUtils.WARNING_VPN_ICS, WarningUtils.shouldWarnVpnIcs(prefProviderWrapper));
        //applyWarning(WarningUtils.WARNING_SDCARD, WarningUtils.shouldWarnSDCard(this, prefProviderWrapper));
        //end change by chw 20160131 将这个warning屏蔽掉
    }

    // 服务监控
    private void startSipService() {
        Thread t = new Thread("StartSip") {
            public void run() {
                Intent serviceIntent = new Intent(SipManager.INTENT_SIP_SERVICE);
                serviceIntent.putExtra(SipManager.EXTRA_OUTGOING_ACTIVITY, new ComponentName(SipHome.this, SipHome.class));
                startService(serviceIntent);//启动了SipService  
                postStartSipService();
            };
        };
        t.start();

    }

    private void postStartSipService() {
        // If we have never set fast settings
        if (CustomDistribution.showFirstSettingScreen()) {
            if (!prefProviderWrapper.getPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP, false)) {
                Intent prefsIntent = new Intent(SipManager.ACTION_UI_PREFS_FAST);
                prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(prefsIntent);
                return;
            }
        } else {
            boolean doFirstParams = !prefProviderWrapper.getPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP, false);
            prefProviderWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP, true);
            if (doFirstParams) {
                prefProviderWrapper.resetAllDefaultValues();
            }
        }

        // If we have no account yet, open account panel,
        if (!hasTriedOnceActivateAcc) {
//        	SipProfile.ACCOUNT_URI＝ "content://com.csipsimple.db/accounts" ;
            Cursor c = getContentResolver().query(SipProfile.ACCOUNT_URI, new String[] {
                    SipProfile.FIELD_ID
            }, null, null, null);
            
            int accountCount = 0;
            if (c != null) {
                try {
                    accountCount = c.getCount();
                } catch (Exception e) {
                    Log.e(THIS_FILE, "Something went wrong while retrieving the account", e);
                } finally {
                    c.close();
                }
            }

            if (accountCount == 0) {
            	Log.e(THIS_FILE, "没有帐号，进入帐号设置界面：accountCount = "+accountCount);
            	autoCreateAccount();	//首次打开软件使用手机卡作为账号和密码进行登陆；如果没有手机卡，就是用默认账号创建账号自动登录
//                Intent accountIntent = null;
//                WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();
//                if (distribWizard != null) {
//                    accountIntent = new Intent(this, BasePrefsWizard.class);
//                    accountIntent.putExtra(SipProfile.FIELD_WIZARD, distribWizard.id);
//                } else {
//                    accountIntent = new Intent(this, AccountsEditList.class);
//                }
//
//                if (accountIntent != null) {
//                    accountIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(accountIntent);
//                    hasTriedOnceActivateAcc = true;
//                    return;
//                }
            }
            hasTriedOnceActivateAcc = true;
        }
    }
    
    /**
     * change by chw 20151021 
     * 首次打开软件使用手机卡作为账号和密码进行登陆；
     * 如果没有手机卡，就是用默认账号创建账号自动登录
     */
    private void autoCreateAccount() {
    	Log.i(THIS_FILE, "自动创建一个帐号 cp_Default"); 
    	String displayName = null ;		//账户名称
    	String number = null ;		//账户账号
    	String password = null ;	//账户密码
    	
    	displayName = getNativeDisplayName();	//获取账户名称
    	number = getNativePhoneNumber();		//获取本机号码
    	password = getNativePhoneNumber();		//获取本机号码为密码
    	
    	if(displayName == null) {
    		//获取默认账户名称
    		displayName = ConfigInfoUtil.getDefaultAccountName(this);
    	}
    	if(number == null) {
        	//获取默认账户账号
        	number = ConfigInfoUtil.getDefaultAccountNumber(this);
    	}
    	if(password == null) {
        	//获取默认账户密码
        	password = ConfigInfoUtil.getDefaultAccountPassword(this);
    	}
    	//获取默认服务器和端口号
    	String SipService = ConfigInfoUtil.getDefaultServerIpAndPort(this);
    	String proxy = ConfigInfoUtil.getDefaultServerIpAndPort(this);
    	boolean isUseTcp = true;
    	
    	cp_Default cp = new cp_Default(this,number,displayName,number,displayName,password,SipService,proxy,isUseTcp);
    	cp.SaveAccount();
	}

    /**
     * 获取本机电话号码
     */ 
    public String getNativePhoneNumber() { 
        String NativePhoneNumber=null; 
   		TelephonyManager mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        NativePhoneNumber=mTelephonyMgr.getLine1Number(); 
        return NativePhoneNumber; 
    } 
    
    /**
     * 获取本机手机服务商信息
     */ 
    public String getNativeDisplayName() { 
        String displayName = "未知运营商"; 
        TelephonyManager mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try{ 
	        String IMSI = mTelephonyMgr.getSubscriberId(); 
	        if(IMSI != null) {
		        // IMSI号前面3位460是国家，紧接着后面2位00 02是中国移动，01是中国联通，03是中国电信。 
		        if (IMSI.startsWith("46000") || IMSI.startsWith("46002")) { 
		        	displayName = "中国移动"; 
		        } else if (IMSI.startsWith("46001")) { 
		        	displayName = "中国联通"; 
		        } else if (IMSI.startsWith("46003")) { 
		        	displayName = "中国电信"; 
		        } else {
		        	displayName = null;
		        }
	        } else {
	        	displayName = null;
	        }
        }catch(Exception e){ 
        	displayName = null;
        	Log.i(THIS_FILE, "获取运营商名字失败...");
            e.printStackTrace(); 
        } 
        Log.i(THIS_FILE, "获取运营商名字 : " + displayName);
        return displayName; 
    } 
    //end change by chw 20151021 进行自动注册 


    //进行自动更新的标志，不管怎样都是true
    private boolean onForeground = false;

    /**
     * 当由SipHome界面跳转到其他界面时，进入暂停状态，此方法被执行
     */
    @Override
    protected void onPause() {
        Log.d(THIS_FILE, "On Pause SIPHOME");
        onForeground = false;
        if(asyncSanityChecker != null) {
            if(asyncSanityChecker.isAlive()) {
                asyncSanityChecker.interrupt();
                asyncSanityChecker = null;
            }
        }
        super.onPause();

    }

    /**
     * 当进入SipHome界面的时候会被执行
     */
    @Override
    protected void onResume() {
        Log.d(THIS_FILE, "On Resume SIPHOME");
        super.onResume();
        onForeground = true;

        prefProviderWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_BEEN_QUIT, false);
        
        // Set visible the currently selected account
        sendFragmentVisibilityChange(mViewPager.getCurrentItem(), true);
        
        Log.d(THIS_FILE, "WE CAN NOW start SIP service");
        startSipService();
        
        applyTheme();
    }
    
    private ArrayList<View> getVisibleLeafs(View v) {
        ArrayList<View> res = new ArrayList<View>();
        if(v.getVisibility() != View.VISIBLE) {
            return res;
        }
        if(v instanceof ViewGroup) {
            for(int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
                ArrayList<View> subLeafs = getVisibleLeafs(((ViewGroup) v).getChildAt(i));
                res.addAll(subLeafs);
            }
            return res;
        }
        res.add(v);
        return res;
    }

    private void applyTheme() {
        Theme t = Theme.getCurrentTheme(this);
        if (t != null) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                View vg = getWindow().getDecorView().findViewById(android.R.id.content);
                // Action bar container
                ViewGroup abc = (ViewGroup) ((ViewGroup) vg.getParent()).getChildAt(0);
                //
                ArrayList<View> leafs = getVisibleLeafs(abc);
                int i = 0;
                for (View leaf : leafs) {
                    if (leaf instanceof ImageView) {
                        Integer id = mTabsAdapter.getIdForPosition(i);
                        if (id != null) {
                            int tabId = id;
                            Drawable customIcon = null;
                            switch (tabId) {
                                case TAB_ID_DIALER:
                                    customIcon = t.getDrawableResource("ic_ab_dialer");
                                    break;
                                case TAB_ID_CALL_LOG:
                                    customIcon = t.getDrawableResource("ic_ab_history");
                                    break;
                                case TAB_ID_MESSAGES:
                                    customIcon = t.getDrawableResource("ic_ab_text");
                                    break;
                                case TAB_ID_FAVORITES:
                                    customIcon = t.getDrawableResource("ic_ab_favourites");
                                    break;
                                default:
                                    break;
                            }
                            if (customIcon != null) {
                                ((ImageView) leaf).setImageDrawable(customIcon);
                            }

                            t.applyBackgroundStateListSelectableDrawable((View) leaf.getParent(),
                                    "tab");
                            if (i == 0) {
                                ViewParent tabLayout = leaf.getParent().getParent();
                                if (tabLayout instanceof LinearLayout) {
                                    Drawable d = t.getDrawableResource("tab_divider");
                                    if (d != null) {
                                        UtilityWrapper.getInstance()
                                                .setLinearLayoutDividerDrawable(
                                                        (LinearLayout) tabLayout, d);
                                    }
                                    Integer dim = t.getDimension("tab_divider_padding");
                                    if (dim != null) {
                                        UtilityWrapper.getInstance().setLinearLayoutDividerPadding(
                                                (LinearLayout) tabLayout, dim);
                                    }
                                }
                            }
                            i++;
                        }
                    }
                }
                if(i > 0) {
                    t.applyBackgroundDrawable((View) leafs.get(0).getParent().getParent(), "abs_background");
                }
                
                Drawable d = t.getDrawableResource("split_background");
                if (d != null) {
                    ab.setSplitBackgroundDrawable(d);
                }
                
                t.applyBackgroundDrawable(vg, "content_background");
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        selectTabWithAction(intent);
    }

    private String initDialerWithText = null;
    Integer initTabId = null;
    private void selectTabWithAction(Intent intent) {
        if (intent != null) {
            String callAction = intent.getAction();
            if (!TextUtils.isEmpty(callAction)) {
                ActionBar ab = getSupportActionBar();
                Tab toSelectTab = null;
                Integer toSelectId = null;
                if (callAction.equalsIgnoreCase(SipManager.ACTION_SIP_DIALER)
                        || callAction.equalsIgnoreCase(Intent.ACTION_DIAL)
                        || callAction.equalsIgnoreCase(Intent.ACTION_VIEW)
                        || callAction.equalsIgnoreCase(Intent.ACTION_SENDTO) /* TODO : sendto should im if not csip? */) {
                    Integer pos = mTabsAdapter.getPositionForId(TAB_ID_DIALER);
                    if(pos != null) {
                        toSelectTab = ab.getTabAt(pos);
                        Uri data = intent.getData();
                        String nbr = UriUtils.extractNumberFromIntent(intent, this);

                        if (!TextUtils.isEmpty(nbr)) {
                            if (data != null && mDialpadFragment != null) {
                                mDialpadFragment.setTextDialing(true);
                                mDialpadFragment.setTextFieldValue(nbr);
                            } else {
                                initDialerWithText = nbr;
                            }
                        }
                        toSelectId = TAB_ID_DIALER;
                    }
                } else if (callAction.equalsIgnoreCase(SipManager.ACTION_SIP_CALLLOG)) {
                    Integer pos = mTabsAdapter.getPositionForId(TAB_ID_CALL_LOG);
                    if(pos != null) {
                        toSelectTab = ab.getTabAt(pos);
                        toSelectId = TAB_ID_CALL_LOG;
                    }
                } else if (callAction.equalsIgnoreCase(SipManager.ACTION_SIP_FAVORITES)) {
                    Integer pos = mTabsAdapter.getPositionForId(TAB_ID_FAVORITES);
                    if(pos != null) {
                        toSelectTab = ab.getTabAt(pos);
                        toSelectId = TAB_ID_FAVORITES;
                    }
                } else if (callAction.equalsIgnoreCase(SipManager.ACTION_SIP_MESSAGES)) {
                    Integer pos = mTabsAdapter.getPositionForId(TAB_ID_MESSAGES);
                    if(pos != null) {
                        toSelectTab = ab.getTabAt(pos);
                        toSelectId = TAB_ID_MESSAGES;
                    }
                }
                if (toSelectTab != null) {
                    ab.selectTab(toSelectTab);
                    initTabId = toSelectId;
                }else {
                    initTabId = 0;
                }
                
            }
        }
    }

    @Override
    protected void onDestroy() {
        disconnect(false);
        super.onDestroy();
        Log.d(THIS_FILE, "---DESTROY SIP HOME END---");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        int actionRoom = getResources().getBoolean(R.bool.menu_in_bar) ? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER;
        
        WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();
        if (distribWizard != null) {
            menu.add(Menu.NONE, DISTRIB_ACCOUNT_MENU, Menu.NONE, "My " + distribWizard.label)
                    .setIcon(distribWizard.icon)
                    .setShowAsAction(actionRoom);
        }
        if (CustomDistribution.distributionWantsOtherAccounts()) {
            int accountRoom = actionRoom;
            if(Compatibility.isCompatible(13)) {
                accountRoom |= MenuItem.SHOW_AS_ACTION_WITH_TEXT;
            }
            menu.add(Menu.NONE, ACCOUNTS_MENU, Menu.NONE,
                    (distribWizard == null) ? R.string.accounts : R.string.other_accounts)
                    .setIcon(R.drawable.ic_menu_account_list)
                    .setAlphabeticShortcut('a')
                    .setShowAsAction( accountRoom );
        }
        
        //change by chw 20150909 将设置功能进行隐藏，当用户18612567487登录时才显示，这个号码是管理员账号
        //设置
        menu.add(Menu.NONE, PARAMS_MENU, Menu.NONE, R.string.prefs)
//        		.setVisible(false)
                .setIcon(android.R.drawable.ic_menu_preferences)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        //end change by chw 20150909 将设置功能进行隐藏，当用户18612567487登录时才显示，这个号码是管理员账号
        
        //帮助
        menu.add(Menu.NONE, HELP_MENU, Menu.NONE, R.string.help)
                .setIcon(android.R.drawable.ic_menu_help)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        //退出
        menu.add(Menu.NONE, CLOSE_MENU, Menu.NONE, R.string.menu_disconnect)
                .setIcon(R.drawable.ic_lock_power_off)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
        	//账号管理按钮，也就是应用的左下角的按钮
            case ACCOUNTS_MENU:
                startActivity(new Intent(this, AccountsEditList.class));
                return true;
            //设置
            case PARAMS_MENU:
            	Log.d(THIS_FILE, "设置...");
            	//change by chw 检测该账户是否有进入设置功能的权限
            	if(prefProviderWrapper.getPreferenceBooleanValue("isEnterSetting")) {		//进入设置界面
            		startActivityForResult(new Intent(SipManager.ACTION_UI_PREFS_GLOBAL), CHANGE_PREFS);
            	} else {		//提示不能进入
            		Toast.makeText(this, getString(R.string.enter_setting_hint), Toast.LENGTH_SHORT).show();
            	}
                //end change by chw 检测该账户是否有进入设置功能的权限
                return true;
            case CLOSE_MENU:
                Log.d(THIS_FILE, "CLOSE");
                boolean currentlyActiveForIncoming = prefProviderWrapper.isValidConnectionForIncoming();
                boolean futureActiveForIncoming = (prefProviderWrapper.getAllIncomingNetworks().size() > 0);
                if (currentlyActiveForIncoming || futureActiveForIncoming) {
                    // Alert user that we will disable for all incoming calls as
                    // he want to quit
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.warning)
                            .setMessage(
                                    getString(currentlyActiveForIncoming ? R.string.disconnect_and_incoming_explaination
                                            : R.string.disconnect_and_future_incoming_explaination))
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    prefProviderWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_BEEN_QUIT, true);
                                    disconnect(true);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } else {
                    disconnect(true);
                }
                return true;
            //帮助
            case HELP_MENU:
                // Create the fragment and show it as a dialog.
                DialogFragment newFragment = Help.newInstance();
                newFragment.show(getSupportFragmentManager(), "dialog");
                return true;
            case DISTRIB_ACCOUNT_MENU:
                WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();

                Cursor c = getContentResolver().query(SipProfile.ACCOUNT_URI, new String[] {
                        SipProfile.FIELD_ID
                }, SipProfile.FIELD_WIZARD + "=?", new String[] {
                        distribWizard.id
                }, null);

                Intent it = new Intent(this, BasePrefsWizard.class);
                it.putExtra(SipProfile.FIELD_WIZARD, distribWizard.id);
                Long accountId = null;
                if (c != null && c.getCount() > 0) {
                    try {
                        c.moveToFirst();
                        accountId = c.getLong(c.getColumnIndex(SipProfile.FIELD_ID));
                    } catch (Exception e) {
                        Log.e(THIS_FILE, "Error while getting wizard", e);
                    } finally {
                        c.close();
                    }
                }
                if (accountId != null) {
                    it.putExtra(SipProfile.FIELD_ID, accountId);
                }
                startActivityForResult(it, REQUEST_EDIT_DISTRIBUTION_ACCOUNT);

                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private final static int CHANGE_PREFS = 1;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CHANGE_PREFS) {
            sendBroadcast(new Intent(SipManager.ACTION_SIP_REQUEST_RESTART));
            BackupWrapper.getInstance(this).dataChanged();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void disconnect(boolean quit) {
        Log.d(THIS_FILE, "True disconnection...");
        Intent intent = new Intent(SipManager.ACTION_OUTGOING_UNREGISTER);
        intent.putExtra(SipManager.EXTRA_OUTGOING_ACTIVITY, new ComponentName(this, SipHome.class));
        sendBroadcast(intent);
        if(quit) {
            finish();
        }
    }
    
    
    
    
    // Warning view
    
    private List<String> warningList = new ArrayList<String>();
    private void applyWarning(String warnCode, boolean active) {
        synchronized (warningList) {
            if(active) {
                warningList.add(warnCode);
            } else {
                warningList.remove(warnCode);
            }
        }
        runOnUiThread(refreshWarningTabRunnable);
    }
    
    Runnable refreshWarningTabRunnable = new Runnable() {
        @Override
        public void run() {
            refreshWarningTabDisplay();
        }
    };
    
    private void refreshWarningTabDisplay() {
        List<String> warnList = new ArrayList<String>();
        synchronized (warningList) {
            warnList.addAll(warningList);
        }
        if(mWarningFragment != null) {
            mWarningFragment.setWarningList(warnList);
            mWarningFragment.setOnWarningChangedListener(this);
        }
        if(warnList.size() > 0) {
            // Show warning tab if any to display
            if(mTabsAdapter.getPositionForId(TAB_ID_WARNING) == null) {
                // And not yet displayed
                Log.w(THIS_FILE, "Reason to warn " + warnList);
                
                mTabsAdapter.addTab(warningTab, WarningFragment.class, TAB_ID_WARNING);
                warningTabfadeAnim.start();
            }
        }else {
            // Hide warning tab since nothing to warn about
            ActionBar ab = getSupportActionBar();
            int selPos = -1;
            if(ab != null) {
                selPos = ab.getSelectedTab().getPosition();
            }
            Integer pos = mTabsAdapter.getPositionForId(TAB_ID_WARNING);
            if(pos != null) {
                mTabsAdapter.removeTabAt(pos);
                if(selPos == pos && ab != null) {
                    ab.selectTab(ab.getTabAt(0));
                }
            }
            if(warningTabfadeAnim.isStarted()) {
                warningTabfadeAnim.end();
            }
        }
    }

    @Override
    public void onWarningRemoved(String warnKey) {
        applyWarning(warnKey, false);
    }
}
