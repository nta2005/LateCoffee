package com.nta.latecoffee.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.asksira.loopingviewpager.LoopingViewPager;
import com.nta.latecoffee.R;
import com.nta.latecoffee.activity.ChatActivity;
import com.nta.latecoffee.activity.HomeActivity;
import com.nta.latecoffee.adapter.MyBestDealsAdapter;
import com.nta.latecoffee.adapter.MyPopularCategoriesAdapter;
import com.nta.latecoffee.ui.cart.CartFragment;
import com.nta.latecoffee.ui.menu.MenuFragment;
import com.nta.latecoffee.ui.view_orders.ViewOrderFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class HomeFragment extends Fragment {

    FragmentTransaction transaction;
    Unbinder unbinder;
    @BindView(R.id.recycler_popular)
    RecyclerView recycler_popular;
    @BindView(R.id.viewpager)
    LoopingViewPager viewpager;
    @BindView(R.id.refreshHome)
    SwipeRefreshLayout refreshHome;

    @BindView(R.id.menuHome)
    LinearLayout menuHome;

    LayoutAnimationController layoutAnimationController;
    private HomeViewModel homeViewModel;

    @OnClick(R.id.ln_menu)
    void onClickMenu() {
        transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, new MenuFragment());
        ((HomeActivity) getActivity()).getSupportActionBar().setTitle("Menu");
        transaction.commit();
    }

    @OnClick(R.id.ln_view_order)
    void onClickOrder() {
        transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, new ViewOrderFragment());
        ((HomeActivity) getActivity()).getSupportActionBar().setTitle("Xem đơn hàng");
        transaction.commit();
    }

    @OnClick(R.id.ln_cart)
    void onClickCart() {
        transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, new CartFragment());
        ((HomeActivity) getActivity()).getSupportActionBar().setTitle("Giỏ hàng");
        transaction.commit();
    }

    @OnClick(R.id.ln_chat)
    void onClickChat() {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        startActivity(intent);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        unbinder = ButterKnife.bind(this, root);

        initViews();
        reloadHome(); //Reload

        homeViewModel.getPopularList().observe(getViewLifecycleOwner(), popularCategoryModels -> {
            MyPopularCategoriesAdapter adapter = new MyPopularCategoriesAdapter(getContext(), popularCategoryModels);
            recycler_popular.setAdapter(adapter);
            recycler_popular.setLayoutAnimation(layoutAnimationController);
        });

        homeViewModel.getBestDealList().observe(getViewLifecycleOwner(), bestDealModels -> {
            MyBestDealsAdapter adapter = new MyBestDealsAdapter(getContext(), bestDealModels, true);
            viewpager.setAdapter(adapter);
            viewpager.setLayoutAnimation(layoutAnimationController);
        });

        return root;
    }

    private void initViews() {
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_item_from_left);

        recycler_popular.setHasFixedSize(true);
        recycler_popular.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        menuHome.setLayoutAnimation(layoutAnimationController);
    }

    private void reloadHome() {
        refreshHome.setOnRefreshListener(() -> {
            homeViewModel.loadBestDealList();
            homeViewModel.loadPopularList();
            menuHome.startAnimation(layoutAnimationController.getAnimation());
            refreshHome.setRefreshing(false);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewpager.resumeAutoScroll();
    }

    @Override
    public void onPause() {
        super.onPause();
        viewpager.pauseAutoScroll();
    }
}