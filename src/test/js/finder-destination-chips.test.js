import assert from 'node:assert/strict';
import test from 'node:test';

import { DESTINATION_CHIPS } from '../../main/resources/static/js/finder-destination-chips.js';

test('DESTINATION_CHIPS covers the four confirmed stations with label matching the query', () => {
    assert.deepEqual(
        DESTINATION_CHIPS.map((chip) => chip.label),
        ['부산역', '대전역', '강릉역', '광주송정역']
    );
    DESTINATION_CHIPS.forEach((chip) => {
        assert.equal(chip.label, chip.destinationQuery);
    });
});
