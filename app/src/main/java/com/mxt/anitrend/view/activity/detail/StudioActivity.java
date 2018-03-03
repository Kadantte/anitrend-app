package com.mxt.anitrend.view.activity.detail;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.mxt.anitrend.R;
import com.mxt.anitrend.base.custom.activity.ActivityBase;
import com.mxt.anitrend.base.custom.view.widget.FavouriteToolbarWidget;
import com.mxt.anitrend.model.entity.anilist.Studio;
import com.mxt.anitrend.presenter.base.BasePresenter;
import com.mxt.anitrend.util.CompatUtil;
import com.mxt.anitrend.util.FilterProvider;
import com.mxt.anitrend.util.KeyUtils;
import com.mxt.anitrend.util.TapTargetUtil;
import com.mxt.anitrend.view.fragment.search.SeriesSearchFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

/**
 * Created by max on 2017/12/14.
 */

public class StudioActivity extends ActivityBase<Studio, BasePresenter> {

    protected @BindView(R.id.toolbar)
    Toolbar toolbar;

    private Studio model;
    private FavouriteToolbarWidget favouriteWidget;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frame_generic);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        setViewModel(true);
        setPresenter(new BasePresenter(this));
        if(getIntent().hasExtra(KeyUtils.arg_id))
            id = getIntent().getLongExtra(KeyUtils.arg_id, 0);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        onActivityReady();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean isAuth = getPresenter().getApplicationPref().isAuthenticated();
        getMenuInflater().inflate(R.menu.custom_menu, menu);
        menu.findItem(R.id.action_favourite).setVisible(isAuth);
        if(isAuth) {
            MenuItem favouriteMenuItem = menu.findItem(R.id.action_favourite);
            favouriteWidget = (FavouriteToolbarWidget) favouriteMenuItem.getActionView();
            if(model != null)
                favouriteWidget.setModel(model);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(model == null)
            makeRequest();
        else
            updateUI();
    }

    /**
     * Make decisions, check for permissions or fire background threads from this method
     * N.B. Must be called after onPostCreate
     */
    @Override
    protected void onActivityReady() {
        mFragment = SeriesSearchFragment.newInstance(getIntent().getExtras(), KeyUtils.ANIME);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, mFragment, mFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    protected void updateUI() {
        mActionBar.setTitle(model.getStudio_name());
        getPresenter().notifyAllListeners(model, false);
        if(favouriteWidget != null)
            favouriteWidget.setModel(model);
    }

    @Override
    protected void makeRequest() {
        Bundle params = getViewModel().getParams();
        params.putLong(KeyUtils.arg_id, id);
        getViewModel().requestData(KeyUtils.STUDIO_INFO_REQ, getApplicationContext());
    }

    /**
     * Called when the model state is changed.
     *
     * @param model The new data
     */
    @Override
    public void onChanged(@Nullable Studio model) {
        super.onChanged(model);
        if(model != null) {
            if(getPresenter().getApplicationPref().isAuthenticated() && getPresenter().getDatabase().getCurrentUser() != null)
                this.model = FilterProvider.getStudioFilter(model, getPresenter().getApplicationPref(), getPresenter().getDatabase().getCurrentUser().getTitle_language());
            else
                this.model = FilterProvider.getStudioFilter(model);
            updateUI();
        }
    }
}