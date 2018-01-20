package by.potato.holder;

public class City {

    private final String rusName;
    private final String engName;

    public City(String rusName, String engName) {
        this.rusName = rusName;
        this.engName = engName;
    }

    public String getRusName() {
        return rusName;
    }

    public String getEngName() {
        return engName;
    }
}
