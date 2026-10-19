package com.dandragu.saasetl.execution.application;

import org.springframework.stereotype.Service;

import com.dandragu.saasetl.csv.application.RemoveColumnDownloadCommand;
import com.dandragu.saasetl.csv.application.RemoveColumnDownloadResult;
import com.dandragu.saasetl.csv.application.RemoveColumnDownloadService;
import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionType;

@Service
public class RemoveColumnExecutionService {

	private final RemoveColumnDownloadService downloadService;
	private final EtlExecutionHistoryService historyService;

	public RemoveColumnExecutionService(
			RemoveColumnDownloadService downloadService,
			EtlExecutionHistoryService historyService) {
		this.downloadService = downloadService;
		this.historyService = historyService;
	}

	public RemoveColumnDownloadResult download(RemoveColumnDownloadCommand command) {
		EtlExecution execution = historyService.createPending(
				EtlExecutionType.REMOVE_COLUMN,
				safeSourceFileName(command));
		historyService.markRunning(execution.companyId(), execution.id());

		try {
			RemoveColumnDownloadResult result = downloadService.download(command);
			historyService.markSuccess(
					execution.companyId(),
					execution.id(),
					result.processedRecordCount(),
					result.downloadFileName());
			return result;
		}
		catch (RuntimeException exception) {
			historyService.markFailed(execution.companyId(), execution.id());
			throw exception;
		}
	}

	private String safeSourceFileName(RemoveColumnDownloadCommand command) {
		if (command == null || command.file() == null) {
			return null;
		}
		String original = command.file().originalFileName();
		if (original == null || original.isBlank()) {
			return null;
		}
		int separator = Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\'));
		String baseName = original.substring(separator + 1)
				.replaceAll("[\\p{Cntrl}]", "")
				.strip();
		if (baseName.isEmpty()) {
			return null;
		}
		if (baseName.codePointCount(0, baseName.length()) <= 255) {
			return baseName;
		}
		int endIndex = baseName.offsetByCodePoints(0, 255);
		return baseName.substring(0, endIndex);
	}
}
