package uteq.edu.ec.artisync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uteq.edu.ec.artisync.service.shared.EmailService;

@SpringBootTest
@TestPropertySource(properties = {
    "JWT_SECRET=test-fake-jwt-secret-do-not-use-in-production-0000000000"
})
class ArtisyncApplicationTests {

	@MockitoBean
	private EmailService emailService;

	@Test
	void contextLoads() {
	}

}
