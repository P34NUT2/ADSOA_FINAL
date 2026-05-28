#!/bin/bash

echo "==========================================="
echo "Deteniendo todos los nodos..."
echo "==========================================="

# Buscar todos los procesos Java que ejecutan NODE.jar
PIDS=$(ps aux | grep "NODE.jar" | grep -v grep | awk '{print $2}')

if [ -z "$PIDS" ]; then
    echo "No se encontraron nodos en ejecución."
else
    echo "Procesos encontrados:"
    ps aux | grep "NODE.jar" | grep -v grep
    echo ""
    echo "Deteniendo procesos..."

    # Detener cada proceso
    for PID in $PIDS; do
        echo "  - Deteniendo PID $PID"
        kill $PID 2>/dev/null
    done

    # Esperar un momento
    sleep 2

    # Verificar si alguno sigue vivo y forzar si es necesario
    REMAINING=$(ps aux | grep "NODE.jar" | grep -v grep | awk '{print $2}')
    if [ ! -z "$REMAINING" ]; then
        echo ""
        echo "Algunos procesos no respondieron, forzando detención..."
        for PID in $REMAINING; do
            echo "  - Forzando detención de PID $PID"
            kill -9 $PID 2>/dev/null
        done
    fi

    echo ""
    echo "✓ Todos los nodos han sido detenidos."
fi

echo "==========================================="
