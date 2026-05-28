#!/bin/bash
echo "Arrancando Malla de Nodos..."
(cd Node1 && java -jar NODE.jar) &
(cd Node2 && java -jar NODE.jar) &
(cd Node3 && java -jar NODE.jar) &
(cd Node4 && java -jar NODE.jar) &
echo "Nodos iniciados en segundo plano."
