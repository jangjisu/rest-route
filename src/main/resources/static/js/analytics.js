const ANALYTICS_CONFIG_ENDPOINT = '/api/analytics-config';
const GA_SCRIPT_ID = 'ga4-script';

/**
 * GA4로 임의의 이름을 가진 이벤트를 보낸다. 측정 ID가 설정 안 됐거나 광고 차단 등으로
 * window.gtag가 없을 때도 안전하게 아무 일 없이 넘어간다.
 */
export function trackEvent(eventName, params = {}, gtagFn = globalThis.window?.gtag) {
    if (typeof gtagFn !== 'function') {
        return false;
    }
    gtagFn('event', eventName, params);
    return true;
}

/** 화면 전환을 GA4 표준 이벤트인 screen_view로 보낸다. */
export function trackScreenView(screenName, extraParams = {}, gtagFn = globalThis.window?.gtag) {
    return trackEvent('screen_view', { screen_name: screenName, ...extraParams }, gtagFn);
}

/**
 * 어떤 버튼이 눌렸는지 사람이 알아볼 수 있는 이름을 뽑는다.
 * aria-label > 텍스트 내용 > id 순으로 쓰고, 아무 것도 없으면 'unknown'.
 */
export function describeButtonClick(button) {
    const ariaLabel = button.getAttribute('aria-label');
    const text = button.textContent?.trim();
    const label = ariaLabel || text || button.id || 'unknown';
    return { button_label: label, button_id: button.id || undefined };
}

/**
 * document 전체에 클릭 위임 리스너를 하나만 달아서, button(또는 role=button) 클릭마다
 * button_click 이벤트를 보낸다. 나중에 새로 생기는 버튼도 따로 등록할 필요 없이 자동으로 잡힌다.
 */
export function initButtonClickTracking(doc = document, gtagFn = globalThis.window?.gtag) {
    doc.addEventListener('click', (event) => {
        const button = event.target.closest?.('button, [role="button"]');
        if (!button) {
            return;
        }
        trackEvent('button_click', describeButtonClick(button), gtagFn);
    });
}

/**
 * 서버에서 GA4 측정 id를 받아와, 설정돼 있을 때만 gtag.js를 붙이고 초기화한다.
 * 네이버 지도 키를 /api/map-config로 받아와 스크립트를 붙이는 것과 같은 방식이다.
 */
export async function initAnalytics(fetchImpl = fetch, doc = document) {
    const measurementId = await fetchMeasurementId(fetchImpl);
    if (!measurementId) {
        return false;
    }

    loadGtagScript(measurementId, doc);
    return true;
}

async function fetchMeasurementId(fetchImpl) {
    const response = await fetchImpl(ANALYTICS_CONFIG_ENDPOINT);
    const body = await response.json();

    if (!response.ok || body.code !== 'SUCCESS') {
        throw new Error(`Analytics config API failed: ${body.code}`);
    }

    return body.data?.measurementId ?? '';
}

function loadGtagScript(measurementId, doc) {
    if (doc.getElementById(GA_SCRIPT_ID)) {
        return;
    }

    const script = doc.createElement('script');
    script.id = GA_SCRIPT_ID;
    script.async = true;
    script.src = `https://www.googletagmanager.com/gtag/js?id=${measurementId}`;
    doc.head.appendChild(script);

    const win = doc.defaultView ?? globalThis.window;
    win.dataLayer = win.dataLayer || [];
    win.gtag = function gtag() {
        win.dataLayer.push(arguments);
    };
    win.gtag('js', new Date());
    win.gtag('config', measurementId);
}
