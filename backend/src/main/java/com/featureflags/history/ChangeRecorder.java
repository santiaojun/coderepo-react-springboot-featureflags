package com.featureflags.history;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Writes the append-only audit trail. Every flag and configuration mutation
 * records the change entries that produced a version plus the restorable
 * snapshot of that version.
 */
@Service
public class ChangeRecorder {

	public record Subject(String flagId, String flagKey, String flagName, String environmentId,
			String environmentKey, int version) {
	}

	public record Actor(String id, String name) {
	}

	public record Change(String action, String summary, List<ChangeEntry> entries, Map<String, Object> snapshot,
			String comment, Integer restoredFromVersion) {

		public static Change of(String action, String summary, List<ChangeEntry> entries,
				Map<String, Object> snapshot, String comment) {
			return new Change(action, summary, entries, snapshot, comment, null);
		}
	}

	private final ChangeRecordRepository changeRecordRepository;

	public ChangeRecorder(ChangeRecordRepository changeRecordRepository) {
		this.changeRecordRepository = changeRecordRepository;
	}

	public ChangeRecord record(Subject subject, Actor actor, Change change) {
		ChangeRecord record = new ChangeRecord();
		record.setFlagId(subject.flagId());
		record.setFlagKey(subject.flagKey());
		record.setFlagName(subject.flagName());
		record.setEnvironmentId(subject.environmentId());
		record.setEnvironmentKey(subject.environmentKey());
		record.setVersion(subject.version());
		record.setAction(change.action());
		record.setSummary(change.summary());
		record.setChanges(change.entries() == null ? new ArrayList<>() : new ArrayList<>(change.entries()));
		record.setSnapshot(change.snapshot() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(change.snapshot()));
		record.setRestoredFromVersion(change.restoredFromVersion());
		record.setComment(change.comment() == null ? "" : change.comment());
		record.setActorId(actor.id());
		record.setActorName(actor.name());
		record.setCreatedAt(Instant.now());

		return changeRecordRepository.save(record);
	}
}
