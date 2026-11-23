# Voltia Mini en Netlify y iPhone

Esta preparación publica únicamente archivos estáticos. Voltia Mini funciona en `/mini/`, sin Spring Boot, PostgreSQL, autenticación ni sincronización remota. Los datos permanecen en IndexedDB en el navegador del dispositivo.

> Voltia Mini es una beta de demostración. Los presupuestos generados no son válidos para emisión fiscal.

## Publicar cuando se autorice

1. En Netlify, seleccione **Add new site > Import an existing project** y conecte el repositorio `Dandragu99/saas-etl-platform`.
2. Elija la rama que se quiera publicar. Para revisar esta preparación, use `feature/voltiamini-netlify-ios`.
3. Netlify leerá `netlify.toml` desde la raíz. Confirme estos valores:
   - Base directory: `frontend`
   - Build command: `npm ci && npm run build`
   - Publish directory: `frontend/dist/saas-etl-frontend/browser` desde la raíz, o `dist/saas-etl-frontend/browser` respecto a la base.
4. No añada variables de backend, credenciales ni claves API; la demo no las necesita.
5. Ejecute **Deploy site** únicamente cuando exista autorización.
6. Abra `https://<sitio>.netlify.app/mini/`. Compruebe una recarga directa también en `/mini/clientes` y `/mini/presupuestos`.

La redirección SPA de `netlify.toml` devuelve `index.html` para las rutas Angular. El service worker solo controla `/mini/`, guarda el shell y recursos estáticos y no almacena IndexedDB ni respuestas de API.

## Instalación desde Safari en iPhone

1. Abra la URL `/mini/` en Safari, no en el navegador interno de otra aplicación.
2. Pulse **Compartir**.
3. Seleccione **Añadir a pantalla de inicio**.
4. Confirme el nombre “Voltia Mini” y abra el icono instalado.

Safari separa los datos por origen. Cambiar de subdominio o dominio crea otro almacenamiento local. El modo privado y la opción **Borrar historial y datos de sitios web** pueden eliminar IndexedDB.

## Checklist manual en iPhone

- [ ] Safari abre `/mini/` sin redirigir a login ni mostrar Voltia Pro.
- [ ] El título, icono, color y etiqueta Beta se muestran correctamente.
- [ ] **Añadir a pantalla de inicio** instala la aplicación y la abre sin la barra de Safari.
- [ ] La cabecera y navegación respetan notch, Dynamic Island y área inferior segura.
- [ ] Inicio, Clientes, Presupuestos, Trabajos y Ajustes son navegables.
- [ ] Crear y editar un cliente; cerrar y reabrir la PWA; confirmar que persiste.
- [ ] Crear un presupuesto con materiales, mano de obra, decimales e IVA.
- [ ] Confirmar que el teclado no tapa el campo activo ni provoca zoom de página.
- [ ] Descargar el PDF desde Safari y abrirlo desde Archivos/Descargas.
- [ ] Compartir el PDF y cancelar una compartición sin que aparezca un error.
- [ ] Cambiar el estado de un trabajo y comprobar persistencia tras reiniciar.
- [ ] Activar modo avión después de una primera carga completa y volver a abrir `/mini/`.
- [ ] Recuperar conexión, recargar y confirmar que una actualización no borra datos.
- [ ] Recargar directamente `/mini/clientes` y `/mini/presupuestos` para verificar la regla SPA.

## Datos de demostración y privacidad

Los datos iniciales son ficticios y solo se insertan cuando la base local no contiene clientes. La aplicación no borra ni reemplaza automáticamente datos existentes. No deben utilizarse datos personales reales durante la evaluación; no existe copia de seguridad ni recuperación remota.

## Limitaciones de Safari/iOS

- iOS puede purgar almacenamiento de sitios poco utilizados cuando necesita espacio. Una PWA instalada suele tener una persistencia más predecible, pero no sustituye una copia de seguridad.
- El funcionamiento offline requiere haber abierto la aplicación al menos una vez con conexión y haber completado la instalación del service worker.
- La descarga de archivos puede abrir una vista previa o la hoja de compartir en lugar de descargar directamente, según la versión de iOS.
- Safari limita procesos en segundo plano; no hay sincronización ni notificaciones push en esta demo.
- La instalación PWA debe hacerse manualmente desde Safari.
- La compatibilidad visual y de teclado debe validarse en un iPhone físico.

## Android

Capacitor sigue usando `dist/saas-etl-frontend/browser`. Para verificar y actualizar el proyecto nativo:

```bash
cd frontend
npm run build:android
```

Los cambios web conservan la rama nativa: `Capacitor.isNativePlatform()` mantiene Filesystem y Share en Android, mientras Safari usa Blob, descarga y Web Share. La compilación final de APK requiere Android SDK o Android Studio.
