package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIProtocol extends Remote {

    /**
     * Executes the backup file service.
     *
     * @param filePath       - the file path
     * @param replicationDeg - the desired replication degree
     */
    void backup(String filePath, int replicationDeg) throws RemoteException;

    /**
     * Executes the restore file service.
     *
     * @param filePath - the file path
     */
    void restore(String filePath) throws RemoteException;

    /**
     * Executes the delete file service.
     *
     * @param filePath the file path
     */
    void delete(String filePath) throws RemoteException;

    /**
     * Executes the reclaim space service.
     *
     * @param space - the value to be reclaimed from the used local storage space
     */
    void reclaim(long space) throws RemoteException;

    /**
     * Executes the retrieve state service.
     */
    void state() throws RemoteException;
}
