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

package org.olat.course.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;
import org.olat.core.manager.BasicManager;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.nodes.CourseNode;
import org.olat.group.BusinessGroup;
import org.olat.properties.NarrowedPropertyManager;
import org.olat.properties.Property;

/**
 * Initial Date: May 5, 2004
 * 
 * @author gnaegi<br>
 *         Comment: The course property manager handles all course specific read / write operations for course properties. Usually other managers use the course property
 *         manager (like assessment manager or audit manager), only view controllers use the course property manager directly.
 */
public class PersistingCoursePropertyManager extends BasicManager implements CoursePropertyManager {

	private final NarrowedPropertyManager pm;
	private final Map anonymizerMap;
	private final ICourse ores;
	private static Random random = new Random(System.currentTimeMillis());

	private PersistingCoursePropertyManager(final ICourse course) {
		this.ores = course;
		this.pm = NarrowedPropertyManager.getInstance(course);
		// Initialize identity anonymizer map
		this.anonymizerMap = new HashMap();
	}

	/**
	 * Get an instance of the course property manager for this course that persists properties to the database.
	 * 
	 * @param olatCourse The course
	 * @return The course property manager
	 */
	public static PersistingCoursePropertyManager getInstance(final ICourse course) {
		return new PersistingCoursePropertyManager(course);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#createCourseNodePropertyInstance(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity,
	 *      org.olat.group.BusinessGroup, java.lang.String, java.lang.Float, java.lang.Long, java.lang.String, java.lang.String)
	 */
	@Override
	public Property createCourseNodePropertyInstance(final CourseNode node, final Identity identity, final BusinessGroup group, final String name,
			final Float floatValue, final Long longValue, final String stringValue, final String textValue) {
		final String myCategory = buildCourseNodePropertyCategory(node);
		return pm.createPropertyInstance(identity, group, myCategory, name, floatValue, longValue, stringValue, textValue);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#deleteProperty(org.olat.properties.Property)
	 */
	@Override
	public void deleteProperty(final Property p) {
		pm.deleteProperty(p);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#saveProperty(org.olat.properties.Property)
	 */
	@Override
	public void saveProperty(final Property p) {
		pm.saveProperty(p);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#updateProperty(org.olat.properties.Property)
	 */
	@Override
	public void updateProperty(final Property p) {
		pm.updateProperty(p);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#listCourseNodeProperties(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity,
	 *      org.olat.group.BusinessGroup, java.lang.String)
	 */
	@Override
	public List listCourseNodeProperties(final CourseNode node, final Identity identity, final BusinessGroup grp, final String name) {
		final String myCategory = buildCourseNodePropertyCategory(node);
		return pm.listProperties(identity, grp, myCategory, name);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#findCourseNodeProperties(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity,
	 *      org.olat.group.BusinessGroup, java.lang.String)
	 */
	@Override
	public List findCourseNodeProperties(final CourseNode node, final Identity identity, final BusinessGroup grp, final String name) {
		final String myCategory = buildCourseNodePropertyCategory(node);
		return pm.findProperties(identity, grp, myCategory, name);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#findCourseNodeProperty(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity,
	 *      org.olat.group.BusinessGroup, java.lang.String)
	 */
	@Override
	public Property findCourseNodeProperty(final CourseNode node, final Identity identity, final BusinessGroup grp, final String name) {
		final String myCategory = buildCourseNodePropertyCategory(node);
		return pm.findProperty(identity, grp, myCategory, name);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#deleteNodeProperties(org.olat.course.nodes.CourseNode, java.lang.String)
	 */
	@Override
	public void deleteNodeProperties(final CourseNode courseNode, final String name) {
		if (courseNode == null) { throw new AssertException("courseNode can not be null when deleting course node properties"); }
		pm.deleteProperties(null, null, buildCourseNodePropertyCategory(courseNode), name);
	}

	private String buildCourseNodePropertyCategory(final CourseNode node) {
		final String type = (node.getType().length() > 4 ? node.getType().substring(0, 4) : node.getType());
		return ("NID:" + type + "::" + node.getIdent());
	}

	/**
	 * @see org.olat.ims.qti.export.helper.IdentityAnonymizerCallback#getAnonymizedUserName(org.olat.core.id.Identity)
	 */
	@Override
	public String getAnonymizedUserName(final Identity identity) {
		synchronized (anonymizerMap) {
			String anonymizedName = (String) anonymizerMap.get(identity.getName());
			if (anonymizedName == null) {
				// try to fetch from course properties
				Property anonymizedProperty = pm.findProperty(identity, null, "Anonymizing", "AnonymizedUserName");
				if (anonymizedProperty == null) {
					// not found - create a new anonymized name
					anonymizedName = "RANDOM-" + random.nextInt(100000000);
					// add as course properties
					anonymizedProperty = pm.createPropertyInstance(identity, null, "Anonymizing", "AnonymizedUserName", null, null, anonymizedName, null);
					pm.saveProperty(anonymizedProperty);
				} else {
					// property found - use property value from there
					anonymizedName = anonymizedProperty.getStringValue();
				}
				anonymizerMap.put(identity.getName(), anonymizedName);
			}
			return anonymizedName;
		}
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#deleteAllCourseProperties()
	 */
	@Override
	public void deleteAllCourseProperties() {
		pm.deleteAllProperties();
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#getAllIdentitiesWithCourseAssessmentData()
	 */
	@Override
	public List getAllIdentitiesWithCourseAssessmentData() {
		final StringBuffer query = new StringBuffer();
		query.append("select distinct i from ");
		query.append(" org.olat.basesecurity.IdentityImpl as i,");
		query.append(" org.olat.properties.Property as p");
		query.append(" where i = p.identity and p.resourceTypeName = :resname");
		query.append(" and p.resourceTypeId = :resid");
		query.append(" and p.identity is not null");
		query.append(" and ( p.name = '").append(AssessmentManager.SCORE);
		query.append("' or p.name = '").append(AssessmentManager.PASSED);
		query.append("' )");

		final DB db = DBFactory.getInstance();
		final DBQuery dbq = db.createQuery(query.toString());
		final ICourse course = CourseFactory.loadCourse(ores);
		dbq.setLong("resid", course.getResourceableId().longValue());
		dbq.setString("resname", course.getResourceableTypeName());

		final List res = dbq.list();
		return res;
	}

}