package com.growth360.service;

import com.growth360.dao.SaleDAO;
import com.growth360.model.Sale;

import java.sql.SQLException;
import java.util.List;

public class SaleService {
    private final SaleDAO saleDAO;

    public SaleService() {
        this.saleDAO = new SaleDAO();
    }

    public SaleService(SaleDAO saleDAO) {
        this.saleDAO = saleDAO;
    }

    public List<Sale> getAllSales() throws SQLException {
        return saleDAO.findAll();
    }

    public Sale recordSale(Sale sale) throws SQLException {
        return saleDAO.recordSaleTransactional(sale);
    }

    public void deleteSale(int id) throws SQLException {
        saleDAO.delete(id);
    }

    public double getTotalRevenue() throws SQLException {
        return saleDAO.getTotalRevenue();
    }
}
