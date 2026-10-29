package co.cambodia.farmer.service;

import co.cambodia.farmer.dto.product.ProductRequest;
import co.cambodia.farmer.dto.product.ProductResponse;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse getProductById(UUID id);
    List<ProductResponse> getAllProducts();
    List<ProductResponse> getProductsByFarmer(UUID farmerId);
    List<ProductResponse> getProductsByCategory(UUID categoryId);
    ProductResponse updateProduct(UUID id, ProductRequest request);
    void deleteProduct(UUID id);
}
