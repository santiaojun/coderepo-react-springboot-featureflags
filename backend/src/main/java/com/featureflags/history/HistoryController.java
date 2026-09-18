package com.featureflags.history;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/change-history")
public class HistoryController {

	private final HistoryService historyService;

	public HistoryController(HistoryService historyService) {
		this.historyService = historyService;
	}

	@GetMapping
	public Map<String, Object> listChanges(@RequestParam Map<String, String> parameters) {
		List<ChangeRecord> records = historyService.list(HistoryValidator.validateList(parameters));

		return Map.of("data", records.stream().map(ChangeRecord::toMap).toList());
	}
}
