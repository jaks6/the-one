package ee.mass.epm.sim.task;

import ee.mass.epm.sim.SimMessage;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * This task should be used for messages which are meant to start new process instances
 */
public class StartEventMessageTask extends MessageTask {

    @Override
    SimMessage getSimMessage(DelegateExecution execution) {
        SimMessage message = super.getSimMessage(execution);
        message.isForStartEvent = true;
        return message;
    }
}
