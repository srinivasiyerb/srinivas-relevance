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

package org.olat.course.statistic.orgtype;

import java.util.Date;

import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.course.ICourse;
import org.olat.course.statistic.IStatisticManager;
import org.olat.course.statistic.StatisticDisplayController;
import org.olat.course.statistic.StatisticResult;
import org.olat.course.statistic.TotalAwareColumnDescriptor;
import org.olat.shibboleth.ShibbolethModule;

/**
 * Implementation of the IStatisticManager for 'organisation type' statistic
 * <P>
 * Initial Date: 12.02.2010 <br>
 * 
 * @author Stefan
 */
public class OrgTypeStatisticManager implements IStatisticManager {

	@Override
	public StatisticResult generateStatisticResult(final UserRequest ureq, final ICourse course, final long courseRepositoryEntryKey) {
		final DBQuery dbQuery = DBFactory.getInstance().createQuery(
				"select businessPath,orgType,value from org.olat.course.statistic.orgtype.OrgTypeStat sv " + "where sv.resId=:resId");
		dbQuery.setLong("resId", courseRepositoryEntryKey);

		return new StatisticResult(course, dbQuery.list());
	}

	@Override
	public ColumnDescriptor createColumnDescriptor(final UserRequest ureq, final int column, String headerId) {
		if (column == 0) { return new DefaultColumnDescriptor("stat.table.header.node", 0, null, ureq.getLocale()); }

		if (headerId != null) {
			final Translator translator = Util.createPackageTranslator(ShibbolethModule.class, ureq.getLocale());
			if (translator != null) {
				final String newHeaderId = translator.translate("swissEduPersonHomeOrganizationType." + headerId);
				if (newHeaderId != null && !newHeaderId.startsWith(Translator.NO_TRANSLATION_ERROR_PREFIX)) {
					headerId = newHeaderId;
				}
			}
		}

		final TotalAwareColumnDescriptor cd = new TotalAwareColumnDescriptor(headerId, column, StatisticDisplayController.CLICK_TOTAL_ACTION + column, ureq.getLocale(),
				ColumnDescriptor.ALIGNMENT_RIGHT);
		cd.setTranslateHeaderKey(false);
		return cd;
	}

	@Override
	public StatisticResult generateStatisticResult(final UserRequest ureq, final ICourse course, final long courseRepositoryEntryKey, final Date fromDate,
			final Date toDate) {
		return generateStatisticResult(ureq, course, courseRepositoryEntryKey);
	}

}
