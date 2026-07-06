-- ════════════════════════════════════════════════════════════
-- [1] secondpro 계정 생성(DBeaver-sys에서 생성)
-- ════════════════════════════════════════════════════════════

ALTER SESSION SET "_ORACLE_SCRIPT" = TRUE;
--사용자 생성 명령어
CREATE USER secondpro IDENTIFIED BY 1234;
--접속할 수 있는 권한 부여 명령어
GRANT CREATE SESSION TO secondpro;
--테이블 만들 수 있는 권한 부여 명령어
GRANT CREATE TABLE TO secondpro;
--시퀀스 만들 수 있는 권한 부여 명령어
GRANT CREATE SEQUENCE TO secondpro;
--view 만들 수 있는 권한 부여 명령어
GRANT CREATE VIEW TO secondpro;
-- users tablespace에 데이터를 저장할 권한 부여하는 명령어
ALTER USER secondpro quota unlimited ON users;

