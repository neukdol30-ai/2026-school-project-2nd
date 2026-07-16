-- ════════════════════════════════════════════════════════════
-- secondpro 수동 마이그레이션 V003
-- 변경 내용: MEMBER 닉네임 UNIQUE·회원 정지 기능 추가
-- ════════════════════════════════════════════════════════════
-- 1. 기존 데이터를 유지한 채 MEMBER만 이 시점 구조로 바꿔야 하는 특수한 경우에만 실행
-- 2. 01_schema_latest.sql로 DB를 새로 만든 경우 실행 X
-- ════════════════════════════════════════════════════════════


-- ════════════════════════════════════════════════════════════
-- [1] 닉네임 중복 확인
-- 아래 조회를 먼저 실행 -> 결과가 0행이어야 다음 단계로 넘어갑니다.
-- 만약 결과가 나오면(중복 닉네임이 있으면) UNIQUE 추가에서 오류,
-- 중복을 먼저 정리한 뒤 아래 ALTER를 실행
-- ════════════════════════════════════════════════════════════
SELECT nickname,
       COUNT(*) AS duplicate_count
FROM member
GROUP BY nickname
HAVING COUNT(*) > 1;

-- 닉네임 중복을 막는 UNIQUE 제약조건을 추가합니다.
ALTER TABLE member
ADD CONSTRAINT uk_member_nickname
UNIQUE (nickname);


-- ════════════════════════════════════════════════════════════
-- [2] 회원 정지 관련 컬럼 추가
-- 4개 컬럼을 한 문장으로 묶어서 추가
-- ════════════════════════════════════════════════════════════
ALTER TABLE member
ADD (
    ban_yn      CHAR(1) DEFAULT 'N' NOT NULL,
    ban_reason  VARCHAR2(500 CHAR),
    banned_date TIMESTAMP,
    banned_by   NUMBER
);

-- ban_yn 컬럼에는 Y 또는 N만 저장
ALTER TABLE member
ADD CONSTRAINT ck_member_ban
CHECK (ban_yn IN ('Y', 'N'));

-- 정지 처리 관리자 번호는 member.no를 참조
ALTER TABLE member
ADD CONSTRAINT fk_member_banned_by
FOREIGN KEY (banned_by)
REFERENCES member(no)
ON DELETE SET NULL;


-- ════════════════════════════════════════════════════════════
-- [3] 회원 정지 컬럼 설명
-- ════════════════════════════════════════════════════════════
COMMENT ON COLUMN member.ban_yn IS '관리자 정지 여부(Y/N)';
COMMENT ON COLUMN member.ban_reason IS '관리자 정지 사유';
COMMENT ON COLUMN member.banned_date IS '관리자 정지 처리 시각';
COMMENT ON COLUMN member.banned_by IS '정지 처리 관리자 회원번호';
