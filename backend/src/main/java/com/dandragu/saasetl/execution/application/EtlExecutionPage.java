package com.dandragu.saasetl.execution.application;

import java.util.List;

import com.dandragu.saasetl.execution.domain.EtlExecution;

public record EtlExecutionPage(
		List<EtlExecution> content,
		long totalElements,
		int page,
		int size) {

	public EtlExecutionPage {
		content = List.copyOf(content);
	}

	public int totalPages() {
		return totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
	}
}
