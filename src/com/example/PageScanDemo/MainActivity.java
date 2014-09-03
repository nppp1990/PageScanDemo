package com.example.PageScanDemo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * @author yuanjian
 * @version 1.0
 * @created 2014-09-01
 */
public class MainActivity extends FragmentActivity implements View.OnClickListener, TabPageScanView.OnPageChangeListener {

    private Button leftBtn;
    private Button rightBtn;
    private Button newBtn;

    private View scanLayout;
    private FrameLayout screenView;

    private TabPageScanView tabPageScanView;
    private TextView urlTextView;
    private int tagIndex;// 每添加一个page就+1
    private String currentTag;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        screenView = (FrameLayout) findViewById(R.id.list_screen);
        leftBtn = (Button) findViewById(R.id.left_btn);
        rightBtn = (Button) findViewById(R.id.right_btn);
        newBtn = (Button) findViewById(R.id.new_btn);
        tabPageScanView = (TabPageScanView) findViewById(R.id.test);
        tabPageScanView.setImgPadding(30);
        tabPageScanView.setOnPageChangeListener(this);
        scanLayout = findViewById(R.id.layout_scan);
        urlTextView = (TextView) findViewById(R.id.url);

        leftBtn.setOnClickListener(this);
        rightBtn.setOnClickListener(this);
        newBtn.setOnClickListener(this);

        init();

    }

    private void init() {
        addPage();
        screenView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (tabPageScanView.getMainBitmap() == null) {
                    Bitmap bitmap = TabPageScanView.convertViewToBitmap(screenView, SCALE_X, SCALE_Y);
                    if (bitmap != null) {
                        tabPageScanView.setMainBitmap(bitmap);
                    }
                }
            }
        });
        setLeftBtnEnable(true);
    }

    private void setLeftBtnEnable(boolean enable) {
        leftBtn.setEnabled(enable);
        rightBtn.setEnabled(!enable);
    }


    public static final float SCALE_X = 5.0f / 8;
    public static final float SCALE_X_REVERSE = 8.0f / 5;
    public static final float SCALE_Y = 5.0f / 8;
    public static final float SCALE_Y_REVERSE = 8.0f / 5;

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.left_btn) {
            Bitmap bitmap = TabPageScanView.convertViewToBitmap(screenView, SCALE_X, SCALE_Y);
            if (bitmap != null) {
                if (tabPageScanView.getChildCount() == 0) {
                    tabPageScanView.addPageView(0, genFragmentTag(tagIndex), bitmap);
                } else {
                    tabPageScanView.changeView(tabPageScanView.getCurrentPosition(), bitmap);
                }
                startShowScanAnimation();
                currentTag = (String) tabPageScanView.getChildAt(tabPageScanView.getCurrentPosition()).getTag();
                setLeftBtnEnable(false);
            }

        } else if (view.getId() == R.id.right_btn) {
            String newTag = (String) tabPageScanView.getChildAt(tabPageScanView.getCurrentPosition()).getTag();
            changeFragment(currentTag, newTag);
            startHideScanAnimation();
            setLeftBtnEnable(true);
        } else if (view.getId() == R.id.new_btn) {
            addPage();
            tabPageScanView.addMainView(tabPageScanView.getCurrentPosition() + 1, genFragmentTag(tagIndex));
            tabPageScanView.goToCenter(tabPageScanView.getCurrentPosition() + 1);
        }
    }

    private void startShowScanAnimation() {
        screenView.setVisibility(View.GONE);
        scanLayout.setVisibility(View.VISIBLE);
        ScaleAnimation scaleAnimation = new ScaleAnimation(SCALE_X_REVERSE, 1, SCALE_Y_REVERSE, 1,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setAnimationListener(animatorListener1);
        scaleAnimation.setDuration(500);
        int currentPosition = tabPageScanView.getCurrentPosition();
        View view = tabPageScanView.getChildAt(currentPosition);
        view.startAnimation(scaleAnimation);

        if (currentPosition > 0) {
            View leftView = tabPageScanView.getChildAt(currentPosition - 1);
            leftView.setVisibility(View.INVISIBLE);
        }

        if (currentPosition < tabPageScanView.getChildCount() - 1) {
            View rightView = tabPageScanView.getChildAt(currentPosition + 1);
            rightView.setVisibility(View.INVISIBLE);
        }


    }

    private void startHideScanAnimation() {
        newBtn.setVisibility(View.GONE);
        int currentPosition = tabPageScanView.getCurrentPosition();
        if (currentPosition > 0) {
            View leftView = tabPageScanView.getChildAt(currentPosition - 1);
            leftView.setVisibility(View.INVISIBLE);
        }

        if (currentPosition < tabPageScanView.getChildCount() - 1) {
            View rightView = tabPageScanView.getChildAt(currentPosition + 1);
            rightView.setVisibility(View.INVISIBLE);
        }

        ScaleAnimation scaleAnimation = new ScaleAnimation(1, SCALE_X_REVERSE, 1, SCALE_Y_REVERSE,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setAnimationListener(animatorListener2);
        scaleAnimation.setDuration(500);
        scaleAnimation.setFillAfter(false);
        View view = tabPageScanView.getChildAt(currentPosition);
        view.startAnimation(scaleAnimation);
    }

    private void addPage() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.list_screen, new PageFragment(), genFragmentTag(++tagIndex));
        ft.commitAllowingStateLoss();
    }

    private void changeFragment(String oldTabIndex, String newTabIndex) {
        if (oldTabIndex.equals(newTabIndex)) {
            return;
        }
        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(oldTabIndex);
        Fragment targetFragment = getSupportFragmentManager().findFragmentByTag(newTabIndex);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (currentFragment != null) {
            ft.detach(currentFragment);
        }
        if (targetFragment == null) {
            targetFragment = createFragment();
            ft.add(R.id.list_screen, targetFragment, newTabIndex);
        } else {
            ft.attach(targetFragment);
        }
        ft.commitAllowingStateLoss();
        getSupportFragmentManager().executePendingTransactions();
    }

    private String genFragmentTag(int index) {
        return "tag" + index;
    }

    private Fragment createFragment() {
        return new PageFragment();
    }


    private Animation.AnimationListener animatorListener1 = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            int currentPosition = tabPageScanView.getCurrentPosition();
            if (currentPosition > 0) {
                View leftView = tabPageScanView.getChildAt(currentPosition - 1);
                leftView.setVisibility(View.VISIBLE);
            }

            if (currentPosition < tabPageScanView.getChildCount() - 1) {
                View rightView = tabPageScanView.getChildAt(currentPosition + 1);
                rightView.setVisibility(View.VISIBLE);
            }
            newBtn.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

    private Animation.AnimationListener animatorListener2 = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            int currentPosition = tabPageScanView.getCurrentPosition();
            if (currentPosition > 0) {
                View leftView = tabPageScanView.getChildAt(currentPosition - 1);
                leftView.setVisibility(View.VISIBLE);
            }

            if (currentPosition < tabPageScanView.getChildCount() - 1) {
                View rightView = tabPageScanView.getChildAt(currentPosition + 1);
                rightView.setVisibility(View.VISIBLE);
            }
            screenView.setVisibility(View.VISIBLE);
            scanLayout.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

    @Override
    public void onPageSelected(int position) {
        urlTextView.setText("position==" + position);
    }
}
