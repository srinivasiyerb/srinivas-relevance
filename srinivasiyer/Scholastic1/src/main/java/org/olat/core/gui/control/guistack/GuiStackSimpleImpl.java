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
package org.olat.core.gui.control.guistack;

import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.util.Util;

/**
 * Description:<br>
 * TODO: Felix Jost Class Description for Trans
 * <P>
 * Initial Date: 24.01.2007 <br>
 * 
 * @author Felix Jost, http://www.goodsolutions.ch
 */
public class GuiStackSimpleImpl implements GuiStack {
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(GuiStackSimpleImpl.class);

	private Panel contentPanel;

	/**
	 * 
	 */
	public GuiStackSimpleImpl(Component initialComponent) {
		contentPanel = new Panel("simpleguistack");
		contentPanel.setContent(initialComponent);
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.control.GuiStack#pushModalDialog(java.lang.String, org.olat.core.gui.components.Component)
	 */
	@Override
	public void pushModalDialog(Component content) {
		// wrap the component into a modal foreground dialog with alpha-blended-background
		VelocityContainer inset = new VelocityContainer("simpleinset", VELOCITY_ROOT + "/simpleinset.html", null, null);
		inset.put("cont", content);
		contentPanel.pushContent(inset);
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.control.GuiStack#pushContent(org.olat.core.gui.components.Component)
	 */
	@Override
	public void pushContent(Component newContent) {
		contentPanel.pushContent(newContent);
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.control.GuiStack#popContent()
	 */
	@Override
	public void popContent() {
		contentPanel.popContent();
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.control.GuiStack#getPanel()
	 */
	@Override
	public Panel getPanel() {
		return contentPanel;
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.control.GuiStack#getModalPanel()
	 */
	@Override
	public Panel getModalPanel() {
		return null;
	}

}
