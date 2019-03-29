package test;


import rmi.RMIProtocol;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestApp {


    /**
      * The host port where the app will run (default 1099)
      */
    private int port = 1099;

    private TestApp(String[] args) throws RemoteException {
        String[] accessPoint = parseAccessPoint(args[0]);
        if(accessPoint == null)
            return;

        RMIProtocol remoteService = initRMI(accessPoint[0], accessPoint[1]);

        switch(args[1]) {
            case "backup":
                remoteService.backupFile(args[2], Integer.parseInt(args[3]));
                break;
            case "restore":
                remoteService.recoverFile(args[2]);
                break;
            case "delete":
                remoteService.deleteFile(args[2]);
                break;
            case "reclaim":
                remoteService.reclaimSpace(Integer.parseInt(args[2]));
                break;
            case "state":
                remoteService.retrieveState();
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
            Registry registry = LocateRegistry.getRegistry(host, port);
            remoteService = (RMIProtocol) registry.lookup(name);
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
