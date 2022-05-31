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

import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_NOTIFICATION_CLICK;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_RESTART_APP;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_SEND_AND_EXIT;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_SHORTCUT;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_SHORTCUT_WIDGET;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_START_APP;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_WIDGET;
import static it.feio.android.omninotes.utils.ConstantsBase.ACTION_WIDGET_TAKE_PHOTO;
import static it.feio.android.omninotes.utils.ConstantsBase.INTENT_GOOGLE_NOW;
import static it.feio.android.omninotes.utils.ConstantsBase.INTENT_KEY;
import static it.feio.android.omninotes.utils.ConstantsBase.INTENT_NOTE;
import static it.feio.android.omninotes.utils.ConstantsBase.PREF_PASSWORD;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.pixplicity.easyprefs.library.Prefs;

import org.apache.commons.collections4.ArrayStack;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import it.feio.android.omninotes.async.UpdateWidgetsTask;
import it.feio.android.omninotes.async.bus.PasswordRemovedEvent;
import it.feio.android.omninotes.async.bus.SwitchFragmentEvent;
import it.feio.android.omninotes.async.notes.NoteProcessorDelete;
import it.feio.android.omninotes.databinding.ActivityMainBinding;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.helpers.LogDelegate;
import it.feio.android.omninotes.helpers.NotesHelper;
import it.feio.android.omninotes.intro.IntroActivity;
import it.feio.android.omninotes.models.Attachment;
import it.feio.android.omninotes.models.Category;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.models.ONStyle;
import it.feio.android.omninotes.utils.FileProviderHelper;
import it.feio.android.omninotes.utils.Navigation;
import it.feio.android.omninotes.utils.PasswordHelper;
import it.feio.android.omninotes.utils.SystemHelper;
import it.feio.android.pixlui.links.UrlCompleter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import lombok.Getter;
import lombok.Setter;


public class MainActivity extends BaseActivity implements
    SharedPreferences.OnSharedPreferenceChangeListener {

  private boolean isPasswordAccepted = false;
  public final static String FRAGMENT_DRAWER_TAG = "fragment_drawer";
  public final static String FRAGMENT_LIST_TAG = "fragment_list";
  public final static String FRAGMENT_DETAIL_TAG = "fragment_detail";
  public final static String FRAGMENT_SKETCH_TAG = "fragment_sketch";
  @Getter
  @Setter
  private Uri sketchUri;
  private boolean prefsChanged = false;
  private FragmentManager mFragmentManager;

  private ActivityMainBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTheme(R.style.OmniNotesTheme_ApiSpec);

    binding = ActivityMainBinding.inflate(getLayoutInflater());
    View view = binding.getRoot();
    setContentView(view);

    EventBus.getDefault().register(this);
    Prefs.getPreferences().registerOnSharedPreferenceChangeListener(this);

    initUI();

    if (IntroActivity.mustRun()) {
      startActivity(new Intent(getApplicationContext(), IntroActivity.class));
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (isPasswordAccepted) {
      init();
    } else {
      checkPassword();
    }
  }


  @Override
  protected void onStop() {
    super.onStop();
    EventBus.getDefault().unregister(this);
  }


  private void initUI() {
    setSupportActionBar(binding.toolbar.toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);
  }

  /**
   * This method starts the bootstrap chain.
   */
  private void checkPassword() {
    boolean passwordExists = Prefs.getString(PREF_PASSWORD, null) != null
            && Prefs.getBoolean("settings_password_access", false);
    if (passwordExists) {
      PasswordHelper.requestPassword(this, passwordConfirmed -> {
        switch (passwordConfirmed) {
          case SUCCEED:
            init();
            break;
          case FAIL:
            finish();
            break;
          case RESTORE:
            PasswordHelper.resetPassword(this);
        }
      });
    } else {
      init();
    }
  }

  public void onEvent(PasswordRemovedEvent passwordRemovedEvent) {
    showMessage(R.string.password_successfully_removed, ONStyle.ALERT);
    init();
  }

  private void init() {
    setPasswordAcceptedTrue();
    initNavigationDrawerFragment();
    beginListFragmentTransaction();
    handleIntents();
  }

  private void setPasswordAcceptedTrue() {
    isPasswordAccepted = true;
  }

  private void initNavigationDrawerFragment() {
    NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManagerInstance()
            .findFragmentById(R.id.navigation_drawer);
    if (mNavigationDrawerFragment == null) {
      FragmentTransaction fragmentTransaction = getFragmentManagerInstance().beginTransaction();
      fragmentTransaction.replace(R.id.navigation_drawer, new NavigationDrawerFragment(),
              FRAGMENT_DRAWER_TAG).commit();
    }
  }

  private void beginListFragmentTransaction() {
    ListFragment mListFragment = (ListFragment) getFragmentManagerInstance().findFragmentByTag(FRAGMENT_LIST_TAG);
    if (mListFragment == null) {
      FragmentTransaction fragmentTransaction = getFragmentManagerInstance().beginTransaction();
      fragmentTransaction.add(R.id.fragment_container, new ListFragment(), FRAGMENT_LIST_TAG)
              .commit();
    }
  }

  private FragmentManager getFragmentManagerInstance() {
    if (mFragmentManager == null) {
      mFragmentManager = getSupportFragmentManager();
    }
    return mFragmentManager;
  }

  @Override
  protected void onNewIntent(Intent intent) {
    if (intent.getAction() == null) {
      intent.setAction(ACTION_START_APP);
    }
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntents();
    LogDelegate.debugLog("onNewIntent");
  }


  public MenuItem getSearchMenuItem() {
    Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
    if (f != null) {
      return ((ListFragment) f).getSearchMenuItem();
    } else {
      return null;
    }
  }


  public void editTag(Category tag) {
    Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
    if (f != null) {
      ((ListFragment) f).editCategory(tag);
    }
  }

  public void initNotesList(Intent intent) {
    if (intent != null) {
      Fragment searchTagFragment = startSearchView();
      new Handler(getMainLooper()).post(() -> ((ListFragment) searchTagFragment).initNotesList(intent));
    }
  }

  private Fragment startSearchView() {
    FragmentTransaction transaction = getFragmentManagerInstance().beginTransaction();
    animateTransition(transaction, TRANSITION_HORIZONTAL);
    ListFragment mListFragment = new ListFragment();
    transaction.replace(R.id.fragment_container, mListFragment, FRAGMENT_LIST_TAG).addToBackStack
            (FRAGMENT_DETAIL_TAG).commit();
    setListFragmentArguments(mListFragment);
    return mListFragment;
  }

  private void setListFragmentArguments(ListFragment mListFragment) {
    Bundle args = new Bundle();
    args.putBoolean("setSearchFocus", true);
    mListFragment.setArguments(args);
  }


  public void commitPending() {
    Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
    if (f != null) {
      ((ListFragment) f).commitPending();
    }
  }


  /**
   * Checks if allocated fragment is of the required type and then returns it or returns null
   */
  private Fragment checkFragmentInstance(int id, Object instanceClass) {
    Fragment result = null;
    Fragment fragment = getFragmentManagerInstance().findFragmentById(id);
    if (fragment != null && instanceClass.equals(fragment.getClass())) {
      result = fragment;
    }
    return result;
  }

  @Override
  public void onBackPressed() {
    inCaseOfSketchFragment();
    inCaseOfDetailFragment();
    inCaseOfListFragment();
    super.onBackPressed();
  }

  private void inCaseOfSketchFragment() {
    // SketchFragment
    Fragment fragment = checkFragmentInstance(R.id.fragment_container, SketchFragment.class);
    if (fragment != null) {
      ((SketchFragment) fragment).save();

      // Removes forced portrait orientation for this fragment
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

      getFragmentManagerInstance().popBackStack();
    }
  }

  private void inCaseOfDetailFragment() {
    // DetailFragment
    Fragment fragment = checkFragmentInstance(R.id.fragment_container, DetailFragment.class);
    if (fragment != null) {
      ((DetailFragment) fragment).goBack = true;
      ((DetailFragment) fragment).saveAndExit((DetailFragment) fragment);
    }
  }

  private void inCaseOfListFragment() {
    // ListFragment
    Fragment fragment = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
    if (fragment != null) {
      // Before exiting from app the navigation drawer is opened
      boolean isNavdrawerOnExitSetted = Prefs.getBoolean("settings_navdrawer_on_exit", false);
      boolean isDrawerLayoutInExistence = getDrawerLayout() != null;
      boolean isDrawerOpened = getDrawerLayout().isDrawerOpen(GravityCompat.START);
      if (isNavdrawerOnExitSetted && isDrawerLayoutInExistence && !isDrawerOpened) {
        getDrawerLayout().openDrawer(GravityCompat.START);
      } else if (!isNavdrawerOnExitSetted && isDrawerLayoutInExistence && isDrawerOpened) {
        getDrawerLayout().closeDrawer(GravityCompat.START);
      } else {
        if (!((ListFragment) fragment).closeFab()) {
          setPasswordAcceptedFalse();
        }
      }
    }
  }

  private void setPasswordAcceptedFalse() {
    isPasswordAccepted = false;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString("navigationTmp", navigationTmp);
  }


  @Override
  protected void onPause() {
    super.onPause();
    Crouton.cancelAllCroutons();
  }


  public DrawerLayout getDrawerLayout() {
    return binding.drawerLayout;
  }


  public ActionBarDrawerToggle getDrawerToggle() {
    NavigationDrawerFragment mNavigationDrawerFragment =
            (NavigationDrawerFragment) getFragmentManagerInstance().findFragmentById(R.id.navigation_drawer);
    if (mNavigationDrawerFragment != null) {
      return mNavigationDrawerFragment.mDrawerToggle;
    } else {
      return null;
    }
  }


  /**
   * Finishes multiselection mode started by ListFragment
   */
  public void finishActionMode() {
    ListFragment fragment = (ListFragment) getFragmentManagerInstance().findFragmentByTag(FRAGMENT_LIST_TAG);
    if (fragment != null) {
      fragment.finishActionMode();
    }
  }


  Toolbar getToolbar() {
    return binding.toolbar.toolbar;
  }


  private void handleIntents() {
    Intent intent = getIntent();

    if (intent.getAction() == null) {
      return;
    }

    boolean isRestartAppAction = ACTION_RESTART_APP.equals(intent.getAction());
    if (isRestartAppAction) {
      SystemHelper.restartApp(getApplicationContext(), MainActivity.class);
    }

    if (receivedIntent(intent)) {
      openNoteFromIntent(intent);
      return;
    }

    boolean isSendAndExitAction = ACTION_SEND_AND_EXIT.equals(intent.getAction());
    if (isSendAndExitAction) {
      saveAndExit(intent);
      return;
    }

    // Tag search
    if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getDataString()
            .startsWith(UrlCompleter.HASHTAG_SCHEME)) {
      switchToList();
      return;
    }

    // Home launcher shortcut widget
    if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
      Long id = Long.valueOf(Uri.parse(intent.getDataString()).getQueryParameter("id"));
      Note note = DbHelper.getInstance().getNote(id);
      if (note == null) {
        showMessage(R.string.note_doesnt_exist, ONStyle.ALERT);
        return;
      }
      switchToDetail(note);
      return;
    }

    // Home launcher "new note" shortcut widget
    boolean isShortcutWidgetAction = ACTION_SHORTCUT_WIDGET.equals(intent.getAction());
    if (isShortcutWidgetAction) {
      switchToDetail(new Note());
    }
  }


  private void openNoteFromIntent(Intent intent) {
    Note note = intent.getParcelableExtra(INTENT_NOTE);
    if (note == null) {
      note = DbHelper.getInstance().getNote(intent.getIntExtra(INTENT_KEY, 0));
    }
    // Checks if the same note is already opened to avoid to open again
    if (note != null && noteAlreadyOpened(note)) {
      return;
    }
    // Empty note instantiation
    if (note == null) {
      note = new Note();
    }
    switchToDetail(note);
  }

  /**
   * Used to perform a quick text-only note saving (eg. Tasker+Pushbullet)
   */
  private void saveAndExit(Intent intent) {
    Note note = new Note();
    note.setTitle(intent.getStringExtra(Intent.EXTRA_SUBJECT));
    note.setContent(intent.getStringExtra(Intent.EXTRA_TEXT));
    DbHelper.getInstance().updateNote(note, true);
    showToast(getString(R.string.note_updated), Toast.LENGTH_SHORT);
    finish();
  }


  private boolean receivedIntent(Intent intent) {
    return ACTION_SHORTCUT.equals(intent.getAction())
        || ACTION_NOTIFICATION_CLICK.equals(intent.getAction())
        || ACTION_WIDGET.equals(intent.getAction())
        || ACTION_WIDGET_TAKE_PHOTO.equals(intent.getAction())
        || ((Intent.ACTION_SEND.equals(intent.getAction())
        || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())
        || INTENT_GOOGLE_NOW.equals(intent.getAction()))
        && intent.getType() != null)
        || intent.getAction().contains(ACTION_NOTIFICATION_CLICK);
  }


  private boolean noteAlreadyOpened(Note note) {
    DetailFragment detailFragment = (DetailFragment) getFragmentManagerInstance().findFragmentByTag(
        FRAGMENT_DETAIL_TAG);
    boolean isSameNote = NotesHelper.haveSameId(note, detailFragment.getCurrentNote());
    return detailFragment != null && isSameNote;
  }


  public void switchToList() {
    FragmentTransaction transaction = getFragmentManagerInstance().beginTransaction();
    animateTransition(transaction, TRANSITION_HORIZONTAL);
    ListFragment mListFragment = new ListFragment();
    transaction.replace(R.id.fragment_container, mListFragment, FRAGMENT_LIST_TAG).addToBackStack
        (FRAGMENT_DETAIL_TAG).commitAllowingStateLoss();
    ActionBarDrawerToggle mActionBarDrawerToggle = getDrawerToggle();
    if (mActionBarDrawerToggle != null) {
      mActionBarDrawerToggle.setDrawerIndicatorEnabled(false);
    }
    EventBus.getDefault().post(new SwitchFragmentEvent(SwitchFragmentEvent.Direction.PARENT));
  }


  public void switchToDetail(Note note) {
    FragmentTransaction transaction = getFragmentManagerInstance().beginTransaction();
    animateTransition(transaction, TRANSITION_HORIZONTAL);
    DetailFragment mDetailFragment = new DetailFragment();
    setDetailFragmentArgument(mDetailFragment, note);
    DetailFragment currentDetailFragment =
            (DetailFragment) getFragmentManagerInstance().findFragmentByTag(FRAGMENT_DETAIL_TAG);
    if (currentDetailFragment == null) {
      transaction.replace(R.id.fragment_container, mDetailFragment, FRAGMENT_DETAIL_TAG)
          .addToBackStack(FRAGMENT_LIST_TAG)
          .commitAllowingStateLoss();
    } else {
      getFragmentManagerInstance().popBackStackImmediate();
      transaction.replace(R.id.fragment_container, mDetailFragment, FRAGMENT_DETAIL_TAG)
          .addToBackStack(FRAGMENT_DETAIL_TAG)
          .commitAllowingStateLoss();
    }
  }

  private void setDetailFragmentArgument(DetailFragment mDetailFragment, Note note) {
    Bundle b = new Bundle();
    b.putParcelable(INTENT_NOTE, note);
    mDetailFragment.setArguments(b);
  }


  /**
   * Notes sharing
   */
  public void shareNote(Note note) {

    String titleText = note.getTitle();

    String contentText = titleText
        + System.getProperty("line.separator")
        + note.getContent();

    Intent shareIntent = new Intent();
    List<Attachment> attachmentsList = note.getAttachmentsList();

    if (attachmentsList.isEmpty()) {
      shareIntentOnlyWithText(shareIntent);

    } else if (attachmentsList.size() == 1) {
      shareIntentWithSingleImageAttachment(shareIntent, attachmentsList);

    } else if (attachmentsList.size() > 1) {
      shareIntentWithMultipleImages(shareIntent, attachmentsList);
    }
    shareIntent.putExtra(Intent.EXTRA_SUBJECT, titleText);
    shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);

    startActivity(Intent
        .createChooser(shareIntent, getResources().getString(R.string.share_message_chooser)));
  }

  private void shareIntentWithMultipleImages(Intent shareIntent, List<Attachment> attachmentsList) {
    shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
    ArrayList<Uri> uris = getShareableUris(attachmentsList);

    // A check to decide the mime type of attachments to share is done here
    HashMap<String, Boolean> mimeTypes = getMimeTypes(attachmentsList);

    // If many mime types are present a general type is assigned to intent
    setShareIntentType(shareIntent, mimeTypes);

    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
  }

  private void shareIntentWithSingleImageAttachment(Intent shareIntent, List<Attachment> attachmentsList) {
    shareIntent.setAction(Intent.ACTION_SEND);
    Attachment attachment = attachmentsList.get(0);
    shareIntent.setType(attachment.getMime_type());
    shareIntent.putExtra(Intent.EXTRA_STREAM, FileProviderHelper.getShareableUri(attachment));
  }

  private void shareIntentOnlyWithText(Intent shareIntent) {
    shareIntent.setAction(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
  }

  private void setShareIntentType(Intent shareIntent, HashMap<String, Boolean> mimeTypes) {
    if (mimeTypes.size() > 1) {
      shareIntent.setType("*/*");
    } else {
      shareIntent.setType((String) mimeTypes.keySet().toArray()[0]);
    }
  }

  private ArrayList<Uri> getShareableUris(List<Attachment> attachmentsList) {
    ArrayList<Uri> uris = new ArrayList<>();
    for (Attachment attachment : attachmentsList) {
      uris.add(FileProviderHelper.getShareableUri(attachment));
    }
    return uris;
  }

  private HashMap<String, Boolean> getMimeTypes(List<Attachment> attachmentsList) {
    HashMap<String, Boolean> mimeTypes = new HashMap<>();
    for (Attachment attachment : attachmentsList) {
      mimeTypes.put(attachment.getMime_type(), true);
    }
    return mimeTypes;
  }


  /**
   * Single note permanent deletion
   *
   * @param note Note to be deleted
   */
  public void deleteNote(Note note) {
    new NoteProcessorDelete(Collections.singletonList(note)).process();
    BaseActivity.notifyAppWidgets(this);
    LogDelegate.debugLog("Deleted permanently note with ID '" + note.get_id() + "'");
  }


  public void updateWidgets() {
    new UpdateWidgetsTask(getApplicationContext())
        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }


  public void showMessage(int messageId, Style style) {
    showMessage(getString(messageId), style);
  }


  public void showMessage(String message, Style style) {
    // ViewGroup used to show Crouton keeping compatibility with the new Toolbar
    runOnUiThread(
        () -> Crouton.makeText(this, message, style, binding.croutonHandle.croutonHandle).show());
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    setPrefsChangedTrue();
  }

  private void setPrefsChangedTrue() {
    prefsChanged = true;
  }

  public void setPrefsChangedFalse() {
    prefsChanged = false;
  }

  public boolean getPrefsChanged() {
    return prefsChanged;
  }

  public Uri getSketchUri() { return sketchUri; }
  public void setSketchUri() { this.sketchUri = sketchUri; }
}
