package com.dandragu.saasetl.csv.application;

import java.io.InputStream;

public record CsvPreviewCommand(String originalFileName, long size, InputStream content) {
}
