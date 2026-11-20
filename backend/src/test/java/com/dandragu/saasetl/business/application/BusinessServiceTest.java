package com.dandragu.saasetl.business.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dandragu.saasetl.business.application.BusinessModels.LineInput;

class BusinessServiceTest {

	@Test
	void shouldCalculateLinesAndTaxWithCommercialRounding() {
		var result = BusinessService.calculate(List.of(
				new LineInput("Cuadro eléctrico", new BigDecimal("1"), new BigDecimal("350.00"),
						new BigDecimal("21")),
				new LineInput("Punto de luz", new BigDecimal("6"), new BigDecimal("42.50"),
						new BigDecimal("21"))));

		assertThat(result.subtotal()).isEqualByComparingTo("605.00");
		assertThat(result.taxTotal()).isEqualByComparingTo("127.05");
		assertThat(result.total()).isEqualByComparingTo("732.05");
		assertThat(result.lines()).hasSize(2);
	}

	@Test
	void shouldRejectDocumentsWithoutLinesAndInvalidAmounts() {
		assertThatThrownBy(() -> BusinessService.calculate(List.of()))
				.isInstanceOf(BusinessValidationException.class);
		assertThatThrownBy(() -> BusinessService.calculate(List.of(
				new LineInput("Invalid", BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("21")))))
				.isInstanceOf(BusinessValidationException.class);
		assertThatThrownBy(() -> BusinessService.calculate(List.of(
				new LineInput("Invalid", BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("101")))))
				.isInstanceOf(BusinessValidationException.class);
	}
}
