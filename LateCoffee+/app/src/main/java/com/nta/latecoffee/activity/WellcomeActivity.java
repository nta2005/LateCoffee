package com.nta.latecoffee.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.nta.latecoffee.R;
import com.nta.latecoffee.adapter.IntroViewPagerAdapter;
import com.nta.latecoffee.model.ScreenItem;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WellcomeActivity extends AppCompatActivity {

    IntroViewPagerAdapter adapter;
    int position = 0;

    @BindView(R.id.tv_skip)
    TextView tvSkip;
    @BindView(R.id.btn_next)
    Button btnNext;
    @BindView(R.id.btn_get_started)
    Button btnGetStarted;
    @BindView(R.id.tab_indicator)
    TabLayout tabIndicator;
    @BindView(R.id.screen_viewpager)
    ViewPager screenPager;

    Animation btnAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //When this activity is about to be launch we need to check if its opened before or not
        if (restorePrefData()) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
        setContentView(R.layout.activity_wellcome);

        //Init views
        btnAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_animation);
        ButterKnife.bind(this);

        //Fill list screen
        final List<ScreenItem> mList = new ArrayList<>();
        mList.add(new ScreenItem(
                "Lựa chọn đồ uống yêu thích của bạn",
                "Hãy lựa chọn và đặt hàng đồ uống bất kỳ mà bạn thích, chúng tôi sẽ đem nó đến cho bạn.",
                R.drawable.image1));
        mList.add(new ScreenItem(
                "Thanh toán tiện lợi",
                "Bạn có thể thanh toán trực tiếp khi nhận được hàng hoặc thanh toán online cho chúng tôi.",
                R.drawable.image2));
        mList.add(new ScreenItem(
                "Thư giãn và thưởng thức cùng bạn bè",
                "Thư giãn và chờ đợi đồ uống bạn yêu thích để thưởng thức cùng bạn bè.",
                R.drawable.image3));

        //Setup Viewpager
        adapter = new IntroViewPagerAdapter(this, mList);
        screenPager.setAdapter(adapter);

        //Setup TabLayout with ViewPager
        tabIndicator.setupWithViewPager(screenPager);

        //Next Button click
        btnNext.setOnClickListener(view -> {
            position = screenPager.getCurrentItem();
            if (position < mList.size()) {
                position++;
                screenPager.setCurrentItem(position);
            }

            if (position == mList.size() - 1) { // when we reach to the last screen
                // TODO : show the GETSTARTED Button and hide the indicator and the next button
                loadLastScreen();
            }
        });

        //TabLayout add change listener
        tabIndicator.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == mList.size() - 1) {
                    loadLastScreen();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });


        //Get Started Button click
        btnGetStarted.setOnClickListener(view -> {
            //Open MainActivity
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            //Also we need to save a boolean value to storage so next time when the user run the app
            //We could know that he is already checked the intro screen activity
            //I'm going to use shared preferences to that process
            savePrefsData();
            finish();
        });

        //Skip Button click
        tvSkip.setOnClickListener(v -> screenPager.setCurrentItem(mList.size()));
    }

    private boolean restorePrefData() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("myPrefs", MODE_PRIVATE);
        Boolean isIntroActivityOpnendBefore = pref.getBoolean("isIntroOpened", false);
        return isIntroActivityOpnendBefore;
    }

    private void savePrefsData() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("isIntroOpened", true);
        editor.commit();
    }

    //Show the GetStarted Button and hide the indicator and the next button
    private void loadLastScreen() {
        btnNext.setVisibility(View.INVISIBLE);
        btnGetStarted.setVisibility(View.VISIBLE);
        tvSkip.setVisibility(View.INVISIBLE);
        tabIndicator.setVisibility(View.INVISIBLE);
        // TODO : ADD an animation the getstarted button
        //Setup Animation
        btnGetStarted.setAnimation(btnAnim);
    }
}