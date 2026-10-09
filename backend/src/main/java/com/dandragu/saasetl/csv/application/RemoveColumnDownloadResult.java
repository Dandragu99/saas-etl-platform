package com.dandragu.saasetl.csv.application;

public record RemoveColumnDownloadResult(
		String downloadFileName,
		byte[] content,
		long processedRecordCount) {

	public RemoveColumnDownloadResult {
		if (processedRecordCount < 0) {
			throw new IllegalArgumentException("processedRecordCount must not be negative");
		}
		content = content.clone();
	}

	@Override
	public byte[] content() {
		return content.clone();
	}
}
