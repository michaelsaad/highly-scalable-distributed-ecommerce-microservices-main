package com.m1kllz.ecommerce.product.service;

import com.m1kllz.ecommerce.product.exception.ResourceNotFoundException;
import com.m1kllz.ecommerce.product.model.Product;
import com.m1kllz.ecommerce.product.repository.ProductRepository;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

        private final ProductRepository productRepository;

        @Override
        @Observed(name = "product.create")
        public Product createProduct(Product product) {
                Product saved = productRepository.save(product);
                log.info("Product created id={} skuCode={}", saved.getId(), saved.getSkuCode());
                return saved;
        }

        @Override
        public List<Product> getAllProducts() {
                return productRepository.findAll();
        }

        @Override
        public Optional<Product> getProductById(String id) {
                return productRepository.findById(id);
        }

        @Override
        @Observed(name = "product.delete")
        public void deleteProduct(String id) {
                if (!productRepository.existsById(id)) {
                        throw new ResourceNotFoundException("Product not found with id " + id);
                }
                productRepository.deleteById(id);
                log.info("Product deleted id={}", id);
        }

        @Override
        @Observed(name = "product.update")
        public Product updateProduct(String id, Product updatedProduct) {
                return productRepository.findById(id)
                                .map(existing -> {
                                        existing.setName(updatedProduct.getName());
                                        existing.setDescription(updatedProduct.getDescription());
                                        existing.setSkuCode(updatedProduct.getSkuCode());
                                        existing.setPrice(updatedProduct.getPrice());
                                        Product saved = productRepository.save(existing);
                                        log.info("Product updated id={} skuCode={}", saved.getId(), saved.getSkuCode());
                                        return saved;
                                })
                                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
        }

        @Override
        public List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
                return productRepository.findByPriceBetween(minPrice, maxPrice);
        }

        @Override
        public List<Product> searchProducts(String keyword) {
                return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword,
                                keyword);
        }

        @Override
        public Optional<Product> getProductBySku(String skuCode) {
                return productRepository.findBySkuCode(skuCode);
        }
}
