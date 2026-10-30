package com.growth360.service;

import com.growth360.dao.CustomerDAO;
import com.growth360.model.Customer;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CustomerService {
    private final CustomerDAO customerDAO;

    public CustomerService() {
        this.customerDAO = new CustomerDAO();
    }

    public CustomerService(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    public List<Customer> getAllCustomers() throws SQLException {
        return customerDAO.findAll();
    }

    public Optional<Customer> getCustomerById(int id) throws SQLException {
        return customerDAO.findById(id);
    }

    public Customer createCustomer(Customer customer) throws SQLException {
        return customerDAO.save(customer);
    }

    public void updateCustomer(Customer customer) throws SQLException {
        customerDAO.update(customer);
    }

    public void deleteCustomer(int id) throws SQLException {
        customerDAO.delete(id);
    }
}
