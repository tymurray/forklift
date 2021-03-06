package forklift.retry;

import forklift.connectors.ForkliftConnectorI;
import forklift.connectors.ForkliftMessage;
import forklift.producers.ForkliftProducerI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class RetryRunnable implements Runnable {
    public static final Logger log = LoggerFactory.getLogger(RetryRunnable.class);

    private RetryMessage msg;
    private ForkliftConnectorI connector;

    public RetryRunnable(RetryMessage msg, ForkliftConnectorI connector) {
        this.msg = msg;
        this.connector = connector;
    }

    @Override
    public void run() {
        ForkliftProducerI producer = null;
        try {
        if (msg.getQueue() != null)
            producer = connector.getQueueProducer(msg.getQueue());
        else if (msg.getTopic() != null)
            producer = connector.getTopicProducer(msg.getTopic());
        } catch (Throwable e) {
            log.error("", e);
            e.printStackTrace();
            return;
        }

        log.info("Retrying {}", msg);
        try {
            producer.send(toForkliftMessage(msg));
        } catch (Exception e) {
            log.warn("Unable to resend msg", e);
            // TODO schedule this message to run again.
            return;
        }

        log.info("Cleaning up persistent file {}", msg.getPersistedPath());
        final File f = new File(msg.getPersistedPath());
        if (f.exists())
            f.delete();
    }

    private ForkliftMessage toForkliftMessage(RetryMessage msg) {
        final ForkliftMessage forkliftMsg = new ForkliftMessage();
        forkliftMsg.setHeaders(msg.getHeaders());
        forkliftMsg.setMsg(msg.getText());
        forkliftMsg.setProperties(msg.getProperties());
        return forkliftMsg;
    }
}
