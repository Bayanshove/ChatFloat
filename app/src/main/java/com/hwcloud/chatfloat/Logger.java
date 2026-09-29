package com.hwcloud.chatfloat;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Logger {

    private static Logger instance;
    private File logFile;
    private BufferedWriter writer;
    private SimpleDateFormat sdf;
    private boolean enabled = true;

    private Logger(Context ctx) {
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        File dir = new File(ctx.getFilesDir(), "logs");
        dir.mkdirs();
        logFile = new File(dir, "chatfloat.log");
        try {
            writer = new BufferedWriter(new FileWriter(logFile, true));
        } catch (IOException e) { /* ignore */ }
    }

    public static synchronized Logger init(Context ctx) {
        if (instance == null) instance = new Logger(ctx.getApplicationContext());
        return instance;
    }

    public static Logger get() { return instance; }

    public void setEnabled(boolean e) { enabled = e; }
    public boolean isEnabled() { return enabled; }

    public void log(String tag, String msg) {
        if (!enabled || writer == null) return;
        String line = "[" + sdf.format(new Date()) + "] [" + tag + "] " + msg;
        synchronized (this) {
            try {
                writer.write(line);
                writer.newLine();
                writer.flush();
            } catch (IOException ignored) {}
        }
    }

    public static void i(String tag, String msg) {
        if (instance != null) instance.log(tag, msg);
    }

    public File getLogFile() { return logFile; }

    public void clear() {
        synchronized (this) {
            try {
                if (writer != null) writer.close();
                writer = new BufferedWriter(new FileWriter(logFile, false));
                log("LOG", "日誌已清除");
            } catch (IOException ignored) {}
        }
    }

    public void close() {
        synchronized (this) {
            try { if (writer != null) writer.close(); writer = null; } catch (IOException ignored) {}
        }
    }

    /** Export log to /sdcard/Download/ChatFloat_log.txt */
    public File export() {
        try {
            File dest = new File("/sdcard/Download/ChatFloat_log.txt");
            java.io.FileInputStream fis = new java.io.FileInputStream(logFile);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(dest);
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) > 0) fos.write(buf, 0, n);
            fis.close();
            fos.close();
            return dest;
        } catch (Exception e) {
            return null;
        }
    }
}
