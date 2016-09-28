package org.CKANclient;

import java.util.ArrayList;

/**
 * Represents an error in talking to CKAN, in most cases this will
 * be as a result of False being returned in the JSON response success
 * field.
 *
 * @author      Ross Jones <ross.jones@okfn.org>, Jay Guo <jguo144@gmail.com>
 * @version     1.8
 * @since       2016-01-01
 */
public class CKANException extends Exception {

    private ArrayList<String> messages = new ArrayList<String>();

    public CKANException( String message ) {
        messages.add( message );
    }

    public void addError( String error ) {
        messages.add( error );
    }

    public ArrayList<String> getErrorMessages() {
        return messages;
    }

    public String toString() {
        return messages.toString();
    }
}
