#!/usr/bin/env bash
# Restaura un respaldo de respaldar-db.sh en una base NUEVA del contenedor db y verifica las filas.
#
# Uso, desde la raiz del repositorio (en EC2: ~/TEO1_ClinicaDental):
#   scripts/restaurar-db.sh <archivo.dump> [base_destino] [--reemplazar]
#
# base_destino es por defecto clinica_dental_restore_test. Nunca escribe sobre la base que usa el backend
# (POSTGRES_DB): para recuperar produccion se restaura en otra base y se intercambian los nombres con el
# backend detenido (ver docs/respaldo-y-restauracion.md). --reemplazar borra base_destino si ya existe.
#
# Si junto al archivo esta su .conteos.txt, compara las filas de cada tabla y termina con error si difieren.
# DB_EXEC cambia como se entra al contenedor (por defecto "docker compose exec -T db").
set -Eeuo pipefail

cd "$(dirname "$0")/.."
DB_EXEC="${DB_EXEC:-docker compose exec -T db}"

archivo=""
destino="clinica_dental_restore_test"
reemplazar=false
for argumento in "$@"; do
  case "$argumento" in
    --reemplazar) reemplazar=true ;;
    *) if [[ -z "$archivo" ]]; then archivo="$argumento"; else destino="$argumento"; fi ;;
  esac
done

if [[ -z "$archivo" || ! -f "$archivo" ]]; then
  echo "Uso: scripts/restaurar-db.sh <archivo.dump> [base_destino] [--reemplazar]" >&2
  exit 1
fi
if [[ ! "$destino" =~ ^[a-z_][a-z0-9_]*$ ]]; then
  echo "Nombre de base no valido: $destino (solo minusculas, numeros y _)" >&2
  exit 1
fi

base_backend="$($DB_EXEC sh -c 'printf %s "$POSTGRES_DB"')"
if [[ "$destino" == "$base_backend" ]]; then
  echo "No se restaura sobre $base_backend, la base que usa el backend. Usa otra base y sigue el" >&2
  echo "procedimiento de recuperacion de docs/respaldo-y-restauracion.md" >&2
  exit 1
fi

psql_en() {
  $DB_EXEC sh -c "psql -U \"\$POSTGRES_USER\" -d $1 -X -q -t -A -v ON_ERROR_STOP=1"
}

existe="$(echo "SELECT 1 FROM pg_database WHERE datname = '$destino'" | psql_en postgres)"
if [[ -n "$existe" ]]; then
  if [[ "$reemplazar" != true ]]; then
    echo "La base $destino ya existe; agrega --reemplazar para borrarla y restaurar de nuevo" >&2
    exit 1
  fi
  echo "Borrando la base $destino"
  echo "DROP DATABASE $destino WITH (FORCE)" | psql_en postgres
fi

echo "Creando la base $destino y restaurando $archivo"
echo "CREATE DATABASE $destino" | psql_en postgres
$DB_EXEC sh -c "pg_restore --no-owner --exit-on-error -U \"\$POSTGRES_USER\" -d $destino" < "$archivo"

conteos_restaurados="$(psql_en "$destino" <<'SQL'
SELECT format('SELECT %L || ''|'' || count(*) FROM public.%I', tablename, tablename)
FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename
\gexec
SQL
)"

conteos_respaldo="${archivo%.dump}.conteos.txt"
if [[ ! -f "$conteos_respaldo" ]]; then
  echo "Restauracion terminada. No hay $conteos_respaldo para comparar; filas en $destino:"
  column -t -s '|' <<< "$conteos_restaurados"
  exit 0
fi

echo
printf '%-28s %10s %10s\n' TABLA RESPALDO RESTAURADA
diferencias=0
while IFS='|' read -r tabla filas; do
  restauradas="$(grep "^$tabla|" <<< "$conteos_restaurados" | cut -d'|' -f2)"
  marca=""
  if [[ "$filas" != "$restauradas" ]]; then
    marca="  <-- DIFERENTE"
    diferencias=$((diferencias + 1))
  fi
  printf '%-28s %10s %10s%s\n' "$tabla" "$filas" "${restauradas:--}" "$marca"
done < "$conteos_respaldo"

echo
if [[ "$diferencias" -gt 0 ]]; then
  echo "ERROR: $diferencias tabla(s) con filas distintas al respaldo" >&2
  exit 1
fi
echo "OK: $destino tiene las mismas filas que el respaldo en todas las tablas"
