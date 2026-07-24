package com.siyan1234.itproject2nd.member.dao;

import com.siyan1234.itproject2nd.member.dto.SocialAccountDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SocialAccountDao {


    // provider + provider_id 조합으로 이 소셜 계정 이미 연결되어 있는지 조회.
    // 있으면 SocialAccountDto, 없으면 null 반환.
    // 매개변수 2개 이상이라 @Param -> XML 인식 가능.
    SocialAccountDto findByProviderAndProviderId(@Param("provider") String provider,
                                                 @Param("providerId") String providerId);

    // 새 소셜 계정 연결 저장. MyBatis는 INSERT/UPDATE/DELETE 결과 "영향 받은 행 수"를 int로 반환.
    int insertSocialAccount(SocialAccountDto socialAccountDto);

    // 한 회원에게 연결된 모든 소셜 계정을 조회
    List<SocialAccountDto> findAllByMemberNo(
            @Param("memberNo") Integer memberNo); // member 테이블의 회원 고유 번호를 XML에 전달
}
