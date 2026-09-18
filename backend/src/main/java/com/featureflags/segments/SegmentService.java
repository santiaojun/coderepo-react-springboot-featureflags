package com.featureflags.segments;

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
import com.featureflags.flags.FlagConfiguration;
import com.featureflags.flags.FlagConfigurationRepository;
import com.featureflags.history.ChangeRecorder;
import com.featureflags.shared.ApiException;
import com.featureflags.shared.FieldErrors;

@Service
public class SegmentService {

	private static final int NOT_FOUND = 404;

	private static final int CONFLICT = 409;

	private final SegmentRepository segmentRepository;

	private final EnvironmentService environmentService;

	private final AudienceRepository audienceRepository;

	private final FlagConfigurationRepository flagConfigurationRepository;

	public SegmentService(SegmentRepository segmentRepository, EnvironmentService environmentService,
			AudienceRepository audienceRepository, FlagConfigurationRepository flagConfigurationRepository) {
		this.segmentRepository = segmentRepository;
		this.environmentService = environmentService;
		this.audienceRepository = audienceRepository;
		this.flagConfigurationRepository = flagConfigurationRepository;
	}

	public Map<String, Object> list(String environmentKey) {
		Environment environment = environmentService.requireByKey(environmentKey);
		List<EndUser> users = audienceRepository.listAll();
		List<Map<String, Object>> items = new ArrayList<>();

		for (Segment segment : segmentRepository.listByEnvironment(environment.getId())) {
			Map<String, Object> item = segment.toMap();
			item.put("memberCount", matching(segment, users).size());
			item.put("usedByFlagKeys", usedByFlagKeys(segment.getId()));
			items.add(item);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("segments", items);
		body.put("audienceSize", users.size());

		return body;
	}

	public Map<String, Object> members(String segmentKey, String environmentKey) {
		Environment environment = environmentService.requireByKey(environmentKey);
		Segment segment = require(segmentKey, environment);
		List<EndUser> users = audienceRepository.listAll();
		List<Map<String, Object>> members = new ArrayList<>();

		for (EndUser user : matching(segment, users)) {
			Map<String, Object> entry = user.toMap();
			entry.put("included", segment.getIncludedUserKeys().contains(user.getKey()));
			members.add(entry);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("segment", segment.toSummaryMap());
		body.put("members", members);
		body.put("audienceSize", users.size());

		return body;
	}

	public Map<String, Object> create(String environmentKey, SegmentValidator.CreateInput input,
			ChangeRecorder.Actor actor) {
		Environment environment = environmentService.requireByKey(environmentKey);

		if (segmentRepository.findByKey(input.key(), environment.getId()) != null) {
			throw new ApiException(CONFLICT, "SEGMENT_KEY_CONFLICT",
					"A segment with this key already exists in " + environment.getName() + ".",
					Map.of("key", input.key()));
		}

		verifyUserKeys(input.includedUserKeys(), input.excludedUserKeys());

		Segment segment = new Segment();
		segment.setKey(input.key());
		segment.setName(input.name());
		segment.setDescription(input.description());
		segment.setEnvironmentId(environment.getId());
		segment.setEnvironmentKey(environment.getKey());
		segment.setIncludedUserKeys(new ArrayList<>(input.includedUserKeys()));
		segment.setExcludedUserKeys(new ArrayList<>(input.excludedUserKeys()));
		segment.setRules(new ArrayList<>(input.rules()));
		segment.setLastModifiedBy(actor.id());
		segment.setLastModifiedByName(actor.name());

		Segment saved = segmentRepository.save(segment);

		return present(saved);
	}

	public Map<String, Object> update(String segmentKey, String environmentKey, Map<String, Object> updates,
			ChangeRecorder.Actor actor) {
		Environment environment = environmentService.requireByKey(environmentKey);
		Segment segment = require(segmentKey, environment);

		if (updates.containsKey("name")) {
			segment.setName((String) updates.get("name"));
		}

		if (updates.containsKey("description")) {
			segment.setDescription((String) updates.get("description"));
		}

		if (updates.containsKey("includedUserKeys")) {
			@SuppressWarnings("unchecked")
			List<String> included = (List<String>) updates.get("includedUserKeys");
			segment.setIncludedUserKeys(new ArrayList<>(included));
		}

		if (updates.containsKey("excludedUserKeys")) {
			@SuppressWarnings("unchecked")
			List<String> excluded = (List<String>) updates.get("excludedUserKeys");
			segment.setExcludedUserKeys(new ArrayList<>(excluded));
		}

		if (updates.containsKey("rules")) {
			@SuppressWarnings("unchecked")
			List<SegmentRule> rules = (List<SegmentRule>) updates.get("rules");
			segment.setRules(new ArrayList<>(rules));
		}

		verifyUserKeys(segment.getIncludedUserKeys(), segment.getExcludedUserKeys());

		if (segment.getIncludedUserKeys().isEmpty() && segment.getExcludedUserKeys().isEmpty()
				&& segment.getRules().isEmpty()) {
			FieldErrors errors = new FieldErrors();
			errors.addFormError("Add a rule or at least one included member so this segment matches somebody.");
			errors.throwIfAny();
		}

		segment.setLastModifiedBy(actor.id());
		segment.setLastModifiedByName(actor.name());

		return present(segmentRepository.save(segment));
	}

	public void remove(String segmentKey, String environmentKey) {
		Environment environment = environmentService.requireByKey(environmentKey);
		Segment segment = require(segmentKey, environment);
		List<String> flagKeys = usedByFlagKeys(segment.getId());

		if (!flagKeys.isEmpty()) {
			throw new ApiException(CONFLICT, "SEGMENT_IN_USE",
					"Remove this segment from targeting rules before deleting it.",
					Map.of("flagKeys", flagKeys));
		}

		segmentRepository.remove(segment.getId());
	}

	private Segment require(String segmentKey, Environment environment) {
		Segment segment = segmentKey == null ? null : segmentRepository.findByKey(segmentKey, environment.getId());

		if (segment == null) {
			throw new ApiException(NOT_FOUND, "SEGMENT_NOT_FOUND",
					"This segment does not exist in " + environment.getName() + ".");
		}

		return segment;
	}

	private Map<String, Object> present(Segment segment) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("segment", segment.toMap());
		body.put("memberCount", matching(segment, audienceRepository.listAll()).size());
		body.put("usedByFlagKeys", usedByFlagKeys(segment.getId()));

		return body;
	}

	private List<EndUser> matching(Segment segment, List<EndUser> users) {
		List<EndUser> members = new ArrayList<>();

		for (EndUser user : users) {
			if (SegmentMatcher.matches(segment, user.getKey(), user.toEvaluationAttributes())) {
				members.add(user);
			}
		}

		return members;
	}

	private List<String> usedByFlagKeys(String segmentId) {
		Set<String> flagKeys = new LinkedHashSet<>();

		for (FlagConfiguration configuration : flagConfigurationRepository.listBySegmentId(segmentId)) {
			flagKeys.add(configuration.getFlagKey());
		}

		return new ArrayList<>(flagKeys);
	}

	private void verifyUserKeys(List<String> included, List<String> excluded) {
		Set<String> requested = new LinkedHashSet<>(included);
		requested.addAll(excluded);

		if (requested.isEmpty()) {
			return;
		}

		Set<String> known = new LinkedHashSet<>();

		for (EndUser user : audienceRepository.findByKeys(new ArrayList<>(requested))) {
			known.add(user.getKey());
		}

		FieldErrors errors = new FieldErrors();

		for (String key : included) {
			if (!known.contains(key)) {
				errors.add("includedUserKeys", "No audience member has the key " + key + ".");

				break;
			}
		}

		for (String key : excluded) {
			if (!known.contains(key)) {
				errors.add("excludedUserKeys", "No audience member has the key " + key + ".");

				break;
			}
		}

		errors.throwIfAny();
	}
}
