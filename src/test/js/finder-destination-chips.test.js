import assert from 'node:assert/strict';
import test from 'node:test';

import { DESTINATION_CHIPS } from '../../main/resources/static/js/finder-destination-chips.js';

test('DESTINATION_CHIPS covers the four confirmed stations with label matching the destination name', () => {
    assert.deepEqual(
        DESTINATION_CHIPS.map((chip) => chip.label),
        ['부산역', '대전역', '강릉역', '광주송정역']
    );
    // 화면에 보이는 라벨과 서버로 보내는 이름이 갈리지 않는지만 본다.
    // 이 이름이 서버 PopularDestination과 일치하는지는 여기서 확인할 수 없어
    // PopularDestinationTest가 이 파일을 직접 읽어 비교한다.
    DESTINATION_CHIPS.forEach((chip) => {
        assert.equal(chip.label, chip.destinationName);
    });
});
