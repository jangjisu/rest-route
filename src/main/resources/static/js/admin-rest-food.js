import { fetchAdminRestStops, validateRestStopImageFile } from './admin-rest-stop-image.js';
import {
    clearAdminRestFoodOverride,
    createAdminRestFood,
    deleteAdminRestFood,
    deleteAdminRestFoodImage,
    fetchAdminRestFoodImage,
    fetchAdminRestFoods,
    saveAdminRestFoodImage,
    updateAdminRestFood
} from './admin-rest-food-request.js';
import { csrfFrom } from './admin-common.js';

const EMPTY_FOOD_LIST_MESSAGE = '등록된 메뉴가 없습니다.';

function createOption(document, restStop) {
    const option = document.createElement('option');
    option.value = restStop.serviceAreaCode;
    option.textContent = `${restStop.unitName || '이름 정보 없음'} · ${restStop.serviceAreaCode}`;
    return option;
}

export function initializeAdminRestFood(document, {
    fetchImpl = fetch,
    confirmImpl = globalThis.confirm,
    onNotice = () => {},
    urlApi = globalThis.URL
} = {}) {
    const select = document.getElementById('restStopFoodSelect');
    const status = document.getElementById('restStopFoodStatus');
    const listSection = document.getElementById('restStopFoodListSection');
    const list = document.getElementById('restStopFoodList');
    const editModal = document.getElementById('restStopFoodEditModal');
    const editModalClose = document.getElementById('restStopFoodEditModalClose');
    const editForm = document.getElementById('restStopFoodEditForm');
    const editStateLabel = document.getElementById('restStopFoodEditStateLabel');
    const editName = document.getElementById('restStopFoodEditName');
    const editCost = document.getElementById('restStopFoodEditCost');
    const editDescription = document.getElementById('restStopFoodEditDescription');
    const editSubmit = document.getElementById('restStopFoodEditSubmit');
    const editClearOverride = document.getElementById('restStopFoodEditClearOverride');
    const addButton = document.getElementById('restStopFoodAddButton');
    const addModal = document.getElementById('restStopFoodAddModal');
    const addModalClose = document.getElementById('restStopFoodAddModalClose');
    const addForm = document.getElementById('restStopFoodAddForm');
    const addName = document.getElementById('restStopFoodAddName');
    const addCost = document.getElementById('restStopFoodAddCost');
    const addDescription = document.getElementById('restStopFoodAddDescription');
    if (!select || !status || !listSection || !list || !editModal || !editModalClose || !editForm
        || !editStateLabel || !editName || !editCost || !editDescription || !editSubmit || !editClearOverride
        || !addButton || !addModal || !addModalClose || !addForm || !addName || !addCost || !addDescription) {
        return;
    }

    let editingFoodId = null;
    let editingWasOverridden = false;
    let activeImagePreviewUrls = [];

    function pageCsrf() {
        return csrfFrom(addForm);
    }

    function closeEditModal() {
        editModal.close();
        editingFoodId = null;
    }

    function openEditModal(food) {
        editingFoodId = food.id;
        editingWasOverridden = food.adminOverridden === true;
        editName.value = food.foodName ?? '';
        editCost.value = food.foodCost ?? '';
        editDescription.value = food.description ?? '';
        editStateLabel.textContent = editingWasOverridden
            ? '관리자가 수정하여 자동 동기화에서 제외된 메뉴입니다.'
            : '동기화 중인 메뉴입니다. 저장하면 자동 동기화 대상에서 제외됩니다.';
        editSubmit.textContent = '저장';
        editClearOverride.hidden = !editingWasOverridden;
        editModal.showModal();
    }

    function revokeImagePreviews() {
        activeImagePreviewUrls.forEach((url) => urlApi.revokeObjectURL(url));
        activeImagePreviewUrls = [];
    }

    function appendImagePreview(imageRow, food) {
        const preview = document.createElement('img');
        preview.className = 'food-item-image-preview';
        preview.alt = `${food.foodName} 이미지 미리보기`;
        preview.hidden = true;
        imageRow.appendChild(preview);
        fetchAdminRestFoodImage(select.value, food.id, fetchImpl).then((result) => {
            if (result.status !== 'success') {
                return;
            }
            const url = urlApi.createObjectURL(result.blob);
            activeImagePreviewUrls.push(url);
            preview.src = url;
            preview.hidden = false;
        });
    }

    function createFoodItem(food) {
        const item = document.createElement('li');
        item.className = 'food-item';

        const header = document.createElement('div');
        header.className = 'food-item-header';
        const name = document.createElement('span');
        name.className = 'food-item-name';
        name.textContent = food.foodName;
        const badge = document.createElement('span');
        badge.className = 'food-item-badge';
        badge.dataset.state = food.adminOverridden ? 'overridden' : 'synced';
        badge.textContent = food.adminOverridden ? '관리자 수정됨' : '동기화중';
        header.appendChild(name);
        header.appendChild(badge);

        const meta = document.createElement('div');
        meta.className = 'food-item-meta';
        meta.textContent = [food.foodCost, food.description].filter(Boolean).join(' · ');

        const actions = document.createElement('div');
        actions.className = 'food-item-actions';
        const editButton = document.createElement('button');
        editButton.className = 'food-item-edit';
        editButton.type = 'button';
        editButton.textContent = '수정';
        editButton.addEventListener('click', () => openEditModal(food));
        actions.appendChild(editButton);

        if (food.adminCreated) {
            const deleteButton = document.createElement('button');
            deleteButton.className = 'food-item-delete';
            deleteButton.type = 'button';
            deleteButton.textContent = '삭제';
            deleteButton.addEventListener('click', async () => {
                if (!confirmImpl('이 메뉴를 삭제할까요?')) {
                    return;
                }
                const result = await deleteAdminRestFood(select.value, food.id, pageCsrf(), fetchImpl);
                if (result.status !== 'success') {
                    onNotice('메뉴 삭제에 실패했습니다.', 'error');
                    return;
                }
                if (editingFoodId === food.id) {
                    closeEditModal();
                }
                onNotice('메뉴를 삭제했습니다.');
                await loadFoods();
            });
            actions.appendChild(deleteButton);
        }

        const imageRow = document.createElement('div');
        imageRow.className = 'food-item-image-row';
        const imageInput = document.createElement('input');
        imageInput.className = 'food-item-image-input';
        imageInput.type = 'file';
        imageInput.addEventListener('change', async () => {
            const file = imageInput.files?.[0];
            if (!file) {
                return;
            }
            const validationMessage = validateRestStopImageFile(file);
            if (validationMessage) {
                imageInput.value = '';
                onNotice(validationMessage, 'error');
                return;
            }
            const result = await saveAdminRestFoodImage(select.value, food.id, file, pageCsrf(), fetchImpl);
            imageInput.value = '';
            if (result.status !== 'success') {
                onNotice('메뉴 이미지 저장에 실패했습니다.', 'error');
                return;
            }
            onNotice('메뉴 이미지를 저장했습니다.');
            await loadFoods();
        });
        imageRow.appendChild(imageInput);

        if (food.hasImage) {
            appendImagePreview(imageRow, food);

            const imageDeleteButton = document.createElement('button');
            imageDeleteButton.className = 'food-item-image-delete';
            imageDeleteButton.type = 'button';
            imageDeleteButton.textContent = '이미지 삭제';
            imageDeleteButton.addEventListener('click', async () => {
                if (!confirmImpl('등록된 메뉴 이미지를 삭제할까요?')) {
                    return;
                }
                const result = await deleteAdminRestFoodImage(select.value, food.id, pageCsrf(), fetchImpl);
                if (result.status !== 'success') {
                    onNotice('메뉴 이미지 삭제에 실패했습니다.', 'error');
                    return;
                }
                onNotice('메뉴 이미지를 삭제했습니다.');
                await loadFoods();
            });
            imageRow.appendChild(imageDeleteButton);
        }

        item.appendChild(header);
        item.appendChild(meta);
        item.appendChild(actions);
        item.appendChild(imageRow);
        return item;
    }

    function renderFoodList(foods) {
        revokeImagePreviews();
        if (foods.length === 0) {
            const empty = document.createElement('li');
            empty.className = 'food-list-empty';
            empty.textContent = EMPTY_FOOD_LIST_MESSAGE;
            list.replaceChildren(empty);
            return;
        }
        list.replaceChildren(...foods.map((food) => createFoodItem(food)));
    }

    async function loadFoods() {
        const serviceAreaCode = select.value;
        listSection.hidden = true;
        status.textContent = '';
        addButton.disabled = serviceAreaCode === '';

        if (serviceAreaCode === '') {
            return;
        }

        status.textContent = '메뉴 목록을 불러오고 있습니다.';
        const result = await fetchAdminRestFoods(serviceAreaCode, fetchImpl);

        if (result.status !== 'success') {
            status.textContent = '메뉴 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.';
            return;
        }

        renderFoodList(result.foods);
        status.textContent = '';
        listSection.hidden = false;
    }

    select.addEventListener('change', loadFoods);

    editModalClose.addEventListener('click', closeEditModal);

    editForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (editingFoodId === null) {
            return;
        }

        const payload = {
            foodName: editName.value,
            foodCost: editCost.value,
            description: editDescription.value
        };
        const wasOverridden = editingWasOverridden;
        const result = await updateAdminRestFood(select.value, editingFoodId, payload, pageCsrf(), fetchImpl);

        if (result.status === 'invalid') {
            onNotice(result.message, 'error');
            return;
        }
        if (result.status !== 'success') {
            onNotice('메뉴 수정에 실패했습니다.', 'error');
            return;
        }

        closeEditModal();
        onNotice(wasOverridden ? '메뉴를 수정했습니다.' : '메뉴를 잠그고 저장했습니다.');
        await loadFoods();
    });

    editClearOverride.addEventListener('click', async () => {
        if (editingFoodId === null) {
            return;
        }

        const result = await clearAdminRestFoodOverride(select.value, editingFoodId, pageCsrf(), fetchImpl);
        if (result.status !== 'success') {
            onNotice('동기화 잠금 해제에 실패했습니다.', 'error');
            return;
        }

        closeEditModal();
        onNotice('동기화 잠금을 해제했습니다.');
        await loadFoods();
    });

    addButton.addEventListener('click', () => addModal.showModal());
    addModalClose.addEventListener('click', () => addModal.close());

    addForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const serviceAreaCode = select.value;
        if (serviceAreaCode === '') {
            return;
        }

        const payload = {
            foodName: addName.value,
            foodCost: addCost.value,
            description: addDescription.value
        };
        const result = await createAdminRestFood(serviceAreaCode, payload, pageCsrf(), fetchImpl);

        if (result.status === 'invalid') {
            onNotice(result.message, 'error');
            return;
        }
        if (result.status !== 'success') {
            onNotice('메뉴 추가에 실패했습니다.', 'error');
            return;
        }

        addModal.close();
        addName.value = '';
        addCost.value = '';
        addDescription.value = '';
        onNotice('메뉴를 추가했습니다.');
        await loadFoods();
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
