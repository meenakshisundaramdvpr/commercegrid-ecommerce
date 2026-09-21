package com.commercegrid.auth;

import com.commercegrid.auth.entity.Admin;
import com.commercegrid.auth.enums.AdminRole;
import com.commercegrid.auth.enums.AdminStatus;
import com.commercegrid.auth.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
		replace = AutoConfigureTestDatabase.Replace.NONE
)
class AdminRepositoryTest {

	@Autowired
	private AdminRepository adminRepository;

	@Test
	void shouldReturnTrueWhenEmailExists() {

		Admin admin = new Admin();

		admin.setName("Test Admin");
		admin.setEmail("existing@commercegrid.com");
		admin.setPassword("dummy-hashed-password");
		admin.setRole(AdminRole.ADMIN);
		admin.setStatus(AdminStatus.ACTIVE);

		adminRepository.save(admin);

		boolean exists =
				adminRepository.existsByEmail(
						"existing@commercegrid.com"
				);

		assertTrue(exists);
	}

	@Test
	void shouldFindAdminByEmail() {

		Admin admin = new Admin();

		admin.setName("Test Admin");
		admin.setEmail("test@commercegrid.com");
		admin.setPassword("dummy-hashed-password");
		admin.setRole(AdminRole.ADMIN);
		admin.setStatus(AdminStatus.ACTIVE);

		adminRepository.save(admin);

		Optional<Admin> result =
				adminRepository.findByEmail(
						"test@commercegrid.com"
				);

		assertTrue(result.isPresent());
		assertEquals(
				"test@commercegrid.com",
				result.get().getEmail()
		);
	}

	@Test
	void shouldReturnFalseWhenEmailDoesNotExist() {

		boolean exists =
				adminRepository.existsByEmail(
						"unknown@commercegrid.com"
				);

		assertFalse(exists);
	}
}