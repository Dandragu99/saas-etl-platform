# Demo para instaladores eléctricos

Esta rama incorpora un prototipo comercial multiempresa. El recorrido preparado para la demostración es:

1. Crear o editar un cliente.
2. Crear un presupuesto con uno o varios conceptos, cantidades, precios e IVA.
3. Convertir el presupuesto en factura.
4. Descargar ambos documentos en PDF.

## Recorrido comercial móvil

La identidad **Voltia** y las pantallas de dashboard, obras, archivo de facturas, inventario y asistente están optimizadas para una presentación desde móvil. El recorrido recomendado es:

1. Revisar facturación, cobros y agenda en **Inicio**.
2. Abrir **Obras** y filtrar trabajos en curso.
3. Consultar la ficha de un cliente en **Clientes**.
4. Entrar en **Presupuestos** para utilizar el flujo real conectado al backend.
5. Filtrar facturas pendientes y abrir su detalle en **Facturas**.
6. Preguntar por tareas y cobros en **Asistente**.

Son funcionales y persistentes la creación y edición de clientes, los presupuestos, la conversión a factura y los PDF asociados. El dashboard comercial, obras, archivo inicial de cuatro facturas, inventario y respuestas del asistente usan datos locales ficticios y aparecen identificados como demostración.

El inventario funcional integrado previamente en Nexo se conserva en `/inventario-operativo`; la ruta `/inventario` muestra la pantalla visual simplificada de la demo.

La opción **Cargar datos de demo** crea un cliente y una obra ficticios únicamente cuando la empresa activa todavía no tiene clientes. Los datos se guardan bajo la empresa activa.

## Arranque local

Define `DATABASE_PASSWORD` con el mismo valor para Docker y Spring Boot. Después:

```bash
docker compose up -d postgres
cd backend
./mvnw spring-boot:run
```

En otra terminal:

```bash
cd frontend
npm ci
npm start
```

Abre `http://localhost:4200`, registra una empresa o inicia sesión y entra en **Clientes y ventas**. El backend escucha en `http://localhost:8080` y PostgreSQL se publica sólo en `localhost:55432`.

## Alcance del prototipo

Los PDFs son documentos de demostración. No implementan series fiscales configurables, firma, rectificación, estados de cobro, normativa VeriFactu ni integración contable, por lo que no deben considerarse aptos para facturación real.

La primera versión crea una obra ficticia para alimentar el dashboard, pero la gestión completa de obras, la factura desde cero y la configuración de los datos fiscales del emisor quedan para una iteración posterior.

## Preparación para despliegue (sin desplegar)

El backend acepta el puerto de la plataforma mediante `PORT` y mantiene `8080` en local. Para Railway deben configurarse, como secretos del servicio, `DATABASE_URL` en formato JDBC, `DATABASE_USERNAME`, `DATABASE_PASSWORD` y `SESSION_COOKIE_SECURE=true`. Flyway aplicará las migraciones al arrancar.

Angular genera una aplicación estática con `npm run build`; el directorio publicable es `frontend/dist/saas-etl-frontend/browser`. En Netlify será necesario añadir una regla SPA `/* /index.html 200` y un proxy `/api/*` hacia el dominio definitivo del backend. El proxy permite conservar cookies de sesión y evita incorporar una URL de producción al código. Esa regla no se incluye con un dominio inventado: debe completarse cuando exista el servicio Railway.

Para probar desde un móvil antes de desplegar, ambos servicios deben exponerse desde el mismo host o mediante un proxy HTTPS. No se deben abrir directamente PostgreSQL ni sus credenciales a Internet.
