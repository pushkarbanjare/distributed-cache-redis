package com.pushkar.distributedcacheredis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pushkar.distributedcacheredis.entity.Product;
import com.pushkar.distributedcacheredis.service.ProductCacheService;

@RestController 
@RequestMapping("/products")
public class ProductController {
    private final ProductCacheService productCacheService;

    @Autowired 
    public ProductController(ProductCacheService productCacheService) {
        this.productCacheService = productCacheService;
    }

    @PostMapping 
    public Product create(@RequestBody Product product) {
        return productCacheService.writeThrough(product);
    }

    @PostMapping("/write-behind")
    public Product createWriteBehind(@RequestBody Product product) {
        return productCacheService.writeBehind(product);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable String id, @RequestBody Product product) {
        product.setId(id);
        return productCacheService.writeThrough(product);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        productCacheService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable String id) {
        return productCacheService.getProduct(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
