package co.cambodia.farmer.mapper;

import co.cambodia.farmer.domain.Product;
import co.cambodia.farmer.dto.product.ProductRequest;
import co.cambodia.farmer.dto.product.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        if (request == null) return null;

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
        product.setImage(request.image());
        return product;
    }

    public ProductResponse toResponse(Product product) {
        if (product == null) return null;

        Long farmerId = (product.getFarmer() != null) ? product.getFarmer().getId() : null;
        String farmerName = (product.getFarmer() != null) ? product.getFarmer().getFullName() : null;
        String categoryName = (product.getCategory() != null) ? product.getCategory().getName() : null;

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getImage(),
                farmerId,
                farmerName,
                categoryName
        );
    }
}