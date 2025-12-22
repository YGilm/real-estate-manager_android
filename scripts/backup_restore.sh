#!/usr/bin/env bash
set -euo pipefail

APP_ID="com.example.my_project"
ADB="${ADB:-/Users/yurygilm/Library/Android/sdk/platform-tools/adb}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/backup_restore.sh backup [DEST_DIR]
  ./scripts/backup_restore.sh restore SOURCE_DIR

Notes:
  - backup creates a folder with DB + files.tar on Desktop by default.
  - restore replaces the app DB and files/ with the contents from SOURCE_DIR.
EOF
}

timestamp() {
  date +%Y%m%d_%H%M%S
}

backup() {
  local dest="${1:-$HOME/Desktop/real_estate_backup_$(timestamp)}"
  mkdir -p "$dest"
  "$ADB" shell am force-stop "$APP_ID"
  "$ADB" exec-out run-as "$APP_ID" cat "/data/data/$APP_ID/databases/real_estate.db" > "$dest/real_estate.db"
  "$ADB" exec-out run-as "$APP_ID" cat "/data/data/$APP_ID/databases/real_estate.db-wal" > "$dest/real_estate.db-wal" || true
  "$ADB" exec-out run-as "$APP_ID" cat "/data/data/$APP_ID/databases/real_estate.db-shm" > "$dest/real_estate.db-shm" || true
  "$ADB" exec-out run-as "$APP_ID" tar -cf - -C "/data/data/$APP_ID/files" . > "$dest/files.tar"
  echo "Backup saved to: $dest"
}

restore() {
  local src="${1:-}"
  if [[ -z "$src" || ! -d "$src" ]]; then
    echo "Source directory is required." >&2
    usage
    exit 1
  fi

  if [[ ! -f "$src/real_estate.db" ]]; then
    echo "Missing $src/real_estate.db" >&2
    exit 1
  fi

  "$ADB" shell am force-stop "$APP_ID"
  "$ADB" push "$src/real_estate.db" /data/local/tmp/real_estate.db >/dev/null
  "$ADB" shell "run-as $APP_ID sh -c 'cp /data/local/tmp/real_estate.db /data/data/$APP_ID/databases/real_estate.db; rm -f /data/data/$APP_ID/databases/real_estate.db-wal /data/data/$APP_ID/databases/real_estate.db-shm'"
  "$ADB" shell rm -f /data/local/tmp/real_estate.db

  if [[ -f "$src/files.tar" ]]; then
    "$ADB" push "$src/files.tar" /data/local/tmp/files.tar >/dev/null
    "$ADB" shell "run-as $APP_ID sh -c 'rm -rf /data/data/$APP_ID/files/*'"
    "$ADB" shell "run-as $APP_ID sh -c 'tar -xf /data/local/tmp/files.tar -C /data/data/$APP_ID/files'"
    "$ADB" shell rm -f /data/local/tmp/files.tar
  fi

  echo "Restore completed from: $src"
}

cmd="${1:-}"
shift || true

case "$cmd" in
  backup) backup "${1:-}" ;;
  restore) restore "${1:-}" ;;
  *) usage; exit 1 ;;
esac
