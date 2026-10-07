package com.babyshophub;

import com.babyshophub.dto.OrderResponse;
import com.babyshophub.dto.RegisterRequest;
import com.babyshophub.entity.Address;
import com.babyshophub.entity.Brand;
import com.babyshophub.entity.Category;
import com.babyshophub.entity.Product;
import com.babyshophub.entity.User;
import com.babyshophub.enums.Role;
import com.babyshophub.repository.AddressRepository;
import com.babyshophub.repository.BrandRepository;
import com.babyshophub.repository.CategoryRepository;
import com.babyshophub.repository.ProductRepository;
import com.babyshophub.repository.UserRepository;
import com.babyshophub.service.CartService;
import com.babyshophub.service.AuthService;
import com.babyshophub.service.OrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootTest
@ActiveProfiles("test")
class BabyshophubApplicationTests {

	@Autowired private UserRepository userRepository;
	@Autowired private CategoryRepository categoryRepository;
	@Autowired private BrandRepository brandRepository;
	@Autowired private ProductRepository productRepository;
	@Autowired private AddressRepository addressRepository;
	@Autowired private CartService cartService;
	@Autowired private AuthService authService;
	@Autowired private OrderService orderService;

	@Test
	void contextLoads() {
	}

	@Test
	void registrationUsesConsoleVerificationCodeInLocalMode() {
		String email = "register-" + java.util.UUID.randomUUID() + "@example.com";
		RegisterRequest request = new RegisterRequest();
		request.setName("Registration test");
		request.setEmail(email);
		request.setPassword("test-password");
		request.setDob(LocalDate.of(2000, 1, 1));
		request.setPhoneNumber("08000000000");

		String response = authService.registerCustomer(request);

		Assertions.assertTrue(response.contains("server console"));
		Assertions.assertTrue(userRepository.findByEmail(email).orElseThrow().getVerificationCode().matches("\\d{6}"));
	}

	@Test
	void checkoutSnapshotsCurrentPricesReducesStockAndRestrictsOrderOwnership() {
		String email = "checkout-" + java.util.UUID.randomUUID() + "@example.com";
		User user = new User();
		user.setName("Checkout test"); user.setEmail(email); user.setPassword("already-encoded-for-test");
		user.setPhoneNumber("08000000000"); user.setDob(LocalDate.of(2000, 1, 1)); user.setEnabled(true);
		user.getRoles().add(Role.ROLE_CUSTOMER); user = userRepository.save(user);
		Category category = new Category(); category.setName("Test category " + java.util.UUID.randomUUID());
		category = categoryRepository.save(category);
		Brand brand = new Brand(); brand.setName("Test brand " + java.util.UUID.randomUUID()); brand = brandRepository.save(brand);
		Product product = new Product(); product.setName("Test product"); product.setPrice(new BigDecimal("12.50"));
		product.setStockQty(4); product.setCategory(category); product.setBrand(brand); product = productRepository.save(product);
		Address address = new Address(); address.setUser(user); address.setLine1("1 Test Street");
		address.setCity("Lagos"); address.setCountry("Nigeria"); address = addressRepository.save(address);
		cartService.addItem(email, product.getProductId(), 2);
		OrderResponse order = orderService.checkout(email, address.getAddressId());
		Assertions.assertEquals(new BigDecimal("25.00"), order.totalAmount());
		Assertions.assertEquals("PAID", order.paymentStatus());
		Assertions.assertEquals(2, order.items().getFirst().quantity());
		Assertions.assertEquals(Integer.valueOf(2), productRepository.findById(product.getProductId()).orElseThrow().getStockQty());
		ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
				() -> orderService.myOrder("someone-else@example.com", order.orderId()));
		Assertions.assertNotNull(exception);
	}

}
