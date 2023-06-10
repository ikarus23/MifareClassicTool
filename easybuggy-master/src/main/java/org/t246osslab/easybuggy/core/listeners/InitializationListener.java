package org.t246osslab.easybuggy.core.listeners;

import java.io.OutputStream;
import java.io.PrintStream;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.owasp.esapi.ESAPI;
import org.t246osslab.easybuggy.core.utils.Closer;

@WebListener
public class InitializationListener implements ServletContextListener {
    public void contextInitialized(ServletContextEvent event) {

        /*
         * Suppress noisy messages output by the ESAPI library. For more detail:
         * https://stackoverflow.com/questions/45857064/how-to-suppress-messages-output-by-esapi-library
         */
        PrintStream printStream = null;
        OutputStream outputStream = null;
        PrintStream original = System.out;
        try {
            outputStream = new OutputStream() {
                public void write(int b) {
                    // Do nothing
                }
            };
            printStream = new PrintStream(outputStream);
            System.setOut(printStream);
            System.setErr(printStream);
            ESAPI.encoder();
        } catch (Exception e) {
            // Do nothing
        } finally {
            System.setOut(original);
            Closer.close(printStream, outputStream);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Do nothing
    }
}
