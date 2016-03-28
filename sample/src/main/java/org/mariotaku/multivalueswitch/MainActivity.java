package org.mariotaku.multivalueswitch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
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
                Toast.makeText(getApplicationContext(),
                        String.valueOf(mMultiValueSwitch.getCheckedPosition()), Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.change).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMultiValueSwitch.setMax((int) (1 + Math.random() * 9));
            }
        });
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mMultiValueSwitch = ((MultiValueSwitch) findViewById(R.id.mvs));
    }
}
