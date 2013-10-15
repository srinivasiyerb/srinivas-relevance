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

package org.olat.core.gui.components;

import java.util.HashMap;
import java.util.Map;

import org.olat.core.gui.translator.Translator;

/**
 * Description: <br>
 * 
 * @author Felix Jost
 */
public abstract class Container extends Component {
	private Map<String, Component> components = new HashMap<String, Component>(5);

	/**
	 * @param name
	 */
	public Container(String name) {
		super(name);
	}

	/**
	 * @param name
	 */
	public Container(String name, Translator translator) {
		super(name, translator);
	}

	/**
	 * puts a component into this container
	 * 
	 * @deprecated Please use put(String name, Component component) instead!
	 * @param component
	 */
	// FIXME fj: replace with new style
	@Deprecated
	public void put(Component component) {
		String coName = component.getComponentName();
		put(coName, component);
	}

	/*
	 * puts the component into this container.
	 * @param name how the component is called, e.g. for rendering in a html fragment: $r.render("name")
	 * @param component the component to add as a child into this container
	 */
	public void put(String name, Component component) {
		if (name == null) throw new RuntimeException("name of component may not be null: childtype=" + component.getClass().getName() + ", parent=" + getComponentName());
		components.put(name, component);
		component.setParent(this);
		setDirty(true);
		// inherit translator from container if component does not provide
		// translator (e.g. from velocitycontainer to Link component. it assumes that all containers have a translator on create time.
		if (component.getTranslator() == null) component.setTranslator(getTranslator());

	}

	/**
	 * removes the component from the container. Hint: it can often be more appropriate to use a panel and then use panel.setContent(null) to empty that panel
	 * 
	 * @param component
	 */
	public void remove(Component component) {
		if (component != null) component.setParent(null);
		boolean removed = components.values().remove(component);
		if (removed) {
			setDirty(true);
		}
	}

	/**
	 * use only rarely!
	 * 
	 * @param name
	 * @return
	 */
	public Component getComponent(String name) {
		return components.get(name);
	}

	/**
	 * Use only rarely!!
	 * 
	 * @return
	 */
	public Map<String, Component> getComponents() {
		return components;
	}

	/**
	 * @see org.olat.core.gui.components.Component#getExtendedDebugInfo()
	 */
	@Override
	public String getExtendedDebugInfo() {
		return "";

		/*
		 * StringBuilder sb = new StringBuilder("children:"); for (Iterator it_chd = components.keySet().iterator(); it_chd.hasNext();) { String chdkey = (String)
		 * it_chd.next(); Component chdcomp = getComponent(chdkey); String origCompName = chdcomp.getComponentName(); if (chdkey.equals(origCompName)) {
		 * sb.append(chdkey); } else { // name of components differs from the key with which it was put // in the container
		 * sb.append(chdkey).append(" (").append(origCompName).append(")"); } sb.append(", "); } return sb.toString();
		 */
	}

}