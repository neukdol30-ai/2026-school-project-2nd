package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final MemberService memberService;

    @GetMapping({"", "/"})
    public String adminMain() {
        return "admin/main";
    }

    @GetMapping("/members")
    public String memberList(Model model) {

        List<MemberDto> memberList = memberService.findAllMembers();

        model.addAttribute("memberList", memberList);

        return "admin/member-list";
    }

    // 관리자 회원 상세보기 화면 / GET /admin/members/{no}
    @GetMapping("/members/{no}")
    public String memberDetail(@PathVariable("no") Integer no, Model model) {

        MemberDto member = memberService.findByNo(no); // no로 회원 한 명 조회.

        if (member == null) {
            return "redirect:/admin/members";
        }

        model.addAttribute("member", member); // 조회한 회원을 화면으로 전달(이름 "member")

        return "admin/member-detail"; // templates/admin/member-detail.html
    }

}
