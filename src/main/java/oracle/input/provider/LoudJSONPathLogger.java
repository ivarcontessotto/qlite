package oracle.input.provider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

public class LoudJSONPathLogger {

    public static void shutUp() {
        LoggerContext logContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger log = logContext.getLogger("com.jayway.jsonpath.internal.path.CompiledPath");
        log.setLevel(Level.INFO);
    }
}
