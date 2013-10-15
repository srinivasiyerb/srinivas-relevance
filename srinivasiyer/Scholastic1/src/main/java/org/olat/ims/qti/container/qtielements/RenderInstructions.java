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

package org.olat.ims.qti.container.qtielements;

import java.util.HashMap;

/**
 * Initial Date: 26.11.2004
 * 
 * @author Mike Stock
 */
public class RenderInstructions extends HashMap {

	/**
	 * Render as form for input.
	 */
	public static final int RENDER_MODE_FORM = 0;
	/**
	 * Render static
	 */
	public static final int RENDER_MODE_STATIC = 1;

	/**
	 * Render as flow
	 */
	public static final int RENDER_FLOW_BLOCK = 0;
	/**
	 * Render as list
	 */
	public static final int RENDER_FLOW_LIST = 1;

	/**
	 * Denotes statics path for resources.
	 */
	public static final String KEY_STATICS_PATH = "sp";
	/**
	 * Denotes Locale.
	 */
	public static final String KEY_LOCALE = "loc";
	/**
	 * Denotes the URI to submit flash item responses to.
	 */
	public static final String KEY_APPLET_SUBMIT_URI = "sfuri";
	/**
	 * Denotes this item's input. May be null if no answer given at this time.
	 */
	public static final String KEY_ITEM_INPUT = "iinput";
	/**
	 * How to render... see render mode constants
	 */
	public static final String KEY_RENDER_MODE = "mode";
	/**
	 * Render title TRUE/FALSE
	 */
	public static final String KEY_RENDER_TITLE = "rtitle";

	public static final String KEY_RENDER_AUTOENUM_LIST = "rautoenumlist";
	public static final String KEY_RENDER_AUTOENUM_IDX = "rautoenumidx";

	// Denotes response cardinality according to Response_lid.rcardinality
	protected static final String KEY_RESPONSE_RCARDINALITY = "rcc";
	// Denotes render class -> i.e. render_choice, render_fib, etc.
	protected static final String KEY_RENDER_CLASS = "rclass";
	// Denotes flow render instruction
	protected static final String KEY_FLOW = "flow";
	// Denotes flow label render instruction
	protected static final String KEY_FLOW_LABEL = "flowlabel";
	// Denotes flow mat render instruction
	protected static final String KEY_FLOW_MAT = "flowmat";
	// Denotes this item's ident
	protected static final String KEY_ITEM_IDENT = "iident";
	// Denotes this item's ident
	protected static final String KEY_RESPONSE_IDENT = "rident";
	// Denotes row attribute of a render_fib element
	protected static final String KEY_FIB_ROWS = "rows";
	// Denotes columns attribute of a render_fib element
	protected static final String KEY_FIB_COLUMNS = "cols";
	// Denotes maxlength attribute of a render_fib element
	protected static final String KEY_FIB_MAXLENGTH = "max";

	// just a simple HashMap
}
