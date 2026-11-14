package com.pushkar.distributedcacheredis.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.pushkar.distributedcacheredis.entity.Product;
import com.pushkar.distributedcacheredis.repository.ProductRepository;

@SpringBootTest
class WriteBehindBehaviorTest {

    @Autowired
    private ProductCacheService productCacheService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void dbShouldLagBehindCacheThenEventuallyCatchUp() {
        String id = "writebehind-test-" + UUID.randomUUID();
        Product p = new Product(id, "Lag Test Product", 500.0);

        productCacheService.writeBehind(p);

        assertFalse(productRepository.findById(id).isPresent(), "DB should not be updated immediately under write-behind");
        await().atMost(8, SECONDS).untilAsserted(() -> assertTrue(productRepository.findById(id).isPresent(), "DB should eventually catch up after the flush interval"));
    }
}