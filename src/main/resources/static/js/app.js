/* ===================================================
   app.js — 진입점 (entry point)
   =================================================== */

import { initRestStopMap } from './rest-stops-map.js';
import { initThemeToggle } from './theme.js';
import { initAnalytics, initButtonClickTracking } from './analytics.js';

document.addEventListener('DOMContentLoaded', () => {
    initRestStopMap();
    initThemeToggle(document, window);
    initAnalytics();
    initButtonClickTracking();
});
