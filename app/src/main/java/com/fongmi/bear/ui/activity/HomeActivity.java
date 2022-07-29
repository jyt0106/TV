package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.R;
import com.fongmi.bear.bean.Func;
import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.databinding.ActivityHomeBinding;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.player.Players;
import com.fongmi.bear.server.Server;
import com.fongmi.bear.ui.custom.CustomRowPresenter;
import com.fongmi.bear.ui.custom.CustomSelector;
import com.fongmi.bear.ui.presenter.FuncPresenter;
import com.fongmi.bear.ui.presenter.ProgressPresenter;
import com.fongmi.bear.ui.presenter.TitlePresenter;
import com.fongmi.bear.ui.presenter.VodPresenter;
import com.fongmi.bear.utils.Clock;
import com.fongmi.bear.utils.Notify;
import com.fongmi.bear.utils.ResUtil;

import java.util.List;

public class HomeActivity extends BaseActivity implements VodPresenter.OnClickListener {

    private ActivityHomeBinding mBinding;
    private SiteViewModel mSiteViewModel;
    private FuncPresenter mFuncPresenter;
    private ArrayObjectAdapter mAdapter;
    private boolean mConfirmExit;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HomeActivity.class));
        activity.finish();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        Clock.start(mBinding.time);
        Server.get().start();
        Players.get().init();
        setRecyclerView();
        setViewModel();
        setAdapter();
        getVideo();
    }

    @Override
    protected void initEvent() {
        mFuncPresenter.setOnClickListener(this::onFuncClick);
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                mBinding.time.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(Integer.class, new TitlePresenter());
        selector.addPresenter(String.class, new ProgressPresenter());
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), FuncPresenter.class);
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.result.observe(this, result -> {
            mAdapter.remove("progress");
            if (result == null) return;
            for (List<Vod> items : result.partition()) {
                VodPresenter presenter = new VodPresenter(result.getColumns());
                ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
                presenter.setOnClickListener(this);
                adapter.addAll(0, items);
                mAdapter.add(new ListRow(adapter));
            }
        });
    }

    private void setAdapter() {
        mAdapter.add(R.string.app_name);
        mAdapter.add(getFuncRow());
        mAdapter.add(R.string.home_recent);
        mAdapter.add(R.string.home_recommend);
    }

    private void getVideo() {
        if (mAdapter.size() > 4) mAdapter.removeItems(4, mAdapter.size() - 4);
        if (ApiConfig.get().getHome().getKey().isEmpty()) return;
        mSiteViewModel.homeContent();
        mAdapter.add("progress");
    }

    private ListRow getFuncRow() {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(mFuncPresenter = new FuncPresenter());
        adapter.add(Func.create(R.string.home_vod));
        adapter.add(Func.create(R.string.home_live));
        adapter.add(Func.create(R.string.home_search));
        adapter.add(Func.create(R.string.home_push));
        adapter.add(Func.create(R.string.home_setting));
        return new ListRow(adapter);
    }

    private void onFuncClick(Func item) {
        switch (item.getResId()) {
            case R.string.home_vod:
                VodActivity.start(this, mSiteViewModel.getResult().getValue());
                break;
            case R.string.home_setting:
                SettingActivity.start(this);
                break;
        }
    }

    @Override
    public void onItemClick(Vod item) {
        DetailActivity.start(getActivity(), item.getVodId());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        String type = data != null ? data.getStringExtra("type") : "";
        if (type.equals("thumbnail")) mAdapter.notifyArrayItemRangeChanged(4, mAdapter.size() - 4);
        else getVideo();
    }

    @Override
    public void onBackPressed() {
        if (!mConfirmExit) {
            mConfirmExit = true;
            Notify.show(R.string.app_exit);
            new Handler().postDelayed(() -> mConfirmExit = false, 1000);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        ApiConfig.get().release();
        Players.get().release();
        Clock.get().release();
        Server.get().stop();
        super.onDestroy();
    }
}