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

package org.olat.course.nodes.projectbroker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.course.nodes.projectbroker.datamodel.CustomField;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerManagerFactory;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerModuleConfiguration;

/**
 * @author guretzki
 */

public class ProjectListTableModel extends DefaultTableDataModel {
	private static final int COLUMN_COUNT = 6;
	private final Identity identity;
	private final Translator translator;
	private final ProjectBrokerModuleConfiguration moduleConfig;
	private final int numberOfCustomFieldInTable;
	private final int numberOfEventInTable;
	private final int nbrSelectedProjects;
	private final List<Project.EventType> enabledEventList;
	private final boolean isParticipantInAnyProject;
	// Array with numbers of the customfields [0...MAX_NBR_CUSTOMFIELDS] which are enabled for table-view
	private final int[] enabledCustomFieldNumbers;

	private final OLog log = Tracing.createLoggerFor(this.getClass());

	/**
	 * @param owned list of projects
	 */
	public ProjectListTableModel(final List owned, final Identity identity, final Translator translator, final ProjectBrokerModuleConfiguration moduleConfig,
			final int numberOfCustomFieldInTable, final int numberOfEventInTable, final int nbrSelectedProjects, final boolean isParticipantInAnyProject) {
		super(owned);
		this.identity = identity;
		this.translator = translator;
		this.moduleConfig = moduleConfig;
		this.numberOfCustomFieldInTable = numberOfCustomFieldInTable;
		this.numberOfEventInTable = numberOfEventInTable;
		this.nbrSelectedProjects = nbrSelectedProjects;
		this.enabledEventList = getEnabledEvents(moduleConfig);
		this.isParticipantInAnyProject = isParticipantInAnyProject;
		this.enabledCustomFieldNumbers = new int[numberOfCustomFieldInTable];
		// loop over all custom fields
		int index = 0;
		int customFiledIndex = 0;
		for (final Iterator<CustomField> iterator = moduleConfig.getCustomFields().iterator(); iterator.hasNext();) {
			final CustomField customField = iterator.next();
			if (customField.isTableViewEnabled()) {
				enabledCustomFieldNumbers[index++] = customFiledIndex;
			}
			customFiledIndex++;
		}
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return COLUMN_COUNT + numberOfCustomFieldInTable + numberOfEventInTable;
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getValueAt(int, int)
	 */
	@Override
	public Object getValueAt(final int row, final int col) {
		final Project project = (Project) objects.get(row);
		if (col == 0) {
			log.debug("project=" + project); // debug-output only once for each project
			final String name = project.getTitle();
			return name;
		} else if (col == 1) {
			// get identity_date list sorted by AddedDate
			final List<Object[]> identities = BaseSecurityManager.getInstance().getIdentitiesAndDateOfSecurityGroup(project.getProjectLeaderGroup(), true);
			if (identities.isEmpty()) {
				return "-";
			} else {
				// Get last added identity
				final Object[] identityAndDate = identities.get(identities.size() - 1);
				final String last = ((Identity) identityAndDate[0]).getUser().getProperty(UserConstants.LASTNAME, getLocale());
				final String first = ((Identity) identityAndDate[0]).getUser().getProperty(UserConstants.FIRSTNAME, getLocale());
				final String showName = last + " " + first;
				if (identities.size() == 1) {
					return showName;
				} else {
					// more than one projectleader => show ...
					return showName + ", ...";
				}
			}
		} else if (col == (numberOfCustomFieldInTable + numberOfEventInTable + 2)) {
			return ProjectBrokerManagerFactory.getProjectBrokerManager().getStateFor(project, identity, moduleConfig);
		} else if (col == (numberOfCustomFieldInTable + numberOfEventInTable + 3)) {
			final StringBuilder buf = new StringBuilder();
			buf.append(project.getSelectedPlaces());
			if (project.getMaxMembers() != Project.MAX_MEMBERS_UNLIMITED) {
				buf.append(" ");
				buf.append(translator.translate("projectlist.numbers.delimiter"));
				buf.append(" ");
				buf.append(project.getMaxMembers());
			}
			return buf.toString();
		} else if (col == (numberOfCustomFieldInTable + numberOfEventInTable + 4)) { // enroll
			return ProjectBrokerManagerFactory.getProjectBrokerManager().canBeProjectSelectedBy(identity, project, moduleConfig, nbrSelectedProjects,
					isParticipantInAnyProject);
		} else if (col == (numberOfCustomFieldInTable + numberOfEventInTable + 5)) { // cancel enrollment
			return ProjectBrokerManagerFactory.getProjectBrokerManager().canBeCancelEnrollmentBy(identity, project, moduleConfig);
		} else if ((col == 2) && (numberOfCustomFieldInTable > 0)) {
			return project.getCustomFieldValue(enabledCustomFieldNumbers[0]);
		} else if ((col == 3) && (numberOfCustomFieldInTable > 1)) {
			return project.getCustomFieldValue(enabledCustomFieldNumbers[1]);
		} else if ((col == 4) && (numberOfCustomFieldInTable > 2)) {
			return project.getCustomFieldValue(enabledCustomFieldNumbers[2]);
		} else if ((col == 5) && (numberOfCustomFieldInTable > 3)) {
			return project.getCustomFieldValue(enabledCustomFieldNumbers[3]);
		} else if ((col == 6) && (numberOfCustomFieldInTable > 4)) {
			return project.getCustomFieldValue(enabledCustomFieldNumbers[4]);
		} else if (col == (2 + numberOfCustomFieldInTable)) {
			return project.getProjectEvent(enabledEventList.get(0));
		} else if (col == (3 + numberOfCustomFieldInTable)) {
			return project.getProjectEvent(enabledEventList.get(1));
		} else if (col == (4 + numberOfCustomFieldInTable)) {
			return project.getProjectEvent(enabledEventList.get(2));
		} else {
			return "ERROR";
		}
	}

	private List<Project.EventType> getEnabledEvents(final ProjectBrokerModuleConfiguration moduleConfig) {
		final List<Project.EventType> enabledEventList = new ArrayList<Project.EventType>();
		for (final Project.EventType eventType : Project.EventType.values()) {
			if (moduleConfig.isProjectEventEnabled(eventType) && moduleConfig.isProjectEventTableViewEnabled(eventType)) {
				enabledEventList.add(eventType);
			}
		}
		return enabledEventList;
	}

	/**
	 * @param owned
	 */
	public void setEntries(final List owned) {
		this.objects = owned;
	}

	/**
	 * @param row
	 * @return the project at the given row
	 */
	public Project getProjectAt(final int row) {
		return (Project) objects.get(row);
	}

	@Override
	public Object createCopyWithEmptyList() {
		final ProjectListTableModel copy = new ProjectListTableModel(new ArrayList(), identity, translator, moduleConfig, numberOfCustomFieldInTable,
				numberOfEventInTable, nbrSelectedProjects, isParticipantInAnyProject);
		return copy;
	}

}