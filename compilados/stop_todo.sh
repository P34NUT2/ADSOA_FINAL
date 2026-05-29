#!/bin/bash
echo "=== Deteniendo células (app.jar) ==="
CELL_PIDS=$(ps aux | grep "app.jar" | grep -v grep | awk '{print $2}')
if [ -z "$CELL_PIDS" ]; then
    echo "  No hay células corriendo."
else
    for PID in $CELL_PIDS; do
        echo "  Kill PID $PID"
        kill $PID 2>/dev/null
    done
fi

echo "=== Deteniendo nodos (NODE.jar) ==="
NODE_PIDS=$(ps aux | grep "NODE.jar" | grep -v grep | awk '{print $2}')
if [ -z "$NODE_PIDS" ]; then
    echo "  No hay nodos corriendo."
else
    for PID in $NODE_PIDS; do
        echo "  Kill PID $PID"
        kill $PID 2>/dev/null
    done
fi

sleep 2

# Forzar si quedan vivos
RESTA=$(ps aux | grep -E "NODE.jar|app.jar" | grep -v grep | awk '{print $2}')
if [ ! -z "$RESTA" ]; then
    echo "=== Forzando kill -9 ==="
    for PID in $RESTA; do
        kill -9 $PID 2>/dev/null
    done
fi

echo "=== Todo detenido ==="
