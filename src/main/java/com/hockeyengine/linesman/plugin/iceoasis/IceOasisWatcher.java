/**
 * 
 */
package com.hockeyengine.linesman.plugin.iceoasis;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.common.base.Stopwatch;
import com.hockeyengine.quickshift.core.Plugin;
import com.hockeyengine.quickshift.core.PluginException;

/**
 * @author mpietras
 *
 */
@Component
public class IceOasisWatcher implements Plugin {

	private Logger logger = LoggerFactory.getLogger(IceOasisWatcher.class);

	private static final String SOLD_OUT = "SOLD OUT!";

	private static final String CRON_EXPRESSION = "0 0/5 * * * ?";

	private RestTemplate restTemplate = new RestTemplate();

	private AmazonSNS snsClient = AmazonSNSClient.builder().withRegion("us-west-2").build();

	private Scheduler scheduler;

	private List<String> lastExecutionLog = new ArrayList<String>();

	private Map<String, String> notifications = new HashMap<String, String>();
	
	private List<String> notificationsToDisplay = new ArrayList<String>();

	private List<Config> configs = new ArrayList<Config>();

	private List<String> scheduleUrls = new ArrayList<String>();

	@Override
	public void init() {
		logger.info("Initializing plugin iceOasisWatcher");
		
		// Configuration of schedule urls
		this.scheduleUrls.add("https://iceoasis.frontline-connect.com/sessionslist.cfm?fac=iceoasis&facid=1");
		this.scheduleUrls.add("https://iceoasis.frontline-connect.com/sessionslist.cfm?fac=iceoasis&facid=1&webruleid=12");

		// Configuration of events to watch and notification phone numbers
		this.configs.add(new Config("Stick N Shoot", null, null, Arrays.asList("+13023454133")));
		this.configs.add(new Config("Figure Skating FreeStyle 60 Minutes", LocalTime.of(7, 31, 0, 0),
				LocalTime.of(13, 00, 0, 0), Arrays.asList("+13023454133", "+16504208430")));
		this.configs.add(new Config("Figure Skating FREESTYLE 90 Minutes", LocalTime.of(7, 01, 0, 0),
				LocalTime.of(13, 00, 0, 0), Arrays.asList("+13023454133", "+16504208430")));
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
			logger.error("Initialize plugin iceOasisWatcher fail", e);
		}
		logger.info("Plugin iceOasisWatcher initialization success");
	}

	@Override
	public String helthCheck() throws PluginException {
		logger.info("Healthchecking plugin iceOasisWatcher");
		Map<String, String> context = new HashMap<String, String>();
		context.put("mode", "test");
		List<String> executionLog = new ArrayList<String>();

		watchIceOasis(context);
		notifyViaSMS("HelthCheck", new ArrayList<String>(), Arrays.asList("+13023454133"), executionLog, context);
		logger.info("Healthcheck plugin iceOasisWatcher success");
		return "Healthcheck success. Configuration: " + this.configs + ". Last execution log: " + this.lastExecutionLog;
	}

	@Override
	public String getReport() throws PluginException {
		logger.info("Getting report from plugin iceOasisWatcher");
		Map<String, String> context = new HashMap<String, String>();
		context.put("mode", "report");

		StringBuilder report = new StringBuilder();
		report.append("Cron job set to ");
		report.append(CRON_EXPRESSION);
		report.append(" is running: ");
		try {
			report.append(this.scheduler.checkExists(createWatchJobDetail(context).getKey()));
		} catch (SchedulerException e) {
			throw new PluginException("Stopping plugin iceOasisWatcher fail", e);
		}
		report.append("<br><br>Schedule Urls:<br>");
		report.append(this.scheduleUrls);
		report.append("<br><br>Configuration:<br>");
		report.append(this.configs);
		report.append("<br><br>Last execution log:<br>");
		report.append(this.lastExecutionLog);
		report.append("<br><br>Notification sent:<br>");
		report.append(this.notificationsToDisplay);
		logger.info("Get report for plugin iceOasisWatcher success");
		return report.toString();
	}

	@Override
	public String start() throws PluginException {
		logger.info("Starting plugin iceOasisWatcher");
		Map<String, String> context = new HashMap<String, String>();
		context.put("mode", "cron");

		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("cronTrigger1", "group1")
				.withSchedule(CronScheduleBuilder.cronSchedule(CRON_EXPRESSION)).build();
		JobDetail watchJobDetail = createWatchJobDetail(context);
		try {
			if (this.scheduler.checkExists(watchJobDetail.getKey())) {
				this.scheduler.deleteJob(watchJobDetail.getKey());
			}
			scheduler.scheduleJob(watchJobDetail, trigger);
		} catch (SchedulerException e) {
			throw new PluginException("Starting plugin iceOasisWatcher fail", e);
		}
		logger.info("Start plugin iceOasisWatcher success");
		return "IceOasisWatcher started";
	}

	@Override
	public String stop() throws PluginException {
		logger.info("Stopping plugin iceOasisWatcher");
		Map<String, String> context = new HashMap<String, String>();
		context.put("mode", "cron");

		JobDetail watchJobDetail = createWatchJobDetail(context);
		try {
			if (this.scheduler.checkExists(watchJobDetail.getKey())) {
				this.scheduler.deleteJob(watchJobDetail.getKey());
			}
		} catch (SchedulerException e) {
			throw new PluginException("Stopping plugin iceOasisWatcher fail", e);
		}
		logger.info("Stop plugin iceOasisWatcher success");
		return "IceOasisWatcher stopped";
	}

	public void watchIceOasis(Map<String, String> context) throws PluginException {
		logger.info("Watching IceOasis plugin");
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<String> executionLog = new ArrayList<String>();
		executionLog.add("Context=" + context.toString());
		executionLog.add("Timestamp=" + new Timestamp(System.currentTimeMillis()));

		List<String> htmlSchedules = getIceOasisHtmlSchedule(executionLog, context);
		List<DayIO> schedule = new ArrayList<DayIO>();
		for (String htmlSchedule : htmlSchedules) {
			List<DayIO> schedulePerMonth = parseHtml(htmlSchedule, executionLog, context);
			schedule.addAll(schedulePerMonth);
		}
//		logger.info("IceOasis schedule: " + schedule);

		for (Config config : this.configs) {
			watchIceOasisForEvent(config.getEventName(), config.getBefore(), config.getAfter(),
					config.getPhoneNumbers(), schedule, executionLog, context);
		}
		this.lastExecutionLog = executionLog;
		logger.info("Watch IceOasis success in {} ms ", stopwatch.elapsed(TimeUnit.SECONDS));
	}

	private void watchIceOasisForEvent(String eventName, LocalTime before, LocalTime after, List<String> phoneNumbers,
			List<DayIO> schedule, List<String> executionLog, Map<String, String> context) {
		List<DayIO> filteredSchedule = filterOpenScheduleByNameAndTime(eventName, before, after, schedule, executionLog,
				context);
		notifyAboutEvents(eventName, filteredSchedule, phoneNumbers, executionLog, context);
	}

	private List<DayIO> filterOpenScheduleByNameAndTime(String name, LocalTime before, LocalTime after,
			List<DayIO> sourceSchedule, List<String> executionLog, Map<String, String> context) {
		List<DayIO> filteredSchedule = new ArrayList<DayIO>();
		int filteredNumberOfSessions = 0;

		for (DayIO day : sourceSchedule) {
			DayIO newDay = new DayIO(day.getDate());
			for (SessionIO session : day.getSessions()) {
				LocalTime sessionStartTime = parseStartTime(session.getTime());
				if (name.equalsIgnoreCase(session.getName().trim())
						&& !SOLD_OUT.equalsIgnoreCase(session.getOpenings().trim())
						&& ((before == null || sessionStartTime.isBefore(before))
								|| (after == null || sessionStartTime.isAfter(after)))) {
					newDay.getSessions().add(session);
					filteredNumberOfSessions++;
				}
			}
			if (!newDay.getSessions().isEmpty()) {
				filteredSchedule.add(newDay);
			}
		}
		executionLog.add("FilterOpenSchedule=Success: Filtered schedule for name " + name + ": "
				+ filteredSchedule.size() + " days, " + filteredNumberOfSessions + " sessions");
		logger.info("Filtered schedule for name {}: {} days, {} sessions", name, filteredSchedule.size(),
				filteredNumberOfSessions);
		return filteredSchedule;
	}

	public LocalTime parseStartTime(String timeAsString) {
		StringTokenizer startTimeWithAPTokanizer = new StringTokenizer(timeAsString, " :");
		String hoursAsString = (String) startTimeWithAPTokanizer.nextElement();
		String minutesAsString = (String) startTimeWithAPTokanizer.nextElement();
		String ap = (String) startTimeWithAPTokanizer.nextElement();
//		logger.info("Start Time: {} {} {}", hoursAsString, minutesAsString, ap);

		int hours = Integer.parseInt(hoursAsString);
		int minutes = Integer.parseInt(minutesAsString);

		if ("P".equalsIgnoreCase(ap) && hours != 12) {
			hours = hours + 12;
		}

		LocalTime startTime = LocalTime.of(hours, minutes, 0, 0);
		return startTime;
	}

	private void notifyAboutEvents(String eventName, List<DayIO> schedule, List<String> phoneNumbers,
			List<String> executionLog, Map<String, String> context) {
		logger.info("Notifing about event {}, schedule {}", eventName, schedule);
		if (schedule.isEmpty()) {
			executionLog.add("NotificationSMS=There is nothing to notify about event " + eventName);
			logger.info("There is nothing to notify about event {}", eventName);
		} else {
			List<String> sessions = getSessions(schedule);
			List<String> sessionsToNotify = new ArrayList<String>();
			for (String sessionId : sessions) {
				if (!this.notifications.containsKey(sessionId)) {
					sessionsToNotify.add(sessionId);
				}
			}
			if (sessionsToNotify.isEmpty()) {
				executionLog.add("NotificationSMS=Notifications for all " + sessions.size() + " events " + eventName
						+ " sent before");
				logger.info("Notifications for all {} events {} sent before", sessions.size(), eventName);
			} else {
				notifyViaSMS(eventName, sessionsToNotify, phoneNumbers, executionLog, context);
				for (String sessionId : sessionsToNotify) {
					this.notifications.put(sessionId, "");
					this.notificationsToDisplay.add(sessionId);
				}
			}
		}
	}

	private List<String> getSessions(List<DayIO> schedule) {
		List<String> sessions = new ArrayList<String>();
		for (DayIO dayIO : schedule) {
			for (SessionIO sessionIO : dayIO.getSessions()) {
				sessions.add(dayIO.getDate() + " :: " + sessionIO.getTime() + " :: " + sessionIO.getName());
			}
		}
		return sessions;
	}

	private void notifyViaSMS(String eventName, List<String> sessions, List<String> phoneNumbers,
			List<String> executionLog, Map<String, String> context) {
		logger.info("Notifing via SMS {}, sessions {}", eventName, sessions);
		StringBuilder message = new StringBuilder();
		if ("test".equalsIgnoreCase(context.get("mode"))) {
			message.append("TEST ");
		}
		message.append("HockeyEngine Linesman found ");
		message.append(sessions.size());
		message.append(" new events ");
		message.append(eventName);
//		message.append(": ");
//		message.append(sessions);
		message.append(". Schedule at ");
		message.append(this.scheduleUrls.get(0));

		for (String phoneNumber : phoneNumbers) {
			sendSMSMessage(message.toString(), phoneNumber);
		}
		executionLog.add("NotificationSMS=Success: " + sessions.size() + " new events " + phoneNumbers);
	}

	public void sendSMSMessage(String message, String phoneNumber) {
		Map<String, MessageAttributeValue> smsAttributes = new HashMap<String, MessageAttributeValue>();
		smsAttributes.put("AWS.SNS.SMS.SenderID",
				new MessageAttributeValue().withStringValue("Linesman").withDataType("String"));
		smsAttributes.put("AWS.SNS.SMS.MaxPrice",
				new MessageAttributeValue().withStringValue("1.00").withDataType("Number"));
		smsAttributes.put("AWS.SNS.SMS.SMSType",
				new MessageAttributeValue().withStringValue("Transactional").withDataType("String"));
		PublishResult result = this.snsClient.publish(new PublishRequest().withMessage(message)
				.withPhoneNumber(phoneNumber).withMessageAttributes(smsAttributes));
		logger.info("Send SMS message result: {}", result);
	}

	private List<String> getIceOasisHtmlSchedule(List<String> executionLog, Map<String, String> context)
			throws PluginException {
		List<String> htmls = new ArrayList<String>();
		Stopwatch stopwatch = Stopwatch.createStarted();
		try {
			for (String scheduleUrl : this.scheduleUrls) {
				String html = getIceOasisHtmlScheduleFromURL(scheduleUrl, executionLog, context);
				if (!StringUtils.isEmpty(html)) {
					htmls.add(html);
				}
			}
			logger.info("Getting IceOasis schedule success in {} ms ", stopwatch.elapsed(TimeUnit.SECONDS));
			return htmls;
		} catch (Exception e) {
			throw new PluginException("Getting IceOasis schedule from URL fail", e);
		}
	}

	private String getIceOasisHtmlScheduleFromURL(String url, List<String> executionLog, Map<String, String> context)
			throws PluginException {
		try {
			Stopwatch stopwatch = Stopwatch.createStarted();
			String html = restTemplate.getForObject(url, String.class);
			logger.info("Getting IceOasis schedule from URL {} success in {} ms ", url,
					stopwatch.elapsed(TimeUnit.SECONDS));
			executionLog.add("GetIceOasisScheduleFromURL=Success");
			return html;
		} catch (Exception e) {
			executionLog.add("GetIceOasisScheduleFromURL=Error");
			throw new PluginException("Getting IceOasis schedule from URL fail", e);
		}
	}

	private List<DayIO> parseHtml(String html, List<String> executionLog, Map<String, String> context) {
		List<DayIO> days = new ArrayList<DayIO>();
		Document doc = Jsoup.parse(html);
		Elements tableBody = doc.select("tbody");

		DayIO dayIO = null;
		int totalNumberOfSessions = 0;
		for (Element tableRow : tableBody.first().select("tr")) {
			Elements tableData = tableRow.select("td");
			if (tableData.size() == 1) {
				// Table row is one liner
				// Checking if table row is day header or no sessions message
				Elements elements = tableData.get(0).getElementsByClass("alert alert-info");
				if (!elements.isEmpty()) {
					// Table row is day header
					String date = tableData.get(0).text();
					dayIO = new DayIO(date);
					days.add(dayIO);
//					logger.info("DayIO: " + dayIO);
				} else {
					// Table row is no sessions message
				}
			} else {
				// Table row is session 
				String time = tableData.get(1).text();
				String openings = tableData.get(2).text();
				String name = tableData.get(3).text();
				String length = tableData.get(4).text();
				String price = tableData.get(6).text();
				SessionIO sessionIO = new SessionIO(time, openings, name, length, price);
				totalNumberOfSessions++;
				dayIO.getSessions().add(sessionIO);
//				logger.info("SessionIO: " + sessionIO);
			}
			
			
//			Elements elements = tableData.get(0).getElementsByClass("alert alert-info");
//			if (elements.isEmpty()) {
//				String time = tableData.get(1).text();
//				String openings = tableData.get(2).text();
//				String name = tableData.get(3).text();
//				String length = tableData.get(4).text();
//				String price = tableData.get(6).text();
//				SessionIO sessionIO = new SessionIO(time, openings, name, length, price);
//				totalNumberOfSessions++;
//				dayIO.getSessions().add(sessionIO);
////				logger.info("SessionIO: " + sessionIO);
//			} else {
//				String date = tableData.get(0).text();
//				dayIO = new DayIO(date);
//				days.add(dayIO);
////				logger.info("DayIO: " + dayIO);
//			}
		}
		executionLog.add("ParseHTML=Success: Found schedule for " + days.size() + " days, " + totalNumberOfSessions
				+ " sessions in total");
		logger.info("Found schedule for {} days, {} sessions in total", days.size(), totalNumberOfSessions);
		return days;
	}

	private JobDetail createWatchJobDetail(Map<String, String> context) {
		JobDetail watchJobDetail = JobBuilder.newJob(WatchJob.class).withIdentity("WatchJob1", "group1").build();
		watchJobDetail.getJobDataMap().put("plugin", this);
		watchJobDetail.getJobDataMap().put("context", context);
		return watchJobDetail;
	}

	class Config {

		private String eventName;

		private LocalTime before;

		private LocalTime after;

		private List<String> phoneNumbers;

		public String getEventName() {
			return eventName;
		}

		public LocalTime getBefore() {
			return before;
		}

		public LocalTime getAfter() {
			return after;
		}

		public List<String> getPhoneNumbers() {
			return phoneNumbers;
		}

		public Config(String eventName, LocalTime before, LocalTime after, List<String> phoneNumbers) {
			super();
			this.eventName = eventName;
			this.before = before;
			this.after = after;
			this.phoneNumbers = phoneNumbers;
		}

		@Override
		public String toString() {
			return "Config [eventName=" + eventName + ", before=" + before + ", after=" + after + ", phoneNumbers="
					+ phoneNumbers + "]";
		}

	}

}
