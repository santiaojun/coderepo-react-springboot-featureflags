package com.featureflags.members;

import java.util.List;

import org.springframework.stereotype.Service;

import com.featureflags.shared.ObjectIds;

@Service
public class MemberService {

	private final MemberRepository memberRepository;

	public MemberService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public List<Member> listProfiles(List<String> ids) {
		return memberRepository.list(ids);
	}

	public Member findProfileById(String id) {
		return ObjectIds.isValid(id) ? memberRepository.findById(id) : null;
	}
}
