package peer;

import receiver.Channel;
import protocol.*;
import receiver.SocketReceiver;
import rmi.RMIProtocol;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Peer implements RMIProtocol {

    private static final int MAX_INITIATOR_THREADS = 50;

    /**
      * The peer's identifier
      */
    private int peerID;

    /**
      * The protocol version being executed
      */
    private String version;

    /**
      * The RMI access point for client communication
      */
    private String rmiAccessPoint;

    /**
      * Control channel address
      */
    private String MCAddress;

    /**
      * Control channel port
      */
    private int MCPort;

    /**
      * Backup channel address
      */
    private String MDBAddress;

    /**
      * Backup channel port
      */
    private int MDBPort;

    /**
      * Restore channel address
      */
    private String MDRAddress;

    /**
      * Restore channel port
      */
    private int MDRPort;

    /**
      * Control channel
      */
    private Channel MC;

    /**
      * Backup channel
      */
    private Channel MDB;

    /**
      * Restore channel
      */
    private Channel MDR;

    /**
      * Controller
      */
    private PeerController controller;

    private ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(MAX_INITIATOR_THREADS);


// peer.Peer args
    //<protocol version> <peer id> <service access point> <MCReceiver address> <MCReceiver port> <MDBReceiver address> <MDBReceiver port> <MDRReceiver address> <MDRReceiver port>
    public static void main(final String args[]) throws IOException {
        Peer peer;
        if(!checkArgs(args))
            printUsage();
        else
            peer = new Peer(args);
    }

    /**
      * Constructor. Initiates peer from CLI args
      *
      * @param args initialization arguments
      * @throws IOException
      */
    private Peer(final String args[]) throws IOException {
        System.out.println("Starting Peer with protocol version " + args[0]);
        this.version = args[0];
        System.out.println("Starting Peer with ID " + args[1]);
        this.peerID = Integer.parseInt(args[1]);

        // RMI
        this.rmiAccessPoint = args[2];
        this.initRemoteStub(rmiAccessPoint);

        this.MCAddress = args[3]; this.MCPort = Integer.parseInt(args[4]);
        this.MDBAddress = args[5]; this.MDBPort = Integer.parseInt(args[6]);
        this.MDRAddress = args[7]; this.MDRPort = Integer.parseInt(args[8]);

        if(!loadPeerController())
            this.controller = new PeerController(version, peerID, MCAddress, MCPort, MDBAddress, MDBPort, MDRAddress, MDRPort);

        // save peerController data every 3 seconds
        threadPool.scheduleAtFixedRate(() -> {
            this.saveController();
        }, 0, 3, TimeUnit.SECONDS);

        this.MC = new Channel(args[3], Integer.parseInt(args[4]));
        this.MDB = new Channel(args[5], Integer.parseInt(args[6]));
        this.MDR = new Channel(args[7], Integer.parseInt(args[8]));
    }

    /**
      * Checks if peer arguments are correct
      *
      * @param args args to be checked
      * @return true if args are correct, false otherwise
      */
    private static boolean checkArgs(final String args[]) {
        //TODO: add thorough args verification
        return args.length >= 9;
    }

    /**
      * Print usage to show an user how to properly start a peer
      */
    private static void printUsage() {
        //TODO: add thorough usage information
        System.out.println("Usage:");
        System.out.println("Java peer.Peer : <protocol version> <peer id> <service access point> <MCReceiver address> <MCReceiver port> <MDBReceiver address> <MDBReceiver port> <MDRReceiver address> <MDRReceiver port>");
    }

    /**
     * Initiates remote stub.
     *
     * @param accessPoint the RMI access point
     */
    protected void initRemoteStub(String accessPoint) {
        try {
            RMIProtocol stub = (RMIProtocol) UnicastRemoteObject.exportObject(this, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
      * Loads the peer controller from non-volatile memory, if file is present, or starts a new one.
      *
      * @return true if controller successfully loaded from .ser file, false otherwise
      */
    public boolean loadPeerController() {
        try {
            FileInputStream controllerFile = new FileInputStream("PeerController" + peerID + ".ser");
            ObjectInputStream controllerObject = new ObjectInputStream(controllerFile);
            this.controller = (PeerController)controllerObject.readObject();
            this.controller.initTransientMethods(MCAddress, MCPort, MDBAddress, MDBPort, MDRAddress, MDRPort);
            controllerObject.close();
            controllerFile.close();
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("No pre-existing PeerController found, starting new one");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
      * Saves the controller state to non-volatile memory
      */
    public void saveController() {
        try {
            FileOutputStream controllerFile = new FileOutputStream("PeerController" + peerID + ".ser");
            ObjectOutputStream controllerObject = new ObjectOutputStream(controllerFile);
            controllerObject.writeObject(this.controller);
            controllerObject.close();
            controllerFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("PeerController not found");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the protocol version.
     *
     * @return the protocol version
     */
    public String getProtocolVersion() {
        return version;
    }

    /**
     * Gets the peer id.
     *
     * @return the peer id
     */
    public int getPeerID() {
        return peerID;
    }

    /**
     * Gets the controller.
     *
     * @return the controller
     */
    public PeerController getController() {
        return controller;
    }

    /**
      * Submits an initiator instance of the backup protocol to the thread pool
      *
      * @param filePath filename of file to be backed up
      * @param replicationDegree desired replication degree
      * @throws RemoteException
      */
    @Override
    public void backupFile(String filePath, int replicationDegree) throws RemoteException{
        ProtocolInitiator backupInstance = new BackupInitiator(this, filePath, replicationDegree, MDB);
        threadPool.submit(backupInstance);
    }

    /**
      * Submits an initiator instance of the restore protocol to the thread pool
      *
      * @param filePath filename of file to be restored
      * @throws RemoteException
      */
    @Override
    public void recoverFile(String filePath) throws RemoteException {
        //TODO: make proper verification
        if(!version.equals("1.0")) {
            System.out.println("Starting enhanced restore protocol");
            threadPool.submit(new SocketReceiver(MDRPort, controller.getDispatcher()));
        }

        ProtocolInitiator recoverInstance = new RestoreInitiator(this, filePath, MC);
        threadPool.submit(recoverInstance);
    }

    /**
      * Submits an initiator instance of the delete protocol to the thread pool
      *
      * @param filePath filename of file to be deleted
      * @throws RemoteException
      */
    @Override
    public void deleteFile(String filePath) throws RemoteException {
        ProtocolInitiator deleteInstance = new DeleteInitiator(this, filePath, MC);
        threadPool.submit(deleteInstance);
    }

    /**
      * Submits an initiator instance of the reclaim protocol to the thread pool
      *
      * @param space new amount of reserved space for peer, in kB
      * @throws RemoteException
      */
    @Override
    public void reclaimSpace(long space) throws RemoteException {
        ProtocolInitiator reclaimInstance = new ReclaimInitiator(this, space);
        threadPool.submit(reclaimInstance);
    }

    /**
      * Retrieves the peer's local state by printing out its controller
      *
      * @throws RemoteException
      */
    @Override
    public void retrieveState() throws RemoteException {
        System.out.println(controller.toString());
    }
}
