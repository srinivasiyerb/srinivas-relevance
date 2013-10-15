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

package org.olat.core.gui.control;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.state.ControllerState;
import org.olat.core.gui.control.state.ExtendedControllerState;
import org.olat.core.gui.control.state.StateConstants;
import org.olat.core.gui.control.winmgr.ControllerStateImpl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.logging.activity.ILoggingResourceable;
import org.olat.core.logging.activity.IUserActivityLogger;
import org.olat.core.logging.activity.ThreadLocalUserActivityLoggerInstaller;
import org.olat.core.logging.activity.UserActivityLoggerImpl;
import org.olat.core.util.RunnableWithException;
import org.olat.core.util.UserSession;
import org.olat.core.util.Util;
import org.olat.core.util.i18n.I18nModule;

/**
 * Description: <br>
 * 
 * @author Felix Jost
 */
public abstract class DefaultController implements Controller, ControllerEventListener {
	private static final String DEFAULTDISPOSED_PAGE = "defaultdisposed";
	OLog log = Tracing.createLoggerFor(DefaultController.class);
	// for memory watch only.
	private static AtomicInteger controllerCnt = new AtomicInteger(0);
	private final Object DISPOSE_LOCK = new Object();

	private List<ControllerEventListener> listeners;
	private Component initialComponent;
	private boolean disposed = false;
	private Panel wrapperPanel;
	private final IUserActivityLogger userActivityLogger;

	private WindowControl newWControl;
	private WindowControl origWControl = null;

	private ControllerState previousState = StateConstants.NULL_STATE;

	private ControllerState state = StateConstants.NULL_STATE;
	private boolean newTransition = false;
	private boolean isInDispatching;
	private Controller disposedMessageController = null;
	private Locale locale;

	/**
	 * for debugging / statistical information only!<br>
	 * 
	 * @return the number of controllers which are initialized but not yet disposed (disposed by state, not necessarily by the GC yet)
	 */
	public static int getControllerCount() {
		return controllerCnt.get();
	}

	/**
	 * if you need to instantiate controllers e.g. via spring -> see SpringController and SpringDefaultController
	 */
	@SuppressWarnings("unused")
	private DefaultController() {
		// prevent instantation

		// have to set userActivityLogger to null to comply with the fact that it's final
		userActivityLogger = null;
	}

	/**
	 * @param wControl should always be provided except for basechiefcontroller etc. which provide their own impl. of a windowcontrol
	 */
	protected DefaultController(WindowControl wControl) {
		controllerCnt.incrementAndGet();

		// set the ThreadLocalUserActivityLogger
		this.userActivityLogger = UserActivityLoggerImpl.setupLoggerForController(wControl);

		// wControl may be null, e.g. for DefaultChiefController.
		// normal controllers should provide a windowcontrol, even though they may not need it
		this.origWControl = wControl;
		if (wControl != null) {
			this.newWControl = new LocalWindowControl(wControl, this);
			// we inform the window's backoffice about the creation of a new controller, so that a suitable business control path
			// may be assembled (for jump-in / bookmarking feature)
			wControl.getWindowBackOffice().informControllerCreated(wControl, this);

		}

	}

	/**
	 * do NOT use normally. use the constructor super(wControl). only used for classes which are loaded by Class.forName and need an empty contstructor
	 * 
	 * @param wControl not null
	 */
	protected void setOrigWControl(WindowControl wControl) {
		if (this.origWControl != null) throw new AssertException("can only set origWControl once!");
		if (wControl == null) throw new AssertException("can not accept a null Windowcontrol here");
		this.origWControl = wControl;
		this.newWControl = new LocalWindowControl(wControl, this);
	}

	protected void overrideWindowControl(WindowControl wControl) {
		this.newWControl = wControl; // new LocalWindowControl(wControl, this);
	}

	/**
	 * @return the windowcontrol for this controller
	 */
	protected WindowControl getWindowControl() {
		if (newWControl == null) throw new AssertException("no windowcontrol set!");
		return newWControl;
	}

	/**
	 * 
	 */
	@Override
	public WindowControl getWindowControlForDebug() {
		return getWindowControl();
	}

	/**
	 * @see org.olat.core.gui.control.Controller#addControllerListener(org.olat.core.gui.control.ControllerEventListener)
	 */
	@Override
	public void addControllerListener(ControllerEventListener el) {
		if (listeners == null) {
			listeners = new ArrayList<ControllerEventListener>();
		}
		if (listeners.contains(el)) throw new AssertException("controllerEventListener '" + el.toString() + "' was already added to controller '" + toString());
		listeners.add(el);
	}

	// brasato:: prio c : clean up classes using this - does not make sense really
	protected List<ControllerEventListener> getListeners() {
		return listeners;
	}

	/**
	 * fires events to registered listeners of controller events. To see all events set this class and also AbstractEventBus and Component to debug.
	 * 
	 * @param event
	 * @param ores
	 */
	protected void fireEvent(UserRequest ureq, Event event) {
		if (listeners == null) return;
		for (Iterator<ControllerEventListener> iter = listeners.iterator(); iter.hasNext();) {
			ControllerEventListener listener = iter.next();
			if (log.isDebug()) log.debug("Controller event: " + this.getClass().getName() + ": fires event to: " + listener.getClass().getName());
			listener.dispatchEvent(ureq, this, event);
		}
	}

	/**
	 * @see org.olat.core.gui.control.Controller#dispatchEvent(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 *      Note: This method is final to make sure no subclass fiddles with this - core - framework method. As the framework part includes setting up
	 *      ThreadLocalUserActivityLogger etc
	 */
	@Override
	public final void dispatchEvent(UserRequest ureq, Component source, Event event) {
		if (!disposed) {
			// 1. dispatch the event
			isInDispatching = true;
			event(ureq, source, event);
			isInDispatching = false;

			// 2. if there is a windowcontrol (always except for a) legacy controllers or b) controllers which provide their own windowcontrol:
			// inform the windowbackoffice about the dispatching
			WindowControl ref = newWControl;
			if (ref != null) {
				ref.getWindowBackOffice().informControllerDispatched(ref, this, source, event);
			}
		} else {
			// COMMMENT:2008-02-28:pb: reviewed 'little hack' which is not a hack.
			// The introduced setDisposedMsgController allows the surrounding Controller
			// to set a specific controller for handling the disposed case. This
			// specific controller always has the correct language.
			//
			// The fall back is to present a DisposedMessage with the locale catched
			// here. Typically this is either because the programmer forgot to set
			// a specific disposed message where it is needed, or it is just a place
			// where it was not expected. And most of the times this indicates a
			// legacy or bad designed work flow.
			//
			// :::Original comment:::
			// since this abstract controller does not know the
			// locale of the current user, we catch it here upon dispatching
			// if a controller gets disposed asynchronously before it has been
			// dispatched, we use the default locale
			// :::----------------:::
			if (locale == null) {
				locale = ureq.getLocale();
			}

			// show message
			if (disposedMessageController != null && wrapperPanel != null) {
				wrapperPanel.setContent(disposedMessageController.getInitialComponent());
			} else if (wrapperPanel != null) {
				// place disposed message
				Translator pT = Util.createPackageTranslator(DefaultController.class, locale);
				Component dispMsgVC = new VelocityContainer(DEFAULTDISPOSED_PAGE, DefaultController.class, DEFAULTDISPOSED_PAGE, pT, null);
				wrapperPanel.pushContent(dispMsgVC);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.ControllerEventListener#dispatchEvent(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller,
	 *      org.olat.core.gui.control.Event)
	 */
	@Override
	public void dispatchEvent(final UserRequest ureq, final Controller source, final Event event) {
		if (!disposed) {
			getUserActivityLogger().frameworkSetBusinessPathFromWindowControl(getWindowControl());
			ThreadLocalUserActivityLoggerInstaller.runWithUserActivityLogger(new Runnable() {

				@Override
				public void run() {
					event(ureq, source, event);
				}

			}, getUserActivityLogger());
		} else {
			// show message
			if (disposedMessageController != null && wrapperPanel != null) {
				wrapperPanel.setContent(disposedMessageController.getInitialComponent());
			} else if (wrapperPanel != null) {
				if (locale == null) {
					locale = ureq.getLocale();
				}
				// place disposed message
				Translator pT = Util.createPackageTranslator(DefaultController.class, locale);
				Component dispMsgVC = new VelocityContainer(DEFAULTDISPOSED_PAGE, DefaultController.class, DEFAULTDISPOSED_PAGE, pT, null);
				wrapperPanel.pushContent(dispMsgVC);
			}
		}
	}

	/**
	 * the only method of the interface controllereventlistener. always gets called when a controller we 'subscribed' to fires an event. we provide a default
	 * implementation here since there are many controllers which are standalone and need no subcontrollers
	 * 
	 * @param ureq
	 * @param source
	 * @param event
	 */
	@SuppressWarnings("unused")
	protected void event(UserRequest ureq, Controller source, Event event) {
		// default impl does nothing
	}

	/**
	 * abstract event method for subclasses. the event received from the component we are listening to are always rerouted to this method here, except when the component
	 * has been disposed, in which case the events are simply ignored.
	 * 
	 * @param ureq
	 * @param source
	 * @param event
	 */
	protected abstract void event(UserRequest ureq, Component source, Event event);

	/**
	 * Sets the initialComponent.
	 * 
	 * @param initialComponent The mainComponent to set
	 */
	protected void setInitialComponent(Component initialComponent) {
		if (this.initialComponent != null) throw new AssertException("can only set initialcomponent once! comp:" + initialComponent.getComponentName() + ", controller: "
				+ toString());
		// wrap a panel around the initial component which is used when this
		// controller is disposed: after having called doDispose on subclasses
		// which clean up their subcontrollers by calling dispose (and therefore
		// also (must) cleanup the gui stack to the level our initial component
		// is at), we put a generic message in the panel that the user sees this
		// message upon the next click (which will not be dispatched, since the
		// component does not exist anymore,
		// but it will just be rerendered.
		// we also take care that no event is deliverd to implementors of this
		// abstract class after this controller has been disposed

		if (initialComponent instanceof Panel) {
			wrapperPanel = (Panel) initialComponent;
		} else {
			wrapperPanel = new Panel("autowrapper of controller " + this.getClass().getName());
			wrapperPanel.setContent(initialComponent);
		}
		this.initialComponent = wrapperPanel;
	}

	/**
	 * @return Component
	 */
	@Override
	public Component getInitialComponent() {
		return initialComponent;
	}

	/**
	 * Sets the UserRequest on this Controller's IUserActivityLogger.
	 * <p>
	 * The actual action is to set the session.
	 * 
	 * @param req the UserRequest from which the session is fetched
	 */
	protected void setLoggingUserRequest(UserRequest req) {
		IUserActivityLogger logger = getUserActivityLogger();
		if (logger == null) {
			// logger is never null - guaranteed.
			// throw this in the unlikely odd still
			throw new IllegalStateException("no logger set");
		}
		logger.frameworkSetSession(UserSession.getUserSessionIfAlreadySet(req.getHttpReq()));
	}

	/**
	 * Add a LoggingResourceable (e.g. a wrapped Course or a wrapped name of a CP file) to this Controller's IUserActivityLogger.
	 * <p>
	 * This method is usually called in the constructor of a Controller - in rarer cases it can be called outside constructors as well.
	 * 
	 * @param loggingResourceable the loggingResourceable to be set on this Controller's IUserActivityLogger
	 */
	public void addLoggingResourceable(ILoggingResourceable loggingResourceable) {
		IUserActivityLogger logger = getUserActivityLogger();
		if (logger == null) {
			// logger is never null - guaranteed.
			// throw this in the unlikely odd still
			throw new IllegalStateException("no logger set");
		}
		logger.addLoggingResourceInfo(loggingResourceable);
	}

	/**
	 * FRAMEWORK USE ONLY!
	 * <p>
	 * Returns the UserActivityLogger of this controller or null if no logger is set yet.
	 * <p>
	 * 
	 * @return UserActivityLogger of this controller or null if no logger is set
	 */
	@Override
	public IUserActivityLogger getUserActivityLogger() {
		return this.userActivityLogger;
	}

	/**
	 * Controller should override the method doDispose() instead of this one. makes sure that doDispose is only called once.
	 * 
	 * @param asynchronous if true, then this method is invoked by a different thread than the current user-gui-thread ("mouse-click-thread"). this means if set to true,
	 *            then you should inform the user by replacing the current render subtree of your controller's component with e.g. a velocitycontainer stating a message
	 *            like 'this object has been disposed by an other process/user. please click some other link to continue...
	 */
	@Override
	public synchronized void dispose() { // o_clusterOK by:fj
		// protect setting disposed to true by synchronized block
		synchronized (DISPOSE_LOCK) {// o_clusterok
			if (disposed) {
				return;
			} else {
				disposed = true; // disable any further event dispatching
			}
		}
		// dispose the controller now
		if (log.isDebug()) {
			log.debug("now disposing controller: " + this.toString());
		}

		try {
			ThreadLocalUserActivityLoggerInstaller.runWithUserActivityLoggerWithException(new RunnableWithException() {
				@Override
				public void run() throws Exception {
					doPreDispose();
					doDispose();
				}
			}, getUserActivityLogger());
		} catch (Exception e) {
			log.error("error while disposing controller: " + this.getClass().getName(), e);
		}
		if (log.isDebug()) {
			log.debug("end of: " + this.toString());
		}
		// FIXME:pb 2008-04-16 provide a default message controller without always create one?!
		// how much memory is used here
		if (disposedMessageController != null && wrapperPanel != null) {
			wrapperPanel.setContent(disposedMessageController.getInitialComponent());
		} else if (wrapperPanel != null) {
			if (locale == null) {
				// fallback to default locale
				locale = I18nModule.getDefaultLocale();
			}
			// place disposed message
			Translator pT = Util.createPackageTranslator(DefaultController.class, locale);
			Component dispMsgVC = new VelocityContainer(DEFAULTDISPOSED_PAGE, DefaultController.class, DEFAULTDISPOSED_PAGE, pT, null);
			wrapperPanel.pushContent(dispMsgVC);
		}

		controllerCnt.decrementAndGet();// count controller count down. this should event work if a disposed msg controller is created.

	}

	/**
	 * for classes like basiccontroller to get a hook for disposing without using doDispose()
	 */
	protected void doPreDispose() {
		// default impl does nothing
	}

	/**
	 * to be implemented by the concrete controllers to dispose resources, locks, subcontrollers, and so on
	 */
	protected abstract void doDispose();

	/**
	 * @see java.lang.Object#toString()
	 */

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("state: " + state + ", cListener:");
		if (listeners == null) {
			sb.append("-");
		} else {
			for (Iterator<ControllerEventListener> it_cl = listeners.iterator(); it_cl.hasNext();) {
				ControllerEventListener cev = it_cl.next();
				String ins = cev.getClass().getName(); // toString(); not cool when
				// controllers are listening to each other in a circle (should
				// never be, but then debug mode crashes because of infinite recursion.
				sb.append(ins).append(", ");
			}
		}
		return this.getClass().getName() + " [" + sb + "]";
	}

	/**
	 * @return the current state
	 */
	ControllerState getState() {
		return state;
	}

	protected void setState(String state) {
		doSetState(new ControllerStateImpl(state));
	}

	private void doSetState(@SuppressWarnings("hiding") ControllerState state) {
		if (!this.state.isSame(state)) {
			this.previousState = this.state;
			this.state = state;
			// a state change set as an effect of a call to adjustState(...) (browser-back/forward handling)
			// is not added to the history as a new transition - because we are just replaying the history,
			// not creating a new history entry
			newTransition = isInDispatching;
		} // else ignore if there is no real change (state A to state A)
	}

	/**
	 * called by the framework upon a direct jump url or as a result of the need to adjust a controller's state after pressing the browser-back-button (so that the
	 * back-button works as expected)
	 * 
	 * @param cstate the new state to change to. the concrete controller must adjust its own state to a state it once created. normally controllers will only remember the
	 *            "important" states - the ones that must be accessible using a permalink and which can be reached using browser-back-button
	 * @param ureq the UserRequest: using as normal, but calling ureq.getParameter(...) doesn't make sense here, since those are the parameters of a call in the past.
	 */
	@SuppressWarnings("unused")
	protected void adjustState(ControllerState cstate, UserRequest ureq) {
		// default impl does nothing
	}

	/**
	 * @see org.olat.core.gui.control.Controller#isDisposed()
	 */
	@Override
	public boolean isDisposed() {
		return disposed;
	}

	protected ExtendedControllerState createdExtendedControllerState() {
		ExtendedControllerState ecs;
		if (newTransition) {
			newTransition = false;
			ecs = new ExtendedControllerStateImpl(previousState, state, System.identityHashCode(this), this.getClass().getName());
		} else {
			// no transition has taken place in the mean time: indicate a state-to-same-state-transition which means no transition.
			ecs = new ExtendedControllerStateImpl(state, state, System.identityHashCode(this), this.getClass().getName());
		}

		// note defaultController.hashCode is not guaranteed to be unique, however most(all?) vm do
		return ecs;
	}

	/**
	 * register a controller creator which is used in case the controller was disposed and a specific message should be displayed.
	 * 
	 * @param disposeMsgControllerCreator
	 */
	protected void setDisposedMsgController(Controller disposeMsgController) {
		if (disposedMessageController != null) {
			// COMMENT:2008-04-27:pb only one disposed Message Controller allowed within a class hierarchy
			// so far no case in my mind where overriding makes sense.
			// typically a XYZRunMainController implementing a MainLayoutBasicController
			// will dispose all its children and place a disposed message in case of
			// an external event.
			String clazz = disposedMessageController.getClass().getName();
			Component comp = disposeMsgController.getInitialComponent();
			String compName = comp != null ? comp.getComponentName() : "Component is null";
			throw new AssertException("already defined disposedMsgController: " + clazz + " | " + compName);
		}
		disposedMessageController = disposeMsgController;
	}

}