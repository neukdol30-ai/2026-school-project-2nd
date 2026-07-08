package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
}
