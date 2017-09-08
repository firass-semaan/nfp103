/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Objets;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Firass Semaan
 */
public class Avion implements Serializable {

    private Coordonnees _position;
    private Deplacement _deplacement;
    private String _flightName;
    private int _flightId;
    private boolean _isLocked;
    public boolean IsLocked() {
         return _isLocked ;
    }

    public synchronized void Lock()  {
        while(_isLocked)
        {
            try {
                wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(Avion.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        _isLocked = true ;
    }

    public synchronized void Unlock() {
        _isLocked = false;
        notifyAll();
    }
    public Avion(Coordonnees coordonnees, Deplacement deplacement, String flightName) {
        _position = coordonnees;
        _deplacement = deplacement;
        _flightName = flightName;
        _isLocked = false;
    }

    /**
     * @return the _position
     */
    public Coordonnees getPosition() {
        return _position;
    }

    /**
     * @param _position the _position to set
     */
    public void setPosition(Coordonnees _position) {
        this._position = _position;
    }

    /**
     * @return the _deplacement
     */
    public Deplacement getDeplacement() {
        return _deplacement;
    }

    /**
     * @param _deplacement the _deplacement to set
     */
    public void setDeplacement(Deplacement _deplacement) {
        this._deplacement = _deplacement;
    }

    /**
     * @return the _flightName
     */
    public String getFlightName() {
        return _flightName;
    }

    /**
     * @param _flightName the _flightName to set
     */
    public void setFlightName(String _flightName) {
        this._flightName = _flightName;
    }

    public String getInfo() {
        String info = "Vol name:" + _flightName + ", " 
                + "Position: " + _position.getX()+":"+_position.getY()+":"+_position.getAltitude()+", "
                + "Vitesse: " + _deplacement.getVitesse()+",Cap: "+_deplacement.getCap();

        return info;
    }

    /**
     * @return the _flightId
     */
    public int getFlightId() {
        return _flightId;
    }

    /**
     * @param _flightId the _flightId to set
     */
    public void setFlightId(int _flightId) {
        this._flightId = _flightId;
    }

    /**
     * @return the _volColor
     */
   
       
}
