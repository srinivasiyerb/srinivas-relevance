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

package org.olat.group.ui.wizard;

import java.util.Iterator;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.wizard.WizardController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.Util;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManager;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.group.GroupLoggingAction;
import org.olat.group.context.BGContext;
import org.olat.group.ui.BGConfigFlags;
import org.olat.group.ui.BGTranslatorFactory;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Description: <BR>
 * Two step wizard to generate multiple groups as a copy of a given business group. In the first step the user is asked about what should be copied, in the second step
 * the usr is asked for a list of groupnames.
 * <P>
 * Initial Date: Sep 11, 2004
 * 
 * @author Florian Gnägi
 */
public class BGMultipleCopyWizardController extends WizardController {
	private static final String PACKAGE = Util.getPackageName(BGMultipleCopyWizardController.class);

	private final BGCopyWizardCopyForm copyForm;
	private final Translator trans;
	private final BGConfigFlags flags;
	private BusinessGroup originalGroup;
	private GroupNamesForm groupNamesForm;

	/**
	 * Constructor fot the business group multiple copy wizard
	 * 
	 * @param ureq
	 * @param wControl
	 * @param originalGroup original business group: master that should be copied
	 * @param flags
	 */
	public BGMultipleCopyWizardController(final UserRequest ureq, final WindowControl wControl, final BusinessGroup originalGroup, final BGConfigFlags flags) {
		super(ureq, wControl, 2);
		this.trans = BGTranslatorFactory.createBGPackageTranslator(PACKAGE, originalGroup.getType(), ureq.getLocale());
		this.flags = flags;
		this.originalGroup = originalGroup;
		// init wizard step 1
		this.copyForm = new BGCopyWizardCopyForm(ureq, wControl);
		this.copyForm.addControllerListener(this);
		// init wizard title and set step 1
		setWizardTitle(trans.translate("bgcopywizard.multiple.title"));
		setNextWizardStep(trans.translate("bgcopywizard.copyform.title"), this.copyForm.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// default wizard will listen to cancel wizard event
		super.event(ureq, source, event);
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == copyForm) {
			if (event == Event.DONE_EVENT) {
				groupNamesForm = new GroupNamesForm(ureq, wControl, this.originalGroup.getMaxParticipants());
				groupNamesForm.addControllerListener(this);
				setNextWizardStep(trans.translate("bgcopywizard.multiple.groupnames.title"), groupNamesForm.getInitialComponent());
			}
		} else if (source == groupNamesForm) {
			if (event == Event.DONE_EVENT) {
				final List groupNames = this.groupNamesForm.getGroupNamesList();
				final StringBuilder okGroups = new StringBuilder();
				final StringBuilder nokGroups = new StringBuilder();
				final Integer max = this.groupNamesForm.getGroupMax();
				final Iterator iter = groupNames.iterator();
				while (iter.hasNext()) {
					final String groupName = (String) iter.next();
					final BusinessGroup newGroup = doCopyGroup(groupName, max);
					if (newGroup == null) {
						nokGroups.append("<li>");
						nokGroups.append(groupName);
						nokGroups.append("</li>");
					} else {
						okGroups.append("<li>");
						okGroups.append(groupName);
						okGroups.append("</li>");
						// do logging
						ThreadLocalUserActivityLogger.log(GroupLoggingAction.BG_GROUP_COPIED, getClass(), LoggingResourceable.wrap(originalGroup),
								LoggingResourceable.wrap(newGroup));
					}
				}
				if (nokGroups.length() > 0) {
					final String warning = trans.translate("bgcopywizard.multiple.groupnames.douplicates", new String[] { okGroups.toString(), nokGroups.toString() });
					getWindowControl().setWarning(warning);
				}
				// in all cases quit workflow
				fireEvent(ureq, Event.DONE_EVENT);
			}
		}
	}

	private BusinessGroup doCopyGroup(final String newGroupName, final Integer max) {
		final BusinessGroupManager groupManager = BusinessGroupManagerImpl.getInstance();
		// reload original group to prevent context proxy problems
		this.originalGroup = groupManager.loadBusinessGroup(this.originalGroup);
		final BGContext bgContext = this.originalGroup.getGroupContext();
		final boolean copyAreas = (flags.isEnabled(BGConfigFlags.AREAS) && copyForm.isCopyAreas());

		final BusinessGroup newGroup = groupManager.copyBusinessGroup(this.originalGroup, newGroupName, this.originalGroup.getDescription(), null, max, bgContext, null,
				copyAreas, copyForm.isCopyTools(), copyForm.isCopyRights(), copyForm.isCopyOwners(), copyForm.isCopyParticipants(), copyForm.isCopyMembersVisibility(),
				copyForm.isCopyWaitingList());
		return newGroup;
	}

}
