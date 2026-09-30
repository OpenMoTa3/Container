package org.dev.custom.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import org.dev.custom.databinding.ActivityMainBinding;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import org.dev.custom.R;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding amb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem arg0) {
        int id = arg0.getItemId();
        if (id == R.id.settings) startActivity(new Intent(this, SettingsActivity.class));
        return super.onOptionsItemSelected(arg0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        amb = null;
    }

    public void init() {
        amb = ActivityMainBinding.inflate(getLayoutInflater());
        setSupportActionBar(amb.toolbar);
        setContentView(amb.getRoot());
        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder(R.id.navigation_home, R.id.navigation_termux)
                        .build();
        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(amb.navView, navController);
    }
}
