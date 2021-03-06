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

package org.olat.course.nodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.olat.course.editor.StatusDescription;

/**
 * Description:<br>
 * TODO: patrick Class Description for StatusDescriptionHelper
 * <P>
 * Initial Date: Aug 11, 2005 <br>
 * 
 * @author patrick
 */
public class StatusDescriptionHelper {

	public static StatusDescription[] sort(final StatusDescription[] status) {
		final List<StatusDescription> tmp = Arrays.asList(status);
		return sort(tmp);
	}

	public static StatusDescription[] sort(final List<StatusDescription> missingNames) {
		Collections.sort(missingNames, new ErrorsGtWarningGtInfo());
		/*
		 * remove NOERRORS size bigger 1; NOERROR -> Level == OFF > SEVERE > WARNING > INFO
		 */
		if (missingNames.size() > 1) {
			for (int i = 0; i < missingNames.size(); i++) {
				if (missingNames.get(i) == StatusDescription.NOERROR) {
					// aha found one to remove. Remove shifts all elements to the left, e.g. subtracts one of the indices
					missingNames.remove(i);
					// thus we have to loop on place to remove all NOERRORS which shift to the ith place.
					while (missingNames.get(i) == StatusDescription.NOERROR) {
						missingNames.remove(i);
					}
				}
			}
		}
		StatusDescription[] retVal = new StatusDescription[missingNames.size()];
		retVal = missingNames.toArray(retVal);
		return retVal;
	}

	static class ErrorsGtWarningGtInfo implements Comparator<StatusDescription> {
		@Override
		public int compare(final StatusDescription o1, final StatusDescription o2) {
			final StatusDescription s1 = o1;
			final StatusDescription s2 = o2;
			return s2.getLevel().intValue() - s1.getLevel().intValue();
		}
	}

}
