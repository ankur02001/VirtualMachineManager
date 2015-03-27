package com.vmware.general;

/////////////////////////////////////////////////////////////////////
//Connection.java - Helps in connecting to server and vm      	   //
//ver 1.0                                                          //
//Language:      Java                                              //
//Platform:      Dell, Windows 8.1                                 //
//Application:   Project 1, Cloud Computing, spring2015            //
//Author:		   Ankur Pandey     Ref: TestConnection.Java       //
/////////////////////////////////////////////////////////////////////
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;
import java.net.URL;
import java.util.Map;
import com.vmware.vim25.*;
import com.vmware.vm.VMotion;
import com.vmware.*;
import com.vmware.common.ssl.TrustAllTrustManager;
import com.vmware.performance.VITop;
import com.vmware.performance.VIUsage;
import com.vmware.connection.BasicConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import com.vmware.connection.ConnectedVimServiceBase;

public class Connection extends ConnectedVimServiceBase {
	public static void initializeCertificates()
			throws NoSuchAlgorithmException, KeyManagementException {
		// Declare a host name verifier that will automatically enable
		// the connection. The host name verifier is invoked during
		// the SSL handshake.
		javax.net.ssl.HostnameVerifier verifier = new HostnameVerifier() {
			public boolean verify(String urlHostName, SSLSession session) {
				return true;
			}
		};
		// Create the trust manager.
		javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
		javax.net.ssl.TrustManager trustManager = new TrustAllTrustManager();
		trustAllCerts[0] = trustManager;

		// Create the SSL context
		javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext
				.getInstance("SSL");

		// Create the session context
		javax.net.ssl.SSLSessionContext sslsc = sc.getServerSessionContext();

		// Initialize the contexts; the session context takes the trust manager.
		sslsc.setSessionTimeout(0);
		sc.init(null, trustAllCerts, null);

		// Use the default socket factory to create the socket for the secure
		// connection
		javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc
				.getSocketFactory());
		// Set the default host name verifier to enable the connection.
		HttpsURLConnection.setDefaultHostnameVerifier(verifier);

	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		String SERVER_NAME = "https://128.230.247.52/sdk";
		String url = "https://128.230.247.52/sdk" + "/vimService";
		String USER_NAME = "AD\\apandey";
		String PASSWORD = "Hanu2Man!";
		VimPortType vimPort = null;
		VimService vimService = null;
		ServiceContent serviceContent;

		initializeCertificates();

		ManagedObjectReference servicesInstance = new ManagedObjectReference();
		servicesInstance.setType("ServiceInstance");
		servicesInstance.setValue("ServiceInstance");
		vimService = new VimService();
		vimPort = vimService.getVimPort();
		Map<String, Object> ctxt = ((BindingProvider) vimPort)
				.getRequestContext();
		System.out.println(vimPort);
		ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
		ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
		serviceContent = vimPort.retrieveServiceContent(servicesInstance);
		try {
			vimPort.login(serviceContent.getSessionManager(), USER_NAME,
					PASSWORD, null);
		} catch (InvalidLocaleFaultMsg e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidLoginFaultMsg e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			// print out the product name, server type, and product version
			System.out.println(serviceContent.getAbout().getFullName());
			System.out.printf("Server type is %s", serviceContent.getAbout()
					.getApiType());
			System.out.printf("API version is %s", serviceContent.getAbout()
					.getVersion());
			RealTime realtime_ = new RealTime(serviceContent, vimPort);
			System.out.printf(" RunTime Start ");
			realtime_.run();
			System.out.printf(" RunTime Ended ");
		} catch (Exception e) {
			System.err.println("Sample code failed ");
			e.printStackTrace();
			System.exit(1);
		} finally {
			if (vimPort != null && serviceContent != null) {
				try {
					vimPort.logout(serviceContent.getSessionManager());
				} catch (RuntimeFaultFaultMsg rffm) {
					rffm.printStackTrace();
				}
			}
		}
	}
}
