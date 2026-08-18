import { initializeAdminFlightHoliday } from './admin-flight-holiday.js';
import { showToast } from './admin-common.js';

if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', () => {
        initializeAdminFlightHoliday(document, {
            onNotice: (message, type) => showToast(document, message, type)
        });
    });
}
