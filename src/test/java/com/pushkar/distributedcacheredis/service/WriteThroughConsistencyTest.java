package com.pushkar.distributedcacheredis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import com.pushkar.distributedcacheredis.entity.Product;
import com.pushkar.distributedcacheredis.repository.ProductRepository;

@SpringBootTest
class WriteThroughConsistencyTest {

    @Autowired
    private ProductCacheService productCacheService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Product> redisTemplate;

    @Test
    void cacheShouldAlwaysMatchDbAfterConcurrentWrites() throws InterruptedException {
        String productId = "race-test-" + UUID.randomUUID();
        int threadCount = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLine = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final double price = i;
            executor.submit(() -> {
                try {
                    startLine.await();
                    Product p = new Product(productId, "Race Test Product", price);
                    productCacheService.writeThrough(p);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startLine.countDown();
        finishLine.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        Product fromDb = productRepository.findById(productId).orElseThrow();
        Product fromCache = redisTemplate.opsForValue().get("product:" + productId);

        assertEquals(fromDb.getPrice(), fromCache.getPrice(), "Cache and DB must agree after concurrent writes — no inconsistency allowed");
    }
}