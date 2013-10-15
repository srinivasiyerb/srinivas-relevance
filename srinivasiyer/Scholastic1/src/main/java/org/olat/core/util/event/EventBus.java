package org.olat.core.util.event;

import java.util.Map;
import java.util.Set;

import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;

/**
 * <!--**************--> <h3>Multiuser events</h3> This is the center distributing the multiuser events into the system.<br>
 * Classes implementing the {@link org.olat.core.util.event.GenericEventListener GenericEventListener} can register for an event bound to a certain
 * {@link org.olat.core.id.OLATResourceable}. A class having "news" concerning an OLATResourceable may fire {@link org.olat.core.util.event.MultiUserEvent Events} which
 * are sent to all listeners.<br>
 * <b>NOTE:</b> the listeners are put in a WeakHashMap, so they need to have another reference than just the event center.
 * 
 * @author Felix Jost
 */
public interface EventBus {

	/**
	 * registers a GenericEventListener to listen to events concerning the OLATResourceable ores
	 * 
	 * @param gel the GenericEventListener / the class implementing it
	 * @param identity the identity to whicinfoh the listening (controller) belongs, or null if that is not known or the olat-system itself.
	 * @param ores the OLATResourceable
	 */
	public abstract void registerFor(GenericEventListener gel, Identity identity, OLATResourceable ores);

	/**
	 * deregisters/removes a GenericEventListener to listen to events concerning the OLATResourceable ores
	 * 
	 * @param gel
	 * @param ores
	 */
	public abstract void deregisterFor(GenericEventListener gel, OLATResourceable ores);

	/**
	 * fires an event to all listeners interested in events concerning this OLATResourceable ores. The events may be fired and received synchronously or asynchronously,
	 * depending on the concrete implementation.
	 * 
	 * @param event the OLATResourceableEvent (must be serializable!, for multiple server olat installations)
	 * @param ores the OLATResourceable
	 */
	public abstract void fireEventToListenersOf(MultiUserEvent event, OLATResourceable ores);

	/**
	 * returns a Set of Identities which had at the very moment controllers which were listening to the OLATResourceable ores. Useful to e.g. warn a power-user which
	 * wants to delete a resource (e.g. a course) that there are users using this resource right at this time. NOTE: please use only for OLAT admin tools in order to
	 * protect the privacy of users. If you would like to say "the resource you want to delete is currently used by 10 users", but not say which users, then please use
	 * method getListeningIdentityCntFor(OLATResourceable ores).. <br>
	 * Note for cluster: this method returns only the names of listeners within one node/vm. However, the cnt of listeners is broadcasted amongst cluster nodes - see
	 * getListeningIdentityCntFor(OLATResourceable ores);
	 * 
	 * @param ores the OLATResourceable
	 * @return a Set of Identities which had at the very moment controllers which were listening to the OLATResourceable ores
	 */
	public abstract Set getListeningIdentityNamesFor(OLATResourceable ores);

	/**
	 * Note for cluster: this method is cluster-safe. in a cluster, it takes the latest counts received from all cluster nodes and sums them up.
	 * 
	 * @param ores the resourceable
	 * @return the number of people currently using this resource
	 */
	public abstract int getListeningIdentityCntFor(OLATResourceable ores);

	/**
	 * For admin purposes only! gets the whole infocenter and typeInfocenter map (keys: String-form of OLATResoureable; values: EventAgencies) as a map.
	 * 
	 * @return the whole infocenter map
	 */
	public abstract Map getUnmodifiableInfoCenter();

}