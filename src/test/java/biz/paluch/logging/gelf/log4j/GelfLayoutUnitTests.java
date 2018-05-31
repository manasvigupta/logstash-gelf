package biz.paluch.logging.gelf.log4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import biz.paluch.logging.gelf.JsonUtil;
import biz.paluch.logging.gelf.LogMessageField;

/**
 * @author <a href="mailto:kai.geisselhardt@kaufland.com">Kai Geisselhardt</a>
 */
public class GelfLayoutUnitTests {

    private Logger logger;

    @BeforeAll
    public static void beforeClass() {
        DOMConfigurator.configure(GelfLayoutUnitTests.class.getResource("/log4j/log4j-gelf-layout.xml"));
    }

    @BeforeEach
    public void before() {
        TestAppender.clearLoggedLines();
        logger = Logger.getLogger(GelfLayoutUnitTests.class);
    }

    @Test
    public void test() {

        logger.info("test1");
        logger.info("test2");
        logger.info("test3");

        String[] loggedLines = TestAppender.getLoggedLines();
        assertThat(loggedLines.length).isEqualTo(3);
        assertThat(parseToJSONObject(loggedLines[0]).get("full_message")).isEqualTo("test1");
        assertThat(parseToJSONObject(loggedLines[1]).get("full_message")).isEqualTo("test2");
        assertThat(parseToJSONObject(loggedLines[2]).get("full_message")).isEqualTo("test3");
    }

    @Test
    public void testDefaults() throws Exception {

        NDC.push("ndc message");
        logger.info("test1");
        logger.info("test2");
        logger.info("test3");
        NDC.clear();

        Map<String, Object> message = getMessage();

        assertThat(message.get("version")).isNull();
        assertThat(message).containsEntry("full_message", "test1");
        assertThat(message).containsEntry("short_message", "test1");
        assertThat(message).containsEntry("NDC", "ndc message");
        assertThat(message).containsEntry("facility", "logstash-gelf");
        assertThat(message).containsEntry("level", "6");
        assertThat(message).containsEntry(LogMessageField.NamedLogField.SourceMethodName.name(), "testDefaults");
        assertThat(message).containsEntry(LogMessageField.NamedLogField.SourceClassName.name(), getClass().getName());
        assertThat(message).containsKeys("Thread", "timestamp", "MyTime");
    }

    @Test
    public void testConfiguration() throws Exception {
        logger = Logger.getLogger("biz.paluch.logging.gelf.log4j.configured");
        testCommonConfiguration(logger);
        Map<String, Object> message = getMessage();
        assertThat(message).containsEntry("LoggerName", "biz.paluch.logging.gelf.log4j.configured");
    }

    @Test
    public void testCallerLocationInfoIsDisabled() throws Exception {
        logger = Logger.getLogger("biz.paluch.logging.gelf.log4j.callerLocationDisabled");
        testCommonConfiguration(logger);
        Map<String, Object> message = getMessage();
        assertThat(message).containsEntry("LoggerName", "biz.paluch.logging.gelf.log4j.callerLocationDisabled");
        assertThat(message).doesNotContainKeys("SourceMethodName", "SourceClassName", "SourceLineNumber", "SourceSimpleClassName");
    }

    private void testCommonConfiguration(Logger logger) {
        MDC.put("mdcField1", "mdcValue1");
        NDC.push("ndc message");
        logger.info("test1");
        logger.info("test2");
        logger.info("test3");
        NDC.clear();

        Map<String, Object> message = getMessage();

        assertThat(message.get("version")).isNull();
        assertThat(message).containsEntry("full_message", "test1");
        assertThat(message).containsEntry("short_message", "test1");
        assertThat(message).containsEntry("NDC", "ndc message");
        assertThat(message).containsEntry("facility", "test");
        assertThat(message).containsEntry("level", "6");

        assertThat(message).containsEntry("fieldName1", "fieldValue1");
        
        if (Log4jUtil.isLog4jMDCAvailable()) {
            assertThat(message).containsEntry("mdcField1", "mdcValue1");
        }
        assertThat(message).containsKeys("timestamp", "MyTime");
    }

    public Map<String, Object> getMessage() {
        String s = TestAppender.getLoggedLines()[0];
        try {
            return (Map) JsonUtil.parseToMap(s);
        } catch (RuntimeException e) {
            System.out.println("Trying to parse: " + s);
            throw e;
        }
    }

    private Map<String, Object> parseToJSONObject(String value) {
        return JsonUtil.parseToMap(value);
    }
}