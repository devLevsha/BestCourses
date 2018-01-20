package by.potato.holder;


import by.potato.Enum.TypeOfCurrency;

public class Currency {

	private TypeOfCurrency type;
	private Double valueSell;
	private Double valueBuy;
	private Integer multiplier;
	
	public Currency(TypeOfCurrency type, Integer multiplier) {
		this.type = type;
		this.multiplier = multiplier;
	}

	public Currency(){

	}

	public Currency(TypeOfCurrency type) {
		this.type = type;
	}

	public void setType(TypeOfCurrency type) {
		this.type = type;
	}

	public void setMultiplier(Integer multiplier) {
		this.multiplier = multiplier;
	}

	public TypeOfCurrency getType() {
		return type;
	}

	public Integer getMultiplier() {
		return multiplier;
	}

	public Double getValueSell() {
		return valueSell;
	}

	public void setValueSell(Double valueSell) {
		this.valueSell = valueSell;
	}

	public Double getValueBuy() {
		return valueBuy;
	}

	public void setValueBuy(Double valueBuy) {
		this.valueBuy = valueBuy;
	}
	
	@Override
	public String toString() {
		return "Currency [type=" + type + ", valueSell=" + valueSell + ", valueBuy=" + valueBuy + ", multiplier="
				+ multiplier + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((multiplier == null) ? 0 : multiplier.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Currency other = (Currency) obj;
		if (multiplier == null) {
			if (other.multiplier != null)
				return false;
		} else if (!multiplier.equals(other.multiplier))
			return false;
		return type == other.type;
	}


}
