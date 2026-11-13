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

    @Autowired 
    public ProductCacheService(ProductRepository productRepository, RedisTemplate<String, Product> redisTemplate) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
    }

    public Optional<Product> getProduct(String id) {
        String cacheKey = CACHE_PREFIX + id;

        // check redis
        Product cached = redisTemplate.opsForValue().get(cacheKey);
        if(cached != null) {
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

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
}
