/**
 * fat TV 真实公网音源脚本（ES5 语法，兼容 Rhino 1.7.14）
 * 规范：CommonJS 模块，必须导出 search / resolveUrl / getLyric
 *
 * 适配说明：
 * - 默认对接网易云音乐 API 格式（Binaryify/NeteaseCloudMusicApi）
 * - 用户修改 CONFIG.apiBase 即可指向自建/第三方后端
 * - 所有 HTTP 调用通过 __fattv_native__.http_get 走 Native 网络层
 * - 仅使用 ES5 语法：var、function、for、if；禁止 let/const/箭头函数/Promise/async/展开/rest 参数
 */

var module = module || {};

// ============================ 配置区 ============================
var CONFIG = {
    // API 基础地址，修改为自建后端地址或第三方镜像
    // 自建参考：https://github.com/Binaryify/NeteaseCloudMusicApi
    // 部署后填写，例如 "http://192.168.1.100:3000" 或 "https://your-api.com"
    apiBase: "https://netease-cloud-music-api-fate-d.vercel.app",

    // 搜索每页条数
    pageSize: 20,

    // 音质偏好优先级：exhigh > high > standard
    qualityOrder: ["exhigh", "high", "standard"]
};

// ============================ 工具函数 ============================

/**
 * 简单 URL 编码（兼容性 Polyfill）
 */
function encodeParam(str) {
    if (typeof str !== "string") {
        str = String(str);
    }
    return encodeURIComponent(str);
}

/**
 * 安全 JSON 解析，失败返回 null 并打印日志
 */
function safeJsonParse(jsonStr, label) {
    try {
        return JSON.parse(jsonStr);
    } catch (e) {
        __fattv_native__.log("error", label + " JSON parse failed: " + e.message);
        return null;
    }
}

/**
 * 发送 HTTP GET 请求，解析 wrapper {status, body, headers}，返回 body 字符串
 */
function httpGetBody(url, tag) {
    __fattv_native__.log("info", "[" + tag + "] GET " + url);
    var resp = __fattv_native__.http_get(url, JSON.stringify({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Referer": "https://music.163.com"
    }));
    if (!resp) {
        __fattv_native__.log("warn", "[" + tag + "] empty response");
        return null;
    }
    var wrapper = safeJsonParse(resp, tag + "_wrapper");
    if (!wrapper || wrapper.error) {
        __fattv_native__.log("warn", "[" + tag + "] http_get error: " + (wrapper ? wrapper.error : "parse fail"));
        return null;
    }
    var body = wrapper.body || "";
    __fattv_native__.log("debug", "[" + tag + "] status=" + wrapper.status + " body length=" + body.length);
    return body;
}

/**
 * 发送 HTTP GET 请求并返回解析后的 JSON 对象
 * @param {string} url  完整 URL
 * @param {string} tag  日志标签
 * @returns {Object|null} JSON 对象或 null
 */
function httpGetJson(url, tag) {
    var body = httpGetBody(url, tag);
    if (!body) return null;
    return safeJsonParse(body, tag);
}

/**
 * 格式化毫秒为 [mm:ss.xx] 时间戳
 */
function formatTime(ms) {
    var totalSec = Math.floor(ms / 1000);
    var min = Math.floor(totalSec / 60);
    var sec = totalSec % 60;
    var ms2 = Math.floor((ms % 1000) / 10);
    return (min < 10 ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec + "." + (ms2 < 10 ? "0" : "") + ms2;
}

/**
 * 将网易云歌词格式转换为标准 LRC 格式
 * @param {Array} lrcArr  [{time: 毫秒, text: "歌词"}, ...]
 * @returns {string} LRC 文本
 */
function convertLrc(lrcArr) {
    if (!lrcArr || lrcArr.length === 0) {
        return "[00:00.00]暂无歌词";
    }
    var lines = [];
    for (var i = 0; i < lrcArr.length; i++) {
        var item = lrcArr[i];
        if (item && item.text) {
            lines.push("[" + formatTime(item.time || 0) + "]" + item.text);
        }
    }
    if (lines.length === 0) {
        return "[00:00.00]暂无歌词";
    }
    return lines.join("\n");
}

/**
 * 从网易云歌曲对象提取标准歌曲字段
 */
function mapSong(item) {
    if (!item || !item.id) {
        return null;
    }
    var artists = [];
    if (item.artists && item.artists.length > 0) {
        for (var i = 0; i < item.artists.length; i++) {
            artists.push(item.artists[i].name);
        }
    } else if (item.ar && item.ar.length > 0) {
        for (var j = 0; j < item.ar.length; j++) {
            artists.push(item.ar[j].name);
        }
    }
    var album = "";
    if (item.album && item.album.name) {
        album = item.album.name;
    } else if (item.al && item.al.name) {
        album = item.al.name;
    }
    var pic = "";
    if (item.album && item.album.picUrl) {
        pic = item.album.picUrl;
    } else if (item.al && item.al.picUrl) {
        pic = item.al.picUrl;
    }
    var duration = 0;
    if (item.duration) {
        duration = item.duration;
    } else if (item.dt) {
        duration = item.dt;
    }
    return {
        id: String(item.id),
        name: item.name || "未知歌曲",
        artist: artists.join(" / ") || "未知艺人",
        album: album || "未知专辑",
        duration: duration,
        pic: pic
    };
}

// ============================ 核心接口 ============================

module.exports = {
    id: "netease",
    name: "网易云音乐",
    version: 1,

    /**
     * 搜索歌曲
     * @param {string} keyword  关键词
     * @param {number} page     页码（从1开始）
     * @param {number} limit    每页数量
     * @returns {Array} 歌曲对象数组 [{id, name, artist, album, duration, pic}]
     */
    search: function(keyword, page, limit) {
        __fattv_native__.log("info", "[Netease] search: keyword=" + keyword + " page=" + page + " limit=" + limit);

        var offset = (page - 1) * limit;
        var url = CONFIG.apiBase + "/search?keywords=" + encodeParam(keyword)
            + "&limit=" + limit + "&offset=" + offset;

        var data = httpGetJson(url, "search");
        if (!data || !data.result) {
            __fattv_native__.log("warn", "[Netease] search empty response");
            return [];
        }

        var songs = data.result.songs || [];
        var results = [];
        for (var i = 0; i < songs.length; i++) {
            var mapped = mapSong(songs[i]);
            if (mapped) {
                results.push(mapped);
            }
        }
        __fattv_native__.log("info", "[Netease] search results: " + results.length);
        return results;
    },

    /**
     * 解析歌曲播放地址
     * @param {string} songId   歌曲 ID
     * @param {string} quality  音质: 128k / 320k / flac（映射到 exhigh/high/standard）
     * @returns {Object} {url, headers, bitrate}
     */
    resolveUrl: function(songId, quality) {
        __fattv_native__.log("info", "[Netease] resolveUrl: songId=" + songId + " quality=" + quality);

        var level = "standard";
        if (quality === "flac" || quality === "lossless") {
            level = "exhigh";
        } else if (quality === "320k" || quality === "high") {
            level = "high";
        }

        var url = CONFIG.apiBase + "/song/url/v1?id=" + encodeParam(songId)
            + "&level=" + level;

        var data = httpGetJson(url, "resolveUrl");
        if (!data || !data.data || data.data.length === 0) {
            __fattv_native__.log("warn", "[Netease] resolveUrl empty response");
            return { url: "", headers: {}, bitrate: "0k" };
        }

        var item = data.data[0];
        var songUrl = item.url || "";
        var bitrate = "0k";
        if (item.br) {
            bitrate = Math.round(item.br / 1000) + "k";
        }

        __fattv_native__.log("info", "[Netease] resolveUrl OK, url length=" + songUrl.length + " bitrate=" + bitrate);
        return {
            url: songUrl,
            headers: {
                "Referer": "https://music.163.com"
            },
            bitrate: bitrate
        };
    },

    /**
     * 获取歌词
     * @param {string} songId  歌曲 ID
     * @returns {Object} {lrc, tlyric}
     */
    getLyric: function(songId) {
        __fattv_native__.log("info", "[Netease] getLyric: songId=" + songId);

        var url = CONFIG.apiBase + "/lyric?id=" + encodeParam(songId);
        var data = httpGetJson(url, "lyric");

        if (!data) {
            return { lrc: "[00:00.00]获取歌词失败", tlyric: "" };
        }

        var lrcText = "";
        var tlyricText = "";

        // 优先使用逐字歌词（klyric），其次是普通歌词（lrc）
        if (data.klyric && data.klyric.lyric) {
            lrcText = data.klyric.lyric;
        } else if (data.lrc && data.lrc.lyric) {
            lrcText = data.lrc.lyric;
        }

        if (data.tlyric && data.tlyric.lyric) {
            tlyricText = data.tlyric.lyric;
        }

        // 如果完全没有歌词，返回默认文本
        if (!lrcText) {
            lrcText = "[00:00.00]暂无歌词";
        }

        return {
            lrc: lrcText,
            tlyric: tlyricText
        };
    }
};
