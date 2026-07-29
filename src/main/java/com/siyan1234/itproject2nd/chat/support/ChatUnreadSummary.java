package com.siyan1234.itproject2nd.chat.support;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis에 남아 있는 안읽음 메시지의 전체 개수와 발신자별 개수를 계산합니다.
 *
 * 특정 사용자가 볼 안읽음 개수는
 * 전체 안읽음 개수에서 그 사용자가 직접 보낸 메시지 개수를 제외하여 구합니다.
 */
public final class ChatUnreadSummary {

    private final int totalCount;
    private final Map<Integer, Integer> senderCounts;

    private ChatUnreadSummary(int totalCount, Map<Integer, Integer> senderCounts) {
        this.totalCount = totalCount;
        this.senderCounts = Collections.unmodifiableMap(senderCounts);
    }

    public static ChatUnreadSummary from(List<ChatMessageDto> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ChatUnreadSummary(0, Map.of());
        }

        int totalCount = 0;
        Map<Integer, Integer> senderCounts = new LinkedHashMap<>();

        for (ChatMessageDto message : messages) {
            if (message == null || message.getSenderNo() == null) {
                continue;
            }

            if (!ChatReadStatus.isUnread(message.getReadYn())) {
                continue;
            }

            totalCount++;
            senderCounts.merge(message.getSenderNo(), 1, Integer::sum);
        }

        return new ChatUnreadSummary(totalCount, senderCounts);
    }

    public int getTotalCount() {
        return totalCount;
    }

    public Map<Integer, Integer> getSenderCounts() {
        return senderCounts;
    }

    public int countForViewer(Integer viewerNo) {
        if (viewerNo == null) {
            return totalCount;
        }

        return Math.max(0, totalCount - senderCounts.getOrDefault(viewerNo, 0));
    }
}
