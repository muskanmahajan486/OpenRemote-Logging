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

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * This is a JUL based logging facade that provides convenience methods for common logging tasks.
 * The API provided is what most developers are used to seeing in a log interface, with
 * error, warning, info, etc. messages. <p>
 *
 * A notable difference in this facade is that it enforces type-safe log hierarchies to be used
 * instead of the more common convention of using developer-oriented full class name strings.
 * <p>
 *
 * For maximum portability this facade only depends on the JUL API. It allows for plugging in
 * logging redirectors where alternative log frameworks are desired. <p>
 *
 * @author <a href="mailto:juha@openremote.org">Juha Lindfors</a>
 */
public class Logger extends LogService
{

  // NOTE:
  //
  //  This implementation should remain agnostic regardless of runtime environment, whether
  //  in server-side code (Controller and Beehive Services), a GWT app or Android app.
  //



  /**
   * Returns a logger instance registered with a global java.util.logging LogManager.  <p>
   *
   * This instance provides additional convenience API on top of the default
   * java.util.logging.Logger API for common logging tasks.
   *
   * @param hierarchy   log hierarchy of the logger
   *
   * @return      A JUL Logger subclass with additional convenience API for logging.
   */
  public static Logger getInstance(Hierarchy hierarchy)
  {
    return new Logger(hierarchy);
  }



  // Constructors ---------------------------------------------------------------------------------


  /**
   * Constructs a new logger instance
   *
   * @param hierarchy  log hierarchy for the logger
   */
  protected Logger(Hierarchy hierarchy)
  {
    super(hierarchy);
  }



  // Public Instance Methods ----------------------------------------------------------------------


  // TODO
  //
  //   Java MessageFormat has in the past had poor performance. It may be relevant to replace
  //   it with something else for message formatting that has less of a performance impact.


  /**
   * Synonymous to using {@link java.util.logging.Logger#severe}.
   *
   * @param msg   log message
   */
  public void error(String msg)
  {
    super.logDelegate.log(Level.SEVERE, msg);
  }

  /**
   * Same as {@link #error} but allows parameterized log messages.
   *
   * @param msg     log message
   * @param params  log message parameters -- message parameterization must be compatible with
   *                {@link java.text.MessageFormat} API
   *
   * @see java.text.MessageFormat
   */
  public void error(String msg, Object... params)
  {
    super.logDelegate.log(Level.SEVERE, msg, params);
  }

  /**
   * Same as {@link #error} with an additional exception stack trace added to the logging record.
   *
   * @param msg         log message
   * @param throwable   exception or error associated with the log message
   */
  public void error(String msg, Throwable throwable)
  {
    super.logDelegate.log(Level.SEVERE, msg, throwable);
  }

  /**
   * Same as {@link #error} with an additional exception stack trace and message parameterization
   * added to the logging record.
   *
   * @param msg         log message
   * @param throwable   exception or error associated with the log message
   * @param params      log message parameters -- message parameterization must be compatible with
   *                    {@link java.text.MessageFormat} API
   *
   * @see java.text.MessageFormat
   */
  public void error(String msg, Throwable throwable, Object... params)
  {
    LogRecord record = new LogRecord(Level.SEVERE, msg);
    record.setThrown(throwable);
    record.setParameters(params);
    record.setLoggerName(super.logDelegate.getName());

    super.logDelegate.log(record);
  }


  // Warning Logging ------------------------------------------------------------------------------

  /**
   * Synonymous to using {@link java.util.logging.Logger#warning}.
   *
   * @param msg   log message
   */
  public void warn(String msg)
  {
    super.logDelegate.log(Level.WARNING, msg);
  }


  /**
   * Same as {@link #warn} but allows parameterized log messages.
   *
   * @param msg     log message
   * @param params  log message parameters -- message parameterization must be compatible with
   *                {@link java.text.MessageFormat} API
   *
   * @see java.text.MessageFormat
   */
  public void warn(String msg, Object... params)
  {
    super.logDelegate.log(Level.WARNING, msg, params);
  }


  /**
   * Same as {@link #warn} with an additional exception stack trace added to the logging record.
   *
   * @param msg         log message
   * @param throwable   exception or error associated with the log message
   */
  public void warn(String msg, Throwable throwable)
  {
    super.logDelegate.log(Level.WARNING, msg, throwable);
  }


  /**
   * Same as {@link #warn} with an additional exception stack trace and message parameterization
   * added to the logging record.
   *
   * @param msg         log message
   * @param throwable   exception or error associated with the log message
   * @param params      log message parameters -- message parameterization must be compatible with
   *                    {@link java.text.MessageFormat} API
   *
   * @see java.text.MessageFormat
   */
  public void warn(String msg, Throwable throwable, Object... params)
  {
    LogRecord record = new LogRecord(Level.WARNING, msg);
    record.setThrown(throwable);
    record.setParameters(params);
    record.setLoggerName(super.logDelegate.getName());

    super.logDelegate.log(record);
  }


  // Info Logging ---------------------------------------------------------------------------------


  /**
   * Synonymous to using {@link java.util.logging.Logger#info(String)}.
   *
   * @param msg   log message
   */
  public void info(String msg)
  {
    super.logDelegate.log(Level.INFO, msg);
  }

  /**
   * Same as {@link #info} but allows parameterized log messages.
   *
   * @param msg     log message
   * @param params  log message parameters -- message parameterization must be compatible with
   *                {@link java.text.MessageFormat} API
   *
   * @see java.text.MessageFormat
   */
  public void info(String msg, Object... params)
  {
    super.logDelegate.log(Level.INFO, msg, params);
  }

  /**
   * Same as {@link #info} with an additional exception stack trace added to the logging record.
   *
   * @param msg         log message
   * @param throwable   exception or error associated with the log message
   */
  public void info(String msg, Throwable throwable)
  {
    super.logDelegate.log(Level.INFO, msg, throwable);
  }

  /**
   * Same as {@link #info} with an additional exception stack trace and message parameterization
   * added to the logging record.
   *
   * @param msg         log message
   * @param throwable   exception or error associated with the log message
   * @param params      log message parameters -- message parameterization must be compatible with
   *                    {@link java.text.MessageFormat} API
   *
   * @see java.text.MessageFormat
   */
  public void info(String msg, Throwable throwable, Object... params)
  {
    LogRecord record = new LogRecord(Level.INFO, msg);
    record.setThrown(throwable);
    record.setParameters(params);
    record.setLoggerName(super.logDelegate.getName());

    super.logDelegate.log(record);
  }


  // Debug Logging --------------------------------------------------------------------------------

  /**
   * Synonymous to using {@link java.util.logging.Logger#fine}.
   *
   * @param msg   log message
   */
  public void debug(String msg)
  {
    super.logDelegate.log(Level.FINE, msg);
  }

  /**
   * Same as {@link #debug} but allows parameterized log messages.
   *
   * @param msg     log message
   * @param params  log message parameters -- message parameterization must be compatible with
   *                {@link java.text.MessageFormat} API
   *
   * @see java.text.MessageFormat
   */
  public void debug(String msg, Object... params)
  {
    super.logDelegate.log(Level.FINE, msg, params);
  }

  /**
   * Same as {@link #debug} with an additional exception stack trace added to the logging record.
   *
   * @param msg         log message
   * @param throwable   exception or error associated with the log message
   */
  public void debug(String msg, Throwable throwable)
  {
    super.logDelegate.log(Level.FINE, msg, throwable);
  }

  /**
   * Same as {@link #debug} with an additional exception stack trace and message parameterization
   * added to the logging record.
   *
   * @param msg         log message
   * @param throwable   exception or error associated with the log message
   * @param params      log message parameters -- message parameterization must be compatible with
   *                    {@link java.text.MessageFormat} API
   *
   * @see java.text.MessageFormat
   */
  public void debug(String msg, Throwable throwable, Object... params)
  {
    LogRecord record = new LogRecord(Level.FINE, msg);
    record.setThrown(throwable);
    record.setParameters(params);
    record.setLoggerName(super.logDelegate.getName());

    super.logDelegate.log(record);
  }


  // Trace Logging --------------------------------------------------------------------------------

  /**
   * Synonymous to using {@link java.util.logging.Logger#finer}.
   *
   * @param msg   log message
   */
  public void trace(String msg)
  {
    super.logDelegate.log(Level.FINER, msg);
  }

  /**
   * Same as {@link #trace} but allows parameterized log messages.
   *
   * @param msg     log message
   * @param params  log message parameters -- message parameterization must be compatible with
   *                {@link java.text.MessageFormat} API
   *
   * @see java.text.MessageFormat
   */
  public void trace(String msg, Object... params)
  {
    super.logDelegate.log(Level.FINER, msg, params);
  }

  /**
   * Same as {@link #trace} with an additional exception stack trace added to the logging record.
   *
   * @param msg         log message
   * @param throwable   exception or error associated with the log message
   */
  public void trace(String msg, Throwable throwable)
  {
    super.logDelegate.log(Level.FINER, msg, throwable);
  }

  /**
   * Same as {@link #trace} with an additional exception stack trace and message parameterization
   * added to the logging record.
   *
   * @param msg         log message
   * @param throwable   exception or error associated with the log message
   * @param params      log message parameters -- message parameterization must be compatible with
   *                    {@link java.text.MessageFormat} API
   *
   * @see java.text.MessageFormat
   */
  public void trace(String msg, Throwable throwable, Object... params)
  {
    LogRecord record = new LogRecord(Level.FINER, msg);
    record.setThrown(throwable);
    record.setParameters(params);
    record.setLoggerName(super.logDelegate.getName());

    super.logDelegate.log(record);
  }

}

