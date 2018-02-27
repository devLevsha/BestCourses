package by.potato.holder;

public class UserSettings {
    private Boolean rubSell;
    private Boolean rubBuy;
    private Boolean usdSell;
    private Boolean usdBuy;
    private Boolean eurSell;
    private Boolean eurBuy;
    private Boolean phone;
    private Boolean workTime;

    public UserSettings(Builder builder) {
        this.rubSell = builder.rubSell;
        this.rubBuy = builder.rubBuy;
        this.usdSell = builder.usdSell;
        this.usdBuy = builder.usdBuy;
        this.eurSell = builder.eurSell;
        this.eurBuy = builder.eurBuy;
        this.phone = builder.phone;
        this.workTime = builder.workTime;
    }

    public Boolean getRubSell() {
        return rubSell;
    }

    public void setRubSell(Boolean rubSell) {
        this.rubSell = rubSell;
    }

    public Boolean getRubBuy() {
        return rubBuy;
    }

    public void setRubBuy(Boolean rubBuy) {
        this.rubBuy = rubBuy;
    }

    public Boolean getUsdSell() {
        return usdSell;
    }

    public void setUsdSell(Boolean usdSell) {
        this.usdSell = usdSell;
    }

    public Boolean getUsdBuy() {
        return usdBuy;
    }

    public void setUsdBuy(Boolean usdBuy) {
        this.usdBuy = usdBuy;
    }

    public Boolean getEurSell() {
        return eurSell;
    }

    public void setEurSell(Boolean eurSell) {
        this.eurSell = eurSell;
    }

    public Boolean getEurBuy() {
        return eurBuy;
    }

    public void setEurBuy(Boolean eurBuy) {
        this.eurBuy = eurBuy;
    }

    public Boolean getPhone() {
        return phone;
    }

    public void setPhone(Boolean phone) {
        this.phone = phone;
    }

    public Boolean getWorkTime() {
        return workTime;
    }

    public void setWorkTime(Boolean workTime) {
        this.workTime = workTime;
    }

    public static class Builder {
        private Boolean rubSell;
        private Boolean rubBuy;
        private Boolean usdSell;
        private Boolean usdBuy;
        private Boolean eurSell;
        private Boolean eurBuy;
        private Boolean phone;
        private Boolean workTime;

        public Builder setRubSell(Boolean rubSell) {
            this.rubSell = rubSell;
            return this;
        }

        public Builder setRubBuy(Boolean rubBuy) {
            this.rubBuy = rubBuy;
            return this;
        }

        public Builder setUsdSell(Boolean usdSell) {
            this.usdSell = usdSell;
            return this;
        }

        public Builder setUsdBuy(Boolean usdBuy) {
            this.usdBuy = usdBuy;
            return this;
        }

        public Builder setEurSell(Boolean eurSell) {
            this.eurSell = eurSell;
            return this;
        }

        public Builder setEurBuy(Boolean eurBuy) {
            this.eurBuy = eurBuy;
            return this;
        }

        public Builder setPhone(Boolean phone) {
            this.phone = phone;
            return this;
        }

        public Builder setWorkTime(Boolean workTime) {
            this.workTime = workTime;
            return this;
        }
    }
}
