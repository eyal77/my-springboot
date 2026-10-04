package com.example.eyal.rest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.cloud.zookeeper.enabled=false")
class RestApplicationTests {

	@Test
	void contextLoads() {
	}

}
