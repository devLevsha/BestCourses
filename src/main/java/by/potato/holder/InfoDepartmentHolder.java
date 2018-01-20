package by.potato.holder;

import java.util.HashMap;
import java.util.Map;

public class InfoDepartmentHolder {

	private final String address;
	private Map<String,Double> course;

	public InfoDepartmentHolder(String address) {
		this.address = address;

		this.course = new HashMap<>();
	}

	public String getAddress() {
		return address;
	}

	public Map<String, Double> getCourse() {
		return course;
	}

	
}
