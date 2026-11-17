#!/bin/sh
# Construit le site statique (page de téléchargement + Javadoc) dans public/.
# Attend : dist/*.jar (job package) et target/reports/apidocs (job javadoc).
# Variables : VERSION, COMMIT_SHA, REPO_URL, PIPELINE_URL (toutes optionnelles).
set -eu

OUT=${OUT:-public}
VERSION=${VERSION:-dev}
COMMIT_SHA=${COMMIT_SHA:-inconnu}
REPO_URL=${REPO_URL:-#}
PIPELINE_URL=${PIPELINE_URL:-#}
BUILD_DATE=$(date -u +"%Y-%m-%d %H:%M UTC")

rm -rf "$OUT"
mkdir -p "$OUT/downloads"
cp dist/*.jar "$OUT/downloads/"
if [ -d target/reports/apidocs ]; then cp -r target/reports/apidocs "$OUT/javadoc"; fi

ROWS=""
for f in "$OUT"/downloads/*.jar; do
  name=$(basename "$f")
  size=$(du -k "$f" | cut -f1)
  case "$name" in
    *server*) desc="Serveur de centralisation (jar autonome, dépendances incluses)"; cmd="java -jar $name 5000" ;;
    *client*) desc="Client de test (simule un microcontrôleur)"; cmd="java -jar $name 127.0.0.1 5000" ;;
    *) desc=""; cmd="" ;;
  esac
  ROWS="$ROWS<tr><td><a href=\"downloads/$name\">$name</a></td><td>$desc</td><td>${size} Ko</td><td><code>$cmd</code></td></tr>"
done

cat > "$OUT/index.html" <<HTML
<!doctype html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>BioWatcher - serveur TCP</title>
<style>
  :root { --bg:#f7f8f5; --fg:#1d2a1f; --muted:#5b6b5d; --accent:#2f6b3a; --line:#d9dfd6; --card:#fff; }
  @media (prefers-color-scheme: dark) { :root { --bg:#141a15; --fg:#e4ebe3; --muted:#9aab9c; --accent:#7cc48a; --line:#2c372d; --card:#1b231c; } }
  body { margin:0; background:var(--bg); color:var(--fg); font:16px/1.5 system-ui, sans-serif; }
  main { max-width:960px; margin:0 auto; padding:32px 16px; }
  h1 { margin:0 0 4px; } .meta { color:var(--muted); margin:0 0 24px; }
  a { color:var(--accent); }
  .card { background:var(--card); border:1px solid var(--line); border-radius:8px; padding:16px; margin-bottom:20px; overflow-x:auto; }
  table { border-collapse:collapse; width:100%; } th, td { text-align:left; padding:8px; border-bottom:1px solid var(--line); vertical-align:top; }
  code { font-size:13px; }
</style>
</head>
<body><main>
  <h1>BioWatcher - serveur TCP de centralisation</h1>
  <p class="meta">Version <strong>$VERSION</strong> · commit <code>$COMMIT_SHA</code> · construit le $BUILD_DATE</p>
  <div class="card">
    <h2>Téléchargements</h2>
    <table><thead><tr><th>Fichier</th><th>Description</th><th>Taille</th><th>Lancement</th></tr></thead>
    <tbody>$ROWS</tbody></table>
    <p>Prérequis : Java 21+, MongoDB sur <code>localhost:27017</code> et l'API Node sur <code>localhost:4567</code>.</p>
  </div>
  <div class="card">
    <h2>Documentation</h2>
    <p><a href="javadoc/index.html">Javadoc du serveur et du client</a></p>
  </div>
  <div class="card">
    <h2>Liens</h2>
    <p><a href="$REPO_URL">Dépôt</a> · <a href="$PIPELINE_URL">Pipeline qui a produit cette page</a></p>
  </div>
</main></body>
</html>
HTML

echo "site généré dans $OUT/"
