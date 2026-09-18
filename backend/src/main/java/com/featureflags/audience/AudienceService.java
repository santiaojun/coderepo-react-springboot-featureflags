package com.featureflags.audience;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.environments.Environment;
import com.featureflags.environments.EnvironmentService;
import com.featureflags.flags.Flag;
import com.featureflags.flags.FlagConfiguration;
import com.featureflags.flags.FlagConfigurationRepository;
import com.featureflags.flags.FlagRepository;
import com.featureflags.segments.Segment;
import com.featureflags.segments.SegmentRepository;
import com.featureflags.shared.ApiException;
import com.featureflags.targeting.EvaluationResult;
import com.featureflags.targeting.EvaluationUser;
import com.featureflags.targeting.FlagEvaluator;

@Service
public class AudienceService {

	private static final int NOT_FOUND = 404;

	private final AudienceRepository audienceRepository;

	private final FlagRepository flagRepository;

	private final FlagConfigurationRepository flagConfigurationRepository;

	private final SegmentRepository segmentRepository;

	private final EnvironmentService environmentService;

	public AudienceService(AudienceRepository audienceRepository, FlagRepository flagRepository,
			FlagConfigurationRepository flagConfigurationRepository, SegmentRepository segmentRepository,
			EnvironmentService environmentService) {
		this.audienceRepository = audienceRepository;
		this.flagRepository = flagRepository;
		this.flagConfigurationRepository = flagConfigurationRepository;
		this.segmentRepository = segmentRepository;
		this.environmentService = environmentService;
	}

	public Map<String, Object> list(AudienceValidator.ListQuery query) {
		List<EndUser> users = audienceRepository.search(query.search(), query.plan(), query.country(), query.limit());

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("users", users.stream().map(EndUser::toMap).toList());
		body.put("total", audienceRepository.count());
		body.put("plans", audienceRepository.listPlans());
		body.put("countries", audienceRepository.listCountries());

		return body;
	}

	/**
	 * Every active flag's value for one audience member, with the reason each
	 * value was served. This is the answer to "why is this person seeing that?".
	 */
	public Map<String, Object> flagsForUser(String userKey, String environmentKey) {
		Environment environment = environmentService.requireByKey(environmentKey);
		EndUser user = audienceRepository.findByKey(userKey);

		if (user == null) {
			throw new ApiException(NOT_FOUND, "AUDIENCE_USER_NOT_FOUND",
					"No audience member has the key " + userKey + ".");
		}

		Map<String, Segment> segmentsById = new LinkedHashMap<>();

		for (Segment segment : segmentRepository.listByEnvironment(environment.getId())) {
			segmentsById.put(segment.getId(), segment);
		}

		Map<String, FlagConfiguration> byFlagId = new LinkedHashMap<>();

		for (FlagConfiguration configuration : flagConfigurationRepository.listByEnvironment(environment.getId())) {
			byFlagId.put(configuration.getFlagId(), configuration);
		}

		EvaluationUser evaluationUser = new EvaluationUser(user.getKey(), user.toEvaluationAttributes());
		List<Map<String, Object>> evaluations = new ArrayList<>();

		for (Flag flag : flagRepository.list(null, null, Flag.ACTIVE)) {
			FlagConfiguration configuration = byFlagId.get(flag.getId());

			if (configuration == null) {
				continue;
			}

			EvaluationResult result = FlagEvaluator.evaluate(flag, configuration, evaluationUser, segmentsById);
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("flagKey", flag.getKey());
			entry.put("flagName", flag.getName());
			entry.put("flagKind", flag.getKind());
			entry.put("enabled", configuration.isEnabled());
			entry.putAll(result.toMap());
			evaluations.add(entry);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("user", user.toMap());
		body.put("environment", environment.toMap());
		body.put("evaluations", evaluations);

		return body;
	}
}
