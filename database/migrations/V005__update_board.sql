-- 1. 이 파일은 이력 보관용. 평소에는 실행하지 않음 (01_schema_latest.sql에 이미 포함)
-- 2. 기존 데이터를 유지한 채 board 계열만 최신화해야 하는 경우에만 실행
-- 3. 한 번만 실행. 두 번 실행하면 이미 존재하는 객체 오류 발생
-- 4. [중요] board_comment.content(VARCHAR2 → CLOB)는 이미 데이터가 있으면
--    ALTER MODIFY로 바로 못 바꿈(ORA-22858). 이 경우 아래 [3] 안내 참고.
--    개발 단계에서는 00_reset_local → 01_schema_latest 리셋이 더 간단함.


-- ════════════════════════════════════════════════════════════
-- [1] BOARD 테이블 최신화
-- category 기본값·허용값 변경, answer_status(답변 상태) 컬럼 추가
-- ════════════════════════════════════════════════════════════

-- 답변 상태 컬럼을 추가합니다. (기존 행은 자동으로 기본값 'WAITING'이 채워짐)
ALTER TABLE board
ADD answer_status VARCHAR2(20 CHAR) DEFAULT 'WAITING' NOT NULL;

-- 기존 category 값 중 새 허용값('NOTICE','QUESTION')에 없는 값을 QUESTION으로 정리합니다.
-- (예전 'FAQ','FREE' 데이터가 남아 있으면 아래 CHECK 추가에서 막히므로 먼저 정리)
UPDATE board
SET category = 'QUESTION'
WHERE category NOT IN ('NOTICE', 'QUESTION');

COMMIT;

-- category 기본값을 QUESTION으로 바꿉니다.
ALTER TABLE board
MODIFY category DEFAULT 'QUESTION';

-- 기존 category CHECK 제약조건을 지우고 새 허용값으로 다시 겁니다.
ALTER TABLE board
DROP CONSTRAINT ck_board_category;

ALTER TABLE board
ADD CONSTRAINT ck_board_category
CHECK (category IN ('NOTICE', 'QUESTION'));

-- answer_status CHECK 제약조건을 추가합니다.
ALTER TABLE board
ADD CONSTRAINT ck_board_answer_status
CHECK (answer_status IN ('WAITING', 'ANSWERED'));


-- ════════════════════════════════════════════════════════════
-- [2] BOARD_COMMENT 테이블 최신화
-- accepted_yn(채택 여부) 컬럼 추가
-- ════════════════════════════════════════════════════════════

-- 채택 여부 컬럼을 추가합니다. (기존 행은 자동으로 기본값 'N'이 채워짐)
ALTER TABLE board_comment
ADD accepted_yn CHAR(1) DEFAULT 'N' NOT NULL;

-- accepted_yn CHECK 제약조건을 추가합니다.
ALTER TABLE board_comment
ADD CONSTRAINT ck_board_comment_accepted
CHECK (accepted_yn IN ('Y', 'N'));


-- ════════════════════════════════════════════════════════════
-- [3] BOARD_COMMENT.content 타입 변경 (VARCHAR2 → CLOB)
-- Oracle은 데이터가 있는 VARCHAR2 컬럼을 CLOB으로 바로 못 바꿈(ORA-22858).
-- 아래는 임시 컬럼을 거치는 우회 방법. 데이터가 없으면 이 블록은 건너뛰고
-- content를 처음부터 CLOB으로 만든 01_schema_latest.sql을 쓰는 게 간단함.
-- ════════════════════════════════════════════════════════════

-- 1) CLOB 임시 컬럼 추가
ALTER TABLE board_comment
ADD content_clob CLOB;

-- 2) 기존 VARCHAR2 데이터를 CLOB 임시 컬럼으로 복사
UPDATE board_comment
SET content_clob = content;

COMMIT;

-- 3) 기존 VARCHAR2 컬럼 삭제
ALTER TABLE board_comment
DROP COLUMN content;

-- 4) 임시 컬럼 이름을 content로 변경
ALTER TABLE board_comment
RENAME COLUMN content_clob TO content;

-- 5) NOT NULL 제약을 다시 적용
ALTER TABLE board_comment
MODIFY content NOT NULL;


-- ════════════════════════════════════════════════════════════
-- [4] 인덱스 최신화
-- category 단일 인덱스 → (category, answer_status) 복합 인덱스로 교체
-- ════════════════════════════════════════════════════════════

-- 기존 category 단일 인덱스를 삭제합니다.
DROP INDEX idx_board_category;

-- category + answer_status 복합 인덱스를 새로 만듭니다.
CREATE INDEX idx_board_category_status
    ON board(category, answer_status);
