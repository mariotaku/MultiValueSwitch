/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mariotaku.multivalueswitch.library;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.TintableBackgroundView;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.MVS_AppCompatBackgroundHelperAccessor;
import android.support.v7.widget.MVS_DrawableUtilsAccessor;
import android.support.v7.widget.MVS_TintContextWrapperAccessor;
import android.support.v7.widget.TintTypedArray;
import android.support.v7.widget.ViewUtils;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * SwitchCompat is a version of the Switch widget which on devices back to API v7. It does not
 * make any attempt to use the platform provided widget on those devices which it is available
 * normally.
 */
@SuppressWarnings("RestrictedApi")
public class MultiValueSwitch extends View implements TintableBackgroundView {
    private static final int THUMB_ANIMATION_DURATION = 250;

    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int TOUCH_MODE_DRAGGING = 2;

    // We force the accessibility events to have a class name of Switch, since screen readers
    // already know how to handle their events
    private static final String ACCESSIBILITY_EVENT_CLASS_NAME = "android.widget.Switch";
    private final MVS_AppCompatBackgroundHelperAccessor mBackgroundTintHelper;

    @Nullable
    private CharSequence[] mEntries;


    private Drawable mThumbDrawable;
    private ColorStateList mThumbTintList = null;
    private PorterDuff.Mode mThumbTintMode = null;
    private boolean mHasThumbTint = false;
    private boolean mHasThumbTintMode = false;

    private Drawable mTrackDrawable;
    private ColorStateList mTrackTintList = null;
    private PorterDuff.Mode mTrackTintMode = null;
    private boolean mHasTrackTint = false;
    private boolean mHasTrackTintMode = false;

    private ColorStateList mBackgroundTintList = null;
    private PorterDuff.Mode mBackgroundTintMode = null;
    private boolean mHasBackgroundTint = false;
    private boolean mHasBackgroundTintMode = false;

    private int mSwitchMinWidth;
    private int mSwitchPadding;
    private boolean mSplitTrack;
    private Paint mPointPaint;
    private float mPointRadius;

    private int mTouchMode;
    private int mTouchSlop;
    private boolean mTouchDown;
    private float mTouchX;
    private float mTouchY;
    private VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private int mMinFlingVelocity;

    private float mThumbPosition;

    /**
     * Width required to draw the switch track and thumb. Includes padding and
     * optical bounds for both the track and thumb.
     */
    private int mSwitchWidth;

    /**
     * Height required to draw the switch track and thumb. Includes padding and
     * optical bounds for both the track and thumb.
     */
    private int mSwitchHeight;

    /**
     * Width of the thumb's content region. Does not include padding or
     * optical bounds.
     */
    private int mThumbWidth;

    /**
     * Left bound for drawing the switch track and thumb.
     */
    private int mSwitchLeft;

    /**
     * Top bound for drawing the switch track and thumb.
     */
    private int mSwitchTop;

    /**
     * Right bound for drawing the switch track and thumb.
     */
    private int mSwitchRight;

    /**
     * Bottom bound for drawing the switch track and thumb.
     */
    private int mSwitchBottom;

    private ThumbAnimation mPositionAnimator;

    @SuppressWarnings("hiding")
    private final Rect mTempRect = new Rect();

    private final AppCompatDrawableManager mDrawableManager;

    private static final int[] CHECKED_STATE_SET = {
            android.R.attr.state_checked
    };
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private int mMax;
    private OnThumbPositionChangeListener mOnCheckedOffsetChangeListener;
    private int[] mHighlightCheckedPositions;
    private int mTargetCheckedPosition;


    /**
     * Construct a new Switch with default styling.
     *
     * @param context The Context that will determine this widget's theming.
     */
    public MultiValueSwitch(Context context) {
        this(context, null);
    }

    /**
     * Construct a new Switch with default styling, overriding specific style
     * attributes as requested.
     *
     * @param context The Context that will determine this widget's theming.
     * @param attrs   Specification of attributes that should deviate from default styling.
     */
    public MultiValueSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.switchStyle);
    }

    /**
     * Construct a new Switch with a default style determined by the given theme attribute,
     * overriding specific style attributes as requested.
     *
     * @param context      The Context that will determine this widget's theming.
     * @param attrs        Specification of attributes that should deviate from the default styling.
     * @param defStyleAttr An attribute in the current theme that contains a
     *                     reference to a style resource that supplies default values for
     *                     the view. Can be 0 to not look for defaults.
     */
    @SuppressLint("PrivateResource")
    public MultiValueSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(MVS_TintContextWrapperAccessor.wrap(context), attrs, defStyleAttr);


        mDrawableManager = AppCompatDrawableManager.get();
        mBackgroundTintHelper = new MVS_AppCompatBackgroundHelperAccessor(this);
        mBackgroundTintHelper.loadFromAttributes(attrs, defStyleAttr);

        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context,
                attrs, android.support.v7.appcompat.R.styleable.SwitchCompat, defStyleAttr, 0);
        mThumbDrawable = a.getDrawable(android.support.v7.appcompat.R.styleable.SwitchCompat_android_thumb);
        if (mThumbDrawable != null) {
            mThumbDrawable.setCallback(this);
        }
        mTrackDrawable = a.getDrawable(android.support.v7.appcompat.R.styleable.SwitchCompat_track);
        if (mTrackDrawable != null) {
            mTrackDrawable.setCallback(this);
        }

        mPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointPaint.setColor(0x33000000);
        mPointPaint.setStyle(Paint.Style.FILL);
        mPointRadius = getResources().getDisplayMetrics().density * 2;

        mSwitchMinWidth = a.getDimensionPixelSize(
                android.support.v7.appcompat.R.styleable.SwitchCompat_switchMinWidth, 0);
        mSwitchPadding = a.getDimensionPixelSize(
                android.support.v7.appcompat.R.styleable.SwitchCompat_switchPadding, 0);
        mSplitTrack = a.getBoolean(android.support.v7.appcompat.R.styleable.SwitchCompat_splitTrack, false);

        a.recycle();

        a = TintTypedArray.obtainStyledAttributes(context,
                attrs, R.styleable.MultiValueSwitch, defStyleAttr, 0);
        setMax(a.getInt(R.styleable.MultiValueSwitch_android_max, 2));
        setEntries(a.getTextArray(R.styleable.MultiValueSwitch_android_entries));
        mThumbPosition = getThumbPosition(a.getInt(R.styleable.MultiValueSwitch_android_position, 0));
        setEnabled(a.getBoolean(R.styleable.MultiValueSwitch_android_enabled, true));
        a.recycle();

        final ViewConfiguration config = ViewConfiguration.get(context);
        mTouchSlop = config.getScaledTouchSlop();
        mMinFlingVelocity = config.getScaledMinimumFlingVelocity();

        // Refresh display with current params
        refreshDrawableState();
        setCheckedPosition(getCheckedPosition());
    }

    public void setEntries(@Nullable CharSequence[] entries) {
        if (entries != null && entries.length != getMax()) throw new IllegalArgumentException();
        mEntries = entries;
    }

    /**
     * Set the amount of horizontal padding between the switch and the associated text.
     *
     * @param pixels Amount of padding in pixels
     */
    public void setSwitchPadding(int pixels) {
        mSwitchPadding = pixels;
        requestLayout();
    }

    /**
     * Get the amount of horizontal padding between the switch and the associated text.
     *
     * @return Amount of padding in pixels
     */
    public int getSwitchPadding() {
        return mSwitchPadding;
    }

    /**
     * Set the minimum width of the switch in pixels. The switch's width will be the maximum
     * of this value and its measured width as determined by the switch drawables and text used.
     *
     * @param pixels Minimum width of the switch in pixels
     */
    public void setSwitchMinWidth(int pixels) {
        mSwitchMinWidth = pixels;
        requestLayout();
    }

    /**
     * Get the minimum width of the switch in pixels. The switch's width will be the maximum
     * of this value and its measured width as determined by the switch drawables and text used.
     *
     * @return Minimum width of the switch in pixels
     */
    public int getSwitchMinWidth() {
        return mSwitchMinWidth;
    }

    /**
     * Set the drawable used for the track that the switch slides within.
     *
     * @param track Track drawable
     */
    public void setTrackDrawable(Drawable track) {
        mTrackDrawable = track;
        requestLayout();
    }

    /**
     * Set the drawable used for the track that the switch slides within.
     *
     * @param resId Resource ID of a track drawable
     */
    public void setTrackResource(int resId) {
        setTrackDrawable(mDrawableManager.getDrawable(getContext(), resId));
    }

    /**
     * Get the drawable used for the track that the switch slides within.
     *
     * @return Track drawable
     */
    public Drawable getTrackDrawable() {
        return mTrackDrawable;
    }

    /**
     * Applies a tint to the track drawable. Does not modify the current
     * tint mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p/>
     * Subsequent calls to {@link #setTrackDrawable(Drawable)} will
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #getTrackTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setTrackTintList(@Nullable ColorStateList tint) {
        mTrackTintList = tint;
        mHasTrackTint = true;

        applyTrackTint();
    }

    /**
     * @return the tint applied to the track drawable
     * @see #setTrackTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getTrackTintList() {
        return mTrackTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setTrackTintList(ColorStateList)}} to the track drawable.
     * The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @see #getTrackTintMode()
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setTrackTintMode(@Nullable PorterDuff.Mode tintMode) {
        mTrackTintMode = tintMode;
        mHasTrackTintMode = true;

        applyTrackTint();
    }

    /**
     * @return the blending mode used to apply the tint to the track
     * drawable
     * @see #setTrackTintMode(PorterDuff.Mode)
     */
    @Nullable
    public PorterDuff.Mode getTrackTintMode() {
        return mTrackTintMode;
    }

    private void applyTrackTint() {
        if (mTrackDrawable != null && (mHasTrackTint || mHasTrackTintMode)) {
            mTrackDrawable = mTrackDrawable.mutate();

            if (mHasTrackTint) {
                DrawableCompat.setTintList(mTrackDrawable, mTrackTintList);
            }

            if (mHasTrackTintMode) {
                DrawableCompat.setTintMode(mTrackDrawable, mTrackTintMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mTrackDrawable.isStateful()) {
                mTrackDrawable.setState(getDrawableState());
            }
        }
    }

    /**
     * Set the drawable used for the switch "thumb" - the piece that the user
     * can physically touch and drag along the track.
     *
     * @param thumb Thumb drawable
     */
    public void setThumbDrawable(Drawable thumb) {
        mThumbDrawable = thumb;
        requestLayout();
    }

    /**
     * Set the drawable used for the switch "thumb" - the piece that the user
     * can physically touch and drag along the track.
     *
     * @param resId Resource ID of a thumb drawable
     */
    public void setThumbResource(int resId) {
        setThumbDrawable(mDrawableManager.getDrawable(getContext(), resId));
    }

    /**
     * Get the drawable used for the switch "thumb" - the piece that the user
     * can physically touch and drag along the track.
     *
     * @return Thumb drawable
     */
    public Drawable getThumbDrawable() {
        return mThumbDrawable;
    }

    /**
     * Applies a tint to the thumb drawable. Does not modify the current
     * tint mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p/>
     * Subsequent calls to {@link #setThumbDrawable(Drawable)} will
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #getThumbTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setThumbTintList(@Nullable ColorStateList tint) {
        mThumbTintList = tint;
        mHasThumbTint = true;

        applyThumbTint();
    }

    /**
     * @return the tint applied to the thumb drawable
     * @see #setThumbTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getThumbTintList() {
        return mThumbTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setThumbTintList(ColorStateList)}} to the thumb drawable.
     * The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @see #getThumbTintMode()
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setThumbTintMode(@Nullable PorterDuff.Mode tintMode) {
        mThumbTintMode = tintMode;
        mHasThumbTintMode = true;

        applyThumbTint();
    }

    /**
     * @return the blending mode used to apply the tint to the thumb
     * drawable
     * @see #setThumbTintMode(PorterDuff.Mode)
     */
    @Nullable
    public PorterDuff.Mode getThumbTintMode() {
        return mThumbTintMode;
    }

    private void applyThumbTint() {
        if (mThumbDrawable != null && (mHasThumbTint || mHasThumbTintMode)) {
            mThumbDrawable = mThumbDrawable.mutate();

            if (mHasThumbTint) {
                DrawableCompat.setTintList(mThumbDrawable, mThumbTintList);
            }

            if (mHasThumbTintMode) {
                DrawableCompat.setTintMode(mThumbDrawable, mThumbTintMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mThumbDrawable.isStateful()) {
                mThumbDrawable.setState(getDrawableState());
            }
        }
    }

    @Override
    public void setBackgroundResource(@DrawableRes int resId) {
        super.setBackgroundResource(resId);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundResource(resId);
        }
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        super.setBackgroundDrawable(background);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundDrawable(background);
        }
    }


    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#setBackgroundTintList(android.view.View, ColorStateList)}
     *
     * @hide
     */
    @Override
    public void setSupportBackgroundTintList(@Nullable ColorStateList tint) {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.setSupportBackgroundTintList(tint);
        }
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#getBackgroundTintList(android.view.View)}
     *
     * @hide
     */
    @Override
    @Nullable
    public ColorStateList getSupportBackgroundTintList() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintList() : null;
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#setBackgroundTintMode(android.view.View, PorterDuff.Mode)}
     *
     * @hide
     */
    @Override
    public void setSupportBackgroundTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.setSupportBackgroundTintMode(tintMode);
        }
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#getBackgroundTintMode(android.view.View)}
     *
     * @hide
     */
    @Override
    @Nullable
    public PorterDuff.Mode getSupportBackgroundTintMode() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintMode() : null;
    }

    /**
     * Specifies whether the track should be split by the thumb. When true,
     * the thumb's optical bounds will be clipped out of the track drawable,
     * then the thumb will be drawn into the resulting gap.
     *
     * @param splitTrack Whether the track should be split by the thumb
     */
    public void setSplitTrack(boolean splitTrack) {
        mSplitTrack = splitTrack;
        invalidate();
    }

    /**
     * Returns whether the track should be split by the thumb.
     */
    public boolean getSplitTrack() {
        return mSplitTrack;
    }

    public int[] getHighlightCheckedPositions() {
        return mHighlightCheckedPositions;
    }

    public void setHighlightCheckedPositions(final int[] highlightCheckedPositions) {
        mHighlightCheckedPositions = highlightCheckedPositions;
        refreshDrawableState();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final Rect padding = mTempRect;
        final int thumbWidth;
        final int thumbHeight;
        if (mThumbDrawable != null) {
            // Cached thumb width does not include padding.
            mThumbDrawable.getPadding(padding);
            thumbWidth = mThumbDrawable.getIntrinsicWidth() - padding.left - padding.right;
            thumbHeight = mThumbDrawable.getIntrinsicHeight();
        } else {
            thumbWidth = 0;
            thumbHeight = 0;
        }

        mThumbWidth = thumbWidth;

        final int trackHeight;
        if (mTrackDrawable != null) {
            mTrackDrawable.getPadding(padding);
            trackHeight = mTrackDrawable.getIntrinsicHeight();
        } else {
            padding.setEmpty();
            trackHeight = 0;
        }

        // Adjust left and right padding to ensure there's enough room for the
        // thumb's padding (when present).
        int paddingLeft = padding.left;
        int paddingRight = padding.right;
        if (mThumbDrawable != null) {
            final Rect inset = MVS_DrawableUtilsAccessor.getOpticalBounds(mThumbDrawable);
            paddingLeft = Math.max(paddingLeft, inset.left);
            paddingRight = Math.max(paddingRight, inset.right);
        }

        final int switchWidth = Math.max(mSwitchMinWidth,
                getMax() * mThumbWidth + paddingLeft + paddingRight);
        final int switchHeight = Math.max(trackHeight, thumbHeight);
        mSwitchWidth = switchWidth;
        mSwitchHeight = switchHeight;

        final int measuredHeight = getMeasuredHeight();
        if (measuredHeight < switchHeight) {
            setMeasuredDimension(switchWidth, switchHeight);
        } else {
            setMeasuredDimension(switchWidth, measuredHeight);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);

        final CharSequence text = getEntry(getCheckedPosition());
        if (text != null) {
            event.getText().add(text);
        }
    }

    /**
     * @return true if (x, y) is within the target area of the switch thumb
     */
    private boolean hitThumb(float x, float y) {
        if (mThumbDrawable == null) {
            return false;
        }

        // Relies on mTempRect, MUST be called first!
        final int thumbOffset = getThumbOffset();

        mThumbDrawable.getPadding(mTempRect);
        final int thumbTop = mSwitchTop - mTouchSlop;
        final int thumbLeft = mSwitchLeft + thumbOffset - mTouchSlop;
        final int thumbRight = thumbLeft + mThumbWidth +
                mTempRect.left + mTempRect.right + mTouchSlop;
        final int thumbBottom = mSwitchBottom + mTouchSlop;
        return x > thumbLeft && x < thumbRight && y > thumbTop && y < thumbBottom;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mVelocityTracker.addMovement(ev);
        final int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mTouchDown = true;
                final float x = ev.getX();
                final float y = ev.getY();
                if (isEnabled() && hitThumb(x, y)) {
                    mTouchMode = TOUCH_MODE_DOWN;
                    mTouchX = x;
                    mTouchY = y;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                switch (mTouchMode) {
                    case TOUCH_MODE_IDLE: {
                        // Didn't target the thumb, treat normally.
                        break;
                    }
                    case TOUCH_MODE_DOWN: {
                        mTouchDown = false;
                        final float x = ev.getX();
                        final float y = ev.getY();
                        if (Math.abs(x - mTouchX) > mTouchSlop ||
                                Math.abs(y - mTouchY) > mTouchSlop) {
                            mTouchMode = TOUCH_MODE_DRAGGING;
                            getParent().requestDisallowInterceptTouchEvent(true);
                            mTouchX = x;
                            mTouchY = y;
                            return true;
                        }
                        break;
                    }

                    case TOUCH_MODE_DRAGGING: {
                        mTouchDown = false;
                        final float x = ev.getX();
                        final int thumbScrollRange = getThumbScrollRange();
                        final float thumbScrollOffset = x - mTouchX;
                        float dPos;
                        if (thumbScrollRange != 0) {
                            dPos = thumbScrollOffset / thumbScrollRange;
                        } else {
                            // If the thumb scroll range is empty, just use the
                            // movement direction to snap on or off.
                            dPos = thumbScrollOffset > 0 ? 1 : -1;
                        }
                        if (ViewUtils.isLayoutRtl(this)) {
                            dPos = -dPos;
                        }
                        final float newPos = constrain(mThumbPosition + dPos, 0, 1);
                        if (newPos != mThumbPosition) {
                            mTouchX = x;
                            setThumbPosition(newPos);
                        }
                        return true;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                final boolean touchedDown = mTouchDown;
                mTouchDown = false;
                if (mTouchMode == TOUCH_MODE_DRAGGING) {
                    stopDrag(ev);
                    // Allow super class to handle pressed state, etc.
                    super.onTouchEvent(ev);
                    return true;
                } else if (touchedDown && isEnabled()) {
                    // Handle click event
                    final float x = ev.getX();

                    final int thumbScrollStart = mSwitchLeft + mThumbWidth - mThumbWidth / 2;
                    final int thumbScrollRange = mSwitchRight - mSwitchLeft - mThumbWidth;
                    float thumbPos = 0;
                    if (thumbScrollRange != 0) {
                        thumbPos = constrain((x - thumbScrollStart) / thumbScrollRange, 0, 1);
                    }
                    if (ViewUtils.isLayoutRtl(this)) {
                        thumbPos = 1 - thumbPos;
                    }
                    final int newCheckedPosition = getCheckedPosition(constrain(thumbPos, 0, 1));
                    if (newCheckedPosition != getCheckedPosition(mThumbPosition)) {
                        setCheckedPosition(newCheckedPosition);
                    }
                }
                mTouchMode = TOUCH_MODE_IDLE;
                mVelocityTracker.clear();
                break;
            }
        }

        return super.onTouchEvent(ev);
    }

    private void cancelSuperTouch(MotionEvent ev) {
        MotionEvent cancel = MotionEvent.obtain(ev);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        super.onTouchEvent(cancel);
        cancel.recycle();
    }

    /**
     * Called from onTouchEvent to end a drag operation.
     *
     * @param ev Event that triggered the end of drag mode - ACTION_UP or ACTION_CANCEL
     */
    private void stopDrag(MotionEvent ev) {
        mTouchMode = TOUCH_MODE_IDLE;

        // Commit the change if the event is up and not canceled and the switch
        // has not been disabled during the drag.
        final boolean commitChange = ev.getAction() == MotionEvent.ACTION_UP && isEnabled();
        final int oldState = getCheckedPosition();
        final int newState;
        if (commitChange) {
            mVelocityTracker.computeCurrentVelocity(1000);
            final float xvel = mVelocityTracker.getXVelocity();
            if (Math.abs(xvel) > mMinFlingVelocity) {
                int diff = Math.round(xvel / (ViewUtils.isLayoutRtl(this) ? -Math.abs(xvel) : Math.abs(xvel)));
                newState = Math.min(getMax() - 1, Math.max(0, getCheckedPosition() + diff));
            } else {
                newState = getCheckedPosition();
            }
        } else {
            newState = oldState;
        }

        if (newState != oldState) {
            playSoundEffect(SoundEffectConstants.CLICK);
        }
        // Always call setChecked so that the thumb is moved back to the correct edge
        setCheckedPosition(newState);
        cancelSuperTouch(ev);
    }

    private void animateThumbToCheckedPosition(final int newCheckedPosition) {
        mTargetCheckedPosition = newCheckedPosition;
        if (mPositionAnimator != null) {
            // If there's a current animator running, cancel it
            cancelPositionAnimator();
        }

        mPositionAnimator = new ThumbAnimation(mThumbPosition, getThumbPosition(newCheckedPosition));
        mPositionAnimator.setDuration(THUMB_ANIMATION_DURATION);
        mPositionAnimator.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                refreshDrawableState();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mPositionAnimator == animation) {
                    // If we're still the active animation, ensure the final position
                    setThumbPosition(getThumbPosition(newCheckedPosition));
                    mPositionAnimator = null;
                    if (mOnCheckedChangeListener != null) {
                        mOnCheckedChangeListener.onCheckedChange(newCheckedPosition);
                    }
                    mTargetCheckedPosition = -1;
                    refreshDrawableState();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        startAnimation(mPositionAnimator);
    }

    private float getThumbPosition(int checkedPosition) {
        return constrain(checkedPosition / (float) (getMax() - 1), 0, 1);
    }

    private int getCheckedPosition(float thumbPosition) {
        return Math.round(constrain(thumbPosition, 0, 1) * (getMax() - 1));
    }

    private void cancelPositionAnimator() {
        if (mPositionAnimator != null) {
            clearAnimation();
            mPositionAnimator = null;
        }
    }

    public void setCheckedPosition(int checkedPosition) {
        if (getWindowToken() != null && ViewCompat.isLaidOut(this) && isShown()) {
            animateThumbToCheckedPosition(checkedPosition);
        } else {
            mTargetCheckedPosition = -1;
            final int oldPosition = getCheckedPosition();
            // Immediately move the thumb to the new position.
            cancelPositionAnimator();
            setThumbPosition(getThumbPosition(checkedPosition));
            if (oldPosition != checkedPosition) {
                if (mOnCheckedChangeListener != null) {
                    mOnCheckedChangeListener.onCheckedChange(checkedPosition);
                }
            }
        }
        refreshDrawableState();
    }

    public int getCheckedPosition() {
        return getCheckedPosition(mThumbPosition);
    }

    /**
     * Sets the thumb position as a decimal value between 0 (off) and 1 (on).
     *
     * @param thumbPosition new position between [0,1]
     */
    private void setThumbPosition(float thumbPosition) {
        mThumbPosition = thumbPosition;
        if (mOnCheckedOffsetChangeListener != null) {
            final float positionOffset = constrain(thumbPosition, 0, 1) * (getMax() - 1);
            mOnCheckedOffsetChangeListener.onThumbPositionChange(positionOffset);
        }
        invalidate();
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int opticalInsetLeft = 0;
        int opticalInsetRight = 0;
        if (mThumbDrawable != null) {
            final Rect trackPadding = mTempRect;
            if (mTrackDrawable != null) {
                mTrackDrawable.getPadding(trackPadding);
            } else {
                trackPadding.setEmpty();
            }

            final Rect insets = MVS_DrawableUtilsAccessor.getOpticalBounds(mThumbDrawable);
            opticalInsetLeft = Math.max(0, insets.left - trackPadding.left);
            opticalInsetRight = Math.max(0, insets.right - trackPadding.right);
        }

        final int switchRight;
        final int switchLeft;
        if (ViewUtils.isLayoutRtl(this)) {
            switchLeft = getPaddingLeft() + opticalInsetLeft;
            switchRight = switchLeft + mSwitchWidth - opticalInsetLeft - opticalInsetRight;
        } else {
            switchRight = getWidth() - getPaddingRight() - opticalInsetRight;
            switchLeft = switchRight - mSwitchWidth + opticalInsetLeft + opticalInsetRight;
        }

        final int switchTop = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2 -
                mSwitchHeight / 2;
        final int switchBottom = switchTop + mSwitchHeight;

        mSwitchLeft = switchLeft;
        mSwitchTop = switchTop;
        mSwitchBottom = switchBottom;
        mSwitchRight = switchRight;
    }

    @Override
    public void draw(Canvas c) {
        final Rect padding = mTempRect;
        final int switchLeft = mSwitchLeft;
        final int switchTop = mSwitchTop;
        final int switchRight = mSwitchRight;
        final int switchBottom = mSwitchBottom;

        int thumbInitialLeft = switchLeft + getThumbOffset();

        final Rect thumbInsets;
        if (mThumbDrawable != null) {
            thumbInsets = MVS_DrawableUtilsAccessor.getOpticalBounds(mThumbDrawable);
        } else {
            thumbInsets = MVS_DrawableUtilsAccessor.INSETS_NONE;
        }

        // Layout the track.
        if (mTrackDrawable != null) {
            mTrackDrawable.getPadding(padding);

            // Adjust thumb position for track padding.
            thumbInitialLeft += padding.left;

            // If necessary, offset by the optical insets of the thumb asset.
            int trackLeft = switchLeft;
            int trackTop = switchTop;
            int trackRight = switchRight;
            int trackBottom = switchBottom;
            if (thumbInsets != null) {
                if (thumbInsets.left > padding.left) {
                    trackLeft += thumbInsets.left - padding.left;
                }
                if (thumbInsets.top > padding.top) {
                    trackTop += thumbInsets.top - padding.top;
                }
                if (thumbInsets.right > padding.right) {
                    trackRight -= thumbInsets.right - padding.right;
                }
                if (thumbInsets.bottom > padding.bottom) {
                    trackBottom -= thumbInsets.bottom - padding.bottom;
                }
            }
            mTrackDrawable.setBounds(trackLeft, trackTop, trackRight, trackBottom);
        }

        // Layout the thumb.
        if (mThumbDrawable != null) {
            mThumbDrawable.getPadding(padding);

            final int thumbLeft = thumbInitialLeft - padding.left;
            final int thumbRight = thumbInitialLeft + mThumbWidth + padding.right;
            mThumbDrawable.setBounds(thumbLeft, switchTop, thumbRight, switchBottom);

            final Drawable background = getBackground();
            if (background != null) {
                DrawableCompat.setHotspotBounds(background, thumbLeft, switchTop,
                        thumbRight, switchBottom);
            }
        }

        // Draw the background.
        super.draw(c);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final Rect padding = mTempRect;
        final Drawable trackDrawable = mTrackDrawable;
        if (trackDrawable != null) {
            trackDrawable.getPadding(padding);
        } else {
            padding.setEmpty();
        }

        final Drawable thumbDrawable = mThumbDrawable;
        if (trackDrawable != null) {
            if (mSplitTrack && thumbDrawable != null) {
                final Rect insets = MVS_DrawableUtilsAccessor.getOpticalBounds(thumbDrawable);
                thumbDrawable.copyBounds(padding);
                padding.left += insets.left;
                padding.right -= insets.right;

                final int saveCount = canvas.save();
                canvas.clipRect(padding, Region.Op.DIFFERENCE);
                trackDrawable.draw(canvas);
                canvas.restoreToCount(saveCount);
            } else {
                trackDrawable.draw(canvas);
            }

            final int thumbScrollStart = mSwitchLeft + mThumbWidth - mThumbWidth / 2;
            final int thumbScrollRange = mSwitchRight - mSwitchLeft - mThumbWidth;
            final int y = trackDrawable.getBounds().centerY();
            for (int i = 0, j = getMax(); i < j; i++) {
                if (i == 0 || i == j - 1) continue;

                canvas.drawCircle(thumbScrollStart + i * (thumbScrollRange / (j - 1)), y,
                        mPointRadius, mPointPaint);
            }
        }

        final int saveCount = canvas.save();

        if (thumbDrawable != null) {
            thumbDrawable.draw(canvas);
        }

        canvas.restoreToCount(saveCount);

    }

    /**
     * Translates thumb position to offset according to current RTL setting and
     * thumb scroll range. Accounts for both track and thumb padding.
     *
     * @return thumb offset
     */
    private int getThumbOffset() {
        final float thumbPosition;
        if (ViewUtils.isLayoutRtl(this)) {
            thumbPosition = 1 - mThumbPosition;
        } else {
            thumbPosition = mThumbPosition;
        }
        return (int) (thumbPosition * getThumbScrollRange() + 0.5f);
    }

    private int getThumbScrollRange() {
        if (mTrackDrawable != null) {
            final Rect padding = mTempRect;
            mTrackDrawable.getPadding(padding);

            final Rect insets;
            if (mThumbDrawable != null) {
                insets = MVS_DrawableUtilsAccessor.getOpticalBounds(mThumbDrawable);
            } else {
                insets = MVS_DrawableUtilsAccessor.INSETS_NONE;
            }

            return mSwitchWidth - mThumbWidth - padding.left - padding.right
                    - insets.left - insets.right;
        } else {
            return 0;
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isInCheckedState()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.applySupportBackgroundTint();
        }

        final int[] myDrawableState = getDrawableState();

        if (mThumbDrawable != null) {
            mThumbDrawable.setState(myDrawableState);
        }

        if (mTrackDrawable != null) {
            mTrackDrawable.setState(myDrawableState);
        }

        invalidate();
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.drawableHotspotChanged(x, y);
        }

        if (mThumbDrawable != null) {
            DrawableCompat.setHotspot(mThumbDrawable, x, y);
        }

        if (mTrackDrawable != null) {
            DrawableCompat.setHotspot(mTrackDrawable, x, y);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mThumbDrawable || who == mTrackDrawable;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        if (Build.VERSION.SDK_INT >= 11) {
            super.jumpDrawablesToCurrentState();

            if (mThumbDrawable != null) {
                mThumbDrawable.jumpToCurrentState();
            }

            if (mTrackDrawable != null) {
                mTrackDrawable.jumpToCurrentState();
            }

            cancelPositionAnimator();
            setThumbPosition(getThumbPosition(getCheckedPosition()));
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(ACCESSIBILITY_EVENT_CLASS_NAME);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName(ACCESSIBILITY_EVENT_CLASS_NAME);
            CharSequence switchText = getEntry(getCheckedPosition());
            if (!TextUtils.isEmpty(switchText)) {
                CharSequence oldText = info.getText();
                if (TextUtils.isEmpty(oldText)) {
                    info.setText(switchText);
                } else {
                    StringBuilder newText = new StringBuilder();
                    newText.append(oldText).append(' ').append(switchText);
                    info.setText(newText);
                }
            }
        }
    }

    private boolean isInCheckedState() {
        final int[] highlightPositions = mHighlightCheckedPositions;
        if (highlightPositions == null) return false;
        int checkedPosition = mTargetCheckedPosition;
        if (checkedPosition < 0) {
            checkedPosition = getCheckedPosition();
        }
        for (final int highlightPosition : highlightPositions) {
            if (checkedPosition == highlightPosition) return true;
        }
        return false;
    }

    private CharSequence getEntry(int position) {
        if (mEntries == null) return null;
        return mEntries[position];
    }

    /**
     * Taken from android.util.MathUtils
     */
    private static float constrain(float amount, float low, float high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(@IntRange(from = 2) int max) {
        mMax = max;
        requestLayout();
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
        mOnCheckedChangeListener = onCheckedChangeListener;
    }

    public void setOnThumbOffsetChangeListener(OnThumbPositionChangeListener onCheckedOffsetChangeListener) {
        mOnCheckedOffsetChangeListener = onCheckedOffsetChangeListener;
    }

    public interface OnCheckedChangeListener {
        void onCheckedChange(int position);
    }

    public interface OnThumbPositionChangeListener {
        void onThumbPositionChange(float positionOffset);
    }

    private class ThumbAnimation extends Animation {
        final float mStartPosition;
        final float mEndPosition;
        final float mDiff;

        private ThumbAnimation(float startPosition, float endPosition) {
            mStartPosition = startPosition;
            mEndPosition = endPosition;
            mDiff = endPosition - startPosition;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            setThumbPosition(mStartPosition + (mDiff * interpolatedTime));
        }
    }
}