# API Contract

## 1. Propósito y alcance

Este documento define los contratos HTTP para obtener la vista previa de archivos CSV y la vista previa resultante de eliminar una columna.

La funcionalidad permite enviar un archivo CSV, validar su formato y obtener sus columnas y hasta 20 filas de muestra.

En esta primera versión:

- El archivo no se persiste.
- El endpoint `/api/csv/preview` no aplica transformaciones.
- El endpoint `/api/csv/transform/remove-column` aplica la eliminación únicamente sobre la vista previa devuelta.
- No se infieren tipos de datos.
- Todos los valores se representan como `String`.
- No se admiten formatos distintos de CSV.

## 2. Endpoint

```http
POST /api/csv/preview
```

Content-Type de la petición:

```http
multipart/form-data
```

Content-Type de las respuestas:

```http
application/json
```

## 3. Petición multipart

La petición debe contener una parte obligatoria llamada `file`.

Ejemplo con curl:

```bash
curl --request POST \
  --url http://localhost:8080/api/csv/preview \
  --form "file=@clientes.csv"
```

Reglas de la parte `file`:

- Debe estar presente.
- Debe contener datos.
- El nombre debe terminar en `.csv`, sin distinguir mayúsculas y minúsculas.
- El nombre recibido se considera entrada no confiable y debe tratarse de forma segura.
- El MIME declarado por el cliente puede utilizarse como información adicional, pero nunca como única validación.
- El tamaño máximo permitido es exactamente 5.242.880 bytes, equivalentes a 5 MiB.
- Un archivo de exactamente 5.242.880 bytes está permitido.
- Un archivo de 5.242.881 bytes o más debe rechazarse.

## 4. Formato CSV admitido

La primera versión admite únicamente:

- Codificación UTF-8 estricta.
- BOM UTF-8 opcional al principio del archivo.
- Separador coma (`,`).
- Reglas estándar de campos entrecomillados y comillas escapadas.
- Saltos de línea válidos dentro de campos entrecomillados.
- Valores tratados literalmente como `String`.
- Campos vacíos representados como `""`.

Si existe un BOM UTF-8, se acepta y se retira antes de interpretar el primer encabezado.

Una secuencia de bytes que no sea UTF-8 válida provoca un error `CSV_MALFORMED`. Los caracteres inválidos no se sustituyen silenciosamente.

No se detectan automáticamente otros separadores, codificaciones ni tipos de datos.

## 5. Reglas de encabezados

- El primer registro no vacío es el encabezado.
- El encabezado es obligatorio.
- Debe contener al menos una columna.
- Los nombres de las columnas se conservan literalmente.
- No se recortan ni normalizan automáticamente.
- Una columna sin nombre o cuyo nombre esté compuesto únicamente por espacios se considera inválida.
- No se permiten nombres de columna duplicados.
- La comparación de duplicados es literal y distingue mayúsculas de minúsculas.

Un archivo que contenga un encabezado válido y ninguna fila de datos está permitido.

## 6. Reglas de filas

- Cada fila debe contener exactamente el mismo número de campos que el encabezado.
- Una fila con más o menos campos se rechaza con `CSV_MALFORMED`.
- Las líneas completamente vacías se ignoran y no cuentan como filas.
- Una fila como `,,` no está vacía: representa una fila válida de tres campos con valor `""`.
- El orden de los valores debe coincidir con el orden de `columns`.
- El CSV se procesa completamente mediante streaming para detectar errores posteriores a la fila 20.
- Solo se conservan en memoria las primeras 20 filas válidas para la respuesta.
- El encabezado no cuenta dentro del límite de 20 filas.

## 7. Respuesta correcta

Una petición válida devuelve:

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

Campos de la respuesta:

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `fileName` | `String` | Nombre seguro del archivo recibido. |
| `columns` | `String[]` | Encabezados en el orden original. |
| `rows` | `Object[]` | Hasta 20 filas, representadas como pares columna-valor. |
| `previewRowCount` | `number` | Número de filas incluidas en `rows`, entre 0 y 20. |
| `truncated` | `boolean` | Indica si el CSV contiene más de 20 filas válidas. |

`truncated` es `false` cuando el CSV contiene entre 0 y 20 filas. Es `true` cuando contiene al menos 21 filas, aunque el archivo se procesa hasta el final.

Ejemplo:

```json
{
  "fileName": "clientes.csv",
  "columns": ["id", "nombre", "email"],
  "rows": [
    {
      "id": "1",
      "nombre": "Ana",
      "email": "ana@example.com"
    }
  ],
  "previewRowCount": 1,
  "truncated": false
}
```

Ejemplo para un CSV que solo contiene un encabezado:

```json
{
  "fileName": "clientes.csv",
  "columns": ["id", "nombre", "email"],
  "rows": [],
  "previewRowCount": 0,
  "truncated": false
}
```

## 8. Estructura común de error

Todas las respuestas de error utilizan esta estructura:

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `code` | `String` | Código estable para identificar el error. |
| `message` | `String` | Explicación comprensible para el usuario. |
| `status` | `number` | Estado HTTP de la respuesta. |
| `path` | `String` | Ruta de la petición que produjo el error. |
| `timestamp` | `String` | Instante UTC en formato ISO-8601. |

Ejemplo:

```json
{
  "code": "CSV_FILE_EMPTY",
  "message": "El archivo CSV está vacío.",
  "status": 400,
  "path": "/api/csv/preview",
  "timestamp": "2026-07-21T19:45:00Z"
}
```

Los mensajes pueden evolucionar, pero `code` y `status` forman parte del contrato estable.

## 9. Catálogo de errores

| Código | Estado HTTP | Condición |
| --- | ---: | --- |
| `CSV_FILE_REQUIRED` | 400 | La parte multipart `file` no está presente. |
| `CSV_FILE_EMPTY` | 400 | La parte está presente, pero su tamaño declarado es igual a cero. |
| `CSV_INVALID_EXTENSION` | 400 | El nombre está ausente o no termina en `.csv`, sin distinguir mayúsculas y minúsculas. |
| `CSV_FILE_TOO_LARGE` | 413 | El archivo supera los 5.242.880 bytes. |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | La petición no utiliza `multipart/form-data`. |
| `CSV_HEADER_MISSING` | 422 | El archivo contiene bytes, pero no existe ningún registro utilizable como encabezado. |
| `CSV_INVALID_HEADER` | 422 | El encabezado contiene una columna sin nombre, un nombre compuesto solo por espacios o un nombre duplicado. |
| `CSV_MALFORMED` | 422 | El contenido no es UTF-8 válido, incumple la sintaxis CSV o una fila tiene un número de campos diferente al encabezado. |
| `INTERNAL_ERROR` | 500 | Se produce un error inesperado durante el procesamiento. |

Ejemplo de archivo demasiado grande:

```json
{
  "code": "CSV_FILE_TOO_LARGE",
  "message": "El archivo CSV supera el tamaño máximo permitido de 5 MiB.",
  "status": 413,
  "path": "/api/csv/preview",
  "timestamp": "2026-07-21T19:45:00Z"
}
```

Ejemplo de encabezado inválido:

```json
{
  "code": "CSV_INVALID_HEADER",
  "message": "El encabezado CSV contiene columnas vacías o duplicadas.",
  "status": 422,
  "path": "/api/csv/preview",
  "timestamp": "2026-07-21T19:45:00Z"
}
```

Ejemplo de error interno:

```json
{
  "code": "INTERNAL_ERROR",
  "message": "Se ha producido un error interno.",
  "status": 500,
  "path": "/api/csv/preview",
  "timestamp": "2026-07-21T19:45:00Z"
}
```

## 10. Casos especiales y seguridad

- El archivo se procesa durante la petición y no se conserva de forma permanente.
- El soporte multipart del servidor puede utilizar memoria o almacenamiento temporal durante la petición.
- El nombre original del archivo no debe utilizarse directamente como una ruta del sistema.
- La extensión y el contenido deben validarse aunque el MIME declarado sea `text/csv`.
- Un MIME desconocido o `application/octet-stream` no invalida por sí solo un archivo que cumple el resto de las reglas.
- Todo el contenido subido se considera entrada no confiable.
- Las respuestas `INTERNAL_ERROR` no incluyen nombres de clases, stack traces, rutas internas ni detalles sensibles.
- El procesamiento mediante streaming limita el uso de memoria, pero no evita validar el archivo completo.

## 11. Pruebas de aceptación

### Peticiones válidas

1. Un CSV UTF-8 válido con encabezado y una fila devuelve HTTP 200.
2. Los encabezados aparecen en `columns` en el orden original.
3. Los valores aparecen asociados a sus encabezados y se conservan literalmente.
4. Los caracteres UTF-8, incluidos caracteres acentuados, se preservan.
5. Un BOM UTF-8 inicial se acepta y no aparece en el primer encabezado.
6. Los campos vacíos se devuelven como `""`.
7. Las comas dentro de campos entrecomillados se interpretan como parte del valor.
8. Las comillas escapadas se interpretan correctamente.
9. Los saltos de línea dentro de campos entrecomillados se interpretan correctamente.
10. Una extensión `.CSV` se acepta.
11. Un archivo con encabezado y cero filas devuelve `rows: []`, `previewRowCount: 0` y `truncated: false`.
12. Las líneas completamente vacías se ignoran.
13. Una fila `,,` con un encabezado de tres columnas cuenta como una fila válida con tres valores `""`.
14. Un CSV con exactamente 20 filas devuelve 20 filas y `truncated: false`.
15. Un CSV con 21 o más filas devuelve 20 filas y `truncated: true`.
16. Un error de sintaxis posterior a la fila 20 se detecta y devuelve `CSV_MALFORMED`.
17. Un archivo de exactamente 5.242.880 bytes no se rechaza por tamaño.

### Peticiones inválidas

18. La ausencia de la parte `file` devuelve 400 y `CSV_FILE_REQUIRED`.
19. Un archivo de cero bytes devuelve 400 y `CSV_FILE_EMPTY`.
20. Un archivo compuesto solo por BOM y líneas vacías devuelve 422 y `CSV_HEADER_MISSING`.
21. Una extensión distinta de `.csv` devuelve 400 y `CSV_INVALID_EXTENSION`.
22. Un archivo de 5.242.881 bytes devuelve 413 y `CSV_FILE_TOO_LARGE`.
23. Una petición que no sea multipart devuelve 415 y `UNSUPPORTED_MEDIA_TYPE`.
24. Un archivo con bytes pero sin ningún registro utilizable como encabezado devuelve 422 y `CSV_HEADER_MISSING`.
25. Una columna sin nombre devuelve 422 y `CSV_INVALID_HEADER`.
26. Un encabezado compuesto solo por espacios devuelve 422 y `CSV_INVALID_HEADER`.
27. Un encabezado duplicado devuelve 422 y `CSV_INVALID_HEADER`.
28. Una fila con más campos que el encabezado devuelve 422 y `CSV_MALFORMED`.
29. Una fila con menos campos que el encabezado devuelve 422 y `CSV_MALFORMED`.
30. Un campo entrecomillado sin cerrar devuelve 422 y `CSV_MALFORMED`.
31. Una secuencia UTF-8 inválida devuelve 422 y `CSV_MALFORMED`.
32. Un error inesperado devuelve 500 y `INTERNAL_ERROR` sin detalles técnicos sensibles.

### Contrato común de errores

33. Todos los errores contienen `code`, `message`, `status`, `path` y `timestamp`.
34. `path` contiene `/api/csv/preview`.
35. `timestamp` utiliza UTC y formato ISO-8601.
36. El cuerpo de error y el estado HTTP contienen el mismo valor de estado.

## 12. Vista previa de eliminación de columna

### 12.1. Propósito y alcance

Este endpoint aplica la eliminación de una columna a la vista previa de un archivo CSV.
El resultado representa exclusivamente una vista previa transformada:

- No se genera un archivo CSV descargable.
- No se modifica ni se persiste el archivo original.
- No se procesa una cadena de transformaciones.
- Las reglas de archivo, formato CSV, encabezados, filas, tamaño y seguridad son las mismas que en `POST /api/csv/preview`.
- El archivo se valida y procesa completamente antes de validar y aplicar la eliminación de columna.
- Si el archivo y el nombre de columna son inválidos simultáneamente, tiene prioridad el error del archivo.

### 12.2. Endpoint

```http
POST /api/csv/transform/remove-column
Content-Type: multipart/form-data
```

La petición debe contener:

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| `file` | archivo | Sí | Archivo CSV sujeto a todas las validaciones existentes. |
| `column` | texto | Sí | Nombre exacto de la columna que debe eliminarse. |

Ejemplo con curl:

```bash
curl --request POST \
  --url http://localhost:8080/api/csv/transform/remove-column \
  --form "file=@clientes.csv" \
  --form "column=email"
```

### 12.3. Reglas de transformación

- `column` debe coincidir literalmente con un encabezado y la comparación distingue mayúsculas de minúsculas.
- El valor de `column` no se recorta ni normaliza.
- Un valor ausente, vacío o compuesto únicamente por espacios se rechaza.
- No se aceptan coincidencias parciales.
- La columna se elimina del encabezado y de todas las filas conservadas en la vista previa.
- No se puede eliminar la única columna existente.
- Se mantiene el orden original de las columnas restantes.
- Los valores restantes se conservan literalmente.
- `previewRowCount` y `truncated` conservan los valores obtenidos al procesar el archivo completo.
- Se devuelven como máximo las primeras 20 filas, igual que en la vista previa original.

### 12.4. Respuesta correcta

Una petición válida devuelve HTTP 200:

```json
{
  "fileName": "clientes.csv",
  "columns": ["id", "nombre"],
  "rows": [
    {
      "id": "1",
      "nombre": "Ana"
    }
  ],
  "previewRowCount": 1,
  "truncated": false,
  "transformation": {
    "type": "REMOVE_COLUMN",
    "removedColumn": "email"
  }
}
```

`transformation.type` utiliza el valor estable `REMOVE_COLUMN`. `removedColumn` conserva literalmente el nombre recibido.

### 12.5. Errores específicos

Los errores de archivo y parser conservan los códigos y estados definidos anteriormente.

| Código | Estado HTTP | Condición |
| --- | ---: | --- |
| `CSV_COLUMN_REQUIRED` | 400 | `column` está ausente, vacío o compuesto únicamente por espacios. |
| `CSV_COLUMN_NOT_FOUND` | 422 | No existe un encabezado que coincida exactamente con `column`. |
| `CSV_CANNOT_REMOVE_LAST_COLUMN` | 422 | El CSV tiene una única columna y se intenta eliminar. |

Ejemplo de columna obligatoria:

```json
{
  "code": "CSV_COLUMN_REQUIRED",
  "message": "El nombre de la columna es obligatorio.",
  "status": 400,
  "path": "/api/csv/transform/remove-column",
  "timestamp": "2026-07-22T17:45:00Z"
}
```

Ejemplo de columna inexistente:

```json
{
  "code": "CSV_COLUMN_NOT_FOUND",
  "message": "La columna indicada no existe en el archivo CSV.",
  "status": 422,
  "path": "/api/csv/transform/remove-column",
  "timestamp": "2026-07-22T17:45:00Z"
}
```

Ejemplo al intentar eliminar la única columna:

```json
{
  "code": "CSV_CANNOT_REMOVE_LAST_COLUMN",
  "message": "No se puede eliminar la única columna del archivo CSV.",
  "status": 422,
  "path": "/api/csv/transform/remove-column",
  "timestamp": "2026-07-22T17:45:00Z"
}
```

### 12.6. Pruebas de aceptación

1. Eliminar una columna existente devuelve HTTP 200 y una vista previa sin esa columna.
2. La columna desaparece tanto de `columns` como de todas las filas devueltas.
3. Se conserva el orden original de las columnas restantes.
4. Se conservan literalmente los valores restantes.
5. La comparación del nombre distingue mayúsculas de minúsculas.
6. El nombre recibido no se recorta ni normaliza.
7. Una coincidencia parcial devuelve 422 y `CSV_COLUMN_NOT_FOUND`.
8. La ausencia de `column` devuelve 400 y `CSV_COLUMN_REQUIRED`.
9. Un valor vacío o compuesto solo por espacios devuelve 400 y `CSV_COLUMN_REQUIRED`.
10. Una columna inexistente devuelve 422 y `CSV_COLUMN_NOT_FOUND`.
11. Eliminar la única columna devuelve 422 y `CSV_CANNOT_REMOVE_LAST_COLUMN`.
12. Un CSV con encabezado y cero filas puede transformarse y mantiene `previewRowCount: 0`.
13. Un CSV con más de 20 filas devuelve como máximo 20 y mantiene `truncated: true`.
14. Un error del archivo tiene prioridad cuando `file` y `column` son inválidos simultáneamente.
15. Los errores usan la estructura común y no exponen detalles técnicos internos.
16. El archivo original no se modifica, persiste ni se ofrece como descarga.

## 13. Descarga completa después de eliminar una columna

### 13.1. Propósito y alcance

Este endpoint genera un nuevo CSV completo después de eliminar una columna. A diferencia de la vista previa transformada, procesa y devuelve todas las filas válidas del archivo.

- El archivo original no se modifica ni persiste.
- El resultado no se persiste.
- No se crean archivos temporales.
- No se encadenan varias transformaciones.
- Todo el CSV se valida y genera antes de iniciar la respuesta HTTP 200.
- Nunca se devuelve un CSV parcial como resultado correcto.

### 13.2. Endpoint y petición

```http
POST /api/csv/transform/remove-column/download
Content-Type: multipart/form-data
```

Partes multipart:

| Campo | Tipo | Obligatorio | Descripción |
| --- | --- | --- | --- |
| `file` | archivo | Sí | Archivo sujeto a todas las reglas y límites CSV existentes. |
| `column` | texto | Sí | Nombre literal de la columna que debe eliminarse. |

Ejemplo:

```bash
curl --request POST \
  --url http://localhost:8080/api/csv/transform/remove-column/download \
  --form "file=@clientes.csv" \
  --form "column=email" \
  --output clientes-sin-email.csv
```

El archivo se valida completamente antes de devolver los errores relacionados con `column`. Por tanto, un error de contenido CSV tiene prioridad aunque el nombre de columna también sea inválido.

### 13.3. Reglas de transformación

- La coincidencia de `column` es exacta y distingue mayúsculas de minúsculas.
- No se recorta ni normaliza el nombre para localizar el encabezado.
- Un valor ausente, vacío o compuesto únicamente por espacios produce `CSV_COLUMN_REQUIRED`.
- Una columna inexistente produce `CSV_COLUMN_NOT_FOUND`.
- No se permite eliminar la única columna.
- La columna se elimina del encabezado y de todas las filas.
- Se conserva el orden original de las columnas restantes.
- Las filas se procesan secuencialmente y no se acumulan en una colección.

### 13.4. Formato del archivo resultante

La respuesta correcta utiliza:

```http
HTTP/1.1 200 OK
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="clientes-sin-email.csv"
Content-Length: <tamaño exacto>
```

El cuerpo contiene el CSV completo transformado.

Reglas de salida:

- Codificación UTF-8.
- Sin BOM UTF-8.
- Delimitador coma.
- Comillas dobles conforme a las reglas CSV.
- `QuoteMode.MINIMAL`.
- Separador de registros `\n`.
- Los valores no se recortan ni normalizan.
- Los campos vacíos se conservan.
- Las comas, comillas y saltos de línea dentro de campos se escapan correctamente.

Entrada:

```csv
id,nombre,email
1,Ana,ana@example.com
2,Carlos,carlos@example.com
```

Con `column=email`, el cuerpo descargado es:

```csv
id,nombre
1,Ana
2,Carlos
```

### 13.5. Nombre descargable

- Se elimina únicamente la última extensión `.csv`, sin distinguir mayúsculas de minúsculas.
- Las extensiones anteriores se conservan.
- El formato general es `<base>-sin-<columna>.csv`.
- La base y la columna se sanejan únicamente para construir el filename; el valor saneado nunca se usa para buscar el encabezado.
- El fragmento de columna tiene un máximo de 50 caracteres.
- El filename completo tiene un máximo de 180 caracteres.
- El truncado respeta puntos de código Unicode.
- La cabecera se construye como `attachment` con soporte UTF-8.

Ejemplos:

| Entrada | Columna | Descarga |
| --- | --- | --- |
| `clientes.csv` | `email` | `clientes-sin-email.csv` |
| `clientes.CSV` | `email` | `clientes-sin-email.csv` |
| `clientes.backup.csv` | `email` | `clientes.backup-sin-email.csv` |

### 13.6. Errores

Se reutilizan todos los códigos públicos existentes:

- `CSV_FILE_REQUIRED`
- `CSV_FILE_EMPTY`
- `CSV_INVALID_EXTENSION`
- `CSV_FILE_TOO_LARGE`
- `CSV_HEADER_MISSING`
- `CSV_INVALID_HEADER`
- `CSV_MALFORMED`
- `CSV_COLUMN_REQUIRED`
- `CSV_COLUMN_NOT_FOUND`
- `CSV_CANNOT_REMOVE_LAST_COLUMN`
- `UNSUPPORTED_MEDIA_TYPE`
- `INTERNAL_ERROR`

Las respuestas de error usan `application/json` y la estructura común documentada. Si se detecta un error posterior a la fila 20, no se devuelve ninguna parte del CSV generado.

### 13.7. Pruebas de aceptación

1. Un CSV válido devuelve HTTP 200 y todas sus filas transformadas.
2. Un archivo con más de 20 filas incluye todas las filas en la descarga.
3. La columna desaparece del encabezado y de todas las filas.
4. La salida usa UTF-8, no contiene BOM y separa registros con `\n`.
5. Los campos vacíos, comas, comillas, saltos de línea y caracteres UTF-8 se conservan correctamente.
6. `Content-Disposition` es `attachment` y contiene un filename seguro.
7. `Content-Length` coincide exactamente con el cuerpo.
8. `.csv` y `.CSV` se aceptan y eliminan del nombre resultante.
9. Las extensiones anteriores se conservan.
10. El filename no supera 180 caracteres y el fragmento de columna no supera 50.
11. Un error posterior a la fila 20 devuelve el JSON de error y ningún CSV parcial.
12. Un error CSV tiene prioridad sobre un error simultáneo de `column`.
13. Un error de columna se devuelve únicamente después de validar el archivo completo.
14. El archivo original y el resultado no se persisten ni se escriben en archivos temporales.

## 14. Historial de ejecuciones ETL

### 14.1. Endpoint

```http
GET /api/etl/executions
Accept: application/json
```

La respuesta contiene las ejecuciones más recientes, ordenadas desde la más nueva hasta la más antigua.

```json
[
  {
    "id": "ca5b9df0-28ae-4cd1-859a-075a28297d97",
    "type": "REMOVE_COLUMN",
    "status": "SUCCESS",
    "startedAt": "2026-10-09T10:00:00Z",
    "finishedAt": "2026-10-09T10:00:01Z",
    "processedRecordCount": 25,
    "errorMessage": null
  }
]
```

Cuando no existen ejecuciones, la respuesta es un array vacío:

```json
[]
```

### 14.2. Campos

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `id` | UUID | Identificador único de la ejecución. |
| `type` | texto | Tipo de transformación. Actualmente, `REMOVE_COLUMN`. |
| `status` | texto | `PENDING`, `RUNNING`, `SUCCESS` o `FAILED`. |
| `startedAt` | fecha ISO-8601 | Momento de creación de la ejecución. |
| `finishedAt` | fecha ISO-8601 o `null` | Momento de finalización, si la ejecución ha terminado. |
| `processedRecordCount` | número o `null` | Filas de datos procesadas correctamente; no incluye el encabezado. |
| `errorMessage` | texto o `null` | Mensaje público y controlado para una ejecución fallida. |

### 14.3. Reglas y límites

- Esta primera versión registra las descargas completas de eliminación de columna.
- El historial se almacena en memoria y contiene como máximo las últimas 100 ejecuciones.
- El historial es volátil y desaparece al reiniciar el backend.
- No se almacenan archivos, contenido CSV, nombres de archivo, nombres de columnas, datos personales, excepciones ni stack traces.
- Si una ejecución antigua es expulsada por el límite de 100, su finalización posterior no debe interrumpir la transformación CSV.
- Los errores de validación, lectura o generación detectados dentro del caso de uso pueden registrarse como `FAILED`.
- Los errores multipart anteriores al controlador no generan una ejecución.
- Los fallos de transmisión HTTP posteriores a la generación completa no pueden detectarse de forma fiable y pueden permanecer como `SUCCESS`.

### 14.4. Pruebas de aceptación

1. Sin ejecuciones, el endpoint devuelve HTTP 200 y `[]`.
2. Una descarga válida genera una ejecución `SUCCESS` con fecha final y número de registros.
3. Una descarga que falla durante la validación o generación genera una ejecución `FAILED` con un mensaje seguro.
4. El historial nunca contiene más de 100 elementos.
5. Las peticiones concurrentes no corrompen el historial ni interrumpen una descarga por la expulsión de una entrada antigua.
6. La respuesta no contiene nombres de archivo, columnas, contenido CSV ni detalles técnicos de excepciones.
