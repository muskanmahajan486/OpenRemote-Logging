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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.ErrorManager;
import java.util.logging.FileHandler;
import java.util.logging.ConsoleHandler;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * An abstract implementation of default log service. This class provides some base implementation
 * to log facades that can be used for specific API implementations. It is intended to be extended
 * by an API facade that is geared for specific use cases ('regular' logging, audit logging,
 * administrative logging, alerts, etc.). <p>
 *
 * The idea behind multiple different logging facades is to reuse the extensive and highly
 * decoupled messaging infrastructure in many logging frameworks that allows easy filtering,
 * thresholding and channelling of messages to multiple sources and message consumers. Log facades
 * can also be used for various and specific convenience methods, such as log-only-once semantics. <p>
 *
 * This implementation attempts to minimize dependencies to standard Java SE runtime libraries
 * to make it reusable across as many environments as possible. Therefore specific log service
 * providers, such as log4j, can be optionally and dynamically loaded at runtime, reducing
 * compile-time hard coupling.
 *
 * @see org.openremote.logging.Logger
 *
 * @author <a href="mailto:juha@openremote.org">Juha Lindfors</a>
 */
public abstract class LogService
{

  // NOTE:
  //
  //  This implementation should remain agnostic of runtime environment, whether
  //  in embedded mode (Controller) or Java server-side mode (Beehive Services). It should
  //  remain limited in dependencies so it can be later investigated for use with Android
  //  runtime as well.
  //


  // TODO :
  //          - setDefaultParentHandlerUse
  //          - setDefaultCharset


  // Constants ------------------------------------------------------------------------------------

  /**
   * The top level root hierarchy used by all loggers. Every log category is always prepended
   * with this name: @{value}
   */
  public final static Hierarchy DEFAULT_ROOT_LOG_HIERARCHY = new InternalHierarchy("OpenRemote");

  /**
   * Convenience constant to be used with JUL to configure log output to standard error stream.
   */
  public final static String JUL_CONSOLE_HANDLER = "java.util.logging.ConsoleHandler";

  /**
   * Convenience constant to be used with JUL to configure the level (threshold) of standard error
   * stream log output.
   */
  public final static String JUL_CONSOLE_HANDLER_LEVEL = JUL_CONSOLE_HANDLER + ".level";

  /**
   * Convenience constant to be used with JUL to configure a formatter for standard error stream
   * log output.
   */
  public final static String JUL_CONSOLE_HANDLER_FORMATTER = JUL_CONSOLE_HANDLER + ".formatter";

  /**
   * Convenience constant to be used with JUL to configure root log outputs.
   */
  public final static String JUL_ROOT_HANDLERS = "handlers";

  /**
   * Convenience constant to be used with JUL to configure root log level.
   */
  public final static String JUL_ROOT_LEVEL = ".level";


  // Enums ----------------------------------------------------------------------------------------


  /**
   * Fixed log service provider implementations supported by this implementation. This enum
   * implementation provides a method to dynamically load the provider instances at runtime.
   */
  public enum ProviderType
  {
    /**
     * Default JUL based implementation.
     */
    JUL("org.openremote.logging.LogService$InternalLogger"),

    /**
     * JUL to log4j redirector implementation.
     */
    LOG4J("org.openremote.logging.log4j.Redirector");


    // Instance Fields ----------------------------------------------------------------------------

    /**
     * The fully qualified class name of the provider implementation. The class must contain
     * a Provider(Hierarchy) constructor.
     */
    private String providerClassName;



    // Constructors -------------------------------------------------------------------------------

    /**
     * Enum constructor with provider class name.
     *
     * @param providerClassName   fully qualified class name of provider implementation
     */
    private ProviderType(String providerClassName)
    {
      this.providerClassName  = providerClassName;
    }


    // Instance Methods ---------------------------------------------------------------------------

    /**
     * Dynamically loads the log provider implementation. The provider implementation must
     * contain an accessible constructor with signature Provider(Hierarchy).  <p>
     *
     * In case of any errors, will fall back to default internal JUL log provider.
     *
     * @param   hierarchy   the log hierarchy the provider is created for
     *
     * @return  new provider instance with a given log hierarchy
     */
    private Provider loadInstance(Hierarchy hierarchy) 
    {

      // IMPLEMENTATION NOTE: classloading may be necessary to externalize as it looks like
      //                      Dalvik VM on Android handles dynamic loading a bit differently.


      try
      {
        Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(providerClassName);

        Class<? extends Provider> cp = c.asSubclass(Provider.class);

        Constructor<? extends Provider> ctor = cp.getConstructor(Hierarchy.class);

        return ctor.newInstance(hierarchy);
      }

      catch (InvocationTargetException e)
      {
        errorManager.error(
            "The constructor of log service provider implementation '" + this.providerClassName +
            "' encountered an error (" + e.getMessage() + "). Defaulting to internal JUL " +
            "logger instead...", e, ErrorManager.OPEN_FAILURE
        );

        return new InternalLogger(hierarchy);
      }
      
      catch (NoSuchMethodException e)
      {
        errorManager.error(
            "The log service provider implementation '" + this.providerClassName +
            "' is missing mandatory constructor Provider(Hierarchy). Defaulting to internal " +
            "JUL logger instead...", e, ErrorManager.OPEN_FAILURE
        );

        return new InternalLogger(hierarchy);
      }

      catch (ClassCastException e)
      {
        errorManager.error(
            "The log service provider implementation '" + this.providerClassName +
            "' does not extend Provider class. Defaulting to internal JUL logger instead...",
            e, ErrorManager.OPEN_FAILURE
        );

        return new InternalLogger(hierarchy);
      }

      catch (ClassNotFoundException e)
      {
        errorManager.error(
            "The log service provider implementation '" + this.providerClassName +
            "' was not found in classpath. Defaulting to internal JUL logger instead...",
            e, ErrorManager.OPEN_FAILURE
        );

        return new InternalLogger(hierarchy);
      }

      catch (InstantiationException e)
      {
        errorManager.error(
            "The configured log service provider '" + this.providerClassName +
            "' cannot be instantiated: " + e.getMessage() + ". Defaulting to internal JUL " +
            "logger instead...", e, ErrorManager.OPEN_FAILURE
        );

        return new InternalLogger(hierarchy);
      }

      catch (IllegalAccessException e)
      {
        errorManager.error(
            "The configured log service provider '" + this.providerClassName +
            "' cannot be accessed: " + e.getMessage() + ". Defaulting to internal JUL " +
            "logger instead...", e, ErrorManager.OPEN_FAILURE
        );

        return new InternalLogger(hierarchy);
      }

      catch (ExceptionInInitializerError e)
      {
        errorManager.error(
            "Error initializing log service provider class '" + this.providerClassName +
            "': " + e.getMessage() + ". Defaulting to internal JUL logger instead...",
            null, ErrorManager.OPEN_FAILURE
        );

        return new InternalLogger(hierarchy);
      }

      catch (SecurityException e)
      {
        errorManager.error(
            "Security manager prevented instantiating log service provider class '" +
            this.providerClassName + "': " + e.getMessage() + ". Defaulting to internal JUL " +
            "logger instead...", e, ErrorManager.OPEN_FAILURE
        );

        return new InternalLogger(hierarchy);
      }
    }

  }



  // Class Members --------------------------------------------------------------------------------


  /**
   * An optional application log hierarchy. <p>
   *
   * Application-specific loggers can set an additional hierarchy prefix via
   * {@link #setDefaultApplicationLogHierarchy(String)} method. <p>
   *
   * This application log hierarchy name is appended to the {@link #DEFAULT_ROOT_LOG_HIERARCHY}
   * name, so for example value 'Beehive' will yield a log root hierarchy of 'OpenRemote.Beehive.*'.
   * <p>
   * The value should be set before any log instances are created. Changing this hierarchy value
   * will not impact pre-existing log instances, only new ones.
   */
  private static String defaultApplicationLogHierarchy = "";

  
  /**
   * Current default log service provider type. The logger instances will instantiate this
   * type of provider to delegate logging requests to.
   */
  private static ProviderType defaultProviderType = ProviderType.JUL;


  /**
   * For outputting errors that occur internally within the logging framework.
   */
  private final static ErrorManager errorManager = new ErrorManager();


  /**
   * This will set a default application level log hierarchy prefix for all loggers created
   * within the classloader scope. The given hierarchy name will be appended to the
   * {@link #DEFAULT_ROOT_LOG_HIERARCHY} value. <p>
   *
   * Setting default application log hierarchy should be set before creating any log instances.
   * Changing the hierarchy name will not impact existing and registered log instances, only
   * new ones.
   *
   * @param appLogHierarchyName   application log hierarchy name
   */
  protected static void setDefaultApplicationLogHierarchy(String appLogHierarchyName)
  {
    defaultApplicationLogHierarchy = appLogHierarchyName;
  }


  /**
   * This sets the default log service provider for the current deployment (scoped at classloader
   * level). Setting a default provider should be done before creating any instances of this
   * log service. Changing default provider after log service instances have been created will
   * not affect existing instances, only new ones.
   *
   * @param type    
   */
  public static void setDefaultProvider(ProviderType type)
  {
    // IMPLEMENTATION NOTE: if it ever becomes relevant to have ability to change providers
    //                      automatically and mid-flight, it would require a weak reference
    //                      map to track created log service instances.

    defaultProviderType = type;
  }



  // Instance Fields ------------------------------------------------------------------------------

  /**
   * This is the log framework provider all the logging actions eventually get delegated to.
   */
  protected Provider logDelegate;

  /**
   * The log hierarchy associated with this logger.
   */
  private InternalHierarchy hierarchy;

  /**
   * Log service provider type associated with this logger. If left null, the value from
   * {@link #defaultProviderType} is used.
   */
  private ProviderType providerType = null;

  

  // Constructors ---------------------------------------------------------------------------------


  /**
   * Constructs a new logger instance. The given log hierarchy is prefixed with root hierarchy
   * name {@link #DEFAULT_ROOT_LOG_HIERARCHY} and optionally application hierarchy name
   * {@link #defaultApplicationLogHierarchy} if set. <p>
   *
   * The logger is registered at creation so changes to the logger, such as provider implementation,
   * cannot be made after this instance has been constructed. For programmatic configuration
   * of loggers before registering them, use alternative constructor
   * {@link #LogService(Hierarchy, boolean)} and {@link #register} instead.
   *
   * @param hierarchy  a log category name
   */
  protected LogService(final Hierarchy hierarchy)
  {
    // IMPLEMENTATION NOTE: once a logger is registered, it cannot be unregistered or replaced
    //                      with another implementation (per API design in JUL and Log4j). So
    //                      changes that would require different provider implementation are
    //                      not possible after calling this constructor.

    this(hierarchy, true);
  }

  /**
   * Constructs a new logger instance. The given log hierarchy is prefixed with root hierarchy
   * name {@link #DEFAULT_ROOT_LOG_HIERARCHY} and optionally application hierarchy name
   * {@link #defaultApplicationLogHierarchy} if set. <p>
   *
   * Note that when the register parameter is set to false, the logger instance *must* be
   * registered via an explicit {@link #register} call before it can be used.
   *
   * @param hierarchy  a log category name
   */
  protected LogService(final Hierarchy hierarchy, boolean register)
  {
    InternalHierarchy fullHierarchy = new InternalHierarchy(
        DEFAULT_ROOT_LOG_HIERARCHY, defaultApplicationLogHierarchy, hierarchy
    );

    this.hierarchy = fullHierarchy;

    if (register)
    {
      registerLogger(fullHierarchy, defaultProviderType);
    }
  }



  // Public Instance Methods ----------------------------------------------------------------------

  /**
   * Registers this logger instance with log manager. This registration is mandatory when
   * a logger instance is created without registering it with the log manager first via
   * {@link #LogService(Hierarchy, boolean)} constructor. Typically this would occur when
   * log instances are created programmatically and they should be configured with settings
   * that can no longer be changed after registration (such as provider type).
   */
  public void register()
  {
    registerLogger(hierarchy, providerType);
  }

  /**
   * Returns the log hierarchy of this logger. Loggers are typically organized into a hierarchical
   * tree structure. The hierarchy instance is a type-safe representation of this logger's place
   * in the log tree hierarchy.
   *
   * @return this logger's hierarchy
   */
  public Hierarchy getHierarchy()
  {
    return hierarchy;
  }


  /**
   * TODO
   */
  public void setProvider(ProviderType providerType)
  {
    this.providerType = providerType;

    registerLogger(hierarchy, providerType);
  }

  /**
   * TODO
   *
   * @param applicationHierarchyName
   */
  public void setApplicationHierarchy(String applicationHierarchyName)
  {
    hierarchy.setApplicationHierarchy(applicationHierarchyName);
  }


  /**
   * This is a convenience method to attach a file-based log consumer to this log hierarchy.
   *
   * @param path
   */
  public void addFileLog(URI path)
  {
    logDelegate.addLogConsumer(
        ConsumerType.TEXT_FILE,
        new FileConfiguration(path)
    );
  }

  /**
   * This is a convenience method to attach a file-based log consumer to this log hierarchy.
   *
   * @param path
   * @param backupCount
   * @param fileSizeLimit
   * @param append
   */
  public void addFileLog(URI path, int backupCount, int fileSizeLimit, boolean append)
  {
    FileConfiguration config = new FileConfiguration(path, backupCount, fileSizeLimit, append);

    logDelegate.addLogConsumer(ConsumerType.TEXT_FILE, config);
  }

  public void configure(Properties properties)
  {
    logDelegate.loadConfiguration(properties);
  }

  public static void addRootConsoleOutput()
  {
    addRootConsoleOutput(Level.INFO);
  }

  public static void addRootConsoleOutput(Level level)
  {
    LogManager lm = LogManager.getLogManager();
    java.util.logging.Logger l = lm.getLogger("");

    Handler[] handlers = l.getHandlers();

    for (Handler h : handlers)
    {
      if (h instanceof ConsoleHandler)
      {
        l.removeHandler(h);
      }
    }

    l.addHandler(new StandardOutputHandler(level));
  }

  public void addConsoleOutput()
  {
    logDelegate.addLogConsumer(ConsumerType.STANDARD_OUTPUT, null);
  }

  public void addConsoleOutput(Level level)
  {
    ConsoleConfiguration config = new ConsoleConfiguration(level);

    logDelegate.addLogConsumer(ConsumerType.STANDARD_OUTPUT, config);
  }

  public void setLevel(Level level)
  {
    logDelegate.setLevel(level);
  }

  public Level getLevel()
  {
    return logDelegate.getLevel();
  }

  public static void loadConfiguration(Properties properties)
  {
    LogManager.getLogManager().reset();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    BufferedOutputStream bufOut = new BufferedOutputStream(out);

    try
    {
      properties.store(bufOut, null);

      ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());

      LogManager.getLogManager().readConfiguration(input);
    }

    catch (SecurityException e)
    {
      errorManager.error(
          "Security manager has denied loading of log configuration. Existing log configuration " +
          "has not been changed (" + e.getMessage() + ")", e, ErrorManager.OPEN_FAILURE
      );
    }
    catch (Exception e)
    {
      errorManager.error(
          "Failed to load log configuration : " + e.getMessage(),  e, ErrorManager.OPEN_FAILURE
      );
    }

  }


  private void registerLogger(Hierarchy hierarchy, ProviderType providerType)
  {
    if (providerType == null)
    {
      providerType = defaultProviderType;
    }

    LogManager logManager = LogManager.getLogManager();

    java.util.logging.Logger logger = logManager.getLogger(hierarchy.getCanonicalLogHierarchyName());

    if (logger == null)
    {
      Provider provider = providerType.loadInstance(hierarchy);

      logManager.addLogger(provider);

      logger = logManager.getLogger(hierarchy.getCanonicalLogHierarchyName());
    }

    if (!(logger instanceof Provider))
    {
      errorManager.error(
          "Expected Log Provider instance, got " + logger.getClass().getName(),
          null, ErrorManager.OPEN_FAILURE
      );

      throw new ClassCastException(
          "Cannot create log provider instance from " + logger.getClass().getName()
      );
    }

    else
    {
      logDelegate = (Provider)logger;
    }
  }




  // Nested Classes -------------------------------------------------------------------------------


  private static class InternalHierarchy implements Hierarchy
  {
    private String root = null;
    private String app  = null;
    private String internal = null;

    InternalHierarchy(String root)
    {
      this.root = root;
    }

    InternalHierarchy(Hierarchy root, String app, Hierarchy internal)
    {
      this.root = root.getCanonicalLogHierarchyName();
      this.app = app;
      this.internal = internal.getCanonicalLogHierarchyName();
    }
    
    public String getCanonicalLogHierarchyName()
    {
      StringBuilder builder = new StringBuilder();

      builder.append(root);

      if (app != null && !app.equals(""))
      {
        builder.append(".");

        builder.append(app);
      }

      if (internal != null && !internal.equals(""))
      {
        builder.append(".");

        builder.append(internal);
      }

      return builder.toString();
    }

    private void setApplicationHierarchy(String str)
    {
      this.app = str;
    }

    @Override public String toString()
    {
      return getCanonicalLogHierarchyName();
    }

    // TODO : implement equals
  }


  /**
   * Log service provider. Log framework service providers should extend this class.
   * The parent class is JUL logger class which should be extended as per the JUL logger contract.
   * The JUL log handlers should then delegate to alternative log framework implementation if
   * necessary. <p>
   *
   * This provider implementation includes some convenience methods to programmatically control
   * the underlying log framework service.
   */
  public abstract static class Provider extends java.util.logging.Logger
  {
    protected Hierarchy hierarchy;

    protected ErrorManager errorManager = LogService.errorManager;

    protected Provider(Hierarchy hierarchy)
    {
      super(hierarchy.getCanonicalLogHierarchyName(), null /* no localization */);

      this.hierarchy = hierarchy;
    }

    protected abstract void addLogConsumer(ConsumerType logType, ConsumerConfiguration config);

    protected abstract void loadConfiguration(Properties props);

  }

  /**
   * The default JUL provider.
   */
  private static class InternalLogger extends Provider
  {
    /**
     * Creates a JUL provider with the given hierarchy.
     *
     * @param hierarchy log hierarchy
     */
    public InternalLogger(Hierarchy hierarchy)
    {
      super(hierarchy);
    }

    // TODO : review security manager



    @Override public void loadConfiguration(Properties props)
    {
      LogManager.getLogManager().reset();

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      BufferedOutputStream bufOut = new BufferedOutputStream(out);

      try
      {
        props.store(bufOut, null);

        ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());

        LogManager.getLogManager().readConfiguration(input);
      }

      catch (SecurityException e)
      {
        errorManager.error(
            "Security manager has denied loading of log configuration. Existing log configuration " +
            "has not been changed (" + e.getMessage() + ")", e, ErrorManager.OPEN_FAILURE
        );
      }
      catch (Exception e)
      {
        errorManager.error(
            "Failed to load log configuration : " + e.getMessage(),  e, ErrorManager.OPEN_FAILURE
        );
      }
    }


    @Override public void addLogConsumer(ConsumerType logType, ConsumerConfiguration config)
    {
      switch (logType)
      {
        case TEXT_FILE:

          createTextFileLog(config);

          break;

        case STANDARD_OUTPUT:

          createStandardOutput(config);

        default:

          throw new Error("Unimplemented log consumer option " + logType);
      }
    }

    private void createStandardOutput(ConsumerConfiguration config)
    {
      super.addHandler(new StandardOutputHandler());
    }

    /**
     * Attaches a text file handler to this logger. The file logging is configured
     * with a {@link SingleLineFormatter} formatter and character encoding is set to
     * UTF-8.
     *
     * @param config
     *          a {@link FileConfiguration} instance which contains parameters for the
     *          file output (backup counts, size limits, etc.)
     */
    private void createTextFileLog(ConsumerConfiguration config)
    {
      ErrorManager errorManager = new ErrorManager();
      FileConfiguration fileConfig;

      if (!(config instanceof FileConfiguration))
      {
        fileConfig = new FileConfiguration(errorManager);
      }

      else
      {
        fileConfig = (FileConfiguration)config;
      }

      URI logFileId = fileConfig.getFileIdentifier();
      int backupCount = fileConfig.getFileBackupCount();
      int fileSizeLimit = fileConfig.getFileSizeLimit();
      boolean append = fileConfig.getFileAppendSetting();

      try
      {
        FileHandler fileLog = new FileHandler(
            new File(logFileId).toString() /*+ "-%g"*/, fileSizeLimit,
            backupCount, append
        );

        fileLog.setFormatter(new SingleLineFormatter());
        fileLog.setEncoding("UTF-8");

        addHandler(fileLog);
      }

      catch (SecurityException e)
      {
        errorManager.error(
            "Security manager has denied access to log file at location ''" + logFileId + "'' (" +
            e.getMessage() + "). Redirecting the log to system standard error stream instead.", e,
            ErrorManager.OPEN_FAILURE
        );

        addHandler(new ConsoleHandler());
      }

      catch (UnsupportedEncodingException e)
      {
        throw new Error(e.getMessage(), e); // TODO
      }

      catch (IOException e)
      {
        errorManager.error(
            "I/O Error in opening log file at location ''" + logFileId + "'' (" + e.getMessage() +
            "). Redirecting the log to system standard error stream instead.", e,
            ErrorManager.OPEN_FAILURE
        );

        addHandler(new ConsoleHandler());
      }

      catch (IllegalArgumentException e)
      {
        errorManager.error(
            "Failed to create log file configuration due to misconfigured file count " +
            "(count < 1) or file size limit (limit < 0) values: limit + " + fileSizeLimit +
            ", backup file count = " + backupCount + ". Redirecting the log to system" +
            " standard error stream instead.", e, ErrorManager.OPEN_FAILURE
        );

        addHandler(new ConsoleHandler());
      }
    }
  }


  private static class StandardOutputHandler extends StreamHandler
  {
    private StandardOutputHandler()
    {
      this(Level.INFO);
    }

    private StandardOutputHandler(Level threshold)
    {
      super(System.out, new SingleLineFormatter());

      setLevel(threshold);
    }
  }

  public enum ConsumerType
  {
    TEXT_FILE, XML_FILE, STANDARD_OUTPUT
  }

  public interface ConsumerConfiguration
  {

    void setProperty(String propertyName, String propertyValue);

    String getProperty(String propertyName);
  }


  /**
   * This log record formatter provides a typical server-side logging entries. Each
   * log record contains first a date and time, in UTC timezone and formatted as
   * [yyyy/MM/dd HH:mm:ss.S] where yyyy is the year as four digit value, MM is the
   * month as a two digit number, dd is the day of the month as a two digit number,
   * HH is the hour in 24 hour format, mm are the minutes of the hours, ss the seconds
   * of the minute and S are the milliseconds as a 3 digit number. <p>
   *
   * Exceptions and stack traces are printed in full, as multi-line entries.
   */
  public static class SingleLineFormatter extends Formatter
  {
    // Class Members ------------------------------------------------------------------------------

    private static TimeZone timezone = TimeZone.getTimeZone("UTC");
    private static DateFormat df = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss.S z]");

    // Static Initializers ------------------------------------------------------------------------

    static
    {
      // set the UTC timezone...

      df.setTimeZone(timezone);
    }


    // Implements Formatter -----------------------------------------------------------------------

    /**
     * Formats the log record. After the date/time string in [yyyy/mm/dd hh:mm:ss.mss UTC] format,
     * includes the log level of the given log record and the log message.
     *
     * @param record
     *            the log record to format
     *
     * @return
     */
    @Override public String format(LogRecord record)
    {
      try
      {
        StringBuilder builder = new StringBuilder(200);

        String logMessage = formatMessage(record.getMessage(), record.getParameters());

        builder.append(df.format(new Date(record.getMillis())));
        builder.append(" ");
        builder.append(record.getLevel());
        builder.append(": ");
        builder.append(logMessage);
        builder.append("\n");

        List<Throwable> exceptions = resolveNestedExceptions(record.getThrown());

        if (exceptions.isEmpty())
        {
          return builder.toString();
        }

        builder.append(printNestedExceptionsShortForm(exceptions));
        builder.append(printStackTraces(exceptions));

        return builder.toString();
      }

      catch (Throwable t)
      {
        return "Log Implementation Error: " + t.toString();
      }
    }

    // Private Instance Methods -------------------------------------------------------------------

    /**
     * Formats log records with message parameters. This implementation uses
     * {@link java.text.MessageFormat} style formatting.
     *
     * @param originalMessage
     *            the original logged message
     *
     * @param messageParams
     *            message parameters.
     *
     * @return    formatted log message
     */
    private String formatMessage(String originalMessage, Object... messageParams)
    {
      if (messageParams != null)
      {
        try
        {
          // MessageFormat has had performance issues in the past -- it also uses localized
          // formatting in some places where it is confusing (e.g. number separators). Not
          // replacing it for now, might do so later...
          //                                                                        [JPL]

          return MessageFormat.format(originalMessage, messageParams);
        }

        catch (Throwable t)
        {
          return "[FORMATTING ERROR] " + originalMessage;
        }
      }

      else
      {
        return originalMessage;
      }
    }

    /**
     * Helper method to print the message contained within the exception.
     *
     * @param t
     *          the exception of which message is printed
     *
     * @return  the content of the exception message, or {@code <no message>} string.
     *
     */
    private String printExceptionMessage(Throwable t)
    {
      if (t.getMessage() != null)
      {
        return t.getMessage() + "\n\n";
      }

      else
      {
        return "<no message>\n\n";
      }
    }

    /**
     * Orders nested exceptions into a list where the inner-most, original exception is at
     * the first index of the list.
     *
     * @param rootException
     *            the outer-most, wrapping exception
     *
     * @return    list of throwables that have been ordered in innermost-first order
     */
    private List<Throwable> resolveNestedExceptions(Throwable rootException)
    {
      if (rootException == null)
      {
        return Collections.emptyList();
      }

      List<Throwable> depthFirstExceptionList = new ArrayList<Throwable>(3);
      depthFirstExceptionList.add(rootException);

      Throwable nestedException = rootException.getCause();

      int count = 0;

      while (nestedException != null)
      {
        depthFirstExceptionList.add(0, nestedException);

        nestedException = nestedException.getCause();

        count++;

        // protect against circular references...

        if (count > 100)
        {
          break;
        }
      }

      return depthFirstExceptionList;
    }

    /**
     * Prints exception stack traces. This implementation differs slightly from the standard
     * Java stack trace prints in that it highlights the stack trace elements that originate
     * from OpenRemote packages.
     *
     * @param exceptions
     *            list of nested exception that have been ordered in innermost-first order
     *
     * @return
     *            multi-line string containing the full stack trace of the innermost, originating
     *            exception, and a single-line top of the stack for wrapping exceptions
     */
    private String printStackTraces(List<Throwable> exceptions)
    {
      StringBuilder builder = new StringBuilder(1000);

      String indent = "  ";
      builder.append("CALLSTACK: ");
      builder.append(exceptions.get(0).getClass().getSimpleName());
      builder.append("\n");

      boolean printFullStack = true;

      for (Throwable exception : exceptions)
      {
        StackTraceElement[] stack = exception.getStackTrace();

        if (printFullStack)
        {
          for (StackTraceElement element : stack)
          {
            builder.append(indent);

            String trace = element.toString();

            if (trace.startsWith("org.openremote"))
            {
              builder.append("-> ");
            }

            else
            {
              builder.append("     ");
            }

            builder.append(element.toString());
            builder.append("\n");
          }
        }

        else
        {
          builder.append("\n       ");
          builder.append("Wrapped by : ");
          builder.append(exception.getClass().getSimpleName());
          builder.append("\n");

          StackTraceElement[] wrappingStack = exception.getStackTrace();

          String element = wrappingStack[0].toString();

          builder.append("         ");

          if (element.startsWith("org.openremote"))
          {
            builder.append("-> ");
          }

          else
          {
            builder.append("     ");
          }

          builder.append(element);
          builder.append("\n");

          if (wrappingStack.length >= 3)
          {
            element = wrappingStack[1].toString();

            builder.append("         ");

            if (element.startsWith("org.openremote"))
            {
              builder.append("-> ");
            }

            else
            {
              builder.append("     ");
            }

            builder.append(element);
            builder.append("\n");

            element = wrappingStack[2].toString();

            builder.append("         ");

            if (element.startsWith("org.openremote"))
            {
              builder.append("-> ");
            }

            else
            {
              builder.append("     ");
            }

            builder.append(element);
            builder.append("\n");

            builder.append("         ...\n");
          }
        }

        indent = indent + "     ";
        printFullStack = false;
      }

      return builder.toString();
    }

    /**
     * Prints nested exceptions in a short form that emphasizes the exception message content
     * in the print output. This is different from the typical Java exception print outs in
     * that it attempts to make it easier to read the exception message, and visually separates
     * it from the call stack trace.
     *
     * @param exceptions
     *             list of nested exception that have been ordered in innermost-first order
     *
     * @return     multi-line string containing the exception detail messages, starting from
     *             the inner-most, originating exception, and ending with the outer-most
     *             wrapping exception that was caught.
     */
    private String printNestedExceptionsShortForm(List<Throwable> exceptions)
    {
      StringBuilder builder = new StringBuilder(1000);

      if (exceptions.size() == 0)
      {
        return "";
      }

      Throwable t = exceptions.get(0);

      if (exceptions.size() == 1)
      {
        builder.append("\nEXCEPTION: ");
        builder.append(t.getClass().getSimpleName());
        builder.append("\n  MESSAGE: ");
        builder.append(printExceptionMessage(t));
        builder.append("\n");

        return builder.toString();
      }

      builder.append("\nROOT EXCEPTION: ");
      builder.append(t.getClass().getSimpleName());
      builder.append("\n       MESSAGE: ");
      builder.append(printExceptionMessage(t));

      for (int listPosition = 1; listPosition < exceptions.size(); listPosition++)
      {
        Throwable wrappingException = exceptions.get(listPosition);

        builder.append("                Wrapped by: ");
        builder.append(wrappingException.getClass().getSimpleName());
        builder.append("\n                   Message: ");
        builder.append(printExceptionMessage(wrappingException));
      }

      return builder.toString();
    }
  }


  private static class ConsoleConfiguration implements ConsumerConfiguration
  {

    enum Name
    {
      LEVEL
    }

    private Map<String, String> properties = new HashMap<String, String>(1);


    private ConsoleConfiguration(Level level)
    {
      properties.put(Name.LEVEL.name(), level.getName());
    }


    private String getLevel()
    {
      return properties.get(Name.LEVEL.name());
    }

    public String getProperty(String name)
    {
      return properties.get(name);
    }

    public void setProperty(String name, String value)
    {
      properties.put(name, value);
    }

  }
  public static class FileConfiguration implements ConsumerConfiguration
  {

    private ErrorManager errorManager = new ErrorManager();
    private Map<String, String> properties = new HashMap<String, String>();

    public FileConfiguration(ErrorManager errorManager)
    {
      this.errorManager = errorManager;
    }

    public FileConfiguration(URI logFileId)
    {
      setProperty(Name.FILE_IDENTIFIER.name(), logFileId.toString());
    }

    public FileConfiguration(URI logFileId, Integer backupCount, Integer fileSizeLimit, Boolean append)
    {
      this(logFileId);

      setProperty(Name.FILE_APPEND.name(), append.toString());
      setProperty(Name.FILE_BACKUP_COUNT.name(), backupCount.toString());
      setProperty(Name.FILE_SIZE_LIMIT.name(), fileSizeLimit.toString());
    }

    private enum Name
    {
      FILE_IDENTIFIER, FILE_BACKUP_COUNT, FILE_SIZE_LIMIT, FILE_APPEND
    }

    public URI getFileIdentifier()
    {
      String fileLogValue = getProperty(Name.FILE_IDENTIFIER.name());

      try
      {
        return new URI(fileLogValue);
      }

      catch (URISyntaxException e)
      {
        try
        {
          URI tmpLogFileIdentifier = getTempLogFileIdentifier();

          errorManager.error(
              "ERROR IN FILE LOG CONFIGURATION -- File identifier ''" + fileLogValue +
              "'' cannot be parsed to a valid URI (" + e.getMessage() + "). Attempting to log " +
              "to ''" + tmpLogFileIdentifier + "''...", e, ErrorManager.FORMAT_FAILURE
          );

          return tmpLogFileIdentifier;
        }

        catch (URISyntaxException use)
        {
          throw new Error("Cannot create log file : " + use.getMessage(), use);
        }
      }
    }

    public int getFileBackupCount()
    {
      String value = getProperty(Name.FILE_BACKUP_COUNT.name());

      if (value == null)
      {
        return 1;
      }

      try
      {
        return new Integer(value);
      }

      catch (NumberFormatException e)
      {
        errorManager.error(
            "ERROR IN FILE LOG CONFIGURATION -- backup count value ''" + value + "'' " +
            "for file log ''" + getFileIdentifier() + "'' cannot be parsed to a valid integer. " +
            "Defaulting to value ''1''.", e, ErrorManager.FORMAT_FAILURE
        );

        return 1;
      }
    }

    public int getFileSizeLimit()
    {
      String value = getProperty(Name.FILE_SIZE_LIMIT.name());

      if (value == null)
      {
        return 10000000;
      }

      try
      {
        return new Integer(value);
      }

      catch (NumberFormatException e)
      {
        errorManager.error(
            "ERROR IN FILE LOG CONFIGURATION -- file size limit value ''" + value + "'' " +
            "for file log ''" + getFileIdentifier() + "'' cannot be parsed to a valid integer. " +
            "Defaulting to 10MB file limit size (10,000,000 bytes)", e, ErrorManager.FORMAT_FAILURE
        );

        return 10000000;
      }
    }

    public boolean getFileAppendSetting()
    {
      String value = getProperty(Name.FILE_APPEND.name());

      if (value == null || value.equalsIgnoreCase("false"))
      {
        return false;
      }

      if (value.equalsIgnoreCase("true"))
      {
        return true;
      }

      else
      {
        errorManager.error(
            "Unrecognized value for log file append setting: ''" + value + "''. Defaulting to false.",
           null, ErrorManager.OPEN_FAILURE
        );

        return false;
      }
    }

    public String getProperty(String name)
    {
      return properties.get(name);
    }

    public void setProperty(String name, String value)
    {
      properties.put(name, value);
    }



    private URI getTempLogFileIdentifier() throws URISyntaxException
    {
      try
      {
        // ----- BEGIN PRIVILEGED CODE BLOCK ------------------------------------------------------

        String tmpDir = AccessController.doPrivilegedWithCombiner(new PrivilegedAction<String>()
        {
          @Override public String run()
          {
            return System.getProperty("java.io.tempdir");
          }

        });

        String fileSeparator = AccessController.doPrivilegedWithCombiner(new PrivilegedAction<String>()
        {
          @Override public String run()
          {
            return System.getProperty("fileseparator");
          }

        });

        // ----- END PRIVILEGED CODE BLOCK --------------------------------------------------------

        return new URI(tmpDir + fileSeparator + "openremote.log");
      }

      catch (SecurityException e)
      {
        throw new URISyntaxException(
            "Security manager has denied access to system temporary directory. Cannot create " +
            "log file: " + e.getMessage(), ""
        );
      }

    }
  }



}

