package com.pushkar.distributedcacheredis.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity 
@Getter 
@Setter 
@NoArgsConstructor 
public class Product {
    
    @Id 
    private String id;
    private String name;
    private Double price;

    public Product(String id, String name, Double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
}
