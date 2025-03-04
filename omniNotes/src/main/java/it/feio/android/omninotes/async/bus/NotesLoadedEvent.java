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

package it.feio.android.omninotes.async.bus;

import it.feio.android.omninotes.helpers.LogDelegate;
import it.feio.android.omninotes.models.Note;
import java.util.List;
import lombok.Getter;
import lombok.Setter;


public class NotesLoadedEvent {

  @Getter
  @Setter
  private List<Note> notes;

  public NotesLoadedEvent(List<Note> notes) {
    LogDelegate.debugLog(this.getClass().getName());
    this.notes = notes;
  }

  public NotesLoadedEvent() {

  }

  public List<Note> getNotes() {
    return notes;
  }
}
