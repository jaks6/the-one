package applications.bpm;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import ee.mass.epm.SimulationApplicationEngine;
import ee.mass.epm.sim.message.SimMessage;

/** TODO: consider if there is too much overhead if every node
 *  has its own listener as it is currently implemented.
 *
 * Right now, every node is notified of every message transfer in the world.
 */
public class BpmMessageListener implements MessageListener {


    private final SimulationApplicationEngine engine;
    private final DTNHost host;

    public BpmMessageListener(DTNHost host, SimulationApplicationEngine engine) {
        this.host = host;
        this.engine = engine;
    }

    /** notifySourceOfDelivery is assigned in bpmn using flowable:istriggerable */
    public void messageTransferred(Message msg, DTNHost from, DTNHost to, boolean firstDelivery) {
        if (from == host){
            SimMessage simMsg = (SimMessage) msg.getProperty(BpmEngineApplication.PROPERTY_PROCESS_MSG);
            if (simMsg != null && simMsg.notifySourceOfDelivery){

                engine.notifyMessageTransferred(simMsg);
                host.deleteMessage(msg.getId(), false); //TODO: this may cause problems with routing/relaying
            }
        }
    }
    // not used
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
    public void messageDeleted(Message m, DTNHost where, boolean dropped) { }
    public void newMessage(Message m) { }
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) { }



}
