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

package org.olat.commons.calendar.ui;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.ImportCalendarManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.olat.core.util.WebappHelper;

import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

/**
 * Description:<BR>
 * <P>
 * Initial Date: July 8, 2008
 * 
 * @author Udit Sajjanhar
 */
public class CalendarFileUploadController extends BasicController {

	private static final String PACKAGE = Util.getPackageName(CalendarManager.class);
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(CalendarManager.class);

	private final VelocityContainer calFileUploadVC;
	private final Translator translator;
	private static final String COMMAND_PROCESS_UPLOAD = "pul";
	private static final long fileUploadLimit = 1024;
	private CalendarImportNameForm nameForm;
	private final Link cancelButton;

	CalendarFileUploadController(final UserRequest ureq, final Locale locale, final WindowControl wControl) {
		super(ureq, wControl);

		translator = new PackageTranslator(PACKAGE, locale);
		calFileUploadVC = new VelocityContainer("calmanage", VELOCITY_ROOT + "/calFileUpload.html", translator, this);
		cancelButton = LinkFactory.createButton("cancel", calFileUploadVC, this);
		putInitialPanel(calFileUploadVC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == cancelButton) {
			fireEvent(ureq, Event.CANCELLED_EVENT);
		} else if (source == calFileUploadVC) { // those must be module links
			if (event.getCommand().equals(COMMAND_PROCESS_UPLOAD)) {
				// process calendar file upload
				processCalendarFileUpload(ureq);
			}
		}
	}

	private void processCalendarFileUpload(final UserRequest ureq) {
		// upload the file
		try {
			// don't worry about NullPointerExceptions.
			// we'll catch exceptions if any operation fails.
			final MultipartParser mpp = new MultipartParser(ureq.getHttpReq(), (int) fileUploadLimit * 1024);
			mpp.setEncoding("UTF-8");
			Part part;
			boolean fileWritten = false;
			while ((part = mpp.readNextPart()) != null) {
				if (part.isFile() && !fileWritten) {
					final FilePart fPart = (FilePart) part;
					final String type = fPart.getContentType();
					// get file contents
					Tracing.logWarn(type + fPart.getFileName(), this.getClass());
					if (fPart != null && fPart.getFileName() != null && type.startsWith("text") && (type.toLowerCase().endsWith("calendar"))) {

						// store the uploaded file by a temporary name
						final CalendarManager calManager = CalendarManagerFactory.getInstance().getCalendarManager();
						final String calID = ImportCalendarManager.getTempCalendarIDForUpload(ureq);
						final File tmpFile = calManager.getCalendarFile(CalendarManager.TYPE_USER, calID);
						fPart.writeTo(tmpFile);

						// try to parse the tmp file
						final Object calendar = calManager.readCalendar(CalendarManager.TYPE_USER, calID);
						if (calendar != null) {
							fileWritten = true;
						}

						// the uploaded calendar file is ok.
						fireEvent(ureq, Event.DONE_EVENT);
					}
				} else if (part.isParam()) {
					final ParamPart pPart = (ParamPart) part;
					if (pPart.getName().equals("cancel")) {
						// action cancelled
						fireEvent(ureq, Event.CANCELLED_EVENT);
					}
				}
			}

			if (!fileWritten) {
				getWindowControl().setError(translator.translate("cal.import.form.format.error"));
			}

		} catch (final IOException ioe) {
			// exceeded UL limit
			Tracing.logWarn("IOException in CalendarFileUploadController: ", ioe, this.getClass());
			final String slimitKB = String.valueOf(fileUploadLimit);
			final String supportAddr = WebappHelper.getMailConfig("mailSupport");// ->{0} für e-mail support e-mail adresse
			getWindowControl().setError(translator.translate("cal.import.form.limit.error", new String[] { slimitKB, supportAddr }));
			return;
		} catch (final OLATRuntimeException e) {
			Tracing.logWarn("Imported Calendar file not correct. Parsing failed.", e, this.getClass());
			getWindowControl().setError(translator.translate("cal.import.parsing.failed"));
			return;
		} catch (final Exception e) {
			Tracing.logWarn("Exception in CalendarFileUploadController: ", e, this.getClass());
			getWindowControl().setError(translator.translate("cal.import.form.failed"));
			return;
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// do nothing here yet
	}

}
