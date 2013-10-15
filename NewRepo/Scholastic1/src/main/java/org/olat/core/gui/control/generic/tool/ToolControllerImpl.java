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

package org.olat.core.gui.control.generic.tool;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.logging.AssertException;
import org.olat.core.util.Util;

/**
 * Description: <br>
 * 
 * @author Felix Jost
 */
class ToolControllerImpl extends DefaultController implements ToolController {

	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(ToolControllerImpl.class);

	private List entries;
	private List myLinks;
	private VelocityContainer content;
	private boolean dragEnabled;
	/**
	 * to enumerate the names of the components so the caller has never to care about possible name clashed if adding multiple components
	 */
	private int id;

	/**
	 * package private
	 */
	ToolControllerImpl(WindowControl wControl) {
		super(wControl);
		entries = new ArrayList();
		myLinks = new ArrayList();
		// init content; we do not need a translator here (null argument below)
		content = new VelocityContainer("toolcontent", VELOCITY_ROOT + "/index.html", null, this);
		content.contextPut("entries", entries);
		setDragEnabled(false);
		setInitialComponent(content);
	}

	/**
	 * @see org.olat.core.gui.control.generic.tool.ToolController#addHeader(java.lang.String)
	 */
	@Override
	public void addHeader(String text) {
		addHeader(text, null);
	}

	/**
	 * @see org.olat.core.gui.control.generic.tool.ToolController#addHeader(java.lang.String, java.lang.String)
	 */
	@Override
	public void addHeader(String text, String ident) {
		// use default value
		entries.add(new ToolEntry(ident, text, "b_toolbox_head_default"));
	}

	/**
	 * @see org.olat.core.gui.control.generic.tool.ToolController#addHeader(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void addHeader(String text, String ident, String cssClass) {
		entries.add(new ToolEntry(ident, text, cssClass));
	}

	/**
	 * @see org.olat.core.gui.control.generic.tool.ToolController#addLink(java.lang.String, java.lang.String)
	 */
	@Override
	public void addLink(String action, String text) {
		addLink(action, text, null, null);
	}

	@Override
	public void addLink(String action, String text, String ident, String cssClass, boolean markAsDownloadLink) {
		String linkCmpName = ident;
		if (ident == null) {
			// TODO:pb remove quickfix -> recorder is also removed
			// FIXME:as:pb: quickfix, all components must have names for recorder
			linkCmpName = action;
		}
		if (cssClass == null) {
			Link linkCmp = LinkFactory.createCustomLink(linkCmpName, action, text, Link.TOOLENTRY_DEFAULT + Link.NONTRANSLATED, null, this);
			if (markAsDownloadLink) {
				LinkFactory.markDownloadLink(linkCmp);
			}
			myLinks.add(linkCmp);
			addComponent(linkCmp, ident);
		} else if (cssClass.equals("b_toolbox_close")) {
			Link linkCmp = LinkFactory.createCustomLink(linkCmpName, action, text, Link.TOOLENTRY_CLOSE + Link.NONTRANSLATED, null, this);
			if (markAsDownloadLink) {
				LinkFactory.markDownloadLink(linkCmp);
			}
			myLinks.add(linkCmp);
			addComponent(linkCmp, ident);
		} else {
			entries.add(new ToolEntry(ident, action, text, cssClass));
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tool.ToolController#addLink(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void addLink(String action, String text, String ident, String cssClass) {
		addLink(action, text, ident, cssClass, false);
	}

	/**
	 * @see org.olat.core.gui.control.generic.tool.ToolController#addPopUpLink(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 *      java.lang.String, boolean)
	 */
	@Override
	public void addPopUpLink(String action, String text, String ident, String cssClass, String width, String height, boolean browserMenubarEnabled) {
		entries.add(new ToolEntry(ident, action, text, cssClass, width, height, browserMenubarEnabled));
	}

	/**
	 * @see org.olat.core.gui.control.generic.tool.ToolController#addComponent(org.olat.core.gui.components.Component)
	 */
	@Override
	public void addComponent(Component component) {
		addComponent(component, null);
	}

	/**
	 * @see org.olat.core.gui.control.generic.tool.ToolController#addComponent(org.olat.core.gui.components.Component, java.lang.String)
	 */
	@Override
	public void addComponent(Component component, String ident) {
		String internalCompName = "c" + (id++);
		// add as child of container so it can be rendered
		content.put(internalCompName, component);
		entries.add(new ToolEntry(ident, internalCompName, component));
	}

	/**
	 * @see org.olat.core.gui.control.generic.tool.ToolController#remove(java.lang.String)
	 */
	@Override
	public void remove(String ident) {
		int pos = getToolEntryPosition(ident);
		if (pos == -1) throw new AssertException("Trying to remove a ToolEntry that does not exist.");
		entries.remove(pos);
	}

	private int getToolEntryPosition(String ident) {
		for (int i = 0; i < entries.size(); i++) {
			ToolEntry entry = (ToolEntry) entries.get(i);
			String entryIdent = entry.getIdent();
			if (entryIdent != null && entryIdent.equals(ident)) return i;
		}
		return -1;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == content) {
			String action = event.getCommand();
			if (action == null) throw new AssertException("no action for a tool command");
			// hack check: make sure we only fire clicks that are indeed listed
			boolean isInBox = false;
			int entCnt = entries.size();
			for (int i = 0; i < entCnt; i++) {
				ToolEntry entry = (ToolEntry) entries.get(i);
				String entryAction = entry.getAction();
				if (entryAction != null && entryAction.equals(action)) {
					isInBox = true;
					break;
				}
			}
			if (!isInBox) throw new AssertException("trying to fire a command which is not in the tool: action=" + action);
			// must be links, forward event to controllerlistener(s)
			fireEvent(ureq, event);
		} else if (myLinks.contains(source)) {
			fireEvent(ureq, event);
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tool.ToolController#setEnabled(java.lang.String, boolean)
	 */
	@Override
	public void setEnabled(String ident, boolean enabled) {
		int pos = getToolEntryPosition(ident);
		if (pos == -1) throw new AssertException("Trying to enable/disable a ToolEntry that does not exist.");
		ToolEntry entry = (ToolEntry) entries.get(pos);
		boolean wasEnabled = entry.isEnabled();
		entry.setEnabled(enabled);

		if (wasEnabled ^ enabled) {
			content.setDirty(true);
			if (entry.getComponent() instanceof Link) {
				Link lnk = (Link) entry.getComponent();
				lnk.setEnabled(enabled);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// nothing to do yet
	}

	/**
	 * @see org.olat.core.gui.control.generic.tool.ToolController#setDragEnabled(boolean)
	 */
	@Override
	public void setDragEnabled(boolean dragEnabled) {
		this.dragEnabled = dragEnabled;
		content.contextPut("offerdrag", Boolean.valueOf(dragEnabled));
	}

	/**
	 * @see org.olat.core.gui.control.generic.tool.ToolController#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return entries.isEmpty();
	}

}