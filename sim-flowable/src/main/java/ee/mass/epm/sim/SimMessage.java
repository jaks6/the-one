package ee.mass.epm.sim;

import java.util.Map;

public class SimMessage {

    public boolean isForStartEvent = false;
    public String name;
    public String destinationExecutionid;
    public Map<String, Object> variables;

    // for one sim outgoing messages
    public int destinationAddress;
    public int size;
    public String srcExecutionId;
    public boolean notifySourceOfDelivery; //whether the sending task should be notified on

    @Override
    public String toString() {
        return String.format("MSG: [%s; isStart: %s; dest: %s]", name, isForStartEvent, destinationAddress);
    }
}
