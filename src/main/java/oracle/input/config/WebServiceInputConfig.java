package oracle.input.config;

import com.jayway.jsonpath.JsonPath;
import java.net.URL;
import java.util.Queue;

public class WebServiceInputConfig extends OracleInputConfig {

    private URL serviceUrl;
    private int servicePollingInterval;
    private Queue<JsonPath> valueQueries;

    /**
     * Initializes an instance of the WebServiceInputConfig class.
     *
     * @param valueType the input data type.
     * @param serviceUrl the web service url.
     * @param servicePollingInterval the time interval to poll data from the service in seconds.
     * @param valueQueries A queue of JSON path queries to execute on the raw input from the webservice to find the oracle input value.
     *                      The queries are executed in the order as they are polled from the queue.
     */
    public WebServiceInputConfig(ValueType valueType, URL serviceUrl, int servicePollingInterval, Queue<JsonPath> valueQueries) {
        super(valueType);
        this.serviceUrl = serviceUrl;
        this.servicePollingInterval = servicePollingInterval;
        this.valueQueries = valueQueries;
    }

    /**
     * Gets the web service url.
     * @return The web url.
     */
    public URL getServiceUrl() {
        return this.serviceUrl;
    }

    /**
     * Gets the time interval to poll data from the service.
     * @return the time interval in seconds.
     */
    public int getServicePollingInterval() {
        return this.servicePollingInterval;
    }

    /**
     * Gets a queue of JSON path queries to execute on the raw input from the webservice to find the oracle input value.
     * The queries are executed in the order as they are polled from the queue.
     * @return the JSONPath queries to execute.
     */
    public Queue<JsonPath> getValueQueries() {
        return this.valueQueries;
    }
}
