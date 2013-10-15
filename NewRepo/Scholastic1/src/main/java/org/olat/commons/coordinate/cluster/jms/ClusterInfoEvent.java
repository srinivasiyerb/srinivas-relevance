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

import org.olat.core.util.cache.n.impl.cluster.ClusterConfig;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.core.util.event.businfo.BusListenerInfo;

/**
 * Description: This is an event which is regularly (every other second or so) broadcasted between all olat cluster nodes. it contains the configuration of the cluster
 * that sends it, and also the count statistics for each olatresourceable channel.
 * 
 * @author felix jost
 */
public class ClusterInfoEvent extends MultiUserEvent {

	private final long created;
	private final ClusterConfig config;
	private final BusListenerInfo busListenerInfo;

	ClusterInfoEvent(final ClusterConfig config, final BusListenerInfo busListenerInfo) {
		super("clusterinfo");
		this.config = config;
		this.busListenerInfo = busListenerInfo;
		created = System.currentTimeMillis();
	}

	/**
	 * @return the timestamp (System.currentTimeMillis) when this message had been created
	 */
	long getCreated() {
		return created;
	}

	/**
	 * @return the configuration of node that sent the event
	 */
	ClusterConfig getConfig() {
		return config;
	}

	/**
	 * @return the buslistener information (olatresourceable channel counts)
	 */
	public BusListenerInfo getBusListenerInfo() {
		return busListenerInfo;
	}

}
