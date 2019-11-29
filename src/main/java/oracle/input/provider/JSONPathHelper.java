
package oracle.input.provider;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Queue;

public class JSONPathHelper {

    private static Logger LOGGER = LogManager.getLogger(JSONPathHelper.class);

    public static String find(String source, Queue<JsonPath> queries) {

        try {

            Object value = null;
            JsonPath query = queries.poll();

            if (query != null) {
                value = query.read(source);
                query = queries.poll();
            }

            while (value != null && query != null) {
                value = query.read(value);
                query = queries.poll();
            }

            if (value != null) {
                return String.valueOf(value);
            }
            return null;

        } catch (PathNotFoundException e) {
            LOGGER.debug(e.getMessage());
            return null;
        }
    }
}
