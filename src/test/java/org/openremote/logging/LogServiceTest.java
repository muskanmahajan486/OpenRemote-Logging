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

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.Enumeration;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;


/**
 * Tests creation of domain-specific log facades and the abstract base implementation
 * of {@link LogService}.
 *
 * @author <a href="mailto:juha@openremote.org">Juha Lindfors</a>
 */
public class LogServiceTest
{


  // Test LifeCycle -------------------------------------------------------------------------------

  /**
   * Around each test method to ensure the default app log hierarchy is reset to default value.
   */
  @BeforeMethod @AfterMethod
  public void resetDefaultApplicationLogHierarchy()
  {
    LogService.setDefaultApplicationLogHierarchy("");
  }



  // Tests ----------------------------------------------------------------------------------------

  /**
   * Tests a no-op facade can be created and is initialized correctly.
   */
  @Test public void testLogFacadeImplementation()
  {
    TestFacade log = new TestFacade();

    Assert.assertTrue(log.logDelegate instanceof LogService.Provider);
  }


  /**
   * Test a simple custom log facade.
   */
  @Test public void testCustomLogFacadeAPI()
  {
    CustomLogService fc = new CustomLogService();

    Assert.assertTrue(fc.logDelegate instanceof LogService.Provider);

    fc.alert("danger!");

    TestLogHandler handler = new TestLogHandler();

    // Add a custom handler...

    fc.logDelegate.addHandler(handler);

    fc.alert("meltdown");

    handler.assertLastLog(Level.SEVERE, "meltdown");
  }


  /**
   * Tests that the root log hierarchy is correctly prefixed to the given log hierarchy name.
   */
  @Test public void testRootHierarchy()
  {
    CustomLogService fc = new CustomLogService();

    Assert.assertTrue(
      fc.getHierarchy().getCanonicalLogHierarchyName().startsWith(
      LogService.DEFAULT_ROOT_LOG_HIERARCHY.getCanonicalLogHierarchyName()),
      "Expected to start with '" + LogService.DEFAULT_ROOT_LOG_HIERARCHY.getCanonicalLogHierarchyName() +
      "', got '" + fc.getHierarchy().getCanonicalLogHierarchyName() + "'."
    );
  }

  /**
   * Tests that log hierarchy name is correctly formulated when no optional application log
   * category has been set.
   */
  @Test public void testHierarchy()
  {
    CustomLogService fc = new CustomLogService();

    Assert.assertTrue(
      fc.getHierarchy().getCanonicalLogHierarchyName().equals(
          LogService.DEFAULT_ROOT_LOG_HIERARCHY.getCanonicalLogHierarchyName() + ".custom"),
      "Expected '" + LogService.DEFAULT_ROOT_LOG_HIERARCHY.getCanonicalLogHierarchyName() + ".custom', got '" +
          fc.getHierarchy().getCanonicalLogHierarchyName() + "'"
    );
  }


  /**
   * Tests setting optional app hierarchy name (on an individual logger instance)
   */
  @Test public void testApplicationHierarchyPrefix()
  {
    CustomLogService cf = new CustomLogService(false);

    cf.setApplicationHierarchy("Foo");

    cf.register();

    Assert.assertTrue(cf.getHierarchy().getCanonicalLogHierarchyName().startsWith(
        LogService.DEFAULT_ROOT_LOG_HIERARCHY.getCanonicalLogHierarchyName())
    );

    Assert.assertTrue(cf.getHierarchy().getCanonicalLogHierarchyName().contains(".Foo."));

    Assert.assertTrue(
      cf.getHierarchy().getCanonicalLogHierarchyName().endsWith(".custom"),
      "Expected '" + LogService.DEFAULT_ROOT_LOG_HIERARCHY.getCanonicalLogHierarchyName() +
      ".Foo." + "custom', got '" + cf.getHierarchy().getCanonicalLogHierarchyName() + "'."
    );
  }


  /**
   * Test setting of default app hierarchy.
   */
  @Test public void testDefaultAppHierarchy()
  {
    LogService.setDefaultApplicationLogHierarchy("manna");

    CustomLogService cf = new CustomLogService();

    Assert.assertTrue(
        cf.getHierarchy().getCanonicalLogHierarchyName().equals(
        LogService.DEFAULT_ROOT_LOG_HIERARCHY + ".manna.custom"),
        "Got '" + cf.getHierarchy().getCanonicalLogHierarchyName() + "'"
    );
  }


  /**
   * Tests setting of default app hierarchy with null value.
   */
  @Test public void testDefaultAppHierarchyNullArg()
  {
    LogService.setDefaultApplicationLogHierarchy(null);

    CustomLogService cf = new CustomLogService();

    Assert.assertTrue(
      cf.getHierarchy().getCanonicalLogHierarchyName().equals(
      LogService.DEFAULT_ROOT_LOG_HIERARCHY + ".custom"),
      "Got '" + cf.getHierarchy().getCanonicalLogHierarchyName() + "'"
    );
  }


  /**
   * Basic check that logger gets registered on creation.
   */
  @Test public void testLogRegistration()
  {
    NamedLogService log = new NamedLogService("test-432251122");

    Enumeration<String> names = LogManager.getLogManager().getLoggerNames();

    while (names.hasMoreElements())
    {
      String name = names.nextElement();

      if (name.equals(LogService.DEFAULT_ROOT_LOG_HIERARCHY + ".test-432251122"))
      {
        return;
      }
    }

    Assert.fail("Did not find logger 'OpenRemote.test-432251122'");
  }

  /**
   * Basic check for non-registering constructor to make sure the registration only
   * happens when explicily requested
   */
  @Test public void testLogBeforeRegistration()
  {
    NamedLogService log = new NamedLogService("test-423552311", false);

    Enumeration<String> names = LogManager.getLogManager().getLoggerNames();

    while (names.hasMoreElements())
    {
      String name = names.nextElement();

      if (name.equals(LogService.DEFAULT_ROOT_LOG_HIERARCHY + ".test-423552311"))
      {
        Assert.fail("Should not have registered logger 'OpenRemote.test-423552311");
      }
    }

    log.register();

    names = LogManager.getLogManager().getLoggerNames();

    while (names.hasMoreElements())
    {
      String name = names.nextElement();

      if (name.equals(LogService.DEFAULT_ROOT_LOG_HIERARCHY + ".test-423552311"))
      {
        return;
      }
    }

    Assert.fail("Did not find logger 'OpenRemote.test-423552311'");
  }

  @Test public void testTextFileLogging() throws Exception
  {
    Logger log = new Logger(LogCategory.TEXT_FILE);

    // Don't delegate to parent handlers to avoid unneccessary
    // error logging caused by these tests...

    log.logDelegate.setUseParentHandlers(false);

    URI uri = File.createTempFile("openremote", null).toURI();

    log.addFileLog(uri);

    log.info("Write to file");

    BufferedReader reader = new BufferedReader(new FileReader(new File(uri)));

    String textLog = reader.readLine();

    Assert.assertTrue(
        textLog.contains("Write to file"),
        "Got log line '" + textLog + "'"
    );
  }


  @Test public void testTextFileLogFormatting() throws Exception
  {
    Logger log = new Logger(LogCategory.FORMAT_TEXT_FILE);

    // Don't delegate to parent handlers to avoid unneccessary
    // error logging caused by these tests...

    log.logDelegate.setUseParentHandlers(false);

    URI uri = File.createTempFile("openremote", null).toURI();

    log.addFileLog(uri);

    log.info("Write {0}", "something");

    BufferedReader reader = new BufferedReader(new FileReader(new File(uri)));

    String textLog = reader.readLine();

    Assert.assertTrue(
        textLog.contains("Write something"),
        "Got log line '" + textLog + "'"
    );
  }

  @Test public void testTextFileLogException() throws Exception
  {
    Logger log = new Logger(LogCategory.TEXT_FILE_EXCEPTION);

    // Don't delegate to parent handlers to avoid unneccessary
    // error logging caused by these tests...

    log.logDelegate.setUseParentHandlers(false);

    URI uri = File.createTempFile("openremote", null).toURI();

    log.addFileLog(uri);

    log.info("{0} {1} :", new LogFormatterTestException(), "exception", "is");

    BufferedReader reader = new BufferedReader(new FileReader(new File(uri)));
    StringWriter output = new StringWriter();
    BufferedWriter bout = new BufferedWriter(output);

    while (true)
    {
      String textLog = reader.readLine();

      if (textLog == null)
      {
        break;
      }

      bout.write(textLog);
    }

    bout.close();

    String content = output.getBuffer().toString();


    Assert.assertTrue(content.contains("exception is :"), content);
    Assert.assertTrue(content.contains("EXCEPTION: " + LogFormatterTestException.class.getSimpleName()));
    Assert.assertTrue(content.contains("MESSAGE: <no message>"));
    Assert.assertTrue(content.contains("CALLSTACK: " + LogFormatterTestException.class.getSimpleName()));
    Assert.assertTrue(content.contains("-> " + this.getClass().getName()));
  }

  @Test public void testTextFileLogNestedExceptions() throws Exception
  {
    Logger log = new Logger(LogCategory.TEXT_FILE_NESTED_EXCEPTION);

    // Don't delegate to parent handlers to avoid unneccessary
    // error logging caused by these tests...

    log.logDelegate.setUseParentHandlers(false);

    URI uri = File.createTempFile("openremote", null).toURI();

    log.addFileLog(uri);

    LogFormatterTestException t1 = new LogFormatterTestException();
    NullPointerException t2 = new NullPointerException("one two three");
    OutOfMemoryError t3 = new OutOfMemoryError("bang!");
    Exception e = new Exception("wrapping exception");
    Error err = new Error("wrapping error");

    t2.initCause(t1);
    t3.initCause(t2);
    e.initCause(t3);
    err.initCause(e);

    log.info("{0} {1} {2}:", err, "nested", "exception", "test");

    BufferedReader reader = new BufferedReader(new FileReader(new File(uri)));
    StringWriter output = new StringWriter();
    BufferedWriter bout = new BufferedWriter(output);

    while (true)
    {
      String textLog = reader.readLine();

      if (textLog == null)
      {
        break;
      }

      bout.write(textLog);
    }

    bout.close();

    String content = output.getBuffer().toString();

    Assert.assertTrue(content.contains("INFO: nested exception test:"), content);
    Assert.assertTrue(content.contains("ROOT EXCEPTION: " + LogFormatterTestException.class.getSimpleName()));
    Assert.assertTrue(content.contains("MESSAGE: <no message>"));
    Assert.assertTrue(content.contains("Wrapped by: " + NullPointerException.class.getSimpleName()));
    Assert.assertTrue(content.contains("Message: one two three"));
    Assert.assertTrue(content.contains("Wrapped by: " + OutOfMemoryError.class.getSimpleName()));
    Assert.assertTrue(content.contains("Message: bang!"));
    Assert.assertTrue(content.contains("Wrapped by: " + Exception.class.getSimpleName()));
    Assert.assertTrue(content.contains("Message: wrapping exception"));
    Assert.assertTrue(content.contains("Wrapped by: " + Error.class.getSimpleName()));
    Assert.assertTrue(content.contains("Message: wrapping error"));
    Assert.assertTrue(content.contains("CALLSTACK: " + LogFormatterTestException.class.getSimpleName()));
    Assert.assertTrue(content.contains("-> " + this.getClass().getName()));
  }

  @Test public void testTextFileLogConfiguration() throws Exception
  {
    Logger log = new Logger(LogCategory.TEXT_FILE_CONFIG);

    // Don't delegate to parent handlers to avoid unneccessary
    // error logging caused by these tests...

    log.logDelegate.setUseParentHandlers(false);

    URI uri = File.createTempFile("openremote", null).toURI();

    log.addFileLog(uri, 5, 20, false);

    log.error("test config", new LogFormatterTestException());
  }


  // Helper Classes -------------------------------------------------------------------------------

  /**
   * Minimal facade implementation that provides no API.
   */
  private static class TestFacade extends LogService
  {
    TestFacade()
    {
      super(LogCategory.TEST);
    }
  }


  /**
   * Simple alert API
   */
  private static class CustomLogService extends LogService
  {

    CustomLogService()
    {
      super(LogCategory.CUSTOM);

      // Don't delegate to parent handlers to avoid unneccessary
      // error logging caused by these tests...

      logDelegate.setUseParentHandlers(false);
    }

    CustomLogService(boolean noregister)
    {
      super(LogCategory.CUSTOM, false);
    }

    void alert(String msg)
    {
      logDelegate.severe(msg);
    }
  }

  private static class NamedLogService extends LogService
  {
    NamedLogService(final String name)
    {
      super(
          new Hierarchy()
          {
            @Override public String getCanonicalLogHierarchyName()
            {
              return name;
            }
          }
      );
    }

    NamedLogService(final String name, boolean register)
    {
      super(
          new Hierarchy()
          {
            @Override public String getCanonicalLogHierarchyName()
            {
              return name;
            }
          },

          register
      );
    }
  }


  enum LogCategory implements Hierarchy
  {
    CUSTOM("custom"),
    TEST("test"),
    TEXT_FILE("text.file"),
    FORMAT_TEXT_FILE("format.text.file"),
    TEXT_FILE_EXCEPTION(TEXT_FILE.getCanonicalLogHierarchyName() + ".exception"),
    TEXT_FILE_NESTED_EXCEPTION(TEXT_FILE.getCanonicalLogHierarchyName() + ".nested.exception"),
    TEXT_FILE_CONFIG(TEXT_FILE.getCanonicalLogHierarchyName() + ".config");


    String hierarchy;

    LogCategory(String str)
    {
      hierarchy = str;
    }

    @Override public String getCanonicalLogHierarchyName()
    {
      return hierarchy;
    }

    @Override public String toString()
    {
      return getCanonicalLogHierarchyName();
    }
  }

  /**
   * A test log handler that keeps track of last logged message and level and allows
   * assertions to be executed against this state.
   */
  private static class TestLogHandler extends Handler
  {
    private Level lastLevel;
    private String lastMessage;

    @Override public void publish(LogRecord record)
    {
      lastLevel = record.getLevel();
      lastMessage = record.getMessage();
    }

    @Override public void flush()
    {

    }

    @Override public void close()
    {

    }

    void assertLastLog(Level level, String msg)
    {
      Assert.assertTrue(
        msg.equals(lastMessage),
        "Expected log message '" + msg + "', got '" + lastMessage + "'."
      );

      Assert.assertTrue(
        level.equals(lastLevel),
        "Expected level " + level + ", got " + lastLevel
      );
    }
  }

  private static class LogFormatterTestException extends Exception
  {

  }

}

