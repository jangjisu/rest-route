import { initializeAdminRestStopRestroomLink } from './admin-rest-stop-restroom-link.js';
import { showToast } from './admin-common.js';

if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', () => {
        initializeAdminRestStopRestroomLink(document, {
            onNotice: (message, type) => showToast(document, message, type)
        });
    });
}
