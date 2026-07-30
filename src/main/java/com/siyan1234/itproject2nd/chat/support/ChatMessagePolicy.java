package com.siyan1234.itproject2nd.chat.support;

/**
 * 1:1 채팅 메시지의 공통 입력 정책입니다.
 *
 * <p>브라우저 화면뿐 아니라 WebSocket, HTTP 테스트 API, Redis 임시 저장,
 * Oracle 저장 경로가 모두 같은 최대 길이와 공백 처리 기준을 사용하도록 합니다.</p>
 */
public final class ChatMessagePolicy {

    /** 사용자와 관리자가 한 번에 전송할 수 있는 최대 글자 수입니다. */
    public static final int MAX_LENGTH = 1_000;

    /** JSON 파싱 전에 비정상적으로 큰 WebSocket payload를 차단하는 상한입니다. */
    public static final int MAX_WEBSOCKET_PAYLOAD_LENGTH = 8_192;

    public static final String EMPTY_MESSAGE_CODE = "EMPTY_MESSAGE";
    public static final String MESSAGE_TOO_LONG_CODE = "MESSAGE_TOO_LONG";
    public static final String INVALID_PAYLOAD_CODE = "INVALID_PAYLOAD";
    public static final String CHAT_CLOSED_CODE = "CHAT_CLOSED";

    public static final String EMPTY_MESSAGE_TEXT = "메시지를 입력해 주세요.";
    public static final String MESSAGE_TOO_LONG_TEXT = "메시지는 최대 1,000자까지 입력할 수 있습니다.";
    public static final String INVALID_PAYLOAD_TEXT = "올바르지 않은 채팅 요청입니다. 화면을 새로고침한 뒤 다시 시도해 주세요.";
    public static final String CHAT_CLOSED_TEXT = "상담이 종료되어 메시지를 보낼 수 없습니다.";

    private ChatMessagePolicy() {
    }

    /** 저장 전 앞뒤 공백을 제거합니다. */
    public static String normalize(String messageContent) {
        return messageContent == null ? null : messageContent.strip();
    }

    /** Unicode 코드 포인트 기준으로 실제 글자 수를 계산합니다. */
    public static int length(String messageContent) {
        if (messageContent == null || messageContent.isEmpty()) {
            return 0;
        }
        return messageContent.codePointCount(0, messageContent.length());
    }

    public static boolean isBlank(String messageContent) {
        return messageContent == null || messageContent.isBlank();
    }

    public static boolean exceedsMaxLength(String messageContent) {
        return length(messageContent) > MAX_LENGTH;
    }

    public static boolean isValid(String messageContent) {
        String normalized = normalize(messageContent);
        return !isBlank(normalized) && !exceedsMaxLength(normalized);
    }

    /**
     * 내부 저장 경로에서 사용할 검증 메서드입니다.
     * 잘못된 메시지가 Redis나 Oracle로 넘어가지 않도록 예외로 중단합니다.
     */
    public static String normalizeAndValidate(String messageContent) {
        String normalized = normalize(messageContent);

        if (isBlank(normalized)) {
            throw new IllegalArgumentException(EMPTY_MESSAGE_TEXT);
        }

        if (exceedsMaxLength(normalized)) {
            throw new IllegalArgumentException(MESSAGE_TOO_LONG_TEXT);
        }

        return normalized;
    }
}
