package uteq.edu.ec.artisync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uteq.edu.ec.artisync.service.shared.EmailService;

@SpringBootTest
@TestPropertySource(properties = {
    "JWT_SECRET=f98cf546c1a89c93f0b2f1559868779b76c8c4a4f89d0b676a74c431d1d8ef3f"
})
class ArtisyncApplicationTests {

	@MockitoBean
	private EmailService emailService;

	@Test
	void contextLoads() {
	}

}
