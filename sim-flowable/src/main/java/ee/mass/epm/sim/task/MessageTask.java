package ee.mass.epm.sim.task;

import ee.mass.epm.sim.message.EngineMessageContent;
import ee.mass.epm.sim.message.SimMessageContent;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;

// A message targeted to the Flowable Engine.
public class MessageTask extends AbstractMessageTask {

    static final String INCLUDED_PROCESS_VARS_DELIMITER = ",";

    Expression destinationProcessInstanceId;
    Expression includedProcessVars;


    @Override
    SimMessageContent getMessageContent(DelegateExecution execution) {
        // TODO: consider refactoring all of this logic under the MessageContent class itself
        EngineMessageContent msgContent = new EngineMessageContent();

        try {
            String destinationExecutionId = getStringFromFieldExpression(execution, destinationProcessInstanceId);
            if (destinationExecutionId != null){
                msgContent.destinationProcessInstanceId = destinationExecutionId;
            } else {
                throw new Exception("Destination process instance ID was null!" + execution.getId() );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        msgContent.variables.put(this.msgName + "_execution_id", execution.getId()); // TODO: maybe move to engine-middleware level instead of this task impl.

        if (includedProcessVars != null) {
            String expressionText = includedProcessVars.getExpressionText();
            String[] varNames = StringUtils.stripAll(expressionText.split(MessageTask.INCLUDED_PROCESS_VARS_DELIMITER));
            msgContent.addProcessVarsFromExecution(execution, varNames);
        }
        return msgContent;
    }


}
