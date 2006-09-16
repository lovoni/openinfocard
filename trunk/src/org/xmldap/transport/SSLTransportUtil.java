package org.xmldap.transport;

import com.sun.slamd.example.BlindTrustSocketFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.security.cert.Certificate;

/**
 * Blind trust SSL negotiation to retrieve (server) certs used in handshake. Uses Neil Wilson's
 * SLAMD BlindTrustSocketFactory. There are some weird class loading issues, so be careful when
 * deploying in the wild. In particular, class cast exceptions due to URLConnection impl for SSL
 * connections may occur.
 * @author igb
 *
 */
public class SSLTransportUtil {
    /**
     *
     * @param host (i.e. https://xmldap.org)
     * @return server certs used in SSL handshake
     * @deprecated (see classloader issues described above)
     */
    public static Certificate[] getServerCertificates(String host) throws IOException {
         URL url  = new URL(host);
        return getServerCertificates(url);

    }

    /**
     *
      * @param url URL of host (we presume SSL)
     * @return server certs used in SSL handshake
     * @throws IOException
     * @deprecated (see classloader issues described above)
     */
    public static Certificate[] getServerCertificates(URL url) throws IOException {
        HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();

        return getServerCertificates(conn);
    }

    /**
     *
     * @param conn
     * @return server certs used in SSL handshake
     * @throws IOException
     */
    public static Certificate[] getServerCertificates(HttpsURLConnection conn) throws IOException {
        // weird classloader stuff, this needs to allways be AFTER the opening of the connection
        //TODO: figure out wtf is going on with the classloader
        BlindTrustSocketFactory blindTrust = null;
        try {
            blindTrust = new BlindTrustSocketFactory();
        } catch (Exception e) {
              e.printStackTrace();
            // throws Exception? weird. but, Neil is a better prgrammer than the rest of us, so . . .
            //TODO: dig into this later

        }

        conn.setSSLSocketFactory(blindTrust);
        conn.connect();

        return conn.getServerCertificates();
    }

    /**
     * Test against http://xmldap.org
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Certificate[] certs = SSLTransportUtil.getServerCertificates("https://xmldap.org");
        for (int i = 0; i < certs.length; i++) {
            Certificate cert = certs[i];
            System.out.println(cert);
        }
    }
}
