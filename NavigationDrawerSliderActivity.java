/**
 * 
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;


/**
 * @author Hemant
 *
 */
public class NavigationDrawerSliderActivity extends FragmentActivity implements OnClickListener, INavigationDrawer, TimeCounterCallback, MealBreakCallback, WakeLockListener, IExceptionCaughtHandler {
	
	private static final String TAG = NavigationDrawerSliderActivity.class.getName();
	
	protected NavigationDrawerPresenter mPresenter;
	
	private NavigationDrawerSliderView mSliderView;
	private View leftSideListView;
	private View rightSideAppScreenView;
	
	private TextView mTitleTextView;
	private TextView mSubtitleTextView;
	protected ImageView mHeaderImageView;
	private TextView mTimeTrackerTextView;
	
	private TimeCounter mealBreakTimeCounter;
	private TimeCounter headerTimeCounter;
	
	private View button;
	boolean isposr;
	boolean issbt;
	boolean isDropEnabled = false;
	private FunctionMenuAdapter menuAdapter;
	private boolean isToggle = true;
	private boolean isMenuVisible = false;
	protected Typeface txtHeaderTypeface,txtHeaderTypefaceBold,txtHeaderTypefaceRegular;
	private RelativeLayout mHeaderRelativeLayout;
	protected KeyboardView mKeyboard;
	private ImageButton btnSlide;
	/**
	 * True if slidermenu out, false otherwise.
	 */
	private boolean menuOut = false;
	private ArrayList<String> menuItems;
	
	private int mAttachedViewID;
	
	
	/**
	 * Horizontal scrollview holding the slidermenu and the acivity's layout.
	 */
	private  NavigationDrawerSliderView scrollView;
	/**
	 * Slidermenu instance. 
	 */
    private View menu;
	
    private ClickListenerForScrolling mSliderClickListener;

    /**
     * Indicates if Activity detsroyed or not.
     */
	private boolean mIsDestroyed;
	
    private PowerManager mPowerManager;
	private WakeLock mWakeLock;

	/**
	 * Layout containing home button (icon) and slider menu icon. Set click listener to increase responsiveness.
	 */
	private LinearLayout mHomeBtnLayout;

	private LinearLayout mTimerLayout;
	
	/**
	 * Item click listener for the slider menu list.
	 */
	private NavigationDrawerPresenter.ListItemClickListenerForScrolling mClickListener;
	
	protected boolean isScanningDialogActivated = false;
	
	/**
	 * Indicates whether timers should be destroyed in onPause.
	 */
	private boolean mNoDetsroyTimerOnPause;
	
	private boolean mContinuousScanReqd = true;

	private boolean mIsScannerEnabled = true;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(savedInstanceState);
		
		loadEnvironment();
		loadPresenter();
		showBaseScreen();
		registerUncaughtExceptionHandler();
        
		mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, “MyWakeLock”);
		AppUtility.getAppUtilityInstance().acquireWakeLock(mWakeLock);
	}

	private void registerUncaughtExceptionHandler() {
		ExceptionHandler exceptionHandler = new ExceptionHandler();
		exceptionHandler.init(this, Thread.currentThread(), this);
	}
	
	public void setStoreVisitIconDisable() {
		AppConfig.getAppConfigInstance(this).setStoreVisitIcosDisable();
	}
	public void setStoreVisitIconEnable() {
		AppConfig.getAppConfigInstance(this).setStoreVisitIconEnable();
	}
	public void setShipmentArrivalsDisable() {
		AppConfig.getAppConfigInstance(this).setShipmentArrivalsDisable();
	}
	public void setShipmentArrivalsEnable() {
		AppConfig.getAppConfigInstance(this).setShipmentArrivalsEnable();
	}
	public void setTimeExpensesDisable() {
		AppConfig.getAppConfigInstance(this).setTimeExpensesDisable();
	}
	public void setTimeExpensesEnable() {
		AppConfig.getAppConfigInstance(this).setTimeExpensesEnable();
	}
	public void setTransmissionLogsDisable() {
		AppConfig.getAppConfigInstance(this).setTransmissionLogsDisable();
	}
	public void setTransmissionLogsEnable() {
		AppConfig.getAppConfigInstance(this).setTransmissionLogsEnable();
	}
	public void setAccountListDisable() {
		AppConfig.getAppConfigInstance(this).setAccountListDisable();
	}
	public void setAccountListEnable() {
		AppConfig.getAppConfigInstance(this).setAccountListEnable();
	}
	public void setHelpOptionDisable() {
		AppConfig.getAppConfigInstance(this).setHelpOptionDisable();
	}
	public void setHelpOptionEnable() {
		AppConfig.getAppConfigInstance(this).setHelpOptionEnable();
	}
	public void setSettingOptionDisable() {
		AppConfig.getAppConfigInstance(this).setSettingsOptionDisable();
	}
	public void setSettingOptionEnable() {
		AppConfig.getAppConfigInstance(this).setSettingsOptionEnable();
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		mealBreakTimeCounter = TimerUtility.getTimerUtilityInstance().getTimerObjectFromContainer(IAppConstants.TimerManagerConstants.TIMER_UTILITY_MEALBREAK_TIME);
		if(mealBreakTimeCounter != null) {
			if(mealBreakTimeCounter.isMealBreakRegistered()) {
				mealBreakTimeCounter.unregisterMealBreakCounterCallback();
			}
		}
		
		if(!isNoDetsroyTimerOnPause() && !isScanningDialogActivated){
			AppLogs.d("!!Scanner", "flags: " + isNoDetsroyTimerOnPause() + ", " + isScanningDialogActivated);
			DataWedgePresenter.getDataWedgePresenterInstance(this).resetTimers(this);
		}
	}
	
	@Override
	protected void onStop() {
		//toggleSliderMenu(true);
		if(menu != null) {
			menuOut = mPresenter.toggleSliderMenu(true, menu, menuOut);
		}
		super.onStop();
	}

	@Override
	protected void onResume() {
		AppUtility.getAppUtilityInstance().checkAndInitializeDatabase(this);
		super.onResume();
		setListAdapter();
		mealBreakTimeCounter = TimerUtility.getTimerUtilityInstance().getTimerObjectFromContainer(IAppConstants.TimerManagerConstants.TIMER_UTILITY_MEALBREAK_TIME);
		if(mealBreakTimeCounter != null) {
			if(mealBreakTimeCounter.isMealBreakRegistered()) {
				mealBreakTimeCounter.unregisterMealBreakCounterCallback();
			}
			mealBreakTimeCounter.registerMealBreakCounterCallback(this);
			
			headerTimeCounter = TimerUtility.getTimerUtilityInstance().getTimerObjectFromContainer(IAppConstants.TimerManagerConstants.TIMER_UTILITY_HEADER_TIMER_OBJECT);
			
			if (headerTimeCounter == null) {
				headerTimeCounter = new TimeCounter(this, IAppConstants.TimerManagerConstants.TIMER_UTILITY_HEADER_TIMER_OBJECT, false);
				TimerUtility.getTimerUtilityInstance().addTimerObjectToContainer(IAppConstants.TimerManagerConstants.TIMER_UTILITY_HEADER_TIMER_OBJECT, headerTimeCounter);
			}
			headerTimeCounter.registerTimeCounterCallback(this);
		}
		
	/*	if(AppConfig.getAppConfigInstance(this).isAppSyncing()) {
			disableSlider();
		} else {
			enableSlider();
		}*/
	}
	
	public void startRMServiceTimer() {
		mPresenter.startRMServiceTimer();
	}

	public void startMAAServiceTimer() {
		mPresenter.startMAAServiceTimer();
	}
	
	
	private void loadEnvironment() {
		if( Build.VERSION.SDK_INT >= 9){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

            StrictMode.setThreadPolicy(policy); 
        }
		txtHeaderTypeface = Typeface.createFromAsset(getAssets(),
				IAppConstants.CommonConstants.FONT_PATH_PREFIX
						+ IAppConstants.ROBOTO_MEDIUM_FONTS);
		txtHeaderTypefaceBold = Typeface.createFromAsset(getAssets(),
				IAppConstants.CommonConstants.FONT_PATH_PREFIX
				+ IAppConstants.ROBOTO_MEDIUM_FONTS);
		txtHeaderTypefaceRegular = Typeface.createFromAsset(getAssets(),
				IAppConstants.CommonConstants.FONT_PATH_PREFIX
				+ IAppConstants.ROBOTO_MEDIUM_FONTS);

		if(getActionBar() != null)
			getActionBar().hide();
	}
	
	
	private void loadPresenter() {
		
	}
	
	
	/**
	 * display base activity UI.
	 */
	private void showBaseScreen() {
		LayoutInflater inflater = LayoutInflater.from(this);
		mSliderView = (NavigationDrawerSliderView) inflater.inflate(R.layout.screen_scroll_with_list_menu, null);
        setContentView(mSliderView);
        leftSideListView = inflater.inflate(R.layout.left_pane_menu_list, null);

        setListAdapter();
	}
	
	
	protected void setListAdapter() {
		// TODO Auto-generated method stub

		
		ListView listView = (ListView) leftSideListView.findViewById(R.id.list);
		listView.setAdapter(new NavigationDrawerListAdapter(this,  Typeface.createFromAsset(getAssets(), IAppConstants.CommonConstants.FONT_PATH_PREFIX + IAppConstants.ROBOTO_MEDIUM_FONTS), mPresenter.getScreenIcons()));
		mClickListener =  mPresenter.getListItemClickListenerInstance(mSliderView, leftSideListView, menuOut);
		listView.setOnItemClickListener(mClickListener);
	}

	/**
	 * This method makes header menu bar visible to user.
	 */
	public void displayHeaderMenuBar() {
		button = mHeaderRelativeLayout.findViewById(R.id.bt_header_dropdown_menu_icon);
		mHeaderRelativeLayout = (RelativeLayout) mHeaderRelativeLayout.findViewById(R.id.ll_header_clickable_menubar);
		mHeaderRelativeLayout.setOnClickListener(this);
		mPresenter.displayHeaderMenuBar(button);
	}
	
	public void setDrawerContentView(int layout_id) {
		
		LayoutInflater inflater = LayoutInflater.from(this);
		rightSideAppScreenView = inflater.inflate(layout_id, null);
		mAttachedViewID = rightSideAppScreenView.getId();
		
		mHeaderRelativeLayout = (RelativeLayout)rightSideAppScreenView.findViewById(R.id.navigation_header);
		
		mTitleTextView = (TextView)mHeaderRelativeLayout.findViewById(R.id.tv_header_title);
		mSubtitleTextView = (TextView)mHeaderRelativeLayout.findViewById(R.id.tv_header_subtitle);
		mHeaderImageView = (ImageView)mHeaderRelativeLayout.findViewById(R.id.iv_header_icon);
		
		mTimeTrackerTextView = (TextView) mHeaderRelativeLayout.findViewById(R.id.menu_header_time_tracker);
		mTimerLayout = (LinearLayout)mHeaderRelativeLayout.findViewById(R.id.timer_layout);
		
		mSubtitleTextView.setOnClickListener(this);
		mTimeTrackerTextView.setOnClickListener(this);
		mTimerLayout.setOnClickListener(this);

		ViewGroup tabBar = (ViewGroup) rightSideAppScreenView.findViewById(mAttachedViewID);

		btnSlide = (ImageButton) tabBar.findViewById(R.id.bt_slider);
		btnSlide.setVisibility(View.VISIBLE);

		mSliderClickListener = new ClickListenerForScrolling(mSliderView, leftSideListView, menuOut);
		btnSlide.setOnClickListener(mSliderClickListener);
		
		mHomeBtnLayout = (LinearLayout)mHeaderRelativeLayout.findViewById(R.id.home_layout);
		if(mHomeBtnLayout!= null)
		{
			mHomeBtnLayout.setOnClickListener(mSliderClickListener);
		}

		View[] children = new View[] { leftSideListView, rightSideAppScreenView };

		int scrollToViewIdx = 1;

		mSliderView.initViews(children, scrollToViewIdx, new SizeCallbackForMenu(btnSlide));
		
		setSubTitle(AppConfig.getAppConfigInstance(this).getUserName());
	     setTitle(getResources().getString(R.string.app_title));
		
		displayHeaderMenuBar();
		//If learn mode is active, the titlebar color must be changed.
		if (AppConfig.getAppConfigInstance(this).getDeviceSynched())
			mPresenter.checkLearnMode(this, rightSideAppScreenView,
					mSubtitleTextView);
	}
	
	
	
	public void disableSlidingBarButton() {
		btnSlide.setClickable(false);
		mHeaderImageView.setClickable(false);
		if(mHomeBtnLayout != null){
			mHomeBtnLayout.setClickable(false);
		}
	}
	
	public boolean getSlidingBarEnable() {
		return btnSlide.isClickable();
	}
	
	
	public void enableSlidingBarButton() {
		btnSlide.setClickable(true);
		if(mHomeBtnLayout != null){
			mHomeBtnLayout.setClickable(true);
		}
	}
	
	
	/*
	 * disable the slider button
	 */
	public void disableSlider()
	{
		btnSlide.setVisibility(View.GONE);
		mHeaderImageView.setClickable(false);
		if(mHomeBtnLayout != null){
			mHomeBtnLayout.setClickable(false);
		}
	}
	
	
	/*
	 * remove subtitle
	 */
	public void disableSubtitle() {
		
		mSubtitleTextView.setVisibility(View.GONE);
		
	}


	public void enableSubtitle() {
		setSubTitle(AppConfig.getAppConfigInstance(this).getUserName());
		mSubtitleTextView.setVisibility(View.VISIBLE);
		
	}

	public void enableSlider() {
		if(mHomeBtnLayout != null){
			mHomeBtnLayout.setClickable(true);
		}
		btnSlide.setVisibility(View.VISIBLE);
		
	}
	
	/**
	 * set application header icon
	 * @param drawable header icon
	 */
	public void setLogo(Drawable drawable) {
		mHeaderImageView.setBackground(drawable);
		mHeaderImageView.setOnClickListener(mSliderClickListener);//To increase slider menu clickable area, added same clicklistener to logo. 
	}
	
	/**
	 * set application subtitle
	 * @param subTitle
	 */
	public void setSubTitle(String subTitle) {
		mSubtitleTextView.setText(subTitle);
		mSubtitleTextView.setTypeface(txtHeaderTypeface);
	}
	
	/**
	 * Set application header title
	 * @param title
	 */
	public void setTitle(String title) {
		mTitleTextView.setText(title);
		mTitleTextView.setTypeface(txtHeaderTypeface);
	}
	
	
	/**
	 * This method makes the login time visible to user.
	 */
	public void displayLoginTimer() {
		mPresenter.displayLoginTimer(mTimeTrackerTextView);
		if(headerTimeCounter == null) {
			headerTimeCounter = TimerUtility.getTimerUtilityInstance().getTimerObjectFromContainer(IAppConstants.TimerManagerConstants.TIMER_UTILITY_HEADER_TIMER_OBJECT);
		}
		headerTimeCounter.registerTimeCounterCallback(this);
	}
	
	/**
	 * This method hides login timer from user.
	 */
	public void hideLoginTimer() {
		mTimeTrackerTextView.setVisibility(View.GONE);
	}
	
	public void showMenuListOnHeader() {
		button.setVisibility(View.VISIBLE);
		isDropEnabled = true;
	}

	public void showMenuListOnBottom() {
		isMenuVisible = true;
	}

	public void hideMenuListOnHeader() {
		button.setVisibility(View.GONE);
	}

	public void addItemToMenuList(String item) {
		mPresenter.addItemToMenuList(item, menuItems);
	}
	
	public void addMultipleItemsToMenuList(ArrayList<String> items) {
		if (menuItems == null) {
			menuItems = new ArrayList<String>();
		}
		mPresenter.addMultipleItemsToMenuList(items, menuItems);
	}
	
	class SizeCallbackForMenu implements SizeCallback {
        private int btnWidth;
        private View btnSlide;

        public SizeCallbackForMenu(View btnSlide) {
            super();
            this.btnSlide = btnSlide;
        }

        @Override
        public void onGlobalLayout() {
            btnWidth = btnSlide.getMeasuredWidth()+40;
//            System.out.println("btnWidth=" + btnWidth);
        }

        @Override
        public void getViewSize(int idx, int w, int h, int[] dims) {
            dims[0] = w;
            dims[1] = h;
            final int menuIdx = 0;
            if (idx == menuIdx) {
                dims[0] = w - btnWidth;
            }
        }
    }
	
	
    class ClickListenerForScrolling implements OnClickListener {
        /**
         * Menu must NOT be out/shown to start with.
         */
        public ClickListenerForScrolling(NavigationDrawerSliderView scrollView, View menu, boolean menuOut) {
            super();
            NavigationDrawerSliderActivity.this.scrollView = scrollView;
            NavigationDrawerSliderActivity.this.menu = menu;
            NavigationDrawerSliderActivity.this.menuOut = menuOut;
        }

        @Override
        public void onClick(View v) {
        	menuOut = mPresenter.toggleSliderMenu(false, menu, menuOut);
        	mClickListener.setMenuState(menuOut);
        	//menuOut = !menuOut;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//    	if (isMenuVisible) {
//			MenuInflater menuInflater = getMenuInflater();
//			menuInflater.inflate(R.menu.lookup_options_menu, menu);
//			return true;
//		}
		return super.onCreateOptionsMenu(menu);
    }
    
	//scroll the page and open the webview
//    private void scrollView(NavigationDrawerSliderView scrollView, View menu) {
//         int menuWidth = menu.getMeasuredWidth();
//         menu.setVisibility(View.VISIBLE);
//
//         if (!menuOut) {
//             // Scroll to 0 to reveal menu
//        	 AppLogs.d("===slide==","Scroll to right");
//             int left = 0;
//             scrollView.smoothScrollTo(left, 0);
//         } else {
//             // Scroll to menuWidth so menu isn't on screen.
//        	 AppLogs.d("===slide==","Scroll to left");
//             int left = menuWidth;
//             scrollView.smoothScrollTo(left, 0);
//            
//         }
//         menuOut = false;
//    }


    @Override
	public void updateClockTime(String newtime) {
		if ((mTimeTrackerTextView != null)
				&& (mTimeTrackerTextView.getVisibility() == View.VISIBLE)) {
			
			TimeCounter currentServiceRunningTime = AppUtility.getAppUtilityInstance().getCurrentRunningServiceTimeCounter();
			if(currentServiceRunningTime != null && AppUtility.getAppUtilityInstance().isApplicationStartAfterCrash()) {
				long currentSystemTime = currentServiceRunningTime.getCurrentSystemTime();
				long currentSystemTimeMins = currentSystemTime/(60*1000);
				/*long totalTimeInMins =  AppUtility.getAppUtilityInstance().getAccumulatedPauseTime(currentServiceRunningTime.getStartTimeStamp(), AppUtility.getAppUtilityInstance().getCurrentTimeStampWithoutSecs());
				currentSystemTimeMins -= totalTimeInMins;*/
				
				Date mCurrentDate = new Date(currentSystemTimeMins*60*1000);
				final SimpleDateFormat formatterTime = new SimpleDateFormat(IAppConstants.HOUR_MINS_FORMAT);
				formatterTime.setTimeZone(TimeZone.getTimeZone(IAppConstants.UTC_TIMEZONE));
				newtime = formatterTime.format(mCurrentDate);
			}
			
			mTimeTrackerTextView.setText(newtime);
		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.timer_layout://Layout made clickable to increase click area.
		case R.id.menu_header_time_tracker:
			if(AppUtility.getAppUtilityInstance().isConnectionEstablished()) {
				AppUtility.getAppUtilityInstance().printTimerLogs();
				Intent timerDrawerIntent = new Intent(this, TimeDrawerActivity.class);
				timerDrawerIntent.putExtra(IAppConstants.IS_APP_ON_PAUSE_STATE, false);
				timerDrawerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				
				Object setResult =  v.getTag(R.id.menu_header_time_tracker);
				
				if(setResult != null && (setResult instanceof Integer)) {
					int requestCode = Integer.parseInt(setResult.toString());
					if ((requestCode&0xffff0000) == 0) {
						timerDrawerIntent.putExtra(IAppConstants.StoreVisitSummary.EXTRA_NOTIFY_REFRESH, requestCode );
						startActivityForResult(timerDrawerIntent, requestCode);
					} else {
						startActivity(timerDrawerIntent);
					}
				} else {
					startActivity(timerDrawerIntent);
				}
			}else {
				AppLogs.d("NavigationDrawerSliderActivity", " connection not established!!");
			}
			break;

		case R.id.tv_header_subtitle: //Set click listener to subtitle to increase clickable area.
		case R.id.ll_header_clickable_menubar:
			if (button.getVisibility() == View.VISIBLE) {
				if (isToggle) {
					final ListPopupWindow mPopupMenuWindow = new ListPopupWindow(
							this);
					mPopupMenuWindow.setAnchorView(v);
					mPopupMenuWindow.setHeight(406);
					mPopupMenuWindow.setWidth(325);
					mPopupMenuWindow.getListView();
					mPopupMenuWindow.setBackgroundDrawable(getResources()
							.getDrawable(R.drawable.bg_popover_function_nine));
					if (menuItems != null) {
						menuAdapter = new FunctionMenuAdapter(this, menuItems,
								txtHeaderTypeface);
					}
					mPopupMenuWindow.setAdapter(menuAdapter);
					mPopupMenuWindow.setVerticalOffset(15);
					mPopupMenuWindow.setHorizontalOffset(-19);
					mPopupMenuWindow
							.setOnItemClickListener(new OnItemClickListener() {

								@Override
								public void onItemClick(AdapterView<?> parent,
										View view, int position, long arg3) {
									//TODO: Call through Presenter. Currently, the "this" instance is of HomeScreenActivity, hence making local method call.
									/*mPresenter.*/OnClickSelectedItem(menuItems
											.get(position).toString());
									mPopupMenuWindow.dismiss();
									isToggle = !isToggle;
								}
							});
					mPopupMenuWindow.show();
				}
				isToggle = !isToggle;
			}
			break;
		}
	}


	
	/**
	 * invoke Connection settings screen
	 */
	@Override
	public void showConnectionDetailsScreen() {
		
		
	}
	
	
	public void startServiceTimer(String key) {
		mPresenter.startServiceTimer(key);
	}
	
	
	
	public void startGlobalTimer() {
		mPresenter.startGlobalTimer();
	}
	
	public void startMealBreakTimer() {
		mPresenter.startMealBreakTimer();
		if(mealBreakTimeCounter == null) {
			mealBreakTimeCounter = TimerUtility.getTimerUtilityInstance().getTimerObjectFromContainer(IAppConstants.TimerManagerConstants.TIMER_UTILITY_MEALBREAK_TIME);
		}
		mealBreakTimeCounter.registerMealBreakCounterCallback(this);
	}
	
	/**
	 * Pause timer for specific service
	 * @param key specific service name (ex : global, rmservice etc).
	 */
	public void pauseSystemTimer(String key) {
		mPresenter.pauseSystemTimer(key);
	}

	/**
	 * resume application time for specific service.
	 * @param key
	 */
	public void resumeSystemTimer(String key) {
		mPresenter.resumeSystemTimer(key);
	}

	/**
	 * Reset system time for specific service
	 * @param key
	 */
	public void resetSystemTimer(String key) {
		mPresenter.resetSystemTimer(key);
	}

	/**
	 * Stops system time for specific service
	 * @param key
	 */
	public void stopSystemTimer(String key) {
		mPresenter.stopSystemTimer(key);
	}
	
	/**
	 * Stops system time for specific service
	 * @param key
	 */
	public void stopMealBreakTimer(String key) {
		mPresenter.stopMealBreakTimer(key);
	}
	
	@Override
	public void onBackPressed() {
		if(menuOut){ //If menu out, close.
			menuOut = mPresenter.toggleSliderMenu(true, menu, menuOut);
		} else if(isKeyBoardVisible()){
			hideKeyBoard();
		} else {
			super.onBackPressed();
		}
	}


	@Override
	public void updateMealBreakTime(String newtime) {
		// TODO Auto-generated method stub
		
	}
	
	
	/**
	 * Decide the next action on selection of any menu item.
	 * 
	 * @param menuItem
	 *            selected menu item.
	 */
	public void OnClickSelectedItem(String menuItem) {
		if (menuItem.toString().equalsIgnoreCase(IAppConstants.StoreVisitConstantsStrings.BACK_ORDERS1)) {
			mPresenter.ShowBackorderScreen();
		}else if (menuItem.toString().equalsIgnoreCase(
				IAppConstants.StoreVisitConstantsStrings.EVERYDAY_CREDIT_TALLY1)) {
			mPresenter.ShowEveryDayCreditScreen();
		}else if (menuItem.toString().equalsIgnoreCase(IAppConstants.StoreVisitConstantsStrings.INSTALLATIONS1)) {
			mPresenter.ShowInstallationsScreen();
		}else if (menuItem.toString().equalsIgnoreCase(IAppConstants.StoreVisitConstantsStrings.POSR_AUDIT1)) {
			mPresenter.ShowPOSRAuditScreen();
		}else if (menuItem.toString().equalsIgnoreCase(IAppConstants.StoreVisitConstantsStrings.SBT_DISCARD1)) {
			mPresenter.ShowSBTDiscardScreen();
		}else if (menuItem.toString()
				.equalsIgnoreCase(IAppConstants.StoreVisitConstantsStrings.SBT_TRANSFER_IN1)) {
			mPresenter.ShowTransferScreen();
		}else if (menuItem.toString().equalsIgnoreCase(IAppConstants.StoreVisitConstantsStrings.SHIPMENTS1)) {
			mPresenter.ShowShipmentScreen();
		}else if (menuItem.toString().equalsIgnoreCase(IAppConstants.StoreVisitConstantsStrings.STORE_DETAILS1)) {
			mPresenter.ShowStoreDetailsScreen();
		}else if (menuItem.toString().equalsIgnoreCase(IAppConstants.StoreVisitConstantsStrings.POSR_AUDIT_SCAN)) {
			mPresenter.ShowPOSRAuditScannerScreen();
		} else if(menuItem.toString().equalsIgnoreCase(IAppConstants.StoreVisitConstantsStrings.LOOKUP1)){
			mPresenter.ShowLookupScreen();
		} else if(menuItem.toString().equalsIgnoreCase(IAppConstants.StoreVisitConstantsStrings.STORE_SUMMARY)) {
			mPresenter.ShowStoreVisitSummaryScreen();
		} else if(menuItem.toString().equalsIgnoreCase(IAppConstants.StoreVisitConstantsStrings.SEASON_CREDIT_TALLY1)) {
			mPresenter.showSeasonalCreditScreen();
		}
	}
	
	
	public void ShowLookupScreen() {
		mPresenter.ShowLookupScreen();
	}
	
	
	public void ShowStoreVisitSummaryScreen() {
		mPresenter.ShowStoreVisitSummaryScreen();
	}
	
	public Typeface getRobotoMedium(){
		return txtHeaderTypeface;
	}
	
	/**
	 * Makes custom keyboard visible.
	 */
	protected void showKeyBoard() {
		if(mKeyboard != null){
			mKeyboard.setVisibility(View.VISIBLE);
		}
	}
	
	/**
	 * Hides the custom keykoard.
	 */
	protected void hideKeyBoard() {
		if(mKeyboard != null){
			mKeyboard.setVisibility(View.GONE);
		}
	}
	
	/**
	 * Hides system keyboard for the given EditText.
	 * @param field
	 */
	protected void hideSystemKeyBoard(EditText field){
		mPresenter.hideSystemKeyBoard(field);
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(mIsScannerEnabled) {
			switch(keyCode)
			{
			case KeyEvent.KEYCODE_SOFT_LEFT:
				//	    		Log.d(TAG, "KeyEvent");
				//	    		mPresenter.enableDataWedgeScanner(NavigationDrawerSliderActivity.this);
				DataWedgePresenter.getDataWedgePresenterInstance(this).enableDataWedgeScanner(this, isContinuousScanReqd(), this);
				return true;
			case KeyEvent.KEYCODE_BACK:
				AppLogs.d("!!Timerout", "back key pressed");
				break;
			}
		}
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(mIsScannerEnabled) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				DataWedgePresenter.getDataWedgePresenterInstance(this).resetTimers(this);
				break;

			default:
				break;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	/**
	 * Returns if keyboard visible or not.
	 * @return false if keyboard instance null.
	 */
	protected boolean isKeyBoardVisible(){
		if(mKeyboard != null){
			if(mKeyboard.getVisibility() == View.VISIBLE){
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Returns current keyboard instance. 
	 * @return keyboard instance. null if not set from the respective subclass Activities.
	 */
	public KeyboardView getKeyBoard(){
		return mKeyboard;
	}
	
	/**
	 * If menu showing or not.
	 * @return true if showing, false if hidden.
	 */
	protected boolean isMenuShowing() {
		return menuOut;
	}
	
	/**
	 * Returns true f Activity destroyed.
	 * @return
	 */
	protected boolean isDestoyed() {
		return mIsDestroyed;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mIsDestroyed = true;
		DataWedgePresenter.getDataWedgePresenterInstance(this).resetTimers(this);
	}

	public boolean isNoDetsroyTimerOnPause() {
		return mNoDetsroyTimerOnPause;
	}

	public void setNoDetsroyTimerOnPause(boolean mNoDetsroyTimerOnPause) {
		this.mNoDetsroyTimerOnPause = mNoDetsroyTimerOnPause;
	}


	protected boolean isContinuousScanReqd() {
		return mContinuousScanReqd;
	}


	protected void setContinuousScanReqd(boolean mContinuousScanReqd) {
		this.mContinuousScanReqd = mContinuousScanReqd;
	}
	
	public void releaseWakeLock() {
		final Window window = getWindow();
		if(window != null){
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					AppLogs.d("!!Scanner", "Released wakelock");
				}
			});
		}
	}
	
	public void setWakeLock(){
		Window window = getWindow();
		if(window != null){
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			AppLogs.d("!!Scanner", "wakelock set");
		}
	}

	/**
	 * Enable/disable scanning in activity.
	 * @param enable true if scanner should be enabled on soft left press.
	 */
	public void enabledScanner(boolean enable) {
		mIsScannerEnabled = enable;
	}

	@Override
	public void unCaughtException(Context context, final String message) {
		AppUtility.getAppUtilityInstance().showCrashDialog(context, message, (MyApp)getApplication());
	}
	
}
