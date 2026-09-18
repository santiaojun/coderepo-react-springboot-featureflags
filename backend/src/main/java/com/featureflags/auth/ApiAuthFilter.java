package com.featureflags.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.featureflags.members.Member;
import com.featureflags.members.MemberService;
import com.featureflags.shared.ApiException;
import com.featureflags.shared.GlobalExceptionHandler;
import com.featureflags.shared.ObjectIds;
import com.featureflags.shared.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiAuthFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private static final String API_PREFIX = "/api/v1";

	private static final String HEALTH_PATH = "/api/v1/health";

	private static final String LOGIN_PATH = "/api/v1/auth/login";

	private static final List<String> WORKSPACE_ONLY_PREFIXES = List.of("/api/v1/auth/", "/api/v1/members");

	private static final int UNAUTHORIZED = 401;

	private static final int FORBIDDEN = 403;

	private final AuthService authService;

	private final MemberService memberService;

	private final ObjectMapper objectMapper;

	public ApiAuthFilter(AuthService authService, MemberService memberService, ObjectMapper objectMapper) {
		this.authService = authService;
		this.memberService = memberService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();

		return !path.startsWith(API_PREFIX) || isPublic(request, path);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		try {
			AuthService.Authentication authentication = authService.authenticate(readToken(request));
			request.setAttribute(RequestContext.ACCOUNT, authentication.account());

			if (needsProfile(request.getRequestURI())) {
				request.setAttribute(RequestContext.PROFILE,
						resolveProfile(authentication.account(), authentication.payload().profileId()));
			}
		} catch (ApiException exception) {
			write(response, exception);

			return;
		}

		chain.doFilter(request, response);
	}

	private boolean isPublic(HttpServletRequest request, String path) {
		return HEALTH_PATH.equals(path) && "GET".equals(request.getMethod())
				|| LOGIN_PATH.equals(path) && "POST".equals(request.getMethod());
	}

	private boolean needsProfile(String path) {
		return WORKSPACE_ONLY_PREFIXES.stream().noneMatch(path::startsWith);
	}

	private String readToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");

		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			throw authRequired();
		}

		String token = header.substring(BEARER_PREFIX.length()).trim();

		if (token.isEmpty()) {
			throw authRequired();
		}

		return token;
	}

	private Member resolveProfile(WorkspaceAccount account, String profileId) {
		if (profileId == null || profileId.isEmpty()) {
			throw new ApiException(UNAUTHORIZED, "PROFILE_REQUIRED", "Select a Flagpost member to continue.");
		}

		if (!ObjectIds.isValid(profileId)) {
			throw new ApiException(UNAUTHORIZED, "INVALID_PROFILE", "Select a valid Flagpost member.");
		}

		if (!account.getAllowedProfileIds().contains(profileId)) {
			throw new ApiException(FORBIDDEN, "PROFILE_FORBIDDEN",
					"This member is not available in the current workspace.");
		}

		Member profile = memberService.findProfileById(profileId);

		if (profile == null) {
			throw new ApiException(UNAUTHORIZED, "PROFILE_NOT_FOUND", "This Flagpost member is no longer available.");
		}

		return profile;
	}

	private ApiException authRequired() {
		return new ApiException(UNAUTHORIZED, "AUTH_REQUIRED", "Sign in to the Flagpost workspace to continue.");
	}

	private void write(HttpServletResponse response, ApiException exception) throws IOException {
		response.setStatus(exception.getStatusCode());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(),
				GlobalExceptionHandler.envelope(exception.getCode(), exception.getMessage(), exception.getDetails()));
	}
}
