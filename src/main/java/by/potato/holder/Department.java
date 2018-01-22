package by.potato.holder;

import by.potato.Pairs.Breaks;
import by.potato.helper.Comp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.maps.model.LatLng;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static by.potato.helper.StringHelper.humanReadableFormatFromDuration;

public class Department {

	@JsonIgnore
	private static final Logger logger = LogManager.getLogger(Department.class.getSimpleName());
	private static final String templateToBreak = "до перерыва %s \nПерерыв %s-%s";
	private static final String templateEndBreak = "до конца перерыва  %s\nПерерыв до %s";
	private static final String templateEndWork = "до конца работы %s\nОкончание работы %s";
	@JsonIgnore
	private String bankName;
	@JsonIgnore
	private Double dist;
	@JsonIgnore
	private String cityName;
	@JsonIgnore
	private String tel;
	@JsonIgnore
	private String address;
	@JsonIgnore
	private String nameOfDepartment;
	@JsonIgnore
	private List<Currency> currencies;
	@JsonIgnore
	private LatLng latlng;
	@JsonIgnore
	private Currency eur;
	@JsonIgnore
	private Currency rub;
	@JsonIgnore
	private Currency usd;
	@JsonIgnore
	private String linkToTimes;
	@JsonIgnore
	private String additionalInfo;
	@JsonValue
	private List<Day> worksTime = new ArrayList<>();


	public Department() {
	}

	public Department(Builder builder) {
		this.bankName = builder.bankName;
		this.address = builder.address;
		this.eur = builder.eur;
		this.rub = builder.rub;
		this.usd = builder.usd;
		this.cityName = builder.cityName;
		this.dist = builder.dist;
		this.worksTime = builder.worksTime;
		this.linkToTimes = builder.linkToTimes;
		this.additionalInfo = builder.additionalInfo;
	}

	public String getLinkToTimes() {
		return linkToTimes;
	}

	public void setLinkToTimes(String linkToTimes) {
		this.linkToTimes = linkToTimes;
	}

	public Currency getEur() {
		return eur;
	}

	public void setEur(Currency eur) {
		this.eur = eur;
	}

	public void setEuro(Currency eur) {
		this.eur = eur;
	}

	public Currency getRub() {
		return rub;
	}

	public void setRub(Currency rub) {
		this.rub = rub;
	}

	public Currency getUsd() {
		return usd;
	}

	public void setUsd(Currency usd) {
		this.usd = usd;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getTel() {
		return tel;
	}

	public void setTel(String tel) {
		this.tel = tel;
	}

	public String getNameOfDepartment() {
		return nameOfDepartment;
	}

	public void setNameOfDepartment(String nameOfDepartment) {
		this.nameOfDepartment = nameOfDepartment;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public List<Currency> getCurrencies() {
		return currencies;
	}

	public void setCurrencies(List<Currency> currencies) {
		this.currencies = currencies;
	}

	public LatLng getLatlng() {
		return latlng;
	}

	public void setLatlng(LatLng latlng) {
		this.latlng = latlng;
	}

	public List<Day> getWorksTime() {
		return worksTime;
	}

	public void setWorksTime(List<Day> worksTime) {
		this.worksTime = worksTime;
	}

	public Double getDist() {
		return dist;
	}

	public void setDist(Double dist) {
		this.dist = dist;
	}

	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	//работает ли отделение (перерывы не учитываются)
	public boolean isWork(DayOfWeek dayOfWeek, LocalTime local) {

		for (Day day : worksTime) {
			if (day.getDayOfWeek() == dayOfWeek) {
				if ((local.compareTo(day.getBegin()) > -1) && (local.compareTo(day.getEnd()) < 0)) {
					return true;
				}
			}
		}
		return false;
	}

	//инфо работает сейчас, перервы, сколько осталось до конца работы и т.д.
	public String getWorTime(LocalDateTime localDateTime) {

		DayOfWeek dayOfWeek = localDateTime.getDayOfWeek();
		LocalTime localTime = localDateTime.toLocalTime();

		for (Day day : worksTime) {
			if (day.getDayOfWeek() == dayOfWeek) {

				if (day.getBreaks() != null) {
					day.getBreaks().sort(Comp.BREAKS);

					//время до начало перерыва
					Breaks timeToBreakMin = this.timeToBeginBreak(localTime, day.getBreaks());
					if (timeToBreakMin != null) {
						return String.format(templateToBreak, humanReadableFormatFromDuration(localTime, timeToBreakMin.begin), timeToBreakMin.begin, timeToBreakMin.end);
					} else {
						//время до конца перерыва
						Breaks timeToEndBreakMin = this.timeToEndBreak(localTime, day.getBreaks());
						if (timeToEndBreakMin != null) {
							return String.format(templateEndBreak, humanReadableFormatFromDuration(localTime, timeToEndBreakMin.end), timeToEndBreakMin.end);
						}
					}

				} else {
					//время до конца рабочего дня
					Breaks timeToEndWork = this.timeToEndWork(day, localTime);
					return String.format(templateEndWork, humanReadableFormatFromDuration(localTime, timeToEndWork.end), timeToEndWork.end);
				}
			}
		}
		return null;
	}


	private Breaks timeToBeginBreak(LocalTime localTime, List<Breaks> breaks) {

		for (Breaks br : breaks) {
			if (localTime.compareTo(br.begin) < 0) {//если есть перерыв впереди
				return br;
			}
		}

		return null;
	}

	private Breaks timeToEndBreak(LocalTime localTime, List<Breaks> breaks) {
		for (Breaks br : breaks) {
			if (localTime.compareTo(br.begin) >= 0 && localTime.compareTo(br.end) < 0) {//находимся в промежутке перерыва
				return br;
			}
		}
		return null;
	}

	private Breaks timeToEndWork(Day day, LocalTime localTime) {
		return new Breaks(localTime, day.getEnd());
	}

	//сделать доп поле в БД
	public String getWorkTimeOriginal() {
		//return this.workTimeOriginal; TODO
		return null;
	}


    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((nameOfDepartment == null) ? 0 : nameOfDepartment.hashCode());
		result = prime * result + ((tel == null) ? 0 : tel.hashCode());
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
		Department other = (Department) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (nameOfDepartment == null) {
			if (other.nameOfDepartment != null)
				return false;
		} else if (!nameOfDepartment.equals(other.nameOfDepartment))
			return false;
		if (tel == null) {
            return other.tel == null;
		} else return tel.equals(other.tel);
    }

	public static class Builder {
		private String bankName;
		private String address;
		private Currency eur;
		private Currency rub;
		private Currency usd;
		private String cityName;
		private Double dist;
		private List<Day> worksTime;
		private String linkToTimes;
		private String additionalInfo;


		public Builder setBankName(String bankName) {
			this.bankName = bankName;
			return this;
		}

		public Builder setWorksTime(List<Day> worksTime) {
			this.worksTime = worksTime;
			return this;
		}

		public Builder setLinkToTimes(String linkToTimes) {
			this.linkToTimes = linkToTimes;
			return this;
		}

		public Builder setAddress(String address) {
			this.address = address;
			return this;
		}

		public Builder setEur(Currency eur) {
			this.eur = eur;
			return this;
		}

		public Builder setRub(Currency rub) {
			this.rub = rub;
			return this;
		}

		public Builder setUsd(Currency usd) {
			this.usd = usd;
			return this;
		}

		public Builder setCityName(String cityName) {
			this.cityName = cityName;
			return this;
		}

		public Builder setDist(Double dist) {
			this.dist = dist;
			return this;
		}

		public Builder setAdditionalInfo(String additionalInfo) {
			this.additionalInfo = additionalInfo;
			return this;
		}

		public Department build() {
			return new Department(this);
		}
	}
}
