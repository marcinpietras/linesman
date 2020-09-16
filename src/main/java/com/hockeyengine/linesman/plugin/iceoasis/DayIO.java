/**
 * 
 */
package com.hockeyengine.linesman.plugin.iceoasis;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mpietras
 *
 */
public class DayIO {
	
	private String date;
	
	private List<SessionIO> sessions = new ArrayList<SessionIO>();

	public DayIO(String date) {
		super();
		this.date = date;
	}

	public String getDate() {
		return date;
	}

	public List<SessionIO> getSessions() {
		return sessions;
	}

	@Override
	public String toString() {
		return "DayIO [date=" + date + ", sessions=" + sessions + "]";
	}
	
}
