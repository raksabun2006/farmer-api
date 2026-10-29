package co.cambodia.farmer.service;

import co.cambodia.farmer.dto.category.CategoryRequest;
import co.cambodia.farmer.dto.category.CategoryResponse;
import java.util.List;
import java.util.UUID;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse getCategoryById(UUID id);
    List<CategoryResponse> getAllCategories();
    CategoryResponse updateCategory(UUID id, CategoryRequest request);
    void deleteCategory(UUID id);
}
