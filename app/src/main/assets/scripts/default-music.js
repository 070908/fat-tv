/**
 * fat TV 默认公网音源插件示例
 * 规范：CommonJS 模块，必须导出 search / resolveUrl / getLyric
 * 注意：这是一个示范/测试脚本，实际使用时需对接真实音源 API
 */

var module = module || {};

module.exports = {
    id: "demo",
    name: "示例音源",
    version: 1,

    /**
     * 搜索歌曲
     * @param {string} keyword 关键词
     * @param {number} page 页码（从1开始）
     * @param {number} limit 每页数量
     * @returns {Array} 歌曲对象数组 [{id, name, artist, album, duration, pic}]
     */
    search: function(keyword, page, limit) {
        __fattv_native__.log("info", "search called: " + keyword + " page=" + page);

        // 这里接入真实搜索 API，例如：
        // var resp = __fattv_native__.http_get(
        //     "https://api.example.com/search?key=" + encodeURIComponent(keyword) + "&page=" + page,
        //     JSON.stringify({ "User-Agent": "fatTV/1.0" })
        // );
        // var data = JSON.parse(resp);
        // return data.songs || [];

        // 当前返回占位数据，用于 UI 测试
        var results = [];
        for (var i = 0; i < Math.min(limit, 5); i++) {
            results.push({
                id: "demo_" + page + "_" + i,
                name: keyword + " - 演示曲目 " + (i + 1),
                artist: "演示艺人",
                album: "演示专辑",
                duration: 180000,
                pic: ""
            });
        }
        return results;
    },

    /**
     * 解析歌曲播放地址
     * @param {string} songId 歌曲 ID
     * @param {string} quality 音质: 128k / 320k / flac
     * @returns {Object} {url, headers, bitrate}
     */
    resolveUrl: function(songId, quality) {
        __fattv_native__.log("info", "resolveUrl called: " + songId + " quality=" + quality);

        // 这里接入真实解析 API，例如：
        // var resp = __fattv_native__.http_get(
        //     "https://api.example.com/url?id=" + encodeURIComponent(songId) + "&quality=" + quality,
        //     "{}"
        // );
        // var data = JSON.parse(resp);
        // return { url: data.url, headers: data.headers || {}, bitrate: data.bitrate };

        // 占位：返回测试音频（版权自由音频）
        return {
            url: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            headers: {},
            bitrate: quality === "flac" ? "1411k" : (quality === "320k" ? "320k" : "128k")
        };
    },

    /**
     * 获取歌词
     * @param {string} songId 歌曲 ID
     * @returns {Object} {lrc, tlyric(可选)}
     */
    getLyric: function(songId) {
        __fattv_native__.log("info", "getLyric called: " + songId);

        // 这里接入真实歌词 API，例如：
        // var resp = __fattv_native__.http_get(
        //     "https://api.example.com/lyric?id=" + encodeURIComponent(songId),
        //     "{}"
        // );
        // var data = JSON.parse(resp);
        // return { lrc: data.lrc, tlyric: data.tlyric || "" };

        return {
            lrc: "[00:00.00]暂无歌词\n[00:01.00]请在设置中导入真实音源\n[00:05.00]或配置局域网后端",
            tlyric: ""
        };
    }
};
