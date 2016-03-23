package org.mariotaku.multivalueswitch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.mariotaku.multivalueswitch.library.MultiValueSwitch;

public class MainActivity extends AppCompatActivity {

    private MultiValueSwitch mMultiValueSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMultiValueSwitch.setOnCheckedChangeListener(new MultiValueSwitch.OnCheckedChangeListener() {

            @Override
            public void onCheckedChange(int position) {
                Toast.makeText(getApplicationContext(), mMultiValueSwitch.getValue(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mMultiValueSwitch = ((MultiValueSwitch) findViewById(R.id.mvs));
    }
}
