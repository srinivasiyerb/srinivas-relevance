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

package org.olat.core.gui.control.generic.ajax.autocompletion;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.olat.core.dispatcher.mapper.Mapper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.winmgr.JSCommand;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.media.StringMediaResource;
import org.olat.core.gui.render.velocity.VelocityRenderDecorator;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.LogDelegator;

/**
 * Description:<br>
 * The AutoCompleterController provides an input field with a live-AJAX feed from the database. While typing, after entering a configurable amount of characters, the
 * system performs a server side search and shows a list of search results the user can choose from.
 * <p>
 * This controller uses ExtJS javascript library to implement the feature
 * <p>
 * Fires: an EntriesChosenEvent which contain the chosen entry/entries as strings
 * <P>
 * Initial Date: 06.10.2006 <br>
 * 
 * @author Felix Jost, FLorian Gnägi
 */
public class AutoCompleterController extends BasicController {
	private static final String CONTENT_TYPE_APPLICATION_X_JSON = "application/x-json";
	private static final String CONTENT_TYPE_TEXT_JAVASCRIPT = "text/javascript";
	private static final String RESPONSE_ENCODING = "utf-8";
	private static final String COMMAND_SELECT = "select";
	private static final String PARAM_CALLBACK = "callback";
	private static final String PARAM_QUERY = "query";
	private static final String PARAM_KEY = "key";
	private static final String JSNAME_INPUTFIELD = "b_autocomplete_input";
	private static final String JSNAME_DATASTORE = "autocompleterDatastore";
	private static final String JSNAME_COMBOBOX = "autocompleterCombobox";

	static final String AUTOCOMPLETER_NO_RESULT = "AUTOCOMPLETER_NO_RESULT";

	private VelocityContainer myContent;
	private Mapper mapper;
	private final ListProvider gprovider;
	private final String noResults;

	private String datastoreName;
	private String comboboxName;

	/**
	 * Constructor to create an auto completer controller
	 * 
	 * @param ureq The user request object
	 * @param wControl The window control object
	 * @param provider The provider that can be called to return the search-results for a given search query
	 * @param noResults The translated value to display when no results are found, e.g. "no matches found" or "-no users found-". When a NULL value is provided, the
	 *            controller will use a generic message.
	 * @param showDisplayKey true: show the key for each record; false: don't show the key, only the value
	 * @param inputWidth The input field width in characters
	 * @param minChars The minimum number of characters the user has to enter to perform a search
	 * @param label
	 */
	public AutoCompleterController(UserRequest ureq, WindowControl wControl, ListProvider provider, String noresults, final boolean showDisplayKey, int inputWidth,
			int minChars, String label) {
		super(ureq, wControl);
		this.gprovider = provider;
		this.noResults = (noresults == null ? translate("autocomplete.noresults") : noresults);
		this.myContent = createVelocityContainer("autocomplete");

		// Configure displaying parameters
		if (label != null) {
			myContent.contextPut("autocompleter_label", label);
		}
		myContent.contextPut("showDisplayKey", Boolean.valueOf(showDisplayKey));
		myContent.contextPut("inputWidth", Integer.valueOf(inputWidth));
		myContent.contextPut("minChars", Integer.valueOf(minChars));
		// Create name for addressing the javascript components
		datastoreName = "o_s" + JSNAME_DATASTORE + myContent.getDispatchID();
		comboboxName = "o_s" + JSNAME_COMBOBOX + myContent.getDispatchID();

		// Create a mapper for the server responses for a given input
		mapper = new Mapper() {
			@Override
			@SuppressWarnings({ "synthetic-access" })
			public MediaResource handle(String relPath, HttpServletRequest request) {
				// Prepare resulting media resource
				StringBuffer response = new StringBuffer();
				StringMediaResource smr = new StringMediaResource();
				smr.setEncoding(RESPONSE_ENCODING);
				// Prepare result for ExtJS ScriptTagProxy call-back
				boolean scriptTag = false;
				String cb = request.getParameter(PARAM_CALLBACK);
				if (cb != null) {
					scriptTag = true;
					smr.setContentType(CONTENT_TYPE_TEXT_JAVASCRIPT);
				} else {
					smr.setContentType(CONTENT_TYPE_APPLICATION_X_JSON);
				}
				if (scriptTag) {
					response.append(cb + "(");
				}
				// Read query and generate JSON result
				String lastN = request.getParameter(PARAM_QUERY);
				AutoCompleterListReceiver receiver = new AutoCompleterListReceiver(noResults, showDisplayKey);
				gprovider.getResult(lastN, receiver);
				JSONObject json = new JSONObject();
				try {
					JSONArray result = receiver.getResult();
					json.put("rows", result);
					json.put("results", result.length());
					response.append(json.toString());
				} catch (JSONException e) {
					// Ups, just log error and proceed with empty string
					logError("Could not put rows and results to JSONArray", e);
					response.append("");
				}
				// Close call-back call
				if (scriptTag) {
					response.append(");");
				}
				// Add result to media resource and deliver
				smr.setData(response.toString());
				return smr;
			}
		};
		// Add mapper URL to JS data store in velocity
		String fetchUri = registerMapper(mapper);
		final String fulluri = fetchUri; // + "/" + fileName;
		myContent.contextPut("mapuri", fulluri + "/autocomplete.json");
		//
		putInitialPanel(myContent);
	}

	/**
	 * This dispatches component events...
	 * 
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == myContent) {
			if (event.getCommand().equals(COMMAND_SELECT)) {
				List<String> selectedEntries = new ArrayList<String>(); // init empty result list
				String key = ureq.getParameter(PARAM_KEY);
				if (key == null) {
					// Fallback to submitted input field: the input field does not contain
					// the key but the search value itself
					VelocityRenderDecorator r = (VelocityRenderDecorator) myContent.getContext().get("r");
					String searchValue = ureq.getParameter(r.getId(JSNAME_INPUTFIELD).toString());
					if (searchValue == null) {
						// log error because something went wrong in the code and send empty list as result
						logError("Auto complete JS code must always send 'key' or the autocomplete parameter!", null);
						getWindowControl().setError(translate("autocomplete.error"));
						return;
					} else if (searchValue.equals("") || searchValue.length() < 3) {
						getWindowControl().setWarning(translate("autocomplete.not.enough.chars"));
						return;
					}
					// Create temporary receiver and perform search for given value.
					AutoCompleterListReceiver receiver = new AutoCompleterListReceiver("-", false);
					gprovider.getResult(searchValue, receiver);
					JSONArray result = receiver.getResult();
					// Use key from first result
					if (result.length() > 0) {
						try {
							JSONObject object = result.getJSONObject(0);
							key = object.getString(PARAM_KEY);
						} catch (JSONException e) {
							logError("Error while getting json object from list receiver", e);
							key = "";
						}
					} else {
						key = "";
					}
				}
				// Proceed with a key, empty or valid key
				key = key.trim();
				if (!key.equals("") && !key.equals(AUTOCOMPLETER_NO_RESULT)) {
					// Normal case, add entry
					selectedEntries.add(key);
				} else if (key.equals(AUTOCOMPLETER_NO_RESULT)) { return; }
				fireEvent(ureq, new EntriesChosenEvent(selectedEntries));
			}
		}
	}

	/**
	 * This dispatches controller events...
	 * 
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		// Nothing to dispatch
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// Cleanup javascript objects on browser side by triggering dispose
		// function
		StringBuffer sb = new StringBuffer();
		// first datastore
		sb.append("if (o_info.objectMap.containsKey('").append(datastoreName).append("')) {var oldStore = o_info.objectMap.removeKey('").append(datastoreName)
				.append("');if (oldStore) {oldStore.destroy();} oldStore = null;}");
		// second combobox
		sb.append("if (o_info.objectMap.containsKey('").append(comboboxName).append("')) { var oldCombo = o_info.objectMap.removeKey('").append(comboboxName)
				.append("'); if (oldCombo) {	oldCombo.destroy(); } oldCombo = null;}");
		//
		JSCommand jsCommand = new JSCommand(sb.toString());
		getWindowControl().getWindowBackOffice().sendCommandTo(jsCommand);

		// Mapper autodisposed by basic controller
	}

}

/**
 * Description:<br>
 * The AutoCompleterListReceiver implementes a list receiver that generates JSON output. The class is only used in the AutoCompleterController
 * <P>
 * Initial Date: 25.11.2010 <br>
 * 
 * @author gnaegi
 */
class AutoCompleterListReceiver extends LogDelegator implements ListReceiver {
	private static final String VALUE = "value";
	private static final String CSS_CLASS = "cssClass";
	private static final String CSS_CLASS_EMPTY = "";
	private static final String CSS_CLASS_WITH_ICON = "b_with_small_icon_left ";
	private static final String DISPLAY_KEY = "displayKey";
	private static final String DISPLAY_KEY_NO_RESULTS = "-";

	private final JSONArray list = new JSONArray();
	private final String noresults;
	private final boolean showDisplayKey;

	/**
	 * Constructor
	 * 
	 * @param noResults Text to use when no results are found
	 * @param showDisplayKey true: add displayKey in result; false: don't add displayKey in results (e.g. to protect privacy)
	 */
	AutoCompleterListReceiver(String noresults, boolean showDisplayKey) {
		this.noresults = noresults;
		this.showDisplayKey = showDisplayKey;
	}

	@Override
	public void addEntry(String key, String displayText) {
		addEntry(key, key, displayText, null);
	}

	/**
	 * @return the result as a JSONArray object
	 */
	public JSONArray getResult() {
		if (list.length() == 0) {
			addEntry(AutoCompleterController.AUTOCOMPLETER_NO_RESULT, DISPLAY_KEY_NO_RESULTS, noresults, CSSHelper.CSS_CLASS_ERROR);
		}
		return list;
	}

	@Override
	public void addEntry(String key, String displayKey, String displayText, String iconCssClass) {
		if (key == null) { throw new AssertException("Can not add entry with displayText::" + displayText + " with a NULL key!"); }
		if (isLogDebugEnabled()) {
			logDebug("Add entry with key::" + key + ", displayKey::" + displayKey + ", displayText::" + displayText + ", iconCssClass::" + iconCssClass);
		}
		try {
			JSONObject object = new JSONObject();
			// add key
			object.put("key", key);
			// add displayable key, use key as fallback
			if (showDisplayKey) {
				if (displayKey == null) {
					object.put(DISPLAY_KEY, key);
				} else {
					object.put(DISPLAY_KEY, displayKey);
				}
			}
			// add value to be displayed
			object.put(VALUE, displayText);
			// add optional css class
			if (iconCssClass == null) {
				object.put(CSS_CLASS, CSS_CLASS_EMPTY);
			} else {
				object.put(CSS_CLASS, CSS_CLASS_WITH_ICON + iconCssClass);
			}
			// JSCON object finished
			list.put(object);

		} catch (JSONException e) {
			// do nothing, only log error to logfile
			logError("Could not add entry with key::" + key + ", displayKey::" + displayKey + ", displayText::" + displayText + ", iconCssClass::" + iconCssClass, e);
		}

	}

}
