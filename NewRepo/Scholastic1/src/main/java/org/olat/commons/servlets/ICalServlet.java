/**
 * OLAT - Online Learning and Training<br>
 * http://www.olat.org
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.commons.servlets;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.ICalTokenGenerator;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.dispatcher.DispatcherAction;
import org.olat.core.logging.Tracing;

/**
 * Description:<BR>
 * Servlet that serves the ical document.
 * <P>
 * Initial Date: June 1, 2008
 * 
 * @author Udit Sajjanhar
 */
public class ICalServlet extends HttpServlet {
	private static int outputBufferSize = 2048;
	private static int inputBufferSize = 2048;

	/**
	 * Default constructor.
	 */
	public ICalServlet() {}

	/**
	 * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(final ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		Tracing.logInfo("init statics servlet", ICalServlet.class);
		try {
			String bufSize = servletConfig.getInitParameter("input");
			inputBufferSize = Integer.parseInt(bufSize);
			bufSize = servletConfig.getInitParameter("output");
			inputBufferSize = Integer.parseInt(bufSize);
		} catch (final Exception e) {
			Tracing.logWarn("problem with config parameters for ical servlets:", e, ICalServlet.class);
		}
		Tracing.logInfo("input buffer size: " + inputBufferSize, ICalServlet.class);
		Tracing.logInfo("output buffer size: " + inputBufferSize, ICalServlet.class);
	}

	/**
	 * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		Tracing.setUreq(req);
		final String method = req.getMethod();
		try {
			if (method.equals("GET")) {
				doGet(req, resp);
			} else {
				super.service(req, resp);
			}
		} finally {
			// consume the userrequest.
			Tracing.setUreq(null);
		}
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {
		Calendar icalDoc = null;

		ServletOutputStream ostream = null;
		try {
			final String pathInfo = request.getPathInfo();
			Tracing.logDebug("doGet pathInfo=" + pathInfo, ICalServlet.class);
			if ((pathInfo == null) || (pathInfo.equals(""))) { return; // error
			}

			if (checkPath(pathInfo)) {
				icalDoc = getIcalDocument(pathInfo);
				if (icalDoc == null) {
					DispatcherAction.sendNotFound(pathInfo, response);
					return;
				}
			} else {
				DispatcherAction.sendNotFound(pathInfo, response);
				return;
			}

			// OLAT-5243 related: sending back the reply can take arbitrary long,
			// considering slow end-user connections for example - or a sudden death of the connection
			// on the client-side which remains unnoticed (network partitioning)
			DBFactory.getInstance().intermediateCommit();

			// get the output stream
			response.setBufferSize(outputBufferSize);
			ostream = response.getOutputStream();

			// output the calendar to the stream
			final CalendarOutputter calOut = new CalendarOutputter(false);
			calOut.output(icalDoc, ostream);
		} catch (final ValidationException e) {
			// throw olat exception for nice logging
			Tracing.logWarn("Validation Error when generate iCal stream for path::" + request.getPathInfo(), e, this.getClass());
			DispatcherAction.sendNotFound("none", response);
		} catch (final IOException e) {
			// throw olat exception for nice logging
			Tracing.logWarn("IOException Error when generate iCal stream for path::" + request.getPathInfo(), e, this.getClass());
			DispatcherAction.sendNotFound("none", response);
		} catch (final Exception e) {
			Tracing.logWarn("Unknown Error in icalservlet", e, this.getClass());
			DispatcherAction.sendNotFound("none", response);
		} finally {
			try {
				ostream.close();
			} catch (final Exception e) {
				// ignore
			}

			DBFactory.getInstance(false).commitAndCloseSession();
		}
	}

	/**
	 * Checks the path information to match the prefixs in ICalTokenGenerator.ICAL_PREFIX_COLLECTION
	 * 
	 * @param icalFeedPath
	 * @return boolean
	 */
	private boolean checkPath(final String icalFeedPath) {
		// pathInfo is like /user/<user_name>/AUTH_TOKEN.ics
		// /group/<user_name>/AUTH_TOKEN/<group_id>.ics
		// /course/<user_name>/AUTH_TOKEN/<course_unique_id>.ics

		// check the type of calendar
		boolean calendarTypeMatched = false;
		for (int prefixIndex = 0; prefixIndex < ICalTokenGenerator.ICAL_PREFIX_COLLECTION.length; prefixIndex++) {
			if (icalFeedPath.indexOf(ICalTokenGenerator.ICAL_PREFIX_COLLECTION[prefixIndex]) == 0) {
				calendarTypeMatched = true;
			}
		}
		if (!calendarTypeMatched) { return false; }

		// check the number of tokens in the icalFeedPath
		final int numberOfTokens = icalFeedPath.split("/").length - ICalTokenGenerator.ICAL_PATH_SHIFT;
		if (isRequestForPersonalCalendarFeed(icalFeedPath)) {
			return (numberOfTokens == ICalTokenGenerator.ICAL_PERSONAL_PATH_TOKEN_LENGTH);
		} else {
			return (numberOfTokens == ICalTokenGenerator.ICAL_PATH_TOKEN_LENGTH);
		}
	}

	/**
	 * checks whether the iCal feed request is for a personal calendar
	 * 
	 * @param icalFeedPath
	 * @return boolean
	 */
	private boolean isRequestForPersonalCalendarFeed(final String icalFeedPath) {
		if (icalFeedPath.indexOf(ICalTokenGenerator.ICAL_PREFIX_PERSONAL) != 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Reads in the appropriate ics file, depending upon the pathInfo
	 * 
	 * @param pathInfo
	 * @return Calendar
	 */
	private Calendar getIcalDocument(String pathInfo) {
		// pathInfo is like /user/<user_name>/AUTH_TOKEN.ics
		// /group/<user_name>/AUTH_TOKEN/<group_id>.ics
		// /course/<user_name>/AUTH_TOKEN/<course_unique_id>.ics

		// get the individual path tokens
		pathInfo = pathInfo.replaceAll(".ics", "");
		final String[] pathInfoTokens = pathInfo.split("/");
		final String calendarType = pathInfoTokens[0 + ICalTokenGenerator.ICAL_PATH_SHIFT];
		final String userName = pathInfoTokens[1 + ICalTokenGenerator.ICAL_PATH_SHIFT];
		final String authToken = pathInfoTokens[2 + ICalTokenGenerator.ICAL_PATH_SHIFT];
		String calendarID = userName;
		if (!isRequestForPersonalCalendarFeed(pathInfo)) {
			calendarID = pathInfoTokens[3 + ICalTokenGenerator.ICAL_PATH_SHIFT];
		}

		// check the authentication token
		if (!checkPathAuthenticity(calendarType, userName, authToken, calendarID)) {
			Tracing.logWarn("Authenticity Check failed for the ical feed path: " + pathInfo, this.getClass());
			return null;
		}

		final CalendarManager calendarManager = CalendarManagerFactory.getInstance().getCalendarManager();

		// check if the calendar exists (calendars are only persisted when an event is created)
		if (calendarManager.calendarExists(calendarType, calendarID)) {
			// read and return the calendar file
			return calendarManager.readCalendar(calendarType, calendarID);
		} else {
			// return an empty calendar file
			return new Calendar();
		}

	}

	/**
	 * checks the AUTH_TOKEN in the iCal feed path
	 * 
	 * @param type Type of calendar, i.e. User, Group or Course
	 * @param userName Name of the User
	 * @param authToken Authentication token for the calendar
	 * @param icsFileName Name of the ics file
	 * @return boolean
	 */
	private boolean checkPathAuthenticity(final String calendarType, final String userName, final String authToken, final String calendarID) {

		// get the authentication token stored in the database
		final String authTokenFromDb = ICalTokenGenerator.getIcalAuthToken(calendarType, calendarID, userName, false);

		// check if the token from db matches the token in the url
		if (authTokenFromDb == null) {
			return false;
		} else {
			return authTokenFromDb.equals(authToken);
		}
	}

}
