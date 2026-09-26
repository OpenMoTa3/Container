package org.dev.ime.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class JsonKeyboardView extends View {

    public interface OnKeyboardActionListener {
        void onPress(int primaryCode);
        void onRelease(int primaryCode);
        void onKey(int primaryCode, int[] keyCodes);
        void onText(CharSequence text);
    }

    private JsonKeyboard mKeyboard;
    private JsonKeyboard mMainKeyboard, mSubKeyboard;
    private OnKeyboardActionListener mActionListener;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mRect = new Rect();

    private JsonKeyboard.Key mCurrentKey;
    private boolean mShifted;
    private float mDownX, mDownY;
    private final int mSwipeSlop;

    // 主题动态颜色
    private int mColorKeyNormal, mColorKeyModifier, mColorKeyActive;
    private int mColorTextPrimary, mColorTextSecondary;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRepeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (mCurrentKey != null && mCurrentKey.isRepeatable && mActionListener != null) {
                if (mCurrentKey.codes.length > 0) mActionListener.onKey(mCurrentKey.codes[0], mCurrentKey.codes);
                mHandler.postDelayed(this, 50);
            }
        }
    };

    public JsonKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSwipeSlop = ViewConfiguration.get(context).getScaledTouchSlop() * 2;
        initThemeColors(context);
    }

    private void initThemeColors(Context context) {
        // 从当前上下文主题中汲取标准属性色
        int[] attrs = {
                android.R.attr.colorBackground,
                android.R.attr.textColorPrimary,
                android.R.attr.textColorSecondary,
                android.R.attr.colorControlHighlight
        };
        TypedArray ta = context.obtainStyledAttributes(attrs);
        int bg = ta.getColor(0, Color.WHITE);
        mColorTextPrimary = ta.getColor(1, Color.BLACK);
        mColorTextSecondary = ta.getColor(2, Color.GRAY);
        mColorKeyActive = ta.getColor(3, Color.LTGRAY);
        ta.recycle();

        // 计算普通键底色和功能键底色（基于主题背景色自动微调）
        boolean isDark = (Color.red(bg) * 0.299 + Color.green(bg) * 0.587 + Color.blue(bg) * 0.114) < 128;
        mColorKeyNormal = isDark ? 0xFF2A2A2A : 0xFFFFFFFF;
        mColorKeyModifier = isDark ? 0xFF1F1F1F : 0xFFE0E0E0;
    }

    public void setKeyboard(JsonKeyboard keyboard) {
        this.mKeyboard = keyboard;
        this.mMainKeyboard = keyboard;
        requestLayout();
        invalidate();
    }

    public void setKeyboards(JsonKeyboard mainKeyboard, JsonKeyboard subKeyboard) {
        this.mMainKeyboard = mainKeyboard;
        this.mSubKeyboard = subKeyboard;
        this.mKeyboard = mainKeyboard;
        requestLayout();
        invalidate();
    }

    public void switchKeyboard() {
        if (mSubKeyboard == null) return;
        mKeyboard = (mKeyboard == mMainKeyboard) ? mSubKeyboard : mMainKeyboard;
        requestLayout();
        invalidate();
    }

    public void resetKeyboard() {
        if (mMainKeyboard != null && mKeyboard != mMainKeyboard) {
            mKeyboard = mMainKeyboard;
            requestLayout();
            invalidate();
        }
    }

    public void setOnKeyboardActionListener(OnKeyboardActionListener l) { this.mActionListener = l; }
    public void setShifted(boolean shifted) { this.mShifted = shifted; invalidate(); }
    public boolean isShifted() { return mShifted; }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = mKeyboard != null ? mKeyboard.getMinWidth() : 0;
        int h = mKeyboard != null ? mKeyboard.getHeight() : 0;
        setMeasuredDimension(resolveSize(w + getPaddingLeft() + getPaddingRight(), widthSpec),
                             resolveSize(h + getPaddingTop() + getPaddingBottom(), heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mKeyboard == null) return;

        for (JsonKeyboard.Key key : mKeyboard.getKeys()) {
            if (key.codes == null || (key.codes.length == 0 && (key.label == null || key.label.isEmpty()))) continue;

            // 1. 绘制按键背景
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(key == mCurrentKey ? mColorKeyActive : (key.isModifier || (key.codes.length > 0 && key.codes[0] < 0) ? mColorKeyModifier : mColorKeyNormal));
            canvas.drawRoundRect(key.x + 4, key.y + 4, key.x + key.width - 4, key.y + key.height - 4, 12, 12, mPaint);

            // 2. 绘制图标/主文字
            if (key.icon != null && !key.icon.isEmpty()) {
                int id = getContext().getResources().getIdentifier(key.icon, "drawable", getContext().getPackageName());
                Drawable d = id != 0 ? getContext().getDrawable(id) : null;
                if (d != null) {
                    int w = d.getIntrinsicWidth(), h = d.getIntrinsicHeight();
                    d.setBounds(key.x + (key.width - w) / 2, key.y + (key.height - h) / 2, key.x + (key.width + w) / 2, key.y + (key.height + h) / 2);
                    d.setTint(mColorTextPrimary);
                    d.draw(canvas);
                }
            } else if (key.label != null) {
                String text = (mShifted && key.label.length() == 1) ? key.label.toUpperCase() : key.label;
                mPaint.setColor(mColorTextPrimary);
                mPaint.setTextSize(key.label.length() > 1 ? 32f : 44f);
                mPaint.setTextAlign(Paint.Align.CENTER);
                mPaint.getTextBounds(text, 0, text.length(), mRect);
                canvas.drawText(text, key.x + key.width / 2f, key.y + key.height / 2f + mRect.height() / 2f, mPaint);
            }

            // 3. 顺时针绘制四角角标: 0=左上(上滑), 1=右上(右滑), 2=右下(下滑), 3=左下(左滑)
            if (key.popupCharacters != null && !key.popupCharacters.isEmpty()) {
                mPaint.setColor(mColorTextSecondary);
                mPaint.setTextSize(22f);
                int len = key.popupCharacters.length();
                for (int i = 0; i < len && i < 4; i++) {
                    boolean isRight = (i == 1 || i == 2);
                    mPaint.setTextAlign(isRight ? Paint.Align.RIGHT : Paint.Align.LEFT);
                    float tx = isRight ? key.x + key.width - 10 : key.x + 10;
                    float ty = (i <= 1) ? key.y + 26 : key.y + key.height - 10;
                    canvas.drawText(String.valueOf(key.popupCharacters.charAt(i)), tx, ty, mPaint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int x = (int) e.getX(), y = (int) e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = e.getX();
                mDownY = e.getY();
                mCurrentKey = findKey(x, y);
                if (mCurrentKey != null) {
                    invalidate();
                    if (mActionListener != null && mCurrentKey.codes.length > 0) mActionListener.onPress(mCurrentKey.codes[0]);
                    if (mCurrentKey.isRepeatable) mHandler.postDelayed(mRepeatRunnable, 400);
                }
                return true;

            case MotionEvent.ACTION_UP:
                mHandler.removeCallbacks(mRepeatRunnable);
                if (mCurrentKey != null) {
                    int primaryCode = mCurrentKey.codes.length > 0 ? mCurrentKey.codes[0] : 0;
                    if (primaryCode == -2) {
                        switchKeyboard();
                        if (mActionListener != null) mActionListener.onKey(primaryCode, mCurrentKey.codes);
                    } else if (primaryCode == -1) {
                        setShifted(!mShifted);
                        if (mActionListener != null) mActionListener.onKey(primaryCode, mCurrentKey.codes);
                    } else if (mActionListener != null) {
                        // 判定四方向手势滑动 (顺时针: 0=左上上滑, 1=右上右滑, 2=右下下滑, 3=左下左滑)
                        float dx = e.getX() - mDownX;
                        float dy = e.getY() - mDownY;
                        int swipeDirection = -1;

                        if (Math.abs(dx) > mSwipeSlop || Math.abs(dy) > mSwipeSlop) {
                            if (Math.abs(dy) >= Math.abs(dx)) {
                                swipeDirection = (dy < 0) ? 0 : 2; // 上滑(0) 或 下滑(2)
                            } else {
                                swipeDirection = (dx > 0) ? 1 : 3; // 右滑(1) 或 左滑(3)
                            }
                        }

                        if (swipeDirection != -1 && mCurrentKey.popupCharacters != null && mCurrentKey.popupCharacters.length() > swipeDirection) {
                            mActionListener.onText(String.valueOf(mCurrentKey.popupCharacters.charAt(swipeDirection)));
                        } else if (mCurrentKey.keyOutputText != null) {
                            mActionListener.onText(mCurrentKey.keyOutputText);
                        } else if (primaryCode != 0) {
                            int code = primaryCode;
                            if (mShifted && Character.isLowerCase(code)) code = Character.toUpperCase(code);
                            mActionListener.onKey(code, mCurrentKey.codes);
                        }
                        mActionListener.onRelease(primaryCode);
                    }
                    mCurrentKey = null;
                    invalidate();
                    performClick();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                mHandler.removeCallbacks(mRepeatRunnable);
                mCurrentKey = null;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() { return super.performClick(); }

    private JsonKeyboard.Key findKey(int x, int y) {
        if (mKeyboard == null || mKeyboard.getKeys().isEmpty()) return null;
        int cx = Math.max(0, Math.min(x, mKeyboard.getMinWidth() - 1));
        int cy = Math.max(0, Math.min(y, mKeyboard.getHeight() - 1));

        for (JsonKeyboard.Key k : mKeyboard.getKeys()) {
            if (k.contains(cx, cy) && (k.codes.length > 0 || (k.label != null && !k.label.isEmpty()))) return k;
        }
        JsonKeyboard.Key near = null;
        int minD = Integer.MAX_VALUE;
        for (JsonKeyboard.Key k : mKeyboard.getKeys()) {
            if (k.codes.length == 0 && (k.label == null || k.label.isEmpty())) continue;
            int d = k.distSq(cx, cy);
            if (d < minD) { minD = d; near = k; }
        }
        return near;
    }
}