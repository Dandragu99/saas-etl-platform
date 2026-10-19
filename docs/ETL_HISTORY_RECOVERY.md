# Recuperación de ejecuciones ETL anteriores a V6

## Estado de los registros legacy

V6 añade `company_id` como columna nullable para conservar todas las ejecuciones creadas antes del
aislamiento multiempresa. Esos registros mantienen `company_id = NULL` y no participan en consultas,
resúmenes ni búsquedas empresariales.

No debe inferirse una empresa a partir de fechas, nombres de archivo, usuarios actuales o la primera
empresa disponible. Los nombres de archivo tampoco existían antes de V6.

## Procedimiento de recuperación

La asignación solo puede realizarla un administrador de base de datos después de identificar de
forma verificable al propietario de una ejecución. Antes de actualizar:

1. Crear una copia de seguridad.
2. Registrar el identificador de ejecución, la empresa elegida y la evidencia externa utilizada.
3. Confirmar que la empresa existe y está activa.
4. Actualizar únicamente los identificadores revisados, dentro de una transacción.

Plantilla deliberadamente no ejecutable sin sustituir los valores:

```sql
BEGIN;

SELECT id, type, status, started_at
FROM etl_executions
WHERE id = '<execution-uuid>'::uuid
  AND company_id IS NULL
FOR UPDATE;

UPDATE etl_executions
SET company_id = '<verified-company-uuid>'::uuid
WHERE id = '<execution-uuid>'::uuid
  AND company_id IS NULL;

COMMIT;
```

Si no existe evidencia suficiente, el registro debe permanecer sin empresa y, por tanto, invisible
para usuarios empresariales.
