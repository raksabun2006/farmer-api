package co.cambodia.farmer.mapper;

import co.cambodia.farmer.domain.Product;
import co.cambodia.farmer.dto.product.ProductRequest;
import co.cambodia.farmer.dto.product.ProductResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        if (request == null) return null;

        Product product = new Product();
        product.setName(request.name());
        product.setNameKh(request.nameKh());
        product.setDescription(request.description());
        product.setDescriptionKh(request.descriptionKh());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
        product.setUnit(request.unit() != null && !request.unit().isBlank() ? request.unit().toUpperCase() : "KG");
        product.setStatus(request.status() != null && !request.status().isBlank() ? request.status().toUpperCase() : "ACTIVE");
        product.setLowStockThreshold(request.lowStockThreshold() != null ? request.lowStockThreshold() : 10);
        product.setImage(request.image());
        return product;
    }

    public ProductResponse toResponse(Product product) {
        if (product == null) return null;

        UUID farmerId = (product.getFarmer() != null) ? product.getFarmer().getId() : null;
        String farmerName = (product.getFarmer() != null) ? product.getFarmer().getFullName() : null;
        String categoryName = (product.getCategory() != null) ? product.getCategory().getName() : null;
        int threshold = (product.getLowStockThreshold() != null) ? product.getLowStockThreshold() : 10;
        boolean isLowStock = product.getQuantity() != null && product.getQuantity() <= threshold;

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getNameKh(),
                product.getDescription(),
                product.getDescriptionKh(),
                product.getPrice(),
                product.getQuantity(),
                product.getUnit() != null ? product.getUnit() : "KG",
                product.getStatus() != null ? product.getStatus() : "ACTIVE",
                threshold,
                isLowStock,
                product.getImage(),
                farmerId,
                farmerName,
                categoryName
        );
    }
}
