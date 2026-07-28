package com.siyan1234.itproject2nd.mypage.service;

import com.siyan1234.itproject2nd.mypage.dao.MyPageDao;
import com.siyan1234.itproject2nd.mypage.dto.MyPageProfileDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 마이페이지 모달에 표시할 프로필·활동·최근 내역 조회를 담당합니다. */
@Service
@RequiredArgsConstructor
public class MyPageQueryService {

    private final MyPageDao myPageDao;

    /** 마이페이지 모달을 처음 열거나 변경 후 다시 그릴 때 필요한 데이터를 한 번에 조회합니다. */
    @Transactional(readOnly = true)
    public MyPageResponseDto getMyPage(Integer memberNo) {
        MyPageProfileDto profile = findProfile(memberNo);
        if (profile == null) {
            return MyPageResponseDto.anonymous();
        }

        return MyPageResponseDto.loggedIn(
                profile,
                myPageDao.findActivityByMemberNo(memberNo),
                myPageDao.findRecentBoards(memberNo),
                myPageDao.findRecentChats(memberNo)
        );
    }

    @Transactional(readOnly = true)
    public MyPageProfileDto findProfile(Integer memberNo) {
        if (memberNo == null) {
            return null;
        }
        return myPageDao.findProfileByNo(memberNo);
    }

    @Transactional(readOnly = true)
    public boolean isSocialLoginUser(Integer memberNo) {
        MyPageProfileDto profile = findProfile(memberNo);
        return profile != null && profile.isSocialLoginUser();
    }
}
