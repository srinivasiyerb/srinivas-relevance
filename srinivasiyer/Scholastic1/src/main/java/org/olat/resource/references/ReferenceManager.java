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

package org.olat.resource.references;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hibernate.Hibernate;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.OLATResourceable;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.Util;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceImpl;
import org.olat.resource.OLATResourceManager;

/**
 * Initial Date: May 27, 2004
 * 
 * @author Mike Stock Comment:
 */
public class ReferenceManager extends BasicManager {

	private static ReferenceManager INSTANCE;
	private final OLATResourceManager olatResourceManager;

	private ReferenceManager(final OLATResourceManager olatResourceManager) {
		this.olatResourceManager = olatResourceManager;
		INSTANCE = this;
	}

	/**
	 * @return Singleton.
	 */
	public static ReferenceManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Add a new reference. The meaning of source and target is such as the source references the target.
	 * 
	 * @param source
	 * @param target
	 * @param userdata
	 */
	public void addReference(final OLATResourceable source, final OLATResourceable target, final String userdata) {
		// FIXME:ms:b consider the case where source does not exists yet in the OLATResource db table
		final OLATResourceImpl sourceImpl = (OLATResourceImpl) olatResourceManager.findResourceable(source);
		final OLATResourceImpl targetImpl = (OLATResourceImpl) olatResourceManager.findResourceable(target);
		final ReferenceImpl ref = new ReferenceImpl(sourceImpl, targetImpl, userdata);
		DBFactory.getInstance().saveObject(ref);
	}

	/**
	 * List all references the source holds.
	 * 
	 * @param source
	 * @return List of renerences.
	 */
	public List getReferences(final OLATResourceable source) {
		final OLATResourceImpl sourceImpl = (OLATResourceImpl) olatResourceManager.findResourceable(source);
		if (sourceImpl == null) { return new ArrayList(0); }

		return DBFactory.getInstance().find("select v from org.olat.resource.references.ReferenceImpl as v where v.source = ?", sourceImpl.getKey(), Hibernate.LONG);
	}

	/**
	 * List all sources which hold references to the target.
	 * 
	 * @param target
	 * @return List of references.
	 */
	public List getReferencesTo(final OLATResourceable target) {
		final OLATResourceImpl targetImpl = (OLATResourceImpl) olatResourceManager.findResourceable(target);
		if (targetImpl == null) { return new ArrayList(0); }

		return DBFactory.getInstance().find("select v from org.olat.resource.references.ReferenceImpl as v where v.target = ?", targetImpl.getKey(), Hibernate.LONG);
	}

	/**
	 * Check wether any references to the target exist.
	 * 
	 * @param target
	 * @return True if references exist.
	 */
	public boolean hasReferencesTo(final OLATResourceable target) {
		return (getReferencesTo(target).size() > 0);
	}

	/**
	 * Get an HTML summary of existing references or null if no references exist.
	 * 
	 * @param target
	 * @param locale
	 * @return HTML fragment or null if no references exist.
	 */
	public String getReferencesToSummary(final OLATResourceable target, final Locale locale) {
		final Translator translator = Util.createPackageTranslator(this.getClass(), locale);
		final StringBuilder result = new StringBuilder(100);
		final List refs = getReferencesTo(target);
		if (refs.size() == 0) { return null; }
		for (final Iterator iter = refs.iterator(); iter.hasNext();) {
			final ReferenceImpl ref = (ReferenceImpl) iter.next();
			final OLATResourceImpl source = ref.getSource();

			// special treatment for referenced courses: find out the course title
			if (source.getResourceableTypeName().equals(CourseModule.getCourseTypeName())) {
				final ICourse course = CourseFactory.loadCourse(source);
				result.append(translator.translate("ref.course", new String[] { course.getCourseTitle() }));
			} else {
				result.append(translator.translate("ref.generic", new String[] { source.getKey().toString() }));
			}
			result.append("<br />");
		}
		return result.toString();
	}

	/**
	 * @param ref
	 */
	public void delete(final ReferenceImpl ref) {
		DBFactory.getInstance().deleteObject(ref);
	}

	/**
	 * Only for cleanup : Delete all references of an OLAT-resource.
	 * 
	 * @param olatResource an OLAT-Resource
	 */
	public void deleteAllReferencesOf(final OLATResource olatResource) {
		final List references = ReferenceManager.getInstance().getReferences(olatResource);
		for (final Iterator iterator = references.iterator(); iterator.hasNext();) {
			final ReferenceImpl ref = (ReferenceImpl) iterator.next();
			ReferenceManager.getInstance().delete(ref);
		}
	}

}
