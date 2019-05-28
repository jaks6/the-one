package applications.bpm;

import core.ConnectionListener;
import core.DTNHost;
import core.DTNSim;
import core.SimScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class NewConnectionListener implements ConnectionListener {
    private static NewConnectionListener instance;
    private static Logger log = LoggerFactory.getLogger(NewConnectionListener.class);

    //TODO: should we replace this with distance  / movement listener, instead of connection, so that radios can be turned off


    static {
        DTNSim.registerForReset(NewConnectionListener.class.getCanonicalName());
        reset();
    }

    public static void reset() {
        instance = null;
    }

    public static NewConnectionListener getInstance() {
        if (instance == null) {
            instance = new NewConnectionListener();
            log.debug("Attaching NewConnectionListener to Simulation Scenario!");
            SimScenario.getInstance().addConnectionListener(instance);
        }
        return instance;
    }


    public NewConnectionListener() {
    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        HashMap<String, Object> processVars = new HashMap<String, Object>();

        // Send Signal to host1
        processVars.put("sig_remote_address", host2.getAddress());
        signalConnected(host1, BpmEngineApplication.SIGNAL_NEARBY_DEVICE, processVars);

        // Send Signal to host2
        processVars.put("sig_remote_address", host1.getAddress());
        signalConnected(host2, BpmEngineApplication.SIGNAL_NEARBY_DEVICE, processVars);

    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
        HashMap<String, Object> processVars = new HashMap<String, Object>();

        // Send Signal to host1
        processVars.put("sig_remote_address", host2.getAddress());
        signalConnected(host1, BpmEngineApplication.SIGNAL_DISCONNECTED, processVars);

        // Send Signal to host2
        processVars.put("sig_remote_address", host1.getAddress());
        signalConnected(host2, BpmEngineApplication.SIGNAL_DISCONNECTED, processVars);


    }


    private void signalConnected(DTNHost host1, String signal, HashMap<String, Object> processVars) {
        host1.getRouter().getApplications(BpmEngineApplication.APP_ID).forEach(
                a -> ((BpmEngineApplication)a).handleSignal(signal, processVars));
    }

}
