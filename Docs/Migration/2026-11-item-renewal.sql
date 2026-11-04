-- 장비 시스템 리뉴얼: 3등급 → 5등급 전환, 2004 삭제, 옛 item 외래키 제거
-- 실행 시점: 서버를 끈 상태에서, Feat/item-renewal이 반영된 새 버전을 띄우기 직전에 한 번
-- 매핑: GENERAL → COMMON, SUPER → UNCOMMON, RARE → RARE (보정·보상 없음)

-- [점검] 실행 전 상태
SHOW CREATE TABLE equipment;
SELECT grade, COUNT(*) FROM equipment GROUP BY grade;
SELECT COUNT(*) FROM equipment WHERE item_id = 2004;

-- [1] 옛 item 테이블을 가리키는 외래키 제거
--     Item이 DB 테이블이던 시절의 흔적. 시트에 새 아이템을 추가하면 지급이 거부되므로 지운다
--     이름이 DB마다 다르므로 먼저 조회한다. 결과가 없으면 이 단계는 건너뛴다
SELECT CONSTRAINT_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'equipment' AND REFERENCED_TABLE_NAME = 'item';
-- 위 결과의 이름으로 실행 (예: FK58l8d1l6ijhudl7gkbp0vb4ot)
-- ALTER TABLE equipment DROP FOREIGN KEY FK58l8d1l6ijhudl7gkbp0vb4ot;

-- [2] 등급 컬럼을 MySQL ENUM에서 varchar로 (기존 값은 그대로 남는다)
ALTER TABLE equipment MODIFY grade varchar(20) NOT NULL;

-- [3] 2004(구형 군수 벨트) 삭제. 장착 중이던 것도 함께 지워진다
DELETE FROM equipment WHERE item_id = 2004;

-- [4] 등급 이름 변경
UPDATE equipment SET grade = 'COMMON'   WHERE grade = 'GENERAL';
UPDATE equipment SET grade = 'UNCOMMON' WHERE grade = 'SUPER';

-- [점검] 실행 후: grade는 varchar(20), 값은 COMMON / UNCOMMON / RARE만, 2004는 0개
SHOW CREATE TABLE equipment;
SELECT grade, COUNT(*) FROM equipment GROUP BY grade;
SELECT COUNT(*) FROM equipment WHERE item_id = 2004;