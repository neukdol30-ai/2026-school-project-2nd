-- ════════════════════════════════════════════════════════════
-- secondpro 프로젝트 개발용 초기 데이터 SQL
-- ════════════════════════════════════════════════════════════
-- 1. 01_schema_latest.sql 실행 후 사용
-- 2. 개발·화면 확인용 샘플 데이터
-- 3. 같은 INSERT 문을 다시 실행하면 PK 중복 또는 데이터 중복이 발생
-- 4. 회원 계정은 평문 비밀번호 INSERT가 아니라 회원가입 화면에서 생성
-- 5. 관리자 승격 SQL은 맨 아래에서 아이디를 바꾼 뒤 필요한 부분만 실행
-- ════════════════════════════════════════════════════════════


-- ════════════════════════════════════════════════════════════
-- [1] 뉴스 샘플 데이터
-- news_id는 news_seq 시퀀스가 자동으로 번호 매김
-- ════════════════════════════════════════════════════════════
INSERT INTO news (
    news_id,
    title,
    summary,
    source,
    published_at,
    link
) VALUES (
    news_seq.NEXTVAL,
    'AI 산업 관련 주요 뉴스',
    'AI 기술이 여러 산업 분야에 빠르게 적용되고 있다는 내용의 뉴스입니다.',
    '테크뉴스',
    '2026-07-07',
    'https://example.com/news/1'
);

INSERT INTO news (
    news_id,
    title,
    summary,
    source,
    published_at,
    link
) VALUES (
    news_seq.NEXTVAL,
    '오늘의 경제 뉴스 요약',
    '환율과 금리 흐름이 시장에 영향을 주고 있다는 경제 뉴스 요약입니다.',
    '경제신문',
    '2026-07-07',
    'https://example.com/news/2'
);

INSERT INTO news (
    news_id,
    title,
    summary,
    source,
    published_at,
    link
) VALUES (
    news_seq.NEXTVAL,
    '개발자 채용 시장 동향',
    '기업들의 개발자 채용 방식과 요구 역량이 변화하고 있다는 기사입니다.',
    'IT데일리',
    '2026-07-07',
    'https://example.com/news/3'
);


-- ════════════════════════════════════════════════════════════
-- [2] 주식 샘플 데이터
-- ════════════════════════════════════════════════════════════
INSERT INTO stock_info (
    stock_id,
    symbol,
    stock_name,
    price,
    change_price,
    change_rate
) VALUES (
    1,
    '005930',
    '삼성전자',
    78000,
    500,
    0.65
);

INSERT INTO stock_info (
    stock_id,
    symbol,
    stock_name,
    price,
    change_price,
    change_rate
) VALUES (
    2,
    '035420',
    'NAVER',
    210000,
    -1500,
    -0.71
);

INSERT INTO stock_info (
    stock_id,
    symbol,
    stock_name,
    price,
    change_price,
    change_rate
) VALUES (
    3,
    '035720',
    '카카오',
    56000,
    800,
    1.45
);

INSERT INTO stock_info (
    stock_id,
    symbol,
    stock_name,
    price,
    change_price,
    change_rate
) VALUES (
    4,
    '000660',
    'SK하이닉스',
    235000,
    3000,
    1.29
);

COMMIT;


-- ════════════════════════════════════════════════════════════
-- [3] 관리자 권한 승격용 선택 SQL
-- 1. 회원가입 화면에서 계정을 먼저 생성
-- 2. 아래 '승격할_ID'를 실제 회원 아이디로 변경
-- 3. 주석 해제 후 실행할 것
-- ════════════════════════════════════════════════════════════

-- UPDATE member
-- SET role = 'ADMIN'
-- WHERE member_id = '승격할_ID';

-- COMMIT;
