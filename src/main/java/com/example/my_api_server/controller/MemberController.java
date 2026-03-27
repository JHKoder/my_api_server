package com.example.my_api_server.controller;

import ch.qos.logback.core.util.StringUtil;
import com.example.my_api_server.service.MemberDBService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/member")
public class MemberController {

    private final MemberDBService memberService;

    //회원가입
    @PostMapping //POST(리소스 등록, 약속)
    public Long signUp(@Validated @RequestBody MemberSignUpDto dto) {
        log.info("스레드 이름 : {} ", Thread.currentThread().getName());
        log.info("스레드 상태 : {} ", Thread.currentThread().getState());
        log.info("스레드 그룹 : {} ", Thread.currentThread().getThreadGroup());
        log.info("스레드 우선순위 : {} ", Thread.currentThread().getPriority());
        System.out.println("email = " + dto.email());
        System.out.println("password = " + dto.password());

        //validation 검증
        if (StringUtil.isNullOrEmpty(dto.email()) || StringUtil.isNullOrEmpty(
            dto.password())) {
            throw new RuntimeException("email or password가 빈값이 되면 안됩니다!");
        }

        Long memberId = null;
        try {
            memberId = memberService.signUp(dto.email(), dto.password());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return memberId;
    } //즉, 요청이 끝날떄까지 영속성 컨텍스트 살아있게 됩니다.(메모리 쓰게 되죠)

    //회원조회
//    @GetMapping("/{id}")
//    public Member findMember(@PathVariable Long id) {
//        Member member = memberService.findMember(id);
//        return member;
//    }


    @GetMapping("/test")
    public void test() {
        memberService.tx1();
    }
}
