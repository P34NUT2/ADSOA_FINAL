# Script — Video de presentación ADSOA

> **Formato:** narración en voz + indicaciones de pantalla entre corchetes `[PANTALLA: ...]`
> **Duración estimada:** 4–6 minutos
> **Tono:** técnico pero claro, como si le explicaras a un compañero de carrera

---

## INTRO (0:00 – 0:30)

[PANTALLA: Título animado — "ADSOA: Arquitectura de Servicios Distribuidos Orientada a Agentes"]

> "En los sistemas distribuidos tradicionales existe un problema fundamental: si el servidor central cae, todo el sistema cae con él. El punto único de falla es el talón de Aquiles de cualquier arquitectura centralizada."

> "En este proyecto implementamos ADSOA — una arquitectura donde no existe ningún nodo maestro. Cada componente es un agente autónomo que se comunica por mensajes sobre una malla TCP. Si un nodo cae, la malla sigue funcionando."

---

## ARQUITECTURA GENERAL (0:30 – 1:30)

[PANTALLA: Diagrama de la malla — 4 nodos conectados, con celdas colgando de cada nodo]

> "El sistema tiene cuatro tipos de componentes:"

> "Primero, los **NODEs** — los nodos de la malla. Son retransmisores de bytes puros. No entienden el contenido de los mensajes, solo saben si quien les habla es otro nodo o una celda, gracias a un handshake inicial."

[PANTALLA: Highlight del NODE en el diagrama]

> "Segundo, las **CELLs** — las celdas cliente. Son el punto de entrada del usuario. Tienen un menú interactivo para elegir operaciones y se encargan de enviarlas a la red."

[PANTALLA: Highlight de ClientCell en el diagrama]

> "Tercero, las **SERVER_CELLs** — las celdas servidoras. Escuchan peticiones, las filtran por tipo de servicio, ejecutan el microservicio correspondiente y responden."

[PANTALLA: Highlight de ServerCells en el diagrama]

> "Y cuarto, los **SERVICES** — microservicios aritméticos: suma, resta, multiplicación y división. Cada uno es un JAR independiente que se carga en tiempo de ejecución."

---

## LA REGLA DE ORO DEL NODO (1:30 – 2:00)

[PANTALLA: Animación del flujo de mensajes en la malla]

> "El comportamiento de reenvío del nodo sigue una regla simple pero poderosa:"

> "Si el mensaje viene de **otro nodo**, lo reenvía únicamente a las celdas conectadas localmente."

> "Si el mensaje viene de **una celda**, hace inundación total — lo manda a todos los nodos y todas las celdas de la malla."

> "Esto garantiza que cualquier celda servidora competente para atender una petición la va a recibir, sin que el cliente tenga que saber de antemano dónde están los servidores."

---

## PROTOCOLO BINARIO (2:00 – 2:30)

[PANTALLA: Tabla del formato binario del mensaje]

> "La comunicación usa un protocolo binario de longitud fija. Cada mensaje tiene una cabecera de 84 bytes: origen, destino, el identificador de evento, el número de servicio y la longitud del payload."

> "El número de servicio tiene tres estados posibles: positivo para una petición, cero para un acuse de recibo, y negativo para una respuesta. Simple y sin ambigüedades."

> "Dos conceptos clave del protocolo son la **huella** y el **evento**. La huella es el ID único de cada celda — permite deduplicar acuses y filtrar respuestas. El evento es el número de secuencia de cada petición — permite al emisor saber qué acuses corresponden a qué mensaje."

---

## PROTOCOLO DE FOLIADO (2:30 – 3:15)

[PANTALLA: Diagrama de flujo del foliado — CELL → red → SERVER_CELL → ACK → CELL]

> "El protocolo de foliado es el mecanismo de entrega garantizada del sistema."

> "Cuando la celda cliente quiere enviar un mensaje, primero le asigna un número de evento único. Luego lo envía a la red y se bloquea esperando recibir un mínimo de acuses de recibo — `minAcks` — de celdas servidoras **distintas**."

> "La deduplicación es importante: si la misma celda servidora manda dos acuses para el mismo evento, solo cuenta como uno. Esto previene que una celda rápida sature el contador."

> "Solo cuando llegan `minAcks` acuses únicos, el emisor desbloquea y puede enviar el siguiente mensaje. Esto garantiza que los mensajes no se pierden en tránsito."

[PANTALLA: Código de SenderThread — CountDownLatch esperando ACKs]

> "En código, esto se implementa con un `CountDownLatch` en el `SenderThread`. Elegante y thread-safe."

---

## CARGA DINÁMICA DE SERVICIOS (3:15 – 3:50)

[PANTALLA: Código de DynamicServiceLoader — URLClassLoader y reflection]

> "Una de las características más potentes del sistema es la carga dinámica de microservicios."

> "Cuando la celda servidora recibe una petición para un servicio, usa `URLClassLoader` para cargar el JAR correspondiente en tiempo de ejecución. Luego instancia la clase por reflexión y llama al método `execute`."

> "El contrato es implícito: cualquier clase con un método `public byte[] execute(byte[] input)` puede funcionar como microservicio. No hay interfaces compartidas, no hay dependencias en común."

> "Esto significa que puedes añadir un nuevo servicio — por ejemplo, una raíz cuadrada — compilando un JAR nuevo y configurando una `SERVER_CELL` nueva. El resto del sistema no cambia."

---

## DEMO EN VIVO (3:50 – 5:00)

[PANTALLA: Terminal — iniciar los 4 nodos]

> "Veamos el sistema en acción. Primero iniciamos los cuatro nodos."

```bash
cd compilados_ejemplo/Node1 && java -jar NODE.jar
cd compilados_ejemplo/Node2 && java -jar NODE.jar
cd compilados_ejemplo/Node3 && java -jar NODE.jar
cd compilados_ejemplo/Node4 && java -jar NODE.jar
```

[PANTALLA: Terminal — iniciar server cells]

> "Luego iniciamos las celdas servidoras. Desplegamos dos instancias por operación para satisfacer `minAcks=2`."

```bash
cd compilados_ejemplo/ServerCell_Suma_1 && java -jar SERVER_CELL.jar
cd compilados_ejemplo/ServerCell_Suma_2 && java -jar SERVER_CELL.jar
# ... igual para Resta, Mult, Div
```

[PANTALLA: Terminal — iniciar CELL y mostrar menú]

> "Finalmente, la celda cliente. Aparece el menú interactivo."

> "Seleccionamos Suma, ingresamos los operandos... y en milisegundos recibimos el resultado. El sistema confirmó la entrega con dos acuses de recibo antes de responder."

[PANTALLA: Log mostrando ACKs llegando de dos SERVER_CELLs distintas]

> "En los logs vemos exactamente eso: dos ACKs de celdas servidoras distintas, seguidos de las dos respuestas con el resultado."

---

## CONCLUSIÓN (5:00 – 5:30)

[PANTALLA: Diagrama completo de la arquitectura]

> "ADSOA logra lo que ninguna arquitectura cliente-servidor tradicional puede: disponibilidad sin punto único de falla, extensibilidad sin detener el sistema, y entrega garantizada sin coordinador central."

> "La malla se reconecta automáticamente cuando un nodo cae. Los servicios se cargan en caliente. El protocolo de foliado garantiza que los mensajes llegan antes de avanzar."

> "Es una arquitectura que escala horizontalmente por diseño: más celdas servidoras significa más disponibilidad y más capacidad de procesamiento, sin cambiar una sola línea de código."

> "Gracias."

[PANTALLA: Fade a negro con créditos — "Antonio V — 0264679@up.edu.mx — Cómputo Distribuido"]

---

## Notas para la grabación

- **Duración real sugerida:** 4:30–5:30 min. Ajustar demo según fluidez de la terminal.
- **Demo:** tener las terminales ya abiertas y con los comandos listos para copiar/pegar.
- **Orden de inicio:** nodos primero, luego server cells, luego client cell. Los nodos tardan ~1s en conectarse entre sí.
- **Si algo falla en demo:** cortar al diagrama y narrar el flujo; el log de la carpeta `compilados_ejemplo` ya tiene output de referencia.
- **Velocidad de narración:** pausar 1s después de cada punto clave (Regla de Oro, foliado, carga dinámica). Son los conceptos más densos.
