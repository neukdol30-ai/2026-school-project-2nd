-- ════════════════════════════════════════════════════════════
-- secondpro 수동 마이그레이션 V001
-- 변경 내용: CHAT_ROOM 카테고리 컬럼·제약조건·인덱스 추가
-- ════════════════════════════════════════════════════════════
-- 1. 이 파일은 "이력 보관용" -> 평소에는 실행 X
-- 2. 기존 데이터를 유지한 채 CHAT_ROOM만 이 시점 구조로 바꿔야 하는 특수한 경우에만 실행
-- 3. 한 번만 실행. 두 번 실행하면 "이미 존재하는 객체" 오류
-- 4. 01_schema_latest.sql로 DB를 새로 만든 경우 실행 X
-- ════════════════════════════════════════════════════════════


-- chat_room 테이블에 문의 카테고리 컬럼을 추가
ALTER TABLE chat_room
ADD category VARCHAR2(30 CHAR);

-- 기존 채팅방의 category가 NULL이면 ETC로
UPDATE chat_room
SET category = 'ETC'
WHERE category IS NULL;

COMMIT;

-- 앞으로 입력되는 값의 기본값을 ETC로 지정하고 NULL을 막음
ALTER TABLE chat_room
MODIFY category DEFAULT 'ETC' NOT NULL;

-- category 컬럼에는 정해진 값만 들어가게 제한
ALTER TABLE chat_room
ADD CONSTRAINT ck_chat_room_category
CHECK (
    category IN (
        'MAIL',
        'MAP',
        'STOCK',
        'NEWS',
        'WEATHER',
        'CALENDAR',
        'ETC'
    )
);

-- 채팅방 상태 조회용 인덱스
CREATE INDEX idx_chat_room_status
    ON chat_room(status);

-- 카테고리별 문의 목록 조회용 인덱스
CREATE INDEX idx_chat_room_category
    ON chat_room(category);

-- 최근 메시지 순서 조회용 인덱스
CREATE INDEX idx_chat_room_last_message_date
    ON chat_room(last_message_date);
