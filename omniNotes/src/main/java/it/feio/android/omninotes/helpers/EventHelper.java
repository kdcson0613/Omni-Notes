package it.feio.android.omninotes.helpers;

import static it.feio.android.omninotes.async.bus.SwitchFragmentEvent.Direction.CHILDREN;

import android.os.Handler;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import de.greenrobot.event.EventBus;
import it.feio.android.omninotes.NavigationDrawerFragment;
import it.feio.android.omninotes.async.bus.CategoriesUpdatedEvent;
import it.feio.android.omninotes.async.bus.NavigationUpdatedEvent;
import it.feio.android.omninotes.async.bus.NavigationUpdatedNavDrawerClosedEvent;
import it.feio.android.omninotes.async.bus.NotesLoadedEvent;
import it.feio.android.omninotes.async.bus.SwitchFragmentEvent;
import it.feio.android.omninotes.models.Category;
import it.feio.android.omninotes.models.NavigationItem;

public class EventHelper extends NavigationDrawerFragment {

    public static final int BURGER = 0;
    public static final int ARROW = 1;

    public static boolean isDoublePanelActive() {
  //		Resources resources = OmniNotes.getAppContext().getResources();
  //		return resources.getDimension(R.dimen.navigation_drawer_width) == resources.getDimension(R.dimen
  //				.navigation_drawer_reserved_space);
      return false;
    }
    public void onEvent(CategoriesUpdatedEvent event) {
        refreshMenus();
    }

    public void onEvent(NotesLoadedEvent event) {
        if (mDrawerLayout != null) {
            if (!isDoublePanelActive()) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        }
        if (getMainActivity().getSupportFragmentManager().getBackStackEntryCount() == 0) {
            init();
        }
        refreshMenus();
        alreadyInitialized = true;
    }

    public void onEvent(SwitchFragmentEvent event) {
        if (CHILDREN.equals(event.getDirection())) {
            animateBurger(ARROW);
        } else {
            animateBurger(BURGER);
        }
    }

    public void onEvent(NavigationUpdatedEvent navigationUpdatedEvent) {
        if (navigationUpdatedEvent.navigationItem.getClass().isAssignableFrom(NavigationItem.class)) {
            mActivity.getSupportActionBar()
                    .setTitle(((NavigationItem) navigationUpdatedEvent.navigationItem).getText());
        } else {
            mActivity.getSupportActionBar()
                    .setTitle(((Category) navigationUpdatedEvent.navigationItem).getName());
        }
        if (mDrawerLayout != null) {
            if (!isDoublePanelActive()) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            }
            new Handler()
                    .postDelayed(() -> EventBus.getDefault().post(new NavigationUpdatedNavDrawerClosedEvent
                            (navigationUpdatedEvent.navigationItem)), 400);
        }
    }

}
