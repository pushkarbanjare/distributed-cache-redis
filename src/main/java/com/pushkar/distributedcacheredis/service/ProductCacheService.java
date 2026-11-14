package com.pushkar.distributedcacheredis.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.pushkar.distributedcacheredis.entity.Product;
import com.pushkar.distributedcacheredis.repository.ProductRepository;

@Service
public class ProductCacheService {
    private static final String CACHE_PREFIX = "product:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private final ProductRepository productRepository;
    private final RedisTemplate<String, Product> redisTemplate;
    private final WriteBehindQueueService writeBehindQueueService;

    @Autowired
    public ProductCacheService(ProductRepository productRepository, RedisTemplate<String, Product> redisTemplate, WriteBehindQueueService writeBehindQueueService) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.writeBehindQueueService = writeBehindQueueService;
    }

    public Optional<Product> getProduct(String id) {
        String cacheKey = CACHE_PREFIX + id;

        // check redis
        Product cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            System.out.println("CACHE HIT for " + id);
            return Optional.of(cached);
        }

        // cache miss, check db
        System.out.println("CACHE MISS for " + id + " -> hitting DB");
        Optional<Product> fromDb = productRepository.findById(id);

        // save in cache for future
        fromDb.ifPresent(product -> redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL));
        return fromDb;
    }

    public Product writeThrough(Product product) {
        // db write
        Product saved = productRepository.save(product);

        // cache write - in sync
        String cacheKey = CACHE_PREFIX + saved.getId();
        redisTemplate.opsForValue().set(cacheKey, saved, CACHE_TTL);
        System.out.println("WRITE-THROUGH: synced product " + saved.getId() + " to DB and cache");
        return saved;
    }

    public Product writeBehind(Product product) {
        // cache updated
        String cacheKey = CACHE_PREFIX + product.getId();
        redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL);

        // db write queued
        writeBehindQueueService.enqueue(product);

        System.out.println("WRITE-BEHIND: cached " + product.getId() + " immediately, DB write queued");
        return product;
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
        redisTemplate.delete(CACHE_PREFIX + id);
        System.out.println("WRITE-THROUGH: deleted product " + id + " from Db and cache");
    }
}
