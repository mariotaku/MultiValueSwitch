package android.support.v7.widget;

import android.content.Context;

/**
 * Created by mariotaku on 16/4/30.
 */
public class ThemeUtilsAccessor {


    public static final int[] DISABLED_STATE_SET = ThemeUtils.DISABLED_STATE_SET;
    public static final int[] FOCUSED_STATE_SET = ThemeUtils.FOCUSED_STATE_SET;
    public static final int[] ACTIVATED_STATE_SET = ThemeUtils.ACTIVATED_STATE_SET;
    public static final int[] PRESSED_STATE_SET = ThemeUtils.PRESSED_STATE_SET;
    public static final int[] CHECKED_STATE_SET = ThemeUtils.CHECKED_STATE_SET;
    public static final int[] SELECTED_STATE_SET = ThemeUtils.SELECTED_STATE_SET;
    public static final int[] NOT_PRESSED_OR_FOCUSED_STATE_SET = ThemeUtils.NOT_PRESSED_OR_FOCUSED_STATE_SET;
    public static final int[] EMPTY_STATE_SET = ThemeUtils.EMPTY_STATE_SET;

    public static int getThemeAttrColor(Context context, int attr, float alpha) {
        return ThemeUtils.getThemeAttrColor(context, attr, alpha);
    }

    public static int getDisabledThemeAttrColor(Context context, int attr) {
        return ThemeUtils.getDisabledThemeAttrColor(context, attr);
    }
}
