package ee.mass.epm.sim.message;


import ee.mass.epm.sim.task.MessageTask;
import org.flowable.engine.delegate.DelegateExecution;

import java.util.HashMap;
import java.util.Map;

/**
 * This message is forwarded to the BP engine for final consumption.
 */
public class EngineMessageContent implements SimMessageContent{

    public Map<String, Object> variables = new HashMap<>();
    public boolean isForStartEvent = false;
    public String destinationProcessInstanceId;

    @Override
    public SimMessageType getMessageContentType() {
        return SimMessageType.ENGINE_MSG;
    }

    public void addProcessVarsFromExecution(DelegateExecution execution, String[] varNames) {
        for (String varName : varNames) {
            variables.put(varName, execution.getVariable(varName));
        }
    }



}
