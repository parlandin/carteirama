package com.parlandim.carteirama;

import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CarteiramaApplicationTests extends IntegrationTest {

	@Test
	void contextLoads() {
		assertThat(jdbc.queryForObject("SELECT price_cents FROM plans WHERE code = 'cloud'", Integer.class))
				.isEqualTo(2499);
	}

}
