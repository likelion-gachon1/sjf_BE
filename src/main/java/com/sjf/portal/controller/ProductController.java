package com.sjf.portal.controller;

import com.sjf.portal.dto.ProductResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    public List<ProductResponse> getProducts() {
        return List.of(
                new ProductResponse(
                        1L,
                        "Pink Bag",
                        "PINK",
                        "/images/pink-bag.png",
                        "https://kr.mcmworldwide.com/"
                ),
                new ProductResponse(
                        2L,
                        "Black Bag",
                        "BLACK",
                        "/images/black-bag.png",
                        "https://kr.mcmworldwide.com/"
                )
        );
    }
}