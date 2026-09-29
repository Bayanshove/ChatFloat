package com.hwcloud.chatfloat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LanguageUtils {

    private static final String PREFS_NAME = "language";
    private static final String KEY_LANGUAGE = "selected_language";

    // Language codes
    public static final String LANG_SYSTEM = "system";
    public static final String LANG_EN = "en";
    public static final String LANG_ZH_CN = "zh-rCN";
    public static final String LANG_ZH_TW = "zh-rTW";

    /**
     * Apply language to context
     */
    public static Context applyLanguage(Context context) {
        String lang = getSavedLanguage(context);
        Locale locale = getLocale(lang);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

    /**
     * Get Locale from language code
     */
    private static Locale getLocale(String lang) {
        switch (lang) {
            case LANG_EN:
                return Locale.ENGLISH;
            case LANG_ZH_CN:
                return Locale.SIMPLIFIED_CHINESE;
            case LANG_ZH_TW:
                return Locale.TRADITIONAL_CHINESE;
            case LANG_SYSTEM:
            default:
                return Locale.getDefault();
        }
    }

    /**
     * Get saved language preference
     */
    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANG_SYSTEM);
    }

    /**
     * Save language preference
     */
    public static void saveLanguage(Context context, String lang) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, lang).apply();
    }

    /**
     * Get language display name
     */
    public static String getLanguageDisplayName(Context context, String lang) {
        switch (lang) {
            case LANG_SYSTEM:
                return context.getString(R.string.language_system);
            case LANG_EN:
                return context.getString(R.string.language_en);
            case LANG_ZH_CN:
                return context.getString(R.string.language_zh_cn);
            case LANG_ZH_TW:
                return context.getString(R.string.language_zh_tw);
            default:
                return lang;
        }
    }

    /**
     * Get all available languages
     */
    public static String[] getAvailableLanguages() {
        return new String[]{LANG_SYSTEM, LANG_EN, LANG_ZH_CN, LANG_ZH_TW};
    }
}