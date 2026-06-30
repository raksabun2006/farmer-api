package co.cambodia.farmer.mapper;

import co.cambodia.farmer.domain.Category;
import co.cambodia.farmer.dto.category.CategoryRequest;
import co.cambodia.farmer.dto.category.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request) {
        if (request == null) return null;

        Category category = new Category();
        category.setName(request.name());
        return category;
    }

    public CategoryResponse toResponse(Category category) {
        if (category == null) return null;

        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }
}