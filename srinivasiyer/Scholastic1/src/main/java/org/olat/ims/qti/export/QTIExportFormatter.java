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

package org.olat.ims.qti.export;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.ims.qti.export.helper.IdentityAnonymizerCallback;

/**
 * Initial Date: May 23, 2006 <br>
 * 
 * @author Alexander Schneider
 */
public abstract class QTIExportFormatter {
	protected static final String PACKAGE = Util.getPackageName(QTIExportFormatter.class);
	protected StringBuilder sb;
	protected Translator translator;
	protected IdentityAnonymizerCallback anonymizerCallback;
	protected List qtiItemObjectList;
	protected Map mapWithExportItemConfigs;

	/**
	 *
	 */
	public QTIExportFormatter(final Locale locale, final IdentityAnonymizerCallback anonymizerCallback) {
		this.sb = new StringBuilder();
		this.translator = new PackageTranslator(PACKAGE, locale);
		this.anonymizerCallback = anonymizerCallback;
	}

	public abstract void openReport();

	public abstract void openResultSet(QTIExportSet set);

	public abstract void visit(QTIExportItem item);

	public abstract void closeResultSet();

	public abstract void closeReport();

	public abstract String getReport();

	public abstract String getFileNamePrefix();

	public abstract int getType();

	/**
	 * @param qtiItemObjectList
	 */
	public void setQTIItemObjectList(final List qtiItemObjectList) {
		this.qtiItemObjectList = qtiItemObjectList;
	}

	/**
	 * @param mapWithConfigs
	 */
	public void setMapWithExportItemConfigs(final Map mapWithConfigs) {
		this.mapWithExportItemConfigs = mapWithConfigs;
	}

	/**
	 * @return
	 */
	public Map getMapWithExportItemConfigs() {
		return this.mapWithExportItemConfigs;
	}
}
