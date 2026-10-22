# Respaldo y restauración de la base de datos

La base de PostgreSQL corre en el contenedor `db` de Docker Compose (en EC2, desde `~/TEO1_ClinicaDental`). Los dos scripts de `scripts/` entran a ese contenedor con `docker compose exec` y usan las credenciales que ya tiene (`POSTGRES_USER` y `POSTGRES_DB`), así que **no piden ni guardan contraseñas**.

| Script | Qué hace |
|---|---|
| `scripts/respaldar-db.sh [directorio]` | `pg_dump` en formato custom a `<directorio>/clinica_dental-AAAA-MM-DD-HHMM.dump` (por defecto `~/respaldos`, modo 600), valida el archivo con `pg_restore --list` y guarda junto a él un `.conteos.txt` con las filas de cada tabla. |
| `scripts/restaurar-db.sh <archivo.dump> [base] [--reemplazar]` | Crea una base **nueva** (por defecto `clinica_dental_restore_test`), restaura el respaldo y compara las filas de cada tabla con el `.conteos.txt`. Termina con error si alguna tabla difiere. Nunca escribe sobre la base que usa el backend. |

Requisitos: Docker con Compose v2 y el contenedor `db` en marcha (`docker compose ps db` debe mostrarlo `healthy`). Los scripts se corren desde la raíz del repositorio. Si el contenedor se maneja de otra forma, la variable `DB_EXEC` cambia el comando para entrar a él; por ejemplo, `DB_EXEC="podman exec -i <contenedor>"` para probar en local con podman.

## 1. Generar un respaldo

```bash
cd ~/TEO1_ClinicaDental
scripts/respaldar-db.sh
```

La salida muestra la ruta del archivo, su tamaño y las filas por tabla. Hay que hacerlo **siempre antes de aplicar una migración** y antes de cada release a `main`.

## 2. Guardar una copia fuera de EC2

Los respaldos que se quedan en la instancia se pierden si se borra la instancia o su disco. Desde la computadora local, copiar el archivo y sus conteos a una carpeta privada:

```bash
mkdir -p ~/respaldos-clinica && chmod 700 ~/respaldos-clinica
scp -i <llave.pem> 'ec2-user@<IP-EC2>:respaldos/clinica_dental-AAAA-MM-DD-HHMM.*' ~/respaldos-clinica/
```

Los respaldos tienen datos personales de los pacientes: no se suben al repositorio ni a carpetas compartidas.

## 3. Ensayar la restauración (sin tocar producción)

```bash
cd ~/TEO1_ClinicaDental
scripts/restaurar-db.sh ~/respaldos/clinica_dental-AAAA-MM-DD-HHMM.dump
```

El script crea `clinica_dental_restore_test`, restaura el respaldo y muestra una tabla con las filas del respaldo y las restauradas. Debe terminar con `OK: ... tiene las mismas filas que el respaldo en todas las tablas`. Para repetir el ensayo se agrega `--reemplazar`, que borra la base de prueba anterior.

Al terminar, borrar la base de prueba:

```bash
echo "DROP DATABASE clinica_dental_restore_test" \
  | docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d postgres -X -q'
```

## 4. Recuperar producción desde un respaldo

Solo si la base `clinica_dental` se dañó o hay que volver a un estado anterior. El backend deja de responder durante unos segundos y se pierde lo que se haya guardado después del respaldo.

1. Generar un respaldo del estado actual (paso 1), aunque esté dañado, para poder investigarlo después.
2. Restaurar el respaldo bueno en una base aparte y confirmar que termina con `OK`:

   ```bash
   scripts/restaurar-db.sh ~/respaldos/<respaldo-bueno>.dump clinica_dental_recuperada
   ```

3. Detener el backend, intercambiar los nombres de las bases (la actual queda como `clinica_dental_anterior`, sin borrarse) y volver a levantarlo:

   ```bash
   docker compose stop backend
   docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d postgres -X -v ON_ERROR_STOP=1' <<'SQL'
   ALTER DATABASE clinica_dental RENAME TO clinica_dental_anterior;
   ALTER DATABASE clinica_dental_recuperada RENAME TO clinica_dental;
   SQL
   docker compose start backend
   ```

4. Comprobar que el backend arrancó (`docker compose logs --tail 50 backend` y `curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/v3/api-docs`, que debe dar 200) y que la aplicación muestra los datos esperados.
5. Cuando todo esté confirmado, borrar `clinica_dental_anterior` con `DROP DATABASE` como en el paso 3.

El backend valida el esquema al arrancar (`ddl-auto=validate`): si el respaldo es de antes de una migración, hay que volver a aplicar las migraciones de `db/migrations/` sobre la base recuperada antes del paso 3 (ver la sección de migraciones del README).

## Verificación en EC2 (2026-10-21)

Respaldo de la base de producción y restauración en `clinica_dental_restore_test` sobre la instancia de EC2, con el despliegue del Sprint 5 (`34c8a6b`). Se copiaron los scripts a una carpeta temporal fuera del checkout para no dejar archivos sin seguimiento, con `DB_EXEC="docker compose -f $HOME/TEO1_ClinicaDental/docker-compose.yml exec -T db"`. Salida:

```text
Respaldo listo: /home/ec2-user/respaldos/clinica_dental-2026-10-21-2222.dump (40K)
Filas por tabla guardadas en /home/ec2-user/respaldos/clinica_dental-2026-10-21-2222.conteos.txt:
bloqueo_doctor           1
cita                     17
cita_reprogramacion      3
cita_tratamiento         4
cliente                  9
doctor                   3
historial_clinico        2
horario                  5
medicamento              0
pago                     0
rol                      4
secretaria               2
tratamiento              10
tratamiento_medicamento  0
usuario                  15
valoracion               0
total 80
-rw-------. 1 ec2-user ec2-user   220 Oct 21 22:22 clinica_dental-2026-10-21-2222.conteos.txt
-rw-------. 1 ec2-user ec2-user 39289 Oct 21 22:22 clinica_dental-2026-10-21-2222.dump
-rw-rw-r--. 1 ec2-user ec2-user 32997 Oct 15 19:28 clinica_dental-antes-sprint5-2026-10-15-1328.dump

Creando la base clinica_dental_restore_test y restaurando /home/ec2-user/respaldos/clinica_dental-2026-10-21-2222.dump

TABLA                          RESPALDO RESTAURADA
bloqueo_doctor                        1          1
cita                                 17         17
cita_reprogramacion                   3          3
cita_tratamiento                      4          4
cliente                               9          9
doctor                                3          3
historial_clinico                     2          2
horario                               5          5
medicamento                           0          0
pago                                  0          0
rol                                   4          4
secretaria                            2          2
tratamiento                          10         10
tratamiento_medicamento               0          0
usuario                              15         15
valoracion                            0          0

OK: clinica_dental_restore_test tiene las mismas filas que el respaldo en todas las tablas
```

Además se comparó la estructura de las dos bases con `pg_dump -s`: ambas tienen las mismas 465 líneas y solo cambia cómo PostgreSQL escribe 5 restricciones `CHECK ... IN (...)` después de restaurar (`ARRAY[...]::text[]` frente a `ARRAY[(...)::text]`), que son condiciones equivalentes. Al terminar se borró `clinica_dental_restore_test` y la carpeta temporal; el respaldo quedó en `~/respaldos` con modo 600.

El intercambio de nombres del paso 4 se probó en local con podman y `postgres:16-alpine`, la misma imagen que usa EC2.
