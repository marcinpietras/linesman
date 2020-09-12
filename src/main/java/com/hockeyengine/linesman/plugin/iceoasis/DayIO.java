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
	
	private String Date;
	
	private List<SessionIO> sessions = new ArrayList<SessionIO>();

	public DayIO(String date) {
		super();
		Date = date;
	}

	public String getDate() {
		return Date;
	}

	public List<SessionIO> getSessions() {
		return sessions;
	}

	@Override
	public String toString() {
		return "DayIO [Date=" + Date + ", sessions=" + sessions + "]";
	}
	
}
