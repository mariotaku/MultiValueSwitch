package android.support.v7.widget;

import android.content.Context;

/**
 * Created by mariotaku on 16/4/30.
 */
public class MVS_TintContextWrapperAccessor {
    public static Context wrap(Context context) {
        return TintContextWrapper.wrap(context);
    }
}
