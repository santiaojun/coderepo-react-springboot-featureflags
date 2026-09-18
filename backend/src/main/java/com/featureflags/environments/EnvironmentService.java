package com.featureflags.environments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.flags.Flag;
import com.featureflags.flags.FlagConfigurationRepository;
import com.featureflags.flags.FlagRepository;
import com.featureflags.segments.SegmentRepository;
import com.featureflags.shared.ApiException;

@Service
public class EnvironmentService {

	private static final int NOT_FOUND = 404;

	private final EnvironmentRepository environmentRepository;

	private final FlagRepository flagRepository;

	private final FlagConfigurationRepository flagConfigurationRepository;

	private final SegmentRepository segmentRepository;

	public EnvironmentService(EnvironmentRepository environmentRepository, FlagRepository flagRepository,
			FlagConfigurationRepository flagConfigurationRepository, SegmentRepository segmentRepository) {
		this.environmentRepository = environmentRepository;
		this.flagRepository = flagRepository;
		this.flagConfigurationRepository = flagConfigurationRepository;
		this.segmentRepository = segmentRepository;
	}

	public List<Environment> list() {
		return environmentRepository.list();
	}

	public Environment requireByKey(String key) {
		Environment environment = key == null ? null : environmentRepository.findByKey(key);

		if (environment == null) {
			throw new ApiException(NOT_FOUND, "ENVIRONMENT_NOT_FOUND",
					"The requested environment does not exist.");
		}

		return environment;
	}

	public Map<String, Object> summary(String key) {
		Environment environment = requireByKey(key);
		String environmentId = environment.getId();
		long configured = flagConfigurationRepository.countByEnvironment(environmentId);
		long enabled = flagConfigurationRepository.countEnabledByEnvironment(environmentId);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("environment", environment.toMap());
		body.put("flagCount", configured);
		body.put("enabledCount", enabled);
		body.put("disabledCount", configured - enabled);
		body.put("rollingOutCount", flagConfigurationRepository.countRollingOutByEnvironment(environmentId));
		body.put("targetedCount", flagConfigurationRepository.countTargetedByEnvironment(environmentId));
		body.put("segmentCount", segmentRepository.countByEnvironment(environmentId));
		body.put("retiredCount", flagRepository.countByStatus(Flag.RETIRED));

		return body;
	}
}
