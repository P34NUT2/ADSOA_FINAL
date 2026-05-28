CARPETA DE COMPILADOS - INSTRUCCIONES
======================================

Esta carpeta contiene la estructura de ejemplo para ejecutar los nodos compilados.

IMPORTANTE: Los archivos NODE.jar NO están incluidos aquí.
Debes compilarlos y copiarlos manualmente siguiendo las instrucciones a continuación.

ESTRUCTURA:
-----------
compilados_ejemplo/
├── Node1/
│   ├── conections.json    (Nodo_A - puerto 5000)
│   ├── logs/              (carpeta para logs - se crea automáticamente)
│   └── NODE.jar           (COPIAR AQUÍ después de compilar)
├── Node2/
│   ├── conections.json    (Nodo_B - puerto 5001)
│   ├── logs/
│   └── NODE.jar           (COPIAR AQUÍ después de compilar)
├── Node3/
│   ├── conections.json    (Nodo_C - puerto 5002)
│   ├── logs/
│   └── NODE.jar           (COPIAR AQUÍ después de compilar)
├── Node4/
│   ├── conections.json    (Nodo_D - puerto 5003)
│   ├── logs/
│   └── NODE.jar           (COPIAR AQUÍ después de compilar)
├── run_all_nodes.sh       (Iniciar todos los nodos - SOLO LINUX/MAC)
├── stop_all_nodes.sh      (Detener todos los nodos - SOLO LINUX/MAC)
└── clean_logs.sh          (Limpiar logs - SOLO LINUX/MAC)


PASOS PARA PREPARAR:
--------------------

1. Compilar el proyecto:
   cd NODE
   mvn clean package

2. Copiar el JAR compilado a cada carpeta de nodo:
   cp NODE/target/NODE-1.0.0-jar-with-dependencies.jar compilados_ejemplo/Node1/NODE.jar
   cp NODE/target/NODE-1.0.0-jar-with-dependencies.jar compilados_ejemplo/Node2/NODE.jar
   cp NODE/target/NODE-1.0.0-jar-with-dependencies.jar compilados_ejemplo/Node3/NODE.jar
   cp NODE/target/NODE-1.0.0-jar-with-dependencies.jar compilados_ejemplo/Node4/NODE.jar

3. Ejecutar los nodos:

   LINUX/MAC:
   cd compilados_ejemplo
   ./run_all_nodes.sh

   WINDOWS:
   Abrir 4 ventanas de CMD o PowerShell y en cada una ejecutar:
   cd compilados_ejemplo\Node1
   java -jar NODE.jar

   cd compilados_ejemplo\Node2
   java -jar NODE.jar

   cd compilados_ejemplo\Node3
   java -jar NODE.jar

   cd compilados_ejemplo\Node4
   java -jar NODE.jar


DETENER NODOS:
--------------

LINUX/MAC:
./stop_all_nodes.sh

WINDOWS:
Presionar Ctrl+C en cada ventana de CMD/PowerShell


NOTA SOBRE SCRIPTS .sh:
-----------------------
Los scripts .sh son archivos de shell script que SOLO funcionan en Linux y macOS.
Windows NO puede ejecutar estos scripts directamente.

Si estás en Windows, debes:
- Ejecutar cada nodo manualmente en ventanas separadas de CMD/PowerShell
- O usar WSL (Windows Subsystem for Linux) para ejecutar los scripts .sh
