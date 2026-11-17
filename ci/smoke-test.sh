#!/usr/bin/env bash
# Test de bout en bout du serveur TCP : lance le jar construit et lui parle comme un µC.
#
# usage : ci/smoke-test.sh <biowatcher-tcp-server.jar> [port]
#
# Le serveur utilise MongoDB sur localhost:27017 et l'API sur localhost:4567/weatherapi
# (valeurs codées en dur dans MainServer). Si WITH_API=true, l'API réelle doit tourner :
# on teste alors la chaîne complète AUTOREGISTER -> STOREMEASURE -> GETCONFIG.
set -euo pipefail

JAR=${1:?usage: $0 <server.jar> [port]}
PORT=${2:-5000}
WITH_API=${WITH_API:-false}
LOG=${SERVER_LOG:-server.log}

java -jar "$JAR" "$PORT" > "$LOG" 2>&1 &
SERVER_PID=$!
trap 'kill "$SERVER_PID" 2>/dev/null || true' EXIT

echo "attente du serveur sur le port $PORT..."
for _ in $(seq 1 30); do
  if (exec 9<>"/dev/tcp/127.0.0.1/$PORT") 2>/dev/null; then break; fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then echo "le serveur s'est arrêté :"; cat "$LOG"; exit 1; fi
  sleep 1
done

exec 3<>"/dev/tcp/127.0.0.1/$PORT"

FAILED=0
ANSWER=""
req() {
  printf '%s\n' "$1" >&3
  if ! IFS= read -r -t 15 ANSWER <&3; then ANSWER="<pas de réponse>"; fi
  printf '  > %s\n  < %s\n' "$1" "$ANSWER"
}
expect() {
  case "$ANSWER" in
    "$1"*) echo "  ok" ;;
    *) echo "  ÉCHEC : attendu '$1...'"; FAILED=1 ;;
  esac
}

NOW=$(date +%Y-%m-%dT%H:%M:%S)
UNKNOWN_KEY=$(cat /proc/sys/kernel/random/uuid)

echo "== protocole"
req "HELLO";                                   expect "ERR unknown command"
req "STOREMEASURE temperature 21";             expect "ERR invalid number of parameters"
req "AUTOREGISTER esp32 lm35 999 48.85";       expect "ERR coordinates out of range"

echo "== accès direct MongoDB"
req "STOREANALYSIS smoke $NOW 1";              expect "OK"
req "GETCONFIG $UNKNOWN_KEY";                  expect "ERR"

if [ "$WITH_API" = "true" ]; then
  echo "== chaîne complète avec l'API Node"
  req "AUTOREGISTER esp32 lm35 2.3522 48.8566"; expect "OK"
  KEY=${ANSWER##*,}
  req "STOREMEASURE temperature $NOW 21.5 $KEY"; expect "OK"
  req "STOREMEASURE temperature $NOW 21.5 $UNKNOWN_KEY"; expect "ERR"
  req "GETCONFIG $KEY";                        expect "OK"
else
  echo "== sans API (WITH_API!=true) : le serveur doit le signaler proprement"
  req "AUTOREGISTER esp32 lm35 2.3522 48.8566"; expect "ERR cannot join the API"
fi

printf '\n' >&3   # ligne vide = fin de session
exec 3>&-

if [ "$FAILED" -ne 0 ]; then
  echo "=== log serveur ==="; cat "$LOG"
  exit 1
fi
echo "smoke test OK"
