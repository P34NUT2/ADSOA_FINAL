#!/bin/bash

echo "==========================================="
echo "Limpiando archivos de log..."
echo "==========================================="

# Contador de archivos eliminados
count=0

# Limpiar logs en cada carpeta de nodo
for node_dir in Node1 Node2 Node3 Node4; do
    if [ -d "$node_dir/logs" ]; then
        echo "Limpiando $node_dir/logs/..."

        # Contar archivos antes de eliminar
        files=$(find "$node_dir/logs" -type f 2>/dev/null | wc -l)

        if [ $files -gt 0 ]; then
            # Eliminar todos los archivos de log
            rm -rf "$node_dir/logs/"*
            count=$((count + files))
            echo "  ✓ $files archivo(s) eliminado(s)"
        else
            echo "  - Sin archivos de log"
        fi
    else
        echo "$node_dir/logs/ no existe"
    fi
done

echo ""
if [ $count -gt 0 ]; then
    echo "✓ Total: $count archivo(s) de log eliminado(s)"
else
    echo "No se encontraron archivos de log para eliminar"
fi

echo "==========================================="
