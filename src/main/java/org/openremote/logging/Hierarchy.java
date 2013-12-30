/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2014, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.logging;

/**
 * Defines a logging hierarchy. An instance of this interface must implement and return a canonical
 * dot-separated hierarchy string (the form used by Java Util Logging and Log4j frameworks). <p>
 *
 * The hierarchy type can be used to create type-safe log hierarchy constants instead of
 * String based log category names.
 *
 * @author <a href="mailto:juha@openremote.org">Juha Lindfors</a>
 */
public interface Hierarchy
{
  /**
   * Returns a canonical dot-separated hierarchy string.
   *
   * @return    log hierarchy as dot-separated string (see Java Util Logging and/or Log4j
   *            documentation for details).
   */
  String getCanonicalLogHierarchyName();
}

