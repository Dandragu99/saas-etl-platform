package com.dandragu.saasetl.csv.application;

public record RemoveColumnDownloadCommand(CsvPreviewCommand file, String column) {
}
