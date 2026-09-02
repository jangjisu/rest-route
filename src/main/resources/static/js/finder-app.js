/* ===================================================
   finder-app.js — 이름·거리로 찾기 / 목적지로 추천받기 진입점

   실제 동작은 아래 세 모듈에 나눠져 있고, 이 파일은 DOMContentLoaded에서 그것들을 엮기만 하는
   조립부다: finder-entry-flow.js(랜딩 → 위치·연료 팝업), finder-mode1.js(이름·거리로 찾기 화면),
   finder-mode2.js(목적지로 추천받기 화면). 셋 다 서로를 모르고, 위치+관심 항목이 정해지면
   이 파일이 콜백으로 연결해준다.
   =================================================== */

import { initThemeToggle } from './theme.js';
import { requestCurrentPosition } from './finder-geolocation.js';
import { showScreen } from './finder-render.js';
import { initializeFinderEntryFlow } from './finder-entry-flow.js';
import { initializeMode1 } from './finder-mode1.js';
import { initializeMode2 } from './finder-mode2.js';

document.addEventListener('DOMContentLoaded', () => {
    initThemeToggle(document, window);

    const mode1 = initializeMode1(document);
    const mode2 = initializeMode2(document);

    initializeFinderEntryFlow(document, {
        requestCurrentPosition,
        onMode1Ready: mode1.enterMode1,
        onMode2Ready: mode2.enterMode2
    });

    showScreen(document, 'landing');
});
