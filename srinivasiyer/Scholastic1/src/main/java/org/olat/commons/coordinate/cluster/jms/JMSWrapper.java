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
package org.olat.commons.coordinate.cluster.jms;

import java.io.Serializable;

import org.olat.core.id.OLATResourceable;
import org.olat.core.util.event.MultiUserEvent;

/**
 * Description:<br>
 * wraps a MultiUserEvent together with its olatresourceable channel descriptor and some extra informations such as ids and timestamps. A JMSWrapper object is finally
 * sent over a fixed-named (olat/sysbus) publish/subscribe channel (topic) of JMS.
 * <P>
 * Initial Date: 10.12.2007 <br>
 * 
 * @author Felix Jost, http://www.goodsolutions.ch
 */
public class JMSWrapper implements Serializable {

	private final Long resId;
	private final String resType;
	private final MultiUserEvent event;
	private final long msgId;
	private final Integer nodeId;

	JMSWrapper(final Integer nodeId, final long msgId, final OLATResourceable ores, final MultiUserEvent event) {
		// in order to make this class serializable, we extract the base types
		// of the OLATResourceable.
		// the MultiUserEvent is by definition serializable.
		this.event = event;
		this.msgId = msgId;
		this.nodeId = nodeId;
		resId = ores.getResourceableId();
		resType = ores.getResourceableTypeName();
	}

	public OLATResourceable getOres() {
		return new OLATResourceable() {

			@Override
			public Long getResourceableId() {
				return resId;
			}

			@Override
			public String getResourceableTypeName() {
				return resType;
			}
		};
	}

	public MultiUserEvent getMultiUserEvent() {
		return event;
	}

	public long getMsgId() {
		return msgId;
	}

	public Integer getNodeId() {
		return nodeId;
	}

}
