package ee.mass.epm.sim.task;

import ee.mass.epm.sim.message.DeployMessageContent;
import ee.mass.epm.sim.message.SimMessageContent;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;

// TODO: add support for multiple resources at once
public class DeployMessageTask extends AbstractMessageTask {

    Expression resourcePath;

    //TODO : considering adding resource name as required Var

    @Override
    SimMessageContent getMessageContent(DelegateExecution execution) {

        DeployMessageContent messageContent = new DeployMessageContent();
        try {
            messageContent.resourcePath = getStringFromFieldExpression(execution, resourcePath);
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            return null;
        }

        return messageContent;
    }

}
