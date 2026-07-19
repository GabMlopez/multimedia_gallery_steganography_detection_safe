module.exports = async (browser) => {
  const page = await browser.newPage();

  await page.evaluateOnNewDocument(() => {
    localStorage.setItem('user', JSON.stringify({
      id: 1,
      username: 'supervisor',
      role: 'ROLE_SUPERVISOR'
    }));

    const API_PATTERN = '/api/';

    function isApiCall(url) {
      return url && typeof url === 'string' && url.includes(API_PATTERN);
    }

    function mockResponse() {
      return {
        status: 200,
        statusText: 'OK',
        ok: true,
        headers: new Headers({ 'Content-Type': 'application/json' }),
        text: () => Promise.resolve('[]'),
        json: () => Promise.resolve([]),
        blob: () => Promise.resolve(new Blob(['[]'], { type: 'application/json' })),
        arrayBuffer: () => Promise.resolve(new TextEncoder().encode('[]').buffer),
        clone() { return this; }
      };
    }

    const origFetch = window.fetch;
    window.fetch = function (input, init) {
      const url = typeof input === 'string' ? input : (input && input.url);
      if (isApiCall(url)) {
        return Promise.resolve(mockResponse());
      }
      return origFetch.call(this, input, init);
    };

    const origXhrOpen = XMLHttpRequest.prototype.open;
    const origXhrSend = XMLHttpRequest.prototype.send;
    const origXhrSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader;

    XMLHttpRequest.prototype.open = function (method, url) {
      this._apiUrl = typeof url === 'string' ? url : (url ? String(url) : '');
      this._apiMethod = method;
      this._isApi = isApiCall(this._apiUrl);
      if (!this._isApi) {
        return origXhrOpen.apply(this, arguments);
      }
      this._savedUrl = this._apiUrl;
      return origXhrOpen.apply(this, [method, 'data:text/plain,']);
    };

    XMLHttpRequest.prototype.setRequestHeader = function (header, value) {
      if (!this._isApi) {
        return origXhrSetRequestHeader.apply(this, arguments);
      }
    };

    XMLHttpRequest.prototype.send = function (body) {
      if (!this._isApi) {
        return origXhrSend.apply(this, arguments);
      }

      const xhr = this;
      setTimeout(function () {
        try {
          Object.defineProperty(xhr, 'readyState', { value: 4, configurable: true, writable: true });
          Object.defineProperty(xhr, 'status', { value: 200, configurable: true, writable: true });
          Object.defineProperty(xhr, 'statusText', { value: 'OK', configurable: true, writable: true });
          Object.defineProperty(xhr, 'responseText', { value: '[]', configurable: true, writable: true });
          Object.defineProperty(xhr, 'response', { value: '[]', configurable: true, writable: true });
        } catch (_) { /* readonly in some browsers */ }

        if (xhr.onreadystatechange) xhr.onreadystatechange(new Event('readystatechange'));
        if (xhr.onload) xhr.onload(new ProgressEvent('load'));
        if (xhr.onloadend) xhr.onloadend(new ProgressEvent('loadend'));
        xhr.dispatchEvent(new Event('readystatechange'));
        xhr.dispatchEvent(new Event('load'));
        xhr.dispatchEvent(new Event('loadend'));
      }, 10);
    };
  });

  await page.goto('http://localhost:3001/supervisor', {
    waitUntil: 'networkidle0',
    timeout: 30000
  });

  await page.close();
};
