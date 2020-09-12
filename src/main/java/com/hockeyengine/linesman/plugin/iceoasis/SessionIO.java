/**
 * 
 */
package com.hockeyengine.linesman.plugin.iceoasis;

/**
 * @author mpietras
 *
 */
public class SessionIO {
	
	private String time;
	
	private String openings;
	
	private String name;
	
	private String length;
	
	private String price;

	public SessionIO(String time, String openings, String name, String length, String price) {
		super();
		this.time = time;
		this.openings = openings;
		this.name = name;
		this.length = length;
		this.price = price;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getOpenings() {
		return openings;
	}

	public void setOpenings(String openings) {
		this.openings = openings;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLength() {
		return length;
	}

	public void setLength(String length) {
		this.length = length;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	@Override
	public String toString() {
		return "SessionIO [time=" + time + ", openings=" + openings + ", name=" + name + ", length=" + length
				+ ", price=" + price + "]";
	}

}
