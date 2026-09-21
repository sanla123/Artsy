package io.artsy.Service;

import io.artsy.DTO.ProductDTO;
import io.artsy.DTO.ProductRequest;
import io.artsy.Model.Category;
import io.artsy.Model.Product;
import io.artsy.Model.Vendor;
import io.artsy.Repository.CategoryRepository;
import io.artsy.Repository.ProductRepository;
import io.artsy.Repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private VendorRepository vendorRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    public List<ProductDTO> getAllProducts() {
        return productRepo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public Optional<ProductDTO> getProductById(int id) {
        return productRepo.findById(id).map(this::toDTO);
    }

    public ProductDTO createProduct(ProductRequest request) {
        Vendor vendor = vendorRepo.findById(request.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id " + request.getVendorId()));

        Product product = new Product();
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setProductType(request.getProductType());
        product.setImage(request.getImage());
        product.setVendor(vendor);

        if (request.getCategoryId() != 0) {
            Category category = categoryRepo.findById(request.getCategoryId()).orElse(null);
            product.setCategory(category);
        }

        return toDTO(productRepo.save(product));
    }

    public Optional<ProductDTO> updateProduct(int id, ProductRequest request) {
        return productRepo.findById(id).map(product -> {
            product.setTitle(request.getTitle());
            product.setDescription(request.getDescription());
            product.setPrice(request.getPrice());
            product.setStock(request.getStock());
            product.setProductType(request.getProductType());
            if (request.getImage() != null && !request.getImage().isEmpty()) {
                product.setImage(request.getImage());
            }
            return toDTO(productRepo.save(product));
        });
    }

    public boolean deleteProduct(int id) {
        if (productRepo.existsById(id)) {
            productRepo.deleteById(id);
            return true;
        }
        return false;
    }

    private ProductDTO toDTO(Product p) {
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setPrice(p.getPrice());
        dto.setImage(p.getImage());
        dto.setStock(p.getStock());
        dto.setProductType(p.getProductType());
        dto.setCategoryName(p.getCategory() != null ? p.getCategory().getCategoryName() : null);
        if (p.getVendor() != null) {
            dto.setVendorId(p.getVendor().getId());
            dto.setVendorShopName(p.getVendor().getShopName());
        }
        return dto;
    }
}