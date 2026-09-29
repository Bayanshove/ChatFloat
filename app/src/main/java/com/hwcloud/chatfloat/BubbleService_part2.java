panelContent.addView(btnReply, match());

        LinearLayout replyCard = new LinearLayout(this);
        replyCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable rpCardBg = new GradientDrawable();
        rpCardBg.setCornerRadius(dp(8));
        rpCardBg.setColor(0xFFF0FDF4);
        rpCardBg.setStroke(dp(1), 0xFF86EFAC);
        replyCard.setBackground(rpCardBg);
        replyCard.setPadding(dp(10), dp(8), dp(10), dp(8));
        tvReplyOut = new TextView(this);
        tvReplyOut.setTextSize(14);
        tvReplyOut.setTextColor(0xFF0F766E);
        tvReplyOut.setTypeface(Typeface.DEFAULT_BOLD);
        tvReplyOut.setTextIsSelectable(true);
        tvReplyOut.setMinLines(1);
        tvReplyOut.setMaxLines(6);
        replyCard.addView(tvReplyOut, match());
        panelContent.addView(replyCard, match());

        Button btnCopy = new Button(this);
        btnCopy.setText(R.string.btn_copy);
        btnCopy.setOnClickListener(v -> copyReply());
        panelContent.addView(btnCopy, match());

        // --- tap outside to dismiss ---
        panelScroll.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_OUTSIDE) {
                hidePanel();
                return true;
            }
            return false;
        });
    }

    private void togglePanel() {
        if (panelScroll != null && panelScroll.getParent() != null) hidePanel();
        else showPanel();
    }

    private void showPanel() {
        if (panelScroll == null) buildPanel();
        if (panelScroll.getParent() != null) return;
        panelLp = new WindowManager.LayoutParams(
                getResources().getDisplayMetrics().widthPixels - dp(24),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.60),
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        panelLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        panelLp.y = dp(36);
        wm.addView(panelScroll, panelLp);
        Logger.i("UI", "panel shown");

        // Don't auto-translate, just show hint
        tvStatus.setText(R.string.status_clip_hint);
    }

    private void hidePanel() {
        if (panelScroll != null && panelScroll.getParent() != null) {
            try { wm.removeView(panelScroll); } catch (Exception ignored) {}
            Logger.i("UI", "panel hidden");
        }
    }

    // ===== Actions =====

    private void doTranslate(boolean fromClipboard) {
        if (fromClipboard) {
            String clip = readClipboard();
            if (clip != null && !clip.trim().isEmpty()) {
                String cleaned = sanitizeInput(clip.trim());
                if (!cleaned.isEmpty()) {
                    etOriginal.setText(cleaned);
                    Logger.i("CLIP", "manual-read: " + cleaned.substring(0, Math.min(cleaned.length(), 80)));
                } else {
                    tvStatus.setText(R.string.status_no_text);
                    return;
                }
            }
        }
        String text = sanitizeInput(etOriginal.getText().toString());
        if (text.isEmpty()) {
            tvStatus.setText(R.string.status_no_text);
            return;
        }
        String prompt = PREFIX_TRANSLATE + "\n" + text;
        callApi(prompt, true);
    }

    private void doReply() {
        String text = sanitizeInput(etReply.getText().toString());
        if (text.isEmpty()) {
            tvStatus.setText(R.string.status_no_reply);
            return;
        }
        String prompt = PREFIX_REPLY + "\n" + text;
        callApi(prompt, false);
    }

    private void callApi(final String input, final boolean isTranslate) {
        final String base = prefs.getString("base", "");
        final String key = prefs.getString("key", "");
        final String model = prefs.getString("model", "");
        final String prompt = prefs.getString("prompt", ApiClient.DEFAULT_PROMPT);

        Logger.i("API", "callApi isTranslate=" + isTranslate + " input(len)=" + input.length());

        startThinkingAnimation();
        new Thread(() -> {
            try {
                final String out = ApiClient.chat(base, key, model, prompt, input, 60);
                main.post(() -> {
                    stopThinkingAnimation();
                    if (isTranslate) {
                        parseAndDisplayTranslateResult(out);
                    } else {
                        tvReplyOut.setText(out);
                        tvStatus.setText(R.string.status_done);
                    }
                    Logger.i("API", "result set, len=" + out.length());
                });
            } catch (final Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                main.post(() -> {
                    stopThinkingAnimation();
                    tvStatus.setText(getString(R.string.status_error, msg));
                    Logger.i("API", "error: " + msg);
                });
            }
        }).start();
    }

    /** Parse combined response into thinking and result parts */
    private void parseAndDisplayTranslateResult(String combined) {
        String thinking = null;
        String result = combined;

        // Check for "💭 思考過程：" marker
        int thinkStart = combined.indexOf("💭 思考過程：");
        if (thinkStart >= 0) {
            int resultStart = combined.indexOf("\n\n📝 結果：");
            if (resultStart > thinkStart) {
                thinking = combined.substring(thinkStart + "💭 思考過程：".length(), resultStart).trim();
                result = combined.substring(resultStart + "\n\n📝 結果：".length()).trim();
            } else {
                thinking = combined.substring(thinkStart + "💭 思考過程：".length()).trim();
                result = "";
            }
        }

        // Also check for "📝 結果：" without thinking marker
        if (thinking == null) {
            int resStart = combined.indexOf("📝 結果：");
            if (resStart >= 0) {
                result = combined.substring(resStart + "📝 結果：".length()).trim();
            }
        }

        // Update UI
        if (thinking != null && !thinking.isEmpty()) {
            tvThinking.setText(thinking);
            thinkingCard.setVisibility(View.VISIBLE);
            Logger.i("UI", "thinking shown, len=" + thinking.length());
        } else {
            thinkingCard.setVisibility(View.GONE);
        }

        tvResult.setText(result);
        tvStatus.setText(R.string.status_done);
    }

    // ===== Thinking animation =====

    private void startThinkingAnimation() {
        stopThinkingAnimation();
        thinkingDots = 0;
        thinkingRunnable = new Runnable() {
            @Override public void run() {
                thinkingDots = (thinkingDots % 3) + 1;
                StringBuilder dots = new StringBuilder();
                for (int i = 0; i < thinkingDots; i++) dots.append('·');
                tvStatus.setText(getString(R.string.status_thinking) + dots);
                main.postDelayed(this, 500);
            }
        };
        main.post(thinkingRunnable);
    }

    private void stopThinkingAnimation() {
        if (thinkingRunnable != null) {
            main.removeCallbacks(thinkingRunnable);
            thinkingRunnable = null;
        }
    }

    // ===== Clipboard =====

    private String readClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                ClipData cd = cm.getPrimaryClip();
                if (cd != null && cd.getItemCount() > 0) {
                    CharSequence cs = cd.getItemAt(0).coerceToText(this);
                    if (cs != null) return cs.toString();
                }
            }
        } catch (Exception e) {
            Logger.i("CLIP", "read failed: " + e.getMessage());
        }
        return null;
    }

    private void copyReply() {
        String text = tvReplyOut.getText().toString().trim();
        if (text.isEmpty()) { tvStatus.setText(R.string.status_no_reply_generated); return; }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("reply", text));
            Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
            Logger.i("CLIP", "reply copied: " + text.substring(0, Math.min(text.length(), 60)));
        }
    }

    private void copyLogToClipboard() {
        try {
            File f = Logger.get().getLogFile();
            if (f == null || !f.exists()) { Toast.makeText(this, R.string.toast_no_log, Toast.LENGTH_SHORT).show(); return; }
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line; int count = 0;
            while ((line = br.readLine()) != null) { sb.append(line).append('\n'); count++; if (count > 200) break; }
            br.close();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("log", sb.toString()));
                Toast.makeText(this, getString(R.string.toast_log_copied, count), Toast.LENGTH_SHORT).show();
            }
            File exported = Logger.get().export();
            if (exported != null) Toast.makeText(this, R.string.toast_log_exported, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.toast_log_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Helpers =====

    /**
     * Sanitize input: remove control characters, limit length
     */
    private String sanitizeInput(String input) {
        if (input == null) return "";
        // Remove control characters (except newline, carriage return, tab)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // Allow printable characters (32-126, 128+), newline, carriage return, tab
            if ((c >= 32 && c != 127) || c == '\n' || c == '\r' || c == '\t') {
                sb.append(c);
            }
        }
        String cleaned = sb.toString().trim();
        // Limit length to 5000 characters
        if (cleaned.length() > 5000) {
            cleaned = cleaned.substring(0, 5000);
        }
        return cleaned;
    }

    /**
     * Create input filters for EditText: length limit + control character filter
     */
    private InputFilter[] createInputFilters() {
        InputFilter lengthFilter = new InputFilter.LengthFilter(5000);
        InputFilter textFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                StringBuilder sb = new StringBuilder();
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    // Allow printable characters (32-126, 128+), newline, carriage return, tab
                    if ((c >= 32 && c != 127) || c == '\n' || c == '\r' || c == '\t') {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }
        };
        return new InputFilter[]{lengthFilter, textFilter};
    }

    private TextView sectionLabel(int resId) {
        TextView tv = new TextView(this);
        tv.setText(resId);
        tv.setTextSize(12);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(0xFF374151);
        tv.setPadding(0, dp(8), 0, dp(3));
        return tv;
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopThinkingAnimation();
        hidePanel();
        if (bubble != null) { try { wm.removeView(bubble); } catch (Exception ignored) {} }
        Logger.i("SVC", "BubbleService onDestroy");
        Logger.get().close();
        stopForeground(true);
    }
}