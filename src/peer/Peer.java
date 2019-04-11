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

import static utils.Utils.MAX_THREADS;
import static utils.Utils.SAVING_INTERVAL;
import static utils.Utils.parseRMI;

public class Peer implements RMIProtocol {

    private Channel MCChannel;
    private Channel MDBChannel;
    private Channel MDRChannel;
    private MessageHandler messageHandler;
    private TCPSender tcpSender;
    private TCPReceiver tcpReceiver = null;
    private int serverId;
    private String version;
    private PeerState peerState;
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(MAX_THREADS);
    private int MDRPort;
    private final int TCP_PORT = 4444;
    private boolean isEnhanced = false;

    private Peer(final String args[]) {
        UI.printBoot("------------------- Booting Peer " + args[1] + " -------------------");
        UI.nl();
        UI.printBoot("Protocols version " + args[0]);
        version = args[0];
        serverId = Integer.parseInt(args[1]);

        if (!version.equals("1.0")) {
            isEnhanced = true;
        }

        String[] serviceAccessPoint = parseRMI(args[2], false);
        if (serviceAccessPoint == null) {
            return;
        }

        initRMI(args[1]);

        if (!loadPeerState()) {
            peerState = new PeerState(version, serverId);
        }

        messageHandler = new MessageHandler(this);

        UI.printBoot("------------- Booting Multicast Channels -------------");
        UI.nl();

        scheduledExecutorService.scheduleAtFixedRate(this::saveController, 0, SAVING_INTERVAL, TimeUnit.SECONDS);

        MDRPort = Integer.parseInt(args[8]);
        initChannels(args[3], Integer.parseInt(args[4]), args[5], Integer.parseInt(args[6]), args[7], MDRPort);

        UI.nl();
        UI.printBoot("-------------------- Peer " + args[1] + " Ready --------------------");

        if (isEnhanced) {
            sendCONTROL();
        }
    }

    /**
     * Sends a CONTROL message to the MC channel.
     */
    private void sendCONTROL() {
        Message messageCONTROL = new Message(version, serverId, null, Message.MessageType.CONTROL);
        MCChannel.sendMessage(messageCONTROL);
        UI.printBoot("-------------- Sending CONTROL message ---------------");
        UI.printBoot("------------------------------------------------------");
    }

    public static void main(final String args[]) {
        if (args.length != 9) {
            UI.printError("Wrong input!");
            UI.printWarning("Please use: java peer.Peer" + " <protocol_version> <peer_id> <service_access_point>" +
                    " <MCReceiver_address> <MCReceiver_port> <MDBReceiver_address>" + " <MDBReceiver_port> <MDRReceiver_address> <MDRReceiver_port>");
            return;
        }
        new Peer(args);
    }

    /**
     * Initiates the remote service.
     *
     * @param accessPoint - the RMI access point
     */
    private void initRMI(String accessPoint) {
        try {
            RMIProtocol remoteService = (RMIProtocol) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, remoteService);

            UI.printBoot("Connection to RMI server established");
            UI.nl();
        } catch (Exception e) {
            UI.printError("Failed to connect to RMI server. \nReason: " + e.toString());
        }
    }

    /**
     * Loads the peer state from non-volatile memory, if it exists. Otherwise, creates a new Peer.
     *
     * @return true if the peer state was loaded successfully or false if otherwise
     */
    private boolean loadPeerState() {
        try {
            FileInputStream fileInputStream = new FileInputStream("PeerState" + serverId + ".ser");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            peerState = (PeerState) objectInputStream.readObject();
            peerState.setVersion(version);
            objectInputStream.close();
            fileInputStream.close();
            return true;
        } catch (FileNotFoundException e) {
            UI.printWarning("Couldn't find any saved peer state. Starting a new one");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Initiates channels.
     */
    private void initChannels(String MCAddress, int MCPort, String MDBAddress, int MDBPort, String MDRAddress, int MDRPort) {
        try {
            MCChannel = new Channel("MC", MCAddress, MCPort, messageHandler);
            MDBChannel = new Channel("MDB", MDBAddress, MDBPort, messageHandler);
            MDRChannel = new Channel("MDR", MDRAddress, MDRPort, messageHandler);

            new Thread(MCChannel).start();
            new Thread(MDBChannel).start();
            new Thread(MDRChannel).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isEnhanced) {
            tcpSender = new TCPSender(TCP_PORT);
        }
    }

    /**
     * Saves the peer state to non-volatile memory
     */
    private void saveController() {
        try {
            FileOutputStream controllerFile = new FileOutputStream("PeerState" + serverId + ".ser");
            ObjectOutputStream controllerObject = new ObjectOutputStream(controllerFile);
            controllerObject.writeObject(this.peerState);
            controllerObject.close();
            controllerFile.close();
        } catch (FileNotFoundException e) {
            UI.printError("Error creating PeerState" + serverId + ".ser");
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

    public PeerState getPeerState() {
        return peerState;
    }

    public boolean isEnhanced() {
        return isEnhanced;
    }

    @Override
    public void backup(String filePath, int replicationDeg) {
        scheduledExecutorService.submit(new BackupInitiator(peerState, filePath, replicationDeg, MDBChannel));
    }

    @Override
    public void restore(String filePath) {
        if (!version.equals("1.0")) {
            UI.printInfo("Enhanced restore protocols initiated  (v" + version + ")");
            tcpReceiver = new TCPReceiver(MDRPort + serverId, messageHandler);
            scheduledExecutorService.submit(tcpReceiver);
        }

        scheduledExecutorService.submit(new RestoreInitiator(peerState, filePath, MCChannel));
    }

    @Override
    public void delete(String filePath) {
        scheduledExecutorService.submit(new DeleteInitiator(this, filePath, MCChannel));
    }

    @Override
    public void reclaim(long space) {
        scheduledExecutorService.submit(new ReclaimInitiator(peerState, space, MCChannel));
    }

    @Override
    public void state() {
        UI.printInfo("-------------------- Peer " + serverId + " State --------------------");
        UI.print(peerState.getPeerState());
        UI.printInfo("------------------------------------------------------");
    }

    public Channel getMCChannel() {
        return MCChannel;
    }

    public Channel getMDBChannel() {
        return MDBChannel;
    }

    /**
     * If the peer is enhanced, sends the CHUNK to the TCP socket and just the header to the MDC channel.
     * Else, sends the CHUNK to the MDC channel.
     * @param message - the received CHUNK message
     * @param address - the address of the TCP socket
     */
    public void sendMessage(Message message, InetAddress address) {
        if (isEnhanced && !message.getVersion().equals("1.0")) {
            MDRChannel.sendMessage(message, false);
            tcpSender.sendMessage(message, address);
        } else {
            MDRChannel.sendMessage(message);
        }
    }

    public void closeTcpReceiver() {
        if(tcpReceiver != null){
            tcpReceiver.close();
            tcpReceiver = null;
        }
    }
}
