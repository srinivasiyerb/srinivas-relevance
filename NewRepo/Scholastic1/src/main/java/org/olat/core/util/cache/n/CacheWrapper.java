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
package org.olat.core.util.cache.n;

import java.io.Serializable;

import org.olat.core.id.OLATResourceable;

/**
 * Description:<br>
 * Facade to the underlying cache.
 * <P>
 * Initial Date: 03.10.2007 <br>
 * 
 * @author Felix Jost, http://www.goodsolutions.ch
 */
public interface CacheWrapper {

	/**
	 * @param key the cache for the cache entry
	 * @return the cache entry or null when the element has expired, never been put into yet, or removed due to max-size, or a put in a different cluster node which led
	 *         to an invalidate message
	 */
	public Serializable get(String key);

	/**
	 * o_clusterREVIEW :pb review references puts a value in the cache. this method is thread-safe<br>
	 * Use this method if you generate new data (or change existing data) that cannot be known to other nodes yet.
	 * 
	 * @see public void putSilent(String key, Serializable value);
	 * @param key
	 * @param value
	 */
	public void update(String key, Serializable value);

	/**
	 * use this put whenever you just fill up a cache from data which is already on the db or the filesystem. e.g. use it when you simply load some properties again into
	 * cache. e.g.
	 * 
	 * <pre>
	 * 		CacheWrapper cw = aCache.getOrCreateChildCacheWrapper(ores);
	 * 		synchronized(cw) {
	 * 			   String data = (String) cw.get(FULLUSERSET);
	 * 			if (data == null) {
	 * 				// cache entry has expired or has never been stored yet into the cache.
	 * 				// or has been invalidated in cluster mode
	 * 				data = loadDataFromDiskWeDidNotChangeAnythingButSimplyNeedTheDataAgain(...);
	 * 				cw.putSilent(FULLUSERSET, data);
	 * 			}
	 * 			return data;
	 * 		}
	 * </pre>
	 * 
	 * @param key
	 * @param value
	 */
	public void put(String key, Serializable value);

	/**
	 * puts several values at once into the cache. same as repeatably calling put(key, value), but more efficient. please use in favor of put if applicable.
	 * 
	 * @param keys the array of keys
	 * @param values the array of values
	 */
	public void updateMulti(String[] keys, Serializable[] values);

	/**
	 * removes a value from the cache. this method is thread-safe
	 * 
	 * @param key
	 */
	public void remove(String key);

	/**
	 * this method is thread safe. creates a child cachewrapper that represents the cachewrapper for the given olatresourceable within this parent cachewrapper(the 'this'
	 * object)
	 * 
	 * @param ores the olat resourceable
	 * @return the cachewrapper
	 */
	public CacheWrapper getOrCreateChildCacheWrapper(OLATResourceable ores);

}
