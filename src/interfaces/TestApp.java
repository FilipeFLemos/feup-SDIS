package interfaces;


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import user_interface.UI;


import static utils.Utils.parseRMI;

public class TestApp {

    private TestApp(String[] args) throws RemoteException {
        String[] peer_ap = parseRMI(args[0], true);
        if (peer_ap == null) {
            return;
        }

        RMIProtocol remoteService = initRMI(peer_ap);

        switch (args[1]) {
            case "BACKUP":
                remoteService.backup(args[2], Integer.parseInt(args[3]));
                break;
            case "RESTORE":
                remoteService.restore(args[2]);
                break;
            case "DELETE":
                remoteService.delete(args[2]);
                break;
            case "RECLAIM":
                remoteService.reclaim(Integer.parseInt(args[2]));
                break;
            case "STATE":
                remoteService.state();
                break;
        }
    }

    public static void main(final String args[]) {
        if (args.length < 2 || args.length > 4) {
            UI.print("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            return;
        }

        if (!(args[1].equals("BACKUP") || args[1].equals("RESTORE") || args[1].equals("DELETE") || args[1].equals("RECLAIM") || args[1].equals("STATE"))) {
            UI.printError("Invalid sub_protocol. (BACKUP, RESTORE, DELETE, RECLAIM, STATE)");
            return;
        }

        try {
            new TestApp(args);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initiates the remote service given the host's information.
     *
     * @return remote stub
     */
    private RMIProtocol initRMI(String[] peer_ap) {
        RMIProtocol remoteService = null;
        try {
            Registry registry = getRegistry(peer_ap);
            remoteService = (RMIProtocol) registry.lookup(peer_ap[2]);
        } catch (Exception e) {
            UI.printError("Error when opening RMI stub");
            e.printStackTrace();
        }

        return remoteService;
    }

    /**
     * Binds the remote stub with the peer registry.
     * @param serviceAccessPoint - the peer access point
     * @return the peer registry
     */
    private Registry getRegistry(String[] serviceAccessPoint) {
        Registry registry = null;
        try {
            if (serviceAccessPoint[1] == null) {
                if (serviceAccessPoint[0].equals("localhost")) {
                    registry = LocateRegistry.getRegistry();
                } else {
                    registry = LocateRegistry.getRegistry(serviceAccessPoint[0]);
                }
            } else {
                if (serviceAccessPoint[0].equals("localhost")) {
                    registry = LocateRegistry.getRegistry(serviceAccessPoint[1]);
                } else {
                    registry = LocateRegistry.getRegistry(serviceAccessPoint[0], Integer.parseInt(serviceAccessPoint[1]));
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return registry;
    }
}
