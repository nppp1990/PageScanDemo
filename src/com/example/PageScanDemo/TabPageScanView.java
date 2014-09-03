package com.example.PageScanDemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.Scroller;

/**
 * @author yuanjian
 * @version 1.0
 * @created 2014-09-01
 */
public class TabPageScanView extends ViewGroup {

    public interface OnPageChangeListener {
        public void onPageSelected(int position);
    }

    public static final String TAG = "yj";
    private Bitmap mainBitmap;
    private Context context;
    private VelocityTracker mVelocityTracker;
    private Scroller scroller;
    private int mTouchSlop;
    public static int SNAP_VELOCITY = 600;

    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLLING = 1;
    private int mTouchState = TOUCH_STATE_REST;

    private OnPageChangeListener listener;
    private int currentPosition;

    public TabPageScanView(Context context) {
        super(context);
        init(context);
    }

    public TabPageScanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        scroller = new Scroller(context);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.listener = listener;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setMainBitmap(Bitmap mainBitmap) {
        this.mainBitmap = mainBitmap;
    }

    public Bitmap getMainBitmap() {
        return mainBitmap;
    }

    public void addMainView(int index, String tag) {
        ImageView imageView = new ImageView(context);
        imageView.setImageBitmap(mainBitmap);
        imageView.setTag(tag);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(imageView, index, layoutParams);
    }

    public void addPageView(int index, String tag, Bitmap bitmap) {
        ImageView imageView = new ImageView(context);
        imageView.setImageBitmap(bitmap);
        imageView.setTag(tag);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(imageView, index, layoutParams);
    }

    public void changeView(int index, Bitmap bitmap) {
        ImageView imageView = (ImageView) getChildAt(index);
        imageView.setImageBitmap(bitmap);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft = layoutPadding;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View childView = getChildAt(i);
            if (childView.getVisibility() != View.GONE) {
                final int childWidth = childView.getMeasuredWidth();
                final int childHeight = childView.getMeasuredHeight();
                int top = (height - childHeight) / 2;
                childView.layout(childLeft, top,
                        childLeft + childWidth, top + childView.getMeasuredHeight());
                childLeft += (childWidth + imgPadding);
            }
        }
    }

    private int imgWidth;
    private int imgPadding;  // 图片之间的间距
    private int layoutPadding;


    private int width;
    private int height;
    private int specWidth;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        specWidth = MeasureSpec.getSize(widthMeasureSpec);
        this.height = MeasureSpec.getSize(heightMeasureSpec);
        final int count = getChildCount();
        width = layoutPadding * 2;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            getChildAt(i).measure(child.getMeasuredWidth(), child.getMeasuredHeight());
            if (imgWidth == 0) {
                imgWidth = child.getMeasuredWidth();
            } else if (imgWidth != child.getMeasuredWidth()) {
                Log.e(TAG, "error");
            }
            width += child.getMeasuredWidth();
        }
        width += (count - 1) * imgPadding;
        layoutPadding = (specWidth - imgWidth) / 2;
    }


    private float downPointX = 0.0f;


    /**
     * Called by a parent to request that a child update its values for mScrollX
     * and mScrollY if necessary. This will typically be done if the child is
     * animating a scroll using a {@link android.widget.Scroller Scroller}
     * object.
     */
    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), 0);
            Log.d(TAG, "X==" + scroller.getCurrX());
            postInvalidate();
        } else {
            currentPosition = getNearPosition();
            if (listener != null) {
                listener.onPageSelected(currentPosition);
            }
        }
    }


    //滑动到相应的View
    private void moveToScreen(int position) {
        int dx = layoutPadding + (imgPadding + imgWidth) * position - (specWidth - imgWidth) / 2 - getScrollX();
        Log.d(TAG, "---dx==" + dx);
        scroller.startScroll(getScrollX(), 0, dx, 0, Math.abs(dx) * 2);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        mVelocityTracker.addMovement(ev);
        super.onTouchEvent(ev);
        final int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if (scroller != null) {
                    if (!scroller.isFinished()) {
                        scroller.abortAnimation();
                    }
                }
                downPointX = ev.getX();
                break;
            }
            case MotionEvent.ACTION_MOVE:
                final float currentX = ev.getX();
                final float dis = downPointX - currentX;
                onDistanceSlide(dis);
                downPointX = currentX;
                break;
            case MotionEvent.ACTION_UP:


//                goToCenter(getNearPosition());

                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000);
                int velocityX = (int) velocityTracker.getXVelocity();
                Log.d(TAG, "------vo==" + velocityX);
                Log.d(TAG, "------currentPosition==" + currentPosition);

                if (velocityX > SNAP_VELOCITY && currentPosition > 0) {
                    moveToScreen(currentPosition - 1);
                } else if (velocityX < -SNAP_VELOCITY && currentPosition < (getChildCount() - 1)) {
                    moveToScreen(currentPosition + 1);
                } else {
                    moveToScreen(currentPosition);
                }

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP:
                break;
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();

        if ((action == MotionEvent.ACTION_MOVE) && (mTouchState != TOUCH_STATE_REST)) {
            return true;
        }

        final float x = ev.getX();

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                final int xDiff = (int) Math.abs(downPointX - x);
                if (xDiff > mTouchSlop) {
                    mTouchState = TOUCH_STATE_SCROLLING;
                }
                break;

            case MotionEvent.ACTION_DOWN:
                downPointX = x;
                mTouchState = scroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mTouchState = TOUCH_STATE_REST;
                break;
        }
        return mTouchState != TOUCH_STATE_REST;
    }


    private int getNearPosition() {
        float i = (getScrollX() + specWidth / 2 - layoutPadding + imgPadding / 2.0f) / (imgWidth + imgPadding);
//        Log.d(TAG, "i===" + i + "--near==" + (int) Math.floor(i));
        return (int) Math.floor(i);

    }

    public void goToCenter(int position) {
        int scrollX;
        if (position <= 0) {
            scrollX = 0;
        } else {
            scrollX = layoutPadding + (position + 1) * imgWidth + position * imgPadding - (specWidth + imgWidth) / 2;
        }
        scrollTo(scrollX, 0);
        currentPosition = position;
        if (listener != null) {
            listener.onPageSelected(position);
        }

    }

    public void onDistanceSlide(float distance) {
        if (getChildCount() == 0) {
            return;
        }
        final int maxScroll = width - specWidth;

        int scrollX = getScrollX();
        if (scrollX == 0 && distance < 0) {
            return;
        }

        if (scrollX >= maxScroll && distance > 0) {
            return;
        }

        if (distance < 0 && (distance + scrollX) <= 0) {
            scrollTo(0, 0);
            return;
        }
        if (distance > 0 && (distance + scrollX) >= maxScroll) {
            scrollTo(maxScroll, 0);
            return;
        }
        scrollBy((int) distance, 0);

        currentPosition = getNearPosition();
        if (listener != null) {
            listener.onPageSelected(currentPosition);
        }
    }

    public void setImgPadding(int padding) {
        imgPadding = padding;
    }


    public static Bitmap convertViewToBitmap(View view, float scaleX, float scaleY) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap b1 = view.getDrawingCache();
        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY);
        Bitmap bitmap = Bitmap.createBitmap(b1, 0, 0, view.getMeasuredWidth(), view.getMeasuredHeight(), matrix, false);
        view.destroyDrawingCache();
//        view.setWillNotCacheDrawing(false);
//        view.setDrawingCacheEnabled(false);
//        view.destroyDrawingCache();
        return bitmap;
    }
}
