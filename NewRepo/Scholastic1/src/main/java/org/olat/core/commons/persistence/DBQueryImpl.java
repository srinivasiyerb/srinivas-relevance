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

package org.olat.core.commons.persistence;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.type.Type;
import org.olat.core.logging.DBRuntimeException;
import org.olat.core.logging.Tracing;
import org.olat.testutils.codepoints.server.Codepoint;

/**
 * A <b>DBQueryImpl</b> is a wrapper around a Hibernate Query object.
 * 
 * @author Andreas Ch. Kapp
 */
public class DBQueryImpl implements DBQuery {

	private Query query = null;

	public final static Map<String, SimpleProbe> listTableStatsMap_ = new HashMap<String, SimpleProbe>();

	public final static Set<String> registeredTables_ = new HashSet<String>();

	static {
		registeredTables_.add("org.olat.basesecurity.SecurityGroupMembershipImpl");
		registeredTables_.add("org.olat.group.area.BGAreaImpl");
		registeredTables_.add("org.olat.group.BusinessGroupImpl");
		registeredTables_.add("org.olat.resource.OLATResourceImpl");
		registeredTables_.add("org.olat.commons.lifecycle.LifeCycleEntry");
	}

	/**
	 * Default construcotr.
	 * 
	 * @param q
	 */
	public DBQueryImpl(Query q) {
		query = q;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setLong(java.lang.String, long)
	 */
	@Override
	public DBQuery setLong(String string, long value) {
		query.setLong(string, value);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setString(java.lang.String, java.lang.String)
	 */
	@Override
	public DBQuery setString(String string, String value) {
		query.setString(string, value);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setTime(java.lang.String, java.util.Date)
	 */
	@Override
	public DBQuery setTime(String name, Date date) {
		query.setTime(name, date);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#executeUpdate(FlushMode)
	 */
	@Override
	public int executeUpdate(FlushMode nullOrFlushMode) {
		if (nullOrFlushMode != null) {
			query.setFlushMode(nullOrFlushMode);
		}
		return query.executeUpdate();
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#list()
	 */
	@Override
	public List list() {
		Codepoint.codepoint(getClass(), "list-entry");
		final long startTime = System.currentTimeMillis();
		try {
			boolean doLog = Tracing.isDebugEnabled(DBQueryImpl.class);
			long start = 0;
			if (doLog) start = System.currentTimeMillis();
			List li = query.list();
			if (doLog) {
				long time = (System.currentTimeMillis() - start);
				Tracing.logDebug("list dbquery (time " + time + ") query " + getQueryString(), DBQueryImpl.class);
			}
			String queryString = query.getQueryString().trim();
			String queryStringToLowerCase = queryString.toLowerCase();
			if (queryStringToLowerCase.startsWith("from ")) {
				queryString = queryString.substring(5).trim();
				queryStringToLowerCase = queryString.toLowerCase();
			} else if (queryStringToLowerCase.startsWith("select ") && (queryStringToLowerCase.contains(" from "))) {
				queryString = queryString.substring(queryStringToLowerCase.indexOf(" from ") + 6).trim();
				queryStringToLowerCase = queryString.toLowerCase();
			} else {
				queryString = null;
			}
			if (queryString != null) {
				final long endTime = System.currentTimeMillis();
				final long diff = endTime - startTime;
				int wherePos = queryStringToLowerCase.indexOf(" where ");
				if (wherePos != -1) {
					queryString = queryString.substring(0, wherePos);
				}
				queryString = queryString.trim();
				StringTokenizer st = new StringTokenizer(queryString, ",");
				while (st.hasMoreTokens()) {
					String aTable = st.nextToken();
					aTable = aTable.trim();
					int spacePos = aTable.toLowerCase().indexOf(" ");
					if (spacePos != -1) {
						aTable = aTable.substring(0, spacePos);
					}
					aTable = aTable.trim();
					SimpleProbe probe = listTableStatsMap_.get(aTable);
					if (probe == null) {
						probe = new SimpleProbe();
						listTableStatsMap_.put(aTable, probe);
					}
					probe.addMeasurement(diff);
					if (!registeredTables_.contains(aTable)) {
						aTable = "THEREST";
						probe = listTableStatsMap_.get(aTable);
						if (probe == null) {
							probe = new SimpleProbe();
							listTableStatsMap_.put(aTable, probe);
						}
						probe.addMeasurement(diff);
					}
					// System.out.println(" A TABLE: "+aTable+" stats: "+probe);
				}
			}
			return li;
		} catch (HibernateException he) {
			String msg = "Error in list()";
			throw new DBRuntimeException(msg, he);
		} finally {
			Codepoint.codepoint(getClass(), "list-exit", query);
		}
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#getNamedParameters()
	 */
	@Override
	public String[] getNamedParameters() {
		try {
			return query.getNamedParameters();
		} catch (HibernateException e) {
			throw new DBRuntimeException("GetNamedParameters failed. ", e);
		}
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#getQueryString()
	 */
	@Override
	public String getQueryString() {
		return query.getQueryString();
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#getReturnTypes()
	 */
	@Override
	public Type[] getReturnTypes() {

		try {
			return query.getReturnTypes();
		} catch (HibernateException e) {
			throw new DBRuntimeException("GetReturnTypes failed. ", e);
		}
	}

	/**
	 * @return iterator
	 */
	public Iterator iterate() {
		try {
			return query.iterate();
		} catch (HibernateException e) {
			throw new DBRuntimeException("Iterate failed. ", e);
		}
	}

	/*
	 * public ScrollableResults scroll() { try { return query.scroll(); } catch (HibernateException e) { throw new DBRuntimeException("Scroll failed. ", e); } }
	 */

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setBigDecimal(int, java.math.BigDecimal)
	 */
	@Override
	public DBQuery setBigDecimal(int position, BigDecimal number) {
		query.setBigDecimal(position, number);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setBigDecimal(java.lang.String, java.math.BigDecimal)
	 */
	@Override
	public DBQuery setBigDecimal(String name, BigDecimal number) {
		query.setBigDecimal(name, number);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setBinary(int, byte[])
	 */
	@Override
	public DBQuery setBinary(int position, byte[] val) {
		query.setBinary(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setBinary(java.lang.String, byte[])
	 */
	@Override
	public DBQuery setBinary(String name, byte[] val) {
		query.setBinary(name, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setBoolean(int, boolean)
	 */
	@Override
	public DBQuery setBoolean(int position, boolean val) {
		query.setBoolean(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setBoolean(java.lang.String, boolean)
	 */
	@Override
	public DBQuery setBoolean(String name, boolean val) {
		query.setBoolean(name, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setByte(int, byte)
	 */
	@Override
	public DBQuery setByte(int position, byte val) {
		query.setByte(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setByte(java.lang.String, byte)
	 */
	@Override
	public DBQuery setByte(String name, byte val) {
		query.setByte(name, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setCacheable(boolean)
	 */
	@Override
	public DBQuery setCacheable(boolean cacheable) {
		query.setCacheable(cacheable);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setCacheRegion(java.lang.String)
	 */
	@Override
	public DBQuery setCacheRegion(String cacheRegion) {
		query.setCacheRegion(cacheRegion);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setCalendar(int, java.util.Calendar)
	 */
	@Override
	public DBQuery setCalendar(int position, Calendar calendar) {
		query.setCalendar(position, calendar);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setCalendar(java.lang.String, java.util.Calendar)
	 */
	@Override
	public DBQuery setCalendar(String name, Calendar calendar) {
		query.setCalendar(name, calendar);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setCalendarDate(int, java.util.Calendar)
	 */
	@Override
	public DBQuery setCalendarDate(int position, Calendar calendar) {
		query.setCalendarDate(position, calendar);
		return this;

	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setCalendarDate(java.lang.String, java.util.Calendar)
	 */
	@Override
	public DBQuery setCalendarDate(String name, Calendar calendar) {
		query.setCalendarDate(name, calendar);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setCharacter(int, char)
	 */
	@Override
	public DBQuery setCharacter(int position, char val) {
		query.setCharacter(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setCharacter(java.lang.String, char)
	 */
	@Override
	public DBQuery setCharacter(String name, char val) {
		query.setCharacter(name, val);
		return this;

	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setDate(int, java.util.Date)
	 */
	@Override
	public DBQuery setDate(int position, Date date) {
		query.setDate(position, date);
		return this;

	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setDate(java.lang.String, java.util.Date)
	 */
	@Override
	public DBQuery setDate(String name, Date date) {
		query.setDate(name, date);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setDouble(int, double)
	 */
	@Override
	public DBQuery setDouble(int position, double val) {
		query.setDouble(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setDouble(java.lang.String, double)
	 */
	@Override
	public DBQuery setDouble(String name, double val) {
		query.setDouble(name, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setEntity(int, java.lang.Object)
	 */
	@Override
	public DBQuery setEntity(int position, Object val) {
		query.setEntity(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setEntity(java.lang.String, java.lang.Object)
	 */
	@Override
	public DBQuery setEntity(String name, Object val) {
		query.setEntity(name, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setFirstResult(int)
	 */
	@Override
	public DBQuery setFirstResult(int firstResult) {
		query.setFirstResult(firstResult);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setFloat(int, float)
	 */
	@Override
	public DBQuery setFloat(int position, float val) {
		query.setFloat(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setFloat(java.lang.String, float)
	 */
	@Override
	public DBQuery setFloat(String name, float val) {
		query.setFloat(name, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setInteger(int, int)
	 */
	@Override
	public DBQuery setInteger(int position, int val) {
		query.setInteger(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setInteger(java.lang.String, int)
	 */
	@Override
	public DBQuery setInteger(String name, int val) {
		query.setInteger(name, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setLocale(int, java.util.Locale)
	 */
	@Override
	public DBQuery setLocale(int position, Locale locale) {
		query.setLocale(position, locale);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setLocale(java.lang.String, java.util.Locale)
	 */
	@Override
	public DBQuery setLocale(String name, Locale locale) {
		query.setLocale(name, locale);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setLockMode(java.lang.String, org.hibernate.LockMode)
	 */
	@Override
	public void setLockMode(String alias, LockMode lockMode) {
		query.setLockMode(alias, lockMode);

	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setLong(int, long)
	 */
	@Override
	public DBQuery setLong(int position, long val) {
		query.setLong(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setMaxResults(int)
	 */
	@Override
	public DBQuery setMaxResults(int maxResults) {
		query.setMaxResults(maxResults);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setParameter(int, java.lang.Object, org.hibernate.type.Type)
	 */
	@Override
	public DBQuery setParameter(int position, Object val, Type type) {
		query.setParameter(position, val, type);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setParameter(int, java.lang.Object)
	 */
	@Override
	public DBQuery setParameter(int position, Object val) {
		try {
			query.setParameter(position, val);
		} catch (HibernateException e) {
			throw new DBRuntimeException("DBQuery error. ", e);
		}
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setParameter(java.lang.String, java.lang.Object, org.hibernate.type.Type)
	 */
	@Override
	public DBQuery setParameter(String name, Object val, Type type) {
		query.setParameter(name, val, type);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setParameter(java.lang.String, java.lang.Object)
	 */
	@Override
	public DBQuery setParameter(String name, Object val) {
		try {
			query.setParameter(name, val);
		} catch (HibernateException e) {
			throw new DBRuntimeException("DBQuery error. ", e);
		}
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setParameterList(java.lang.String, java.util.Collection, org.hibernate.type.Type)
	 */
	@Override
	public DBQuery setParameterList(String name, Collection vals, Type type) {
		try {
			query.setParameterList(name, vals, type);
		} catch (HibernateException e) {
			throw new DBRuntimeException("DBQuery error. ", e);
		}
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setParameterList(java.lang.String, java.util.Collection)
	 */
	@Override
	public DBQuery setParameterList(String name, Collection vals) {
		try {
			query.setParameterList(name, vals);
		} catch (HibernateException e) {
			throw new DBRuntimeException("DBQuery error. ", e);
		}
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setParameterList(java.lang.String, java.lang.Object[], org.hibernate.type.Type)
	 */
	@Override
	public DBQuery setParameterList(String name, Object[] vals, Type type) {
		try {
			query.setParameterList(name, vals, type);
		} catch (HibernateException e) {
			throw new DBRuntimeException("DBQuery error. ", e);
		}
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setParameterList(java.lang.String, java.lang.Object[])
	 */
	@Override
	public DBQuery setParameterList(String name, Object[] vals) {
		try {
			query.setParameterList(name, vals);
		} catch (HibernateException e) {
			throw new DBRuntimeException("DBQuery error. ", e);
		}
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setProperties(java.lang.Object)
	 */
	@Override
	public DBQuery setProperties(Object bean) {
		try {
			query.setProperties(bean);
		} catch (HibernateException e) {
			throw new DBRuntimeException("DBQuery error. ", e);
		}
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setSerializable(int, java.io.Serializable)
	 */
	@Override
	public DBQuery setSerializable(int position, Serializable val) {
		query.setSerializable(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setSerializable(java.lang.String, java.io.Serializable)
	 */
	@Override
	public DBQuery setSerializable(String name, Serializable val) {
		query.setSerializable(name, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setShort(int, short)
	 */
	@Override
	public DBQuery setShort(int position, short val) {
		query.setShort(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setShort(java.lang.String, short)
	 */
	@Override
	public DBQuery setShort(String name, short val) {
		query.setShort(name, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setString(int, java.lang.String)
	 */
	@Override
	public DBQuery setString(int position, String val) {
		query.setString(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setText(int, java.lang.String)
	 */
	@Override
	public DBQuery setText(int position, String val) {
		query.setText(position, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setText(java.lang.String, java.lang.String)
	 */
	@Override
	public DBQuery setText(String name, String val) {
		query.setText(name, val);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setTime(int, java.util.Date)
	 */
	@Override
	public DBQuery setTime(int position, Date date) {
		query.setTime(position, date);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setTimeout(int)
	 */
	@Override
	public DBQuery setTimeout(int timeout) {
		query.setTimeout(timeout);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setTimestamp(int, java.util.Date)
	 */
	@Override
	public DBQuery setTimestamp(int position, Date date) {
		query.setTimestamp(position, date);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#setTimestamp(java.lang.String, java.util.Date)
	 */
	@Override
	public DBQuery setTimestamp(String name, Date date) {
		query.setTimestamp(name, date);
		return this;
	}

	/**
	 * @see org.olat.core.commons.persistence.DBQuery#uniqueResult()
	 */
	@Override
	public Object uniqueResult() {
		try {
			return query.uniqueResult();
		} catch (HibernateException e) {
			throw new DBRuntimeException("DBQuery error. ", e);
		}
	}

}
