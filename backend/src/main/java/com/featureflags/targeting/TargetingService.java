package com.featureflags.targeting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.featureflags.audience.AudienceRepository;
import com.featureflags.audience.EndUser;
import com.featureflags.environments.Environment;
import com.featureflags.environments.EnvironmentService;
import com.featureflags.flags.Flag;
import com.featureflags.flags.FlagConfiguration;
import com.featureflags.flags.FlagConfigurationService;
import com.featureflags.flags.FlagService;
import com.featureflags.flags.FlagSnapshots;
import com.featureflags.flags.FlagSummaries;
import com.featureflags.flags.FlagTarget;
import com.featureflags.flags.RolloutWeight;
import com.featureflags.flags.Serve;
import com.featureflags.flags.TargetRule;
import com.featureflags.flags.Variation;
import com.featureflags.history.ChangeEntry;
import com.featureflags.history.ChangeRecord;
import com.featureflags.history.ChangeRecorder;
import com.featureflags.history.HistoryService;
import com.featureflags.segments.Segment;
import com.featureflags.segments.SegmentRepository;
import com.featureflags.shared.ApiException;
import com.featureflags.shared.FieldErrors;

@Service
public class TargetingService {

	private static final int NOT_FOUND = 404;

	private static final int CONFLICT = 409;

	private final FlagService flagService;

	private final FlagConfigurationService flagConfigurationService;

	private final EnvironmentService environmentService;

	private final SegmentRepository segmentRepository;

	private final AudienceRepository audienceRepository;

	private final HistoryService historyService;

	public TargetingService(FlagService flagService, FlagConfigurationService flagConfigurationService,
			EnvironmentService environmentService, SegmentRepository segmentRepository,
			AudienceRepository audienceRepository, HistoryService historyService) {
		this.flagService = flagService;
		this.flagConfigurationService = flagConfigurationService;
		this.environmentService = environmentService;
		this.segmentRepository = segmentRepository;
		this.audienceRepository = audienceRepository;
		this.historyService = historyService;
	}

	public Map<String, Object> updateTargeting(String flagKey, String environmentKey,
			TargetingValidator.TargetingInput input, ChangeRecorder.Actor actor) {
		Flag flag = flagService.requireActiveFlag(flagKey);
		Environment environment = environmentService.requireByKey(environmentKey);
		FlagConfiguration configuration = flagConfigurationService.require(flag, environment);
		flagConfigurationService.ensureVersion(configuration, input.version());

		Map<String, Segment> segmentsById = segmentsById(environment.getId());
		verifyReferences(flag, environment, input, segmentsById);

		List<ChangeEntry> entries = describeTargetingChanges(flag, configuration, input);

		if (entries.isEmpty()) {
			return flagConfigurationService.present(flag, configuration, null);
		}

		configuration.setOffVariationId(input.offVariationId());
		configuration.setTargets(new ArrayList<>(input.targets()));
		configuration.setRules(new ArrayList<>(input.rules()));
		configuration.setFallthrough(input.fallthrough());

		FlagConfigurationService.Commit commit = flagConfigurationService.commit(flag, environment, configuration,
				actor, ChangeRecord.TARGETING_UPDATED, "Targeting updated in " + environment.getName(), entries,
				input.comment(), null);

		return flagConfigurationService.present(flag, commit.configuration(), commit.change());
	}

	public Map<String, Object> evaluate(String flagKey, String environmentKey,
			TargetingValidator.EvaluateInput input) {
		Flag flag = flagService.requireFlag(flagKey);
		Environment environment = environmentService.requireByKey(environmentKey);
		FlagConfiguration configuration = flagConfigurationService.require(flag, environment);
		EndUser user = audienceRepository.findByKey(input.userKey());

		if (user == null) {
			throw new ApiException(NOT_FOUND, "AUDIENCE_USER_NOT_FOUND",
					"No audience member has the key " + input.userKey() + ".");
		}

		EvaluationResult result = FlagEvaluator.evaluate(flag, configuration,
				new EvaluationUser(user.getKey(), user.toEvaluationAttributes()), segmentsById(environment.getId()));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("user", user.toMap());
		body.put("evaluation", result.toMap());

		return body;
	}

	public Map<String, Object> restore(String flagKey, String environmentKey,
			TargetingValidator.RestoreInput input, ChangeRecorder.Actor actor) {
		Flag flag = flagService.requireActiveFlag(flagKey);
		Environment environment = environmentService.requireByKey(environmentKey);
		FlagConfiguration configuration = flagConfigurationService.require(flag, environment);

		if (input.version() == configuration.getVersion()) {
			throw new ApiException(CONFLICT, "ALREADY_CURRENT_VERSION",
					"Version " + input.version() + " is already the current configuration.");
		}

		ChangeRecord record = historyService.findVersion(flag.getId(), environment.getId(), input.version());

		if (record == null || record.getSnapshot().isEmpty()) {
			throw new ApiException(NOT_FOUND, "VERSION_NOT_FOUND",
					"This flag has no restorable version " + input.version() + " in " + environment.getName() + ".");
		}

		Set<String> missing = new LinkedHashSet<>();

		for (String variationId : FlagSnapshots.referencedVariationIds(record.getSnapshot())) {
			if (!flag.hasVariation(variationId)) {
				missing.add(variationId);
			}
		}

		if (!missing.isEmpty()) {
			throw new ApiException(CONFLICT, "INCOMPATIBLE_VERSION",
					"Version " + input.version() + " refers to variations this flag no longer has.",
					Map.of("missingVariationIds", new ArrayList<>(missing)));
		}

		FlagConfiguration previous = copyOf(configuration);
		FlagSnapshots.apply(configuration, record.getSnapshot());

		List<ChangeEntry> entries = describeRestoredChanges(flag, previous, configuration);
		String summary = "Rolled back to version " + input.version() + " in " + environment.getName();

		FlagConfigurationService.Commit commit = flagConfigurationService.commit(flag, environment, configuration,
				actor, ChangeRecord.ROLLED_BACK, summary, entries, input.comment(), input.version());

		return flagConfigurationService.present(flag, commit.configuration(), commit.change());
	}

	public Map<String, Object> distributionFor(String flagKey, String environmentKey) {
		Flag flag = flagService.requireFlag(flagKey);
		Environment environment = environmentService.requireByKey(environmentKey);
		FlagConfiguration configuration = flagConfigurationService.require(flag, environment);

		return distribution(flag, configuration, environment.getId());
	}

	/**
	 * Evaluates the whole seeded audience against a configuration so the rollout
	 * screen can show the split that members actually receive rather than the
	 * percentages that were requested.
	 */
	public Map<String, Object> distribution(Flag flag, FlagConfiguration configuration, String environmentId) {
		Map<String, Segment> segmentsById = segmentsById(environmentId);
		List<EndUser> users = audienceRepository.listAll();
		Map<String, Integer> countsByVariation = new LinkedHashMap<>();
		Map<String, Integer> countsByReason = new LinkedHashMap<>();

		for (Variation variation : flag.getVariations()) {
			countsByVariation.put(variation.getId(), 0);
		}

		for (EndUser user : users) {
			EvaluationResult result = FlagEvaluator.evaluate(flag, configuration,
					new EvaluationUser(user.getKey(), user.toEvaluationAttributes()), segmentsById);
			countsByVariation.merge(result.variationId(), 1, Integer::sum);
			countsByReason.merge(result.reasonKind(), 1, Integer::sum);
		}

		List<Map<String, Object>> variations = new ArrayList<>();

		for (Variation variation : flag.getVariations()) {
			int count = countsByVariation.getOrDefault(variation.getId(), 0);
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("variationId", variation.getId());
			entry.put("variationName", variation.getName());
			entry.put("count", count);
			entry.put("percentage", users.isEmpty() ? 0.0
					: Math.round(count * 1000.0 / users.size()) / 10.0);
			variations.add(entry);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("audienceSize", users.size());
		body.put("variations", variations);
		body.put("reasons", countsByReason);

		return body;
	}

	public Map<String, Segment> segmentsById(String environmentId) {
		Map<String, Segment> segmentsById = new LinkedHashMap<>();

		for (Segment segment : segmentRepository.listByEnvironment(environmentId)) {
			segmentsById.put(segment.getId(), segment);
		}

		return segmentsById;
	}

	private void verifyReferences(Flag flag, Environment environment, TargetingValidator.TargetingInput input,
			Map<String, Segment> segmentsById) {
		FieldErrors errors = new FieldErrors();

		if (!flag.hasVariation(input.offVariationId())) {
			errors.add("offVariationId", "Select a variation this flag serves.");
		}

		verifyServe(flag, input.fallthrough(), "fallthrough", errors);

		Set<String> requestedUserKeys = new LinkedHashSet<>();

		for (int index = 0; index < input.targets().size(); index += 1) {
			FlagTarget target = input.targets().get(index);

			if (!flag.hasVariation(target.getVariationId())) {
				errors.add("targets." + index + ".variationId", "Select a variation this flag serves.");
			}

			requestedUserKeys.addAll(target.getUserKeys());
		}

		verifyUserKeys(requestedUserKeys, input.targets(), errors);

		for (int index = 0; index < input.rules().size(); index += 1) {
			TargetRule rule = input.rules().get(index);
			String field = "rules." + index;
			verifyServe(flag, rule.getServe(), field + ".serve", errors);

			for (String segmentId : rule.getSegmentIds()) {
				if (!segmentsById.containsKey(segmentId)) {
					errors.add(field + ".segmentIds",
							"Select a segment that exists in " + environment.getName() + ".");

					break;
				}
			}
		}

		errors.throwIfAny();
	}

	private void verifyUserKeys(Set<String> requestedUserKeys, List<FlagTarget> targets, FieldErrors errors) {
		if (requestedUserKeys.isEmpty()) {
			return;
		}

		Set<String> known = new LinkedHashSet<>();

		for (EndUser user : audienceRepository.findByKeys(new ArrayList<>(requestedUserKeys))) {
			known.add(user.getKey());
		}

		for (int index = 0; index < targets.size(); index += 1) {
			for (String key : targets.get(index).getUserKeys()) {
				if (!known.contains(key)) {
					errors.add("targets." + index + ".userKeys",
							"No audience member has the key " + key + ".");

					break;
				}
			}
		}
	}

	private void verifyServe(Flag flag, Serve serve, String field, FieldErrors errors) {
		if (serve == null) {
			errors.add(field, "Choose what this serves.");

			return;
		}

		if (!Serve.ROLLOUT.equals(serve.getKind())) {
			if (!flag.hasVariation(serve.getVariationId())) {
				errors.add(field + ".variationId", "Select a variation this flag serves.");
			}

			return;
		}

		if (serve.getRollout() == null || serve.getRollout().getWeights().isEmpty()) {
			errors.add(field + ".rollout", "Set a percentage for each variation.");

			return;
		}

		for (RolloutWeight weight : serve.getRollout().getWeights()) {
			if (!flag.hasVariation(weight.getVariationId())) {
				errors.add(field + ".rollout.weights", "Select a variation this flag serves.");

				return;
			}
		}
	}

	private List<ChangeEntry> describeTargetingChanges(Flag flag, FlagConfiguration current,
			TargetingValidator.TargetingInput input) {
		List<ChangeEntry> entries = new ArrayList<>();

		if (!input.offVariationId().equals(current.getOffVariationId())) {
			entries.add(ChangeEntry.of("offVariationId", "Serves when off",
					FlagSummaries.variationName(flag, current.getOffVariationId()),
					FlagSummaries.variationName(flag, input.offVariationId())));
		}

		String currentTargets = FlagSummaries.describeTargets(flag, current.getTargets());
		String nextTargets = FlagSummaries.describeTargets(flag, input.targets());

		if (!currentTargets.equals(nextTargets)) {
			entries.add(ChangeEntry.of("targets", "Individual targets", currentTargets, nextTargets));
		}

		List<String> currentRules = ruleLabels(flag, current.getRules());
		List<String> nextRules = ruleLabels(flag, input.rules());

		if (!currentRules.equals(nextRules)) {
			entries.add(ChangeEntry.of("rules", "Targeting rules", currentRules, nextRules));
		}

		String currentFallthrough = FlagSummaries.describeServe(flag, current.getFallthrough());
		String nextFallthrough = FlagSummaries.describeServe(flag, input.fallthrough());

		if (!currentFallthrough.equals(nextFallthrough)) {
			entries.add(ChangeEntry.of("fallthrough", "Serves everyone else", currentFallthrough, nextFallthrough));
		}

		return entries;
	}

	private List<ChangeEntry> describeRestoredChanges(Flag flag, FlagConfiguration previous,
			FlagConfiguration restored) {
		List<ChangeEntry> entries = new ArrayList<>();

		if (previous.isEnabled() != restored.isEnabled()) {
			entries.add(ChangeEntry.of("enabled", "Flag state", previous.isEnabled() ? "on" : "off",
					restored.isEnabled() ? "on" : "off"));
		}

		if (!String.valueOf(previous.getOffVariationId()).equals(String.valueOf(restored.getOffVariationId()))) {
			entries.add(ChangeEntry.of("offVariationId", "Serves when off",
					FlagSummaries.variationName(flag, previous.getOffVariationId()),
					FlagSummaries.variationName(flag, restored.getOffVariationId())));
		}

		String previousTargets = FlagSummaries.describeTargets(flag, previous.getTargets());
		String restoredTargets = FlagSummaries.describeTargets(flag, restored.getTargets());

		if (!previousTargets.equals(restoredTargets)) {
			entries.add(ChangeEntry.of("targets", "Individual targets", previousTargets, restoredTargets));
		}

		List<String> previousRules = ruleLabels(flag, previous.getRules());
		List<String> restoredRules = ruleLabels(flag, restored.getRules());

		if (!previousRules.equals(restoredRules)) {
			entries.add(ChangeEntry.of("rules", "Targeting rules", previousRules, restoredRules));
		}

		String previousFallthrough = FlagSummaries.describeServe(flag, previous.getFallthrough());
		String restoredFallthrough = FlagSummaries.describeServe(flag, restored.getFallthrough());

		if (!previousFallthrough.equals(restoredFallthrough)) {
			entries.add(ChangeEntry.of("fallthrough", "Serves everyone else", previousFallthrough,
					restoredFallthrough));
		}

		return entries;
	}

	private List<String> ruleLabels(Flag flag, List<TargetRule> rules) {
		List<String> labels = new ArrayList<>();

		for (TargetRule rule : rules) {
			String label = rule.getDescription().isEmpty() ? "Rule " + rule.getId() : rule.getDescription();
			labels.add(label + " serves " + FlagSummaries.describeServe(flag, rule.getServe()));
		}

		return labels;
	}

	private FlagConfiguration copyOf(FlagConfiguration configuration) {
		FlagConfiguration copy = new FlagConfiguration();
		copy.setEnabled(configuration.isEnabled());
		copy.setOffVariationId(configuration.getOffVariationId());
		copy.setTargets(new ArrayList<>(configuration.getTargets()));
		copy.setRules(new ArrayList<>(configuration.getRules()));
		copy.setFallthrough(configuration.getFallthrough());

		return copy;
	}
}
