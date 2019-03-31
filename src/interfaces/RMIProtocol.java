package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIProtocol extends Remote {

    /**
     * BackupChunk file service.
     *
     * @param filePath          the file path
     * @param replicationDegree the desired replication degree
     * @throws RemoteException
     */
    void backup(String filePath, int replicationDegree) throws RemoteException;

    /**
     * Restore file service.
     *
     * @param filePath the file path
     * @throws RemoteException
     */
    void restore(String filePath) throws RemoteException;

    /**
     * Delete file service.
     *
     * @param filePath the file path
     * @throws RemoteException
     */
    void delete(String filePath) throws RemoteException;

    /**
     * Reclaim space service.
     *
     * @param space new value for reserved peer storage space
     * @throws RemoteException
     */
    void reclaim(long space) throws RemoteException;

    /**
     * Retrieve state service.
     *
     * @throws RemoteException
     */
    void state() throws RemoteException;
}
