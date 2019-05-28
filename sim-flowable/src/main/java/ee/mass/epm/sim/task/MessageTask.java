package ee.mass.epm.sim.task;

import ee.mass.epm.SimulatedProcessEngineConfiguration;
import ee.mass.epm.sim.SimMessage;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class MessageTask implements JavaDelegate {

    private static final String INCLUDED_PROCESS_VARS_DELIMITER = ",";
    Expression messageName;
    Expression msgSize;
    Expression msgSizeVariable;
    Expression includedProcessVars;
    Expression destinationAddressVariable;
    Expression destinationAddress;

    Logger log = LoggerFactory.getLogger(this.getClass());


    @Override
    public void execute(DelegateExecution execution) {

        SimulatedProcessEngineConfiguration engine = (SimulatedProcessEngineConfiguration) Context.getProcessEngineConfiguration();
//        System.out.println("started execution = [" + execution + "; "+ execution.getCurrentFlowElement().getName() +  " engine: "+ engine+"]" );

        SimMessage msg = getSimMessage(execution);
        engine.getOutgoingMessages().add(msg);
    }

    SimMessage getSimMessage(DelegateExecution execution) {
        SimMessage msg = new SimMessage();

        if (destinationAddress == null && destinationAddressVariable == null){
            log.error("Both destinationAddress and destinationAddressVariable were null (undefined!");
            return null;
        }
        int dest = -1;
        if (destinationAddress != null){
            dest = Integer.valueOf(destinationAddress.getExpressionText());
        }
        if (destinationAddressVariable != null){
            if (destinationAddress == null){
                log.debug("set dest from process variable");
                dest = (int) execution.getVariable(destinationAddressVariable.getExpressionText());
            } else {
                log.info("Using extensionField 'destinationAddress' instead of 'destinationAddressVariable', as both have been set");
            }
        }

        msg.destinationAddress = dest;
        msg.name = (String) messageName.getValue(execution);
        msg.srcExecutionId = execution.getId();

        /** The destinationExecutionid of message source is attached to msg as a process var.
         *  If in current process the src_execution_id is defined, use that as the destination
         *  of this message. TODO: make naming more clear
         */
        if (execution.hasVariable("dest_execution_id")) {
            msg.destinationExecutionid = (String) execution.getVariable("dest_execution_id");
        } else if (execution.hasVariable("last_msg_src_execution_id")) {
            msg.destinationExecutionid = (String) execution.getVariable("last_msg_src_execution_id");
        }

        msg.variables = new HashMap<>();
        msg.variables.put("last_msg_src_execution_id", execution.getId()); // TODO: maybe move to engine-middleware level instead of this task impl.


        if (includedProcessVars != null){
            String[] varNames = includedProcessVars.getExpressionText().split(INCLUDED_PROCESS_VARS_DELIMITER);
            for (String varName : varNames) {
                msg.variables.put(varName, execution.getVariable(varName));
            }
        }

        //if the task has attribute flowable:triggerable, the task doesnt finish until a trigger is sent to this task
        boolean triggerable = ((ServiceTask) execution.getCurrentFlowElement()).isTriggerable();
        msg.notifySourceOfDelivery = triggerable;


        int msgsize = 1;
        if (msgSize != null){
            msgsize = Integer.valueOf(msgSize.getExpressionText());
        }
        if (msgSizeVariable != null){
            if (msgSize == null){
                log.debug("set msgSize from process variable");
                msgsize = Integer.parseInt((String) execution.getVariable(msgSizeVariable.getExpressionText()));
            } else {
                log.info("Using extensionField 'msgSize' instead of 'msgSizeVariable', as both have been set");
            }
        }
        msg.size = msgsize;

        return msg;
    }
}
