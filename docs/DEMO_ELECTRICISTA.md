# Demo para instaladores eléctricos

Esta rama incorpora un prototipo comercial multiempresa. El recorrido preparado para la demostración es:

1. Crear o editar un cliente.
2. Crear un presupuesto con uno o varios conceptos, cantidades, precios e IVA.
3. Convertir el presupuesto en factura.
4. Descargar ambos documentos en PDF.

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
