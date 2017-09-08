/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Sockets;

import Objets.Avion;
import Objets.Message;
import Interfaces.SACAInterface;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Firass Semaan
 */
class ClientThread extends Thread {

    private ObjectInputStream _inStream;
    private ObjectOutputStream _outStream;
    private SACAInterface _serverFrame;
    private Socket _clientSocket;
    SACA _socketServer;
    private String _type;
    private int _Id;
    public Avion _flightInfo;

    public int getID() {
        return _Id;
    }

    public String getType() {
        return _type;
    }

    public ClientThread(SACA _socketServer) {
        try {
            this._socketServer = _socketServer;
            _serverFrame = _socketServer._serverFrame;
            _clientSocket = _socketServer._serverSocket.accept();
            _Id = _clientSocket.getLocalPort();

            _outStream = new ObjectOutputStream(_clientSocket.getOutputStream());
            _outStream.flush();
            _inStream = new ObjectInputStream(_clientSocket.getInputStream());
            Object msgObject = _inStream.readObject();
            if (msgObject != null) {
                Message msg = Message.class.cast(msgObject);
                _type = msg.type;

                _serverFrame.txtInfo.append("\n[" + msg.sender + "]>>>>[SACA] Nouveau " + msg.type + " détecté..");

                if (msg.type.equals("flight")) {
                    _socketServer._flightThreads.add(this);
                    _flightInfo = msg.content;
                    _serverFrame.txtInfo.append("\n[SACA]>> Informations de vol:" + msg.content.getInfo());
                } else {
                    _socketServer._radarThreads.add(this);
                }

            }
            this.start();
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        while (true) {
            if (_inStream == null || _outStream == null
                    || _clientSocket.isClosed() || !_clientSocket.isConnected()
                    || _clientSocket.isInputShutdown() || _clientSocket.isOutputShutdown()) {
                break;
            }
            if ( !ReceiveObject())
                break;

        }
    }

    public void SendFlightList(Object obj, String message) {
        try {
            _outStream.writeObject(obj);
            _outStream.flush();
            _serverFrame.txtInfo.append(message);
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public synchronized void SendObject(Message obj) {
        try {
            if (obj.type.equals("controller")) {
                this._flightInfo.Lock();

            }
            _outStream.reset();
            _outStream.writeObject(obj);
            _outStream.flush();
         

        } catch (IOException ex) {
      
        }
    }

    public boolean ReceiveObject() {
        try {
            Object message = _inStream.readObject();
            if (message == null) {
                _serverFrame.txtInfo.append("\nMessage recu n'est pas valide..");
                return true;

            }

            Message msg = Message.class.cast(message);
            if (msg.command.contains("Updated")) {
                this._flightInfo = msg.content;
                this._flightInfo.Unlock();
            }
       
            _socketServer.HandleMessage(_Id, msg);
         
            return true;
          
        }
        catch(EOFException ex)
        {}
        catch (IOException ex) {
            Logger.getLogger(ClientThread.class
                    .getName()).log(Level.SEVERE, null, ex);

        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ClientThread.class
                    .getName()).log(Level.SEVERE, null, ex);

        }
        return false;
    }
}

public class SACA extends Thread {

    public SACAInterface _serverFrame;
    public ServerSocket _serverSocket;
    public static List<ClientThread> _flightThreads;
    public List<ClientThread> _radarThreads;
    private int _port = 9876;

    public SACA(SACAInterface serverFrame) {
        try {
            _serverFrame = serverFrame;
            _serverSocket = new ServerSocket(_port);
            _flightThreads = new ArrayList();
            _radarThreads = new ArrayList();
            this.start();
        } catch (IOException ex) {
            Logger.getLogger(SACA.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void HandleMessage(int threadId, Message msg) {

        if (msg.type.equals("flight")) {//msg received from pilot
            ClientThread senderFlightThread = FindThread(msg.content.getFlightId());//find thread of this flight
            if (senderFlightThread == null) {
                _serverFrame.txtInfo.append("\nVol (" + msg.recipient + ") n'est pas trouvé..");
                return;
            }
            if (msg.command.contains("Connection")) {
                
                _flightThreads.remove(senderFlightThread);
                if (_radarThreads != null && _radarThreads.size() > 0) {
                    for (int i = 0; i < _radarThreads.size(); i++) {
                        _radarThreads.get(i).SendObject(new Message("Alerte: connexion fermée..", "radar", msg.sender, msg.content, "Radar|Controller" + _radarThreads.get(i).getID()));
                    }
                }
                return;
            }
            if (msg.command.contains("Updated")) {
                senderFlightThread._flightInfo = msg.content;

                _serverFrame.txtInfo.append("\n[" + msg.sender + "] >>>> [" + msg.recipient + "]: " + msg.command);
                if (_radarThreads != null && _radarThreads.size() > 0) {
                    for (int i = 0; i < _radarThreads.size(); i++) {
                        _radarThreads.get(i).SendObject(new Message(msg.command, "radar", msg.sender, msg.content, "Radar|Controller" + _radarThreads.get(i).getID()));
                    }
                }
            }
            if (msg.command.equals("VolInfo")) {// vol info updated
                senderFlightThread._flightInfo = msg.content;
                _serverFrame.txtInfo.append("\n[" + msg.sender + "] >>>> [ALL]: Position mise à jour..");

                if (_radarThreads != null && _radarThreads.size() > 0) {
                    for (int i = 0; i < _radarThreads.size(); i++) {
                        _radarThreads.get(i).SendObject(new Message("VolInfo", "radar", msg.sender, msg.content, "Radar|Controller-" + _radarThreads.get(i).getID()));
                    }
                }
            } else if (msg.command.contains("Alert")) {// avion ecraser ou sur sol
                senderFlightThread._flightInfo = msg.content;
                _serverFrame.txtInfo.append("\n[" + msg.sender + "] >>>> [" + msg.recipient + "]: " + msg.command);
                _flightThreads.remove(senderFlightThread);
                senderFlightThread.interrupt();

                if (_radarThreads != null && _radarThreads.size() > 0) {
                    for (int i = 0; i < _radarThreads.size(); i++) {
                        _radarThreads.get(i).SendObject(msg);
                    }
                }
            }
        } else if (msg.type.equals("controller")) {//msgs recu du controller
            if (msg.command.contains("Connection")) {//Seulement info
                _serverFrame.txtInfo.append("\n[" + msg.sender + "] >>>> [SACA]: Connexion fermée..");
                if (  msg.sender == null )
                    return;
                int senderId ;
                try
                {
                    senderId = Integer.parseInt(msg.sender);
                }
                catch(Exception e)
                {
                    return;
                }
                ClientThread conThread = FindRadarThread(senderId);
                if (conThread != null) {
                    _radarThreads.remove(conThread);
                }
                return;
            }
            HandleControllerMessages(msg);
        }
    }

    private void HandleControllerMessages(Message msg) {
        ClientThread destinationFlightThread = FindThread(msg.content.getFlightId());//find the flight to send the command

        if (destinationFlightThread == null) {
            _serverFrame.txtInfo.append("\nVol (" + msg.recipient + ") n'est pas trouvé..");
            return;
        }
        if (msg.command.equals("changer_cap")) {
            _serverFrame.txtInfo.append("\n[" + msg.sender + "] >>>> [" + msg.recipient + "]:  Changement de cap..");
            destinationFlightThread.SendObject(msg);
            return;
        }
        if (msg.command.equals("changer_altitude")) {
            _serverFrame.txtInfo.append("\n[" + msg.sender + "] >>>> [" + msg.recipient + "]:  Changement d'altitude..");
            destinationFlightThread.SendObject(msg);
            return;
        }
        if (msg.command.equals("changer_vitesse")) {
            _serverFrame.txtInfo.append("\n[" + msg.sender + "] >>>> [" + msg.recipient + "]:  Changement de vitesse..");
            destinationFlightThread.SendObject(msg);
        }
    }

    public void AddClient() {
        new ClientThread(this);
    }

    public List<Avion> GetFlights() {
        if (_flightThreads == null) {
            return null;
        }
        List<Avion> infoCollection = new ArrayList();
        ClientThread th = null;
        for (int i = 0; i < _flightThreads.size(); i++) {
            th = _flightThreads.get(i);
            infoCollection.add(th._flightInfo);
        }
        return infoCollection;
    }

    private ClientThread FindRadarThread(int ID) {
        for (int i = 0; i < _radarThreads.size(); i++) {
            if (_radarThreads.get(i).getID() == ID) {
                return _radarThreads.get(i);
            }
        }
        return null;
    }

    private ClientThread FindThread(int ID) {
        for (int i = 0; i < _flightThreads.size(); i++) {
            if (_flightThreads.get(i)._flightInfo.getFlightId() == ID) {
                return _flightThreads.get(i);
            }
        }
        return null;
    }

    @Override
    public void run() {
        while (true) {
            _serverFrame.txtInfo.append("\n[SACA] en cours de Gestion ...");
            AddClient();
        }
    }

}
