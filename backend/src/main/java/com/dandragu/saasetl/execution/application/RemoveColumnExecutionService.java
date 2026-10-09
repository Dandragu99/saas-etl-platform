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
		EtlExecution execution = historyService.createPending(EtlExecutionType.REMOVE_COLUMN);
		historyService.markRunning(execution.id());

		try {
			RemoveColumnDownloadResult result = downloadService.download(command);
			historyService.markSuccess(execution.id(), result.processedRecordCount());
			return result;
		}
		catch (RuntimeException exception) {
			historyService.markFailed(execution.id());
			throw exception;
		}
	}
}
