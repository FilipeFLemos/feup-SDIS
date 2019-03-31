package interfaces;


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static utils.Utils.getRegistry;
import static utils.Utils.parseRMI;

public class TestApp {


    private TestApp(String[] args) throws RemoteException {
        String[] peer_ap = parseRMI(false, args[0]);
        if (peer_ap == null) {
            return;
        }

        RMIProtocol remoteService = initRMI(peer_ap);

        switch(args[1]) {
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

    public static void main(final String args[]){
        if (args.length < 2 || args.length > 4) {
            System.out.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            return;
        }

        if(!(args[1].equals("BACKUP") || args[1].equals("RESTORE") || args[1].equals("DELETE") || args[1].equals("RECLAIM") || args[1].equals("STATE"))){
            System.out.println("Invalid sub_protocol. (BACKUP, RESTORE, DELETE, RECLAIM, STATE)");
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
      * @return remote stub
      */
    private RMIProtocol initRMI(String[] peer_ap) {
        RMIProtocol remoteService = null;
        try {
            Registry registry = getRegistry(peer_ap);
            remoteService = (RMIProtocol) registry.lookup(peer_ap[2]);
        } catch (Exception e) {
            System.out.println("Error when opening RMI stub");
            e.printStackTrace();
        }

        return remoteService;
    }
}
