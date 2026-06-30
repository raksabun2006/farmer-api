package co.cambodia.farmer.service;

import co.cambodia.farmer.dto.product.ProductRequest;
import co.cambodia.farmer.dto.product.ProductResponse;
import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse getProductById(Long id);
    List<ProductResponse> getAllProducts();
    List<ProductResponse> getProductsByFarmer(Long farmerId);
    List<ProductResponse> getProductsByCategory(Long categoryId);
    ProductResponse updateProduct(Long id, ProductRequest request);
    void deleteProduct(Long id);
}