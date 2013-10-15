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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.render.ValidationResult;
import org.olat.core.util.component.ComponentTraverser;
import org.olat.core.util.component.ComponentVisitor;

/**
 * Description: <br>
 * 
 * @author Felix Jost
 */
public class ComponentHelper {

	/**
	 * @param startFrom
	 * @param id
	 * @param foundPath
	 * @return
	 */
	public static Component findDescendantOrSelfByID(Component startFrom, long id, List foundPath) {
		return dofind(startFrom, id, foundPath);
	}

	private static Component dofind(Component current, long id, List foundPath) {
		if (current.getDispatchID() == id) return current;
		if (current instanceof Container) {
			Container co = (Container) current;
			Map children = co.getComponents();
			for (Iterator iter = children.values().iterator(); iter.hasNext();) {
				Component child = (Component) iter.next();
				Component found = dofind(child, id, foundPath);
				if (found != null) {
					foundPath.add(child);
					return found;
				}
			}
		}
		return null;
	}

	/**
	 * @param ureq
	 * @param top
	 * @param vr
	 */
	public static void validateComponentTree(UserRequest ureq, Container top, ValidationResult vr) {
		doValidate(ureq, top, vr);
	}

	/**
	 * validates all the visible components
	 * 
	 * @param vr
	 */
	private static void doValidate(UserRequest ureq, Component current, ValidationResult vr) {
		if (!current.isVisible()) return; // invisible components are not validated,
		// since they are not displayed
		current.validate(ureq, vr);
		if (current instanceof Container) { // visit children
			Container co = (Container) current;
			Map children = co.getComponents();
			for (Iterator iter = children.values().iterator(); iter.hasNext();) {
				Component child = (Component) iter.next();
				doValidate(ureq, child, vr);
			}
		}
	}

	/**
	 * REVIEW:pb: 2008-05-05 -> not referenced from .html or .java
	 * 
	 * @param wins
	 * @param compToFind
	 * @return
	 * @deprecated
	 */
	@Deprecated
	protected static Window findWindowWithComponentInIt(Windows wins, final Component compToFind) {
		Window awin = null;
		for (Iterator it_wins = wins.getWindowIterator(); it_wins.hasNext();) {
			awin = (Window) it_wins.next();

			// find the correct component within the window
			MyVisitor v = new MyVisitor(compToFind);
			ComponentTraverser ct = new ComponentTraverser(v, awin.getContentPane(), false);
			ct.visitAll(null);
			if (v.f != null) return awin;

		}
		return null;
	}

	static class MyVisitor implements ComponentVisitor {
		private Component compToFind;
		public Component f = null;

		public MyVisitor(Component compToFind) {
			this.compToFind = compToFind;
		}

		@Override
		public boolean visit(Component comp, UserRequest ureq) {
			if (comp == compToFind) {
				f = comp;
				return false;
			}
			return true;
		}
	}

}