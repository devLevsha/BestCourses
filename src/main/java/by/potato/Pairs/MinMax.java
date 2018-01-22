package by.potato.Pairs;

public class MinMax {

    private Double minSell;
    private Double maxBuy;

    public MinMax() {
    }

    public MinMax(Double minSell, Double maxBuy) {
        this.minSell = minSell;
        this.maxBuy = maxBuy;
    }

    public Double getMinSell() {
        return minSell;
    }

    public void setMinSell(Double minSell) {
        this.minSell = minSell;
    }

    public Double getMaxBuy() {
        return maxBuy;
    }

    public void setMaxBuy(Double maxBuy) {
        this.maxBuy = maxBuy;
    }
}
