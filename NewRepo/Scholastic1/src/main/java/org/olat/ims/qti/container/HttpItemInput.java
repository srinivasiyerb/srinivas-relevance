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

package org.olat.ims.qti.container;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Initial Date: 02.04.2003
 * 
 * @author Felix Jost
 */
public class HttpItemInput implements ItemInput, Serializable {

	private final Map m;
	private final String ident;

	/**
	 * Constructor
	 */
	public HttpItemInput(final String itemIdent) {
		m = new HashMap();
		ident = itemIdent;
	}

	public void addTestVariableVal(final String varName) {
		List li = (List) m.get(varName);
		if (li == null) {
			li = new ArrayList();
			m.put(varName, li);
		}
		li.add("1.23456"); // a value which satisfies all compares
	}

	public Object putSingle(final String key, final String value) {
		List l = getAsList(key);
		if (l == null) {
			l = new ArrayList();
		}
		l.add(value.trim());
		return m.put(key, l);
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.qti.process.ItemInput#getAsString(java.lang.String)
	 */
	@Override
	public String getSingle(final String varName) {
		final List li = getAsList(varName);
		if (li == null) { return ""; }
		if (li.size() > 1) { throw new RuntimeException("expected one, but more than one entry from user for variable:" + varName); }
		return (String) li.get(0);
	}

	/**
	 * @see org.olat.qti.process.ItemInput#getAsSet(java.lang.String)
	 */
	@Override
	public List getAsList(final String varName) {
		final List li = (List) m.get(varName);
		return li;
	}

	/**
	 * Return the map of answers for all inputs
	 * 
	 * @return
	 */
	@Override
	public Map getInputMap() {
		return m;
	}

	@Override
	public boolean contains(final String varName, final String value) {
		final List li = (List) m.get(varName);
		if (li == null) {
			/*
			 * If variable was not declared, we return false without throwing up This is necessary for example for composite multiple choice, single select items where
			 * the user does not provide an answer to some or all of the questions making up the composite item.
			 */
			// throw new RuntimeException("variable "+varName+" was not declared!");
			return false;
		}
		return li.contains(value);
	}

	@Override
	public boolean containsIgnoreCase(final String varName, final String value) {
		final List li = (List) m.get(varName);
		if (li == null) {
			/*
			 * If variable was not declared, we return false without throwing up This is necessary for example for composite multiple choice, single select items where
			 * the user does not provide an answer to some or all of the questions making up the composite item.
			 */
			// throw new RuntimeException("variable "+varName+" was not declared!");
			return false;
		}
		for (final Iterator iter = li.iterator(); iter.hasNext();) {
			final String element = (String) iter.next();
			if (element.equalsIgnoreCase(value)) { return true; }
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ident + ":" + m.toString() + "=" + super.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.qti.process.ItemInput#getIdent()
	 */
	@Override
	public String getIdent() {
		return ident;
	}

	@Override
	public boolean isEmpty() {
		return (m.size() == 0);
	}

}
