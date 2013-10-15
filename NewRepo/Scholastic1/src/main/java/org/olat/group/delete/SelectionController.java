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
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.group.delete;

import java.util.List;

import org.apache.velocity.VelocityContext;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.IntegerElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.StaticColumnDescriptor;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.table.TableMultiSelectEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Util;
import org.olat.core.util.mail.MailNotificationEditController;
import org.olat.core.util.mail.MailTemplate;
import org.olat.group.BusinessGroup;
import org.olat.group.delete.service.GroupDeletionManager;
import org.olat.group.ui.BGTranslatorFactory;
import org.olat.group.ui.main.BGMainController;

/**
 * Controller for tab 'Learning-resource selection'
 * 
 * @author Christian Guretzki
 */
public class SelectionController extends BasicController {
	private static final String PACKAGE_BG_MAIN_CONTROLLER = Util.getPackageName(BGMainController.class);
	private static final String MY_PACKAGE = Util.getPackageName(SelectionController.class);

	private static final String ACTION_SINGLESELECT_CHOOSE = "ssc";
	private static final String ACTION_MULTISELECT_CHOOSE = "msc";
	private static final String KEY_EMAIL_SUBJECT = "delete.announcement.email.subject";
	private static final String KEY_EMAIL_BODY = "delete.announcement.email.body";

	private final VelocityContainer myContent;
	private final Panel deleteSelectionPanel;
	private SelectionForm selectionForm;
	private TableController tableCtr;
	private GroupDeleteTableModel redtm;
	private VelocityContainer selectionListContent;
	private Link editParameterLink;
	private MailNotificationEditController deleteGroupMailCtr;
	private List selectedGroups;
	private final PackageTranslator tableModelTypeTranslator;

	private CloseableModalController cmc;

	/**
	 * @param ureq
	 * @param wControl
	 * @param cancelbutton
	 */
	public SelectionController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		final PackageTranslator fallbackTrans = new PackageTranslator(PACKAGE_BG_MAIN_CONTROLLER, ureq.getLocale());
		this.setTranslator(new PackageTranslator(MY_PACKAGE, ureq.getLocale(), fallbackTrans));
		myContent = createVelocityContainer("panel");

		// used to translate the BusinessGroup.getType() String in the table model
		tableModelTypeTranslator = BGTranslatorFactory.createBGPackageTranslator(MY_PACKAGE, /* doesnt matter */BusinessGroup.TYPE_BUDDYGROUP, ureq.getLocale());

		deleteSelectionPanel = new Panel("deleteSelectionPanel");
		deleteSelectionPanel.addListener(this);
		myContent.put("panel", deleteSelectionPanel);
		initializeTableController(ureq);
		initializeContent();

		putInitialPanel(myContent);
	}

	private void initializeContent() {
		updateGroupList();
		selectionListContent = createVelocityContainer("selectionlist");
		selectionListContent.put("repositorylist", tableCtr.getInitialComponent());
		selectionListContent.contextPut("header",
				translate("selection.delete.header", new String[] { Integer.toString(GroupDeletionManager.getInstance().getLastUsageDuration()) }));
		editParameterLink = LinkFactory.createButtonXSmall("button.editParameter", selectionListContent, this);
		deleteSelectionPanel.setContent(selectionListContent);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == editParameterLink) {

			removeAsListenerAndDispose(selectionForm);
			selectionForm = new SelectionForm(ureq, getWindowControl());
			listenTo(selectionForm);

			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), selectionForm.getInitialComponent(), true, translate("edit.parameter.header"));
			listenTo(cmc);

			cmc.activate();
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == selectionForm) {
			if (event == Event.DONE_EVENT) {
				GroupDeletionManager.getInstance().setLastUsageDuration(selectionForm.getLastUsageDuration());
				GroupDeletionManager.getInstance().setDeleteEmailDuration(selectionForm.getDeleteEmailDuration());
				initializeContent();
			} else if (event == Event.CANCELLED_EVENT) {
				fireEvent(ureq, Event.CANCELLED_EVENT);
			}
			cmc.deactivate();

		} else if (source == tableCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				final TableEvent te = (TableEvent) event;
				if (te.getActionId().equals(ACTION_SINGLESELECT_CHOOSE)) {
					final int rowid = te.getRowId();
					GroupDeletionManager.getInstance().setLastUsageNowFor((BusinessGroup) redtm.getObject(rowid));
					updateGroupList();
				}
			} else if (event.getCommand().equals(Table.COMMAND_MULTISELECT)) {
				final TableMultiSelectEvent tmse = (TableMultiSelectEvent) event;
				if (tmse.getAction().equals(ACTION_MULTISELECT_CHOOSE)) {
					handleEmailButtonEvent(ureq, tmse);
				}
			}
			initializeContent();
		} else if (source == deleteGroupMailCtr) {
			if (event == Event.DONE_EVENT) {
				final String warningMessage = GroupDeletionManager.getInstance().sendDeleteEmailTo(selectedGroups, deleteGroupMailCtr.getMailTemplate(),
						deleteGroupMailCtr.isTemplateChanged(), KEY_EMAIL_SUBJECT, KEY_EMAIL_BODY, ureq.getIdentity(), (PackageTranslator) getTranslator());
				cmc.deactivate();
				if (deleteGroupMailCtr.getMailTemplate() != null) {
					// when mailtemplate is null, user decides to send no email => no status message
					if (warningMessage.length() > 0) {
						getWindowControl().setWarning(translate("delete.email.announcement.warning.header") + warningMessage);
					} else {
						showInfo("selection.feedback.msg");
					}
				}
				updateGroupList();

			} else if (event == Event.CANCELLED_EVENT) {
				cmc.deactivate();
			} else {
				throw new RuntimeException("unknown event ::" + event.getCommand());
			}
		}
	}

	private void handleEmailButtonEvent(final UserRequest ureq, final TableMultiSelectEvent tmse) {
		if (redtm.getObjects(tmse.getSelection()).size() != 0) {
			selectedGroups = redtm.getObjects(tmse.getSelection());

			final MailTemplate deleteMailTemplate = createMailTemplate(translate(KEY_EMAIL_SUBJECT), translate(KEY_EMAIL_BODY));
			deleteMailTemplate.addToContext("lastloginduration", Integer.toString(UserDeletionManager.getInstance().getLastLoginDuration()));
			deleteMailTemplate.addToContext("durationdeleteemail", Integer.toString(UserDeletionManager.getInstance().getDeleteEmailDuration()));

			removeAsListenerAndDispose(deleteGroupMailCtr);
			deleteGroupMailCtr = new MailNotificationEditController(getWindowControl(), ureq, deleteMailTemplate, true);
			listenTo(deleteGroupMailCtr);

			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), deleteGroupMailCtr.getInitialComponent());
			listenTo(cmc);

			cmc.activate();

		} else {
			showWarning("nothing.selected.msg");
		}
	}

	private void initializeTableController(final UserRequest ureq) {
		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(translate("error.no.repository.found"));

		removeAsListenerAndDispose(tableCtr);
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		listenTo(tableCtr);

		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.bgname", 0, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.description", 1, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.type", 2, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.lastusage", 3, null, ureq.getLocale()));
		tableCtr.addMultiSelectAction("action.delete.selection", ACTION_MULTISELECT_CHOOSE);
		tableCtr.addColumnDescriptor(new StaticColumnDescriptor(ACTION_SINGLESELECT_CHOOSE, "table.header.action", myContent.getTranslator().translate("action.activate")));
		tableCtr.setMultiSelect(true);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}

	public void updateGroupList() {
		final List l = GroupDeletionManager.getInstance().getDeletableGroups(GroupDeletionManager.getInstance().getLastUsageDuration());
		redtm = new GroupDeleteTableModel(l, tableModelTypeTranslator);
		tableCtr.setTableDataModel(redtm);
	}

	/**
	 * Create default template which fill in context 'firstname' , 'lastname' and 'username'.
	 * 
	 * @param subject
	 * @param body
	 * @return
	 */
	private MailTemplate createMailTemplate(final String subject, final String body) {
		return new MailTemplate(subject, body, null) {
			@Override
			public void putVariablesInMailContext(final VelocityContext context, final Identity identity) {
				context.put("firstname", identity.getUser().getProperty(UserConstants.FIRSTNAME, null));
				context.put("lastname", identity.getUser().getProperty(UserConstants.LASTNAME, null));
				context.put("username", identity.getName());
			}
		};
	}

}

class SelectionForm extends FormBasicController {

	private IntegerElement lastUsageDuration;
	private IntegerElement emailDuration;

	/**
	 * @param name
	 * @param cancelbutton
	 * @param isAdmin if true, no field must be filled in at all, otherwise validation takes place
	 */

	public SelectionForm(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		initForm(ureq);
	}

	public int getDeleteEmailDuration() {
		return emailDuration.getIntValue();
	}

	public int getLastUsageDuration() {
		return lastUsageDuration.getIntValue();
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void formCancelled(final UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {

		lastUsageDuration = uifactory.addIntegerElement("lastUsageDuration", "edit.parameter.form.lastusage.duration", GroupDeletionManager.getInstance()
				.getLastUsageDuration(), formLayout);
		emailDuration = uifactory.addIntegerElement("emailDuration", "edit.parameter.form.email.duration", GroupDeletionManager.getInstance().getDeleteEmailDuration(),
				formLayout);

		lastUsageDuration.setMinValueCheck(1, null);
		emailDuration.setMinValueCheck(1, null);

		lastUsageDuration.setDisplaySize(3);
		emailDuration.setDisplaySize(3);

		final FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonGroupLayout", getTranslator());
		formLayout.add(buttonGroupLayout);

		uifactory.addFormSubmitButton("submit", "submit", buttonGroupLayout);
		uifactory.addFormCancelButton("cancel", buttonGroupLayout, ureq, getWindowControl());
	}

	@Override
	protected void doDispose() {
		//
	}
}