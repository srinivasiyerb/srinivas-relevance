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

package org.olat.ims.qti.editor.beecom.objects;

import org.dom4j.Element;
import org.olat.ims.qti.process.QTIHelper;

/**
 * Initial Date: 01.09.2003
 * 
 * @author Mike Stock
 */
public class Duration implements QTIObject {

	private int iMin;
	private int iSec;

	public Duration(final long millis) {
		setDuration(millis);
	}

	public Duration(final String sMin, final String sSec) {
		setDuration(sMin, sSec);
	}

	private long getMillis() {
		return ((iMin * 60) + iSec) * 1000;
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.ims.qti.editor.beecom.objects.QTIObject#addToElement(org.dom4j.Element)
	 */
	@Override
	public void addToElement(final Element root) {
		final Element duration = root.addElement("duration");
		duration.setText(getISODuration());
	}

	public void setDuration(final String min, final String sec) {
		try {
			iMin = Integer.parseInt(min);
			iSec = Integer.parseInt(sec);
		} catch (final NumberFormatException nfe) {
			return;
		}
	}

	public void setDuration(final long millis) {
		final long iTmpSec = millis / 1000;
		iMin = (int) (iTmpSec / 60);
		iSec = (int) (iTmpSec - (iMin * 60));
	}

	public String getISODuration() {
		return QTIHelper.getISODuration(getMillis());
	}

	public int getMin() {
		return iMin;
	}

	public int getSec() {
		return iSec;
	}

	public boolean isSet() {
		return getMillis() > 0;
	}
}
