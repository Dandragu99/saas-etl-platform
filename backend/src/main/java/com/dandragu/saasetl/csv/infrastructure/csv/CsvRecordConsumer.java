package com.dandragu.saasetl.csv.infrastructure.csv;

import java.util.List;

public interface CsvRecordConsumer {

	void acceptHeader(List<String> columns);

	void acceptRow(List<String> row);
}
