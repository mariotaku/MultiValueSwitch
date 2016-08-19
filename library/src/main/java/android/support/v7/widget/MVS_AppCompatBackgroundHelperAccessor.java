package android.support.v7.widget;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by mariotaku on 16/4/30.
 */
public class MVS_AppCompatBackgroundHelperAccessor {
    private final AppCompatBackgroundHelper mHelper;

    public MVS_AppCompatBackgroundHelperAccessor(View view) {
        mHelper = new AppCompatBackgroundHelper(view);
    }

    public void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        mHelper.loadFromAttributes(attrs, defStyleAttr);
    }

    public void setSupportBackgroundTintList(ColorStateList tint) {
        mHelper.setSupportBackgroundTintList(tint);
    }

    public PorterDuff.Mode getSupportBackgroundTintMode() {
        return mHelper.getSupportBackgroundTintMode();
    }

    public void applySupportBackgroundTint() {
        mHelper.applySupportBackgroundTint();
    }

    public void onSetBackgroundResource(int resId) {
        mHelper.onSetBackgroundResource(resId);
    }

    public void setSupportBackgroundTintMode(PorterDuff.Mode tintMode) {
        mHelper.setSupportBackgroundTintMode(tintMode);
    }

    public ColorStateList getSupportBackgroundTintList() {
        return mHelper.getSupportBackgroundTintList();
    }

    public void onSetBackgroundDrawable(Drawable background) {
        mHelper.onSetBackgroundDrawable(background);
    }

    public void setInternalBackgroundTint(ColorStateList tint) {
        mHelper.setInternalBackgroundTint(tint);
    }
}
