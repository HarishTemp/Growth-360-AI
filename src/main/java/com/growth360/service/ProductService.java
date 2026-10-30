package com.growth360.service;

import com.growth360.dao.ProductDAO;
import com.growth360.model.Product;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProductService {
    private final ProductDAO productDAO;

    public ProductService() {
        this.productDAO = new ProductDAO();
    }

    public ProductService(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    public List<Product> getAllProducts() throws SQLException {
        return productDAO.findAll();
    }

    public Optional<Product> getProductById(int id) throws SQLException {
        return productDAO.findById(id);
    }

    public Product createProduct(Product product) throws SQLException {
        return productDAO.save(product);
    }

    public void updateProduct(Product product) throws SQLException {
        productDAO.update(product);
    }

    public void deleteProduct(int id) throws SQLException {
        productDAO.delete(id);
    }

    public void restockProduct(int productId, int quantity) throws SQLException {
        productDAO.increaseStock(productId, quantity);
    }
}
