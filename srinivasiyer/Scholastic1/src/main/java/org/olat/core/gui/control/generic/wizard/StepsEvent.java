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
 * Copyright (c) 1999-2007 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.core.gui.control.generic.wizard;

import org.olat.core.gui.control.Event;

/**
 * Description:<br>
 * TODO: patrickb Class Description for StepsEvent
 * <P>
 * Initial Date: 11.01.2008 <br>
 * 
 * @author patrickb
 */
public class StepsEvent extends Event {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3050157198786051420L;

	public final static StepsEvent ACTIVATE_NEXT = new StepsEvent("next", PrevNextFinishConfig.NOOP);
	public final static StepsEvent ACTIVATE_PREVIOUS = new StepsEvent("prev", PrevNextFinishConfig.NOOP);
	public final static StepsEvent INFORM_FINISHED = new StepsEvent("finished", PrevNextFinishConfig.NOOP);

	private PrevNextFinishConfig pnfConf;

	public StepsEvent(String command, PrevNextFinishConfig pnfConf) {
		super(command);
		this.pnfConf = pnfConf;
	}

	public boolean isBackIsEnabled() {
		return pnfConf.isBackIsEnabled();
	}

	public boolean isNextIsEnabled() {
		return pnfConf.isNextIsEnabled();
	}

	public boolean isFinishIsEnabled() {
		return pnfConf.isFinishIsEnabled();
	}

}
