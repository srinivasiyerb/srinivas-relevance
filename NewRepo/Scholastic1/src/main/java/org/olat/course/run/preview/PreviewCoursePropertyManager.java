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

package org.olat.course.run.preview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;
import org.olat.core.manager.BasicManager;
import org.olat.course.nodes.CourseNode;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.group.BusinessGroup;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;

/**
 * Initial Date: 08.02.2005
 * 
 * @author Mike Stock
 */
final class PreviewCoursePropertyManager extends BasicManager implements CoursePropertyManager {

	/**
	 * Hashmap contains hasmaps
	 */
	private final Map properties = new HashMap();

	/**
	 * Creates a new course proprerty manager that stores properties per instance.
	 */
	public PreviewCoursePropertyManager() {
		//
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#createCourseNodePropertyInstance(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity,
	 *      org.olat.group.BusinessGroup, java.lang.String, java.lang.Float, java.lang.Long, java.lang.String, java.lang.String)
	 */
	@Override
	public Property createCourseNodePropertyInstance(final CourseNode node, final Identity identity, final BusinessGroup group, final String name,
			final Float floatValue, final Long longValue, final String stringValue, final String textValue) {
		final Property p = PropertyManager.getInstance().createProperty();
		p.setCategory(buildCourseNodePropertyCategory(node));
		p.setIdentity(identity);
		p.setGrp(null);
		p.setName(name);
		p.setLongValue(longValue);
		p.setFloatValue(floatValue);
		p.setStringValue(stringValue);
		p.setTextValue(textValue);
		return p;
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#deleteProperty(org.olat.properties.Property)
	 */
	@Override
	public void deleteProperty(final Property p) {
		final List propertyList = getListOfProperties(p);
		for (int i = 0; i < propertyList.size(); i++) {
			final Property propertyElement = (Property) propertyList.get(i);
			if (propertyElement.getLongValue().equals(p.getLongValue()) && propertyElement.getFloatValue().equals(p.getFloatValue())
					&& propertyElement.getStringValue().equals(p.getStringValue()) && propertyElement.getTextValue().equals(p.getTextValue())) {
				propertyList.remove(i);
				break;
			}
		}
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#saveProperty(org.olat.properties.Property)
	 */
	@Override
	public void saveProperty(final Property p) {
		final List propertyList = getListOfProperties(p);
		// since this is a save (only done once after creation) we
		// can safely add it to the list without looking for duplicates
		propertyList.add(p);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#updateProperty(org.olat.properties.Property)
	 */
	@Override
	public void updateProperty(final Property p) {
		throw new AssertException("Not implemented for preview.");
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#listCourseNodeProperties(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity,
	 *      org.olat.group.BusinessGroup, java.lang.String)
	 */
	@Override
	public List listCourseNodeProperties(final CourseNode node, final Identity identity, final BusinessGroup grp, final String name) {
		throw new AssertException("Not implemented for preview.");
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#findCourseNodeProperties(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity,
	 *      org.olat.group.BusinessGroup, java.lang.String)
	 */
	@Override
	public List findCourseNodeProperties(final CourseNode node, final Identity identity, final BusinessGroup grp, final String name) {
		List propertiesList = (List) properties.get(buildPropertyHashKey(buildCourseNodePropertyCategory(node), (identity == null ? "" : identity.getName()), grp, name));
		if (propertiesList == null) {
			propertiesList = new ArrayList();
		}
		return propertiesList;
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#findCourseNodeProperty(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity,
	 *      org.olat.group.BusinessGroup, java.lang.String)
	 */
	@Override
	public Property findCourseNodeProperty(final CourseNode node, final Identity identity, final BusinessGroup grp, final String name) {
		final List propertyList = (List) properties.get(buildPropertyHashKey(buildCourseNodePropertyCategory(node), (identity == null ? "" : identity.getName()), grp,
				name));
		if (propertyList == null) { return null; }
		return (Property) propertyList.get(0);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#deleteNodeProperties(org.olat.course.nodes.CourseNode, java.lang.String)
	 */
	@Override
	public void deleteNodeProperties(final CourseNode courseNode, final String name) {
		final String category = buildCourseNodePropertyCategory(courseNode);
		final Object[] keys = properties.keySet().toArray();
		for (int i = 0; i < keys.length; i++) {
			final String key = (String) keys[i];
			if (key.startsWith(category) && key.endsWith(name)) {
				properties.remove(key);
			}
		}
	}

	/**
	 * A property key consists of Category, Identity, Group and Name. Each property can have multiple values for the same given key. This returns the list of properties
	 * with the same key.
	 * 
	 * @param p
	 * @return list of properties with the same key
	 */
	private List getListOfProperties(final Property p) {
		final String propertyKey = buildPropertyHashKey(p);
		// get the list of properties for this key...
		List propertyList = (List) properties.get(propertyKey);
		if (propertyList == null) {
			propertyList = new ArrayList();
			properties.put(propertyKey, propertyList);
		}
		return propertyList;
	}

	private String buildPropertyHashKey(final Property p) {
		return buildPropertyHashKey(p.getCategory(), (p.getIdentity() == null) ? "" : p.getIdentity().getName(), p.getGrp(), p.getName());
	}

	private String buildPropertyHashKey(final String category, final String identityName, final BusinessGroup group, final String name) {
		return (category + identityName + (group == null ? "" : group.getKey().toString()) + name);
	}

	private String buildCourseNodePropertyCategory(final CourseNode node) {
		final String type = (node.getType().length() > 4 ? node.getType().substring(0, 4) : node.getType());
		return ("NID:" + type + "::" + node.getIdent());
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#getAnonymizedUserName(org.olat.core.id.Identity)
	 */
	@Override
	public String getAnonymizedUserName(final Identity identity) {
		throw new AssertException("Not implemented for preview.");
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#deleteAllCourseProperties()
	 */
	@Override
	public void deleteAllCourseProperties() {
		throw new AssertException("Not implemented for preview.");
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#getAllIdentitiesWithCourseAssessmentData()
	 */
	@Override
	public List getAllIdentitiesWithCourseAssessmentData() {
		throw new AssertException("Not implemented for preview.");
	}
}
