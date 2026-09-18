package com.featureflags.flags;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.environments.Environment;
import com.featureflags.history.ChangeEntry;
import com.featureflags.history.ChangeRecord;
import com.featureflags.history.ChangeRecorder;
import com.featureflags.shared.ApiException;

/**
 * Owns the single write path for per-environment flag configuration. Toggling,
 * targeting, rollouts, and rollbacks all commit through here so that version
 * numbers and the audit trail can never drift apart.
 */
@Service
public class FlagConfigurationService {

	private static final int NOT_FOUND = 404;

	private static final int CONFLICT = 409;

	public record Commit(FlagConfiguration configuration, ChangeRecord change) {
	}

	private final FlagConfigurationRepository flagConfigurationRepository;

	private final ChangeRecorder changeRecorder;

	public FlagConfigurationService(FlagConfigurationRepository flagConfigurationRepository,
			ChangeRecorder changeRecorder) {
		this.flagConfigurationRepository = flagConfigurationRepository;
		this.changeRecorder = changeRecorder;
	}

	public FlagConfiguration find(Flag flag, Environment environment) {
		return flagConfigurationRepository.findOne(flag.getId(), environment.getId());
	}

	public FlagConfiguration require(Flag flag, Environment environment) {
		FlagConfiguration configuration = find(flag, environment);

		if (configuration == null) {
			throw new ApiException(NOT_FOUND, "CONFIGURATION_NOT_FOUND",
					"This flag has no configuration in " + environment.getName() + ".");
		}

		return configuration;
	}

	public void ensureVersion(FlagConfiguration configuration, Integer expectedVersion) {
		if (expectedVersion != null && expectedVersion != configuration.getVersion()) {
			throw new ApiException(CONFLICT, "STALE_CONFIGURATION",
					"This flag changed while you were editing it. Reload to see the current configuration.",
					Map.of("currentVersion", configuration.getVersion(), "providedVersion", expectedVersion));
		}
	}

	public FlagConfiguration create(Flag flag, Environment environment, boolean enabled, String onVariationId,
			String offVariationId, ChangeRecorder.Actor actor) {
		FlagConfiguration configuration = new FlagConfiguration();
		configuration.setFlagId(flag.getId());
		configuration.setEnvironmentId(environment.getId());
		configuration.setFlagKey(flag.getKey());
		configuration.setEnvironmentKey(environment.getKey());
		configuration.setEnabled(enabled);
		configuration.setVersion(0);
		configuration.setOffVariationId(offVariationId);
		configuration.setFallthrough(Serve.ofVariation(onVariationId));

		return commit(flag, environment, configuration, actor, ChangeRecord.FLAG_CREATED,
				"Created serving " + FlagSummaries.variationName(flag, enabled ? onVariationId : offVariationId),
				List.of(), "", null).configuration();
	}

	public Commit commit(Flag flag, Environment environment, FlagConfiguration configuration,
			ChangeRecorder.Actor actor, String action, String summary, List<ChangeEntry> entries, String comment,
			Integer restoredFromVersion) {
		configuration.setVersion(configuration.getVersion() + 1);
		configuration.setLastModifiedAt(Instant.now());
		configuration.setLastModifiedBy(actor.id());
		configuration.setLastModifiedByName(actor.name());

		FlagConfiguration saved = flagConfigurationRepository.save(configuration);
		ChangeRecorder.Subject subject = new ChangeRecorder.Subject(flag.getId(), flag.getKey(), flag.getName(),
				environment.getId(), environment.getKey(), saved.getVersion());
		ChangeRecord change = changeRecorder.record(subject, actor,
				new ChangeRecorder.Change(action, summary, entries, saved.toSnapshot(), comment, restoredFromVersion));

		return new Commit(saved, change);
	}

	public Map<String, Object> present(Flag flag, FlagConfiguration configuration, ChangeRecord change) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("flag", flag.toMap());
		body.put("configuration", configuration.toMap());

		if (change != null) {
			body.put("change", change.toMap());
		}

		return body;
	}
}
