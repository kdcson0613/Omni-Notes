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
import lombok.Getter;
import lombok.Setter;

public class SwitchFragmentEvent {

    public SwitchFragmentEvent() {

    }

    public enum Direction {
    CHILDREN, PARENT
  }

  @Getter
  @Setter
  private Direction direction;

  public SwitchFragmentEvent(Direction direction) {
    LogDelegate.debugLog(this.getClass().getName());
    this.direction = direction;
  }

  public  Direction getDirection() {
      return this.direction;
  }
}
