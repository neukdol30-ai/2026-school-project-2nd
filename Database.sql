CREATE TABLE NEWS (
                      NEWS_ID NUMBER PRIMARY KEY,
                      TITLE VARCHAR2(300) NOT NULL,
                      SUMMARY VARCHAR2(1000),
                      SOURCE VARCHAR2(100),
                      PUBLISHED_AT VARCHAR2(50),
                      LINK VARCHAR2(1000),
                      CREATED_AT DATE DEFAULT SYSDATE
);

CREATE SEQUENCE NEWS_SEQ
    START WITH 1
    INCREMENT BY 1
    NOCACHE;

INSERT INTO NEWS (
    NEWS_ID,
    TITLE,
    SUMMARY,
    SOURCE,
    PUBLISHED_AT,
    LINK
) VALUES (
             NEWS_SEQ.NEXTVAL,
             'AI 산업 관련 주요 뉴스',
             'AI 기술이 여러 산업 분야에 빠르게 적용되고 있다는 내용의 뉴스입니다.',
             '테크뉴스',
             '2026-07-07',
             'https://example.com/news/1'
         );

INSERT INTO NEWS (
    NEWS_ID,
    TITLE,
    SUMMARY,
    SOURCE,
    PUBLISHED_AT,
    LINK
) VALUES (
             NEWS_SEQ.NEXTVAL,
             '오늘의 경제 뉴스 요약',
             '환율과 금리 흐름이 시장에 영향을 주고 있다는 경제 뉴스 요약입니다.',
             '경제신문',
             '2026-07-07',
             'https://example.com/news/2'
         );

INSERT INTO NEWS (
    NEWS_ID,
    TITLE,
    SUMMARY,
    SOURCE,
    PUBLISHED_AT,
    LINK
) VALUES (
             NEWS_SEQ.NEXTVAL,
             '개발자 채용 시장 동향',
             '기업들의 개발자 채용 방식과 요구 역량이 변화하고 있다는 기사입니다.',
             'IT데일리',
             '2026-07-07',
             'https://example.com/news/3'
         );

COMMIT;