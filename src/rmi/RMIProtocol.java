package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIProtocol extends Remote {

    /**
     * Backup file service.
     *
     * @param filePath          the file path
     * @param replicationDegree the desired replication degree
     * @throws RemoteException
     */
    void backupFile(String filePath, int replicationDegree) throws RemoteException;

    /**
     * Restore file service.
     *
     * @param filePath the file path
     * @throws RemoteException
     */
    void recoverFile(String filePath) throws RemoteException;

    /**
     * Delete file service.
     *
     * @param filePath the file path
     * @throws RemoteException
     */
    void deleteFile(String filePath) throws RemoteException;

    /**
     * Reclaim space service.
     *
     * @param space new value for reserved peer storage space
     * @throws RemoteException
     */
    void reclaimSpace(long space) throws RemoteException;

    /**
     * Retrieve state service.
     *
     * @throws RemoteException
     */
    String retrieveState() throws RemoteException;
}
