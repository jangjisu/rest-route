import assert from 'node:assert/strict';
import test from 'node:test';

import { DESTINATION_CHIPS, resolveDestinationQuery } from '../../main/resources/static/js/finder-destination-chips.js';

test('resolveDestinationQuery maps a known chip label to its station name', () => {
    assert.equal(resolveDestinationQuery('부산'), '부산역');
    assert.equal(resolveDestinationQuery('광주'), '광주송정역');
});

test('resolveDestinationQuery passes through free-text input unchanged', () => {
    assert.equal(resolveDestinationQuery('전주 한옥마을'), '전주 한옥마을');
});

test('resolveDestinationQuery trims whitespace before matching', () => {
    assert.equal(resolveDestinationQuery('  부산  '), '부산역');
});

test('DESTINATION_CHIPS covers the four confirmed cities', () => {
    assert.deepEqual(
        DESTINATION_CHIPS.map((chip) => chip.label),
        ['부산', '대전', '강릉', '광주']
    );
});
