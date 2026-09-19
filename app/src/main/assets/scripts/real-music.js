/**
 * fat TV 真实公网音源脚本（ES5 语法，兼容 Rhino 1.7.14）
 * 规范：CommonJS 模块，必须导出 search / resolveUrl / getLyric
 *
 * 适配说明：
 * - 默认走网易云旧版公开 GET API（music.163.com/api/...，当前环境实测可用，
 *   含搜索/榜单/新碟/精选歌单/歌词/播放直链，无需加密参数）
 * - 保留 weapi 加密接口实现（AES/RSA 由 Java 侧桥接提供），真机家庭宽带可
 *   通过 CONFIG.useOfficialApi=true 切换，当前数据中心 IP 会被风控返回空 body
 * - 用户可通过 CONFIG.apiBase 指向自建 NeteaseCloudMusicApi 后端
 * - 所有 HTTP 调用通过 __fattv_native__.http_get / http_post_form 走 Native 网络层
 * - 仅使用 ES5 语法：var、function、for、if；禁止 let/const/箭头函数/Promise/async/展开/rest 参数
 */

var module = module || {};

// ============================ 配置区 ============================
var CONFIG = {
    // 官方接口基址（公开 API 与 weapi 都挂在 music.163.com 下）
    // 修改为自建后端时，例如 "http://192.168.1.100:3000" 或 "https://your-api.com"
    apiBase: "https://music.163.com",

    // 是否走官方 weapi（true=官方加密接口，适合家庭宽带；false=旧版公开 GET API，适合开发/模拟器）
    useOfficialApi: false,

    // 搜索每页条数
    pageSize: 20,

    // 音质优先级（旧版公开 API 直链固定 128k 通播，flac 不可用）
    qualityOrder: ["standard", "high", "exhigh"]
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
 * 网易云官方 weapi 加密请求（保留实现，供真机家庭宽带 CONFIG.useOfficialApi=true 使用）
 * @param {string} path  接口路径（不含 /weapi/ 前缀），例如 "cloudsearch/get/web"
 * @param {Object} params  业务参数
 * @param {string} tag  日志标签
 * @returns {Object|null} 响应 JSON 对象（data 层）或 null
 */
function weapi(path, params, tag) {
    var merged = {};
    var p;
    for (p in params) {
        if (params.hasOwnProperty(p)) {
            merged[p] = params[p];
        }
    }
    merged.csrf_token = "";

    var signed = __fattv_native__.weapi_sign(JSON.stringify(merged), path);
    if (!signed) {
        __fattv_native__.log("warn", "[" + tag + "] weapi_sign failed");
        return null;
    }
    var signedObj = safeJsonParse(signed, tag + "_sign");
    if (!signedObj || signedObj.error) {
        __fattv_native__.log("warn", "[" + tag + "] weapi_sign error: " + (signedObj ? signedObj.error : "parse fail"));
        return null;
    }

    var form = "params=" + encodeParam(signedObj.params) + "&encSecKey=" + encodeParam(signedObj.encSecKey);
    var url = "https://music.163.com/weapi/" + path;
    __fattv_native__.log("info", "[" + tag + "] POST " + url);

    var resp = __fattv_native__.http_post_form(url, form, JSON.stringify({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0",
        "Referer": "https://music.163.com/",
        "Cookie": "os=pc; appver=8.9.70",
        "Content-Type": "application/x-www-form-urlencoded"
    }));
    if (!resp) {
        __fattv_native__.log("warn", "[" + tag + "] empty response");
        return null;
    }
    var wrapper = safeJsonParse(resp, tag + "_wrapper");
    if (!wrapper || wrapper.error) {
        __fattv_native__.log("warn", "[" + tag + "] http_post_form error: " + (wrapper ? wrapper.error : "parse fail"));
        return null;
    }
    __fattv_native__.log("debug", "[" + tag + "] status=" + wrapper.status + " body length=" + (wrapper.body || "").length);
    var data = safeJsonParse(wrapper.body || "", tag);
    if (data && data.code && data.code !== 200) {
        __fattv_native__.log("warn", "[" + tag + "] api code=" + data.code + " msg=" + (data.message || ""));
        return null;
    }
    return data;
}

/**
 * 通用接口调用：官方 weapi 优先（真机），默认走公开 GET API / 自定义后端 REST
 * @param {string} path   接口路径：公开 API 形如 "api/search/get"；weapi 形如 "cloudsearch/get/web"
 */
function apiCall(path, params, tag) {
    if (CONFIG.useOfficialApi) {
        var we = path.replace(/^api\//, "");
        return weapi(we, params, tag);
    }
    // 公开 API / 自定义后端：REST 风格 GET，参数拼 query
    var qs = [];
    for (var k in params) {
        if (params.hasOwnProperty(k)) {
            qs.push(encodeParam(k) + "=" + encodeParam(params[k]));
        }
    }
    var url = CONFIG.apiBase + "/" + path + (qs.length > 0 ? "?" + qs.join("&") : "");
    var data = httpGetJson(url, tag);
    if (data && data.code && data.code !== 200) {
        __fattv_native__.log("warn", "[" + tag + "] api code=" + data.code + " msg=" + (data.message || ""));
    }
    return data;
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
    var albumId = 0;
    if (item.album && item.album.name) {
        album = item.album.name;
        albumId = item.album.id || 0;
    } else if (item.al && item.al.name) {
        album = item.al.name;
        albumId = item.al.id || 0;
    }
    var pic = "";
    if (item.album && item.album.picUrl) {
        pic = item.album.picUrl;
    } else if (item.al && item.al.picUrl) {
        pic = item.al.picUrl;
    }
    if (!pic && item.picUrl) {
        pic = item.picUrl;
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
        albumId: albumId,
        duration: duration,
        pic: pic
    };
}

// ============================ 核心接口 ============================

module.exports = {
    id: "netease",
    name: "网易云音乐",
    version: 3,

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
        var data = apiCall("api/search/get", {
            s: keyword,
            type: 1,
            limit: limit,
            offset: offset
        }, "search");

        if (!data || !data.result || !data.result.songs) {
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
     * 公开 API 模式：直接返回官方外链（music.163.com/song/media/outer/url 302 到 CDN），
     * ExoPlayer 自动跟随跳转；weapi 模式走增强播放接口。
     * @param {string} songId   歌曲 ID
     * @param {string} quality  音质（公开 API 固定 128k）
     * @returns {Object} {url, headers, bitrate}
     */
    resolveUrl: function(songId, quality) {
        __fattv_native__.log("info", "[Netease] resolveUrl: songId=" + songId + " quality=" + quality);

        if (CONFIG.useOfficialApi) {
            var level = "standard";
            if (quality === "flac" || quality === "lossless") {
                level = "exhigh";
            } else if (quality === "320k" || quality === "high") {
                level = "high";
            }
            var data = apiCall("api/song/enhance/player/url/v1", {
                ids: "[" + songId + "]",
                level: level
            }, "resolveUrl");
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
            __fattv_native__.log("info", "[Netease] resolveUrl(weapi) OK, url length=" + songUrl.length + " bitrate=" + bitrate);
            return {
                url: songUrl,
                headers: {
                    "Referer": "https://music.163.com",
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                },
                bitrate: bitrate
            };
        }

        // 公开 API：外链直连（无需加密参数，ExoPlayer 跟随 302）
        var outer = "https://music.163.com/song/media/outer/url?id=" + songId + ".mp3";
        __fattv_native__.log("info", "[Netease] resolveUrl(public) OK: " + outer);
        return {
            url: outer,
            headers: {
                "Referer": "https://music.163.com",
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            },
            bitrate: "128k"
        };
    },

    /**
     * 获取歌词
     * @param {string} songId  歌曲 ID
     * @returns {Object} {lrc, tlyric}
     */
    getLyric: function(songId) {
        __fattv_native__.log("info", "[Netease] getLyric: songId=" + songId);

        var data = apiCall("api/song/lyric", { id: songId, lv: 1, kv: 1, tv: -1 }, "lyric");

        if (!data) {
            return { lrc: "[00:00.00]获取歌词失败", tlyric: "" };
        }

        var lrcText = "";
        var tlyricText = "";

        if (data.klyric && data.klyric.lyric) {
            lrcText = data.klyric.lyric;
        } else if (data.lrc && data.lrc.lyric) {
            lrcText = data.lrc.lyric;
        }

        if (data.tlyric && data.tlyric.lyric) {
            tlyricText = data.tlyric.lyric;
        }

        if (!lrcText) {
            lrcText = "[00:00.00]暂无歌词";
        }

        return {
            lrc: lrcText,
            tlyric: tlyricText
        };
    },

    /**
     * 获取排行榜热门歌曲（云音乐热歌榜 / 飙升榜）
     * 公开 API：先查 /api/toplist 找「热歌榜」动态 id，再取 /api/playlist/detail 的 tracks
     * @param {number} limit  数量
     * @returns {Array} 歌曲对象数组
     */
    getTopSongs: function(limit) {
        __fattv_native__.log("info", "[Netease] getTopSongs limit=" + limit);
        var count = Math.max(limit || 20, 20);

        var playlistId = "3778678"; // fallback：云音乐热歌榜

        if (!CONFIG.useOfficialApi) {
            // 动态获取热歌榜 id（榜单列表里 name 含「热歌」的官方榜）
            var top = apiCall("api/toplist", {}, "toplist");
            if (top && top.list) {
                for (var i = 0; i < top.list.length; i++) {
                    var t = top.list[i];
                    if (t && t.name && t.name.indexOf("热歌") >= 0 && t.id) {
                        playlistId = String(t.id);
                        break;
                    }
                }
            }
        }

        var data = apiCall("api/playlist/detail", { id: playlistId }, "getTopSongs");
        var tracks = null;
        if (data && data.result && data.result.tracks) {
            tracks = data.result.tracks;
        } else if (data && data.playlist && data.playlist.tracks) {
            tracks = data.playlist.tracks;
        } else if (data && data.songs) {
            tracks = data.songs;
        }

        if (!tracks) {
            __fattv_native__.log("warn", "[Netease] getTopSongs empty");
            return [];
        }

        var results = [];
        var n = Math.min(tracks.length, count);
        for (var j = 0; j < n; j++) {
            var mapped = mapSong(tracks[j]);
            if (mapped) {
                results.push(mapped);
            }
        }
        __fattv_native__.log("info", "[Netease] getTopSongs results: " + results.length);
        return results;
    },

    /**
     * 获取精选歌单推荐
     * @param {number} limit  数量
     * @returns {Array} [{id, name, artist, pic}]
     */
    getTopPlaylists: function(limit) {
        __fattv_native__.log("info", "[Netease] getTopPlaylists limit=" + limit);

        var data = apiCall("api/personalized/playlist", {
            limit: limit || 12
        }, "getTopPlaylists");

        var playlists = (data && data.result) ? data.result : null;
        if (!playlists) {
            __fattv_native__.log("warn", "[Netease] getTopPlaylists empty");
            return [];
        }

        var results = [];
        for (var i = 0; i < playlists.length; i++) {
            var item = playlists[i];
            if (!item || !item.id) continue;
            var creatorName = "";
            if (item.creator && item.creator.nickname) {
                creatorName = item.creator.nickname;
            }
            results.push({
                id: String(item.id),
                name: item.name || "未知歌单",
                artist: creatorName || "精选歌单",
                pic: item.picUrl || (item.coverImgUrl || "")
            });
        }
        __fattv_native__.log("info", "[Netease] getTopPlaylists results: " + results.length);
        return results;
    },

    /**
     * 获取歌单内歌曲列表
     * @param {string} playlistId  歌单 ID
     * @returns {Array} 歌曲对象数组
     */
    getPlaylistTracks: function(playlistId) {
        __fattv_native__.log("info", "[Netease] getPlaylistTracks: playlistId=" + playlistId);

        var data = apiCall("api/playlist/detail", { id: playlistId }, "getPlaylistTracks");
        var tracks = null;
        if (data && data.result && data.result.tracks) {
            tracks = data.result.tracks;
        } else if (data && data.playlist && data.playlist.tracks) {
            tracks = data.playlist.tracks;
        }

        if (!tracks) {
            __fattv_native__.log("warn", "[Netease] getPlaylistTracks empty");
            return [];
        }

        var results = [];
        for (var i = 0; i < tracks.length; i++) {
            var mapped = mapSong(tracks[i]);
            if (mapped) {
                results.push(mapped);
            }
        }
        __fattv_native__.log("info", "[Netease] getPlaylistTracks results: " + results.length);
        return results;
    },

    /**
     * 获取新碟/热门专辑列表
     * @param {number} limit  数量
     * @returns {Array} [{id, name, artist, pic}]
     */
    getTopAlbums: function(limit) {
        __fattv_native__.log("info", "[Netease] getTopAlbums limit=" + limit);

        var data = apiCall("api/album/new", {
            area: "ALL",
            limit: limit || 12,
            offset: 0
        }, "getTopAlbums");

        var albums = null;
        if (data) {
            if (data.albums) {
                albums = data.albums;
            } else if (data.monthData) {
                albums = data.monthData;
            } else if (data.new_albums) {
                albums = data.new_albums;
            }
        }
        if (!albums) {
            __fattv_native__.log("warn", "[Netease] getTopAlbums empty");
            return [];
        }

        var results = [];
        for (var i = 0; i < albums.length; i++) {
            var item = albums[i];
            if (!item || !item.id) continue;
            var artistName = "";
            if (item.artist) {
                artistName = item.artist.name || "";
            } else if (item.artists && item.artists.length > 0) {
                artistName = item.artists[0].name || "";
            }
            results.push({
                id: String(item.id),
                name: item.name || "未知专辑",
                artist: artistName || "未知艺人",
                pic: item.picUrl || (item.blurPicUrl || "")
            });
        }
        __fattv_native__.log("info", "[Netease] getTopAlbums results: " + results.length);
        return results;
    },

    /**
     * 获取热门 MV 列表
     * @param {number} limit  数量
     * @returns {Array} [{id, name, artist, pic, duration}]
     */
    getTopMvs: function(limit) {
        __fattv_native__.log("info", "[Netease] getTopMvs limit=" + limit);

        var data = apiCall("api/mv/exclusive/rcmd", {
            limit: limit || 12
        }, "getTopMvs");

        var mvs = (data && data.data) ? data.data : null;
        if (!mvs) {
            __fattv_native__.log("warn", "[Netease] getTopMvs empty");
            return [];
        }

        var results = [];
        for (var i = 0; i < mvs.length; i++) {
            var item = mvs[i];
            if (!item || !item.id) continue;
            var mv = item.data || item;
            if (!mv || !mv.id) continue;
            results.push({
                id: String(mv.id),
                name: mv.name || (mv.title || "未知MV"),
                artist: mv.artistName || (mv.artist ? mv.artist.name : "") || "",
                pic: mv.cover || (mv.picUrl || ""),
                duration: mv.duration || 0
            });
        }
        __fattv_native__.log("info", "[Netease] getTopMvs results: " + results.length);
        return results;
    },

    /**
     * 解析 MV 播放地址
     * 公开 API：/api/mv/detail 返回 data.brs（码率->直链），取最大码率
     * @param {string} mvId  MV ID
     * @returns {Object} {url}
     */
    resolveMvUrl: function(mvId) {
        __fattv_native__.log("info", "[Netease] resolveMvUrl: mvId=" + mvId);

        var data = apiCall("api/mv/detail", { id: mvId }, "resolveMvUrl");
        if (!data || !data.data) {
            __fattv_native__.log("warn", "[Netease] resolveMvUrl empty");
            return { url: "" };
        }

        var mvUrl = data.data.url || "";
        if (!mvUrl && data.data.brs) {
            var maxRate = -1;
            var brsUrl = "";
            for (var key in data.data.brs) {
                if (data.data.brs.hasOwnProperty(key)) {
                    var rate = parseInt(key, 10) || 0;
                    if (rate > maxRate && data.data.brs[key]) {
                        maxRate = rate;
                        brsUrl = data.data.brs[key];
                    }
                }
            }
            mvUrl = brsUrl;
        }

        __fattv_native__.log("info", "[Netease] resolveMvUrl OK, url length=" + mvUrl.length);
        return { url: mvUrl };
    },

    /**
     * 获取专辑内歌曲列表
     * 公开 API：优先 /api/album/{id}；若被风控（code=-462），降级用专辑名搜索并过滤 album.id
     * @param {string} albumId    专辑 ID
     * @param {string} albumName  专辑名（降级搜索用，可空）
     * @returns {Array} 歌曲对象数组
     */
    getAlbumTracks: function(albumId, albumName) {
        __fattv_native__.log("info", "[Netease] getAlbumTracks: albumId=" + albumId + " albumName=" + albumName);

        var data = apiCall("api/album/" + albumId, {}, "getAlbumTracks");
        if (data && data.songs && data.songs.length > 0) {
            var results = [];
            var songs = data.songs || [];
            for (var i = 0; i < songs.length; i++) {
                var mapped = mapSong(songs[i]);
                if (mapped) {
                    results.push(mapped);
                }
            }
            __fattv_native__.log("info", "[Netease] getAlbumTracks via album detail: " + results.length);
            return results;
        }

        // 降级：专辑详情被风控/不可用，用专辑名搜索后按 album.id 过滤
        if (!albumName) {
            __fattv_native__.log("warn", "[Netease] getAlbumTracks failed and no albumName for fallback");
            return [];
        }
        __fattv_native__.log("info", "[Netease] getAlbumTracks fallback: search albumName=" + albumName);
        var targetId = parseInt(albumId, 10);
        var searchData = apiCall("api/search/get", {
            s: albumName,
            type: 1,
            limit: 50,
            offset: 0
        }, "getAlbumTracks_fallback");

        var fallback = [];
        if (searchData && searchData.result && searchData.result.songs) {
            var found = searchData.result.songs || [];
            for (var j = 0; j < found.length; j++) {
                var song = found[j];
                var songAlbumId = 0;
                if (song.album && song.album.id) {
                    songAlbumId = song.album.id;
                }
                if (songAlbumId === targetId) {
                    var ms = mapSong(song);
                    if (ms) {
                        fallback.push(ms);
                    }
                }
            }
        }
        __fattv_native__.log("info", "[Netease] getAlbumTracks fallback results: " + fallback.length);
        return fallback;
    }
};