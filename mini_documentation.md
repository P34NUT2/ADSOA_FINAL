# Mini Documentación — ADSOA

## ¿Qué es ADSOA?

Arquitectura de Servicios Distribuidos Orientada a Agentes. Cada componente es un agente autónomo que se comunica por mensajes binarios a través de una malla de nodos TCP.

---

## Componentes

### NODE (Nodo de malla)

El nodo es un **relay de bytes**. No interpreta el contenido de los mensajes, solo sabe quién los envía (NODE o CELL) gracias al handshake inicial.

**Responsabilidades:**
- Aceptar conexiones de otros nodos y celdas
- Registrar cada conexión con su tipo e identidad
- Reenviar mensajes según la Regla de Oro
- Reconectar a peers caídos cada 5 segundos

**Regla de Oro (reenvío):**
- Mensaje de **NODO** → solo a celdas conectadas localmente
- Mensaje de **CELDA** → inundación total: todos los nodos y todas las celdas

```
      [Node1] ←───── CELL envía ────→ flood a todos
      /      \
  [Node2]  [Node3]          ← nodos reenvían solo a sus celdas locales
    |           |
[ServerCell] [ServerCell]   ← reciben el mensaje
```

**Handshake:**
```
HELLO:NODE:<nodeId>   # conexión nodo a nodo
HELLO:CELL:<cellId>   # conexión celda a nodo (CELL o SERVER_CELL, es lo mismo para el nodo)
```

**Configuración (`conections.json`):**
```json
{
    "nodeId": "Nodo_A",
    "listener_port": 5000,
    "peers": [
        {"id": "Nodo_B", "host": "127.0.0.1", "port": 5001},
        {"id": "Nodo_C", "host": "127.0.0.1", "port": 5002}
    ]
}
```

---

### CELL (Celda cliente)

La celda cliente es el **punto de entrada del usuario**. Tiene un menú interactivo para elegir operaciones y enviarlas a la red.

**¿Por qué existe CELL separado de SERVER_CELL?**
Desde el punto de vista del nodo son idénticas (ambas se conectan con `HELLO:CELL`). La diferencia es de rol:
- **CELL** = consume servicios (solicita, espera respuesta)
- **SERVER_CELL** = provee servicios (escucha, ejecuta, responde)

**Flujo interno de CELL:**

```
MenuThread
   └── OutputQueueManager (cola por serviceId)
           └── SenderThread
                   ├── asigna eventId (AtomicLong)
                   ├── envía mensaje por MeshConnection
                   └── espera minAcks ACKs únicos (CountDownLatch)
                           ↑
ReceiverThread ────────────┘  (svc==0 → ACK, svc<0 → respuesta)
   └── InputQueueManager
           └── DispatchThread → imprime resultado
```

**Protocolo de foliado (acuses):**

Antes de enviar el siguiente mensaje, el `SenderThread` espera recibir exactamente `minAcks` acuses de recibo (ACKs) de distintas celdas servidoras. Esto garantiza que el mensaje llegó y fue recibido por suficientes nodos antes de continuar.

1. `SenderThread` asigna `eventId` único al mensaje
2. Envía el mensaje a la red
3. Espera a que `minAcks` celdas servidoras distintas respondan con `serviceNumber=0` y el mismo `eventId`
4. Deduplicación por **huella** (`cellId`): si la misma celda manda dos ACKs para el mismo evento, solo cuenta una vez
5. Al llegar `minAcks` ACKs únicos → desbloquea → envía siguiente

**Configuración (`config.json`):**
```json
{
    "cellId": "ClientCell1",
    "targetNodeHost": "127.0.0.1",
    "targetNodePort": 5000,
    "minAcks": 2
}
```

**Hilos:**

| Hilo | Función |
|------|---------|
| `SenderThread` | Dequeue mensajes, envía, espera ACKs (foliado) |
| `ReceiverThread` | Lee del socket, distribuye ACKs y respuestas |
| `DispatchThread` | Consume respuestas e imprime resultado |
| `MenuThread` | Menú interactivo de usuario |

---

### SERVER_CELL (Celda servidora)

La celda servidora es el **proveedor de microservicios**. No tiene UI. Escucha peticiones, las filtra por `serviceId`, ejecuta el microservicio correspondiente y responde.

**Flujo interno de SERVER_CELL:**

```
ReceiverThread
   ├── filtra: ¿handlesService(svc)? → si no, ignora silenciosamente
   ├── manda ACK (serviceNumber=0, eventId=mismo, origin=mi cellId)
   └── InputQueueManager
           └── DispatchThread
                   ├── DynamicServiceLoader.execute(svc, data)
                   │       └── URLClassLoader → carga JAR → reflection → execute(byte[])
                   └── responde con header flip + serviceNumber negado
```

**Filtro por número de operación:**
Cada `SERVER_CELL` tiene configurado uno o más `serviceId`. Cuando llega un mensaje:
- `serviceId` coincide → manda ACK + procesa
- `serviceId` no coincide → ignora (sin ACK, sin respuesta)

Esto permite que múltiples celdas servidoras coexistan en la red sin interferirse.

**Header flip (volteo de cabeceras):**
Al responder, la celda servidora intercambia origen y destino:
```
Request:  origin=ClientCell1  →  dest=SERVER_CELL
Response: origin=ServerCell_Suma_1  →  dest=ClientCell1
          serviceNumber = -serviceNumber (negado = respuesta)
```
El cliente filtra respuestas verificando que `destEntity == su propio cellId` (la **huella**).

**Carga dinámica de microservicios:**
```java
// Primera llamada para serviceId=1:
URLClassLoader loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, ...);
Class<?> clazz = loader.loadClass("org.up.cd.services.SumaService");
Method method = clazz.getMethod("execute", byte[].class);
// Cachea instancia y método para siguientes llamadas
```
Los microservicios son JARs independientes con un método `public byte[] execute(byte[] input)`. No se necesita interfaz compartida — pura reflexión.

**Configuración (`config.json`):**
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

**Hilos:**

| Hilo | Función |
|------|---------|
| `ReceiverThread` | Lee del socket, filtra por serviceId, manda ACK, encola |
| `DispatchThread` | Ejecuta microservicio, envía respuesta con header flip |

---

### SERVICES (Microservicios)

Son JARs independientes sin dependencias. Cada uno tiene una sola clase con un método:

```java
public byte[] execute(byte[] input) {
    String expr = new String(input); // e.g. "10.0 + 5.0"
    // ... procesa ...
    return result.getBytes();
}
```

| Servicio | serviceId | Operación |
|----------|-----------|-----------|
| SumaService | 1 | `a + b` |
| RestaService | 2 | `a - b` |
| MultiplicacionService | 3 | `a * b` |
| DivisionService | 4 | `a / b` |

---

## Protocolo Binario

Formato fijo de cada mensaje en la red:

```
[originBusiness  : 16 bytes]
[originSubsystem : 16 bytes]
[originEntity    : 16 bytes]  ← huella del emisor
[destBusiness    : 16 bytes]
[destSubsystem   : 16 bytes]
[destEntity      : 16 bytes]  ← huella del receptor esperado
[eventId         : 8 bytes ]  ← número de evento (foliado)
[serviceNumber   : 4 bytes ]  ← +N petición / 0 ACK / -N respuesta
[dataLength      : 4 bytes ]
[data            : N bytes ]
```

**Huella (cellId):** Identidad única de cada celda. Sirve para:
1. Deduplicar ACKs — una celda no puede acusar el mismo evento dos veces
2. Filtrar respuestas — el cliente descarta mensajes cuyo `destEntity` no coincida con su `cellId`
3. Header flip — la respuesta lleva el `cellId` del servidor como origen y el `cellId` del cliente como destino

**Número de evento (eventId):** Contador `AtomicLong` incrementado por mensaje. Permite al `SenderThread` asociar cada ACK que llega con el mensaje correcto que está esperando.

---

## Despliegue de ejemplo

```
Node1 (:5000) ─── Node2 (:5001)
   |                    |
ServerCell_Suma_1    ServerCell_Suma_2
ServerCell_Mult_1    ServerCell_Mult_2
ClientCell1

Node3 (:5002) ─── Node4 (:5003)
   |                    |
ServerCell_Resta_1   ServerCell_Resta_2
ServerCell_Div_1     ServerCell_Div_2
ClientCell2
```

Con `minAcks=2`: cada operación tiene 2 celdas servidoras. El cliente espera 1 ACK de cada una antes de continuar. Las dos procesan la petición y el cliente recibe 2 respuestas (una de cada celda).

---

## Autor

Antonio V - 0264679@up.edu.mx
