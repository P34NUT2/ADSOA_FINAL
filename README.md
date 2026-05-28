# ADSOA_FINAL

Proyecto final de la materia de Cómputo Distribuido.

## Descripción

Sistema distribuido de comunicación peer-to-peer implementado en Java con Maven. El proyecto consiste en una red de nodos que se comunican entre sí utilizando sockets TCP, implementando un protocolo de handshake y una regla de reenvío de mensajes ("Regla de Oro").

### Arquitectura

El proyecto está organizado en tres módulos principales:

- **NODE**: Nodos de la red que forman la malla de comunicación y reenvían mensajes
- **CELL**: Aplicaciones cliente que se conectan a los nodos
- **SERVER_CELL**: Aplicaciones servidor (en desarrollo)

## Requisitos

- Java 21 o superior
- Maven 3.6+

## Estructura del Proyecto

```
PROYECTO/
├── NODE/               # Módulo principal del nodo
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── org/up/cd/
│   │       │       ├── NodoApp.java
│   │       │       └── network/
│   │       │           ├── Connections.java
│   │       │           └── SocketHandler.java
│   │       └── resources/
│   │           ├── conections.json
│   │           └── log4j2.xml
│   └── pom.xml
├── CELL/               # Módulo de células (cliente)
└── SERVER_CELL/        # Módulo de células servidor
```

## Compilación

### Compilar el proyecto NODE:

```bash
cd NODE
mvn clean package
```

El JAR compilado se genera en:
- `NODE/target/NODE-1.0.0.jar` (requiere dependencias en lib/)
- `NODE/target/NODE-1.0.0-jar-with-dependencies.jar` (JAR completo - recomendado)

### Preparar carpeta de ejecución:

Después de compilar, debes copiar el JAR a cada carpeta de nodo:

```bash
# Desde la raíz del proyecto
cp NODE/target/NODE-1.0.0-jar-with-dependencies.jar compilados_ejemplo/Node1/NODE.jar
cp NODE/target/NODE-1.0.0-jar-with-dependencies.jar compilados_ejemplo/Node2/NODE.jar
cp NODE/target/NODE-1.0.0-jar-with-dependencies.jar compilados_ejemplo/Node3/NODE.jar
cp NODE/target/NODE-1.0.0-jar-with-dependencies.jar compilados_ejemplo/Node4/NODE.jar
```

Cada carpeta de nodo ya incluye su archivo `conections.json` preconfigurado.

## Configuración

Cada nodo requiere un archivo `conections.json` con la siguiente estructura:

```json
{
    "nodeId": "Nodo_B",
    "listener_port": 5001,
    "peers": [
        {
            "id": "Nodo_A",
            "host": "127.0.0.1",
            "port": 5000
        },
        {
            "id": "Nodo_C",
            "host": "127.0.0.1",
            "port": 5002
        },
        {
            "id": "Nodo_D",
            "host": "127.0.0.1",
            "port": 5003
        }
    ]
}
```

### Parámetros de configuración:

- **nodeId**: Identificador único del nodo
- **listener_port**: Puerto en el que el nodo escuchará conexiones entrantes
- **peers**: Lista de nodos pares a los que se conectará automáticamente
  - **id**: Identificador del nodo par
  - **host**: Dirección IP o hostname del nodo par
  - **port**: Puerto del nodo par

## Ejecución

### Ejecución en Linux/macOS

Usar los scripts proporcionados:

```bash
cd compilados_ejemplo

# Iniciar todos los nodos
./run_all_nodes.sh

# Detener todos los nodos
./stop_all_nodes.sh

# Limpiar logs
./clean_logs.sh
```

### Ejecución en Windows

Los scripts `.sh` NO funcionan en Windows. Debes ejecutar cada nodo manualmente:

**Opción 1: Usar múltiples ventanas de CMD o PowerShell**

Abrir 4 ventanas y ejecutar en cada una:

```cmd
REM Ventana 1
cd compilados_ejemplo\Node1
java -jar NODE.jar

REM Ventana 2
cd compilados_ejemplo\Node2
java -jar NODE.jar

REM Ventana 3
cd compilados_ejemplo\Node3
java -jar NODE.jar

REM Ventana 4
cd compilados_ejemplo\Node4
java -jar NODE.jar
```

**Opción 2: Usar WSL (Windows Subsystem for Linux)**

Si tienes WSL instalado, puedes usar los scripts .sh normalmente.

Para detener: Presionar `Ctrl+C` en cada ventana.

### Ejecución manual (cualquier sistema operativo)

#### Opción 1: Desde el directorio con conections.json

El nodo buscará el archivo `conections.json` en el directorio actual:

```bash
cd /ruta/carpeta/con/config
java -jar NODE.jar
```

#### Opción 2: Especificando la ruta del archivo de configuración

```bash
java -jar NODE.jar /ruta/al/conections.json
```

#### Opción 3: Modo escucha sin configuración

Si no se encuentra archivo de configuración, el nodo iniciará en modo escucha en el puerto 9999:

```bash
java -jar NODE.jar
```

Salida esperada:
```
CONFIGURATION NOT FOUND - Starting in LISTENING MODE ONLY
Starting in LISTENING-ONLY mode on default port 9999
```

## Carpeta de Compilados de Ejemplo

La carpeta `compilados_ejemplo/` contiene la estructura lista para usar con los nodos:

```
compilados_ejemplo/
├── Node1/
│   ├── conections.json    # Configuración para Nodo_A (puerto 5000)
│   ├── logs/              # Se crea automáticamente al ejecutar
│   └── NODE.jar           # DEBES COPIAR AQUÍ después de compilar
├── Node2/
│   ├── conections.json    # Configuración para Nodo_B (puerto 5001)
│   ├── logs/
│   └── NODE.jar           # DEBES COPIAR AQUÍ después de compilar
├── Node3/
│   ├── conections.json    # Configuración para Nodo_C (puerto 5002)
│   ├── logs/
│   └── NODE.jar           # DEBES COPIAR AQUÍ después de compilar
├── Node4/
│   ├── conections.json    # Configuración para Nodo_D (puerto 5003)
│   ├── logs/
│   └── NODE.jar           # DEBES COPIAR AQUÍ después de compilar
├── run_all_nodes.sh       # Script para iniciar todos los nodos (Linux/macOS)
├── stop_all_nodes.sh      # Script para detener todos los nodos (Linux/macOS)
├── clean_logs.sh          # Script para limpiar archivos de log (Linux/macOS)
└── README.txt             # Instrucciones detalladas
```

**IMPORTANTE:** Los archivos `NODE.jar` NO están incluidos en el repositorio. Debes compilar el proyecto y copiarlos manualmente siguiendo las instrucciones de compilación.

### Por qué los scripts .sh solo funcionan en Linux/macOS

Los archivos `.sh` (shell scripts) son scripts de Bash que solo pueden ejecutarse en sistemas Unix-like:
- Linux
- macOS
- Windows con WSL (Windows Subsystem for Linux)

Windows nativo (CMD o PowerShell) NO puede ejecutar archivos `.sh` directamente porque usa un intérprete de comandos diferente. En Windows debes ejecutar cada nodo manualmente o crear archivos `.bat` equivalentes.

## Archivos de Log

Los logs se generan en la carpeta `logs/` dentro del directorio de ejecución:
- Archivo principal: `logs/node.log`
- Rotación automática cuando el archivo alcanza 250 MB
- Archivos históricos comprimidos en: `logs/YYYY-MM/node-MM-dd-yyyy-N.log.gz`

## Protocolo de Comunicación

### Handshake

Cuando un nodo o célula se conecta, debe enviar un mensaje de identificación:
```
HELLO:NODE:<id>    # Para nodos
HELLO:CELL:<id>    # Para células
```

### Regla de Oro (Reenvío de Mensajes)

- **Mensajes de NODO**: Se reenvían únicamente a células conectadas localmente
- **Mensajes de CÉLULA**: Se reenvían a todos los nodos y todas las células (inundación total)

## Características Técnicas

- **Thread-safe**: Uso de `ConcurrentHashMap` para gestión de conexiones
- **Reconexión automática**: Monitor que verifica conexiones cada 5 segundos
- **Manejo de bytes**: Los nodos reenvían datos sin interpretar el protocolo
- **Shutdown hooks**: Liberación correcta de recursos al terminar
- **Logs estructurados**: Log4j2 con rotación automática

## Dependencias

- Jackson 2.17.0 (Procesamiento JSON)
- Log4j2 2.23.1 (Sistema de logging)

## Autor

Antonio V - 0264679@up.edu.mx

## Notas

- El proyecto utiliza Java 21 con soporte para características modernas
- Los nodos mantienen conexiones persistentes con sus pares configurados
- Las células son consideradas clientes transitorios
- El sistema está diseñado para manejar redes malladas de nodos
