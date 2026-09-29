package com.hwcloud.chatfloat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.app.Activity;

public class MainActivity extends Activity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply language before super
        super.attachBaseContext(LanguageUtils.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if welcome page has been shown
        boolean welcomeShown = getSharedPreferences("cfg", Context.MODE_PRIVATE)
                .getBoolean("welcome_shown", false);

        if (!welcomeShown) {
            // Show welcome page first
            startActivity(new Intent(this, WelcomeActivity.class));
        } else {
            // Go directly to settings
            startActivity(new Intent(this, SettingsActivity.class));
        }

        finish();
    }
}