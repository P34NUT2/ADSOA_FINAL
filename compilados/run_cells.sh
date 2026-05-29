#!/bin/bash
# Start 4 server cells (one per operation) in background, then 1 client cell interactive.
# Run ./run_all_nodes.sh first.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA=/home/antonio/.antigravity/extensions/redhat.java-1.54.0-linux-x64/jre/21.0.10-linux-x86_64/bin/java

start_server() {
    local NAME=$1
    local DIR="$SCRIPT_DIR/$NAME"
    mkdir -p "$DIR/logs"
    cd "$DIR"
    $JAVA -jar SERVER_CELL.jar >> "logs/ServerCell.log" 2>&1 &
    echo $!
}

echo "[*] Starting ServerCell_Suma_1  (Node1 :5000)..."
PID1=$(start_server ServerCell_Suma_1)
echo "    PID=$PID1"
echo "[*] Starting ServerCell_Suma_2  (Node2 :5001)..."
PID2=$(start_server ServerCell_Suma_2)
echo "    PID=$PID2"

echo "[*] Starting ServerCell_Resta_1 (Node3 :5002)..."
PID3=$(start_server ServerCell_Resta_1)
echo "    PID=$PID3"
echo "[*] Starting ServerCell_Resta_2 (Node4 :5003)..."
PID4=$(start_server ServerCell_Resta_2)
echo "    PID=$PID4"

echo "[*] Starting ServerCell_Mult_1  (Node1 :5000)..."
PID5=$(start_server ServerCell_Mult_1)
echo "    PID=$PID5"
echo "[*] Starting ServerCell_Mult_2  (Node2 :5001)..."
PID6=$(start_server ServerCell_Mult_2)
echo "    PID=$PID6"

echo "[*] Starting ServerCell_Div_1   (Node3 :5002)..."
PID7=$(start_server ServerCell_Div_1)
echo "    PID=$PID7"
echo "[*] Starting ServerCell_Div_2   (Node4 :5003)..."
PID8=$(start_server ServerCell_Div_2)
echo "    PID=$PID8"

echo "$PID1 $PID2 $PID3 $PID4 $PID5 $PID6 $PID7 $PID8" > "$SCRIPT_DIR/.server_cell_pids"
sleep 2

echo ""
echo "All 8 server cells running. Choose a client:"
echo "  1) ClientCell1 -> Node1 :5000"
echo "  2) ClientCell2 -> Node3 :5002"
read -p "Choice [1/2, default 1]: " CHOICE

if [ "$CHOICE" = "2" ]; then
    echo "[*] Starting ClientCell2..."
    cd "$SCRIPT_DIR/ClientCell2"
    $JAVA -jar CELL.jar
else
    echo "[*] Starting ClientCell1..."
    cd "$SCRIPT_DIR/ClientCell1"
    $JAVA -jar CELL.jar
fi
