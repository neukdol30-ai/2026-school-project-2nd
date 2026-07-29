package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.support.ChatReadStatus;
import com.siyan1234.itproject2nd.chat.support.ChatRoomLockManager;
import com.siyan1234.itproject2nd.chat.support.ChatUnreadSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 채팅 메시지 임시 저장 Service입니다.
 *
 * 메시지 처리 흐름:
 * 1. WebSocket 수신 메시지를 Redis List에 먼저 저장합니다.
 * 2. 안읽음 개수는 Redis Hash 카운터로 함께 관리합니다.
 * 3. 저장 대기 상담방 번호는 Redis Set에 등록합니다.
 * 4. Scheduler는 Set에 등록된 상담방만 Oracle DB로 옮깁니다.
 */
@Service
@RequiredArgsConstructor
public class ChatRedisService {

    private static final String MESSAGE_KEY_PREFIX = "chat:room";
    private static final String UNREAD_KEY_PREFIX = "chat:room:";
    private static final String UNREAD_KEY_SUFFIX = ":unread";
    private static final String PENDING_ROOM_KEY = "chat:pending-rooms";
    private static final String UNREAD_TOTAL_FIELD = "total";
    private static final String UNREAD_SENDER_FIELD_PREFIX = "sender:";

    /**
     * 메시지 List 저장, 안읽음 카운터 증가, 저장 대기 방 등록을 한 번에 처리합니다.
     * 세 명령 중 일부만 반영되는 상태를 막기 위해 Redis Lua script를 사용합니다.
     */
    private static final DefaultRedisScript<Long> SAVE_MESSAGE_SCRIPT = new DefaultRedisScript<>("""
            redis.call('RPUSH', KEYS[1], ARGV[1])
            redis.call('HSETNX', KEYS[2], 'total', 0)

            if ARGV[2] == 'N' and ARGV[3] ~= '' then
                redis.call('HINCRBY', KEYS[2], 'total', 1)
                redis.call('HINCRBY', KEYS[2], ARGV[3], 1)
            end

            redis.call('SADD', KEYS[3], ARGV[4])
            return 1
            """, Long.class);

    /**
     * DB 저장이 끝난 List 앞부분 제거, 안읽음 카운터 차감,
     * 저장 대기 Set 정리를 하나의 Redis 작업으로 처리합니다.
     */
    private static final DefaultRedisScript<Long> COMPLETE_FLUSH_SCRIPT = new DefaultRedisScript<>("""
            local savedCount = tonumber(ARGV[2]) or 0
            local currentSize = redis.call('LLEN', KEYS[1])

            if savedCount > 0 then
                if currentSize <= savedCount then
                    redis.call('DEL', KEYS[1])
                else
                    redis.call('LTRIM', KEYS[1], savedCount, -1)
                end
            end

            redis.call('HSETNX', KEYS[2], 'total', 0)

            local unreadTotal = tonumber(ARGV[3]) or 0
            if unreadTotal > 0 then
                local newTotal = redis.call('HINCRBY', KEYS[2], 'total', -unreadTotal)
                if newTotal < 0 then
                    redis.call('HSET', KEYS[2], 'total', 0)
                end
            end

            local pairCount = tonumber(ARGV[4]) or 0
            local argumentIndex = 5

            for i = 1, pairCount do
                local senderField = ARGV[argumentIndex]
                local senderCount = tonumber(ARGV[argumentIndex + 1]) or 0

                if senderCount > 0 and redis.call('HEXISTS', KEYS[2], senderField) == 1 then
                    local newSenderCount = redis.call('HINCRBY', KEYS[2], senderField, -senderCount)
                    if newSenderCount <= 0 then
                        redis.call('HDEL', KEYS[2], senderField)
                    end
                end

                argumentIndex = argumentIndex + 2
            end

            local remainingSize = redis.call('LLEN', KEYS[1])
            if remainingSize == 0 then
                redis.call('SREM', KEYS[3], ARGV[1])
            else
                redis.call('SADD', KEYS[3], ARGV[1])
            end

            return remainingSize
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatRoomLockManager roomLockManager;

    /** 이전 버전 Redis List를 저장 대기 Set에 등록하는 복구 작업은 서버 실행당 한 번만 수행합니다. */
    private final AtomicBoolean legacyRecoveryCompleted = new AtomicBoolean(false);

    /** 메시지를 Redis에 저장하고 해당 상담방을 DB 저장 대기 대상으로 등록합니다. */
    public void saveMessage(ChatMessageDto messageDto) {
        if (messageDto == null || messageDto.getRoomNo() == null) {
            return;
        }

        roomLockManager.execute(messageDto.getRoomNo(), () -> saveMessageLocked(messageDto));
    }

    /** 특정 상담방의 Redis 메시지를 저장 순서대로 조회합니다. */
    public List<ChatMessageDto> findMessages(Integer roomNo) {
        if (roomNo == null) {
            return List.of();
        }

        return roomLockManager.execute(roomNo, () -> {
            List<ChatMessageDto> messages = readMessages(roomNo);
            initializeUnreadCounterIfMissing(roomNo, messages);
            return messages;
        });
    }

    /** Scheduler가 처리해야 하는 상담방 번호만 반환합니다. */
    public Set<Integer> findPendingRoomNos() {
        recoverLegacyPendingRoomsOnce();

        Set<String> roomNoValues = redisTemplate.opsForSet().members(PENDING_ROOM_KEY);
        if (roomNoValues == null || roomNoValues.isEmpty()) {
            return Set.of();
        }

        Set<Integer> roomNos = new LinkedHashSet<>();
        for (String value : roomNoValues) {
            Integer roomNo = parseRoomNo(value);
            if (roomNo != null) {
                roomNos.add(roomNo);
            } else {
                redisTemplate.opsForSet().remove(PENDING_ROOM_KEY, value);
            }
        }

        return roomNos;
    }

    /** 상담방 하드 삭제 시 Redis 메시지, 카운터, 저장 대기 상태를 모두 삭제합니다. */
    public void deleteMessages(Integer roomNo) {
        if (roomNo == null) {
            return;
        }

        roomLockManager.execute(roomNo, () -> {
            redisTemplate.delete(List.of(createMessageKey(roomNo), createUnreadKey(roomNo)));
            redisTemplate.opsForSet().remove(PENDING_ROOM_KEY, String.valueOf(roomNo));
        });
    }

    /**
     * Oracle 저장이 완료된 메시지만 Redis List 앞쪽에서 제거합니다.
     *
     * Scheduler가 메시지를 읽는 동안 새 메시지가 뒤에 들어오더라도
     * 이번에 DB에 저장한 앞부분만 제거하여 새 메시지가 소실되지 않도록 합니다.
     */
    public void deleteSavedMessages(Integer roomNo, List<ChatMessageDto> savedMessages) {
        if (roomNo == null || savedMessages == null || savedMessages.isEmpty()) {
            cleanupPendingRoomIfEmpty(roomNo);
            return;
        }

        roomLockManager.execute(roomNo, () -> completeFlushLocked(roomNo, savedMessages));
    }

    /** Redis와 Oracle의 메시지 읽음 상태를 맞추기 위해 Redis List의 상대방 메시지를 갱신합니다. */
    public void updateReadYn(Integer roomNo, Integer viewerNo) {
        if (roomNo == null || viewerNo == null) {
            return;
        }

        roomLockManager.execute(roomNo, () -> updateReadYnLocked(roomNo, viewerNo));
    }

    /**
     * Redis 안읽음 개수를 Hash 카운터에서 O(1)로 조회합니다.
     *
     * 이전 구현처럼 메시지 JSON 전체를 매번 역직렬화하지 않으므로
     * 관리자 상담 목록과 사용자 상담내역 목록의 조회 비용이 일정하게 유지됩니다.
     */
    public int countUnreadMessages(Integer roomNo, Integer viewerNo) {
        if (roomNo == null) {
            return 0;
        }

        return roomLockManager.execute(roomNo, () -> countUnreadMessagesLocked(roomNo, viewerNo));
    }

    /** 저장 대기 Set에 값은 있지만 실제 메시지가 없는 잘못된 상태를 정리합니다. */
    public void cleanupPendingRoomIfEmpty(Integer roomNo) {
        if (roomNo == null) {
            return;
        }

        roomLockManager.execute(roomNo, () -> {
            Long size = redisTemplate.opsForList().size(createMessageKey(roomNo));
            if (size == null || size == 0L) {
                redisTemplate.opsForSet().remove(PENDING_ROOM_KEY, String.valueOf(roomNo));
                redisTemplate.delete(createUnreadKey(roomNo));
            }
        });
    }

    private void saveMessageLocked(ChatMessageDto messageDto) {
        try {
            if (messageDto.getCreatedDate() == null) {
                messageDto.setCreatedDate(LocalDateTime.now());
            }

            messageDto.setReadYn(ChatReadStatus.normalize(messageDto.getReadYn()));

            String json = objectMapper.writeValueAsString(messageDto);
            String senderField = messageDto.getSenderNo() == null
                    ? ""
                    : createSenderField(messageDto.getSenderNo());

            Long result = redisTemplate.execute(
                    SAVE_MESSAGE_SCRIPT,
                    List.of(
                            createMessageKey(messageDto.getRoomNo()),
                            createUnreadKey(messageDto.getRoomNo()),
                            PENDING_ROOM_KEY
                    ),
                    json,
                    messageDto.getReadYn(),
                    senderField,
                    String.valueOf(messageDto.getRoomNo())
            );

            if (result == null || result != 1L) {
                throw new IllegalStateException("Redis 메시지 저장 script 결과가 올바르지 않습니다.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Redis 메시지 저장 실패", e);
        }
    }

    private List<ChatMessageDto> readMessages(Integer roomNo) {
        List<String> jsonList = redisTemplate.opsForList().range(createMessageKey(roomNo), 0, -1);
        List<ChatMessageDto> messages = new ArrayList<>();

        if (jsonList == null || jsonList.isEmpty()) {
            return messages;
        }

        for (String json : jsonList) {
            try {
                messages.add(objectMapper.readValue(json, ChatMessageDto.class));
            } catch (Exception e) {
                throw new RuntimeException("Redis 메시지 조회 실패", e);
            }
        }

        return messages;
    }

    private void updateReadYnLocked(Integer roomNo, Integer viewerNo) {
        String messageKey = createMessageKey(roomNo);
        List<String> jsonList = redisTemplate.opsForList().range(messageKey, 0, -1);

        if (jsonList == null || jsonList.isEmpty()) {
            cleanupPendingRoomIfEmpty(roomNo);
            return;
        }

        List<ChatMessageDto> allMessages = new ArrayList<>();
        List<ChatMessageDto> changedMessages = new ArrayList<>();
        List<String> updatedJsonValues = new ArrayList<>();
        List<Integer> updatedIndexes = new ArrayList<>();

        for (int i = 0; i < jsonList.size(); i++) {
            String json = jsonList.get(i);

            try {
                ChatMessageDto messageDto = objectMapper.readValue(json, ChatMessageDto.class);
                allMessages.add(messageDto);

                if (messageDto.getSenderNo() == null
                        || messageDto.getSenderNo().equals(viewerNo)
                        || !ChatReadStatus.isUnread(messageDto.getReadYn())) {
                    continue;
                }

                ChatMessageDto unreadSnapshot = copyMessage(messageDto);
                changedMessages.add(unreadSnapshot);

                messageDto.setReadYn(ChatReadStatus.READ);
                updatedIndexes.add(i);
                updatedJsonValues.add(objectMapper.writeValueAsString(messageDto));
            } catch (Exception e) {
                throw new RuntimeException("Redis 읽음 처리 실패", e);
            }
        }

        initializeUnreadCounterIfMissing(roomNo, allMessages);

        for (int i = 0; i < updatedIndexes.size(); i++) {
            redisTemplate.opsForList().set(messageKey, updatedIndexes.get(i), updatedJsonValues.get(i));
        }

        if (!changedMessages.isEmpty()) {
            decreaseUnreadCounter(roomNo, ChatUnreadSummary.from(changedMessages));
        }
    }

    private int countUnreadMessagesLocked(Integer roomNo, Integer viewerNo) {
        Long messageSize = redisTemplate.opsForList().size(createMessageKey(roomNo));
        if (messageSize == null || messageSize == 0L) {
            redisTemplate.delete(createUnreadKey(roomNo));
            return 0;
        }

        String unreadKey = createUnreadKey(roomNo);
        Object totalValue = redisTemplate.opsForHash().get(unreadKey, UNREAD_TOTAL_FIELD);

        if (totalValue == null) {
            List<ChatMessageDto> messages = readMessages(roomNo);
            initializeUnreadCounterIfMissing(roomNo, messages);
            totalValue = redisTemplate.opsForHash().get(unreadKey, UNREAD_TOTAL_FIELD);
        }

        long totalCount = parseLong(totalValue);
        if (viewerNo == null) {
            return toSafeInt(totalCount);
        }

        Object ownValue = redisTemplate.opsForHash().get(unreadKey, createSenderField(viewerNo));
        return toSafeInt(Math.max(0L, totalCount - parseLong(ownValue)));
    }

    private void completeFlushLocked(Integer roomNo, List<ChatMessageDto> savedMessages) {
        ChatUnreadSummary unreadSummary = ChatUnreadSummary.from(savedMessages);
        List<String> arguments = new ArrayList<>();
        arguments.add(String.valueOf(roomNo));
        arguments.add(String.valueOf(savedMessages.size()));
        arguments.add(String.valueOf(unreadSummary.getTotalCount()));
        arguments.add(String.valueOf(unreadSummary.getSenderCounts().size()));

        for (Map.Entry<Integer, Integer> entry : unreadSummary.getSenderCounts().entrySet()) {
            arguments.add(createSenderField(entry.getKey()));
            arguments.add(String.valueOf(entry.getValue()));
        }

        Long result = redisTemplate.execute(
                COMPLETE_FLUSH_SCRIPT,
                List.of(createMessageKey(roomNo), createUnreadKey(roomNo), PENDING_ROOM_KEY),
                arguments.toArray(Object[]::new)
        );

        if (result == null) {
            throw new RuntimeException("Redis 저장 완료 메시지 정리 실패");
        }
    }

    private void initializeUnreadCounterIfMissing(Integer roomNo, List<ChatMessageDto> messages) {
        String unreadKey = createUnreadKey(roomNo);
        Boolean hasTotal = redisTemplate.opsForHash().hasKey(unreadKey, UNREAD_TOTAL_FIELD);

        if (Boolean.TRUE.equals(hasTotal)) {
            return;
        }

        ChatUnreadSummary summary = ChatUnreadSummary.from(messages);
        Map<String, String> counterValues = new LinkedHashMap<>();
        counterValues.put(UNREAD_TOTAL_FIELD, String.valueOf(summary.getTotalCount()));

        for (Map.Entry<Integer, Integer> entry : summary.getSenderCounts().entrySet()) {
            counterValues.put(createSenderField(entry.getKey()), String.valueOf(entry.getValue()));
        }

        redisTemplate.opsForHash().putAll(unreadKey, counterValues);
    }

    private void decreaseUnreadCounter(Integer roomNo, ChatUnreadSummary summary) {
        if (summary.getTotalCount() <= 0) {
            return;
        }

        String unreadKey = createUnreadKey(roomNo);
        Long total = redisTemplate.opsForHash().increment(
                unreadKey,
                UNREAD_TOTAL_FIELD,
                -summary.getTotalCount()
        );

        if (total != null && total < 0) {
            redisTemplate.opsForHash().put(unreadKey, UNREAD_TOTAL_FIELD, "0");
        }

        for (Map.Entry<Integer, Integer> entry : summary.getSenderCounts().entrySet()) {
            String senderField = createSenderField(entry.getKey());
            Boolean exists = redisTemplate.opsForHash().hasKey(unreadKey, senderField);

            if (!Boolean.TRUE.equals(exists)) {
                continue;
            }

            Long senderCount = redisTemplate.opsForHash().increment(
                    unreadKey,
                    senderField,
                    -entry.getValue()
            );

            if (senderCount == null || senderCount <= 0) {
                redisTemplate.opsForHash().delete(unreadKey, senderField);
            }
        }
    }

    /**
     * 패치 적용 전에 Redis에 쌓여 있던 chat:room{번호} List를 한 번만 검색하여
     * 새 저장 대기 Set에 등록합니다. 이후 Scheduler는 KEYS 명령을 사용하지 않습니다.
     */
    private void recoverLegacyPendingRoomsOnce() {
        if (!legacyRecoveryCompleted.compareAndSet(false, true)) {
            return;
        }

        Set<String> legacyKeys = redisTemplate.keys(MESSAGE_KEY_PREFIX + "[0-9]*");
        if (legacyKeys == null || legacyKeys.isEmpty()) {
            return;
        }

        for (String key : legacyKeys) {
            Integer roomNo = parseRoomNo(key.substring(MESSAGE_KEY_PREFIX.length()));
            if (roomNo == null) {
                continue;
            }

            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > 0L) {
                redisTemplate.opsForSet().add(PENDING_ROOM_KEY, String.valueOf(roomNo));
            }
        }
    }

    private ChatMessageDto copyMessage(ChatMessageDto source) {
        ChatMessageDto copy = new ChatMessageDto();
        copy.setMessageNo(source.getMessageNo());
        copy.setRoomNo(source.getRoomNo());
        copy.setSenderNo(source.getSenderNo());
        copy.setMessageContent(source.getMessageContent());
        copy.setReadYn(source.getReadYn());
        copy.setCreatedDate(source.getCreatedDate());
        return copy;
    }

    private String createMessageKey(Integer roomNo) {
        return MESSAGE_KEY_PREFIX + roomNo;
    }

    private String createUnreadKey(Integer roomNo) {
        return UNREAD_KEY_PREFIX + roomNo + UNREAD_KEY_SUFFIX;
    }

    private String createSenderField(Integer senderNo) {
        return UNREAD_SENDER_FIELD_PREFIX + senderNo;
    }

    private Integer parseRoomNo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private long parseLong(Object value) {
        if (value == null) {
            return 0L;
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private int toSafeInt(long value) {
        if (value <= 0L) {
            return 0;
        }

        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
