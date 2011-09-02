package com.era7.bioinfo.servletlibraryneo4j.listeners;

import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.era7.lib.communication.util.SessionAttributes;
import com.era7.lib.era7jdbcapi.DBConnection;

/**
 * This class must be included in the application descriptor as an application listener
 * Otherwise the session guide system for connections with the db would not work properly
 * @author Pablo Pareja Tobes
 * @deprecated
 */
@Deprecated
public class SessionListener implements HttpSessionListener {
	
		
	/**
	 * Method called when a new session has been created
	 */
	@Override
	public void sessionCreated(HttpSessionEvent event) {
		
		//HttpSession session = event.getSession();
		//Code needed when a session has been created should be placed here
		
	}
	
	/**
	 *	Method called when a session has been destroyed
	 */
	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
				
	}

}
