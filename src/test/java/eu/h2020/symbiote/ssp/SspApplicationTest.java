package eu.h2020.symbiote.ssp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestConfiguration
@ActiveProfiles("test")
public class SspApplicationTest {

	@Test
	public void contextLoads() {
	}

}
