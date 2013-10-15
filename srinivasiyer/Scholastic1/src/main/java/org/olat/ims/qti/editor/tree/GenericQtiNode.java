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

package org.olat.ims.qti.editor.tree;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.tree.GenericMementoTreeNode;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;
import org.olat.ims.qti.editor.QTIEditorMainController;

/**
 * Initial Date: Nov 18, 2004 <br>
 * 
 * @author patrick
 */
public abstract class GenericQtiNode extends GenericMementoTreeNode implements IQtiNode {

	/**
	 * @see org.olat.ims.qti.editor.tree.IQtiNode#createRunController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public abstract Controller createRunController(UserRequest ureq, WindowControl wControl);

	/**
	 * @see org.olat.ims.qti.editor.tree.IQtiNode#createEditTabbedPane(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.core.gui.translator.Translator, QTIEditorMainController)
	 */
	@Override
	public abstract TabbedPane createEditTabbedPane(UserRequest ureq, WindowControl wControl, Translator trnsltr, QTIEditorMainController editorMainController);

	/**
	 * Set's the node's title and alt text (truncates title)
	 * 
	 * @param title
	 */
	public void setMenuTitleAndAlt(final String title) {
		setTitle(title);
		setAltText(title);
	}

	protected String formatVariable(final String var) {
		if (StringHelper.containsNonWhitespace(var)) { return var; }
		return "[no entry]";
	}
}