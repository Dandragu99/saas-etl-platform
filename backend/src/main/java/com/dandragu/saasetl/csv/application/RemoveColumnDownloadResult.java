package com.dandragu.saasetl.csv.application;

public record RemoveColumnDownloadResult(String downloadFileName, byte[] content) {

	public RemoveColumnDownloadResult {
		content = content.clone();
	}

	@Override
	public byte[] content() {
		return content.clone();
	}
}
