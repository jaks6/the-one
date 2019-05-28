package ee.mass.epm;

import ee.mass.epm.sim.SimMessage;
import ee.mass.epm.sim.SimulatedWorkQueue;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class SimulatedProcessEngineConfiguration extends StandaloneProcessEngineConfiguration {

    public SimulatedProcessEngineConfiguration() {
        simulatedWorkQueue =  new SimulatedWorkQueue(this);
        outgoingMessages = new LinkedList<>();
        fogMessageRequests = new HashSet<>();
    }


    protected SimulatedWorkQueue simulatedWorkQueue;
    private Queue<SimMessage> outgoingMessages;



    private Set<FogMessageRequest> fogMessageRequests;

    public SimulatedWorkQueue getSimulatedWorkQueue() {
        return simulatedWorkQueue;
    }

    public Queue<SimMessage> getOutgoingMessages() {
        return outgoingMessages;
    }

    public Set<FogMessageRequest> getFogMessageRequests() { return fogMessageRequests; }
}
