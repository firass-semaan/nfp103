/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Objets;

import java.io.Serializable;

/**
 *
 * @author Firass Semaan
 */
public class Deplacement implements Serializable{
    private int _cap;
    private int _vitesse;

    /**
     * @return the cap
     */
    public int getCap() {
        return _cap;
    }

    /**
     * @param cap the cap to set
     */
    public void setCap(int cap) {
        this._cap = cap;
    }

    /**
     * @return the _vitesse
     */
    public int getVitesse() {
        return _vitesse;
    }

    /**
     * @param vitesse the _vitesse to set
     */
    public void setVitesse(int vitesse) {
        this._vitesse = vitesse;
    }
    
    
}
