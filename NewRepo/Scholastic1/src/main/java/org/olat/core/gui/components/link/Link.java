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

package org.olat.core.gui.components.link;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.Tracing;

/**
 * Description:<br>
 * Use @see {@link LinkFactory} to get Link objects.
 * <P>
 * Initial Date: July 06, 2006 <br>
 * 
 * @author Alexander Schneider, Patrick Brunner
 */
public class Link extends Component {
	// single renderer for all users, lazy creation upon first object creation of this class.
	private static final ComponentRenderer RENDERER = new LinkRenderer();

	/**
	 * each can be combined with {@link Link#NONTRANSLATED}<br>
	 * can not be combined with each other!
	 */
	public static final int LINK_CUSTOM_CSS = 0;
	public static final int BUTTON_XSMALL = 1;
	public static final int BUTTON_SMALL = 2;
	public static final int BUTTON = 3;
	public static final int LINK_BACK = 4;
	public static final int LINK = 5;
	/**
	 * to be refactored later into own components
	 */
	public static final int TOOLENTRY_DEFAULT = 6;
	public static final int TOOLENTRY_CLOSE = 7;

	/**
	 * can be added to one of the following:
	 */
	public static final int NONTRANSLATED = 16;
	public static final int FLEXIBLEFORMLNK = 32;

	private String command;
	private int presentation;
	private int presentationBeforeCustomCSS;
	private String i18n;
	private String title;
	private String elementId;
	private String textReasonForDisabling;
	private String customDisplayText;
	private String customEnabledLinkCSS;
	private String customDisabledLinkCSS;
	private String target;
	boolean markIt = false;
	private Object internalAttachedObj;
	private Object userObject;
	private String accessKey;
	private boolean ajaxEnabled = true;
	boolean registerForMousePositionEvent = false;
	MouseEvent mouseEvent;
	String javascriptHandlerFunction;
	// x y coordinates of the mouse position when clicked the link, works only if enabled by registerForMousePositionEvent(true)
	private int offsetX = 0;
	private int offsetY = 0;

	boolean hasTooltip;
	boolean hasStickyTooltip;
	Component tooltipContent;

	private boolean suppressDirtyFormWarning = false;
	private boolean isDownloadLink = false;

	/**
	 * @param name
	 * @param command should not contain :
	 * @param i18n
	 * @param presentation
	 * @param title
	 */
	protected Link(String name, String command, String i18n, int presentation, VelocityContainer vc, Controller listeningController) {
		this(name, command, i18n, presentation, null);
		if (listeningController == null) throw new AssertException("please provide a listening controller, listeningController is null at the moment");
		addListener(listeningController);
		if (vc != null) vc.put(getComponentName(), this);
		setSpanAsDomReplaceable(true);
	}

	/**
	 * the new flexible forms needs links where the VC and listener are unknown at construction time.
	 * 
	 * @param name
	 * @param command
	 * @param i18n
	 * @param presentation
	 */
	protected Link(String name, String command, String i18n, int presentation, Object internalAttachedObj) {
		super(name);
		this.internalAttachedObj = internalAttachedObj;
		this.command = command;
		if (this.command == null || this.command.equals("")) throw new AssertException("command string must be a valid string and not null");
		this.i18n = i18n;
		this.presentation = presentation;
		this.presentationBeforeCustomCSS = presentation;

		if (Tracing.isDebugEnabled(Link.class)) {
			Tracing.logDebug("***LINK_CREATED***" + " name: " + getComponentName() + " component: " + getComponentName() + " dispatchId: " + getDispatchID(), Link.class);
		}
		setElementId("o_lnk" + getDispatchID());
		// use span wrappers - if the custom layout needs div wrappers this flag has
		// to be set manually
		setSpanAsDomReplaceable(true);
	}

	/**
	 * @see org.olat.core.gui.components.Component#dispatchRequest(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void doDispatchRequest(UserRequest ureq) {
		setDirty(true);
		String cmd = ureq.getParameter(VelocityContainer.COMMAND_ID);

		if (Tracing.isDebugEnabled(Link.class)) {
			Tracing.logDebug("***LINK_CLICKED*** " + " dispatchID: " + ureq.getComponentID() + " commandID: " + cmd, Link.class);
		}

		dispatch(ureq, command);

	}

	/**
	 * @param ureq
	 * @param command2
	 */
	private void dispatch(UserRequest ureq, String cmd) {
		if (!command.equals(cmd)) { throw new AssertException("hack attempt! command does not match the one from the UserRequest! Command recieved: " + cmd
				+ " expected: " + command); }
		if (registerForMousePositionEvent) setXYOffest(ureq);
		fireEvent(ureq, new Event(cmd));
	}

	/**
	 * @see org.olat.core.gui.components.Component#getHTMLRendererSingleton()
	 */
	@Override
	public ComponentRenderer getHTMLRendererSingleton() {
		return RENDERER;
	}

	public String getCommand() {
		return command;
	}

	public String getI18n() {
		return i18n;
	}

	public int getPresentation() {
		return presentation;
	}

	/**
	 * The link title, text that shows up when mouse hovers over link. Not the link text. <br>
	 * If ((getPresentation() - Link.NONTRANSLATED) >= 0 ) the returned title is already translated, otherwise an untranslated i18n key is returned
	 * 
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	Object getInternalAttachedObject() {
		return internalAttachedObj;
	}

	/**
	 * Set an link title which gets displayed when hovering over the link. <br>
	 * If ((getPresentation() - Link.NONTRANSLATED) >= 0 ) the provided title is already translated, otherwise its an untranslated i18n key
	 * 
	 * @param the i18n key or the translated key depending on presentation mode
	 */
	public void setTitle(String i18nKey) {
		this.title = i18nKey;
	}

	/**
	 * Only used in olat flexi form stuff
	 * 
	 * @return returns the custom setted element id
	 */
	protected String getElementId() {
		return this.elementId;
	}

	/**
	 * @see org.olat.core.gui.components.Component#setEnabled(boolean)
	 * @param true or false
	 */
	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		setDirty(true);
	}

	protected String getTextReasonForDisabling() {
		return textReasonForDisabling;
	}

	public void setTextReasonForDisabling(String textReasonForDisabling) {
		this.textReasonForDisabling = textReasonForDisabling;
	}

	/**
	 * @return the custom CSS class used for a disabled link
	 */
	public String getCustomDisabledLinkCSS() {
		// if (presentation % NONTRANSLATED != LINK_CUSTOM_CSS) {
		// throw new AssertException("Tried to get custom link CSS class but presentation mode is not set to LINK_CUSTOM_CSS");
		// }
		return customDisabledLinkCSS;
	}

	/**
	 * @param customDisabledLinkCSS the custom CSS class used for a disabled link
	 */
	public void setCustomDisabledLinkCSS(String customDisabledLinkCSS) {
		// if (presentation % NONTRANSLATED != LINK_CUSTOM_CSS) {
		// throw new AssertException("Tried to set custom link CSS class but presentation mode is not set to LINK_CUSTOM_CSS");
		// }
		this.customDisabledLinkCSS = customDisabledLinkCSS;

		// check if it is a flexi.form link with custom css
		boolean flexiformlink = (presentation - Link.FLEXIBLEFORMLNK) >= 0;
		if (flexiformlink) {
			presentation = presentation - Link.FLEXIBLEFORMLNK;
		}

		boolean nontranslated = (presentation - Link.NONTRANSLATED) >= 0;
		if (nontranslated) {
			presentation = Link.NONTRANSLATED;
		} else {
			presentation = Link.LINK_CUSTOM_CSS;
		}
		// enable the flexi.form info again
		if (flexiformlink) {
			presentation += Link.FLEXIBLEFORMLNK;
		}
		setDirty(true);
	}

	/**
	 * @return the custom CSS class used for a enabled link
	 */
	public String getCustomEnabledLinkCSS() {
		// if (presentation % NONTRANSLATED != LINK_CUSTOM_CSS) {
		// throw new AssertException("Tried to get custom link CSS class but presentation mode is not set to LINK_CUSTOM_CSS");
		// }
		return customEnabledLinkCSS;
	}

	/**
	 * @param customEnabledLinkCSS the custom CSS class used for a enabled link
	 */
	public void setCustomEnabledLinkCSS(String customEnabledLinkCSS) {
		// if (presentation % NONTRANSLATED != LINK_CUSTOM_CSS) {
		// throw new AssertException("Tried to set custom link CSS class but presentation mode is not set to LINK_CUSTOM_CSS");
		// }
		this.customEnabledLinkCSS = customEnabledLinkCSS;

		// check if it is a flexi.form link with custom css
		boolean flexiformlink = (presentation - Link.FLEXIBLEFORMLNK) >= 0;
		if (flexiformlink) {
			presentation = presentation - Link.FLEXIBLEFORMLNK;
		}

		boolean nontranslated = (presentation - Link.NONTRANSLATED) >= 0;
		if (nontranslated) {
			presentation = Link.NONTRANSLATED;
		} else {
			presentation = Link.LINK_CUSTOM_CSS;
		}
		// enable the flexi.form info again
		if (flexiformlink) {
			presentation += Link.FLEXIBLEFORMLNK;
		}
		setDirty(true);
	}

	public void removeCSS() {
		this.presentation = presentationBeforeCustomCSS;
		setDirty(true);
	}

	public String getTarget() {
		return target;
	}

	/**
	 * allows setting an custom href target like "_blank" which opens an link in a new window. As ajax links never should open in a new window, the setTarget
	 * automatically disables the ajax feature in the link.
	 * 
	 * @param target
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	public Object getUserObject() {
		return userObject;
	}

	public void setUserObject(Object userObject) {
		this.userObject = userObject;
	}

	/*
	 * this method should not be public, it is restricted to the link package. ID's must be unique and follow the requirements for html id names.
	 */
	void setElementId(String elementID) {
		this.elementId = elementID;
	}

	protected String getAccessKey() {
		return accessKey;
	}

	/**
	 * sets the accesskey, e.g. "5" -> Alt+5 then focusses on this link
	 * 
	 * @param accessKey
	 */
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public void setAjaxEnabled(boolean ajaxEnabled) {
		this.ajaxEnabled = ajaxEnabled;
	}

	public boolean isAjaxEnabled() {
		return ajaxEnabled;
	}

	/**
	 * When pressing this button, should the system prevent the check for any unsubmitted forms?
	 * 
	 * @param suppressDirtyFormWarning true: don't check for dirt forms; false: check for dirty forms (default)
	 */
	public void setSuppressDirtyFormWarning(boolean suppressDirtyFormWarning) {
		this.suppressDirtyFormWarning = suppressDirtyFormWarning;
	}

	/**
	 * @return true: don't check for dirt forms; false: check for dirty forms (default)
	 */
	public boolean isSuppressDirtyFormWarning() {
		return suppressDirtyFormWarning;
	}

	protected String getCustomDisplayText() {
		return customDisplayText;
	}

	public void setCustomDisplayText(String customDisplayText) {
		this.customDisplayText = customDisplayText;
		setDirty(true);
	}

	/**
	 * get the mouse position as event.command coded as x123y456 and appended to the UserRequest catch it inside the event method with the ureq.getModuleUri() method.<br>
	 * Uses prototype.js
	 * 
	 * @param b
	 */
	public void registerForMousePositionEvent(boolean b) {
		this.registerForMousePositionEvent = b;
	}

	/**
	 * register a javascript function to an event of this link TODO:gs:b may pass the event and the link element to the function as arguments <br>
	 * Uses prototype.js
	 * 
	 * @param event
	 * @param handlerFunction: A javascript function name
	 */
	public void registerMouseEvent(MouseEvent event, String handlerFunction) {
		this.mouseEvent = event;
		this.javascriptHandlerFunction = handlerFunction;
	}

	/**
	 * convenience method to set the x and y values you get by <code>link.registerForMousePositionEvent(true)</code> to x and y
	 * 
	 * @param ureq
	 * @param offsetX
	 * @param offsetY
	 */
	public void setXYOffest(UserRequest ureq) {
		String xyOffset = ureq.getModuleURI();
		if (xyOffset != null) {
			try {
				offsetX = Integer.parseInt(xyOffset.substring(1, xyOffset.indexOf("y")));
				offsetY = Integer.parseInt(xyOffset.substring(xyOffset.indexOf("y") + 1, xyOffset.length()));
			} catch (NumberFormatException e) {
				offsetX = 0;
				offsetY = 0;
			}
		}
	}

	/**
	 * valid events for the register mouse event stuff
	 */
	public enum MouseEvent {
		click, mousedown, mouseup, mouseover, mousemove, mouseout
	}

	/**
	 * returs the mouse position when the link was clicked. Only available if registerForMousePositionEvent is set true
	 * 
	 * @return offset x of mouse position
	 */
	public int getOffsetX() {
		return offsetX;
	}

	/**
	 * returs the mouse position when the link was clicked. Only available if registerForMousePositionEvent is set true
	 * 
	 * @return offset y of mouse position
	 */
	public int getOffsetY() {
		return offsetY;
	}

	/**
	 * Sets a tooltip out off the text from the provided i18n key. Tooltips are fastser appearing than normal title tags and can contain HTML tags.
	 * 
	 * @param sticky: sets the tooltip sticky, which means the user has to click the tip to disappear
	 */
	public void setTooltip(String tooltipI18nKey, boolean sticky) {
		setTitle(tooltipI18nKey);
		this.hasTooltip = true;
		this.hasStickyTooltip = sticky;
		setDirty(true);

	}

	/**
	 * sets a tooltip with a more complext content which is passed as an component
	 * 
	 * @param comp
	 * @param stricky if true the user has to close the tooltip himself
	 */
	public void setTooltip(Component comp, boolean stricky) {
		this.tooltipContent = comp;
		this.hasStickyTooltip = stricky;
		setDirty(true);
	}

	/**
	 * 
	 */
	void setStartsDownload() {
		isDownloadLink = true;
	}

	boolean getStartsDownload() {
		return isDownloadLink;
	}

}
