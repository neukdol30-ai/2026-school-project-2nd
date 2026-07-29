/* =========================================================
   V010 방문 기록 최근 방문 갱신 + 장기 통계 + 원본 보관기간

   적용 내용
   1. VISIT_LOG에 최근 방문 시각과 당일 요청 횟수 컬럼 추가
   2. 기존 데이터의 최근 방문 시각을 최초 방문 시각으로 초기화
   3. 개인정보 없는 일별 집계 테이블 생성
   4. 기존 완료 날짜 방문 기록을 일별 통계로 선집계

   주의
   - 이 SQL은 기존 DB에 적용합니다.
   - 신규 DB는 최신 01_schema_latest.sql을 실행하면 별도 적용이 필요 없습니다.
   - 원본 로그 삭제는 애플리케이션 Scheduler가 매일 새벽 수행합니다.
   ========================================================= */


/* =========================================================
   1단계: VISIT_LOG.LAST_VISIT_DATE 컬럼 추가
   ========================================================= */
DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'VISIT_LOG'
      AND column_name = 'LAST_VISIT_DATE';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE visit_log
            ADD last_visit_date TIMESTAMP
        ';
    END IF;
END;
/


/* =========================================================
   2단계: VISIT_LOG.DAILY_REQUEST_COUNT 컬럼 추가
   ========================================================= */
DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_column_count
    FROM user_tab_columns
    WHERE table_name = 'VISIT_LOG'
      AND column_name = 'DAILY_REQUEST_COUNT';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE visit_log
            ADD daily_request_count NUMBER DEFAULT 1
        ';
    END IF;
END;
/


/* =========================================================
   3단계: 기존 방문 기록 값 보정
   ========================================================= */
UPDATE visit_log
SET last_visit_date = created_date
WHERE last_visit_date IS NULL;

UPDATE visit_log
SET daily_request_count = 1
WHERE daily_request_count IS NULL
   OR daily_request_count < 1;

COMMIT;


/* =========================================================
   4단계: 신규 컬럼 기본값 및 NOT NULL 적용
   ========================================================= */
ALTER TABLE visit_log
MODIFY (
    last_visit_date DEFAULT SYSTIMESTAMP NOT NULL,
    daily_request_count DEFAULT 1 NOT NULL
);


/* =========================================================
   5단계: 요청 횟수 CHECK 제약조건 생성
   ========================================================= */
DECLARE
    v_constraint_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_constraint_count
    FROM user_constraints
    WHERE constraint_name = 'CK_VISIT_LOG_REQUEST_COUNT';

    IF v_constraint_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE visit_log
            ADD CONSTRAINT ck_visit_log_request_count
            CHECK (daily_request_count >= 1)
        ';
    END IF;
END;
/


/* =========================================================
   6단계: 최근 방문 정렬용 인덱스 생성
   ========================================================= */
DECLARE
    v_index_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_index_count
    FROM user_indexes
    WHERE index_name = 'IDX_VISIT_LOG_LAST_VISIT_DATE';

    IF v_index_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE INDEX idx_visit_log_last_visit_date
            ON visit_log(last_visit_date)
        ';
    END IF;
END;
/


/* =========================================================
   7단계: 방문 일별 통계 테이블 생성

   IP, User-Agent, 경로 같은 원본 개인정보는 저장하지 않고
   날짜별 합계만 장기 보관합니다.
   ========================================================= */
DECLARE
    v_table_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_table_count
    FROM user_tables
    WHERE table_name = 'VISIT_DAILY_STAT';

    IF v_table_count = 0 THEN
        EXECUTE IMMEDIATE q'[
            CREATE TABLE visit_daily_stat (
                stat_date             DATE,
                unique_visitor_count  NUMBER DEFAULT 0 NOT NULL,
                member_visitor_count  NUMBER DEFAULT 0 NOT NULL,
                guest_visitor_count   NUMBER DEFAULT 0 NOT NULL,
                request_count         NUMBER DEFAULT 0 NOT NULL,
                created_date          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
                updated_date          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,

                CONSTRAINT pk_visit_daily_stat PRIMARY KEY (stat_date),
                CONSTRAINT ck_visit_daily_stat_counts CHECK (
                    unique_visitor_count >= 0
                    AND member_visitor_count >= 0
                    AND guest_visitor_count >= 0
                    AND request_count >= 0
                )
            )
        ]';
    END IF;
END;
/


/* =========================================================
   8단계: 기존 완료 날짜 방문 기록을 일별 통계에 반영

   오늘 데이터는 계속 변경되므로 제외합니다.
   관리자 방문 기록도 통계에서 제외합니다.
   ========================================================= */
MERGE INTO visit_daily_stat target
USING (
    SELECT
        TRUNC(CAST(v.created_date AS DATE)) AS stat_date,
        COUNT(*) AS unique_visitor_count,
        SUM(CASE WHEN v.member_no IS NOT NULL THEN 1 ELSE 0 END) AS member_visitor_count,
        SUM(CASE WHEN v.member_no IS NULL THEN 1 ELSE 0 END) AS guest_visitor_count,
        SUM(NVL(v.daily_request_count, 1)) AS request_count
    FROM visit_log v
    WHERE v.created_date < TRUNC(SYSDATE)
      AND NOT EXISTS (
          SELECT 1
          FROM member admin_member
          WHERE admin_member.no = v.member_no
            AND admin_member.role = 'ADMIN'
      )
    GROUP BY TRUNC(CAST(v.created_date AS DATE))
) source
ON (target.stat_date = source.stat_date)
WHEN MATCHED THEN
    UPDATE SET
        target.unique_visitor_count = source.unique_visitor_count,
        target.member_visitor_count = source.member_visitor_count,
        target.guest_visitor_count = source.guest_visitor_count,
        target.request_count = source.request_count,
        target.updated_date = SYSTIMESTAMP
WHEN NOT MATCHED THEN
    INSERT (
        stat_date,
        unique_visitor_count,
        member_visitor_count,
        guest_visitor_count,
        request_count,
        created_date,
        updated_date
    )
    VALUES (
        source.stat_date,
        source.unique_visitor_count,
        source.member_visitor_count,
        source.guest_visitor_count,
        source.request_count,
        SYSTIMESTAMP,
        SYSTIMESTAMP
    );

COMMIT;


/* =========================================================
   9단계: 적용 결과 확인
   ========================================================= */
SELECT
    no,
    ip_address,
    created_date,
    last_visit_date,
    daily_request_count
FROM visit_log
ORDER BY last_visit_date DESC;

SELECT
    stat_date,
    unique_visitor_count,
    member_visitor_count,
    guest_visitor_count,
    request_count
FROM visit_daily_stat
ORDER BY stat_date DESC;
