package com.featureflags.shared;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.featureflags.auth.WorkspaceAccount;
import com.featureflags.members.Member;

@Component
public class RequestContext {

	public static final String ACCOUNT = "featureflags.account";

	public static final String PROFILE = "featureflags.profile";

	public WorkspaceAccount account() {
		return (WorkspaceAccount) attribute(ACCOUNT);
	}

	public Member profile() {
		return (Member) attribute(PROFILE);
	}

	public String profileId() {
		Member profile = profile();

		return profile == null ? null : profile.getId();
	}

	public String profileName() {
		Member profile = profile();

		return profile == null ? null : profile.getName();
	}

	private Object attribute(String name) {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

		return attributes == null ? null : attributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
	}
}
