/*
 * Created on 26 juil. 06 by richet
 */
package org.funz;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Map;
import java.util.Properties;
import org.funz.conf.Configuration;

public class Proxy {

    public static final String HTTP_PROXY = "http_proxy";
    public static final String PROXYDOMAIN = "proxy.domain";
    public static final String PROXYHOST = "proxy.host";
    public static final String PROXYPASSWORD = "proxy.password";
    public static final String PROXYPORT = "proxy.port";
    public static final String PROXYUSERNAME = "proxy.username";
    private static String host = System.getProperties().getProperty("http.proxyHost");
    private static String port = System.getProperties().getProperty("http.proxyPort");
    private static String username = System.getProperties().getProperty("http.proxyUser");
    private static String password = System.getProperties().getProperty("http.proxyPassword");
    private static String domain = System.getProperties().getProperty("http.proxyDomain");

    static {
        Map<String, String> env = System.getenv();
        if (env.containsKey(HTTP_PROXY)) {
            try {
                String URL = env.get(HTTP_PROXY);
                //String login = null;
                //String passwd = null;
                String nameport = null;
                String domainusername = null;
                //String name;
                //int port = -1;
                if (URL.contains("@")) {
                    String loginpasswd = URL.split("@")[0].substring(("http://").length());
                    domainusername = loginpasswd.split(":")[0];
                    if (domainusername.contains("\\")) {
                        setUsername(domainusername.split("\\")[1]);
                        setDomain(domainusername.split("\\")[0]);
                    } else {
                        setUsername(domainusername);
                        setDomain("");
                    }
                    setPassword(loginpasswd.split(":")[1]);
                    nameport = URL.split("@")[1];
                } else {
                    nameport = URL.substring(("http://").length());
                }

                if (nameport.contains(":")) {
                    setHost(nameport.split(":")[0]);
                    setPort(Integer.parseInt(nameport.split(":")[1].replace("/", "")) + "");
                } else {
                    setHost(nameport);
                }
            } catch (Exception e) {
                System.err.println("Impossible to parse http_proxy=" + env.get(HTTP_PROXY));
            }
        }
    }

    public static void init(Properties props) {
        Properties _props = new Properties();
        for (String k : props.stringPropertyNames()) {
            //System.out.println(k+" -> "+props.getProperty(k));
            if (k.startsWith("proxy.")) {
                _props.put(k, props.getProperty(k));
            }
        }

        read(_props);

        applyProxy();
    }

    static void read(Properties _props) {

        if (_props.containsKey(PROXYHOST) && _props.getProperty(PROXYHOST).length() != 0) {
            setHost(_props.getProperty(PROXYHOST));
            //System.out.println("  proxy.host=" + host);
        }
        if (_props.containsKey(PROXYPORT) && _props.getProperty(PROXYPORT).length() != 0) {
            setPort(_props.getProperty(PROXYPORT));
            //System.out.println("  proxy.port=" + port);
        }
        if (_props.containsKey(PROXYDOMAIN)) {
            setDomain(_props.getProperty(PROXYDOMAIN));
            //System.out.println("  proxy.domain=" + domain);
        }
        if (_props.containsKey(PROXYUSERNAME)) {
            setUsername(_props.getProperty(PROXYUSERNAME));
            //System.out.println("  proxy.username=" + username);
        }
        if (_props.containsKey(PROXYPASSWORD)) {
            try {
                setPassword(Configuration.lowdeCrypt(_props.getProperty(PROXYPASSWORD)));
                //System.err.println("  proxy.password=" + getPassword());
            } catch (Exception e) {
                setPassword("");
            }
            //System.out.println("  proxy.password=" + password);
        }
    }

    public static void write(Properties props) {
        if (getHost() != null) {
            props.setProperty(PROXYHOST, getHost());
        }
        if (getPort() != null) {
            props.setProperty(PROXYPORT, getPort());
        }
        if (getDomain() != null) {
            props.setProperty(PROXYDOMAIN, getDomain());
        }
        if (getUsername() != null) {
            props.setProperty(PROXYUSERNAME, getUsername());
        }
        if (getPassword() != null) {
            props.setProperty(PROXYPASSWORD, /*lowCrypt*/ Configuration.lowCrypt(getPassword()));
        }
    }

    public static void applyProxy() {
        if (getHost() != null && getHost().length() > 0) {
            try {
                // _console.logMessage(IInfoCollector.SeverityLevel.INFO, true, "Loading PROXY properties from " + url.getPath());

                Properties systemSettings = System.getProperties();
                systemSettings.put("http.proxyHost", getHost());
                systemSettings.put("http.proxyPort", getPort());

                if (getUsername() != null && getUsername().length() > 0) {
                    Authenticator.setDefault(new Authenticator() {

                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(getDomain() + "\\" + getUsername(), getPassword().toCharArray());
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public static String http_proxy() {
        //if (_propertiesUrl == null) {
        //  readProperties(null);
        //}
        if (getHost() == null || getHost().length() == 0) {
            return "";
        } else {
            return "http://" + (getDomain() == null || getDomain().length() == 0 ? "" : getDomain() + "\\\\") + (getUsername() == null ? "" : (getUsername() + (getPassword() == null ? "" : ":" + getPassword()) + "@")) + getHost() + ":" + getPort();
        }
    }

    /**
     * @return the host
     */
    public static String getHost() {
        return host;
    }

    /**
     * @return the port
     */
    public static String getPort() {
        return port;
    }

    /**
     * @return the username
     */
    public static String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public static String getPassword() {
        return password;
    }

    /**
     * @return the domain
     */
    public static String getDomain() {
        return domain;
    }

    /**
     * @param aHost the host to set
     */
    public static void setHost(String aHost) {
        host = aHost;
    }

    /**
     * @param aPort the port to set
     */
    public static void setPort(String aPort) {
        port = aPort;
    }

    /**
     * @param aUsername the username to set
     */
    public static void setUsername(String aUsername) {
        username = aUsername;
    }

    /**
     * @param aPassword the password to set
     */
    public static void setPassword(String aPassword) {
        password = aPassword;
    }

    /**
     * @param aDomain the domain to set
     */
    public static void setDomain(String aDomain) {
        domain = aDomain;
    }
}
