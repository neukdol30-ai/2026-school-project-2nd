/* =========================================================
   V011 1:1 채팅 메시지 최대 1,000자 제한

   적용 대상
   - 기존 DB를 계속 사용하는 팀원

   처리 방식
   - 기존 CHAT_MESSAGE 컬럼은 VARCHAR2(2000 CHAR) 상태를 유지합니다.
   - 기존 1,000자 초과 메시지를 삭제하거나 자르지 않습니다.
   - INSERT 또는 MESSAGE_CONTENT 변경 시에만 Trigger가 길이를 검사합니다.
   - 과거 장문 메시지의 READ_YN만 변경하는 읽음 처리는 정상 허용합니다.

   참고
   - 새 DB는 01_schema_latest.sql에서 VARCHAR2(1000 CHAR)와
     CK_CHAT_MESSAGE_LENGTH 제약조건을 적용하므로 V011을 별도 실행하지 않습니다.
   ========================================================= */

CREATE OR REPLACE TRIGGER trg_chat_message_length
BEFORE INSERT OR UPDATE OF message_content ON chat_message
FOR EACH ROW
BEGIN
    IF TRIM(:NEW.message_content) IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, '채팅 메시지는 비어 있을 수 없습니다.');
    END IF;

    IF LENGTH(:NEW.message_content) > 1000 THEN
        RAISE_APPLICATION_ERROR(-20002, '채팅 메시지는 최대 1,000자까지 저장할 수 있습니다.');
    END IF;
END;
/

COMMIT;

/* 적용 확인
SELECT trigger_name, status, triggering_event
FROM user_triggers
WHERE trigger_name = 'TRG_CHAT_MESSAGE_LENGTH';
*/
