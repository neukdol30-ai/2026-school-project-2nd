package com.siyan1234.itproject2nd.admin.support;

/**
 * 관리자 회원 작업의 결과를 명확한 사유로 반환합니다.
 *
 * <p>Controller가 작업 실패 이유를 확인하려고 회원 정보를 다시 조회하지 않도록,
 * Service가 검증 결과와 DB 처리 결과를 하나의 타입으로 전달합니다.</p>
 */
public enum AdminMemberActionResult {

    SUCCESS,
    ACTOR_NOT_ALLOWED,
    TARGET_NOT_FOUND,
    SELF_ACTION_DENIED,
    BANNED_MEMBER_DENIED,
    LAST_ADMIN_DENIED,
    ADMIN_ACCOUNT_DENIED,
    UPDATE_FAILED;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
