# PROJECT_SPEC.md

## 1. Nombre provisional

SaaS ETL Platform

El nombre comercial definitivo se decidirá más adelante.

## 2. Visión del producto

Construir una plataforma web que permita a usuarios sin conocimientos avanzados de programación subir archivos de datos, aplicar transformaciones y descargar el resultado.

La primera versión estará centrada exclusivamente en archivos CSV.

El proyecto debe servir también como proyecto profesional de portfolio, demostrando conocimientos de:

* Java y Spring Boot.
* Angular y TypeScript.
* PostgreSQL.
* APIs REST.
* Procesamiento de archivos.
* Autenticación y autorización.
* Pruebas automatizadas.
* Docker.
* Arquitectura de software.
* Despliegue de aplicaciones.

## 3. Problema que resuelve

Muchas personas y pequeñas empresas trabajan con archivos CSV que necesitan ser limpiados o modificados antes de utilizarlos.

Actualmente estas tareas suelen requerir:

* Conocimientos de Excel.
* Scripts personalizados.
* Herramientas técnicas.
* Procesos manuales repetitivos.

La plataforma permitirá realizar transformaciones habituales mediante una interfaz web sencilla.

## 4. Usuario objetivo inicial

El usuario inicial será una persona o pequeña empresa que necesita modificar archivos CSV sin escribir código.

Ejemplos:

* Eliminar columnas innecesarias.
* Renombrar columnas.
* Filtrar registros.
* Reemplazar valores.
* Eliminar duplicados.
* Tratar valores vacíos.
* Cambiar formatos o tipos de datos.
* Descargar el resultado transformado.

## 5. Flujo principal del usuario

1. El usuario crea una cuenta.
2. Inicia sesión.
3. Sube un archivo CSV.
4. La plataforma valida el archivo.
5. El usuario ve las columnas y una vista previa de los datos.
6. Selecciona una o varias transformaciones.
7. Configura los parámetros de cada transformación.
8. Ejecuta el proceso.
9. La plataforma muestra el resultado o los errores encontrados.
10. El usuario descarga el CSV transformado.
11. El usuario puede consultar su historial de ejecuciones.

## 6. Alcance del MVP

El MVP incluirá:

### Autenticación

* Registro de usuario.
* Inicio de sesión.
* Cierre de sesión.
* Protección de las funcionalidades privadas.
* Asociación de los trabajos ETL con su propietario.

### Carga de archivos

* Subida de archivos CSV.
* Validación de archivos vacíos.
* Validación de extensión y contenido.
* Límite de tamaño configurable.
* Lectura de encabezados.
* Vista previa de un número limitado de filas.

### Transformaciones iniciales

* Renombrar una columna.
* Eliminar una columna.
* Filtrar filas por condición.
* Reemplazar valores.
* Eliminar filas duplicadas.
* Gestionar valores vacíos.
* Convertir tipos básicos cuando sea posible.

### Ejecución

* Aplicación ordenada de las transformaciones seleccionadas.
* Generación de un nuevo archivo CSV.
* Información sobre el estado de la ejecución.
* Gestión de errores comprensible para el usuario.

### Historial

* Registro de trabajos realizados.
* Fecha y hora de ejecución.
* Nombre original del archivo.
* Estado del trabajo.
* Transformaciones aplicadas.
* Posibilidad de consultar el detalle de una ejecución.

### Descarga

* Descarga del archivo transformado.
* Nombre de archivo generado de forma segura.
* Acceso únicamente por parte del propietario del trabajo.

## 7. Fuera del alcance inicial

No formarán parte del primer MVP:

* Archivos Excel.
* Archivos JSON.
* Inteligencia artificial.
* Stripe o pagos.
* Suscripciones.
* Equipos y organizaciones.
* Programación automática de procesos.
* Integraciones con servicios externos.
* API pública para clientes.
* Procesamiento distribuido.
* Microservicios.
* Kafka.
* Kubernetes.
* Redis.
* Aplicaciones móviles.
* Colaboración en tiempo real.

Estas funcionalidades podrán evaluarse después de completar y desplegar el MVP.

## 8. Primera funcionalidad vertical

La primera entrega funcional será la vista previa de un archivo CSV.

Flujo:

1. El usuario selecciona un CSV desde Angular.
2. Angular envía el archivo al backend mediante `multipart/form-data`.
3. Spring Boot valida el archivo.
4. El backend lee los encabezados.
5. El backend lee un máximo de 20 filas.
6. El backend devuelve una respuesta JSON.
7. Angular muestra los datos en una tabla.

Esta primera entrega no incluirá:

* Autenticación.
* PostgreSQL.
* Guardado permanente del archivo.
* Transformaciones.
* Historial.
* Docker obligatorio.
* Diseño visual avanzado.

## 9. Criterios de aceptación de la primera entrega

La primera entrega se considerará terminada cuando:

* El usuario pueda seleccionar un archivo CSV.
* El frontend pueda enviarlo al backend.
* El backend rechace archivos vacíos.
* El backend rechace archivos no válidos.
* Se aplique un límite de tamaño.
* Se devuelvan los encabezados.
* Se devuelva un máximo de 20 filas.
* Los errores tengan mensajes comprensibles.
* Angular muestre la vista previa.
* Backend y frontend compilen correctamente.
* Existan pruebas para la lógica principal del backend.

## 10. Arquitectura inicial

Se utilizará un monolito modular.

El proyecto tendrá dos aplicaciones principales:

```text
saas-etl/
├── backend/
├── frontend/
├── docs/
├── docker/
├── AGENTS.md
├── README.md
└── docker-compose.yml
```

### Backend

Tecnologías:

* Java LTS.
* Spring Boot.
* Maven.
* PostgreSQL.
* Spring Data JPA.
* Flyway para migraciones.
* Spring Security cuando se implemente autenticación.
* JUnit y Mockito para pruebas.

Separación inicial:

* API o controladores.
* Aplicación o casos de uso.
* Dominio.
* Persistencia.
* Infraestructura.

Módulos funcionales previstos:

* Usuarios y autenticación.
* Carga de archivos.
* Vista previa de datos.
* Transformaciones.
* Ejecuciones ETL.
* Historial.
* Descargas.

### Frontend

Tecnologías:

* Angular.
* TypeScript.
* Componentes standalone.
* Configuración estricta.
* Cliente HTTP.
* Formularios reactivos cuando sean necesarios.

Separación inicial:

* Componentes de presentación.
* Servicios de acceso a API.
* Modelos y tipos.
* Estado de la aplicación.
* Gestión de errores.

### Base de datos

La primera versión del seguimiento operativo de ejecuciones utiliza un historial en memoria,
volátil y limitado a 100 entradas. Se utilizará PostgreSQL cuando se introduzcan usuarios y
el historial persistente definitivo.

Los archivos no se almacenarán directamente dentro de PostgreSQL.

La estrategia definitiva de almacenamiento de archivos se decidirá antes de implementar el historial y las descargas persistentes.

## 11. Entidades principales previstas

### User

Representa a un usuario registrado.

Posibles atributos:

* Identificador.
* Correo electrónico.
* Contraseña cifrada.
* Fecha de creación.
* Estado de la cuenta.

### EtlJob

Representa una ejecución o trabajo ETL.

Posibles atributos:

* Identificador.
* Usuario propietario.
* Nombre original del archivo.
* Estado.
* Fecha de creación.
* Fecha de inicio.
* Fecha de finalización.
* Mensaje de error.

### Transformation

Representa una transformación configurada dentro de un trabajo.

Posibles atributos:

* Identificador.
* Trabajo relacionado.
* Tipo de transformación.
* Orden de ejecución.
* Configuración.

### StoredFile

Representa los metadatos de un archivo utilizado o generado.

Posibles atributos:

* Identificador.
* Trabajo relacionado.
* Nombre original.
* Nombre almacenado.
* Tipo de archivo.
* Tamaño.
* Ubicación.
* Fecha de creación.

Estas entidades son provisionales y deberán validarse antes de implementar la persistencia.

## 12. Reglas de negocio iniciales

* Un usuario solo puede acceder a sus propios trabajos y archivos.
* Los archivos deben cumplir los límites de tamaño configurados.
* Todos los archivos subidos se consideran datos no confiables.
* Una transformación debe aplicarse en el orden indicado.
* Si una transformación falla, el trabajo debe registrar el error.
* No debe generarse un resultado que se presente como correcto cuando el procesamiento haya fallado.
* Los nombres de archivo deben tratarse de forma segura.
* Las contraseñas nunca deben almacenarse sin cifrar.
* Los secretos no deben incluirse en el repositorio.

## 13. Riesgos técnicos

### CSV no uniforme

Los archivos CSV pueden utilizar diferentes:

* Separadores.
* Codificaciones.
* Comillas.
* Saltos de línea.
* Encabezados.
* Formatos numéricos.

La primera versión podrá aceptar un conjunto limitado y documentado de formatos.

### Archivos grandes

Cargar archivos completos en memoria puede provocar problemas de rendimiento.

El MVP utilizará límites pequeños y posteriormente podrá evolucionar hacia procesamiento por streaming.

### Inferencia de tipos

Determinar automáticamente si una columna contiene números, fechas o texto puede generar errores.

La inferencia inicial debe ser conservadora y permitir que el usuario corrija decisiones cuando se implemente.

### Seguridad

Los archivos subidos pueden contener datos maliciosos, nombres manipulados o contenido inesperado.

Será obligatorio validar tamaño, nombre, extensión y contenido.

### Complejidad prematura

Existe el riesgo de añadir pagos, inteligencia artificial, microservicios o infraestructura avanzada antes de completar el flujo principal.

Estas ampliaciones quedan expresamente fuera del MVP.

## 14. Entregas previstas

### Entrega 0: preparación

* Documentación inicial.
* Reglas de trabajo.
* Decisiones tecnológicas.
* Estructura del repositorio.
* Configuración de Git.

### Entrega 1: vista previa CSV

* Backend mínimo.
* Frontend mínimo.
* Carga de CSV.
* Validación.
* Vista previa.

### Entrega 2: primera transformación

* Eliminar una columna.
* Vista previa del resultado.
* Pruebas de transformación.

### Entrega 3: motor de transformaciones

* Varias transformaciones.
* Orden configurable.
* Gestión de errores.
* Generación del CSV resultante.

### Entrega 4: autenticación

* Registro.
* Inicio de sesión.
* Seguridad de endpoints.
* Usuario propietario.

### Entrega 5: persistencia e historial

* PostgreSQL.
* Migraciones.
* Trabajos ETL.
* Estados.
* Historial.

### Entrega 6: almacenamiento y descarga

* Estrategia de almacenamiento.
* Archivo resultante.
* Descarga segura.
* Limpieza de archivos temporales.

### Entrega 7: despliegue del MVP

* Docker Compose.
* Variables de entorno.
* Configuración de producción.
* CI.
* Despliegue.
* Documentación de uso.

## 15. Definición general de terminado

Una entrega estará terminada cuando:

* Cumpla sus criterios de aceptación.
* El código compile.
* Las pruebas relevantes pasen.
* No contenga secretos.
* No incluya funcionalidades fuera del alcance.
* Los errores estén gestionados.
* La documentación necesaria esté actualizada.
* El propietario del repositorio pueda explicar el funcionamiento principal.
