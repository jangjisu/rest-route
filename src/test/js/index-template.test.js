import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

test('index exposes CSRF token and header name metadata', async () => {
    const template = await readFile('src/main/resources/templates/index.html', 'utf8');

    assert.match(template, /<meta name="_csrf" th:content="\$\{_csrf\?\.token}">/);
    assert.match(template, /<meta name="_csrf_header" th:content="\$\{_csrf\?\.headerName}">/);
});

test('route result modal provides a rest stop filter container', async () => {
    const template = await readFile('src/main/resources/templates/index.html', 'utf8');

    assert.match(template, /id="routeRestStopFilters"/);
});
