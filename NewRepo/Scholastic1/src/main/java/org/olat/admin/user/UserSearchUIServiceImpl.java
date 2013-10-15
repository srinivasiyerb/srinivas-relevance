package org.olat.admin.user;

import org.olat.basesecurity.events.SingleIdentityChosenEvent;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;

public class UserSearchUIServiceImpl implements UserSearchUIService {
	UserSearchUIServiceImpl() {
		// needed for spring
	}

	@Override
	public Controller createUserSearch(final boolean withCancelButton, final UserRequest ureq, final WindowControl wControl) {
		return new UserSearchController(ureq, wControl, withCancelButton);
	}

	@Override
	public Identity getChosenUser(final Event event) {
		if (event == Event.CANCELLED_EVENT) {
			return null;
		} else {
			final SingleIdentityChosenEvent sice = (SingleIdentityChosenEvent) event;
			return sice.getChosenIdentity();
		}
	}

}
