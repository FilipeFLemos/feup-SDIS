package peer;

import message.Message;
import channels.*;
import protocols.*;
import interfaces.RMIProtocol;
import user_interface.UI;

import java.io.*;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static utils.Utils.parseRMI;

public class Peer implements RMIProtocol {

    private static final int MAX_INITIATOR_THREADS = 50;
    private Channel MCChannel;
    private Channel MDBChannel;
    private Channel MDRChannel;
    private MessageHandler messageHandler;
    private TCPSender TCPController;
    //private SocketController TCPController;
    private int serverId;
    private String version;
    private PeerState controller;
    private ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(MAX_INITIATOR_THREADS);
    private int MDRPort;

    /**
     * Constructor. Initiates peer from CLI args
     *
     * @param args initialization arguments
     */
    private Peer(final String args[]) {
        UI.printBoot("-------------------- Booting Peer --------------------");
        UI.nl();
        UI.printBoot("Starting Peer with protocols version " + args[0]);
        UI.printBoot("Starting Peer with ID " + args[1]);
        version = args[0];
        serverId = Integer.parseInt(args[1]);

        String[] serviceAccessPoint = parseRMI(true, args[2]);
        if (serviceAccessPoint == null) {
            return;
        }

        initRMI(args[1]);

        if (!loadPeerController())
            this.controller = new PeerState(version, serverId);

        UI.printBoot("------------- Booting Multicast Channels -------------");
        UI.nl();

        this.messageHandler = new MessageHandler(this);

        // save peerController data every 3 seconds
        threadPool.scheduleAtFixedRate(this::saveController, 0, 3, TimeUnit.SECONDS);

        MDRPort = Integer.parseInt(args[8]);
        initChannels(args[3], Integer.parseInt(args[4]), args[5], Integer.parseInt(args[6]), args[7], MDRPort);

        UI.nl();
        UI.printBoot("-------------------- Peer "+args[1]+" Ready --------------------");
    }

    // peer.Peer args
    //<protocols version> <peer id> <service access point> <MCChannel address> <MCChannel port> <MDBChannel address> <MDBChannel port> <MDRChannel address> <MDRChannel port>
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
    private void initRMI(String accessPoint) {
        try {
            RMIProtocol remoteService = (RMIProtocol) UnicastRemoteObject.exportObject(this, 0);

            // Get own registry, to rebind to correct remoteService
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, remoteService);

            UI.printBoot("Server ready!");
            UI.nl();
        } catch (Exception e) {
            UI.printError("Server exception: " + e.toString());
        }
    }

    /**
     * Loads the peer controller from non-volatile memory, if file is present, or starts a new one.
     *
     * @return true if controller successfully loaded from .ser file, false otherwise
     */
    private boolean loadPeerController() {
        try {
            FileInputStream controllerFile = new FileInputStream("PeerState" + serverId + ".ser");
            ObjectInputStream controllerObject = new ObjectInputStream(controllerFile);
            this.controller = (PeerState) controllerObject.readObject();
            //this.controller.initChannels(MCAddress, MCPort, MDBAddress, MDBPort, MDRAddress, MDRPort);
            controllerObject.close();
            controllerFile.close();
            return true;
        } catch (FileNotFoundException e) {
            UI.printWarning("No pre-existing PeerState found, starting new one");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Initiates fields not retrievable from non-volatile memory
     *
     * @param MCAddress control channel address
     * @param MCPort control channel port
     * @param MDBAddress backup channel address
     * @param MDBPort backup channel port
     * @param MDRAddress restore channel address
     * @param MDRPort restore channel port
     */
    public void initChannels(String MCAddress, int MCPort, String MDBAddress, int MDBPort, String MDRAddress, int MDRPort) {
        // subscribe to multicast channels
        try {
            this.MCChannel = new Channel(MCAddress, MCPort, messageHandler);
            this.MDBChannel = new Channel(MDBAddress, MDBPort, messageHandler);
            this.MDRChannel = new Channel(MDRAddress, MDRPort, messageHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(!version.equals("1.0")) {
            TCPController = new TCPSender(MDRPort);
            //TCPController = new SocketController(MDRPort);
        }
    }

    /**
     * Saves the controller state to non-volatile memory
     */
    private void saveController() {
        try {
            FileOutputStream controllerFile = new FileOutputStream("PeerState" + serverId + ".ser");
            ObjectOutputStream controllerObject = new ObjectOutputStream(controllerFile);
            controllerObject.writeObject(this.controller);
            controllerObject.close();
            controllerFile.close();
        } catch (FileNotFoundException e) {
            UI.printError("PeerState not found");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String getVersion() {
        return version;
    }


    public int getServerId() {
        return serverId;
    }


    public PeerState getController() {
        return controller;
    }

    /**
     * Submits an initiator instance of the backup protocols to the thread pool
     *
     * @param filePath          filename of file to be backed up
     * @param replicationDegree desired replication degree
     */
    @Override
    public void backup(String filePath, int replicationDegree) {
        threadPool.submit(new BackupInitiator(controller, filePath, replicationDegree, MDBChannel));
    }

    /**
     * Submits an initiator instance of the restore protocols to the thread pool
     *
     * @param filePath filename of file to be restored
     */
    @Override
    public void restore(String filePath) {
        if (!version.equals("1.0")) {
            System.out.println("Starting enhanced restore protocols");
            threadPool.submit(new TCPReceiver(MDRPort, messageHandler));
            //threadPool.submit(new SocketReceiver(MDRPort, messageHandler));
        }

        threadPool.submit(new RestoreInitiator(controller, filePath, MCChannel));
    }

    /**
     * Submits an initiator instance of the delete protocols to the thread pool
     *
     * @param filePath filename of file to be deleted
     */
    @Override
    public void delete(String filePath) {
        threadPool.submit(new DeleteInitiator(this, filePath, MCChannel));
    }

    /**
     * Submits an initiator instance of the reclaim protocols to the thread pool
     *
     * @param space new amount of reserved space for peer, in kB
     */
    @Override
    public void reclaim(long space) {
        threadPool.submit(new ReclaimInitiator(controller, space, MCChannel));
    }

    /**
     * Retrieves the peer's local state by printing out its controller
     */
    @Override
    public void state() {
        UI.printInfo("-------------------- Peer "+serverId+" State --------------------");
        System.out.println(controller.getPeerState());
        UI.printInfo("------------------------------------------------------");
    }

    public Channel getMCChannel() {
        return MCChannel;
    }

    public Channel getMDBChannel() {
        return MDBChannel;
    }

    public void sendMessage(Message message, InetAddress sourceAddress){
        if(controller.isRestoreEnhancement() && !message.getVersion().equals("1.0")) {
            //send chunk via tcp and send header to MDR
            TCPController.sendMessage(message, sourceAddress);
            MDRChannel.sendMessage(message, false);
        }
        else
            MDRChannel.sendMessage(message);
    }
}
