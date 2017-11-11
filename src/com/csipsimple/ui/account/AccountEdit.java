package com.csipsimple.ui.account;

import com.csipsimple.wizards.BasePrefsWizard;

public class AccountEdit extends BasePrefsWizard /*implements OnQuitListener */ {
	//private AccountEditFragment detailFragment;

/*
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // If the screen is now in landscape mode, we can show the
            // dialog in-line with the list so we don't need this activity.
            finish();
            return;
        }

        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            detailFragment = new AccountEditFragment();
            detailFragment.setArguments(getIntent().getExtras());
            detailFragment.setOnQuitListener(this);
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, detailFragment).commit();
        }
	}
	
	@Override
	protected void onStart() {
		super.onStart();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    if(item.getItemId() == Compatibility.getHomeMenuId()) {
	         // app icon in Action Bar clicked; go home
	         Intent intent = new Intent(this, AccountsEditList.class);
	         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	         startActivity(intent);
	         return true;
	    }

        return super.onOptionsItemSelected(item);
	}
	*/

	/*
	@Override
	public void onQuit() {
		finish();
	}

	@Override
	public void onShowProfile(long profileId) {
		
	}
	*/
	
}
