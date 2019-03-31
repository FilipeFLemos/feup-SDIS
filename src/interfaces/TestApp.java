package interfaces;


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static utils.Utils.getRegistry;
import static utils.Utils.parseRMI;

public class TestApp {


    /**
      * The host port where the app will run (default 1099)
      */
    private int port = 1099;

    private TestApp(String[] args) throws RemoteException {
        String[] peer_ap = parseRMI(false, args[0]);
        if (peer_ap == null) {
            return;
        }

        RMIProtocol remoteService = null;
        try {
            Registry registry = getRegistry(peer_ap);
            remoteService = (RMIProtocol) registry.lookup(peer_ap[2]);
        } catch (Exception e) {
            System.out.println("Error when opening RMI stub");
            e.printStackTrace();
        }

        switch(args[1]) {
            case "backup":
                remoteService.backup(args[2], Integer.parseInt(args[3]));
                break;
            case "restore":
                remoteService.restore(args[2]);
                break;
            case "delete":
                remoteService.delete(args[2]);
                break;
            case "reclaim":
                remoteService.reclaim(Integer.parseInt(args[2]));
                break;
            case "state":
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
    private RMIProtocol initRMI(String host, String name) {
        RMIProtocol remoteService = null;

        try {
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1099);
            remoteService = (RMIProtocol) registry.lookup("1");
        } catch (Exception e) {
            System.err.println("Error connecting to remote object '" + name + "' on " + host);
            e.printStackTrace();
        }

        return remoteService;
    }

    /**
      * Parses the information of the remote access host point
      * @param peer_ap host peer_ap given as //host[:port]/name
      * @return [host,name] or null if peer_ap does not match the rmiPattern
      */
    private String[] parseAccessPoint(String peer_ap) {
        Pattern rmiPattern = Pattern.compile("//([\\w.]+)(?::(\\d+))?/(\\w+)");
        Matcher m = rmiPattern.matcher(peer_ap);

        if(!m.matches())
            return null;

        if(m.group(2) != null)
            port = Integer.parseInt(m.group(2));

        return new String[]{m.group(1), m.group(3)};
    }
}
