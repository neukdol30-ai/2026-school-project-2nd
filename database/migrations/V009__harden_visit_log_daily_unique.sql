/* =========================================================
   V009 방문 기록 중복 방지

   실행 순서
   1. 기존 중복 방문 기록 삭제
   2. 삭제 결과 COMMIT
   3. 일별 중복 저장 방지용 UNIQUE 인덱스 생성
   ========================================================= */


/* =========================================================
   1단계: 기존 중복 방문 기록 삭제

   같은 IP 주소가 같은 날짜에 여러 번 저장된 경우
   가장 먼저 저장된 기록 1개만 남기고 나머지를 삭제합니다.

   ROW_NUMBER() 결과
   - duplicate_order = 1 : 유지
   - duplicate_order > 1 : 삭제
   ========================================================= */
DELETE FROM visit_log
WHERE no IN (
    SELECT ranked_visit.no
    FROM (
        SELECT
            no,
            ROW_NUMBER() OVER (
                PARTITION BY
                    ip_address,
                    TRUNC(CAST(created_date AS DATE))
                ORDER BY
                    created_date,
                    no
            ) AS duplicate_order
        FROM visit_log
        WHERE ip_address IS NOT NULL
    ) ranked_visit
    WHERE ranked_visit.duplicate_order > 1
);


/* =========================================================
   2단계: 중복 방문 기록 삭제 결과 확정
   ========================================================= */
COMMIT;


/* =========================================================
   3단계: 같은 IP의 하루 중복 저장 방지

   UK_VISIT_LOG_IP_DAY 인덱스가 존재하지 않을 때만 생성합니다.

   UNIQUE 기준
   - IP 주소
   - 방문 날짜

   따라서 같은 IP는 하루에 한 번만 저장될 수 있습니다.
   ========================================================= */
DECLARE
    v_index_count NUMBER;
BEGIN
    /* 동일한 이름의 인덱스가 이미 존재하는지 확인합니다. */
    SELECT COUNT(*)
    INTO v_index_count
    FROM user_indexes
    WHERE index_name = 'UK_VISIT_LOG_IP_DAY';

    /* 인덱스가 없는 경우에만 새로 생성합니다. */
    IF v_index_count = 0 THEN
        EXECUTE IMMEDIATE
            'CREATE UNIQUE INDEX uk_visit_log_ip_day ' ||
            'ON visit_log (' ||
            '    ip_address, ' ||
            '    TRUNC(CAST(created_date AS DATE))' ||
            ')';
    END IF;
END;
/