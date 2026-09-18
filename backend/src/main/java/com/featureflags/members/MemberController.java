package com.featureflags.members;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.featureflags.shared.RequestContext;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

	private final MemberService memberService;

	private final RequestContext requestContext;

	public MemberController(MemberService memberService, RequestContext requestContext) {
		this.memberService = memberService;
		this.requestContext = requestContext;
	}

	@GetMapping
	public Map<String, Object> listMembers() {
		List<Member> members = memberService.listProfiles(requestContext.account().getAllowedProfileIds());

		return Map.of("data", members.stream().map(Member::toMap).toList());
	}
}
