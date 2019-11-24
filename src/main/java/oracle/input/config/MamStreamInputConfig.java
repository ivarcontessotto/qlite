package oracle.input.config;

import com.jayway.jsonpath.JsonPath;

import java.util.Queue;

public class MamStreamInputConfig extends OracleInputConfig {

    private int servicePort;
    private int servicePollingInterval;
    private String nodeProviderUrl;
    private String rootAddress;
    private Queue<JsonPath> valueQueries;

    /**
     * Initializes an instance of the MamStreamInputConfig class.
     *  @param valueType the input data type.
     * @param servicePort the localhost port where the MAM fetch service publishes the retrieved MAM messages.
     * @param servicePollingInterval the time interval in which the MAM fetch service should poll from the MAM stream in seconds.
     * @param nodeProviderUrl the IOTA node provider URL.
     * @param rootAddress a MAM stream root address from where the fetching service should start reading messages.
     * @param valueQueries A queue of JSON path queries to execute on the raw input from the webservice to find the oracle input value.
     *                      The queries are executed in the order as they are polled from the queue.
     */
    public MamStreamInputConfig(
            ValueType valueType,
            int servicePort,
            int servicePollingInterval,
            String nodeProviderUrl,
            String rootAddress,
            Queue<JsonPath> valueQueries) {
        super(valueType);
        this.servicePort = servicePort;
        this.servicePollingInterval = servicePollingInterval;
        this.nodeProviderUrl = nodeProviderUrl;
        this.rootAddress = rootAddress;
        this.valueQueries = valueQueries;
    }

    /**
     * Gets the localhost listening port of the Mam fetch service.
     * @return The port number.
     */
    public int getServicePort() {
        return this.servicePort;
    }

    /**
     * Gets the time interval in which the MAM fetch service should poll from the MAM stream.
     * @return The time interval in seconds.
     */
    public int getServicePollingInterval() {
        return this.servicePollingInterval;
    }

    /**
     * Gets the IOTA node provider URL.
     * For example: https://nodes.devnet.iota.org:443
     * @return The provider URL.
     */
    public String getNodeProviderUrl() {
        return this.nodeProviderUrl;
    }

    /**
     * Gets a MAM stream root address from where the fetching service should start reading messages.
     * @return The MAM stream root address.
     */
    public String getRootAddress() {
        return this.rootAddress;
    }

    /**
     * Gets a queue of JSON path queries to execute on the raw input from the MAM fetch service to find the oracle input value.
     * The queries are executed in the order as they are polled from the queue.
     * @return the JSONPath queries to execute.
     */
    public Queue<JsonPath> getValueQueries() {
        return this.valueQueries;
    }
}
