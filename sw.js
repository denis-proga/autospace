// Service worker нужен только для того, чтобы приложение можно было установить на телефон
// и компьютер. Он НИЧЕГО не кеширует: тесты, вопросы и оплата всегда берутся с сервера,
// иначе после обновления люди видели бы старую версию.
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()));
self.addEventListener('fetch', () => {});
