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

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.render.ValidationResult;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.logging.activity.ThreadLocalUserActivityLoggerInstaller;
import org.olat.core.util.CodeHelper;

/**
 * Description: <br>
 * 
 * @author Felix Jost
 */
public abstract class Component {

	private static final OLog log_ = Tracing.createLoggerFor(Component.class);

	private boolean spanReplaceable = false;

	private String name;
	private long dispatchID;

	private int timestamp = 1;

	private boolean visible = true;
	private boolean enabled = true;
	// true when contents have changed since last rendering
	private boolean dirty = false;
	private boolean domReplaceable = true;

	private List<Controller> listeners;
	private Translator translator;
	// for debugging reasons to trace where the latest dispatch occured
	private Controller latestDispatchedController;
	// for debugging reasons to trace which event was latest fired before an
	// exception
	private Event latestFiredEvent;
	// watch only

	/**
	 * do not create a logger for this class otherwise millions of useless loggers are created which consumes quite some memory
	 */

	private Container parent = null;

	/**
	 * @param name the name of this component
	 */
	public Component(String name) {
		this(name, null);
	}

	/**
	 * @param name the name of this component
	 * @param translator the translator
	 */
	public Component(String name, Translator translator) {
		dispatchID = CodeHelper.getRAMUniqueID();
		this.name = name;
		this.translator = translator;
		listeners = new ArrayList<Controller>(2);
	}

	/**
	 * @return String
	 */
	public String getComponentName() {
		return name;
	}

	/**
	 * @return boolean
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * True by default: The component gets rendered<br>
	 * false: The componet gets not rendered. Sets the visible.
	 * 
	 * @param visible The visible to set
	 */
	public void setVisible(boolean visible) {
		if (visible ^ this.visible) {
			this.visible = visible;
			setDirty(true);
		}
	}

	public void dispatchRequest(final UserRequest ureq) {
		doDispatchRequest(ureq);
	}

	protected abstract void doDispatchRequest(UserRequest ureq);

	public abstract ComponentRenderer getHTMLRendererSingleton();

	/**
	 * called just before the rendering of the -whole tree- takes place, so e.g. lazy fetching can be implemented, or issueing a request for a new moduleUri (e.g. for
	 * CPComponent, so that the browser loads images correctly). only called when the component is visible
	 */
	public void validate(UserRequest ureq, ValidationResult vr) {
		if (this.dirty) {
			timestamp++;
			if (Tracing.isDebugEnabled(this.getClass())) Tracing.logDebug(
					"increment component.timestamp new value=" + timestamp + " ureq=" + ureq + " component=" + this, this.getClass());
		}
	}

	/**
	 * fires events to registered listeners of generic events. To see all events set this class and also AbstractEventBus and DefaultController to debug.
	 * 
	 * @param event
	 * @param ores
	 */
	protected void fireEvent(final UserRequest ureq, final Event event) {
		Controller[] listenerArray = new Controller[listeners.size()];
		listeners.toArray(listenerArray);

		for (Controller listenerA : listenerArray) {
			final Controller listener = listenerA;
			latestDispatchedController = listenerA;
			latestFiredEvent = event;
			try {
				listener.getUserActivityLogger().frameworkSetBusinessPathFromWindowControl(listener.getWindowControlForDebug());
			} catch (AssertException e) {
				log_.error("Error in setting up the businessPath on the IUserActivityLogger. listener=" + listener, e);
				// still continue
			}

			ThreadLocalUserActivityLoggerInstaller.runWithUserActivityLogger(new Runnable() {

				@Override
				public void run() {
					listener.dispatchEvent(ureq, Component.this, event);
				}

			}, listener.getUserActivityLogger());
			// clear the event for memory reasons, used only for debugging reasons in
			// case of an error
			// TODO:fj: may be useful for admin purposes
			// FIXME:fj:a rework events so we can get useful info out of it
			latestFiredEvent = null;

		}
	}

	/**
	 * @return a list of the controllers listening (normally only one)
	 */
	public List<Controller> debuginfoGetListeners() {
		return listeners;
	}

	/**
	 * @param controller
	 */
	public void addListener(Controller controller) {
		// tests if the same controller was already registered to avoid
		// double-firing.
		// the contains method is fast since there is normally one one listener
		// (controller) in the listener list.
		if (listeners.contains(controller)) throw new AssertException("controller was already added to component '" + getComponentName() + "', controller was: "
				+ controller.toString());
		listeners.add(controller);
	}

	public void removeListener(Controller controller) {
		listeners.remove(controller);
	}

	/**
	 * @return Translator
	 */
	public Translator getTranslator() {
		return translator;
	}

	/**
	 * Sets the translator. (for framework internal use)
	 * 
	 * @param translator The translator to set
	 */
	protected void setTranslator(Translator translator) {
		this.translator = translator;
	}

	/**
	 * @return long the dispatchid (which is assigned at construction time of the component and never changes)
	 */
	public long getDispatchID() {
		return dispatchID;
	}

	/**
	 * @return the extended debuginfo
	 */
	public String getExtendedDebugInfo() {
		// default impl to be overriden
		return "n/a";
	}

	/**
	 * @return
	 */
	public String getListenerInfo() {
		return "listener:" + listeners.toString();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getComponentName() + " " + super.toString();
	}

	/**
	 * true by default: The componet gets rendered and actions get dispatched if false: e.g. @see Link the link gets rendered but is not clickable
	 * 
	 * @return Returns the enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled The enabled to set.
	 */
	public void setEnabled(boolean enabled) {
		if (enabled ^ this.enabled) {
			setDirty(true);
		}
		this.enabled = enabled;
	}

	/**
	 * only for debug purposes!!!
	 * 
	 * @return Returns the latestDispatchedController.
	 */
	public Controller getLatestDispatchedController() {
		return latestDispatchedController;
	}

	/**
	 * only for debugging reasons!!!
	 * 
	 * @return Returns the latestFiredEvent.
	 */
	public Event getAndClearLatestFiredEvent() {
		Event tmp = latestFiredEvent;
		latestFiredEvent = null; // gc
		return tmp;
	}

	/**
	 * @return returns whether the component needs to be rerendered or not
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * used by the screenreader feature to determine whether the component has changed from a user's perspective. normally this is the same as isDirty(), but may differ,
	 * e.g. for a MenuTree (expanding the tree: true; activating a link: false)
	 * 
	 * @see org.olat.core.gui.components.tree.MenuTree
	 * @return whether the component has changed from a user's perspective.
	 */
	public boolean isDirtyForUser() {
		// default implementation
		return isDirty();
	}

	/**
	 * @param dirty The dirty to set.
	 */
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	/**
	 * @return Returns the domReplaceable.
	 */
	public boolean isDomReplaceable() {
		return domReplaceable;
	}

	/**
	 * if set to true(default), then this component can be swapped out in the browser dom tree if that capability is enabled
	 * 
	 * @param domReplaceable The domReplaceable to set.
	 */
	public void setDomReplaceable(boolean domReplaceable) {
		this.domReplaceable = domReplaceable;
	}

	public void setSpanAsDomReplaceable(boolean spanReplaceable) {
		this.spanReplaceable = spanReplaceable;
	}

	public boolean getSpanAsDomReplaceable() {
		return this.spanReplaceable;
	}

	/**
	 * to be called only by the container when a child is added
	 * 
	 * @param parent
	 * @deprecated
	 */
	@Deprecated
	void setParent(Container parent) {
		this.parent = parent;
	}

	/**
	 * @return
	 */
	public Container getParent() {
		return this.parent;
	}

	/**
	 * to be used by Window.java to detect browser back in ajax-mode
	 * 
	 * @return Returns the timestamp.
	 */
	public int getTimestamp() {
		return timestamp;
	}

}