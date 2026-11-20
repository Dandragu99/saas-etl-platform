# Voltia Mini — demo móvil offline

Voltia Mini es una propuesta independiente para un electricista autónomo. Se sirve desde `/mini`, no requiere iniciar sesión y sus funciones básicas no llaman a Spring Boot ni PostgreSQL.

## Qué es funcional

- Clientes creados y editados con persistencia en IndexedDB.
- Presupuestos con materiales, mano de obra, cantidades, precios, IVA y cálculo automático.
- Historial con búsqueda por cliente y filtro de estado.
- PDF generado completamente en el navegador, descargable y compartible mediante Web Share cuando Android lo permite.
- Trabajos persistentes con estados pendiente, en curso y finalizado.
- Datos del negocio y logotipo guardados localmente.
- Manifiesto PWA y service worker para instalación y funcionamiento offline.

Los datos iniciales son ficticios. Los documentos incluyen el aviso **no válido para emisión fiscal**. La aplicación no implementa facturación legal, contabilidad ni sincronización remota.

## Ejecutar en local

```bash
cd frontend
npm ci
npm start
```

Abre `http://localhost:4200/mini`. IndexedDB pertenece al navegador y al origen, por lo que limpiar los datos del sitio elimina clientes, trabajos, ajustes y presupuestos de Voltia Mini.

Para probar exactamente el service worker debe utilizarse el build de producción sobre HTTPS o `localhost`:

```bash
npm run build
npx http-server dist/saas-etl-frontend/browser -p 4200 --proxy http://localhost:4200?
```

Tras visitar la aplicación una vez con conexión, Chrome mostrará **Instalar aplicación**. En Android también puede instalarse desde el menú de Chrome. Conviene abrirla una segunda vez antes de hacer la prueba sin conexión para comprobar que la versión actual está controlada por el service worker.

## Publicación como PWA

El contenido estático de `frontend/dist/saas-etl-frontend/browser` puede publicarse en Netlify u otro hosting HTTPS. Debe existir una regla SPA que devuelva `index.html` para `/mini` y sus rutas. Voltia Mini no necesita proxy `/api` para sus funciones locales.

## Conversión posterior a APK con Capacitor

No se ha instalado el SDK Android en el contenedor. En un equipo con Android Studio:

```bash
cd frontend
npm install @capacitor/core @capacitor/cli @capacitor/android
npx cap init "Voltia Mini" "com.voltia.mini" --web-dir dist/saas-etl-frontend/browser
npm run build
npx cap add android
npx cap sync android
npx cap open android
```

Desde Android Studio se puede generar un APK de depuración o configurar la firma para una versión distribuible. Antes de publicar habría que revisar permisos, copias de seguridad, iconos PNG adaptativos, política de privacidad y estrategia de migración de IndexedDB.

## Limitaciones conocidas

- Los datos sólo existen en el dispositivo y perfil de navegador actual; no hay cuenta, sincronización ni copia de seguridad.
- Desinstalar la PWA o borrar datos del sitio puede eliminar la información.
- El logotipo se conserva localmente, pero la primera plantilla PDF utiliza el nombre comercial en texto.
- La numeración de presupuestos es local y demostrativa.
- El PDF es deliberadamente sencillo y de una página.
- No hay sistema fiscal real ni facturas legalmente válidas.
