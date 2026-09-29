package com.hwcloud.chatfloat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApiClient {

    public static final String DEFAULT_PROMPT =
        "你是社交聊天翻譯助手，幫用戶在 TikTok 等軟件和外國人聊天。\n" +
        "用戶消息以指令前綴開頭：\n" +
        "【譯】＝把後面的內容翻譯成自然繁體中文，解釋俚語/縮寫/網絡梗，並分析語氣；\n" +
        "【回】＝把後面輸入的中文回覆生成地道英文（保留語氣與情緒）。\n" +
        "保證意思和語氣自然地道，不要逐字直譯。";

    public static String normalize(String base) {
        if (base == null) return "";
        String u = base.trim();
        if (u.isEmpty()) return "";
        if (u.toLowerCase().contains("completions")) return u;
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        if (u.toLowerCase().endsWith("/v1")) return u + "/chat/completions";
        return u + "/v1/chat/completions";
    }

    public static String normalizeModels(String base) {
        if (base == null) return "";
        String u = base.trim();
        if (u.isEmpty()) return "";
        if (u.toLowerCase().endsWith("/models")) return u;
        int ci = u.toLowerCase().indexOf("chat/completions");
        if (ci >= 0) u = u.substring(0, ci);
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        if (u.toLowerCase().endsWith("/v1")) return u + "/models";
        return u + "/v1/models";
    }

    /**
     * Chat completion. Returns formatted string:
     * If reasoning_content exists: "💭 思考過程：\n...reasoning...\n\n📝 結果：\n...content..."
     * Otherwise just the content.
     */
    public static String chat(String base, String key, String model, String system, String user, int timeoutSec) throws Exception {
        String urlStr = normalize(base);
        if (urlStr.isEmpty()) throw new Exception("請先在設置頁填寫 API 地址");

        Logger.i("API", ">>> POST " + urlStr);
        Logger.i("API", "model=" + model + " system(len)=" + system.length() + " user(len)=" + user.length());

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(timeoutSec * 1000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            if (key != null && !key.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + key.trim());
            }
            JSONObject body = new JSONObject();
            body.put("model", (model == null || model.trim().isEmpty()) ? "gpt-4o-mini" : model.trim());
            body.put("temperature", 0.3);
            body.put("stream", false);
            JSONArray msgs = new JSONArray();
            msgs.put(new JSONObject().put("role", "system").put("content", system));
            msgs.put(new JSONObject().put("role", "user").put("content", user));
            body.put("messages", msgs);
            byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(out.length);
            OutputStream os = conn.getOutputStream();
            try { os.write(out); } finally { os.close(); }

            int code = conn.getResponseCode();
            Logger.i("API", "<<< HTTP " + code);

            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String resp = readAll(is);
            Logger.i("API", "<<< body(len)=" + resp.length() + " " + brief(resp));

            if (code < 200 || code >= 300) {
                throw new Exception("HTTP " + code + " " + brief(resp));
            }
            JSONObject r = new JSONObject(resp);
            if (r.has("error")) {
                JSONObject err = r.optJSONObject("error");
                String msg = err != null ? err.optString("message", "API 錯誤") : "API 錯誤";
                Logger.i("API", "<<< error: " + msg);
                throw new Exception(msg);
            }
            JSONArray choices = r.optJSONArray("choices");
            if (choices == null || choices.length() == 0) throw new Exception("回應格式異常（沒有 choices）");
            JSONObject msg0 = choices.getJSONObject(0).optJSONObject("message");
            if (msg0 == null) throw new Exception("回應格式異常（沒有 message）");

            String content = msg0.optString("content", "");
            String reasoning = msg0.optString("reasoning_content", null);
            if (reasoning == null) reasoning = msg0.optString("reasoning", null);

            Logger.i("API", "<<< content(len)=" + (content != null ? content.length() : 0)
                    + " reasoning=" + (reasoning != null ? "yes(" + reasoning.length() + ")" : "no"));

            if (content == null || content.trim().isEmpty()) throw new Exception("模型返回了空內容");

            if (reasoning != null && !reasoning.trim().isEmpty()) {
                return "💭 思考過程：\n" + reasoning.trim() + "\n\n📝 結果：\n" + content.trim();
            }
            return content.trim();
        } finally {
            conn.disconnect();
        }
    }

    public static List<String> fetchModels(String base, String key) throws Exception {
        String urlStr = normalizeModels(base);
        if (urlStr.isEmpty()) throw new Exception("請先填寫 API 地址");
        Logger.i("API", ">>> GET " + urlStr);
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            if (key != null && !key.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + key.trim());
            }
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String resp = readAll(is);
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code + " " + brief(resp));
            JSONObject r = new JSONObject(resp);
            JSONArray data = r.optJSONArray("data");
            List<String> list = new ArrayList<>();
            if (data != null) {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject m = data.optJSONObject(i);
                    if (m != null) {
                        String id = m.optString("id");
                        if (id != null && !id.isEmpty()) list.add(id);
                    }
                }
            }
            Collections.sort(list);
            Logger.i("API", "<<< models: " + list.size() + " " + brief(list.toString()));
            return list;
        } finally {
            conn.disconnect();
        }
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        } finally {
            br.close();
        }
        return sb.toString();
    }

    private static String brief(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
