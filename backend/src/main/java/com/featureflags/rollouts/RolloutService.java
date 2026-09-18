package com.featureflags.rollouts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.environments.Environment;
import com.featureflags.environments.EnvironmentService;
import com.featureflags.flags.Flag;
import com.featureflags.flags.FlagConfiguration;
import com.featureflags.flags.FlagConfigurationService;
import com.featureflags.flags.FlagService;
import com.featureflags.flags.FlagSummaries;
import com.featureflags.flags.Rollout;
import com.featureflags.flags.RolloutWeight;
import com.featureflags.flags.Serve;
import com.featureflags.history.ChangeEntry;
import com.featureflags.history.ChangeRecord;
import com.featureflags.history.ChangeRecorder;
import com.featureflags.shared.FieldErrors;
import com.featureflags.targeting.TargetingService;

/**
 * Progressive rollouts. A rollout replaces the fallthrough variation with a
 * weighted split, so raising the percentage step by step is an ordinary
 * configuration change with its own version and audit entry.
 */
@Service
public class RolloutService {

	private final FlagService flagService;

	private final FlagConfigurationService flagConfigurationService;

	private final EnvironmentService environmentService;

	private final TargetingService targetingService;

	public RolloutService(FlagService flagService, FlagConfigurationService flagConfigurationService,
			EnvironmentService environmentService, TargetingService targetingService) {
		this.flagService = flagService;
		this.flagConfigurationService = flagConfigurationService;
		this.environmentService = environmentService;
		this.targetingService = targetingService;
	}

	public Map<String, Object> update(String flagKey, String environmentKey,
			RolloutValidator.RolloutInput input, ChangeRecorder.Actor actor) {
		Flag flag = flagService.requireActiveFlag(flagKey);
		Environment environment = environmentService.requireByKey(environmentKey);
		FlagConfiguration configuration = flagConfigurationService.require(flag, environment);
		flagConfigurationService.ensureVersion(configuration, input.version());
		verifyWeights(flag, input.rollout());

		String previous = FlagSummaries.describeServe(flag, configuration.getFallthrough());
		String next = FlagSummaries.describeRollout(flag, input.rollout());

		if (previous.equals(next)) {
			Map<String, Object> unchanged = flagConfigurationService.present(flag, configuration, null);
			unchanged.put("distribution", targetingService.distribution(flag, configuration, environment.getId()));

			return unchanged;
		}

		configuration.setFallthrough(Serve.ofRollout(input.rollout()));
		List<ChangeEntry> entries = List.of(ChangeEntry.of("fallthrough.rollout", "Rollout", previous, next));

		FlagConfigurationService.Commit commit = flagConfigurationService.commit(flag, environment, configuration,
				actor, ChangeRecord.ROLLOUT_UPDATED, "Rollout set to " + next + " in " + environment.getName(),
				entries, input.comment(), null);

		Map<String, Object> body = flagConfigurationService.present(flag, commit.configuration(), commit.change());
		body.put("distribution", targetingService.distribution(flag, commit.configuration(), environment.getId()));

		return body;
	}

	/**
	 * Evaluates a candidate rollout without saving it, so an operator can see
	 * how many audience members a step would move before committing.
	 */
	public Map<String, Object> preview(String flagKey, String environmentKey,
			RolloutValidator.PreviewInput input) {
		Flag flag = flagService.requireFlag(flagKey);
		Environment environment = environmentService.requireByKey(environmentKey);
		FlagConfiguration configuration = flagConfigurationService.require(flag, environment);
		verifyWeights(flag, input.rollout());

		FlagConfiguration candidate = candidateOf(configuration, input.rollout());

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("summary", FlagSummaries.describeRollout(flag, input.rollout()));
		body.put("current", targetingService.distribution(flag, configuration, environment.getId()));
		body.put("projected", targetingService.distribution(flag, candidate, environment.getId()));

		return body;
	}

	private void verifyWeights(Flag flag, Rollout rollout) {
		FieldErrors errors = new FieldErrors();

		for (RolloutWeight weight : rollout.getWeights()) {
			if (!flag.hasVariation(weight.getVariationId())) {
				errors.add("rollout.weights", "Select a variation this flag serves.");

				break;
			}
		}

		errors.throwIfAny();
	}

	private FlagConfiguration candidateOf(FlagConfiguration configuration, Rollout rollout) {
		FlagConfiguration candidate = new FlagConfiguration();
		candidate.setFlagKey(configuration.getFlagKey());
		candidate.setEnvironmentKey(configuration.getEnvironmentKey());
		candidate.setEnabled(configuration.isEnabled());
		candidate.setOffVariationId(configuration.getOffVariationId());
		candidate.setTargets(new ArrayList<>(configuration.getTargets()));
		candidate.setRules(new ArrayList<>(configuration.getRules()));
		candidate.setFallthrough(Serve.ofRollout(rollout));

		return candidate;
	}
}
