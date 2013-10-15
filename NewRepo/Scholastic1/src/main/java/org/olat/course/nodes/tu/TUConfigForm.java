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

package org.olat.course.nodes.tu;

import java.net.MalformedURLException;
import java.net.URL;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.util.StringHelper;
import org.olat.modules.ModuleConfiguration;

/**
 * Description:<BR/>
 * TODO: Class Description for TUConfigForm
 * <P/>
 * Initial Date: Oct 12, 2004
 * 
 * @author Felix Jost
 * @author Lars Eberle (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class TUConfigForm extends FormBasicController {
	/** config option: password */
	public static final String CONFIGKEY_PASS = "pass";
	/** config option: username */
	public static final String CONFIGKEY_USER = "user";
	/** config option: port */
	public static final String CONFIGKEY_PORT = "port";
	/** config option: uri */
	public static final String CONFIGKEY_URI = "uri";
	/** config option: query */
	public static final String CONFIGKEY_QUERY = "query";
	/** config option: hostname */
	public static final String CONFIGKEY_HOST = "host";
	/** config option: protocol */
	public static final String CONFIGKEY_PROTO = "proto";
	/** supported protocols */
	public static final String[] PROTOCOLS = new String[] { "http", "https" };

	/** Configuration key: use tunnel for iframe or display directly ("<iframe src='www.ethz.ch'></iframe>"). Values: true, false **/
	public static final String CONFIG_TUNNEL = "useframetunnel"; // don't change value, used in config

	/** Configuration key: display content in iframe: Values: true, false **/
	public static final String CONFIG_IFRAME = "iniframe";

	/** Configuration key: display content in new browser window: Values: true, false **/
	public static final String CONFIG_EXTERN = "extern";

	/*
	 * They are only used inside this form and will not be saved anywhere, so feel free to change them...
	 */

	private static final String OPTION_TUNNEL_THROUGH_OLAT_INLINE = "tunnelInline";
	private static final String OPTION_TUNNEL_THROUGH_OLAT_IFRAME = "tunnelIFrame";
	private static final String OPTION_SHOW_IN_OLAT_IN_AN_IFRAME = "directIFrame";
	private static final String OPTION_SHOW_IN_NEW_BROWSER_WINDOW = "extern";

	/*
	 * NLS support:
	 */

	private static final String NLS_OPTION_TUNNEL_INLINE_LABEL = "option.tunnel.inline.label";
	private static final String NLS_OPTION_TUNNEL_IFRAME_LABEL = "option.tunnel.iframe.label";
	private static final String NLS_OPTION_OLAT_IFRAME_LABEL = "option.olat.iframe.label";
	private static final String NLS_OPTION_EXTERN_PAGE_LABEL = "option.extern.page.label";
	private static final String NLS_DESCRIPTION_LABEL = "description.label";
	private static final String NLS_DESCRIPTION_PREAMBLE = "description.preamble";
	private static final String NLS_DISPLAY_CONFIG_EXTERN = "display.config.extern";

	private final ModuleConfiguration config;

	private TextElement thost;
	private TextElement tuser;
	private TextElement tpass;

	private SingleSelection selectables;
	private final String[] selectableValues, selectableLabels;

	String user, pass;
	String fullURI;
	private MultipleSelectionElement checkboxPagePasswordProtected;

	/**
	 * Constructor for the tunneling configuration form
	 * 
	 * @param name
	 * @param config
	 * @param withCancel
	 */
	public TUConfigForm(final UserRequest ureq, final WindowControl wControl, final ModuleConfiguration config, final boolean withCancel) {
		super(ureq, wControl);
		this.config = config;
		final int configVersion = config.getConfigurationVersion();

		final String proto = (String) config.get(CONFIGKEY_PROTO);
		final String host = (String) config.get(CONFIGKEY_HOST);
		String uri = (String) config.get(CONFIGKEY_URI);

		if (uri != null && uri.length() > 0 && uri.charAt(0) == '/') {
			uri = uri.substring(1);
		}
		String query = null;
		if (configVersion == 2) {
			// query string is available since config version 2
			query = (String) config.get(TUConfigForm.CONFIGKEY_QUERY);
		}
		final Integer port = (Integer) config.get(CONFIGKEY_PORT);

		user = (String) config.get(CONFIGKEY_USER);
		pass = (String) config.get(CONFIGKEY_PASS);

		fullURI = getFullURL(proto, host, port, uri, query).toString();

		selectableValues = new String[] { OPTION_TUNNEL_THROUGH_OLAT_INLINE, OPTION_TUNNEL_THROUGH_OLAT_IFRAME, OPTION_SHOW_IN_OLAT_IN_AN_IFRAME,
				OPTION_SHOW_IN_NEW_BROWSER_WINDOW };

		selectableLabels = new String[] { translate(NLS_OPTION_TUNNEL_INLINE_LABEL), translate(NLS_OPTION_TUNNEL_IFRAME_LABEL), translate(NLS_OPTION_OLAT_IFRAME_LABEL),
				translate(NLS_OPTION_EXTERN_PAGE_LABEL) };

		initForm(ureq);
	}

	public static StringBuilder getFullURL(final String proto, final String host, final Integer port, final String uri, final String query) {
		final StringBuilder fullURL = new StringBuilder();
		if (proto != null && host != null) {
			fullURL.append(proto).append("://");
			fullURL.append(host);
			if (port != null) {
				if (proto.equals("http") || proto.equals("https")) {
					if (proto.equals("http") && port.intValue() != 80) {
						fullURL.append(":" + port);
					} else if (proto.equals("https") && port.intValue() != 443) {
						fullURL.append(":" + port);
					}
				} else {
					fullURL.append(":" + port);
				}
			}
			if (uri == null) {
				fullURL.append("/");
			} else {
				// append "/" if not already there, old configurations might have no "/"
				if (uri.indexOf("/") != 0) {
					fullURL.append("/");
				}
				fullURL.append(uri);
			}
			if (query != null) {
				fullURL.append("?").append(query);
			}
		}
		return fullURL;
	}

	@Override
	protected boolean validateFormLogic(final UserRequest ureq) {
		try {
			new URL(thost.getValue());
		} catch (final MalformedURLException e) {
			thost.setErrorKey("TUConfigForm.invalidurl", null);
			return false;
		}
		return true;
	}

	private String convertConfigToNewStyle(final ModuleConfiguration cfg) {
		final Boolean tunnel = cfg.getBooleanEntry(CONFIG_TUNNEL);
		final Boolean iframe = cfg.getBooleanEntry(CONFIG_IFRAME);
		final Boolean extern = cfg.getBooleanEntry(CONFIG_EXTERN);
		if (tunnel == null && iframe == null && extern == null) { // nothing saved yet
			return OPTION_TUNNEL_THROUGH_OLAT_INLINE;
		} else { // something is saved ...
			if (extern != null && extern.booleanValue()) { // ... it was extern...
				return OPTION_SHOW_IN_NEW_BROWSER_WINDOW;
			} else if (tunnel != null && tunnel.booleanValue()) { // ... it was tunneled
				if (iframe != null && iframe.booleanValue()) { // ... and in a iframe
					return OPTION_TUNNEL_THROUGH_OLAT_IFRAME;
				} else { // ... no iframe
					return OPTION_TUNNEL_THROUGH_OLAT_INLINE;
				}
			} else { // ... no tunnel means inline
				return OPTION_SHOW_IN_OLAT_IN_AN_IFRAME;
			}
		}
	}

	/**
	 * @return the updated module configuration using the form data
	 */
	public ModuleConfiguration getUpdatedConfig() {
		URL url = null;
		try {
			url = new URL(thost.getValue());
		} catch (final MalformedURLException e) {
			throw new OLATRuntimeException("MalformedURL in TUConfigForm which should not happen, since we've validated before. URL: " + thost.getValue(), e);
		}
		config.setConfigurationVersion(2);
		config.set(CONFIGKEY_PROTO, url.getProtocol());
		config.set(CONFIGKEY_HOST, url.getHost());
		config.set(CONFIGKEY_URI, url.getPath());
		config.set(CONFIGKEY_QUERY, url.getQuery());
		final int portHere = url.getPort();
		config.set(CONFIGKEY_PORT, new Integer(portHere != -1 ? portHere : url.getDefaultPort()));
		config.set(CONFIGKEY_USER, getFormUser());
		config.set(CONFIGKEY_PASS, getFormPass());

		// now save new mapped config:
		final String selected = selectables.getSelectedKey();

		// if content should be show in extern window
		config.setBooleanEntry(CONFIG_EXTERN, selected.equals(OPTION_SHOW_IN_NEW_BROWSER_WINDOW));
		// if content should be tunneled
		config.setBooleanEntry(CONFIG_TUNNEL, (selected.equals(OPTION_TUNNEL_THROUGH_OLAT_INLINE) || selected.equals(OPTION_TUNNEL_THROUGH_OLAT_IFRAME)));
		// if content should be displayed in iframe
		config.setBooleanEntry(CONFIG_IFRAME, (selected.equals(OPTION_TUNNEL_THROUGH_OLAT_IFRAME) || selected.equals(OPTION_SHOW_IN_OLAT_IN_AN_IFRAME)));
		return config;
	}

	private String getFormUser() {
		if (StringHelper.containsNonWhitespace(tuser.getValue())) {
			return tuser.getValue();
		} else {
			return null;
		}
	}

	private String getFormPass() {
		return tpass.getValue();
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {

		thost = uifactory.addTextElement("st", "TUConfigForm.url", 255, fullURI, formLayout);
		thost.setExampleKey("form.url.example", null);
		thost.setMandatory(true);

		uifactory.addStaticTextElement("expl", NLS_DESCRIPTION_LABEL, translate(NLS_DESCRIPTION_PREAMBLE), formLayout);

		final String loadedConfig = convertConfigToNewStyle(config);
		selectables = uifactory.addRadiosVertical("selectables", NLS_DISPLAY_CONFIG_EXTERN, formLayout, selectableValues, selectableLabels);
		selectables.select(loadedConfig, true);
		selectables.addActionListener(this, FormEvent.ONCLICK);

		checkboxPagePasswordProtected = uifactory.addCheckboxesVertical("checkbox", "TUConfigForm.protected", formLayout, new String[] { "ison" }, new String[] { "" },
				null, 1);

		checkboxPagePasswordProtected.select("ison", (user != null) && !user.equals(""));
		// register for on click event to hide/disable other elements
		checkboxPagePasswordProtected.addActionListener(listener, FormEvent.ONCLICK);

		tuser = uifactory.addTextElement("user", "TUConfigForm.user", 255, user == null ? "" : user, formLayout);
		tpass = uifactory.addPasswordElement("pass", "TUConfigForm.pass", 255, pass == null ? "" : pass, formLayout);

		uifactory.addFormSubmitButton("submit", formLayout);

		update();
	}

	@SuppressWarnings("unused")
	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		update();
	}

	private void update() {
		// Checkbox 'page password protected' only visible when OPTION_TUNNEL_THROUGH_OLAT_INLINE or OPTION_TUNNEL_THROUGH_OLAT_IFRAME
		checkboxPagePasswordProtected.setVisible(selectables.isSelected(0) || selectables.isSelected(1));
		if (checkboxPagePasswordProtected.isSelected(0) && checkboxPagePasswordProtected.isVisible()) {
			tuser.setVisible(true);
			tpass.setVisible(true);
		} else {
			tuser.setValue("");
			tuser.setVisible(false);
			tpass.setValue("");
			tpass.setVisible(false);
		}
	}

	@Override
	protected void doDispose() {
		//
	}
}