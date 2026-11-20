# Voltia Mini para Android

Voltia Mini se distribuye como una aplicación Angular empaquetada con Capacitor. La aplicación abre directamente en `/mini`, no requiere autenticación, Spring Boot ni PostgreSQL y conserva clientes, presupuestos, trabajos y ajustes en IndexedDB dentro del almacenamiento privado de Android.

Los documentos generados siguen siendo demostrativos y no son válidos para emisión fiscal.

## Requisitos en Windows

- Node.js 22 o posterior y npm.
- Android Studio con Android SDK, Platform Tools y un JDK compatible instalados.
- Un dispositivo Android con depuración USB o un emulador configurado.

## Primera compilación

```powershell
git switch feature/voltiamini-android
cd frontend
npm ci
npm run build:android
npx cap open android
```

`build:android` compila Angular y sincroniza el resultado de `dist/saas-etl-frontend/browser` con el proyecto nativo. El proyecto Android ya está generado: no se debe volver a ejecutar `npx cap add android`.

En Android Studio, espere a que finalice la sincronización de Gradle y seleccione **Build > Build APK(s)**. La APK de depuración se crea normalmente en:

```text
frontend/android/app/build/outputs/apk/debug/app-debug.apk
```

También se puede compilar desde PowerShell, después de que Android Studio haya configurado el SDK:

```powershell
cd frontend/android
.\gradlew.bat assembleDebug
```

Instalación mediante ADB:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Actualizar la aplicación

Después de cambiar Angular o las dependencias nativas:

```powershell
cd frontend
npm ci
npm run build:android
```

A continuación, vuelva a compilar desde Android Studio. `npx cap sync android` copia los recursos web y actualiza los plugins; no elimina los datos de la aplicación instalada. Para conservar IndexedDB al instalar una actualización, mantenga el mismo `appId` (`com.voltia.mini`) y no desinstale la aplicación.

## PDF, datos y funcionamiento sin conexión

- En Android, los PDF se escriben con Capacitor Filesystem y se abren o comparten mediante la hoja nativa de Android.
- En navegador/PWA se mantiene la descarga y Web Share existentes.
- IndexedDB funciona en el WebView con el origen estable `https://localhost` y persiste mientras no se borren los datos o se desinstale la aplicación.
- Los recursos Angular están incluidos en la APK, por lo que las funciones de Voltia Mini no necesitan Internet ni backend.
- El permiso de Internet permanece en el manifiesto generado por Capacitor, pero Voltia Mini no depende de servicios remotos para su flujo básico.

## Publicación futura

Para distribución fuera de una demo, genere un Android App Bundle firmado desde **Build > Generate Signed Bundle / APK**, proteja el keystore fuera del repositorio e incremente `versionCode` y `versionName` en `frontend/android/app/build.gradle` en cada versión.

## Limitaciones actuales

- La APK de depuración no está firmada para publicación en Google Play.
- Los datos son locales a un único dispositivo y no tienen sincronización ni copia de seguridad de negocio.
- Borrar datos o desinstalar la aplicación elimina la información de IndexedDB.
- El logotipo configurable del negocio no se incorpora todavía al PDF.
- Los documentos son ejemplos comerciales, no facturas ni presupuestos fiscalmente certificados.
