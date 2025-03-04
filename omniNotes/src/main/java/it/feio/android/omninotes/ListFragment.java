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

import static android.text.Html.fromHtml;
import static android.text.TextUtils.isEmpty;
import static androidx.core.view.ViewCompat.animate;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_FAB_TAKE_PHOTO;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_MERGE;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_POSTPONE;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_SEARCH_UNCOMPLETE_CHECKLISTS;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_SHORTCUT_WIDGET;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_WIDGET_SHOW_LIST;
import static it.feio.android.omninotes.utils.ConstantsBase.INTENT_CATEGORY;
import static it.feio.android.omninotes.utils.ConstantsBase.INTENT_NOTE;
import static it.feio.android.omninotes.utils.ConstantsBase.INTENT_WIDGET;
import static it.feio.android.omninotes.utils.ConstantsBase.MENU_SORT_GROUP_ID;
import static it.feio.android.omninotes.utils.ConstantsBase.PREF_ENABLE_SWIPE;
import static it.feio.android.omninotes.utils.ConstantsBase.PREF_EXPANDED_VIEW;
import static it.feio.android.omninotes.utils.ConstantsBase.PREF_FAB_EXPANSION_BEHAVIOR;
import static it.feio.android.omninotes.utils.ConstantsBase.PREF_FILTER_ARCHIVED_IN_CATEGORIES;
import static it.feio.android.omninotes.utils.ConstantsBase.PREF_FILTER_PAST_REMINDERS;
import static it.feio.android.omninotes.utils.ConstantsBase.PREF_NAVIGATION;
import static it.feio.android.omninotes.utils.ConstantsBase.PREF_SORTING_COLUMN;
import static it.feio.android.omninotes.utils.ConstantsBase.PREF_WIDGET_PREFIX;
import static it.feio.android.omninotes.utils.Navigation.checkNavigation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.pixplicity.easyprefs.library.Prefs;
import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import it.feio.android.omninotes.async.bus.CategoriesUpdatedEvent;
import it.feio.android.omninotes.async.bus.NavigationUpdatedNavDrawerClosedEvent;
import it.feio.android.omninotes.async.bus.NotesLoadedEvent;
import it.feio.android.omninotes.async.bus.NotesMergeEvent;
import it.feio.android.omninotes.async.bus.PasswordRemovedEvent;
import it.feio.android.omninotes.async.notes.NoteLoaderTask;
import it.feio.android.omninotes.async.notes.NoteProcessorArchive;
import it.feio.android.omninotes.async.notes.NoteProcessorCategorize;
import it.feio.android.omninotes.async.notes.NoteProcessorDelete;
import it.feio.android.omninotes.async.notes.NoteProcessorTrash;
import it.feio.android.omninotes.databinding.FragmentListBinding;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.helpers.LogDelegate;
import it.feio.android.omninotes.helpers.NotesHelper;
import it.feio.android.omninotes.models.Category;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.models.ONStyle;
import it.feio.android.omninotes.models.PasswordValidator;
import it.feio.android.omninotes.models.Tag;
import it.feio.android.omninotes.models.UndoBarController;
import it.feio.android.omninotes.models.adapters.CategoryRecyclerViewAdapter;
import it.feio.android.omninotes.models.adapters.NoteAdapter;
import it.feio.android.omninotes.models.listeners.OnViewTouchedListener;
import it.feio.android.omninotes.models.listeners.RecyclerViewItemClickSupport;
import it.feio.android.omninotes.models.views.Fab;
import it.feio.android.omninotes.utils.AnimationsHelper;
import it.feio.android.omninotes.utils.IntentChecker;
import it.feio.android.omninotes.utils.KeyboardUtils;
import it.feio.android.omninotes.utils.Navigation;
import it.feio.android.omninotes.utils.PasswordHelper;
import it.feio.android.omninotes.utils.ReminderHelper;
import it.feio.android.omninotes.utils.TagsHelper;
import it.feio.android.omninotes.utils.TextHelper;
import it.feio.android.pixlui.links.UrlCompleter;
import it.feio.android.simplegallery.util.BitmapUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;


public class ListFragment extends BaseFragment implements OnViewTouchedListener,
    UndoBarController.UndoListener {

  private static final int REQUEST_CODE_CATEGORY = 1;
  private static final int REQUEST_CODE_CATEGORY_NOTES = 2;
  private static final int REQUEST_CODE_ADD_ALARMS = 3;
  public static final String LIST_VIEW_POSITION = "listViewPosition";
  public static final String LIST_VIEW_POSITION_OFFSET = "listViewPositionOffset";

  private FragmentListBinding binding;

  private final List<Note> selectedNotes = new ArrayList<>();
  private SearchView searchView;
  private MenuItem searchMenuItem;
  private Menu menu;
  private AnimationDrawable jinglesAnimation;
  private int listViewPosition;
  private int listViewPositionOffset = 16;
  private boolean sendToArchive;
  private ListFragment mFragment;
  private ActionMode actionMode;
  private boolean keepActionMode = false;

  // Undo archive/trash
  private boolean undoTrash = false;
  private boolean undoArchive = false;
  private boolean undoCategorize = false;
  private Category undoCategorizeCategory = null;
  private final SortedMap<Integer, Note> undoNotesMap = new TreeMap<>();
  // Used to remember removed categories from notes
  private final Map<Note, Category> undoCategoryMap = new HashMap<>();
  // Used to remember archived state from notes
  private final Map<Note, Boolean> undoArchivedMap = new HashMap<>();

  // Search variables
  private String searchQuery;
  private String searchQueryInstant;
  private String searchTags;
  private boolean searchUncompleteChecklists;
  private boolean goBackOnToggleSearchLabel = false;
  private boolean searchLabelActive = false;

  private NoteAdapter listAdapter;
  private UndoBarController ubc;
  private Fab fab;
  private MainActivity mainActivity;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setFragment();
    setHasOptionsMenu(true);
    setRetainInstance(true);
    EventBus.getDefault().register(this, 1);
  }


  private void setFragment() {
    mFragment = this;
  }


  @Override
  public void onDestroy() {
    super.onDestroy();
    EventBus.getDefault().unregister(this);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      boolean isInstanceContainKey = savedInstanceState.containsKey(LIST_VIEW_POSITION);
      if (isInstanceContainKey) {
        loadSavedInstanceState(savedInstanceState);
      }
      keepActionMode = false;
    }
    binding = FragmentListBinding.inflate(inflater, container, false);
    View view = binding.getRoot();
    setBindingListContext();

    return view;
  }

  private void setBindingListContext() {
    binding.list.setHasFixedSize(true);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
    binding.list.setLayoutManager(linearLayoutManager);

    DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
        binding.list.getContext(),
        linearLayoutManager.getOrientation());
    dividerItemDecoration
        .setDrawable(getResources().getDrawable(R.drawable.fragment_list_item_divider));
    binding.list.addItemDecoration(dividerItemDecoration);

    RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
    itemAnimator.setAddDuration(1000);
    itemAnimator.setRemoveDuration(1000);
    binding.list.setItemAnimator(itemAnimator);

    // Replace listview with Mr. Jingles if it is empty
    binding.list.setEmptyView(binding.emptyList);
  }


  private void loadSavedInstanceState(Bundle savedInstanceState) {
    listViewPosition = savedInstanceState.getInt(LIST_VIEW_POSITION);
    listViewPositionOffset = savedInstanceState.getInt(LIST_VIEW_POSITION_OFFSET);
    searchQuery = savedInstanceState.getString("searchQuery");
    searchTags = savedInstanceState.getString("searchTags");
  }


  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mainActivity = (MainActivity) getActivity();
    if (mainActivity!= null && savedInstanceState != null) {
      mainActivity.navigationTmp = savedInstanceState.getString("navigationTmp");
    }
    init();
  }


  private void init() {
    initEasterEgg();
    initListView();
    ubc = new UndoBarController(binding.undobar.getRoot(), this);

    initNotesList(mainActivity.getIntent());
    initFab();
    initTitle();
  }


  private void initFab() {
    fab = new Fab(binding.fab.getRoot(), binding.list,
        Prefs.getBoolean(PREF_FAB_EXPANSION_BEHAVIOR, false));
    fab.setOnFabItemClickedListener(id -> {
      View viewById = mainActivity.findViewById(id);
      switch (id) {
        case R.id.fab_camera:
          Intent intent = mainActivity.getIntent();
          intent.setAction(ACTION_FAB_TAKE_PHOTO);
          mainActivity.setIntent(intent);
          editNote(new Note(), viewById);
          break;
        case R.id.fab_checklist:
          Note note = new Note();
          note.setChecklist(true);
          editNote(note, viewById);
          break;
        default:
          editNote(new Note(), viewById);
      }
    });
  }


  boolean closeFab() {
    if (fab != null && fab.isExpanded()) {
      fab.performToggle();
      return true;
    }
    return false;
  }


  /**
   * Activity title initialization based on navigation
   */
  private void initTitle() {
    String[] navigationList = getResources().getStringArray(R.array.navigation_list);
    String[] navigationListCodes = getResources().getStringArray(R.array.navigation_list_codes);
    String navigation = mainActivity.navigationTmp != null
        ? mainActivity.navigationTmp
        : Prefs.getString (PREF_NAVIGATION, navigationListCodes[0]);
    int index = Arrays.asList(navigationListCodes).indexOf(navigation);
    String title;
    // If is a traditional navigation item
    if (index >= 0 && index < navigationListCodes.length) {
      title = navigationList[index];
    } else {
      Category category = DbHelper.getInstance().getCategory(Long.parseLong(navigation));
      title = category != null ? category.getName() : "";
    }
    title = title == null ? getString(R.string.title_activity_list) : title;
    mainActivity.setActionBarTitle(title);
  }


  /**
   * Starts a little animation on Mr.Jingles!
   */
  private void initEasterEgg() {
    binding.emptyList.setOnClickListener(v -> {
      if (jinglesAnimation == null) {
        jinglesAnimation = (AnimationDrawable) binding.emptyList.getCompoundDrawables()[1];
        binding.emptyList.post(() -> {
          if (jinglesAnimation != null) {
            jinglesAnimation.start();
          }
        });
      } else {
        stopJingles();
      }
    });
  }


  private void stopJingles() {
    if (jinglesAnimation != null) {
      jinglesAnimation.stop();
      jinglesAnimation = null;
      binding.emptyList
          .setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.jingles_animation, 0, 0);
    }
  }


  @Override
  public void onPause() {
    super.onPause();
    searchQueryInstant = searchQuery;
    stopJingles();
    Crouton.cancelAllCroutons();
    closeFab();
    if (!keepActionMode) {
      commitPending();
      if (getActionMode() != null) {
        getActionMode().finish();
      }
    }
  }


  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    refreshListScrollPosition();
    outState.putInt("listViewPosition", listViewPosition);
    outState.putInt(LIST_VIEW_POSITION_OFFSET, listViewPositionOffset);
    outState.putString("searchQuery", searchQuery);
    outState.putString("searchTags", searchTags);
  }


  private void refreshListScrollPosition() {
    if(binding != null) {
      listViewPosition = ((LinearLayoutManager) binding.list.getLayoutManager())
          .findFirstVisibleItemPosition();
      View viewChild = binding.list.getChildAt(0);
      listViewPositionOffset =
          (viewChild == null) ? (int) getResources().getDimension(R.dimen.vertical_margin) : viewChild.getTop();
    }
  }


  @Override
  public void onResume() {
    super.onResume();
    if (mainActivity.getPrefsChanged()) {
      mainActivity.setPrefsChangedFalse();
      init();
    } else if (Intent.ACTION_SEARCH.equals(mainActivity.getIntent().getAction())) {
      initNotesList(mainActivity.getIntent());
    }
  }


  private final class ModeCallback implements ActionMode.Callback {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      // Inflate the menu for the CAB
      MenuInflater inflater = mode.getMenuInflater();
      inflater.inflate(R.menu.menu_list, menu);
      actionMode = mode;
      fab.setAllowed(isFabAllowed());
      fab.hideFab();
      return true;
    }


    @Override
    public void onDestroyActionMode(ActionMode mode) {
      // Here you can make any necessary updates to the activity when
      // the CAB is removed. By default, selected items are
      // deselected/unchecked.
      for (int i = 0; i < listAdapter.getSelectedItems().size(); i++) {
        int key = listAdapter.getSelectedItems().keyAt(i);
      }

      selectedNotes.clear();
      listAdapter.clearSelectedItems();
      listAdapter.notifyDataSetChanged();

      fab.setAllowed(isFabAllowed(true));
      if (undoNotesMap.size() == 0) {
        fab.showFab();
      }

      actionMode = null;
      LogDelegate.debugLog("Closed multiselection contextual menu");
    }


    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      prepareActionModeMenu();
      return true;
    }


    @Override
    public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
      Integer[] protectedActions = {R.id.menu_select_all, R.id.menu_merge};
      if (!Arrays.asList(protectedActions).contains(item.getItemId())) {
        mainActivity.requestPassword(mainActivity, getSelectedNotes(),
            passwordConfirmed -> {
              if (passwordConfirmed.equals(PasswordValidator.Result.SUCCEED)) {
                performAction(item, mode);
              }
            });
      } else {
        performAction(item, mode);
      }
      return true;
    }
  }


  public void finishActionMode() {
    if (getActionMode() != null) {
      getActionMode().finish();
    }
  }


  /**
   * Manage check/uncheck of notes in list during multiple selection phase
   */
  private void toggleListViewItem(View view, int position) {
    Note note = listAdapter.getItem(position);
    LinearLayout cardLayout = view.findViewById(R.id.card_layout);
    if (!getSelectedNotes().contains(note)) {
      getSelectedNotes().add(note);
      listAdapter.addSelectedItem(position);
      cardLayout.setBackgroundColor(getResources().getColor(R.color.list_bg_selected));
    } else {
      getSelectedNotes().remove(note);
      listAdapter.removeSelectedItem(position);
      listAdapter.restoreDrawable(note, cardLayout);
    }
    prepareActionModeMenu();

    if (getSelectedNotes().isEmpty()) {
      finishActionMode();
    }
  }


  /**
   * Notes list initialization. Data, actions and callback are defined here.
   */
  private void initListView() {
    // Note long click to start CAB mode
    RecyclerViewItemClickSupport.addTo(binding.list)
        // Note single click listener managed by the activity itself
        .setOnItemClickListener((recyclerView, position, view) -> {
          if (getActionMode() == null) {
            editNote(listAdapter.getItem(position), view);
            return;
          }
          // If in CAB mode
          toggleListViewItem(view, position);
          setCabTitle();
        }).setOnItemLongClickListener((recyclerView, position, view) -> {
      if (getActionMode() != null) {
        return false;
      }
      // Start the CAB using the ActionMode.Callback defined above
      mainActivity.startSupportActionMode(new ModeCallback());
      toggleListViewItem(view, position);
      setCabTitle();
      return true;
    });

    binding.listRoot.setOnViewTouchedListener(this);
  }


  /**
   * Retrieves from the single listview note item the element to be zoomed when opening a note
   */
  private ImageView getZoomListItemView(View view, Note note) {
    View targetView = null;
    if (!note.getAttachmentsList().isEmpty()) {
      targetView = view.findViewById(R.id.attachmentThumbnail);
    }
    if (targetView == null && note.getCategory() != null) {
      targetView = view.findViewById(R.id.category_marker);
    }
    if (targetView == null) {
      targetView = new ImageView(mainActivity);
      targetView.setBackgroundColor(Color.WHITE);
    }
    targetView.setDrawingCacheEnabled(true);
    targetView.buildDrawingCache();
    Bitmap bmp = targetView.getDrawingCache();
    binding.expandedImage.setBackgroundColor(BitmapUtils.getDominantColor(bmp));

    return binding.expandedImage;
  }


  /**
   * Listener that fires note opening once the zooming animation is finished
   */
  private AnimatorListenerAdapter buildAnimatorListenerAdapter(final Note note) {
    return new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        editNote2(note);
      }
    };
  }


  @Override
  public void onViewTouchOccurred(MotionEvent ev) {
    LogDelegate.vervoseLog("Notes list: onViewTouchOccurred " + ev.getAction());
    commitPending();
  }


  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu_list, menu);
    super.onCreateOptionsMenu(menu, inflater);
    this.menu = menu;
    initSearchView(menu);
  }


  private void initSortingSubmenu() {
    final String[] arrayDb = getResources().getStringArray(R.array.sortable_columns);
    final String[] arrayDialog = getResources()
        .getStringArray(R.array.sortable_columns_human_readable);
    int selected = Arrays.asList(arrayDb).indexOf(Prefs.getString(PREF_SORTING_COLUMN, arrayDb[0]));

    SubMenu sortMenu = this.menu.findItem(R.id.menu_sort).getSubMenu();
    for (int i = 0; i < arrayDialog.length; i++) {
      if (sortMenu.findItem(i) == null) {
        sortMenu.add(MENU_SORT_GROUP_ID, i, i, arrayDialog[i]);
      }
      if (i == selected) {
        sortMenu.getItem(i).setChecked(true);
      }
    }
    sortMenu.setGroupCheckable(MENU_SORT_GROUP_ID, true, true);
  }


  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    setActionItemsVisibility(menu, false);
  }


  private void prepareActionModeMenu() {
    Menu menu = getActionMode().getMenu();
    int navigation = Navigation.getNavigation();
    boolean showArchive =
        navigation == Navigation.NOTES || navigation == Navigation.REMINDERS || navigation ==
            Navigation.UNCATEGORIZED || navigation == Navigation.CATEGORY;
    boolean showUnarchive =
        navigation == Navigation.ARCHIVE || navigation == Navigation.UNCATEGORIZED ||
            navigation == Navigation.CATEGORY;

    if (navigation == Navigation.TRASH) {
      menu.findItem(R.id.menu_untrash).setVisible(true);
      menu.findItem(R.id.menu_delete).setVisible(true);
    } else {
      if (getSelectedCount() == 1) {
        menu.findItem(R.id.menu_share).setVisible(true);
        menu.findItem(R.id.menu_merge).setVisible(false);
        menu.findItem(R.id.menu_archive)
            .setVisible(showArchive && !getSelectedNotes().get(0).isArchived
                ());
        menu.findItem(R.id.menu_unarchive)
            .setVisible(showUnarchive && getSelectedNotes().get(0).isArchived
                ());
      } else {
        menu.findItem(R.id.menu_share).setVisible(false);
        menu.findItem(R.id.menu_merge).setVisible(true);
        menu.findItem(R.id.menu_archive).setVisible(showArchive);
        menu.findItem(R.id.menu_unarchive).setVisible(showUnarchive);

      }
      menu.findItem(R.id.menu_add_reminder).setVisible(true);
      menu.findItem(R.id.menu_category).setVisible(true);
      menu.findItem(R.id.menu_uncomplete_checklists).setVisible(false);
      menu.findItem(R.id.menu_tags).setVisible(true);
      menu.findItem(R.id.menu_trash).setVisible(true);
    }
    menu.findItem(R.id.menu_select_all).setVisible(true);

    setCabTitle();
  }


  private int getSelectedCount() {
    return getSelectedNotes().size();
  }


  private void setCabTitle() {
    if (getActionMode() != null) {
      int title = getSelectedCount();
      getActionMode().setTitle(String.valueOf(title));
    }
  }


  /**
   * SearchView initialization. It's a little complex because it's not using SearchManager but is
   * implementing on its own.
   */
  @SuppressLint("NewApi")
  private void initSearchView(final Menu menu) {

    // Prevents some mysterious NullPointer on app fast-switching
    if (mainActivity == null) {
      return;
    }

    // Save item as class attribute to make it collapse on drawer opening
    searchMenuItem = menu.findItem(R.id.menu_search);

    Bundle args = getArguments();
    if (args != null) {
      Boolean setSearchFocus = args.getBoolean("setSearchFocus");
      if (setSearchFocus == true) {
        searchMenuItem.expandActionView();
        KeyboardUtils.hideKeyboard(this.getView());
      }
    }

    // Associate searchable configuration with the SearchView
    SearchManager searchManager = (SearchManager) mainActivity
        .getSystemService(Context.SEARCH_SERVICE);
    searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));
    searchView.setSearchableInfo(searchManager.getSearchableInfo(mainActivity.getComponentName()));
    searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

    // Expands the widget hiding other actionbar icons
    searchView.setOnQueryTextFocusChangeListener(
        (v, hasFocus) -> setActionItemsVisibility(menu, hasFocus));

    searchMenuItem
        .setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

          boolean searchPerformed = false;


          @Override
          public boolean onMenuItemActionCollapse(MenuItem item) {
            // Reinitialize notes list to all notes when search is collapsed
            searchQuery = null;
            if (binding.searchLayout.getVisibility() == View.VISIBLE) {
              toggleSearchLabel(false);
            }
            mainActivity.getIntent().setAction(Intent.ACTION_MAIN);
            initNotesList(mainActivity.getIntent());
            mainActivity.supportInvalidateOptionsMenu();
            return true;
          }


          @Override
          public boolean onMenuItemActionExpand(MenuItem item) {

            searchView.setOnQueryTextListener(new OnQueryTextListener() {
              @Override
              public boolean onQueryTextSubmit(String arg0) {

                return Prefs.getBoolean("settings_instant_search", false);
              }


              @Override
              public boolean onQueryTextChange(String pattern) {

                if (Prefs.getBoolean("settings_instant_search", false)
                    && binding.searchLayout != null &&
                    searchPerformed && mFragment.isAdded()) {
                  searchTags = null;
                  searchQuery = pattern;
                  NoteLoaderTask.getInstance().execute("getNotesByPattern", pattern);
                  return true;
                } else {
                  searchPerformed = true;
                  return false;
                }
              }
            });
            return true;
          }
        });
  }


  private void setActionItemsVisibility(Menu menu, boolean searchViewHasFocus) {

    boolean drawerOpen =
        mainActivity.getDrawerLayout() != null && mainActivity.getDrawerLayout().isDrawerOpen
            (GravityCompat.START);
    boolean expandedView = Prefs.getBoolean(PREF_EXPANDED_VIEW, true);

    int navigation = Navigation.getNavigation();
    boolean navigationReminders = navigation == Navigation.REMINDERS;
    boolean navigationArchive = navigation == Navigation.ARCHIVE;
    boolean navigationTrash = navigation == Navigation.TRASH;
    boolean navigationCategory = navigation == Navigation.CATEGORY;

    boolean filterPastReminders = Prefs.getBoolean(PREF_FILTER_PAST_REMINDERS, true);
    boolean filterArchivedInCategory = navigationCategory && Prefs
        .getBoolean(PREF_FILTER_ARCHIVED_IN_CATEGORIES + Navigation.getCategory(), false);

    if (isFabAllowed()) {
      fab.setAllowed(true);
      fab.showFab();
    } else {
      fab.setAllowed(false);
      fab.hideFab();
    }
    menu.findItem(R.id.menu_search).setVisible(!drawerOpen);
    menu.findItem(R.id.menu_filter)
        .setVisible(!drawerOpen && !filterPastReminders && navigationReminders &&
            !searchViewHasFocus);
    menu.findItem(R.id.menu_filter_remove)
        .setVisible(!drawerOpen && filterPastReminders && navigationReminders
            && !searchViewHasFocus);
    menu.findItem(R.id.menu_filter_category).setVisible(!drawerOpen && !filterArchivedInCategory &&
        navigationCategory && !searchViewHasFocus);
    menu.findItem(R.id.menu_filter_category_remove)
        .setVisible(!drawerOpen && filterArchivedInCategory &&
            navigationCategory && !searchViewHasFocus);
    menu.findItem(R.id.menu_sort)
        .setVisible(!drawerOpen && !navigationReminders && !searchViewHasFocus);
    menu.findItem(R.id.menu_expanded_view)
        .setVisible(!drawerOpen && !expandedView && !searchViewHasFocus);
    menu.findItem(R.id.menu_contracted_view)
        .setVisible(!drawerOpen && expandedView && !searchViewHasFocus);
    menu.findItem(R.id.menu_empty_trash).setVisible(!drawerOpen && navigationTrash);
    menu.findItem(R.id.menu_uncomplete_checklists).setVisible(searchViewHasFocus);
    menu.findItem(R.id.menu_tags).setVisible(searchViewHasFocus);
  }


  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    Integer[] protectedActions = {R.id.menu_empty_trash};
    if (Arrays.asList(protectedActions).contains(item.getItemId())) {
      mainActivity.requestPassword(mainActivity, getSelectedNotes(), passwordConfirmed -> {
        if (passwordConfirmed.equals(PasswordValidator.Result.SUCCEED)) {
          performAction(item, null);
        }
      });
    } else {
      performAction(item, null);
    }
    return super.onOptionsItemSelected(item);
  }


  /**
   * Performs one of the ActionBar button's actions after checked notes protection
   */
  public void performAction(MenuItem item, ActionMode actionMode) {

    if (isOptionsItemFastClick()) {
      return;
    }

    if (actionMode == null) {
      switch (item.getItemId()) {
        case android.R.id.home:
          DrawerLayout drawerLayout = mainActivity.getDrawerLayout();
          if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
          } else {
            drawerLayout.openDrawer(GravityCompat.START);
          }
          break;
        case R.id.menu_filter:
          filterReminders(true);
          break;
        case R.id.menu_filter_remove:
          filterReminders(false);
          break;
        case R.id.menu_filter_category:
          filterCategoryArchived(true);
          break;
        case R.id.menu_filter_category_remove:
          filterCategoryArchived(false);
          break;
        case R.id.menu_uncomplete_checklists:
          filterByUncompleteChecklists();
          break;
        case R.id.menu_tags:
          filterByTags();
          break;
        case R.id.menu_sort:
          initSortingSubmenu();
          break;
        case R.id.menu_expanded_view:
          switchNotesView();
          break;
        case R.id.menu_contracted_view:
          switchNotesView();
          break;
        case R.id.menu_empty_trash:
          emptyTrash();
          break;
        default:
          LogDelegate.errorLog("Wrong element choosen: " + item.getItemId());
      }
    } else {
      switch (item.getItemId()) {
        case R.id.menu_category:
          categorizeNotes();
          break;
        case R.id.menu_tags:
          tagNotes();
          break;
        case R.id.menu_share:
          share();
          break;
        case R.id.menu_merge:
          merge();
          break;
        case R.id.menu_archive:
          archiveNotes(true);
          break;
        case R.id.menu_unarchive:
          archiveNotes(false);
          break;
        case R.id.menu_trash:
          trashNotes(true);
          break;
        case R.id.menu_untrash:
          trashNotes(false);
          break;
        case R.id.menu_delete:
          deleteNotes();
          break;
        case R.id.menu_select_all:
          selectAllNotes();
          break;
        case R.id.menu_add_reminder:
          addReminders();
          break;
//                case R.ID.menu_synchronize:
//                    synchronizeSelectedNotes();
//                    break;
        default:
          LogDelegate.errorLog("Wrong element choosen: " + item.getItemId());
      }
    }

    checkSortActionPerformed(item);
  }


  private void addReminders() {
    Intent intent = new Intent(OmniNotes.getAppContext(), SnoozeActivity.class);
    intent.setAction(ACTION_POSTPONE);
    intent.putExtra(INTENT_NOTE, selectedNotes.toArray(new Note[selectedNotes.size()]));
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivityForResult(intent, REQUEST_CODE_ADD_ALARMS);
  }


  private void switchNotesView() {
    boolean expandedView = Prefs.getBoolean(PREF_EXPANDED_VIEW, true);
    Prefs.edit().putBoolean(PREF_EXPANDED_VIEW, !expandedView).apply();
    // Change list view
    initNotesList(mainActivity.getIntent());
    // Called to switch menu voices
    mainActivity.supportInvalidateOptionsMenu();
  }


  void editNote(final Note note, final View view) {
    if (note.isLocked() && !Prefs.getBoolean("settings_password_access", false)) {
      PasswordHelper.requestPassword(mainActivity, passwordConfirmed -> {
        if (passwordConfirmed.equals(PasswordValidator.Result.SUCCEED)) {
          note.setPasswordChecked(true);
        }
      });
    }
    AnimationsHelper.zoomListItem(mainActivity, view, getZoomListItemView(view, note),
            binding.listRoot, buildAnimatorListenerAdapter(note));
  }


  void editNote2(Note note) {
    if (note.get_id() == null) {
      LogDelegate.debugLog("Adding new note");
      // if navigation is a category it will be set into note
      try {
        if (checkNavigation(Navigation.CATEGORY) || !isEmpty(mainActivity.navigationTmp)) {
          String categoryId = ObjectUtils.defaultIfNull(mainActivity.navigationTmp,
              Navigation.getCategory().toString());
          note.setCategory(DbHelper.getInstance().getCategory(Long.parseLong(categoryId)));
        }
      } catch (NumberFormatException e) {
        LogDelegate.vervoseLog("Maybe was not a category!");
      }
    } else {
      LogDelegate.debugLog("Editing note with ID: " + note.get_id());
    }

    // Current list scrolling position is saved to be restored later
    refreshListScrollPosition();

    // Fragments replacing
    mainActivity.switchToDetail(note);
  }


  @Override
  // Used to show a Crouton dialog after saved (or tried to) a note
  public void onActivityResult(int requestCode, final int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    switch (requestCode) {
      case REQUEST_CODE_CATEGORY:
        // Dialog retarded to give time to activity's views of being completely initialized
        // The dialog style is choosen depending on result code
        switch (resultCode) {
          case Activity.RESULT_OK:
            mainActivity.showMessage(R.string.category_saved, ONStyle.CONFIRM);
            EventBus.getDefault().post(new CategoriesUpdatedEvent());
            break;
          case Activity.RESULT_FIRST_USER:
            mainActivity.showMessage(R.string.category_deleted, ONStyle.ALERT);
            break;
          default:
            break;
        }

        break;

      case REQUEST_CODE_CATEGORY_NOTES:
        if (intent != null) {
          Category tag = intent.getParcelableExtra(INTENT_CATEGORY);
          categorizeNotesExecute(tag);
        }
        break;

      case REQUEST_CODE_ADD_ALARMS:
        selectedNotes.clear();
        finishActionMode();
        break;

      default:
        break;
    }
  }

  private void checkSortActionPerformed(MenuItem item) {
    if (item.getGroupId() == MENU_SORT_GROUP_ID) {
      final String[] arrayDb = getResources().getStringArray(R.array.sortable_columns);
      Prefs.edit().putString(PREF_SORTING_COLUMN, arrayDb[item.getOrder()]).apply();
      initNotesList(mainActivity.getIntent());
      // Resets list scrolling position
      listViewPositionOffset = 16;
      listViewPosition = 0;
      restoreListScrollPosition();
      toggleSearchLabel(false);
      // Updates app widgets
      mainActivity.updateWidgets();
    }
  }

  /**
   * Empties trash deleting all the notes
   */
  private void emptyTrash() {
    new MaterialDialog.Builder(mainActivity)
        .content(R.string.empty_trash_confirmation)
        .positiveText(R.string.ok)
        .onPositive((dialog, which) -> {
          boolean mustDeleteLockedNotes = false;
          for (int i = 0; i < listAdapter.getItemCount(); i++) {
            selectedNotes.add(listAdapter.getItem(i));
            mustDeleteLockedNotes = mustDeleteLockedNotes || listAdapter.getItem(i).isLocked();
          }
          if (mustDeleteLockedNotes) {
            mainActivity.requestPassword(mainActivity, getSelectedNotes(),
                passwordConfirmed -> {
                  if (passwordConfirmed.equals(PasswordValidator.Result.SUCCEED)) {
                    deleteNotesExecute();
                  }
                });
          } else {
            deleteNotesExecute();
          }
        }).build().show();
  }


  /**
   * Notes list adapter initialization and association to view
   *
   * @FIXME: This method is a divine disgrace and MUST be refactored. I'm ashamed by myself.
   */
  void initNotesList(Intent intent) {
    LogDelegate.debugLog("initNotesList intent: " + intent.getAction());

    binding.progressWheel.setAlpha(1);
    binding.list.setAlpha(0);

    // Search for a tag
    // A workaround to simplify it's to simulate normal search
    if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getCategories() != null
        && intent.getCategories().contains(Intent.CATEGORY_BROWSABLE)) {
      searchTags = intent.getDataString().replace(UrlCompleter.HASHTAG_SCHEME, "");
      goBackOnToggleSearchLabel = true;
    }

    if (ACTION_SHORTCUT_WIDGET.equals(intent.getAction())) {
      return;
    }

    // Searching
    searchQuery = searchQueryInstant;
    searchQueryInstant = null;
    if (searchTags != null || searchQuery != null || searchUncompleteChecklists
        || IntentChecker
        .checkAction(intent, Intent.ACTION_SEARCH, ACTION_SEARCH_UNCOMPLETE_CHECKLISTS)) {

      // Using tags
      if (searchTags != null && intent.getStringExtra(SearchManager.QUERY) == null) {
        searchQuery = searchTags;
        NoteLoaderTask.getInstance()
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "getNotesByTag",
                searchQuery);
      } else if (searchUncompleteChecklists || ACTION_SEARCH_UNCOMPLETE_CHECKLISTS.equals(
          intent.getAction())) {
        searchQuery = getContext().getResources().getString(R.string.uncompleted_checklists);
        searchUncompleteChecklists = true;
        NoteLoaderTask.getInstance()
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "getNotesByUncompleteChecklist");
      } else {
        // Get the intent, verify the action and get the query
        if (intent.getStringExtra(SearchManager.QUERY) != null) {
          searchQuery = intent.getStringExtra(SearchManager.QUERY);
          searchTags = null;
        }
        NoteLoaderTask.getInstance()
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "getNotesByPattern",
                searchQuery);
      }

      toggleSearchLabel(true);

    } else {
      // Check if is launched from a widget with categories
      if ((ACTION_WIDGET_SHOW_LIST.equals(intent.getAction()) && intent.hasExtra(INTENT_WIDGET))
          || !isEmpty(mainActivity.navigationTmp)) {
        String widgetId =
            intent.hasExtra(INTENT_WIDGET) ? intent.getExtras().get(INTENT_WIDGET).toString()
                : null;
        if (widgetId != null) {
          String sqlCondition = Prefs.getString(PREF_WIDGET_PREFIX + widgetId, "");
          String categoryId = TextHelper.checkIntentCategory(sqlCondition);
          mainActivity.navigationTmp = !isEmpty(categoryId) ? categoryId : null;
        }
        intent.removeExtra(INTENT_WIDGET);
        if (mainActivity.navigationTmp != null) {
          Long categoryId = Long.parseLong(mainActivity.navigationTmp);
          NoteLoaderTask.getInstance().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
              "getNotesByCategory", categoryId);
        } else {
          NoteLoaderTask.getInstance()
              .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "getAllNotes", true);
        }

      } else {
        NoteLoaderTask.getInstance()
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "getAllNotes", true);
      }
    }
  }


  public void toggleSearchLabel(boolean activate) {
    if (activate) {
      binding.searchQuery.setText(fromHtml(getString(R.string.search) + ":<b> " + searchQuery + "</b>"));
      binding.searchLayout.setVisibility(View.VISIBLE);
      binding.searchCancel.setOnClickListener(v -> toggleSearchLabel(false));
      searchLabelActive = true;
    } else {
      if (searchLabelActive) {
        searchLabelActive = false;
        AnimationsHelper.expandOrCollapse(binding.searchLayout, false);
        searchTags = null;
        searchQuery = null;
        searchUncompleteChecklists = false;
        if (!goBackOnToggleSearchLabel) {
          mainActivity.getIntent().setAction(Intent.ACTION_MAIN);
          if (searchView != null) {
            searchMenuItem.collapseActionView();
          }
          initNotesList(mainActivity.getIntent());
        } else {
          mainActivity.onBackPressed();
        }
        goBackOnToggleSearchLabel = false;
        if (Intent.ACTION_VIEW.equals(mainActivity.getIntent().getAction())) {
          mainActivity.getIntent().setAction(null);
        }
      }
    }
  }


  public void onEvent(NavigationUpdatedNavDrawerClosedEvent navigationUpdatedNavDrawerClosedEvent) {
    listViewPosition = 0;
    listViewPositionOffset = 16;
    initNotesList(mainActivity.getIntent());
    setActionItemsVisibility(menu, false);
  }


  public void onEvent(CategoriesUpdatedEvent categoriesUpdatedEvent) {
    initNotesList(mainActivity.getIntent());
  }


  public void onEvent(NotesLoadedEvent notesLoadedEvent) {
    listAdapter = new NoteAdapter(mainActivity, Prefs.getBoolean(PREF_EXPANDED_VIEW, true),
        notesLoadedEvent.getNotes());

    initSwipeGesture();

    binding.list.setAdapter(listAdapter);

    // Restores listview position when turning back to list or when navigating reminders
    if (!notesLoadedEvent.getNotes().isEmpty()) {
      if (checkNavigation(Navigation.REMINDERS)) {
        listViewPosition = listAdapter.getClosestNotePosition();
      }
      restoreListScrollPosition();
    }

    animateListView();

    closeFab();
  }

  private void initSwipeGesture() {
    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0,
        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

      @Override
      public boolean onMove(@NonNull RecyclerView recyclerView,
          @NonNull RecyclerView.ViewHolder viewHolder,
          @NonNull RecyclerView.ViewHolder target) {
        return false;
      }

      @Override
      public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
        int swipedPosition = viewHolder.getAdapterPosition();
        finishActionMode();
        swipeNote(swipedPosition);
      }
    };
    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);

    if (Navigation.getNavigation() != Navigation.UNCATEGORIZED && Prefs
        .getBoolean(PREF_ENABLE_SWIPE, true)) {
      itemTouchHelper.attachToRecyclerView(binding.list);
    } else {
      itemTouchHelper.attachToRecyclerView(null);
    }
  }

  private void swipeNote(int swipedPosition) {
    try {
      Note note = listAdapter.getItem(swipedPosition);
      if (note.isLocked()) {
        PasswordHelper.requestPassword(mainActivity, passwordConfirmed -> {
          if (passwordConfirmed.equals(PasswordValidator.Result.SUCCEED)) {
            onNoteSwipedPerformAction(note);
          } else {
            onUndo(null);
          }
        });
      } else {
        onNoteSwipedPerformAction(note);
      }
    } catch (IndexOutOfBoundsException e) {
      LogDelegate.debugLog("Please stop swiping in the zone beneath the last card");
    }
  }

  private void onNoteSwipedPerformAction(Note note) {
    getSelectedNotes().add(note);

    // Depending on settings and note status this action will...
    // ...restore
    if (checkNavigation(Navigation.TRASH)) {
      trashNotes(false);
    }
    // ...removes category
    else if (checkNavigation(Navigation.CATEGORY)) {
      categorizeNotesExecute(null);
    } else {
      // ...trash
      if (Prefs.getBoolean("settings_swipe_to_trash", false)
          || checkNavigation(Navigation.ARCHIVE)) {
        trashNotes(true);
        // ...archive
      } else {
        archiveNotes(true);
      }
    }
  }

  public void onEvent(PasswordRemovedEvent passwordRemovedEvent) {
    initNotesList(mainActivity.getIntent());
  }

  private void animateListView() {
    if (!OmniNotes.isDebugBuild()) {
      animate(binding.progressWheel)
          .setDuration(getResources().getInteger(R.integer.list_view_fade_anim)).alpha(0);
      animate(binding.list).setDuration(getResources().getInteger(R.integer.list_view_fade_anim))
          .alpha(1);
    } else {
      binding.progressWheel.setVisibility(View.INVISIBLE);
      binding.list.setAlpha(1);
    }
  }


  private void restoreListScrollPosition() {
    if (listAdapter.getItemCount() > listViewPosition) {
      binding.list.getLayoutManager().scrollToPosition(listViewPosition);
      new Handler().postDelayed(fab::showFab, 150);
    } else {
      binding.list.getLayoutManager().scrollToPosition(0);
    }
  }


  /**
   * Batch note trashing
   */
  public void trashNotes(boolean trash) {
    int selectedNotesSize = getSelectedNotes().size();

    // Restore is performed immediately, otherwise undo bar is shown
    if (trash) {
      trackModifiedNotes(getSelectedNotes());
      for (Note note : getSelectedNotes()) {
        listAdapter.remove(note);
        ReminderHelper.removeReminder(OmniNotes.getAppContext(), note);
      }
    } else {
      trashNote(getSelectedNotes(), false);
    }

    listAdapter.notifyDataSetChanged();
    finishActionMode();

    // Advice to user
    if (trash) {
      mainActivity.showMessage(R.string.note_trashed, ONStyle.WARN);
    } else {
      mainActivity.showMessage(R.string.note_untrashed, ONStyle.INFO);
    }

    // Creation of undo bar
    if (trash) {
      ubc.showUndoBar(false, selectedNotesSize + " " + getString(R.string.trashed), null);
      fab.hideFab();
      undoTrash = true;
    } else {
      getSelectedNotes().clear();
    }
  }


  private ActionMode getActionMode() {
    return actionMode;
  }


  private List<Note> getSelectedNotes() {
    return selectedNotes;
  }


  /**
   * Single note logical deletion
   */
  @SuppressLint("NewApi")
  protected void trashNote(List<Note> notes, boolean trash) {
    listAdapter.remove(notes);
    new NoteProcessorTrash(notes, trash).process();
  }


  /**
   * Selects all notes in list
   */
  private void selectAllNotes() {
    for (int i = 0; i < binding.list.getChildCount(); i++) {
      LinearLayout v = binding.list.getChildAt(i).findViewById(R.id.card_layout);
      v.setBackgroundColor(getResources().getColor(R.color.list_bg_selected));
    }
    selectedNotes.clear();
    for (int i = 0; i < listAdapter.getItemCount(); i++) {
      selectedNotes.add(listAdapter.getItem(i));
      listAdapter.addSelectedItem(i);
    }
    prepareActionModeMenu();
    setCabTitle();
  }


  /**
   * Batch note permanent deletion
   */
  private void deleteNotes() {
    new MaterialDialog.Builder(mainActivity)
        .content(R.string.delete_note_confirmation)
        .positiveText(R.string.ok)
        .onPositive(
            (dialog, which) -> mainActivity.requestPassword(mainActivity, getSelectedNotes(),
                passwordConfirmed -> {
                  if (passwordConfirmed.equals(PasswordValidator.Result.SUCCEED)) {
                    deleteNotesExecute();
                  }
                }))
        .build()
        .show();
  }


  /**
   * Performs notes permanent deletion after confirmation by the user
   */
  private void deleteNotesExecute() {
    listAdapter.remove(getSelectedNotes());
    new NoteProcessorDelete(getSelectedNotes()).process();
    selectedNotes.clear();
    finishActionMode();
    mainActivity.showMessage(R.string.note_deleted, ONStyle.ALERT);
  }


  /**
   * Batch note archiviation
   */
  public void archiveNotes(boolean archive) {
    int selectedNotesSize = getSelectedNotes().size();
    // Used in undo bar commit
    sendToArchive = archive;

    if (!archive) {
      archiveNote(getSelectedNotes(), false);
    } else {
      trackModifiedNotes(getSelectedNotes());
    }

    for (Note note : getSelectedNotes()) {
      // If is restore it will be done immediately, otherwise the undo bar will be shown
      if (archive) {
        // Saves archived state to eventually undo
        undoArchivedMap.put(note, note.isArchived());
      }

      // If actual navigation is not "Notes" the item will not be removed but replaced to fit the new state
      if (checkNavigation(Navigation.NOTES)
          || (checkNavigation(Navigation.ARCHIVE) && !archive)
          || (checkNavigation(Navigation.CATEGORY) && Prefs.getBoolean(
          PREF_FILTER_ARCHIVED_IN_CATEGORIES + Navigation.getCategory(), false))) {
        listAdapter.remove(note);
      } else {
        note.setArchived(archive);
        listAdapter.replace(note, listAdapter.getPosition(note));
      }
    }

    listAdapter.notifyDataSetChanged();
    finishActionMode();

    // Advice to user
    int msg = archive ? R.string.note_archived : R.string.note_unarchived;
    Style style = archive ? ONStyle.WARN : ONStyle.INFO;
    mainActivity.showMessage(msg, style);

    // Creation of undo bar
    if (archive) {
      ubc.showUndoBar(false, selectedNotesSize + " " + getString(R.string.archived), null);
      fab.hideFab();
      undoArchive = true;
    } else {
      getSelectedNotes().clear();
    }
  }


  /**
   * Saves notes to be eventually restored at right position
   */
  private void trackModifiedNotes(List<Note> modifiedNotesToTrack) {
    for (Note note : modifiedNotesToTrack) {
      undoNotesMap.put(listAdapter.getPosition(note), note);
    }
  }


  private void archiveNote(List<Note> notes, boolean archive) {
    new NoteProcessorArchive(notes, archive).process();
    if (!checkNavigation(Navigation.CATEGORY)) {
      listAdapter.remove(notes);
    }
    LogDelegate.debugLog("Notes" + (archive ? "archived" : "restored from archive"));
  }


  /**
   * Categories addition and editing
   */
  void editCategory(Category category) {
    Intent categoryIntent = new Intent(mainActivity, CategoryActivity.class);
    categoryIntent.putExtra(INTENT_CATEGORY, category);
    startActivityForResult(categoryIntent, REQUEST_CODE_CATEGORY);
  }


  /**
   * Associates to or removes categories
   */
  private void categorizeNotes() {
    // Retrieves all available categories
    final ArrayList<Category> categories = DbHelper.getInstance().getCategories();

    final MaterialDialog dialog = new MaterialDialog.Builder(mainActivity)
        .title(R.string.categorize_as)
        .adapter(new CategoryRecyclerViewAdapter(mainActivity, categories), null)
        .positiveText(R.string.add_category)
        .positiveColorRes(R.color.colorPrimary)
        .negativeText(R.string.remove_category)
        .negativeColorRes(R.color.colorAccent)
        .onPositive((dialog1, which) -> {
          keepActionMode = true;
          Intent intent = new Intent(mainActivity, CategoryActivity.class);
          intent.putExtra("noHome", true);
          startActivityForResult(intent, REQUEST_CODE_CATEGORY_NOTES);
        }).onNegative((dialog12, which) -> categorizeNotesExecute(null)).build();

    RecyclerViewItemClickSupport.addTo(dialog.getRecyclerView())
        .setOnItemClickListener((recyclerView, position, v) -> {
          dialog.dismiss();
          categorizeNotesExecute(categories.get(position));
        });

    dialog.show();
  }


  private void categorizeNotesExecute(Category category) {
    if (category != null) {
      categorizeNote(getSelectedNotes(), category);
    } else {
      trackModifiedNotes(getSelectedNotes());
    }
    for (Note note : getSelectedNotes()) {
      // If is restore it will be done immediately, otherwise the undo bar
      // will be shown
      if (category == null) {
        // Saves categories associated to eventually undo
        undoCategoryMap.put(note, note.getCategory());
      }
      // Update adapter content if actual navigation is the category
      // associated with actually cycled note
      if ((checkNavigation(Navigation.CATEGORY) && !Navigation
          .checkNavigationCategory(category)) ||
          checkNavigation(Navigation.UNCATEGORIZED)) {
        listAdapter.remove(note);
      } else {
        note.setCategory(category);
        listAdapter.replace(note, listAdapter.getPosition(note));
      }
    }

    finishActionMode();

    // Advice to user
    String msg;
    if (category != null) {
      msg = getResources().getText(R.string.notes_categorized_as) + " '" + category.getName() + "'";
    } else {
      msg = getResources().getText(R.string.notes_category_removed).toString();
    }
    mainActivity.showMessage(msg, ONStyle.INFO);

    // Creation of undo bar
    if (category == null) {
      ubc.showUndoBar(false, getString(R.string.notes_category_removed), null);
      fab.hideFab();
      undoCategorize = true;
      undoCategorizeCategory = null;
    } else {
      getSelectedNotes().clear();
    }
  }


  private void categorizeNote(List<Note> notes, Category category) {
    new NoteProcessorCategorize(notes, category).process();
  }


  /**
   * Bulk tag selected notes
   */
  private void tagNotes() {

    // Retrieves all available tags
    final List<Tag> tags = DbHelper.getInstance().getTags();

    // If there is no tag a message will be shown
    if (tags.isEmpty()) {
      finishActionMode();
      mainActivity.showMessage(R.string.no_tags_created, ONStyle.WARN);
      return;
    }

    final Integer[] preSelectedTags = TagsHelper.getPreselectedTagsArray(selectedNotes, tags);

    new MaterialDialog.Builder(mainActivity)
        .title(R.string.select_tags)
        .items(TagsHelper.getTagsArray(tags))
        .positiveText(R.string.ok)
        .itemsCallbackMultiChoice(preSelectedTags, (dialog, which, text) -> {
          dialog.dismiss();
          tagNotesExecute(tags, which, preSelectedTags);
          return false;
        }).build().show();
  }


  private void tagNotesExecute(List<Tag> tags, Integer[] selectedTags, Integer[] preSelectedTags) {
    for (Note note : getSelectedNotes()) {
      tagNote(tags, selectedTags, note);
    }

    if (getActionMode() != null) {
      getActionMode().finish();
    }

    mainActivity.showMessage(R.string.tags_added, ONStyle.INFO);
  }


  private void tagNote(List<Tag> tags, Integer[] selectedTags, Note note) {

    Pair<String, List<Tag>> taggingResult = TagsHelper.addTagToNote(tags, selectedTags, note);

    if (note.isChecklist()) {
      note.setTitle(note.getTitle() + System.getProperty("line.separator") + taggingResult.first);
    } else {
      StringBuilder sb = new StringBuilder(note.getContent());
      if (sb.length() > 0) {
        sb.append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"));
      }
      sb.append(taggingResult.first);
      note.setContent(sb.toString());
    }

    eventuallyRemoveDeselectedTags(note, taggingResult.second);

    DbHelper.getInstance().updateNote(note, false);
  }

  private void eventuallyRemoveDeselectedTags(Note note, List<Tag> tagsToRemove) {
    if (CollectionUtils.isNotEmpty(tagsToRemove)) {
      String titleWithoutTags = TagsHelper.removeTags(note.getTitle(), tagsToRemove);
      note.setTitle(titleWithoutTags);
      String contentWithoutTags = TagsHelper.removeTags(note.getContent(), tagsToRemove);
      note.setContent(contentWithoutTags);
    }
  }

//	private void synchronizeSelectedNotes() {
//		new DriveSyncTask(mainActivity).execute(new ArrayList<Note>(getSelectedNotes()));
//		// Clears data structures
//		listAdapter.clearSelectedItems();
//		list.clearChoices();
//		finishActionMode();
//	}


  @Override
  public void onUndo(Parcelable undoToken) {
    // Cycles removed items to re-insert into adapter
    for (Integer notePosition : undoNotesMap.keySet()) {
      Note currentNote = undoNotesMap.get(notePosition);
      //   Manages uncategorize or archive  undo
      if ((undoCategorize && !Navigation.checkNavigationCategory(undoCategoryMap.get(currentNote)))
          || undoArchive && !checkNavigation(Navigation.NOTES)) {
        if (undoCategorize) {
          currentNote.setCategory(undoCategoryMap.get(currentNote));
        } else if (undoArchive) {
          currentNote.setArchived(undoArchivedMap.get(currentNote));
        }
        listAdapter.replace(currentNote, listAdapter.getPosition(currentNote));
        // Manages trash undo
      } else {
        listAdapter.add(notePosition, currentNote);
      }
    }

    listAdapter.notifyDataSetChanged();

    selectedNotes.clear();
    undoNotesMap.clear();

    undoTrash = false;
    undoArchive = false;
    undoCategorize = false;
    undoNotesMap.clear();
    undoCategoryMap.clear();
    undoArchivedMap.clear();
    undoCategorizeCategory = null;
    Crouton.cancelAllCroutons();

    if (getActionMode() != null) {
      getActionMode().finish();
    }
    ubc.hideUndoBar(false);
    fab.showFab();
  }


  void commitPending() {
    if (undoTrash || undoArchive || undoCategorize) {

      List<Note> notesList = new ArrayList<>(undoNotesMap.values());
      if (undoTrash) {
        trashNote(notesList, true);
      } else if (undoArchive) {
        archiveNote(notesList, sendToArchive);
      } else if (undoCategorize) {
        categorizeNote(notesList, undoCategorizeCategory);
      }

      undoTrash = false;
      undoArchive = false;
      undoCategorize = false;
      undoCategorizeCategory = null;

      // Clears data structures
      selectedNotes.clear();
      undoNotesMap.clear();
      undoCategoryMap.clear();
      undoArchivedMap.clear();

      ubc.hideUndoBar(false);
      fab.showFab();

      LogDelegate.debugLog("Changes committed");
    }
    mainActivity.updateWidgets();
  }


  /**
   * Shares the selected note from the list
   */
  private void share() {
    // Only one note should be selected to perform sharing but they'll be cycled anyhow
    for (final Note note : getSelectedNotes()) {
      mainActivity.shareNote(note);
    }

    getSelectedNotes().clear();
    if (getActionMode() != null) {
      getActionMode().finish();
    }
  }


  public void merge() {
    EventBus.getDefault().post(new NotesMergeEvent(false));
  }


  /**
   * Merges all the selected notes
   */
  public void onEventAsync(NotesMergeEvent notesMergeEvent) {

    final Note finalMergedNote = NotesHelper
        .mergeNotes(getSelectedNotes(), notesMergeEvent.keepMergedNotes);
    new Handler(Looper.getMainLooper()).post(() -> {

      if (!notesMergeEvent.keepMergedNotes) {
        ArrayList<String> notesIds = new ArrayList<>();
        for (Note selectedNote : getSelectedNotes()) {
          notesIds.add(String.valueOf(selectedNote.get_id()));
        }
        mainActivity.getIntent().putExtra("merged_notes", notesIds);
      }

      getSelectedNotes().clear();
      if (getActionMode() != null) {
        getActionMode().finish();
      }

      mainActivity.getIntent().setAction(ACTION_MERGE);
      mainActivity.switchToDetail(finalMergedNote);
    });
  }


  /**
   * Excludes past reminders
   */
  private void filterReminders(boolean filter) {
    Prefs.edit().putBoolean(PREF_FILTER_PAST_REMINDERS, filter).apply();
    // Change list view
    initNotesList(mainActivity.getIntent());
    // Called to switch menu voices
    mainActivity.supportInvalidateOptionsMenu();
  }


  /**
   * Excludes archived notes in categories navigation
   */
  private void filterCategoryArchived(boolean filter) {
    if (filter) {
      Prefs.edit().putBoolean(PREF_FILTER_ARCHIVED_IN_CATEGORIES + Navigation.getCategory(), true)
          .apply();
    } else {
      Prefs.edit().remove(PREF_FILTER_ARCHIVED_IN_CATEGORIES + Navigation.getCategory()).apply();
    }
    // Change list view
    initNotesList(mainActivity.getIntent());
    // Called to switch menu voices
    mainActivity.supportInvalidateOptionsMenu();
  }


  private void filterByUncompleteChecklists() {
    initNotesList(new Intent(ACTION_SEARCH_UNCOMPLETE_CHECKLISTS));
  }

  private void filterByTags() {

    final List<Tag> tags = TagsHelper.getAllTags();

    if (tags.isEmpty()) {
      mainActivity.showMessage(R.string.no_tags_created, ONStyle.WARN);
      return;
    }

    // Dialog and events creation
    new MaterialDialog.Builder(mainActivity)
        .title(R.string.select_tags)
        .items(TagsHelper.getTagsArray(tags))
        .positiveText(R.string.ok)
        .itemsCallbackMultiChoice(new Integer[]{}, (dialog, which, text) -> {
          // Retrieves selected tags
          List<String> selectedTags = new ArrayList<>();
          for (Integer aWhich : which) {
            selectedTags.add(tags.get(aWhich).getText());
          }

          // Saved here to allow persisting search
          searchTags = selectedTags.toString().substring(1, selectedTags.toString().length() - 1)
              .replace(" ", "");
          Intent intent = mainActivity.getIntent();

          // Hides keyboard
          searchView.clearFocus();
          KeyboardUtils.hideKeyboard(searchView);

          intent.removeExtra(SearchManager.QUERY);
          initNotesList(intent);
          return false;
        }).build().show();
  }


  public MenuItem getSearchMenuItem() {
    return searchMenuItem;
  }


  private boolean isFabAllowed() {
    return isFabAllowed(false);
  }


  private boolean isFabAllowed(boolean actionModeFinishing) {

    boolean isAllowed = true;

    // Actionmode check
    isAllowed = isAllowed && (getActionMode() == null || actionModeFinishing);
    // Navigation check
    int navigation = Navigation.getNavigation();
    isAllowed = isAllowed && navigation != Navigation.ARCHIVE && navigation != Navigation.REMINDERS
        && navigation
        != Navigation.TRASH;
    // Navigation drawer check
    isAllowed =
        isAllowed && mainActivity.getDrawerLayout() != null && !mainActivity.getDrawerLayout()
            .isDrawerOpen
                (GravityCompat.START);

    return isAllowed;
  }


}
