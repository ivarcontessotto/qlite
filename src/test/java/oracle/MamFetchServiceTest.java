package oracle;

import integration.IntegrationTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MamFetchServiceTest {

    private final static Logger LOGGER = LogManager.getLogger(IntegrationTest.class);

    @Ignore // Manual Test
    @Test
    public void testListenToMamFetchService() {
        try (Socket socket = new Socket("127.0.0.1", 3000);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            in.lines().forEach(message -> {
                LOGGER.info("New Mesasge: " + message);
            });

        } catch (IOException e) {
            LOGGER.error("Error", e);
        }
    }
}
