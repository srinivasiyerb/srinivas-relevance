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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */

package org.olat.commons.info.ui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.olat.commons.info.manager.InfoMessageFrontendManager;
import org.olat.commons.info.manager.MailFormatter;
import org.olat.commons.info.model.InfoMessage;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.date.DateComponentFactory;
import org.olat.core.gui.components.date.DateElement;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.control.generic.wizard.Step;
import org.olat.core.gui.control.generic.wizard.StepRunnerCallback;
import org.olat.core.gui.control.generic.wizard.StepsMainRunController;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.nodes.info.InfoCourseNodeConfiguration;
import org.olat.modules.ModuleConfiguration;

import com.ibm.icu.util.Calendar;

/**
 * Description:<br>
 * Controller which display the info messages from an OLATResourceable
 * <P>
 * Initial Date: 26 jul. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InfoDisplayController extends FormBasicController {

	private Step start;
	private FormLink newInfoLink;
	private FormLink oldMsgsLink;
	private FormLink newMsgsLink;
	private final List<FormLink> editLinks = new ArrayList<FormLink>();
	private final List<FormLink> deleteLinks = new ArrayList<FormLink>();
	private StepsMainRunController newInfoWizard;
	private DialogBoxController confirmDelete;
	private InfoEditController editController;
	private CloseableModalController editDialogBox;

	private final List<Long> previousDisplayKeys = new ArrayList<Long>();
	private final InfoSecurityCallback secCallback;
	private final OLATResourceable ores;
	private final String resSubPath;
	private final String businessPath;

	private int maxResults = 0;
	private int maxResultsConfig = 0;
	private int duration = -1;
	private Date after = null;
	private Date afterConfig = null;

	private final InfoMessageFrontendManager infoMessageManager;

	private LockResult lockEntry;
	private MailFormatter sendMailFormatter;
	private final List<SendMailOption> sendMailOptions = new ArrayList<SendMailOption>();

	public InfoDisplayController(final UserRequest ureq, final WindowControl wControl, final ModuleConfiguration config, final InfoSecurityCallback secCallback,
			final OLATResourceable ores, final String resSubPath, final String businessPath) {
		super(ureq, wControl, "display");
		this.secCallback = secCallback;
		this.ores = ores;
		this.resSubPath = resSubPath;
		this.businessPath = businessPath;

		infoMessageManager = InfoMessageFrontendManager.getInstance();
		maxResults = maxResultsConfig = getConfigValue(config, InfoCourseNodeConfiguration.CONFIG_LENGTH, 10);
		duration = getConfigValue(config, InfoCourseNodeConfiguration.CONFIG_DURATION, 90);

		if (duration > 0) {
			final Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			cal.add(Calendar.DATE, -duration);
			after = afterConfig = cal.getTime();
		}

		initForm(ureq);
		loadMessages();
	}

	private int getConfigValue(final ModuleConfiguration config, final String key, final int def) {
		final String durationStr = (String) config.get(key);
		if ("\u221E".equals(durationStr)) {
			return -1;
		} else if (StringHelper.containsNonWhitespace(durationStr)) {
			try {
				return Integer.parseInt(durationStr);
			} catch (final NumberFormatException e) { /* fallback to default */}
		}
		return def;
	}

	public List<SendMailOption> getSendMailOptions() {
		return this.sendMailOptions;
	}

	public void addSendMailOptions(final SendMailOption sendMailOption) {
		sendMailOptions.add(sendMailOption);
	}

	public MailFormatter getSendMailFormatter() {
		return sendMailFormatter;
	}

	public void setSendMailFormatter(final MailFormatter sendMailFormatter) {
		this.sendMailFormatter = sendMailFormatter;
	}

	/**
	 * This is the main method which push the messages in the layout container, and clean-up old links.
	 */
	protected void loadMessages() {
		// first clear the current message if any
		for (final Long key : previousDisplayKeys) {
			flc.contextRemove("info.date." + key);
			if (flc.getComponent("info.delete." + key) != null) {
				flc.remove("info.delete." + key);
			}
			if (flc.getComponent("info.edit." + key) != null) {
				flc.remove("info.edit." + key);
			}
		}
		previousDisplayKeys.clear();
		deleteLinks.clear();

		final List<InfoMessage> msgs = infoMessageManager.loadInfoMessageByResource(ores, resSubPath, businessPath, after, null, 0, maxResults);
		final List<InfoMessageForDisplay> infoDisplays = new ArrayList<InfoMessageForDisplay>();
		for (final InfoMessage info : msgs) {
			previousDisplayKeys.add(info.getKey());
			infoDisplays.add(createInfoMessageForDisplay(info));

			final String dateCmpName = "info.date." + info.getKey();
			final DateElement dateEl = DateComponentFactory.createDateElementWithYear(dateCmpName, info.getCreationDate());
			flc.add(dateCmpName, dateEl);

			if (secCallback.canEdit()) {
				final String editName = "info.edit." + info.getKey();
				final FormLink link = uifactory.addFormLink(editName, "edit", "edit", flc, Link.BUTTON);
				link.setUserObject(info);
				editLinks.add(link);
				flc.add(link);
			}
			if (secCallback.canDelete()) {
				final String delName = "info.delete." + info.getKey();
				final FormLink link = uifactory.addFormLink(delName, "delete", "delete", flc, Link.BUTTON);
				link.setUserObject(info);
				deleteLinks.add(link);
				flc.add(link);
			}
		}
		flc.contextPut("infos", infoDisplays);

		final int numOfInfos = infoMessageManager.countInfoMessageByResource(ores, resSubPath, businessPath, null, null);
		oldMsgsLink.setVisible((msgs.size() < numOfInfos));
		newMsgsLink.setVisible((msgs.size() == numOfInfos) && (numOfInfos > maxResultsConfig) && (maxResultsConfig > 0));
	}

	private InfoMessageForDisplay createInfoMessageForDisplay(final InfoMessage info) {
		String message = "";
		if (StringHelper.containsNonWhitespace(info.getMessage())) {
			message = Formatter.escWithBR(info.getMessage()).toString();
		}

		final DateFormat formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, getLocale());

		String modifier = null;
		if (info.getModifier() != null) {
			final User user = info.getModifier().getUser();
			final String formattedName = user.getProperty(UserConstants.FIRSTNAME, null) + " " + user.getProperty(UserConstants.LASTNAME, null);
			final String creationDate = formatter.format(info.getModificationDate());
			modifier = translate("display.modifier", new String[] { formattedName, creationDate });
		}

		final User author = info.getAuthor().getUser();
		final String authorName = author.getProperty(UserConstants.FIRSTNAME, null) + " " + author.getProperty(UserConstants.LASTNAME, null);

		final String creationDate = formatter.format(info.getCreationDate());
		final String infos = translate("display.info", new String[] { authorName, creationDate });

		return new InfoMessageForDisplay(info.getKey(), info.getTitle(), message, infos, modifier);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		if (secCallback.canAdd()) {
			newInfoLink = uifactory.addFormLink("new_message", "new_message", "new_message", formLayout, Link.BUTTON);
		}

		oldMsgsLink = uifactory.addFormLink("display.old_messages", "display.old_messages", "display.old_messages", formLayout, Link.BUTTON);
		newMsgsLink = uifactory.addFormLink("display.new_messages", "display.new_messages", "display.new_messages", formLayout, Link.BUTTON);
	}

	@Override
	protected void doDispose() {
		if (lockEntry != null) {
			CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lockEntry);
			lockEntry = null;
		}
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		//
	}

	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == newInfoWizard) {
			if (event == Event.CANCELLED_EVENT) {
				getWindowControl().pop();
			} else if (event == Event.CHANGED_EVENT) {
				getWindowControl().pop();
				loadMessages();
				flc.setDirty(true);// update the view
			} else if (event == Event.DONE_EVENT) {
				showError("failed");
			}
		} else if (source == confirmDelete) {
			if (DialogBoxUIFactory.isYesEvent(event)) {
				final InfoMessage msgToDelete = (InfoMessage) confirmDelete.getUserObject();
				infoMessageManager.deleteInfoMessage(msgToDelete);
				loadMessages();
			}
			confirmDelete.setUserObject(null);

			// release lock
			CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lockEntry);
			lockEntry = null;
		} else if (source == editController) {
			if (event == Event.DONE_EVENT) {
				loadMessages();
			}
			editDialogBox.deactivate();
			removeAsListenerAndDispose(editController);
			editDialogBox = null;
			editController = null;

			// release lock
			CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lockEntry);
			lockEntry = null;
		} else if (source == editDialogBox) {
			// release lock if the dialog is closed
			CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lockEntry);
			lockEntry = null;
		} else {
			super.event(ureq, source, event);
		}
	}

	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		if (source == newInfoLink) {
			start = new CreateInfoStep(ureq, sendMailOptions);
			newInfoWizard = new StepsMainRunController(ureq, getWindowControl(), start, new FinishedCallback(), new CancelCallback(), translate("create_message"));
			listenTo(newInfoWizard);
			getWindowControl().pushAsModalDialog(newInfoWizard.getInitialComponent());
		} else if (deleteLinks.contains(source)) {
			final InfoMessage msg = (InfoMessage) source.getUserObject();
			popupDelete(ureq, msg);
		} else if (editLinks.contains(source)) {
			final InfoMessage msg = (InfoMessage) source.getUserObject();
			popupEdit(ureq, msg);
		} else if (source == oldMsgsLink) {
			maxResults = -1;
			after = null;
			loadMessages();
		} else if (source == newMsgsLink) {
			maxResults = maxResultsConfig;
			after = afterConfig;
			loadMessages();
		} else {
			super.formInnerEvent(ureq, source, event);
		}
	}

	protected void popupDelete(final UserRequest ureq, InfoMessage msg) {
		final OLATResourceable mres = OresHelper.createOLATResourceableInstance(InfoMessage.class, msg.getKey());
		lockEntry = CoordinatorManager.getInstance().getCoordinator().getLocker().acquireLock(mres, ureq.getIdentity(), "");
		if (lockEntry.isSuccess()) {
			// locked -> reload the message
			msg = infoMessageManager.loadInfoMessage(msg.getKey());
			if (msg == null) {
				showWarning("already.deleted");
				CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lockEntry);
				lockEntry = null;
				loadMessages();
			} else {
				final String confirmDeleteText = translate("edit.confirm_delete", new String[] { msg.getTitle() });
				confirmDelete = activateYesNoDialog(ureq, null, confirmDeleteText, confirmDelete);
				confirmDelete.setUserObject(msg);
			}
		} else {
			final User user = lockEntry.getOwner().getUser();
			final String name = user.getProperty(UserConstants.FIRSTNAME, null) + " " + user.getProperty(UserConstants.LASTNAME, null);
			showWarning("already.edited", name);
		}
	}

	protected void popupEdit(final UserRequest ureq, InfoMessage msg) {
		final OLATResourceable mres = OresHelper.createOLATResourceableInstance(InfoMessage.class, msg.getKey());
		lockEntry = CoordinatorManager.getInstance().getCoordinator().getLocker().acquireLock(mres, ureq.getIdentity(), "");
		if (lockEntry.isSuccess()) {
			msg = infoMessageManager.loadInfoMessage(msg.getKey());
			if (msg == null) {
				showWarning("already.deleted");
				CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lockEntry);
				lockEntry = null;
				loadMessages();
			} else {
				removeAsListenerAndDispose(editController);
				removeAsListenerAndDispose(editDialogBox);
				editController = new InfoEditController(ureq, getWindowControl(), msg);
				listenTo(editController);
				editDialogBox = new CloseableModalController(getWindowControl(), translate("edit"), editController.getInitialComponent());
				editDialogBox.activate();
				listenTo(editDialogBox);
			}
		} else {
			final User user = lockEntry.getOwner().getUser();
			final String name = user.getProperty(UserConstants.FIRSTNAME, null) + " " + user.getProperty(UserConstants.LASTNAME, null);
			showWarning("already.edited", name);
		}
	}

	protected class FinishedCallback implements StepRunnerCallback {
		@Override
		public Step execute(final UserRequest ureq, final WindowControl wControl, final StepsRunContext runContext) {

			final String title = (String) runContext.get(WizardConstants.MSG_TITLE);
			final String message = (String) runContext.get(WizardConstants.MSG_MESSAGE);
			final Set<String> selectedOptions = (Set<String>) runContext.get(WizardConstants.SEND_MAIL);

			final InfoMessage msg = infoMessageManager.createInfoMessage(ores, resSubPath, businessPath, ureq.getIdentity());
			msg.setTitle(title);
			msg.setMessage(message);

			final List<Identity> identities = new ArrayList<Identity>();
			for (final SendMailOption option : sendMailOptions) {
				if (selectedOptions != null && selectedOptions.contains(option.getOptionKey())) {
					identities.addAll(option.getSelectedIdentities());
				}
			}

			infoMessageManager.sendInfoMessage(msg, sendMailFormatter, ureq.getLocale(), identities);
			return StepsMainRunController.DONE_MODIFIED;
		}
	}

	protected class CancelCallback implements StepRunnerCallback {
		@Override
		public Step execute(final UserRequest ureq, final WindowControl wControl, final StepsRunContext runContext) {
			return Step.NOSTEP;
		}
	}
}
