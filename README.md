# ADSOA_FINAL

Proyecto final de Cómputo Distribuido — Arquitectura de Servicios Distribuidos Orientada a Agentes (ADSOA).

## Descripción

Sistema distribuido peer-to-peer implementado en Java 21. Consiste en una red de nodos que forman una malla de comunicación, celdas cliente que solicitan servicios, y celdas servidor que proveen microservicios cargados dinámicamente.

## Módulos

| Módulo | Rol |
|--------|-----|
| **NODE** | Nodo de la malla. Reenvía mensajes (bytes opacos). No interpreta protocolo. |
| **CELL** | Celda cliente. Menú interactivo, envía peticiones, espera respuestas. |
| **SERVER_CELL** | Celda servidora. Carga microservicios dinámicamente (.jar) y los ejecuta. |
| **SERVICES** | Microservicios aritméticos (Suma, Resta, Multiplicacion, Division). |

Ver [mini_documentation.md](mini_documentation.md) para descripción detallada de la arquitectura.

## Requisitos

- Java 21
- Maven 3.6+ (para compilar NODE y SERVICES)

## Estructura

```
PROYECTO/
├── NODE/               # Nodo de malla
├── CELL/               # Celda cliente
├── SERVER_CELL/        # Celda servidora con carga dinámica
├── SERVICES/           # Microservicios (uno por operación)
│   ├── SumaService/
│   ├── RestaService/
│   ├── MultiplicacionService/
│   └── DivisionService/
└── compilados_ejemplo/ # Ejemplo de nodos preconfigurados
```

## Compilación

### NODE

```bash
cd NODE
mvn clean package
cp target/NODE-1.0.0-jar-with-dependencies.jar compilados_ejemplo/Node1/NODE.jar
# repetir para Node2, Node3, Node4
```

### CELL y SERVER_CELL

Se compilan con `javac` directamente (sin Maven CLI). Ver [mini_documentation.md](mini_documentation.md) sección de compilación.

### SERVICES

```bash
cd SERVICES/SumaService
mvn clean package
# genera SumaService-1.0.0-jar-with-dependencies.jar
```

## Configuración

### NODE — `conections.json`

```json
{
    "nodeId": "Nodo_A",
    "listener_port": 5000,
    "peers": [
        {"id": "Nodo_B", "host": "127.0.0.1", "port": 5001}
    ]
}
```

### CELL — `config.json`

```json
{
    "cellId": "ClientCell1",
    "targetNodeHost": "127.0.0.1",
    "targetNodePort": 5000,
    "minAcks": 2
}
```

### SERVER_CELL — `config.json`

```json
{
    "cellId": "ServerCell_Suma_1",
    "targetNodeHost": "127.0.0.1",
    "targetNodePort": 5000,
    "minAcks": 2,
    "servicesDir": "./services",
    "services": [
        {
            "serviceId": 1,
            "class": "org.up.cd.services.SumaService",
            "jar": "SumaService.jar"
        }
    ]
}
```

## Ejecución

```bash
# 1. Iniciar nodos
cd compilados/Node1 && java -jar NODE.jar &
cd compilados/Node2 && java -jar NODE.jar &
cd compilados/Node3 && java -jar NODE.jar &
cd compilados/Node4 && java -jar NODE.jar &

# 2. Iniciar server cells (una por operación, mínimo 2 por operación para foliado)
cd compilados/ServerCell_Suma_1 && java -jar SERVER_CELL.jar &
cd compilados/ServerCell_Suma_2 && java -jar SERVER_CELL.jar &
# ... repetir para Resta, Mult, Div

# 3. Iniciar celda cliente (interactiva)
cd compilados/ClientCell1 && java -jar CELL.jar
```

## Protocolo

### Handshake

```
HELLO:NODE:<id>    # nodo a nodo
HELLO:CELL:<id>    # celda a nodo (tanto CELL como SERVER_CELL)
```

### Regla de Oro

- Mensaje de **NODO** → reenvía solo a celdas locales
- Mensaje de **CELDA** → inundación total (todos los nodos y celdas)

### Protocolo Binario

Formato de cada mensaje:

| Campo | Tamaño |
|-------|--------|
| originBusiness | 16 bytes |
| originSubsystem | 16 bytes |
| originEntity (huella) | 16 bytes |
| destBusiness | 16 bytes |
| destSubsystem | 16 bytes |
| destEntity | 16 bytes |
| eventId | 8 bytes (long) |
| serviceNumber | 4 bytes (int) |
| dataLength | 4 bytes (int) |
| data | N bytes |

### Números de servicio

| Valor | Significado |
|-------|-------------|
| `+N` | Petición (cliente → servidor), N = 1/2/3/4 |
| `0` | ACK de confirmación (foliado) |
| `-N` | Respuesta (servidor → cliente) |

## Dependencias

- Log4j2 2.23.1
- Jackson 2.17.0

## Autor

Antonio V - 0264679@up.edu.mx
