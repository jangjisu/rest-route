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

test('index applies a stored or system theme before first paint to avoid a flash', async () => {
    const template = await readFile('src/main/resources/templates/index.html', 'utf8');

    assert.match(template, /localStorage\.getItem\('rr-theme'\)/);
    assert.match(template, /matchMedia\('\(prefers-color-scheme: dark\)'\)/);
    assert.match(template, /document\.documentElement\.setAttribute\('data-theme', theme\)/);
});

test('navbar exposes a theme toggle button with icon and label hooks', async () => {
    const template = await readFile('src/main/resources/templates/index.html', 'utf8');

    assert.match(template, /id="themeToggleButton"\s+type="button"\s+class="rr-theme-toggle"/);
    assert.match(template, /id="themeToggleIcon"/);
    assert.match(template, /id="themeToggleLabel"/);
});

test('destination route actions expose emphasized search and map button hooks', async () => {
    const template = await readFile('src/main/resources/templates/index.html', 'utf8');

    assert.match(
        template,
        /id="routeDestinationSearchButton"\s+class="btn btn-primary route-destination-search-button"/
    );
    assert.match(
        template,
        /id="routeDestinationSearchButton"[\s\S]*?<i class="bi bi-search" aria-hidden="true"><\/i>[\s\S]*?검색/
    );
    assert.match(
        template,
        /id="routeDestinationMapButton"\s+class="btn btn-outline-secondary route-destination-map-button"/
    );
});
