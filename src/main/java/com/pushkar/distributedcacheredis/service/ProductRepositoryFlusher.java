package com.pushkar.distributedcacheredis.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.pushkar.distributedcacheredis.entity.Product;
import com.pushkar.distributedcacheredis.repository.ProductRepository;

@Component 
public class ProductRepositoryFlusher {
    private final ProductRepository productRepository;
    public ProductRepositoryFlusher(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    public void flushBatch(List<Product> batch) {
        productRepository.saveAll(batch);
    }
}
