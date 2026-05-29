#!/bin/bash
echo "=== Stopping cells ==="

stop_jar() {
    local JAR=$1
    local PIDS=$(ps aux | grep "$JAR" | grep -v grep | awk '{print $2}')
    if [ -z "$PIDS" ]; then
        echo "  [$JAR] No processes running."
    else
        for PID in $PIDS; do
            echo "  [$JAR] Killing PID $PID"
            kill $PID 2>/dev/null
        done
    fi
}

stop_jar "CELL.jar"
stop_jar "SERVER_CELL.jar"

sleep 2

# Force kill if still alive
REMAINING=$(ps aux | grep -E "CELL\.jar|SERVER_CELL\.jar" | grep -v grep | awk '{print $2}')
if [ -n "$REMAINING" ]; then
    echo "  Force kill -9..."
    for PID in $REMAINING; do
        kill -9 $PID 2>/dev/null
    done
fi

echo "=== Cells stopped ==="
