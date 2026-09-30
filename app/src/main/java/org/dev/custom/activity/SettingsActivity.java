package org.dev.custom.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import org.dev.custom.R;
import org.dev.custom.databinding.ActivityMainBinding;
import org.dev.custom.databinding.ActivitySettingsBinding;
public class SettingsActivity extends AppCompatActivity {
    ActivitySettingsBinding asb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        asb=null;
    }
    public void init() {
    	asb = ActivitySettingsBinding.inflate(getLayoutInflater());
        setSupportActionBar(asb.toolbar);
        setContentView(asb.getRoot());
   
    }
}
