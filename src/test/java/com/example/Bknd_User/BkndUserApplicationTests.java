package com.example.Bknd_User;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=clave-test-1234567890-clave-test-1234567890",
    "jwt.expiration=86400000",
    "resend.api.key=test-api-key",
    "resend.from.email=test@monex.cl"
})
class BkndUserApplicationTests {

	@Test
	void contextLoads() {
	}

}