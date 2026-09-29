package com.hwcloud.chatfloat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.app.Activity;

public class WelcomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        // Main container
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(40), dp(24), dp(40));

        // Background gradient
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0F766E, 0xFF064E3B});
        root.setBackground(bg);

        setContentView(root);

        // Title
        TextView title = new TextView(this);
        title.setText(R.string.welcome_title);
        title.setTextSize(32);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, match());

        // Subtitle
        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.welcome_subtitle);
        subtitle.setTextSize(16);
        subtitle.setTextColor(0xFFCCFBF1);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, match());

        // Spacer
        root.addView(spacer(dp(32)));

        // Description card
        LinearLayout descCard = createCard(this, 0xFF0D9488, 0xFFCCFBF1);
        TextView desc = new TextView(this);
        desc.setText(R.string.welcome_desc);
        desc.setTextSize(14);
        desc.setTextColor(0xFFCCFBF1);
        desc.setGravity(Gravity.CENTER);
        descCard.addView(desc, match());
        root.addView(descCard, match());

        // Spacer
        root.addView(spacer(dp(24)));

        // API info card
        LinearLayout apiCard = createCard(this, 0xFF115E59, 0xFFCCFBF1);
        TextView api = new TextView(this);
        api.setText(R.string.welcome_api);
        api.setTextSize(13);
        api.setTextColor(0xFFF0FDFA);
        api.setGravity(Gravity.CENTER);
        apiCard.addView(api, match());
        root.addView(apiCard, match());

        // Spacer
        root.addView(spacer(dp(32)));

        // Privacy notice
        TextView privacy = new TextView(this);
        privacy.setText(R.string.welcome_privacy);
        privacy.setTextSize(12);
        privacy.setTextColor(0xFF99F6E4);
        privacy.setGravity(Gravity.CENTER);
        root.addView(privacy, match());

        // Spacer
        root.addView(spacer(dp(40)));

        // Start button
        Button btnStart = new Button(this);
        btnStart.setText(R.string.welcome_start);
        btnStart.setTextSize(16);
        btnStart.setTypeface(Typeface.DEFAULT_BOLD);
        btnStart.setTextColor(0xFF0F766E);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(dp(28));
        btnBg.setColor(Color.WHITE);
        btnStart.setBackground(btnBg);
        btnStart.setPadding(dp(40), dp(14), dp(40), dp(14));
        btnStart.setOnClickListener(v -> startApp());
        root.addView(btnStart, wrapCenter());
    }

    private LinearLayout createCard(Context ctx, int bgColor, int textColor) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(bgColor);
        card.setBackground(bg);
        return card;
    }

    private View spacer(int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height));
        return v;
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapCenter() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        return lp;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void startApp() {
        // Mark welcome as shown
        getSharedPreferences("cfg", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("welcome_shown", true)
                .apply();

        // Go to MainActivity (settings)
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}