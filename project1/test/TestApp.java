package test;


import rmi.RemoteService;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestApp {

    /**
      * The host where the app will run
      */
    private String host = null;

    /**
      * The host port where the app will run (default 1099)
      */
    private int port = 1099;

    /**
      * The host's remote name where the app will run
      */
    private String name = null;

    public static void main(final String args[]) throws RemoteException {
        if(!validArguments(args)) {
          System.out.println("Invalid arguments: exiting...");
          return;
        }
        new TestApp(args);
    }

    public static boolean validArguments(String args[]) {
        if(args.length < 3) return false;
        return !(!args[1].equals("backup") && !args[1].equals("restore") && !args[1].equals("delete") && !args[1].equals("reclaim") && !args[1].equals("state"));
    }

    private TestApp(String[] args) throws RemoteException {
        parseLocation(args[0]);
        RemoteService stub = initApp();

        //only one of the following will be used
        String filePath = args[2];
        int extraArg;

        switch(args[1]) {
            case "backup":
                extraArg = Integer.parseInt(args[3]);
                stub.backupFile(filePath, extraArg);
                break;
            case "restore":
                stub.recoverFile(filePath);
                break;
            case "delete":
                stub.deleteFile(filePath);
                break;
            case "reclaim":
                extraArg = Integer.parseInt(args[2]);
                stub.reclaimSpace(extraArg);
                break;
            case "state":
                System.out.println(stub.retrieveState());
                break;
        }
    }

    /**
      * Initiates the remote service given the host's information.
      * @return remote stub
      */
    private RemoteService initApp() {
        RemoteService stub = null;

        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            stub = (RemoteService) registry.lookup(name);
        } catch (Exception e) {
            System.err.println("Error connecting to remote object '" + name + "' on " + host);
            e.printStackTrace();
        }

        return stub;
    }

    /**
      * Parses the information of the remote host
      * @param location host location given as //host[:port]/name
      */
    private boolean parseLocation(String location) {
        // 1st group: host (mandatory)
        // 2nd group: port (optional)
        // 3rd group: name (mandatory)
        Pattern p = Pattern.compile("//([\\w.]+)(?::(\\d+))?/(\\w+)");
        Matcher m = p.matcher(location);

        if(!m.matches())
            return false;

        this.host = m.group(1);
        this.name = m.group(3);

        // if port exists
        if(m.group(2) != null)
            this.port = Integer.parseInt(m.group(2));

        return true;
    }
}
