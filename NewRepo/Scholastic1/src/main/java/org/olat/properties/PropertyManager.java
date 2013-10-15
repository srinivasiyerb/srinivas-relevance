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

package org.olat.properties;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.type.Type;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.group.BusinessGroup;
import org.olat.user.UserDataDeletable;

/**
 * Initial Date: Mar 10, 2004
 * 
 * @author Mike Stock Comment:
 */
public class PropertyManager extends BasicManager implements UserDataDeletable {
	private static PropertyManager INSTANCE;

	/**
	 * [used by spring]
	 */
	private PropertyManager(final UserDeletionManager userDeletionManager) {
		userDeletionManager.registerDeletableUserData(this);
		INSTANCE = this;
	}

	/**
	 * @return Singleton.
	 */
	public static PropertyManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Creates a new Property
	 * 
	 * @param identity
	 * @param group
	 * @param olatResourceable
	 * @param category
	 * @param name
	 * @param floatValue
	 * @param longValue
	 * @param stringValue
	 * @param textValue
	 * @return property instance.
	 */
	public Property createPropertyInstance(final Identity identity, final BusinessGroup group, final OLATResourceable olatResourceable, final String category,
			final String name, final Float floatValue, final Long longValue, final String stringValue, final String textValue) {

		final Property p = new Property();
		p.setIdentity(identity);
		p.setGrp(group);
		if (olatResourceable != null) {
			p.setResourceTypeName(olatResourceable.getResourceableTypeName());
			p.setResourceTypeId(olatResourceable.getResourceableId());
		}
		p.setCategory(category);
		p.setName(name);
		p.setFloatValue(floatValue);
		p.setLongValue(longValue);
		p.setStringValue(stringValue);
		p.setTextValue(textValue);
		return p;
	}

	/**
	 * Create a user proprety. Grp, course and node a re set to null.
	 * 
	 * @param identity
	 * @param category
	 * @param name
	 * @param floatValue
	 * @param longValue
	 * @param stringValue
	 * @param textValue
	 * @return property instance limited to a specific user.
	 */
	public Property createUserPropertyInstance(final Identity identity, final String category, final String name, final Float floatValue, final Long longValue,
			final String stringValue, final String textValue) {
		return createPropertyInstance(identity, null, null, category, name, floatValue, longValue, stringValue, textValue);
	}

	/**
	 * Deletes a property on the database
	 * 
	 * @param p the property
	 */
	public void deleteProperty(final Property p) {
		DBFactory.getInstance().deleteObject(p);
	}

	/**
	 * Save a property
	 * 
	 * @param p
	 */
	public void saveProperty(final Property p) {
		p.setLastModified(new Date());
		DBFactory.getInstance().saveObject(p);
	}

	/**
	 * Update a property
	 * 
	 * @param p
	 */
	public void updateProperty(final Property p) {
		p.setLastModified(new Date());
		DBFactory.getInstance().updateObject(p);
	}

	/**
	 * Find a user property.
	 * 
	 * @param identity
	 * @param category
	 * @param name
	 * @return Found property or null if no match.
	 */
	public Property findUserProperty(final Identity identity, final String category, final String name) {

		final List props = DBFactory
				.getInstance()
				.find("from v in class org.olat.properties.Property where v.identity=? and v.category=? and v.name=? and v.grp is null and v.resourceTypeName is null and v.resourceTypeId is null",
						new Object[] { identity.getKey(), category, name }, new Type[] { Hibernate.LONG, Hibernate.STRING, Hibernate.STRING });

		if (props == null || props.size() != 1) {
			if (Tracing.isDebugEnabled(PropertyManager.class)) {
				Tracing.logDebug("Could not find property: " + name, PropertyManager.class);
			}
			return null;
		}

		return (Property) props.get(0);
	}

	/**
	 * Generic method. Returns a list of Property objects. This is an inexact match i.e. parameters with null values will not be included in the query.
	 * 
	 * @param identity
	 * @param grp
	 * @param resourceable
	 * @param category
	 * @param name
	 * @return a list of Property objects
	 */
	public List listProperties(final Identity identity, final BusinessGroup grp, final OLATResourceable resourceable, final String category, final String name) {
		if (resourceable == null) {
			return listProperties(identity, grp, null, null, category, name);
		} else {
			return listProperties(identity, grp, resourceable.getResourceableTypeName(), resourceable.getResourceableId(), category, name);
		}
	}

	/**
	 * Only to use if no OLATResourceable Object is available.
	 * 
	 * @param identity
	 * @param grp
	 * @param resourceTypeName
	 * @param resourceTypeId
	 * @param category
	 * @param name
	 * @return a list of Property objects
	 */
	public List listProperties(final Identity identity, final BusinessGroup grp, final String resourceTypeName, final Long resourceTypeId, final String category,
			final String name) {
		final StringBuilder query = new StringBuilder();
		final ArrayList objs = new ArrayList();
		final ArrayList types = new ArrayList();
		query.append("from v in class org.olat.properties.Property where ");

		boolean previousParam = false;
		if (identity != null) {
			query.append("v.identity = ?");
			objs.add(identity.getKey());
			types.add(Hibernate.LONG);
			previousParam = true;
		}

		if (grp != null) {
			if (previousParam) {
				query.append(" and ");
			}
			query.append("v.grp = ?");
			objs.add(grp.getKey());
			types.add(Hibernate.LONG);
			previousParam = true;
		}

		if (resourceTypeName != null) {
			if (previousParam) {
				query.append(" and ");
			}
			query.append("v.resourceTypeName = ?");
			objs.add(resourceTypeName);
			types.add(Hibernate.STRING);
			previousParam = true;
		}

		if (resourceTypeId != null) {
			if (previousParam) {
				query.append(" and ");
			}
			query.append(" v.resourceTypeId = ?");
			objs.add(resourceTypeId);
			types.add(Hibernate.LONG);
			previousParam = true;
		}

		if (category != null) {
			if (previousParam) {
				query.append(" and ");
			}
			query.append("v.category = ?");
			objs.add(category);
			types.add(Hibernate.STRING);
			previousParam = true;
		}

		if (name != null) {
			if (previousParam) {
				query.append(" and ");
			}
			query.append("v.name = ?");
			objs.add(name);
			types.add(Hibernate.STRING);
		}

		return DBFactory.getInstance().find(query.toString(), objs.toArray(), (Type[]) types.toArray(new Type[types.size()]));

	}

	/**
	 * deletes properties. IMPORTANT: if an argument is null, then it will be not considered in the delete statement, which means not only the record having a "null"
	 * value will be deleted, but all.
	 * 
	 * @param identity
	 * @param grp
	 * @param resourceable
	 * @param category
	 * @param name
	 */
	public void deleteProperties(final Identity identity, final BusinessGroup grp, final OLATResourceable resourceable, final String category, final String name) {
		final StringBuilder query = new StringBuilder();
		final ArrayList objs = new ArrayList();
		final ArrayList types = new ArrayList();
		query.append("from v in class org.olat.properties.Property where ");

		boolean previousParam = false;
		if (identity != null) {
			query.append("v.identity = ?");
			objs.add(identity.getKey());
			types.add(Hibernate.LONG);
			previousParam = true;
		}

		if (grp != null) {
			if (previousParam) {
				query.append(" and ");
			}
			query.append("v.grp = ?");
			objs.add(grp.getKey());
			types.add(Hibernate.LONG);
			previousParam = true;
		}

		if (resourceable != null) {
			if (previousParam) {
				query.append(" and ");
			}
			query.append("v.resourceTypeName = ?");
			objs.add(resourceable.getResourceableTypeName());
			types.add(Hibernate.STRING);

			query.append(" and v.resourceTypeId");
			if (resourceable.getResourceableId() == null) {
				query.append(" is null");
			} else {
				query.append(" = ?");
				objs.add(resourceable.getResourceableId());
				types.add(Hibernate.LONG);
			}
			previousParam = true;
		}

		if (category != null) {
			if (previousParam) {
				query.append(" and ");
			}
			query.append("v.category = ?");
			objs.add(category);
			types.add(Hibernate.STRING);
			previousParam = true;
		}

		if (name != null) {
			if (previousParam) {
				query.append(" and ");
			}
			query.append("v.name = ?");
			objs.add(name);
			types.add(Hibernate.STRING);
		}

		DBFactory.getInstance().delete(query.toString(), objs.toArray(), (Type[]) types.toArray(new Type[types.size()]));
	}

	/**
	 * Generic find method. Returns a list of Property objects. This is an exact match i.e. if you pass-on null values, null values will be included in the query.
	 * 
	 * @param identity
	 * @param grp
	 * @param resourceable
	 * @param category
	 * @param name
	 * @return a list of Property objects.
	 */
	public List findProperties(final Identity identity, final BusinessGroup grp, final OLATResourceable resourceable, final String category, final String name) {
		if (resourceable == null) {
			return findProperties(identity, grp, null, null, category, name);
		} else {
			return findProperties(identity, grp, resourceable.getResourceableTypeName(), resourceable.getResourceableId(), category, name);
		}
	}

	/**
	 * Only to use if no OLATResourceable Object is available.
	 * 
	 * @param identity
	 * @param grp
	 * @param resourceTypeName
	 * @param resourceTypeId
	 * @param category
	 * @param name
	 * @return List of properties
	 */
	public List findProperties(final Identity identity, final BusinessGroup grp, final String resourceTypeName, final Long resourceTypeId, final String category,
			final String name) {
		final StringBuilder query = new StringBuilder();
		final ArrayList objs = new ArrayList();
		final ArrayList types = new ArrayList();
		query.append("from v in class org.olat.properties.Property where ");

		if (identity != null) {
			query.append("v.identity = ?");
			objs.add(identity.getKey());
			types.add(Hibernate.LONG);
		} else {
			query.append("v.identity is null");
		}

		query.append(" and ");
		if (grp != null) {
			query.append("v.grp = ?");
			objs.add(grp.getKey());
			types.add(Hibernate.LONG);
		} else {
			query.append("v.grp is null");
		}

		query.append(" and ");
		if (resourceTypeName != null) {
			query.append("v.resourceTypeName = ?");
			objs.add(resourceTypeName);
			types.add(Hibernate.STRING);
		} else {
			query.append("v.resourceTypeName is null");
		}

		query.append(" and ");
		if (resourceTypeId != null) {
			query.append("v.resourceTypeId = ?");
			objs.add(resourceTypeId);
			types.add(Hibernate.LONG);
		} else {
			query.append("v.resourceTypeId is null");
		}

		query.append(" and ");
		if (category != null) {
			query.append("v.category = ?");
			objs.add(category);
			types.add(Hibernate.STRING);
		} else {
			query.append("v.category is null");
		}

		query.append(" and ");
		if (name != null) {
			query.append("v.name = ?");
			objs.add(name);
			types.add(Hibernate.STRING);
		} else {
			query.append("v.name is null");
		}

		return DBFactory.getInstance().find(query.toString(), objs.toArray(), (Type[]) types.toArray(new Type[types.size()]));
	}

	/**
	 * Get a list of identities that have properties given the restricting values
	 * 
	 * @param resourceable Search restricted to this resourcable
	 * @param category Search restricted to this property category
	 * @param name Search restricted to this property name
	 * @param matchNullValues true: null values in the above restricting values will be added as null values to the query; false: null values in the restricting values
	 *            will be ignored in the query
	 * @return List of identities
	 */
	public List findIdentitiesWithProperty(final OLATResourceable resourceable, final String category, final String name, final boolean matchNullValues) {
		if (resourceable == null) {
			return findIdentitiesWithProperty(null, null, category, name, matchNullValues);
		} else {
			return findIdentitiesWithProperty(resourceable.getResourceableTypeName(), resourceable.getResourceableId(), category, name, matchNullValues);
		}
	}

	/**
	 * Get a list of identities that have properties given the restricting values
	 * 
	 * @param resourceTypeName Search restricted to this resource type name
	 * @param resourceTypeId Search restricted to this resource type id
	 * @param category Search restricted to this property category
	 * @param name Search restricted to this property name
	 * @param matchNullValues true: null values in the above restricting values will be added as null values to the query; false: null values in the restricting values
	 *            will be ignored in the query
	 * @return List of identities
	 */
	public List findIdentitiesWithProperty(final String resourceTypeName, final Long resourceTypeId, final String category, final String name,
			final boolean matchNullValues) {
		final StringBuilder query = new StringBuilder();
		final ArrayList objs = new ArrayList();
		final ArrayList types = new ArrayList();
		query.append("select distinct i from org.olat.basesecurity.IdentityImpl as i");
		query.append(", org.olat.properties.Property as p");
		query.append(" where p.identity = i");

		if (resourceTypeName != null) {
			query.append(" and ");
			query.append("p.resourceTypeName = ?");
			objs.add(resourceTypeName);
			types.add(Hibernate.STRING);
		} else if (matchNullValues) {
			query.append(" and p.resourceTypeName is null");
		}

		if (resourceTypeId != null) {
			query.append(" and ");
			query.append("p.resourceTypeId = ?");
			objs.add(resourceTypeId);
			types.add(Hibernate.LONG);
		} else if (matchNullValues) {
			query.append(" and p.resourceTypeId is null");
		}

		if (category != null) {
			query.append(" and ");
			query.append("p.category = ?");
			objs.add(category);
			types.add(Hibernate.STRING);
		} else if (matchNullValues) {
			query.append(" and p.category is null");
		}

		if (name != null) {
			query.append(" and ");
			query.append("p.name = ?");
			objs.add(name);
			types.add(Hibernate.STRING);
		} else if (matchNullValues) {
			query.append(" and p.name is null");
		}

		return DBFactory.getInstance().find(query.toString(), objs.toArray(), (Type[]) types.toArray(new Type[types.size()]));
	}

	/**
	 * Generic find method.
	 * 
	 * @param identity
	 * @param grp
	 * @param resourceable
	 * @param category
	 * @param name
	 * @return Property if found or null
	 * @throws AssertException if more than one match found
	 */
	public Property findProperty(final Identity identity, final BusinessGroup grp, final OLATResourceable resourceable, final String category, final String name) {

		final List props = findProperties(identity, grp, resourceable, category, name);
		if (props == null || props.size() == 0) {
			if (Tracing.isDebugEnabled(PropertyManager.class)) {
				Tracing.logDebug("Could not find property: " + name, PropertyManager.class);
			}
			return null;
		} else if (props.size() > 1) { throw new AssertException("findProperty found more than one properties for identity::" + identity + ", group::" + grp
				+ ", resourceable::" + resourceable + ", category::" + category + ", name::" + name); }
		return (Property) props.get(0);
	}

	/**
	 * @return a list of all available resource type names
	 */
	public List getAllResourceTypeNames() {
		return DBFactory.getInstance().find("select distinct v.resourceTypeName from org.olat.properties.Property as v where v.resourceTypeName is not null");
	}

	/**
	 * Delete all properties of a certain identity.
	 * 
	 * @see org.olat.user.UserDataDeletable#deleteUserData(org.olat.core.id.Identity)
	 */
	@Override
	public void deleteUserData(final Identity identity, final String newDeletedUserName) {
		final List userProperterties = listProperties(identity, null, null, null, null, null);
		for (final Iterator iter = userProperterties.iterator(); iter.hasNext();) {
			deleteProperty((Property) iter.next());
		}
		Tracing.logDebug("All properties deleted for identity=" + identity, this.getClass());
	}

	public Property createProperty() {
		final Property p = new Property();
		return p;
	}

}
