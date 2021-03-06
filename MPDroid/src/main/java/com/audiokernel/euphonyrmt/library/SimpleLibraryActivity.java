/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.audiokernel.euphonyrmt.library;

import com.audiokernel.euphonyrmt.MPDroidActivities;
import com.audiokernel.euphonyrmt.R;
import com.audiokernel.euphonyrmt.fragments.AlbumsFragment;
import com.audiokernel.euphonyrmt.fragments.AlbumsGridFragment;
import com.audiokernel.euphonyrmt.fragments.BrowseFragment;
import com.audiokernel.euphonyrmt.fragments.FSFragment;
import com.audiokernel.euphonyrmt.fragments.LibraryFragment;
import com.audiokernel.euphonyrmt.fragments.SongsFragment;
import com.audiokernel.euphonyrmt.fragments.StreamsFragment;
import com.audiokernel.euphonyrmt.helpers.MPDControl;

import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.Artist;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class SimpleLibraryActivity extends MPDroidActivities.MPDroidActivity implements
        ILibraryFragmentActivity, OnBackStackChangedListener {

    private static final String EXTRA_ALBUM = "album";

    private static final String EXTRA_ARTIST = "artist";

    private static final String EXTRA_FOLDER = "folder";

    private static final String EXTRA_STREAM = "streams";

    private static final String TAG = "SimpleLibraryActivity";

    private TextView mTitleView;

    private String debugIntent(final Intent intent) {
        final ComponentName callingActivity = getCallingActivity();
        final StringBuilder stringBuilder = new StringBuilder();
        final Bundle extras = intent.getExtras();

        stringBuilder.append("SimpleLibraryActivity started with invalid extra");

        if (callingActivity != null) {
            stringBuilder.append(", calling activity: ");
            stringBuilder.append(callingActivity.getClassName());
        }

        if (extras != null) {
            for (final String what : extras.keySet()) {
                stringBuilder.append(", intent extra: ");
                stringBuilder.append(what);
            }
        }

        stringBuilder.append('.');
        return stringBuilder.toString();
    }

    private Fragment getRootFragment() {
        final Intent intent = getIntent();
        final Fragment rootFragment;

        if (intent.hasExtra(EXTRA_ALBUM)) {
            final Album album = intent.getParcelableExtra(EXTRA_ALBUM);

            rootFragment = new SongsFragment().init(album);
        } else if (intent.hasExtra(EXTRA_ARTIST)) {
            final Artist artist = intent.getParcelableExtra(EXTRA_ARTIST);
            final SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(mApp);

            if (settings.getBoolean(LibraryFragment.PREFERENCE_ALBUM_LIBRARY, true)) {
                rootFragment = new AlbumsGridFragment(artist);
            } else {
                rootFragment = new AlbumsFragment(artist);
            }
        } else if (intent.hasExtra(EXTRA_FOLDER)) {
            final String folder = intent.getStringExtra(EXTRA_FOLDER);

            rootFragment = new FSFragment().init(folder);
        } else if (intent.hasExtra(EXTRA_STREAM)) {
            rootFragment = new StreamsFragment();
        } else {
            throw new IllegalStateException(debugIntent(intent));
        }

        return rootFragment;
    }

    @Override
    public void onBackStackChanged() {
        refreshTitleFromCurrentFragment();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_tabs);

        final LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        mTitleView = (TextView) inflater.inflate(R.layout.actionbar_title, null);

        mTitleView.setFocusable(true);
        mTitleView.setFocusableInTouchMode(true);
        mTitleView.setSelected(true);
        mTitleView.requestFocus();

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setCustomView(mTitleView);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
        }

        if (savedInstanceState == null) {
            final Fragment rootFragment = getRootFragment();

            if (rootFragment == null) {
                throw new RuntimeException("Error : SimpleLibraryActivity root fragment is null");
            }

            if (rootFragment instanceof BrowseFragment) {
                setTitle(((BrowseFragment) rootFragment).getTitle());
            }
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.root_frame, rootFragment);
            ft.commit();
        } else {
            refreshTitleFromCurrentFragment();
        }
        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        final boolean result;

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // For onKeyLongPress to work
            event.startTracking();
            result = !mApp.isLocalAudible();
        } else {
            result = super.onKeyDown(keyCode, event);
        }

        return result;
    }

    @Override
    public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
        boolean result = true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                MPDControl.run(MPDControl.ACTION_NEXT);
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                MPDControl.run(MPDControl.ACTION_PREVIOUS);
                break;
            default:
                result = super.onKeyLongPress(keyCode, event);
                break;
        }

        return result;
    }

    @Override
    public boolean onKeyUp(final int keyCode, @NonNull final KeyEvent event) {
        boolean result = true;

        if (event.isTracking() && !event.isCanceled() && !mApp.isLocalAudible()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    MPDControl.run(MPDControl.ACTION_VOLUME_STEP_UP);
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    MPDControl.run(MPDControl.ACTION_VOLUME_STEP_DOWN);
                    break;
                default:
                    result = super.onKeyUp(keyCode, event);
                    break;
            }
        } else {
            result = super.onKeyUp(keyCode, event);
        }

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean result = false;

        if (item.getItemId() == android.R.id.home) {
            finish();
            result = true;
        }

        return result;
    }

    @Override
    public void pushLibraryFragment(final Fragment fragment, final String label) {
        final String title;
        if (fragment instanceof BrowseFragment) {
            title = ((BrowseFragment) fragment).getTitle();
        } else {
            title = fragment.toString();
        }
        setTitle(title);
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.replace(R.id.root_frame, fragment);
        ft.addToBackStack(label);
        ft.setBreadCrumbTitle(title);
        ft.commit();
    }

    private void refreshTitleFromCurrentFragment() {
        final FragmentManager supportFM = getSupportFragmentManager();
        final int fmStackCount = supportFM.getBackStackEntryCount();

        if (fmStackCount > 0) {
            setTitle(supportFM.getBackStackEntryAt(fmStackCount - 1).getBreadCrumbTitle());
        } else {
            final Fragment displayedFragment = getSupportFragmentManager().findFragmentById(
                    R.id.root_frame);
            if (displayedFragment instanceof BrowseFragment) {
                setTitle(((BrowseFragment) displayedFragment).getTitle());
            } else {
                setTitle(displayedFragment.toString());
            }
        }
    }

    @Override
    public void setTitle(final CharSequence title) {
        super.setTitle(title);
        mTitleView.setText(title);
    }

    @Override
    public void setTitle(final int titleId) {
        super.setTitle(titleId);
        mTitleView.setText(getString(titleId));
    }
}
