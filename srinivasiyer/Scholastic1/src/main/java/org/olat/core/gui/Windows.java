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

package org.olat.core.gui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.olat.core.gui.components.Window;
import org.olat.core.gui.control.Disposable;
import org.olat.core.gui.control.winmgr.WindowManagerImpl;
import org.olat.core.gui.util.bandwidth.SlowBandWidthSimulator;
import org.olat.core.gui.util.bandwidth.SlowBandWidthSimulatorImpl;
import org.olat.core.util.FIFOMap;
import org.olat.core.util.UserSession;

/**
 * @author Felix Jost
 */

public class Windows implements Disposable {

	private static final String SESSIONID_NAME_FOR_WINDOWS = Windows.class.getName();

	// TODO: better solution with named chiefcontrollers
	private FIFOMap windows = new FIFOMap(100); // one user may at most save 100
	// windows in a session
	private int windowId = 1;
	private WindowManager windowManagerImpl;

	private Map<String, Object> attributes = new HashMap<String, Object>();

	private SlowBandWidthSimulator sbws;

	private Windows() {
		// private constructor
		windowManagerImpl = new WindowManagerImpl();
	}

	/**
	 * @param ureq
	 * @return the Windows for this user
	 */
	public static Windows getWindows(UserRequest ureq) {
		UserSession us = ureq.getUserSession();
		return getWindows(us);
	}

	/**
	 * @param us
	 * @return the Windows for this user
	 */
	public static Windows getWindows(UserSession us) {
		Windows ws = (Windows) us.getEntry(SESSIONID_NAME_FOR_WINDOWS);
		if (ws == null) {
			ws = new Windows();
			// make window id kind of unique (only needed for better user convenience
			// when a user tries to bookmark an url and uses that browser bookmark
			// later
			// TODO: make error handling better
			// ws.windowId = (int) (System.currentTimeMillis() % 10) * 10 + 1; // must
			// at least be 1, since 0 is null
			us.putEntry(SESSIONID_NAME_FOR_WINDOWS, ws);
		}
		return ws;
	}

	/**
	 * @param wId
	 * @return true if there is a window with this windowId
	 */
	public boolean isExisting(String wId) {
		return (getWindow(wId) != null);
	}

	/**
	 * the url must be a valid dispatchUri (have a windowId), otherwise an exception is thrown
	 * 
	 * @param ureq
	 * @return null if no window to this windowId could be found or if the windowid is not in the request (e.g. shibbolthe nice error msg)
	 */
	public Window getWindow(UserRequest ureq) {
		String windowID = ureq.getWindowID();
		if (windowID == null) return null;
		return getWindow(windowID);
	}

	/**
	 * gets the window.
	 * 
	 * @param windowID
	 * @return null if the window is not existing (here in windows)
	 */
	private Window getWindow(String windowID) {
		Window w = (Window) windows.get(windowID);
		return w;
	}

	/**
	 * @param w
	 * @return true if already registered
	 */
	public boolean isRegistered(Window w) {
		String id = w.getInstanceId();
		return (windows.get(id) != null);
	}

	/**
	 * registers the window. if the window is already registered, then nothing is done
	 * 
	 * @param w
	 */
	public void registerWindow(Window w) {
		String id = w.getInstanceId();
		if (windows.get(id) != null) return; // we are already registered
		String wiid = String.valueOf(windowId++); // ok since per user session, not
		// global
		// //String.valueOf(CodeHelper.getRAMUniqueID());
		w.setInstanceId(wiid);
		windows.put(wiid, w);
	}

	/**
	 * in case you close a window you should also deregister it. This implies that deregistering does not clean up, e.g. dispose the window!
	 * 
	 * @param w
	 */
	public void deregisterWindow(Window w) {
		String id = w.getInstanceId();
		// no longer available
		if (windows.get(id) == null) return;
		windows.remove(id);
	}

	/**
	 * @return the iterator with windows in it
	 */
	public Iterator getWindowIterator() {
		return windows.getValueIterator();
	}

	/**
	 * @return the number of windows
	 */
	public int getWindowCount() {
		return windows.size();
	}

	/**
	 * @return Returns the windowId.
	 */
	public int getWindowId() {
		return windowId;
	}

	/**
	 * @return Returns the windowManager.
	 */
	public WindowManager getWindowManager() {
		return windowManagerImpl;
	}

	/**
	 * @return Returns the windows.
	 */
	public FIFOMap getWindows() {
		return windows;
	}

	public void setAttribute(String key, Object value) {
		attributes.put(key, value);
	}

	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.control.Disposable#dispose(boolean)
	 */
	@Override
	public void dispose() {
		((Disposable) windowManagerImpl).dispose();
	}

	/**
	 * @return
	 */
	public SlowBandWidthSimulator getSlowBandWidthSimulator() {
		if (sbws == null) sbws = new SlowBandWidthSimulatorImpl();
		return sbws;
	}

}