/*
 * Copyright (C) 2013-2020 Federico Iosue (federico@iosue.it)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.feio.android.omninotes;

import static it.feio.android.omninotes.async.bus.SwitchFragmentEvent.Direction.CHILDREN;
import android.animation.ValueAnimator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import de.greenrobot.event.EventBus;
import it.feio.android.omninotes.async.CategoryMenuTask;
import it.feio.android.omninotes.async.MainMenuTask;
import it.feio.android.omninotes.async.bus.CategoriesUpdatedEvent;
import it.feio.android.omninotes.async.bus.DynamicNavigationReadyEvent;
import it.feio.android.omninotes.async.bus.NavigationUpdatedEvent;
import it.feio.android.omninotes.async.bus.NavigationUpdatedNavDrawerClosedEvent;
import it.feio.android.omninotes.async.bus.NotesLoadedEvent;
import it.feio.android.omninotes.async.bus.NotesUpdatedEvent;
import it.feio.android.omninotes.async.bus.SwitchFragmentEvent;
import it.feio.android.omninotes.helpers.EventHelper;
import it.feio.android.omninotes.helpers.LogDelegate;
import it.feio.android.omninotes.models.Category;
import it.feio.android.omninotes.models.NavigationItem;
import it.feio.android.omninotes.utils.Display;


public class NavigationDrawerFragment extends Fragment {

  ActionBarDrawerToggle mDrawerToggle;
  protected DrawerLayout mDrawerLayout;
  protected MainActivity mActivity;
  protected boolean alreadyInitialized;
  private  EventHelper eventHelper;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
  }


  @Override
  public void onStart() {
    super.onStart();
    EventBus.getDefault().register(this);
  }


  @Override
  public void onStop() {
    super.onStop();
    EventBus.getDefault().unregister(this);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
  }


  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mActivity = (MainActivity) getActivity();
    init();
  }


  protected MainActivity getMainActivity() {
    return (MainActivity) getActivity();
  }


  public void onEventMainThread(DynamicNavigationReadyEvent event) {
    if (alreadyInitialized) {
      alreadyInitialized = false;
    } else {
      refreshMenus();
    }
  }

  public void onEventHandler(CategoriesUpdatedEvent CategoriesEvent,
                             NotesLoadedEvent NotesEvent,
                             SwitchFragmentEvent SwitchEvent,
                             NavigationUpdatedEvent NavigationEvent) {
    if (CategoriesEvent != null) {
      try {
        eventHelper.onEvent(CategoriesEvent);
      } catch ( NullPointerException e) {
        throw new NullPointerException("Null object");
      }
    }

    if (NotesEvent != null) {
      try {
        eventHelper.onEvent(NotesEvent);
      } catch ( NullPointerException e) {
        throw new NullPointerException("Null object");
      }
    }

    if (SwitchEvent != null) {
      try {
        eventHelper.onEvent(SwitchEvent);
      } catch (NullPointerException e) {
        throw new NullPointerException("Null object");
      }
    }

    if (NavigationEvent != null) {
      try {
        eventHelper.onEvent(NavigationEvent);
      } catch (NullPointerException e) {
        throw new NullPointerException("Null object");
      }
    }

  }


  public void onEventAsync(NotesUpdatedEvent event) {
    alreadyInitialized = false;
  }

  public void init() {
    LogDelegate.vervoseLog("Started navigation drawer initialization");

    mDrawerLayout = mActivity.findViewById(R.id.drawer_layout);
    mDrawerLayout.setFocusableInTouchMode(false);

    View leftDrawer = getView().findViewById(R.id.left_drawer);
    int leftDrawerBottomPadding = Display.getNavigationBarHeightKitkat(getActivity());
    leftDrawer.setPadding(leftDrawer.getPaddingLeft(), leftDrawer.getPaddingTop(),
        leftDrawer.getPaddingRight(),
        leftDrawerBottomPadding);

    // ActionBarDrawerToggle± ties together the the proper interactions
    // between the sliding drawer and the action bar app icon
    mDrawerToggle = new ActionBarDrawerToggle(mActivity,
        mDrawerLayout,
        getMainActivity().getToolbar(),
        R.string.drawer_open,
        R.string.drawer_close
    ) {
      public void onDrawerClosed(View view) {
        mActivity.supportInvalidateOptionsMenu();
      }


      public void onDrawerOpened(View drawerView) {
        mActivity.commitPending();
        mActivity.finishActionMode();
      }
    };

    // Styling options
    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    mDrawerLayout.addDrawerListener(mDrawerToggle);
    mDrawerToggle.setDrawerIndicatorEnabled(true);

    LogDelegate.vervoseLog("Finished navigation drawer initialization");
  }


  protected void refreshMenus() {
    buildMainMenu();
    LogDelegate.vervoseLog("Finished main menu initialization");
    buildCategoriesMenu();
    LogDelegate.vervoseLog("Finished categories menu initialization");
    mDrawerToggle.syncState();
  }


  private void buildCategoriesMenu() {
    CategoryMenuTask task = new CategoryMenuTask(this);
    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }


  private void buildMainMenu() {
    MainMenuTask task = new MainMenuTask(this);
    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }


  protected void animateBurger(int targetShape) {
    if (mDrawerToggle != null) {
      if (targetShape != EventHelper.BURGER && targetShape != EventHelper.ARROW) {
        return;
      }
      ValueAnimator anim = ValueAnimator.ofFloat((targetShape + 1) % 2, targetShape);
      anim.addUpdateListener(valueAnimator -> {
        float slideOffset = (Float) valueAnimator.getAnimatedValue();
        mDrawerToggle.onDrawerSlide(mDrawerLayout, slideOffset);
      });
      anim.setInterpolator(new DecelerateInterpolator());
      anim.setDuration(500);
      anim.start();
    }
  }


}
