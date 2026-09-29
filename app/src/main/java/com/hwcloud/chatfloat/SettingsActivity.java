package com.hwcloud.chatfloat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends Activity {

    private EditText etBase, etKey, etModel, etPrompt;
    private Spinner modelSpinner, languageSpinner;
    private ArrayAdapter<String> modelAdapter;
    private ArrayAdapter<String> languageAdapter;
    private TextView tvStatus;
    private SharedPreferences prefs;
    private boolean suppressSpinner = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageUtils.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("cfg", Context.MODE_PRIVATE);
        Logger.init(this);
        boolean logOn = prefs.getBoolean("logging_enabled", true);
        Logger.get().setEnabled(logOn);
        Logger.i("UI", "SettingsActivity onCreate, logging=" + logOn);
        buildUi();
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void buildUi() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        sv.addView(root);
        setContentView(sv);

        // Title
        TextView title = new TextView(this);
        title.setText(R.string.settings_title);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF0F766E);
        root.addView(title);
        root.addView(smallText(R.string.settings_desc));

        // --- Language ---
        root.addView(label(R.string.language_title));
        languageSpinner = new Spinner(this);
        String[] languages = LanguageUtils.getAvailableLanguages();
        languageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<String>());
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String lang : languages) {
            languageAdapter.add(LanguageUtils.getLanguageDisplayName(this, lang));
        }
        languageSpinner.setAdapter(languageAdapter);
        String savedLang = LanguageUtils.getSavedLanguage(this);
        int langIdx = 0;
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(savedLang)) { langIdx = i; break; }
        }
        languageSpinner.setSelection(langIdx);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLang = languages[position];
                if (!selectedLang.equals(savedLang)) {
                    LanguageUtils.saveLanguage(SettingsActivity.this, selectedLang);
                    recreate();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        root.addView(languageSpinner, match());

        // --- API settings ---
        root.addView(label(R.string.api_url));
        etBase = new EditText(this);
        etBase.setHint("https://api.deepseek.com");
        etBase.setSingleLine(true);
        root.addView(etBase, match());

        root.addView(label(R.string.api_key));
        etKey = new EditText(this);
        etKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        etKey.setSingleLine(true);
        root.addView(etKey, match());

        root.addView(label(R.string.model_name));
        LinearLayout modelRow = new LinearLayout(this);
        modelRow.setOrientation(LinearLayout.HORIZONTAL);
        modelRow.setGravity(Gravity.CENTER_VERTICAL);
        modelSpinner = new Spinner(this);
        modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<String>());
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelAdapter.add(getString(R.string.model_hint));
        modelSpinner.setAdapter(modelAdapter);
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinner) return;
                String m = modelAdapter.getItem(position);
                if (m != null && !m.startsWith(getString(R.string.model_hint).substring(0, 1))) etModel.setText(m);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        modelRow.addView(modelSpinner, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button btnFetch = new Button(this);
        btnFetch.setText(R.string.model_fetch);
        btnFetch.setOnClickListener(v -> fetchModels());
        modelRow.addView(btnFetch, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(modelRow, match());

        etModel = new EditText(this);
        etModel.setHint(R.string.model_hint);
        etModel.setSingleLine(true);
        root.addView(etModel, match());

        root.addView(label(R.string.system_prompt));
        etPrompt = new EditText(this);
        etPrompt.setMinLines(6);
        etPrompt.setMaxLines(10);
        etPrompt.setGravity(Gravity.TOP);
        etPrompt.setTextSize(13);
        root.addView(etPrompt, match());

        // --- Action buttons ---
        Button btnSave = new Button(this);
        btnSave.setText(R.string.save);
        btnSave.setOnClickListener(v -> save());
        root.addView(btnSave);

        Button btnTest = new Button(this);
        btnTest.setText(R.string.test);
        btnTest.setOnClickListener(v -> testApi());
        root.addView(btnTest);

        Button btnStart = new Button(this);
        btnStart.setText(R.string.start);
        btnStart.setOnClickListener(v -> startBubble());
        root.addView(btnStart);

        tvStatus = new TextView(this);
        tvStatus.setTextSize(13);
        tvStatus.setPadding(0, dp(8), 0, 0);
        root.addView(tvStatus, match());

        // --- Logging ---
        root.addView(label(R.string.log_title));
        CheckBox cbLogEnabled = new CheckBox(this);
        cbLogEnabled.setText(R.string.log_enabled);
        cbLogEnabled.setChecked(prefs.getBoolean("logging_enabled", true));
        cbLogEnabled.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("logging_enabled", checked).apply();
            Logger.get().setEnabled(checked);
            Logger.i("LOG", "logging " + (checked ? "enabled" : "disabled"));
        });
        root.addView(cbLogEnabled);

        CheckBox cbLogAutoStart = new CheckBox(this);
        cbLogAutoStart.setText(R.string.log_auto_start);
        cbLogAutoStart.setChecked(prefs.getBoolean("logging_auto_start", true));
        cbLogAutoStart.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("logging_auto_start", checked).apply();
        });
        root.addView(cbLogAutoStart);

        LinearLayout logBtnRow = new LinearLayout(this);
        logBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        Button btnExportLog = new Button(this);
        btnExportLog.setText(R.string.log_export);
        btnExportLog.setOnClickListener(v -> exportLog());
        logBtnRow.addView(btnExportLog, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button btnClearLog = new Button(this);
        btnClearLog.setText(R.string.log_clear);
        btnClearLog.setOnClickListener(v -> clearLog());
        logBtnRow.addView(btnClearLog, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(logBtnRow, match());

        // --- Usage ---
        root.addView(label(R.string.usage_title));
        root.addView(smallText(R.string.usage_text));

        // Load saved
        etBase.setText(prefs.getString("base", ""));
        etKey.setText(prefs.getString("key", ""));
        etModel.setText(prefs.getString("model", ""));
        etPrompt.setText(prefs.getString("prompt", ApiClient.DEFAULT_PROMPT));
    }

    private void fetchModels() {
        save();
        final String base = prefs.getString("base", "");
        final String key = prefs.getString("key", "");
        if (base.isEmpty()) { tvStatus.setText(getString(R.string.toast_permission)); return; }
        tvStatus.setText(getString(R.string.status_fetching));
        new Thread(() -> {
            try {
                final List<String> list = ApiClient.fetchModels(base, key);
                runOnUiThread(() -> {
                    if (list.isEmpty()) { tvStatus.setText(getString(R.string.status_empty)); return; }
                    String saved = etModel.getText().toString().trim();
                    List<String> shown = new ArrayList<>(list);
                    if (!saved.isEmpty() && !shown.contains(saved)) shown.add(0, saved);
                    suppressSpinner = true;
                    modelAdapter.clear();
                    modelAdapter.addAll(shown);
                    int idx = shown.indexOf(saved);
                    modelSpinner.setSelection(idx >= 0 ? idx : 0);
                    suppressSpinner = false;
                    tvStatus.setText(String.format(Locale.US, getString(R.string.status_success), list.size()));
                });
            } catch (final Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                runOnUiThread(() -> tvStatus.setText(String.format(getString(R.string.status_error), msg)));
            }
        }).start();
    }

    private void save() {
        prefs.edit()
                .putString("base", etBase.getText().toString().trim())
                .putString("key", etKey.getText().toString().trim())
                .putString("model", etModel.getText().toString().trim())
                .putString("prompt", etPrompt.getText().toString())
                .apply();
        Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
    }

    private void testApi() {
        save();
        final String base = prefs.getString("base", "");
        final String key = prefs.getString("key", "");
        final String model = prefs.getString("model", "");
        final String prompt = prefs.getString("prompt", ApiClient.DEFAULT_PROMPT);
        tvStatus.setText(getString(R.string.status_test));
        new Thread(() -> {
            try {
                final String out = ApiClient.chat(base, key, model, prompt,
                        "【譯】\nHello! How are you doing today?", 30);
                runOnUiThread(() -> tvStatus.setText(getString(R.string.status_done) + "\n" + out));
            } catch (final Exception e) {
                runOnUiThread(() -> tvStatus.setText(String.format(getString(R.string.status_error),
                        e.getMessage() == null ? e.toString() : e.getMessage())));
            }
        }).start();
    }

    private void startBubble() {
        save();
        if (etBase.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.toast_permission, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.permission_overlay, Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            }
            return;
        }
        Intent it = new Intent(this, BubbleService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(it);
        else startService(it);
        Toast.makeText(this, R.string.toast_started, Toast.LENGTH_LONG).show();
    }

    private void exportLog() {
        File f = Logger.get().export();
        if (f != null && f.exists()) {
            Toast.makeText(this, R.string.log_exported, Toast.LENGTH_LONG).show();
            Logger.i("LOG", "exported to " + f.getAbsolutePath());
        } else {
            Toast.makeText(this, R.string.toast_log_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearLog() {
        Logger.get().clear();
        Toast.makeText(this, R.string.log_cleared, Toast.LENGTH_SHORT).show();
    }

    private TextView label(int resId) {
        TextView tv = new TextView(this);
        tv.setText(resId);
        tv.setTextSize(13);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(14), 0, dp(4));
        return tv;
    }

    private TextView smallText(int resId) {
        TextView tv = new TextView(this);
        tv.setText(resId);
        tv.setTextSize(12);
        tv.setTextColor(0xFF6B7280);
        return tv;
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}