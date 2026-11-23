const CACHE = 'voltiamini-shell-v2';
const APP_SHELL = '/mini/';
const CORE = [
  APP_SHELL,
  '/manifest.webmanifest',
  '/icons/voltia-mini.svg',
  '/icons/voltia-mini-192.png',
  '/icons/voltia-mini-512.png',
];
const STATIC_DESTINATIONS = new Set(['font', 'image', 'manifest', 'script', 'style']);

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(CACHE)
      .then(async (cache) => {
        await cache.addAll(CORE);
        await cacheBuildAssets(cache);
      })
      .then(() => self.skipWaiting()),
  );
});

async function cacheBuildAssets(cache) {
  const shell = await fetch(APP_SHELL, { cache: 'no-store' });
  if (!shell.ok) return;
  const html = await shell.clone().text();
  await cache.put(APP_SHELL, shell);

  const pending = [...html.matchAll(/(?:src|href)="([^"?#]+\.(?:css|js))"/g)].map(
    (match) => new URL(match[1], self.location.origin).href,
  );
  const visited = new Set();
  while (pending.length > 0) {
    const assetUrl = pending.shift();
    if (!assetUrl || visited.has(assetUrl)) continue;
    visited.add(assetUrl);
    try {
      const response = await fetch(assetUrl);
      if (!response.ok) continue;
      await cache.put(assetUrl, response.clone());
      if (assetUrl.endsWith('.js')) {
        const source = await response.text();
        for (const match of source.matchAll(/chunk-[A-Za-z0-9_-]+\.js/g)) {
          pending.push(new URL(match[0], assetUrl).href);
        }
      }
    } catch {
      // Runtime caching completes any optional static resource on the next online visit.
    }
  }
}

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((key) => key.startsWith('voltiamini-') && key !== CACHE)
            .map((key) => caches.delete(key)),
        ),
      )
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  if (event.request.method !== 'GET' || url.origin !== self.location.origin) return;

  if (event.request.mode === 'navigate' && url.pathname.startsWith('/mini')) {
    event.respondWith(
      fetch(event.request)
        .then((response) => {
          if (response.ok) {
            const copy = response.clone();
            void caches.open(CACHE).then((cache) => cache.put(APP_SHELL, copy));
          }
          return response;
        })
        .catch(() => caches.match(APP_SHELL)),
    );
    return;
  }

  if (!STATIC_DESTINATIONS.has(event.request.destination)) return;
  event.respondWith(
    caches.match(event.request).then(
      (cached) =>
        cached ??
        fetch(event.request).then((response) => {
          if (response.ok && response.type === 'basic') {
            const copy = response.clone();
            void caches.open(CACHE).then((cache) => cache.put(event.request, copy));
          }
          return response;
        }),
    ),
  );
});
