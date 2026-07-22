package com.dandragu.saasetl.csv.application;

public record RemoveColumnCommand(CsvPreviewCommand file, String column) {
}
