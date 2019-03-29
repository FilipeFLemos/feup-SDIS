package peer;

import receiver.Channel;
import protocol.*;
import receiver.SocketReceiver;
import interfaces.RMIProtocol;

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
    private int peerId;

    /**
     * The protocol protocolVersion being executed
     */
    private String protocolVersion;

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

    /**
     * Constructor. Initiates peer from CLI args
     *
     * @param args initialization arguments
     * @throws IOException
     */
    private Peer(final String args[]) throws IOException {
        System.out.println("Starting Peer with protocol protocolVersion " + args[0]);
        System.out.println("Starting Peer with ID " + args[1]);
        protocolVersion = args[0];
        peerId = Integer.parseInt(args[1]);

        initRMI(args[2]);

        this.MCAddress = args[3];
        this.MCPort = Integer.parseInt(args[4]);
        this.MDBAddress = args[5];
        this.MDBPort = Integer.parseInt(args[6]);
        this.MDRAddress = args[7];
        this.MDRPort = Integer.parseInt(args[8]);

        if (!loadPeerController())
            this.controller = new PeerController(protocolVersion, peerId, MCAddress, MCPort, MDBAddress, MDBPort, MDRAddress, MDRPort);

        // save peerController data every 3 seconds
        threadPool.scheduleAtFixedRate(this::saveController, 0, 3, TimeUnit.SECONDS);

        MC = new Channel(args[3], Integer.parseInt(args[4]));
        MDB = new Channel(args[5], Integer.parseInt(args[6]));
        MDR = new Channel(args[7], Integer.parseInt(args[8]));
    }

    // peer.Peer args
    //<protocol protocolVersion> <peer id> <service access point> <MCReceiver address> <MCReceiver port> <MDBReceiver address> <MDBReceiver port> <MDRReceiver address> <MDRReceiver port>
    public static void main(final String args[]) throws IOException {
        if (args.length != 9) {
            System.out.println("Usage: java peer.Peer" +
                    " <protocol_version> <peer_id> <service_access_point>" +
                    " <MCReceiver_address> <MCReceiver_port> <MDBReceiver_address>" +
                    " <MDBReceiver_port> <MDRReceiver_address> <MDRReceiver_port>");
            return;
        }

        new Peer(args);
    }

    /**
     * Initiates remote service.
     *
     * @param accessPoint the RMI access point
     */
    protected void initRMI(String accessPoint) {
        try {
            RMIProtocol removeService = (RMIProtocol) UnicastRemoteObject.exportObject(this, 0);

            // Bind the remote object's removeService in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, removeService);

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
            FileInputStream controllerFile = new FileInputStream("PeerController" + peerId + ".ser");
            ObjectInputStream controllerObject = new ObjectInputStream(controllerFile);
            this.controller = (PeerController) controllerObject.readObject();
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
            FileOutputStream controllerFile = new FileOutputStream("PeerController" + peerId + ".ser");
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
     * Gets the protocol protocolVersion.
     *
     * @return the protocol protocolVersion
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Gets the peer id.
     *
     * @return the peer id
     */
    public int getPeerId() {
        return peerId;
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
     * @param filePath          filename of file to be backed up
     * @param replicationDegree desired replication degree
     */
    @Override
    public void backup(String filePath, int replicationDegree) {
        threadPool.submit(new BackupInitiator(this, filePath, replicationDegree, MDB));
    }

    /**
     * Submits an initiator instance of the restore protocol to the thread pool
     *
     * @param filePath filename of file to be restored
     */
    @Override
    public void restore(String filePath) {
        //TODO: make proper verification
        if (!protocolVersion.equals("1.0")) {
            System.out.println("Starting enhanced restore protocol");
            threadPool.submit(new SocketReceiver(MDRPort, controller.getDispatcher()));
        }

        threadPool.submit(new RestoreInitiator(this, filePath, MC));
    }

    /**
     * Submits an initiator instance of the delete protocol to the thread pool
     *
     * @param filePath filename of file to be deleted
     */
    @Override
    public void delete(String filePath) {
        threadPool.submit(new DeleteInitiator(this, filePath, MC));
    }

    /**
     * Submits an initiator instance of the reclaim protocol to the thread pool
     *
     * @param space new amount of reserved space for peer, in kB
     */
    @Override
    public void reclaim(long space) {
        threadPool.submit(new ReclaimInitiator(this, space));
    }

    /**
     * Retrieves the peer's local state by printing out its controller
     */
    @Override
    public void state() {
        System.out.println(controller.toString());
    }
}
