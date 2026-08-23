import { fetchAdminRestStops } from './admin-rest-stop-image.js';
import {
    clearRestStopOverride,
    createRestStop,
    fetchEditableRestStop,
    saveEditableRestStop
} from './admin-rest-stop-edit-request.js';
import { csrfFrom } from './admin-common.js';

function createOption(document, restStop) {
    const option = document.createElement('option');
    option.value = restStop.serviceAreaCode;
    option.textContent = `${restStop.unitName || '이름 정보 없음'} · ${restStop.serviceAreaCode}`;
    return option;
}

function oxToChecked(value) {
    return value === 'O';
}

function checkedToOx(checked) {
    return checked ? 'O' : 'X';
}

export function initializeAdminRestStopEdit(document, { fetchImpl = fetch, onNotice = () => {} } = {}) {
    const select = document.getElementById('restStopEditSelect');
    const status = document.getElementById('restStopEditStatus');
    const form = document.getElementById('restStopEditForm');
    const submitButton = document.getElementById('restStopEditSubmit');
    const lockBanner = document.getElementById('restStopEditLockBanner');
    const lockIcon = document.getElementById('restStopEditLockIcon');
    const lockTitle = document.getElementById('restStopEditLockTitle');
    const lockDesc = document.getElementById('restStopEditLockDesc');
    const lockToggle = document.getElementById('restStopEditLockToggle');
    const serviceAreaCodeView = document.getElementById('restStopEditServiceAreaCode');
    const unitCodeView = document.getElementById('restStopEditUnitCode');
    const unitName = document.getElementById('restStopEditUnitName');
    const routeNo = document.getElementById('restStopEditRouteNo');
    const routeName = document.getElementById('restStopEditRouteName');
    const xValue = document.getElementById('restStopEditXValue');
    const yValue = document.getElementById('restStopEditYValue');
    const telNo = document.getElementById('restStopEditTelNo');
    const brand = document.getElementById('restStopEditBrand');
    const routeCode = document.getElementById('restStopEditRouteCode');
    const svarAddr = document.getElementById('restStopEditSvarAddr');
    const convenience = document.getElementById('restStopEditConvenience');
    const maintenanceYn = document.getElementById('restStopEditMaintenanceYn');
    const truckSaYn = document.getElementById('restStopEditTruckSaYn');
    const createButton = document.getElementById('restStopEditCreateButton');
    if (!select || !status || !form || !submitButton || !lockBanner || !lockIcon || !lockTitle || !lockDesc
        || !lockToggle || !serviceAreaCodeView || !unitCodeView || !unitName || !routeNo || !routeName
        || !xValue || !yValue || !telNo || !brand || !routeCode || !svarAddr || !convenience
        || !maintenanceYn || !truckSaYn || !createButton) {
        return;
    }

    let isCreateMode = false;

    function setBusy(busy) {
        select.disabled = busy || isCreateMode;
        submitButton.disabled = busy;
        lockToggle.disabled = busy;
        createButton.disabled = busy;
    }

    function renderLockState(overridden) {
        lockBanner.hidden = false;
        lockBanner.dataset.state = overridden ? 'locked' : 'synced';
        lockIcon.textContent = overridden ? '🔒' : '🔄';
        lockTitle.textContent = overridden
            ? '이 휴게소는 관리자가 수정하여 자동 동기화에서 제외되어 있습니다'
            : '이 휴게소는 자동 동기화 중입니다';
        lockDesc.textContent = overridden
            ? '매일 자정 도로공사 데이터 동기화가 이 휴게소의 값을 덮어쓰지 않습니다. 다시 자동 동기화를 받으려면 잠금을 해제하세요.'
            : '매일 자정 도로공사 데이터로 값이 갱신됩니다. 저장하면 이 휴게소만 자동 동기화에서 제외됩니다.';
        lockToggle.hidden = !overridden;
    }

    function renderFields(data) {
        serviceAreaCodeView.textContent = data.serviceAreaCode ?? '';
        unitCodeView.textContent = data.unitCode ?? '';
        unitName.value = data.unitName ?? '';
        routeNo.value = data.routeNo ?? '';
        routeName.value = data.routeName ?? '';
        xValue.value = data.xValue ?? '';
        yValue.value = data.yValue ?? '';
        telNo.value = data.telNo ?? '';
        brand.value = data.brand ?? '';
        routeCode.value = data.routeCode ?? '';
        svarAddr.value = data.svarAddr ?? '';
        convenience.value = data.convenience ?? '';
        maintenanceYn.checked = oxToChecked(data.maintenanceYn);
        truckSaYn.checked = oxToChecked(data.truckSaYn);
        renderLockState(data.adminOverridden === true);
    }

    function collectPayload() {
        return {
            unitName: unitName.value,
            routeNo: routeNo.value,
            routeName: routeName.value,
            xValue: xValue.value,
            yValue: yValue.value,
            telNo: telNo.value,
            brand: brand.value,
            routeCode: routeCode.value,
            svarAddr: svarAddr.value,
            convenience: convenience.value,
            maintenanceYn: checkedToOx(maintenanceYn.checked),
            truckSaYn: checkedToOx(truckSaYn.checked)
        };
    }

    function enterCreateMode() {
        isCreateMode = true;
        select.value = '';
        status.textContent = '';
        lockBanner.hidden = true;
        serviceAreaCodeView.textContent = '';
        unitCodeView.textContent = '저장하면 자동 발급됩니다';
        unitName.value = '';
        routeNo.value = '';
        routeName.value = '';
        xValue.value = '';
        yValue.value = '';
        telNo.value = '';
        brand.value = '';
        routeCode.value = '';
        svarAddr.value = '';
        convenience.value = '';
        maintenanceYn.checked = false;
        truckSaYn.checked = false;
        form.hidden = false;
    }

    async function loadSelected() {
        isCreateMode = false;
        const serviceAreaCode = select.value;
        form.hidden = true;
        lockBanner.hidden = true;
        status.textContent = '';

        if (serviceAreaCode === '') {
            return;
        }

        setBusy(true);
        status.textContent = '휴게소 정보를 불러오고 있습니다.';
        const result = await fetchEditableRestStop(serviceAreaCode, fetchImpl);
        setBusy(false);

        if (result.status === 'not-found') {
            status.textContent = '존재하지 않는 휴게소 코드입니다.';
            return;
        }

        if (result.status !== 'success') {
            status.textContent = '휴게소 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.';
            return;
        }

        renderFields(result.data);
        status.textContent = '';
        form.hidden = false;
    }

    select.addEventListener('change', loadSelected);

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        if (isCreateMode) {
            setBusy(true);
            const result = await createRestStop(collectPayload(), csrfFrom(form), fetchImpl);
            setBusy(false);

            if (result.status === 'invalid') {
                onNotice(result.message, 'error');
                return;
            }

            if (result.status !== 'success') {
                onNotice('휴게소 등록에 실패했습니다.', 'error');
                return;
            }

            isCreateMode = false;
            select.disabled = false;
            select.append(createOption(document, result.data));
            select.value = result.data.serviceAreaCode;
            renderFields(result.data);
            onNotice('휴게소를 새로 등록했습니다.');
            return;
        }

        const serviceAreaCode = select.value;
        if (serviceAreaCode === '') {
            return;
        }

        setBusy(true);
        const result = await saveEditableRestStop(serviceAreaCode, collectPayload(), csrfFrom(form), fetchImpl);
        setBusy(false);

        if (result.status === 'invalid') {
            onNotice(result.message, 'error');
            return;
        }

        if (result.status === 'not-found') {
            onNotice('존재하지 않는 휴게소입니다.', 'error');
            return;
        }

        if (result.status !== 'success') {
            onNotice('휴게소 정보 저장에 실패했습니다.', 'error');
            return;
        }

        renderFields(result.data);
        onNotice('휴게소 정보를 저장했습니다.');
    });

    createButton.addEventListener('click', enterCreateMode);

    lockToggle.addEventListener('click', async () => {
        const serviceAreaCode = select.value;
        if (serviceAreaCode === '') {
            return;
        }

        setBusy(true);
        const result = await clearRestStopOverride(serviceAreaCode, csrfFrom(form), fetchImpl);
        setBusy(false);

        if (result.status !== 'success') {
            onNotice('동기화 잠금 해제에 실패했습니다.', 'error');
            return;
        }

        renderFields(result.data);
        onNotice('동기화 잠금을 해제했습니다.');
    });

    fetchAdminRestStops(fetchImpl)
        .then((restStops) => {
            select.append(...restStops.map((restStop) => createOption(document, restStop)));
            select.disabled = false;
        })
        .catch((error) => {
            console.error('휴게소 목록 조회에 실패했습니다.', error);
            status.textContent = '휴게소 목록을 불러오지 못했습니다.';
        });
}
