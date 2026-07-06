package com.siyan1234.itproject2nd.member.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MemberDto {
    private Integer no;
    private String memberId;
    private String name;
    private String nickname;
    private String role;
}