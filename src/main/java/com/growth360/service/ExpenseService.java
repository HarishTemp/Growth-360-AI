package com.growth360.service;

import com.growth360.dao.ExpenseDAO;
import com.growth360.model.Expense;

import java.sql.SQLException;
import java.util.List;

public class ExpenseService {
    private final ExpenseDAO expenseDAO;

    public ExpenseService() {
        this.expenseDAO = new ExpenseDAO();
    }

    public ExpenseService(ExpenseDAO expenseDAO) {
        this.expenseDAO = expenseDAO;
    }

    public List<Expense> getAllExpenses() throws SQLException {
        return expenseDAO.findAll();
    }

    public Expense createExpense(Expense expense) throws SQLException {
        return expenseDAO.save(expense);
    }

    public void updateExpense(Expense expense) throws SQLException {
        expenseDAO.update(expense);
    }

    public void deleteExpense(int id) throws SQLException {
        expenseDAO.delete(id);
    }

    public double getTotalExpenses() throws SQLException {
        return expenseDAO.getTotalExpense();
    }
}
