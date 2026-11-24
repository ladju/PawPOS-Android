/* Paw POS Offline-First Service Worker (GitHub Pages & Localhost Ready) */
const CACHE_VERSION = "v12";
const CACHE_NAME = `pawpos-offline-${CACHE_VERSION}`;

const PRECACHE_ASSETS = [
  "./",
  "./index.html",
  "./manifest.webmanifest",
  "./icons/icon-192.png",
  "./icons/icon-512.png",
  "./icons/icon-maskable-512.png",
  "./icons/apple-touch-icon.png"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(PRECACHE_ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((key) => key.startsWith("pawpos-offline-") && key !== CACHE_NAME)
            .map((key) => caches.delete(key))
        )
      )
      .then(() => self.clients.claim())
  );
});

self.addEventListener("message", (event) => {
  const data = event.data || {};
  if (data.type === "SKIP_WAITING") {
    self.skipWaiting();
    return;
  }
  if (data.type === "GET_CACHE_STATUS" && event.ports && event.ports[0]) {
    caches.open(CACHE_NAME)
      .then((cache) => cache.keys())
      .then((keys) => {
        event.ports[0].postMessage({
          ready: keys.length >= 2,
          version: CACHE_VERSION,
          cachedCount: keys.length
        });
      })
      .catch(() => {
        event.ports[0].postMessage({ ready: false, version: CACHE_VERSION, cachedCount: 0 });
      });
  }
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;

  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  // Navigation requests (HTML): Network-First so GitHub Pages updates apply immediately,
  // with guaranteed instant Cache fallback when offline or in airplane mode.
  if (req.mode === "navigate" || (req.headers.get("accept") || "").includes("text/html")) {
    event.respondWith(
      fetch(req)
        .then((networkRes) => {
          if (networkRes && networkRes.ok) {
            const copy = networkRes.clone();
            caches.open(CACHE_NAME).then((cache) => {
              cache.put("./index.html", copy);
            });
          }
          return networkRes;
        })
        .catch(() =>
          caches.match(req, { ignoreSearch: true })
            .then((cached) => cached || caches.match("./index.html") || caches.match("./"))
        )
    );
    return;
  }

  // Same-origin static assets (icons, manifest): Cache-First with background refresh
  event.respondWith(
    caches.match(req, { ignoreSearch: true }).then((cached) => {
      const networkFetch = fetch(req)
        .then((networkRes) => {
          if (networkRes && networkRes.ok) {
            const copy = networkRes.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(req, copy));
          }
          return networkRes;
        })
        .catch(() => cached);

      return cached || networkFetch;
    })
  );
});
