
package oracle.input.provider;

import com.jayway.jsonpath.JsonPath;

import java.util.Queue;

public class JSONPathHelper {

    public static String find(String source, Queue<JsonPath> queries) {

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
    }
}
