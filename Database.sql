CREATE TABLE NEWS (
                      NEWS_ID NUMBER PRIMARY KEY,
                      TITLE VARCHAR2(300) NOT NULL,
                      SUMMARY VARCHAR2(1000),
                      SOURCE VARCHAR2(100),
                      PUBLISHED_AT VARCHAR2(50),
                      LINK VARCHAR2(1000)
);