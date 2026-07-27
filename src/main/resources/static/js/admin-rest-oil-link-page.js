import { initializeAdminRestOilLink } from './admin-rest-oil-link.js';
import { showToast } from './admin-common.js';

if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', () => {
        initializeAdminRestOilLink(document, {
            onNotice: (message, type) => showToast(document, message, type)
        });
    });
}
