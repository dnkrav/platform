package fdk.integration;

public class PriceListSupplier {
    public String idUserPriceList;
    public String item;
    public String supplier;
    public String currency;
    public Double price;
    public Boolean inPriceList;

    public PriceListSupplier(String idUserPriceList, String item, String supplier, String currency, Double price,
                             Boolean inPriceList) {
        this.idUserPriceList = idUserPriceList;
        this.item = item;
        this.supplier = supplier;
        this.currency = currency;
        this.price = price;
        this.inPriceList = inPriceList;
    }
}
