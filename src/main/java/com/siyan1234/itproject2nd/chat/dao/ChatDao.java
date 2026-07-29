package com.siyan1234.itproject2nd.chat.dao;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 채팅 기능 MyBatis Mapper 인터페이스
 *
 * 실제 SQL은 chat-mapper.xml에 작성되어 있고,
 * 이 인터페이스는 Java 코드에서 Mapper SQL을 호출하기 위한 통로 역할을 한다.
 */
@Mapper
public interface ChatDao {

    /**
     * 상담방 생성
     */
    int createRoom(ChatRoomDto chatRoomDto);

    /**
     * 사용자의 OPEN 상담방 조회
     */
    ChatRoomDto findRoomByUserNo(@Param("userNo") Integer userNo);

    /**
     * roomNo로 상담방 상세 조회
     */
    ChatRoomDto findRoomByRoomNo(@Param("roomNo") Integer roomNo);

    /**
     * 관리자 상담 목록 조회
     * 검색 조건과 페이징 조건을 적용한다.
     */
    List<ChatRoomDto> findAdminRooms(
            @Param("status") String status,
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("viewerNo") Integer viewerNo,
            @Param("offset") int offset,
            @Param("size") int size
    );

    /**
     * 관리자 상담 목록 페이징을 위한 전체 개수 조회
     */
    int countAdminRooms(
            @Param("status") String status,
            @Param("category") String category,
            @Param("keyword") String keyword
    );

    /**
     * 문의 유형 변경
     */
    int updateRoomCategory(
            @Param("roomNo") Integer roomNo,
            @Param("category") String category
    );

    /**
     * 마지막 메시지와 마지막 메시지 시간 갱신
     */
    int updateLastMessage(ChatRoomDto chatRoomDto);

    /**
     * 담당 관리자 배정
     */
    int assignAdmin(
            @Param("roomNo") Integer roomNo,
            @Param("adminNo") Integer adminNo
    );

    /**
     * 상담방 종료
     */
    int closeRoom(@Param("roomNo") Integer roomNo);

    /**
     * 종료 상담방 단건 삭제
     */
    int deleteRoom(@Param("roomNo") Integer roomNo);

    /**
     * 종료 상담방 다중 삭제
     */
    int deleteClosedRooms(@Param("roomNoList") List<Integer> roomNoList);

    /**
     * 메시지 Oracle DB 저장
     */
    int saveMessage(ChatMessageDto chatMessageDto);

    /**
     * 한 상담방의 메시지를 Oracle 익명 PL/SQL 블록으로 묶어 저장합니다.
     */
    void saveMessages(@Param("messages") List<ChatMessageDto> messages);

    /**
     * 특정 상담방 메시지 조회
     */
    List<ChatMessageDto> findMessagesByRoomNo(@Param("roomNo") Integer roomNo);

    /**
     * 상대방 메시지 읽음 처리
     */
    int updateReadYn(
            @Param("roomNo") Integer roomNo,
            @Param("viewerNo") Integer viewerNo
    );

    //상담내역 목록 조회용 메서드
    List<ChatRoomDto> findUserRooms(
            @Param("userNo") Integer userNo,
            @Param("pageStart") int pageStart,
            @Param("pageEnd") int  pageEnd
    );

    int countUserRooms(@Param("userNo") Integer userNo);
}