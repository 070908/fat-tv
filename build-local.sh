#!/bin/bash
# build-local.sh — fat TV 本地后台构建脚本
# 用途：解决 bash tool 超时限制（默认 8min 对首次构建不够），后台运行 Gradle 并轮询产物
# 使用方式：bash build-local.sh [clean]

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"

APK_OUT="app/build/outputs/apk/debug/app-debug.apk"
LOG_FILE=".gradle/build-local.log"
PID_FILE=".gradle/build-local.pid"

# 可选 clean
if [ "$1" = "clean" ]; then
    echo "[$(date '+%H:%M:%S')] Cleaning build outputs..."
    rm -rf app/build/outputs/apk/debug/
fi

# 杀掉残留 daemon
jps 2>/dev/null | grep GradleDaemon | awk '{print $1}' | xargs -r kill -9 2>/dev/null || true

# 后台启动构建
nohup bash -c '
    export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
    export PATH="$JAVA_HOME/bin:$PATH"
    cd "'"$SCRIPT_DIR"'"
    ./gradlew assembleDebug --no-daemon --info > "'"$LOG_FILE"'" 2>&1
' > /dev/null 2>&1 &
BUILD_PID=$!
echo $BUILD_PID > "$PID_FILE"
echo "[$(date '+%H:%M:%S')] Build started in background (PID: $BUILD_PID)"
echo "[$(date '+%H:%M:%S')] Log: $SCRIPT_DIR/$LOG_FILE"

# 轮询等待产物
TIMEOUT_SECS=900  # 15 分钟上限
INTERVAL=10
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT_SECS ]; do
    if [ -f "$APK_OUT" ]; then
        SIZE=$(ls -lh "$APK_OUT" | awk '{print $5}')
        echo "[$(date '+%H:%M:%S')] BUILD SUCCESS — APK: $APK_OUT ($SIZE)"
        echo "[$(date '+%H:%M:%S')] To install: adb install -r '$APK_OUT'"
        exit 0
    fi
    if ! ps -p $BUILD_PID > /dev/null 2>&1; then
        echo "[$(date '+%H:%M:%S')] Build process exited. Checking log..."
        if grep -q "BUILD SUCCESSFUL" "$LOG_FILE" 2>/dev/null; then
            SIZE=$(ls -lh "$APK_OUT" 2>/dev/null | awk '{print $5}')
            echo "[$(date '+%H:%M:%S')] BUILD SUCCESS — APK: $APK_OUT ($SIZE)"
            exit 0
        else
            echo "[$(date '+%H:%M:%S')] BUILD FAILED — see $LOG_FILE"
            tail -30 "$LOG_FILE"
            exit 1
        fi
    fi
    sleep $INTERVAL
    ELAPSED=$((ELAPSED + INTERVAL))
    echo "[$(date '+%H:%M:%S')] Waiting... (${ELAPSED}s elapsed)"
done

echo "[$(date '+%H:%M:%S')] TIMEOUT after ${TIMEOUT_SECS}s — build may still be running (PID: $BUILD_PID)"
echo "Check manually: tail -f '$LOG_FILE'"
exit 2
