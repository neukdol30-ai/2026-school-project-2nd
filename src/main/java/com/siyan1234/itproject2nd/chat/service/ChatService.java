package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;

import java.util.List;

/**
 * 1:1 채팅 기능의 비즈니스 로직을 정의하는 Service 인터페이스
 *
 * Controller는 이 인터페이스를 통해 상담방 생성, 메시지 조회,
 * 관리자 목록 조회, 상담 종료, 삭제, 읽음 처리 등을 요청한다.
 */
public interface ChatService {

    /**
     * 문의 유형 없이 상담방을 생성하거나 조회한다.
     * 기본 문의 유형은 ETC로 처리한다.
     */
    ChatRoomDto getOrCreateRoom(Integer userNo);

    /**
     * 문의 유형을 포함하여 상담방을 생성하거나 기존 OPEN 상담방을 조회한다.
     */
    ChatRoomDto getOrCreateRoom(Integer userNo, String category);

    /**
     * 사용자의 진행 중인 OPEN 상담방 조회
     */
    ChatRoomDto findOpenRoomByUserNo(Integer userNo);

    /**
     * roomNo로 상담방 상세 조회
     */
    ChatRoomDto findRoomByRoomNo(Integer roomNo);

    /**
     * 전체 상담방 조회
     * Scheduler가 Redis 메시지를 Oracle로 저장할 때 상담방 목록을 순회하기 위해 사용한다.
     */
    List<ChatRoomDto> findAllRooms();

    /**
     * 메시지를 Oracle DB에 저장한다.
     * Redis에 저장된 메시지를 Scheduler가 Oracle로 옮길 때 주로 사용한다.
     */
    void saveMessage(ChatMessageDto chatMessageDto);

    /**
     * 특정 상담방의 Oracle DB 저장 메시지 조회
     */
    List<ChatMessageDto> findMessagesByRoomNo(Integer roomNo);

    /**
     * 관리자 목록에 표시할 마지막 메시지와 마지막 메시지 시간을 갱신한다.
     */
    void updateLastMessage(Integer roomNo, String lastMessage);

    /**
     * 담당 관리자가 없는 상담방에 관리자를 배정한다.
     */
    void assignAdmin(Integer roomNo, Integer adminNo);

    /**
     * 상담방 상태를 CLOSED로 변경한다.
     */
    void closeRoom(Integer roomNo);

    /**
     * 종료된 상담방을 단건 하드 DELETE 한다.
     */
    void deleteRoom(Integer roomNo);

    /**
     * 종료된 상담방 여러 개를 한 번에 하드 DELETE 한다.
     */
    int deleteClosedRooms(List<Integer> roomNoList);

    /**
     * 현재 접속자가 상대방 메시지를 읽은 것으로 처리한다.
     */
    void updateReadYn(Integer roomNo, Integer viewerNo);

    /**
     * 사용자가 문의 유형을 잘못 선택했을 때 기존 상담방의 category를 변경한다.
     */
    void changeCategory(Integer roomNo, Integer userNo, String category);

    /**
     * 관리자 상담 목록 조회
     * 상태, 문의 유형, 키워드, 페이징 조건을 적용한다.
     */
    List<ChatRoomDto> findAdminRooms(
            String status,
            String category,
            String keyword,
            Integer viewerNo,
            int page,
            int size
    );

    /**
     * 관리자 상담 목록 페이징 계산을 위한 전체 개수 조회
     */
    int countAdminRooms(
            String status,
            String category,
            String keyword
    );
}