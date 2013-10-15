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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.portfolio.model.structel;

import org.olat.core.commons.persistence.PersistentObject;

/**
 * Description:<br>
 * TODO: Class Description for EPStructureToStructureLink
 * <P>
 * Initial Date: 11.06.2010 <br>
 * 
 * @author rhaag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPStructureToStructureLink extends PersistentObject {

	public EPStructureToStructureLink() {
		//
	}

	/**
	 * @uml.property name="order"
	 */
	private int order;

	/**
	 * Getter of the property <tt>order</tt>
	 * 
	 * @return Returns the order.
	 * @uml.property name="order"
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * Setter of the property <tt>order</tt>
	 * 
	 * @param order The order to set.
	 * @uml.property name="order"
	 */
	public void setOrder(final int order) {
		this.order = order;
	}

	/**
	 * @uml.property name="child"
	 * @uml.associationEnd inverse="parent:com.frentix.portfolio.structure.PortfolioStructure"
	 */
	/**
	 * @uml.property name="child"
	 * @uml.associationEnd inverse="parent:com.frentix.portfolio.structure.PortfolioStructure"
	 */
	private PortfolioStructure child;

	/**
	 * Getter of the property <tt>child</tt>
	 * 
	 * @return Returns the child.
	 * @uml.property name="child"
	 */
	/**
	 * Getter of the property <tt>child</tt>
	 * 
	 * @return Returns the child.
	 * @uml.property name="child"
	 */
	public PortfolioStructure getChild() {
		return child;
	}

	/**
	 * Setter of the property <tt>child</tt>
	 * 
	 * @param child The child to set.
	 * @uml.property name="child"
	 */
	/**
	 * Setter of the property <tt>child</tt>
	 * 
	 * @param child The child to set.
	 * @uml.property name="child"
	 */
	public void setChild(final PortfolioStructure child) {
		this.child = child;
	}

	/**
	 * @uml.property name="parent"
	 */
	private PortfolioStructure parent;

	/**
	 * Getter of the property <tt>parent</tt>
	 * 
	 * @return Returns the parent.
	 * @uml.property name="parent"
	 */
	public PortfolioStructure getParent() {
		return parent;
	}

	/**
	 * Setter of the property <tt>parent</tt>
	 * 
	 * @param parent The parent to set.
	 * @uml.property name="parent"
	 */
	public void setParent(final PortfolioStructure parent) {
		this.parent = parent;
	}

}
