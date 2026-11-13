package com.pushkar.distributedcacheredis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pushkar.distributedcacheredis.entity.Product;

@Repository 
public interface ProductRepository extends JpaRepository<Product, String>{}
