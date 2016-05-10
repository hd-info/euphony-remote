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

package com.audiokernel.euphonyrmt.models;

import org.a0z.mpd.item.Music;

public abstract class AbstractPlaylistMusic extends Music {

    private int mCurrentSongIconRefID;

    private boolean mForceCoverRefresh = false;

    protected AbstractPlaylistMusic(final Music music) {
        super(music);
    }

    public int getCurrentSongIconRefID() {
        return mCurrentSongIconRefID;
    }

    public abstract String getPlayListMainLine();

    public abstract String getPlaylistSubLine();

    public boolean isForceCoverRefresh() {
        return mForceCoverRefresh;
    }

    public void setCurrentSongIconRefID(final int currentSongIconRefID) {
        mCurrentSongIconRefID = currentSongIconRefID;
    }

    public void setForceCoverRefresh(final boolean forceCoverRefresh) {
        mForceCoverRefresh = forceCoverRefresh;
    }
}
