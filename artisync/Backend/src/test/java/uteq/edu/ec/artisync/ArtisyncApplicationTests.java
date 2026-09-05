package uteq.edu.ec.artisync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uteq.edu.ec.artisync.service.shared.EmailService;

@SpringBootTest
@TestPropertySource(properties = {
    "JWT_SECRET=d5d0f9946b0a3804c562579f9ad06d66dd9ed91c4a5d7787cf7a139fd34ad834"
})
class ArtisyncApplicationTests {

	@MockitoBean
	private EmailService emailService;

	@Test
	void contextLoads() {
	}

}
