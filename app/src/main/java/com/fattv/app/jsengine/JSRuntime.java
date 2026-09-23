package com.fattv.app.jsengine;

import android.util.Base64;
import android.util.Log;

import com.fattv.app.model.Song;
import com.fattv.app.source.SourceProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * JS 音源引擎运行时（基于 Mozilla Rhino，纯 Java，零 NDK 依赖）
 * 负责：加载 JS 音源脚本、注入 Native Bridge、执行 search/resolveUrl/getLyric
 * 架构上预留 QuickJS 替换接口，当前以 Rhino 实现保证构建稳定性。
 */
public class JSRuntime {
    private static final String TAG = "JSRuntime";
    private static final int OPTIMIZATION_LEVEL = -1; // -1 = interpreted mode (stable on Android)
    private static final int OKHTTP_TIMEOUT_SEC = 15;

    private org.mozilla.javascript.Context rhino;
    private Scriptable globalScope;
    private OkHttpClient httpClient;
    private boolean initialized = false;
    private String currentScriptId = "";
    private String currentScriptName = "";

    /**
     * 构造引擎实例（此时未加载任何脚本）
     */
    public JSRuntime() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(OKHTTP_TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(OKHTTP_TIMEOUT_SEC, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 初始化 Rhino 运行时并注入 __fattv_native__ Bridge
     */
    public synchronized void init() {
        if (initialized) return;
        try {
            org.mozilla.javascript.Context.enter();
            rhino = org.mozilla.javascript.Context.getCurrentContext();
            rhino.setOptimizationLevel(OPTIMIZATION_LEVEL);
            rhino.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);

            globalScope = rhino.initStandardObjects();

            // 注入 Native Bridge: __fattv_native__
            ScriptableObject bridge = new ScriptableObject() {
                @Override
                public String getClassName() {
                    return "NativeBridge";
                }
            };
            bridge.put("http_get", bridge, new HttpGetCallable());
            bridge.put("http_post", bridge, new HttpPostCallable());
            bridge.put("http_post_form", bridge, new HttpPostFormCallable());
            bridge.put("weapi_sign", bridge, new WeapiSignCallable());
            bridge.put("log", bridge, new LogCallable());
            bridge.put("get_cache", bridge, new GetCacheCallable());
            bridge.put("set_cache", bridge, new SetCacheCallable());

            globalScope.put("__fattv_native__", globalScope, bridge);

            // 预置工具函数: JSON 原生已存在; 补充 Promise polyfill 简化
            rhino.evaluateString(globalScope,
                    "var console = { log: function() { __fattv_native__.log('info', Array.prototype.slice.call(arguments).join(' ')); } };",
                    "<console-polyfill>", 1, null);

            initialized = true;
            Log.i(TAG, "Rhino engine initialized, bridge injected.");
        } catch (Exception e) {
            Log.e(TAG, "Engine init failed", e);
            throw new RuntimeException("JS engine init failed", e);
        }
    }

    /**
     * 从 assets 或字符串加载音源脚本
     * @param scriptContent JS 脚本完整内容
     * @param scriptId      唯一标识（如 "default-music"）
     * @param scriptName    显示名称（如 "默认音源"）
     * @return 是否加载成功
     */
    public synchronized boolean loadScript(String scriptContent, String scriptId, String scriptName) {
        if (!initialized) init();
        try {
            // 清空旧脚本状态（保留 bridge）
            // Rhino 不支持真正的沙箱重置，这里简单覆盖 module
            Object existing = globalScope.get("module", globalScope);
            if (existing != Scriptable.NOT_FOUND) {
                globalScope.delete("module");
            }

            // 执行脚本；脚本应设置 module.exports
            rhino.evaluateString(globalScope, scriptContent,
                    scriptId + ".js", 1, null);

            this.currentScriptId = scriptId;
            this.currentScriptName = scriptName;
            Log.i(TAG, "Script loaded: " + scriptId + " (" + scriptName + ")");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Script load failed: " + scriptId, e);
            return false;
        }
    }

    /**
     * 从 assets 目录加载脚本
     */
    public boolean loadScriptFromAssets(android.content.Context context, String assetPath, String scriptId, String scriptName) {
        try {
            InputStream is = context.getAssets().open(assetPath);
            String content = readStream(is);
            return loadScript(content, scriptId, scriptName);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load asset: " + assetPath, e);
            return false;
        }
    }

    /**
     * 调用 JS 脚本的 search 方法
     * @param keyword 搜索关键词
     * @param page    页码（从1开始）
     * @return 歌曲列表，失败返回 null
     */
    @SuppressWarnings("unchecked")
    public synchronized List<Song> search(String keyword, int page) {
        String json = invokeJSReturningJson("search", keyword, page, 20);
        if (json == null) return null;
        try {
            JSONArray arr = new JSONArray(json);
            List<Song> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Song s = new Song();
                s.id = o.optString("id", "");
                s.title = o.optString("name", o.optString("title", ""));
                s.artist = o.optString("artist", o.optString("singer", ""));
                s.album = o.optString("album", "");
                s.albumId = o.optString("albumId", "");
                s.coverUrl = o.optString("pic", o.optString("cover", ""));
                s.duration = o.optLong("duration", 0);
                s.sourceType = SourceProvider.SourceType.PUBLIC;
                list.add(s);
            }
            return list;
        } catch (Exception e) {
            Log.e(TAG, "Parse search result failed", e);
            return null;
        }
    }

    /**
     * 调用 JS 脚本的 resolveUrl 方法
     * @param songId   歌曲 ID
     * @param quality  音质: 128k / 320k / flac
     * @return 播放 URL，失败返回 null
     */
    public synchronized String resolveUrl(String songId, String quality) {
        String json = invokeJSReturningJson("resolveUrl", songId, quality);
        if (json == null) return null;
        try {
            JSONObject o = new JSONObject(json);
            return o.optString("url", null);
        } catch (Exception e) {
            Log.e(TAG, "Parse resolveUrl result failed", e);
            return null;
        }
    }

    /**
     * 调用 JS 脚本的 getLyric 方法
     * @param songId 歌曲 ID
     * @return LRC 格式歌词，失败返回 null
     */
    public synchronized String getLyric(String songId) {
        String json = invokeJSReturningJson("getLyric", songId);
        if (json == null) return null;
        try {
            JSONObject o = new JSONObject(json);
            return o.optString("lrc", o.optString("lyric", null));
        } catch (Exception e) {
            Log.e(TAG, "Parse lyric result failed", e);
            return null;
        }
    }

    /**
     * 调用 JS 脚本中返回 JSON 数组的方法（如 getTopSongs/getTopAlbums/getTopMvs）
     * @return JSONArray，失败返回 null
     */
    public synchronized JSONArray callArrayMethod(String methodName, Object... args) {
        String json = invokeJSReturningJson(methodName, args);
        if (json == null) return null;
        try {
            return new JSONArray(json);
        } catch (Exception e) {
            Log.e(TAG, "Parse array result failed: " + methodName, e);
            return null;
        }
    }

    /**
     * 调用 JS 脚本中返回 JSON 对象的方法（如 resolveMvUrl）
     * @return JSONObject，失败返回 null
     */
    public synchronized JSONObject callObjectMethod(String methodName, Object... args) {
        String json = invokeJSReturningJson(methodName, args);
        if (json == null) return null;
        try {
            return new JSONObject(json);
        } catch (Exception e) {
            Log.e(TAG, "Parse object result failed: " + methodName, e);
            return null;
        }
    }

    /**
     * 获取当前加载的脚本信息
     */
    public ScriptInfo getCurrentScriptInfo() {
        if (currentScriptId.isEmpty()) return null;
        ScriptInfo info = new ScriptInfo();
        info.id = currentScriptId;
        info.name = currentScriptName;
        return info;
    }

    /**
     * 销毁引擎释放资源
     */
    public synchronized void destroy() {
        try {
            if (rhino != null) {
                org.mozilla.javascript.Context.exit();
            }
        } catch (Exception e) {
            Log.w(TAG, "Destroy warning", e);
        }
        initialized = false;
        currentScriptId = "";
        currentScriptName = "";
    }

    // ============== Private helpers ==============

    /**
     * 在当前线程安全的 Rhino Context 中调用 JS 方法并序列化结果为 JSON。
     * Rhino 的 Context 绑定线程：若调用线程没有 Context 则临时 enter，调用完 exit，
     * 且序列化必须在同一 Context 内完成（否则后台线程会抛 No Context 异常）。
     */
    private String invokeJSReturningJson(String methodName, Object... args) {
        if (!initialized) {
            Log.e(TAG, "Engine not initialized");
            return null;
        }
        boolean entered = false;
        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.getCurrentContext();
        if (cx == null) {
            cx = org.mozilla.javascript.Context.enter();
            entered = true;
        }
        try {
            Object moduleObj = globalScope.get("module", globalScope);
            if (moduleObj == null || moduleObj == Scriptable.NOT_FOUND) {
                Log.e(TAG, "module not found in scope");
                return null;
            }
            Scriptable module = (Scriptable) moduleObj;
            Object exportsObj = module.get("exports", module);
            if (exportsObj == null || exportsObj == Scriptable.NOT_FOUND) {
                Log.e(TAG, "module.exports not found");
                return null;
            }
            Scriptable exports = (Scriptable) exportsObj;
            Object fnObj = exports.get(methodName, exports);
            if (!(fnObj instanceof Function)) {
                Log.e(TAG, "Method not found: " + methodName);
                return null;
            }
            Function fn = (Function) fnObj;
            Object result = fn.call(cx, globalScope, exports, wrapArgs(args));
            if (result == null || result instanceof Undefined) return null;
            Object json = NativeJSON.stringify(cx, globalScope, result, null, null);
            return json != null ? json.toString() : null;
        } catch (Exception e) {
            Log.e(TAG, "Call method failed: " + methodName, e);
            return null;
        } finally {
            if (entered) {
                org.mozilla.javascript.Context.exit();
            }
        }
    }

    private Object[] wrapArgs(Object[] args) {
        Object[] wrapped = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            wrapped[i] = org.mozilla.javascript.Context.javaToJS(args[i], globalScope);
        }
        return wrapped;
    }

    private static String readStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        is.close();
        return sb.toString();
    }

    // ============== Native Bridge Callables ==============

    private class HttpGetCallable extends ScriptableObject implements Callable {
        @Override public String getClassName() { return "HttpGetCallable"; }
        @Override
        public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length < 1) return "";
            String url = org.mozilla.javascript.Context.toString(args[0]);
            String headersJson = args.length > 1 ? org.mozilla.javascript.Context.toString(args[1]) : "{}";
            try {
                Request.Builder req = new Request.Builder().url(url);
                if (!headersJson.isEmpty() && !headersJson.equals("{}")) {
                    JSONObject h = new JSONObject(headersJson);
                    for (java.util.Iterator<String> it = h.keys(); it.hasNext(); ) {
                        String key = it.next();
                        req.header(key, h.getString(key));
                    }
                }
                Response resp = httpClient.newCall(req.build()).execute();
                String body = resp.body() != null ? resp.body().string() : "";
                JSONObject result = new JSONObject();
                result.put("status", resp.code());
                result.put("body", body);
                JSONObject respHeaders = new JSONObject();
                for (String name : resp.headers().names()) {
                    respHeaders.put(name, resp.header(name));
                }
                result.put("headers", respHeaders);
                return result.toString();
            } catch (Exception e) {
                Log.e(TAG, "http_get error: " + url, e);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        }
    }

    private class HttpPostCallable extends ScriptableObject implements Callable {
        @Override public String getClassName() { return "HttpPostCallable"; }
        @Override
        public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length < 2) return "";
            String url = org.mozilla.javascript.Context.toString(args[0]);
            String body = org.mozilla.javascript.Context.toString(args[1]);
            String headersJson = args.length > 2 ? org.mozilla.javascript.Context.toString(args[2]) : "{}";
            try {
                okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
                okhttp3.RequestBody reqBody = okhttp3.RequestBody.create(body, JSON);
                Request.Builder req = new Request.Builder().url(url).post(reqBody);
                if (!headersJson.isEmpty() && !headersJson.equals("{}")) {
                    JSONObject h = new JSONObject(headersJson);
                    for (java.util.Iterator<String> it = h.keys(); it.hasNext(); ) {
                        String key = it.next();
                        req.header(key, h.getString(key));
                    }
                }
                Response resp = httpClient.newCall(req.build()).execute();
                String respBody = resp.body() != null ? resp.body().string() : "";
                JSONObject result = new JSONObject();
                result.put("status", resp.code());
                result.put("body", respBody);
                return result.toString();
            } catch (Exception e) {
                Log.e(TAG, "http_post error: " + url, e);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        }
    }

    private class HttpPostFormCallable extends ScriptableObject implements Callable {
        @Override public String getClassName() { return "HttpPostFormCallable"; }
        @Override
        public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length < 2) return "";
            String url = org.mozilla.javascript.Context.toString(args[0]);
            String formBody = org.mozilla.javascript.Context.toString(args[1]);
            String headersJson = args.length > 2 ? org.mozilla.javascript.Context.toString(args[2]) : "{}";
            try {
                okhttp3.MediaType FORM = okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
                okhttp3.RequestBody reqBody = okhttp3.RequestBody.create(formBody, FORM);
                Request.Builder req = new Request.Builder().url(url).post(reqBody);
                if (!headersJson.isEmpty() && !headersJson.equals("{}")) {
                    JSONObject h = new JSONObject(headersJson);
                    for (java.util.Iterator<String> it = h.keys(); it.hasNext(); ) {
                        String key = it.next();
                        req.header(key, h.getString(key));
                    }
                }
                Response resp = httpClient.newCall(req.build()).execute();
                String respBody = resp.body() != null ? resp.body().string() : "";
                JSONObject result = new JSONObject();
                result.put("status", resp.code());
                result.put("body", respBody);
                return result.toString();
            } catch (Exception e) {
                Log.e(TAG, "http_post_form error: " + url, e);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        }
    }

    /**
     * 网易云 weapi 参数签名（AES-CBC 两轮 + RSA 加密随机 key）
     * 入参 args[0] = JSON 字符串（待加密的请求参数），args[1] = 可选路径（仅用于日志）
     * 返回 {"params": "...", "encSecKey": "..."} JSON 字符串
     */
    private class WeapiSignCallable extends ScriptableObject implements Callable {
        @Override public String getClassName() { return "WeapiSignCallable"; }
        @Override
        public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length < 1) return "";
            String paramsJson = org.mozilla.javascript.Context.toString(args[0]);
            String pathTag = args.length > 1 ? org.mozilla.javascript.Context.toString(args[1]) : "";
            try {
                // 1. 第一轮 AES：固定密钥加密
                byte[] once = aesEncrypt(paramsJson.getBytes("UTF-8"), WEAPI_FIXED_KEY, WEAPI_IV);
                // 2. 随机 16 字节 AES key
                byte[] randomKey = new byte[16];
                new SecureRandom().nextBytes(randomKey);
                // 3. 第二轮 AES：随机密钥加密
                byte[] twice = aesEncrypt(once, randomKey, WEAPI_IV);
                String params = android.util.Base64.encodeToString(twice, android.util.Base64.NO_WRAP);
                // 4. RSA 加密随机 key -> encSecKey（hex）
                String encSecKey = rsaEncryptHex(randomKey);
                JSONObject out = new JSONObject();
                out.put("params", params);
                out.put("encSecKey", encSecKey);
                if (!pathTag.isEmpty()) {
                    Log.d(TAG, "weapi_sign(" + pathTag + ") params.len=" + params.length() + " key.len=" + encSecKey.length());
                }
                return out.toString();
            } catch (Exception e) {
                Log.e(TAG, "weapi_sign error", e);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        }
    }

    // ============== weapi 加密常量与算法 ==============
    private static final byte[] WEAPI_FIXED_KEY = "0CoJUm6Qyw8W8jud".getBytes();
    private static final byte[] WEAPI_IV = "0102030405060708".getBytes();

    private static byte[] aesEncrypt(byte[] data, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(data);
    }

    private static String rsaEncryptHex(byte[] data) throws Exception {
        BigInteger modulus = new BigInteger(
                "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b72515"
                        + "2b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ec"
                        + "bda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d8"
                        + "13cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7",
                16);
        BigInteger exponent = new BigInteger("010001", 16);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        java.security.PublicKey pubKey = factory.generatePublic(spec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        byte[] encrypted = cipher.doFinal(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : encrypted) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private class LogCallable extends ScriptableObject implements Callable {
        @Override public String getClassName() { return "LogCallable"; }
        @Override
        public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            if (args.length < 2) return Undefined.instance;
            String level = org.mozilla.javascript.Context.toString(args[0]);
            String msg = org.mozilla.javascript.Context.toString(args[1]);
            switch (level) {
                case "error": Log.e(TAG, "[JS] " + msg); break;
                case "warn":  Log.w(TAG, "[JS] " + msg); break;
                default:      Log.i(TAG, "[JS] " + msg); break;
            }
            return Undefined.instance;
        }
    }

    private class GetCacheCallable extends ScriptableObject implements Callable {
        @Override public String getClassName() { return "GetCacheCallable"; }
        @Override
        public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            // 预留接口：可接入 SharedPreferences 或内存缓存
            return null;
        }
    }

    private class SetCacheCallable extends ScriptableObject implements Callable {
        @Override public String getClassName() { return "SetCacheCallable"; }
        @Override
        public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            // 预留接口：可接入 SharedPreferences 或内存缓存
            return Undefined.instance;
        }
    }

    /**
     * 脚本元信息
     */
    public static class ScriptInfo {
        public String id;
        public String name;
        public String version;
        public String author;
    }
}
