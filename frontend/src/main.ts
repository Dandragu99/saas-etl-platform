import { bootstrapApplication } from '@angular/platform-browser';
import { Capacitor } from '@capacitor/core';
import { appConfig } from './app/app.config';
import { App } from './app/app';

if (Capacitor.isNativePlatform() && window.location.pathname === '/') {
  window.history.replaceState({}, '', '/mini');
}

bootstrapApplication(App, appConfig)
  .then(() => {
    if (!Capacitor.isNativePlatform() && 'serviceWorker' in navigator) {
      window.addEventListener('load', () =>
        navigator.serviceWorker
          .register('/voltiamini-sw.js', { scope: '/mini/' })
          .catch((error) => console.warn('Voltia Mini offline no está disponible.', error)),
      );
    }
  })
  .catch((err) => console.error(err));
