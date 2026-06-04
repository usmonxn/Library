package com.example.excelbot;

public class ProductData {

    private String step = "product_name";

    private String productName;
    private long skladPrice;
    private double uzumPercent;
    private long logistika = 5000;
    private long kgt = 5000;
    private long sellPrice;

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public long getSkladPrice() {
        return skladPrice;
    }

    public void setSkladPrice(long skladPrice) {
        this.skladPrice = skladPrice;
    }

    public double getUzumPercent() {
        return uzumPercent;
    }

    public void setUzumPercent(double uzumPercent) {
        this.uzumPercent = uzumPercent;
    }

    public long getLogistika() {
        return logistika;
    }

    public void setLogistika(long logistika) {
        this.logistika = logistika;
    }

    public long getKgt() {
        return kgt;
    }

    public void setKgt(long kgt) {
        this.kgt = kgt;
    }

    public long getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(long sellPrice) {
        this.sellPrice = sellPrice;
    }

    public double getUzumCommission() {
        return sellPrice * uzumPercent / 100;
    }

    public double getProfit() {
        return sellPrice - skladPrice - getUzumCommission() - logistika - kgt;
    }
}
