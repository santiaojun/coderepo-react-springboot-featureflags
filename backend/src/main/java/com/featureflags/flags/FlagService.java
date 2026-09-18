package com.featureflags.flags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.environments.Environment;
import com.featureflags.environments.EnvironmentService;
import com.featureflags.history.ChangeEntry;
import com.featureflags.history.ChangeRecord;
import com.featureflags.history.ChangeRecorder;
import com.featureflags.segments.Segment;
import com.featureflags.segments.SegmentRepository;
import com.featureflags.shared.ApiException;

@Service
public class FlagService {

	private static final int NOT_FOUND = 404;

	private static final int CONFLICT = 409;

	private static final String DEVELOPMENT_KEY = "development";

	private final FlagRepository flagRepository;

	private final FlagConfigurationRepository flagConfigurationRepository;

	private final FlagConfigurationService flagConfigurationService;

	private final EnvironmentService environmentService;

	private final SegmentRepository segmentRepository;

	private final ChangeRecorder changeRecorder;

	public FlagService(FlagRepository flagRepository, FlagConfigurationRepository flagConfigurationRepository,
			FlagConfigurationService flagConfigurationService, EnvironmentService environmentService,
			SegmentRepository segmentRepository, ChangeRecorder changeRecorder) {
		this.flagRepository = flagRepository;
		this.flagConfigurationRepository = flagConfigurationRepository;
		this.flagConfigurationService = flagConfigurationService;
		this.environmentService = environmentService;
		this.segmentRepository = segmentRepository;
		this.changeRecorder = changeRecorder;
	}

	public Map<String, Object> list(FlagValidator.ListQuery query) {
		Environment environment = environmentService.requireByKey(query.environmentKey());
		List<Flag> flags = flagRepository.list(query.search(), query.tag(), query.status());
		Map<String, FlagConfiguration> byFlagId = configurationsByFlagId(environment);
		List<Map<String, Object>> items = new ArrayList<>();

		for (Flag flag : flags) {
			FlagConfiguration configuration = byFlagId.get(flag.getId());

			if (configuration == null || !matchesState(configuration, query.state())) {
				continue;
			}

			Map<String, Object> item = flag.toMap();
			item.put("configuration", configuration.toMap());
			items.add(item);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("flags", items);
		body.put("tags", flagRepository.listTags());

		return body;
	}

	public Map<String, Object> getByKey(String flagKey, String environmentKey) {
		Flag flag = requireFlag(flagKey);
		Environment environment = environmentService.requireByKey(environmentKey);
		FlagConfiguration configuration = flagConfigurationService.require(flag, environment);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("flag", flag.toMap());
		body.put("configuration", configuration.toMap());
		body.put("segments", referencedSegments(configuration));
		body.put("environments", environmentStates(flag));

		return body;
	}

	public Map<String, Object> create(FlagValidator.CreateInput input, ChangeRecorder.Actor actor) {
		if (flagRepository.existsByKey(input.key())) {
			throw new ApiException(CONFLICT, "FLAG_KEY_CONFLICT", "A flag with this key already exists.",
					Map.of("key", input.key()));
		}

		List<Environment> environments = environmentService.list();

		if (environments.isEmpty()) {
			throw new ApiException(CONFLICT, "NO_ENVIRONMENTS",
					"Create an environment before adding flags.");
		}

		Flag flag = new Flag();
		flag.setKey(input.key());
		flag.setName(input.name());
		flag.setDescription(input.description());
		flag.setKind(input.kind());
		flag.setTags(new ArrayList<>(input.tags()));
		flag.setTemporary(input.temporary());
		flag.setStatus(Flag.ACTIVE);
		flag.setMaintainerId(input.maintainerId() == null ? actor.id() : input.maintainerId());
		flag.setVariations(buildVariations(input.variations()));

		Flag saved = flagRepository.save(flag);
		String onVariationId = saved.getVariations().get(input.defaultOnIndex()).getId();
		String offVariationId = saved.getVariations().get(input.defaultOffIndex()).getId();

		for (Environment environment : environments) {
			boolean enabled = DEVELOPMENT_KEY.equals(environment.getKey());
			flagConfigurationService.create(saved, environment, enabled, onVariationId, offVariationId, actor);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("flag", saved.toMap());
		body.put("environments", environmentStates(saved));

		return body;
	}

	public Map<String, Object> update(String flagKey, Map<String, Object> updates, ChangeRecorder.Actor actor) {
		Flag flag = requireFlag(flagKey);
		List<ChangeEntry> entries = new ArrayList<>();

		if (updates.containsKey("name")) {
			String name = (String) updates.get("name");
			entries.add(ChangeEntry.of("name", "Name", flag.getName(), name));
			flag.setName(name);
		}

		if (updates.containsKey("description")) {
			String description = (String) updates.get("description");
			entries.add(ChangeEntry.of("description", "Description", flag.getDescription(), description));
			flag.setDescription(description);
		}

		if (updates.containsKey("tags")) {
			@SuppressWarnings("unchecked")
			List<String> tags = (List<String>) updates.get("tags");
			entries.add(ChangeEntry.of("tags", "Tags", new ArrayList<>(flag.getTags()), new ArrayList<>(tags)));
			flag.setTags(new ArrayList<>(tags));
		}

		if (updates.containsKey("temporary")) {
			boolean temporary = (Boolean) updates.get("temporary");
			entries.add(ChangeEntry.of("temporary", "Lifetime", lifetime(flag.isTemporary()), lifetime(temporary)));
			flag.setTemporary(temporary);
		}

		if (updates.containsKey("maintainerId")) {
			String maintainerId = (String) updates.get("maintainerId");
			entries.add(ChangeEntry.of("maintainerId", "Maintainer", flag.getMaintainerId(), maintainerId));
			flag.setMaintainerId(maintainerId);
		}

		if (updates.containsKey("variationNames")) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> renames = (List<Map<String, Object>>) updates.get("variationNames");
			entries.addAll(applyVariationNames(flag, renames));
		}

		if (entries.isEmpty()) {
			return Map.of("flag", flag.toMap());
		}

		Flag saved = flagRepository.save(flag);
		ChangeRecord change = recordFlagChange(saved, actor, ChangeRecord.FLAG_UPDATED, "Flag details updated",
				entries);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("flag", saved.toMap());
		body.put("change", change.toMap());

		return body;
	}

	public Map<String, Object> retire(String flagKey, ChangeRecorder.Actor actor) {
		Flag flag = requireFlag(flagKey);

		if (flag.isRetired()) {
			throw new ApiException(CONFLICT, "FLAG_ALREADY_RETIRED", "This flag is already retired.");
		}

		flag.setStatus(Flag.RETIRED);
		Flag saved = flagRepository.save(flag);
		ChangeRecord change = recordFlagChange(saved, actor, ChangeRecord.FLAG_RETIRED, "Flag retired",
				List.of(ChangeEntry.of("status", "Status", Flag.ACTIVE, Flag.RETIRED)));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("flag", saved.toMap());
		body.put("change", change.toMap());

		return body;
	}

	public Map<String, Object> restore(String flagKey, ChangeRecorder.Actor actor) {
		Flag flag = requireFlag(flagKey);

		if (!flag.isRetired()) {
			throw new ApiException(CONFLICT, "FLAG_NOT_RETIRED", "This flag is already active.");
		}

		flag.setStatus(Flag.ACTIVE);
		Flag saved = flagRepository.save(flag);
		ChangeRecord change = recordFlagChange(saved, actor, ChangeRecord.FLAG_RESTORED, "Flag restored to active",
				List.of(ChangeEntry.of("status", "Status", Flag.RETIRED, Flag.ACTIVE)));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("flag", saved.toMap());
		body.put("change", change.toMap());

		return body;
	}

	public void remove(String flagKey) {
		Flag flag = requireFlag(flagKey);

		if (!flag.isRetired()) {
			throw new ApiException(CONFLICT, "FLAG_NOT_RETIRED",
					"Retire this flag before deleting it permanently.");
		}

		flagConfigurationRepository.removeByFlagId(flag.getId());
		flagRepository.remove(flag.getId());
	}

	public Map<String, Object> toggle(String flagKey, String environmentKey, FlagValidator.ToggleInput input,
			ChangeRecorder.Actor actor) {
		Flag flag = requireActiveFlag(flagKey);
		Environment environment = environmentService.requireByKey(environmentKey);
		FlagConfiguration configuration = flagConfigurationService.require(flag, environment);
		flagConfigurationService.ensureVersion(configuration, input.version());

		if (configuration.isEnabled() == input.enabled()) {
			return flagConfigurationService.present(flag, configuration, null);
		}

		boolean previous = configuration.isEnabled();
		configuration.setEnabled(input.enabled());

		String serving = input.enabled() ? FlagSummaries.describeServe(flag, configuration.getFallthrough())
				: FlagSummaries.variationName(flag, configuration.getOffVariationId());
		String summary = "Turned " + (input.enabled() ? "on" : "off") + " in " + environment.getName()
				+ ", serving " + serving;
		List<ChangeEntry> entries = List.of(ChangeEntry.of("enabled", "Flag state", label(previous),
				label(input.enabled())));

		FlagConfigurationService.Commit commit = flagConfigurationService.commit(flag, environment, configuration,
				actor, ChangeRecord.TOGGLED, summary, entries, input.comment(), null);

		return flagConfigurationService.present(flag, commit.configuration(), commit.change());
	}

	public Flag requireFlag(String flagKey) {
		Flag flag = flagKey == null ? null : flagRepository.findByKey(flagKey);

		if (flag == null) {
			throw new ApiException(NOT_FOUND, "FLAG_NOT_FOUND", "The requested flag does not exist.");
		}

		return flag;
	}

	public Flag requireActiveFlag(String flagKey) {
		Flag flag = requireFlag(flagKey);

		if (flag.isRetired()) {
			throw new ApiException(CONFLICT, "FLAG_RETIRED",
					"Restore this flag before changing its configuration.");
		}

		return flag;
	}

	private ChangeRecord recordFlagChange(Flag flag, ChangeRecorder.Actor actor, String action, String summary,
			List<ChangeEntry> entries) {
		ChangeRecorder.Subject subject = new ChangeRecorder.Subject(flag.getId(), flag.getKey(), flag.getName(), null,
				null, 0);

		return changeRecorder.record(subject, actor,
				ChangeRecorder.Change.of(action, summary, entries, null, ""));
	}

	private List<ChangeEntry> applyVariationNames(Flag flag, List<Map<String, Object>> renames) {
		List<ChangeEntry> entries = new ArrayList<>();

		for (Map<String, Object> rename : renames) {
			String id = (String) rename.get("id");
			Variation variation = flag.findVariation(id);

			if (variation == null) {
				throw new ApiException(NOT_FOUND, "VARIATION_NOT_FOUND",
						"This flag has no variation " + id + ".");
			}

			if (rename.containsKey("name")) {
				String name = (String) rename.get("name");

				if (!name.equals(variation.getName())) {
					entries.add(ChangeEntry.of("variations." + id + ".name", "Variation name", variation.getName(),
							name));
					variation.setName(name);
				}
			}

			if (rename.containsKey("description")) {
				String description = (String) rename.get("description");

				if (!description.equals(variation.getDescription())) {
					entries.add(ChangeEntry.of("variations." + id + ".description", "Variation description",
							variation.getDescription(), description));
					variation.setDescription(description);
				}
			}
		}

		return entries;
	}

	private List<Variation> buildVariations(List<FlagValidator.VariationInput> inputs) {
		List<Variation> variations = new ArrayList<>();

		for (int index = 0; index < inputs.size(); index += 1) {
			FlagValidator.VariationInput item = inputs.get(index);
			Variation variation = new Variation();
			variation.setId("v" + (index + 1));
			variation.setName(item.name());
			variation.setValue(item.value());
			variation.setDescription(item.description());
			variations.add(variation);
		}

		return variations;
	}

	private Map<String, FlagConfiguration> configurationsByFlagId(Environment environment) {
		Map<String, FlagConfiguration> byFlagId = new LinkedHashMap<>();

		for (FlagConfiguration configuration : flagConfigurationRepository.listByEnvironment(environment.getId())) {
			byFlagId.put(configuration.getFlagId(), configuration);
		}

		return byFlagId;
	}

	private boolean matchesState(FlagConfiguration configuration, String state) {
		return switch (state) {
			case "on" -> configuration.isEnabled();
			case "off" -> !configuration.isEnabled();
			case "rollout" -> configuration.isEnabled() && configuration.getFallthrough().isRollout();
			case "targeted" -> configuration.isEnabled()
					&& (!configuration.getRules().isEmpty() || configuration.targetedUserCount() > 0);
			default -> true;
		};
	}

	private List<Map<String, Object>> referencedSegments(FlagConfiguration configuration) {
		List<String> segmentIds = configuration.getRules().stream().flatMap(rule -> rule.getSegmentIds().stream())
				.distinct().toList();

		if (segmentIds.isEmpty()) {
			return List.of();
		}

		return segmentRepository.findByIds(segmentIds).stream().map(Segment::toSummaryMap).toList();
	}

	private List<Map<String, Object>> environmentStates(Flag flag) {
		Map<String, FlagConfiguration> byEnvironmentId = new LinkedHashMap<>();

		for (FlagConfiguration configuration : flagConfigurationRepository.listByFlagId(flag.getId())) {
			byEnvironmentId.put(configuration.getEnvironmentId(), configuration);
		}

		List<Map<String, Object>> states = new ArrayList<>();

		for (Environment environment : environmentService.list()) {
			FlagConfiguration configuration = byEnvironmentId.get(environment.getId());
			Map<String, Object> state = new LinkedHashMap<>();
			state.put("key", environment.getKey());
			state.put("name", environment.getName());
			state.put("color", environment.getColor());
			state.put("critical", environment.isCritical());
			state.put("enabled", configuration != null && configuration.isEnabled());
			state.put("version", configuration == null ? 0 : configuration.getVersion());
			state.put("serving", configuration == null ? null
					: configuration.isEnabled()
							? FlagSummaries.describeServe(flag, configuration.getFallthrough())
							: FlagSummaries.variationName(flag, configuration.getOffVariationId()));
			states.add(state);
		}

		return states;
	}

	private String label(boolean value) {
		return value ? "on" : "off";
	}

	private String lifetime(boolean temporary) {
		return temporary ? "temporary" : "permanent";
	}
}
