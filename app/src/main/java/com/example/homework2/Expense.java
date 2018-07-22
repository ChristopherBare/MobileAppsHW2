package com.example.homework2;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;

public class Expense implements Serializable {
    private String name, date, uniqueID;
    private double cost;

    public Expense() {
        generateID();
    }

    public Expense(String name, String date, double cost) {
        this.name = name;
        this.date = date;
        this.cost = cost;
        generateID();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getCost() {
        return cost;
    }

    public String getCostAsString() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        return format.format(getCost());
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public void setUniqueID(String uniqueID) {
        this.uniqueID = uniqueID;
    }

    private void generateID() {
        int random = (int) (Math.random() * 1000000000);
        this.uniqueID = Integer.toString(random);
    }
}
