package com.hwcloud.chatfloat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class BubbleService extends Service {

    // Task instruction prefixes (must match the user's customized system prompt protocol)
    private static final String PREFIX_TRANSLATE = "【譯】";
    private static final String PREFIX_REPLY = "【回】";

    private WindowManager wm;
    private FrameLayout bubble;
    private ScrollView panelScroll;
    private LinearLayout panelContent;
    private WindowManager.LayoutParams bubbleLp;
    private WindowManager.LayoutParams panelLp;
    private Handler main;
    private SharedPreferences prefs;

    private EditText etOriginal;
    private EditText etReply;
    private TextView tvThinking;
    private TextView tvResult;
    private TextView tvReplyOut;
    private TextView tvStatus;
    private LinearLayout thinkingCard;
    private LinearLayout resultCard;

    private float downRawX, downRawY;
    private int startLpX, startLpY;
    private boolean dragged;
    private int touchSlop;

    private int thinkingDots = 0;
    private Runnable thinkingRunnable;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        main = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences("cfg", Context.MODE_PRIVATE);
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        Logger.init(this);
        boolean logOn = prefs.getBoolean("logging_enabled", true);
        Logger.get().setEnabled(logOn);
        Logger.i("SVC", "BubbleService onCreate, logging=" + logOn);

        createNotification();
        addBubble();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "stop".equals(intent.getAction())) {
            Logger.i("SVC", "stop action received");
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void createNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("cf", getString(R.string.app_name) + " " + getString(R.string.welcome_subtitle), NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
            nb = new Notification.Builder(this, "cf");
        } else {
            nb = new Notification.Builder(this);
        }
        Intent stop = new Intent(this, BubbleService.class).setAction("stop");
        PendingIntent pi = PendingIntent.getService(this, 1, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        nb.setContentTitle(getString(R.string.settings_title) + " " + getString(R.string.welcome_subtitle))
                .setContentText(getString(R.string.settings_desc))
                .setSmallIcon(android.R.drawable.ic_menu_more)
                .setOngoing(true);
        nb.addAction(new Notification.Action.Builder(null, "Exit", pi).build());
        startForeground(1, nb.build());
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void addBubble() {
        TextView tv = new TextView(this);
        tv.setText(getString(R.string.welcome_title).substring(0, 1));
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(20);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xEE0F766E);
        bg.setStroke(dp(3), Color.WHITE);
        tv.setBackground(bg);

        bubble = new FrameLayout(this);
        bubble.addView(tv, new FrameLayout.LayoutParams(dp(56), dp(56)));

        bubbleLp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        bubbleLp.gravity = Gravity.TOP | Gravity.START;
        bubbleLp.x = getResources().getDisplayMetrics().widthPixels - dp(72);
        bubbleLp.y = getResources().getDisplayMetrics().heightPixels / 3;

        bubble.setOnTouchListener((v, ev) -> handleBubbleTouch(ev));
        wm.addView(bubble, bubbleLp);
        Logger.i("UI", "bubble added");
    }

    private boolean handleBubbleTouch(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = ev.getRawX();
                downRawY = ev.getRawY();
                startLpX = bubbleLp.x;
                startLpY = bubbleLp.y;
                dragged = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = ev.getRawX() - downRawX;
                float dy = ev.getRawY() - downRawY;
                if (!dragged && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) dragged = true;
                if (dragged) {
                    bubbleLp.x = startLpX + (int) dx;
                    bubbleLp.y = startLpY + (int) dy;
                    try { wm.updateViewLayout(bubble, bubbleLp); } catch (Exception ignored) {}
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragged) togglePanel();
                return true;
            default: return false;
        }
    }

    // ===== Panel =====

    private void buildPanel() {
        panelScroll = new ScrollView(this);
        panelContent = new LinearLayout(this);
        panelContent.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable pbg = new GradientDrawable();
        pbg.setCornerRadius(dp(14));
        pbg.setColor(0xFAFFFFFF);
        panelContent.setBackground(pbg);
        panelContent.setPadding(dp(12), dp(10), dp(12), dp(12));
        panelContent.setElevation(dp(6));

        panelScroll.addView(panelContent);

        // --- header ---
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText(getString(R.string.welcome_title) + " " + getString(R.string.welcome_subtitle));
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF0F766E);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button btnLog = new Button(this);
        btnLog.setText(R.string.btn_log);
        btnLog.setTextSize(12);
        btnLog.setOnClickListener(v -> copyLogToClipboard());
        header.addView(btnLog, wrap());

        Button btnClose = new Button(this);
        btnClose.setText(R.string.btn_close);
        btnClose.setTextSize(12);
        btnClose.setOnClickListener(v -> hidePanel());
        header.addView(btnClose, wrap());
        panelContent.addView(header);

        // --- translate section ---
        panelContent.addView(sectionLabel(R.string.label_opponent));
        etOriginal = new EditText(this);
        etOriginal.setMinLines(2);
        etOriginal.setMaxLines(4);
        etOriginal.setTextSize(13);
        etOriginal.setHint(R.string.bubble_hint);
        etOriginal.setFilters(createInputFilters());
        panelContent.addView(etOriginal, match());

        Button btnTranslate = new Button(this);
        btnTranslate.setText(R.string.btn_clipboard);
        btnTranslate.setOnClickListener(v -> doTranslate(true));
        panelContent.addView(btnTranslate, match());

        tvStatus = new TextView(this);
        tvStatus.setTextSize(12);
        tvStatus.setTextColor(0xFF92400E);
        tvStatus.setPadding(0, dp(4), 0, dp(2));
        panelContent.addView(tvStatus, match());

        // --- thinking card (initially hidden) ---
        panelContent.addView(sectionLabel(R.string.label_thinking));
        thinkingCard = new LinearLayout(this);
        thinkingCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable tCardBg = new GradientDrawable();
        tCardBg.setCornerRadius(dp(8));
        tCardBg.setColor(0xFFFFF7ED);
        tCardBg.setStroke(dp(1), 0xFFFED7AA);
        thinkingCard.setBackground(tCardBg);
        thinkingCard.setPadding(dp(10), dp(8), dp(10), dp(8));
        ScrollView thinkingScroll = new ScrollView(this);
        tvThinking = new TextView(this);
        tvThinking.setTextSize(13);
        tvThinking.setTextColor(0xFF78350F);
        tvThinking.setTextIsSelectable(true);
        thinkingScroll.addView(tvThinking);
        LinearLayout.LayoutParams thinkingLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(180));
        thinkingCard.addView(thinkingScroll, thinkingLp);
        thinkingCard.setVisibility(View.GONE);
        panelContent.addView(thinkingCard, match());

        // --- result card ---
        panelContent.addView(sectionLabel(R.string.label_result));
        resultCard = new LinearLayout(this);
        resultCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable rCardBg = new GradientDrawable();
        rCardBg.setCornerRadius(dp(8));
        rCardBg.setColor(0xFFF0FDFA);
        rCardBg.setStroke(dp(1), 0xFF99F6E4);
        resultCard.setBackground(rCardBg);
        resultCard.setPadding(dp(10), dp(8), dp(10), dp(8));
        tvResult = new TextView(this);
        tvResult.setTextSize(14);
        tvResult.setTextColor(0xFF111827);
        tvResult.setTextIsSelectable(true);
        tvResult.setMinLines(2);
        tvResult.setMaxLines(12);
        resultCard.addView(tvResult, match());
        panelContent.addView(resultCard, match());

        // --- reply section ---
        panelContent.addView(sectionLabel(R.string.label_reply));
        etReply = new EditText(this);
        etReply.setMinLines(2);
        etReply.setMaxLines(3);
        etReply.setTextSize(13);
        etReply.setHint(R.string.hint_reply);
        etReply.setFilters(createInputFilters());
        panelContent.addView(etReply, match());

        Button btnReply = new Button(this);
        btnReply.setText(R.string.btn_reply);
        btnReply.setOnClickListener(v -> doReply());