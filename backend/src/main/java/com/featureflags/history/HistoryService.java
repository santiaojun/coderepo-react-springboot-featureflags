package com.featureflags.history;

import java.util.List;

import org.springframework.stereotype.Service;

import com.featureflags.environments.EnvironmentService;

@Service
public class HistoryService {

	private final ChangeRecordRepository changeRecordRepository;

	private final EnvironmentService environmentService;

	public HistoryService(ChangeRecordRepository changeRecordRepository, EnvironmentService environmentService) {
		this.changeRecordRepository = changeRecordRepository;
		this.environmentService = environmentService;
	}

	public List<ChangeRecord> list(HistoryValidator.ListQuery query) {
		String environmentId = query.environmentKey() == null ? null
				: environmentService.requireByKey(query.environmentKey()).getId();

		return changeRecordRepository.list(query.flagKey(), environmentId, query.actorId(), query.action(),
				query.limit());
	}

	public ChangeRecord findVersion(String flagId, String environmentId, int version) {
		return changeRecordRepository.findVersion(flagId, environmentId, version);
	}
}
