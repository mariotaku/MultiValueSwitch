package android.support.v7.widget;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Created by mariotaku on 16/3/23.
 */
public class MVS_DrawableUtilsAccessor {

    public static final Rect INSETS_NONE = DrawableUtils.INSETS_NONE;

    public static Rect getOpticalBounds(Drawable drawable) {
        return DrawableUtils.getOpticalBounds(drawable);
    }
}
