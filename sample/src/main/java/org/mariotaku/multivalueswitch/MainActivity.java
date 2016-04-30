package org.mariotaku.multivalueswitch;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ThemeUtilsAccessor;
import android.view.View;
import android.widget.Toast;

import com.nineoldandroids.animation.ArgbEvaluator;

import org.mariotaku.multivalueswitch.library.MultiValueSwitch;

public class MainActivity extends AppCompatActivity {

    int[] PRESET_COLORS = {R.color.material_red, R.color.material_pink,
            R.color.material_purple, R.color.material_deep_purple, R.color.material_indigo,
            R.color.material_blue, R.color.material_light_blue, R.color.material_cyan,
            R.color.material_teal, R.color.material_green, R.color.material_light_green,
            R.color.material_lime, R.color.material_yellow, R.color.material_amber,
            R.color.material_orange, R.color.material_deep_orange};

    private MultiValueSwitch mMultiValueSwitch;
    private View mChangeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMultiValueSwitch.setOnCheckedChangeListener(new MultiValueSwitch.OnCheckedChangeListener() {

            @Override
            public void onCheckedChange(int position) {
                Toast.makeText(getApplicationContext(),
                        String.valueOf(mMultiValueSwitch.getCheckedPosition()), Toast.LENGTH_SHORT).show();
            }
        });
        mMultiValueSwitch.setOnThumbOffsetChangeListener(new MultiValueSwitch.OnThumbPositionChangeListener() {
            ArgbEvaluator mEvaluator = new ArgbEvaluator();

            @Override
            public void onThumbPositionChange(float positionOffset) {
                int[] colors = (int[]) mMultiValueSwitch.getTag();
                if (colors == null) return;
                final MainActivity context = MainActivity.this;
                float itemOffset = positionOffset % 1;
                int position = (int) (positionOffset - itemOffset);
                final int randColor;
                if (position + 1 >= colors.length) {
                    randColor = colors[position];
                } else {
                    randColor = (int) mEvaluator.evaluate(itemOffset, colors[position], colors[position + 1]);
                }
                mMultiValueSwitch.setThumbTintList(ColorStateList.valueOf(randColor));
                mMultiValueSwitch.setTrackTintList(createSwitchTrackColorStateList(randColor));
                ViewCompat.setBackgroundTintList(mMultiValueSwitch, createButtonColorStateList(Color.TRANSPARENT, randColor));
            }
        });
        mChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int max = (int) (2 + Math.random() * 5);
                mMultiValueSwitch.setMax(max);
                int arrayStart = (int) (Math.random() * PRESET_COLORS.length), arrayEnd = arrayStart + max;
                int[] colors = new int[max];
                final MainActivity context = MainActivity.this;
                for (int i = 0; i < max; i++) {
                    colors[i] = ContextCompat.getColor(context, PRESET_COLORS[(arrayStart + i)
                            % PRESET_COLORS.length]);
                }
                int pos = mMultiValueSwitch.getCheckedPosition();
                final int randColor = colors[pos];
                mMultiValueSwitch.setTag(colors);
                mMultiValueSwitch.setThumbTintList(ColorStateList.valueOf(randColor));
                mMultiValueSwitch.setTrackTintList(createSwitchTrackColorStateList(randColor));
            }
        });
    }


    private ColorStateList createSwitchTrackColorStateList(@ColorInt int tintColor) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtilsAccessor.DISABLED_STATE_SET;
        colors[i] = ThemeUtilsAccessor.getThemeAttrColor(this, android.R.attr.colorForeground, 0.1f);
        i++;

        states[i] = ThemeUtilsAccessor.CHECKED_STATE_SET;
        colors[i] = ColorUtils.setAlphaComponent(tintColor, Math.round(Color.alpha(tintColor) * 0.3f));
        i++;

        // Default enabled state
        states[i] = ThemeUtilsAccessor.EMPTY_STATE_SET;
        colors[i] = ThemeUtilsAccessor.getThemeAttrColor(this, android.R.attr.colorForeground, 0.3f);

        return new ColorStateList(states, colors);
    }


    private ColorStateList createButtonColorStateList(@ColorInt int baseColor, @ColorInt int tintColor) {
        final int[][] states = new int[4][];
        final int[] colors = new int[4];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtilsAccessor.DISABLED_STATE_SET;
        colors[i] = ThemeUtilsAccessor.getDisabledThemeAttrColor(this, android.support.v7.appcompat.R.attr.colorButtonNormal);
        i++;

        states[i] = ThemeUtilsAccessor.PRESSED_STATE_SET;
        colors[i] = ColorUtils.compositeColors(tintColor, baseColor);
        i++;

        states[i] = ThemeUtilsAccessor.FOCUSED_STATE_SET;
        colors[i] = ColorUtils.compositeColors(tintColor, baseColor);
        i++;

        // Default enabled state
        states[i] = ThemeUtilsAccessor.EMPTY_STATE_SET;
        colors[i] = baseColor;

        return new ColorStateList(states, colors);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mMultiValueSwitch = ((MultiValueSwitch) findViewById(R.id.mvs));
        mChangeButton = findViewById(R.id.change);
    }
}
