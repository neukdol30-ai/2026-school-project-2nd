-- 1. 개인 로컬 개발 DB에서만 실행
-- 2. 실행 후 01_schema_latest.sql → 02_seed_data.sql 순서로 실행


-- ════════════════════════════════════════════════════════════
-- [1] 게시판 관련 테이블 삭제
-- 자식 테이블부터 삭제해야 외래 키 오류 방지
-- board가 guest_author를 참조하므로 guest_author는 board 다음에 삭제
-- ════════════════════════════════════════════════════════════
DROP TABLE board_comment CASCADE CONSTRAINTS PURGE;

DROP TABLE board_file CASCADE CONSTRAINTS PURGE;

DROP TABLE board CASCADE CONSTRAINTS PURGE;

DROP TABLE guest_author CASCADE CONSTRAINTS PURGE;


-- ════════════════════════════════════════════════════════════
-- [2] 방문 기록 테이블 삭제
-- 일별 통계 테이블을 먼저 삭제한 뒤 원본 로그를 삭제
-- ════════════════════════════════════════════════════════════
DROP TABLE visit_daily_stat CASCADE CONSTRAINTS PURGE;

DROP TABLE visit_log CASCADE CONSTRAINTS PURGE;


-- ════════════════════════════════════════════════════════════
-- [3] 채팅 관련 테이블 삭제
-- ════════════════════════════════════════════════════════════
DROP TABLE chat_message CASCADE CONSTRAINTS PURGE;

DROP TABLE chat_room CASCADE CONSTRAINTS PURGE;


-- ════════════════════════════════════════════════════════════
-- [4] 소셜 계정 테이블 삭제
-- ════════════════════════════════════════════════════════════
DROP TABLE social_account CASCADE CONSTRAINTS PURGE;


-- ════════════════════════════════════════════════════════════
-- [5] 주식·뉴스 관련 객체 삭제
-- ════════════════════════════════════════════════════════════
DROP TABLE stock_info CASCADE CONSTRAINTS PURGE;

DROP TABLE news CASCADE CONSTRAINTS PURGE;

DROP SEQUENCE news_seq;


-- ════════════════════════════════════════════════════════════
-- [6] 캘린더 관련 테이블 삭제
-- 두 테이블 모두 member를 참조하므로 member보다 먼저 삭제
-- ════════════════════════════════════════════════════════════
DROP TABLE google_calendar_token CASCADE CONSTRAINTS PURGE;

DROP TABLE calendar_event CASCADE CONSTRAINTS PURGE;


-- ════════════════════════════════════════════════════════════
-- [7] 지도 즐겨찾기 테이블 삭제
-- member를 참조하므로 member보다 먼저 삭제
-- ════════════════════════════════════════════════════════════
DROP TABLE map_favorite_place CASCADE CONSTRAINTS PURGE;


-- ════════════════════════════════════════════════════════════
-- [8] 회원 테이블 삭제
-- 다른 테이블들이 참조하는 부모 테이블이므로 마지막에 삭제
-- ════════════════════════════════════════════════════════════
DROP TABLE member CASCADE CONSTRAINTS PURGE;
