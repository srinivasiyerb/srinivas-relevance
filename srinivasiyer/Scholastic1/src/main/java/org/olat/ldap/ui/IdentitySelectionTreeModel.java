package org.olat.ldap.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.id.Identity;
import org.olat.core.util.nodes.INode;
import org.olat.core.util.tree.INodeFilter;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * <h3>Description:</h3> This tree model displays a list of identities
 * <p>
 * Initial Date: 11.11.2008 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public class IdentitySelectionTreeModel extends GenericTreeModel implements INodeFilter {
	/**
	 * Constructor
	 * 
	 * @param identities The list of identities
	 * @param usageIdentifyer The usageIdentifyer to tell the model which user properties should be used
	 * @param locale The locale used to format the user properties
	 */
	public IdentitySelectionTreeModel(final List<Identity> identities, final String usageIdentifyer, final Locale locale) {
		// Add the root node
		final GenericTreeNode gtn = new GenericTreeNode();
		gtn.setAccessible(false);
		gtn.setTitle("");
		gtn.setIdent("_ROOT_");
		setRootNode(gtn);
		// Add each identity
		final List<UserPropertyHandler> properHandlerList = UserManager.getInstance().getUserPropertyHandlersFor(usageIdentifyer, false);
		for (final Identity identity : identities) {
			// collect user name information
			final StringBuffer sb = new StringBuffer();
			sb.append(identity.getName()).append(": ");
			boolean first = true;
			// collect user properties information
			for (final UserPropertyHandler userProperty : properHandlerList) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(userProperty.getUserProperty(identity.getUser(), locale));
			}
			// Create child node
			final GenericTreeNode identityNode = new GenericTreeNode();
			identityNode.setAccessible(true);
			identityNode.setTitle(sb.toString());
			identityNode.setUserObject(identity);
			identityNode.setIdent(identity.getName());
			// add child to tree - the tree is flat, only one hierarchy
			gtn.addChild(identityNode);
		}
	}

	/**
	 * @see org.olat.core.util.tree.INodeFilter#accept(org.olat.core.util.nodes.INode)
	 */
	@Override
	public boolean accept(final INode node) {
		return true;
	}

	/**
	 * Get all identities from the set of tree nodes identifyers
	 * 
	 * @param selected
	 * @return
	 */
	public List<Identity> getIdentities(final Set<String> selected) {
		final List<Identity> identities = new ArrayList<Identity>();
		for (final String ident : selected) {
			final Identity identity = (Identity) getNodeById(ident).getUserObject();
			identities.add(identity);
		}
		return identities;
	}
}
