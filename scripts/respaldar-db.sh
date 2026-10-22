#!/usr/bin/env bash
# Respaldo de la base de PostgreSQL del contenedor db (pg_dump en formato custom).
#
# Uso, desde la raiz del repositorio (en EC2: ~/TEO1_ClinicaDental):
#   scripts/respaldar-db.sh [directorio]        # por defecto ~/respaldos
#
# Genera <directorio>/clinica_dental-AAAA-MM-DD-HHMM.dump (modo 600) y, junto a el, un .conteos.txt con
# las filas de cada tabla al momento del respaldo, que restaurar-db.sh usa para verificar.
# No pide ni escribe contrasenas: usa las credenciales que ya tiene el contenedor (POSTGRES_USER y POSTGRES_DB).
#
# DB_EXEC cambia como se entra al contenedor (por defecto "docker compose exec -T db").
set -Eeuo pipefail
umask 077

cd "$(dirname "$0")/.."
DB_EXEC="${DB_EXEC:-docker compose exec -T db}"
directorio="${1:-$HOME/respaldos}"
mkdir -p "$directorio"

base="$($DB_EXEC sh -c 'printf %s "$POSTGRES_DB"')"
archivo="$directorio/${base}-$(date +%F-%H%M).dump"
conteos="${archivo%.dump}.conteos.txt"
if [[ -e "$archivo" ]]; then
  echo "Ya existe $archivo; espera un minuto o usa otro directorio" >&2
  exit 1
fi

echo "Respaldando la base $base en $archivo"
if ! $DB_EXEC sh -c 'pg_dump --format=custom --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > "$archivo"; then
  rm -f "$archivo"
  echo "pg_dump fallo; no se genero el respaldo" >&2
  exit 1
fi

# pg_restore --list lee el catalogo completo: si el archivo esta truncado o danado, falla
if ! $DB_EXEC pg_restore --list < "$archivo" > /dev/null; then
  echo "El archivo $archivo no se puede leer con pg_restore" >&2
  exit 1
fi

# Filas por tabla del esquema public, en orden alfabetico: "tabla|filas"
$DB_EXEC sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -X -q -t -A -v ON_ERROR_STOP=1' > "$conteos" <<'SQL'
SELECT format('SELECT %L || ''|'' || count(*) FROM public.%I', tablename, tablename)
FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename
\gexec
SQL

echo "Respaldo listo: $archivo ($(du -h "$archivo" | cut -f1))"
echo "Filas por tabla guardadas en $conteos:"
column -t -s '|' "$conteos"
