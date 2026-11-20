import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.voltia.mini',
  appName: 'Voltia Mini',
  webDir: 'dist/saas-etl-frontend/browser',
  server: {
    androidScheme: 'https',
  },
  android: {
    backgroundColor: '#071f35',
  },
};

export default config;
