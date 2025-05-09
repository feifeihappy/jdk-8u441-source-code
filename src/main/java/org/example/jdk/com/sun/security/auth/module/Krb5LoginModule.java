/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */


package com.sun.security.auth.module;

import java.io.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.*;

import javax.security.auth.*;
import javax.security.auth.kerberos.*;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;
import javax.security.auth.spi.*;

import sun.security.krb5.*;
import sun.security.jgss.krb5.Krb5Util;
import sun.security.krb5.Credentials;

/**
 * <p> This <code>LoginModule</code> authenticates users using
 * Kerberos protocols.
 *
 * <p> The configuration entry for <code>Krb5LoginModule</code> has
 * several options that control the authentication process and
 * additions to the <code>Subject</code>'s private credential
 * set. Irrespective of these options, the <code>Subject</code>'s
 * principal set and private credentials set are updated only when
 * <code>commit</code> is called.
 * When <code>commit</code> is called, the <code>KerberosPrincipal</code>
 * is added to the <code>Subject</code>'s principal set (unless the
 * <code>principal</code> is specified as "*"). If <code>isInitiator</code>
 * is true, the <code>KerberosTicket</code> is
 * added to the <code>Subject</code>'s private credentials.
 *
 * <p> If the configuration entry for <code>KerberosLoginModule</code>
 * has the option <code>storeKey</code> set to true, then
 * <code>KerberosKey</code> or <code>KeyTab</code> will also be added to the
 * subject's private credentials. <code>KerberosKey</code>, the principal's
 * key(s) will be derived from user's password, and <code>KeyTab</code> is
 * the keytab used when <code>useKeyTab</code> is set to true. The
 * <code>KeyTab</code> object is restricted to be used by the specified
 * principal unless the principal value is "*".
 *
 * <p> This <code>LoginModule</code> recognizes the <code>doNotPrompt</code>
 * option. If set to true the user will not be prompted for the password.
 *
 * <p> The user can  specify the location of the ticket cache by using
 * the option <code>ticketCache</code> in the configuration entry.
 *
 * <p>The user can specify the keytab location by using
 * the option <code>keyTab</code>
 * in the configuration entry.
 *
 * <p> The principal name can be specified in the configuration entry
 * by using the option <code>principal</code>. The principal name
 * can either be a simple user name, a service name such as
 * <code>host/mission.eng.sun.com</code>, or "*". The principal can also
 * be set using the system property <code>sun.security.krb5.principal</code>.
 * This property is checked during login. If this property is not set, then
 * the principal name from the configuration is used. In the
 * case where the principal property is not set and the principal
 * entry also does not exist, the user is prompted for the name.
 * When this property of entry is set, and <code>useTicketCache</code>
 * is set to true, only TGT belonging to this principal is used.
 *
 * <p> The following is a list of configuration options supported
 * for <code>Krb5LoginModule</code>:
 * <blockquote><dl>
 * <dt><b><code>refreshKrb5Config</code></b>:</dt>
 * <dd> Set this to true, if you want the configuration
 * to be refreshed before the <code>login</code> method is called.</dd>
 * <dt><b><code>useTicketCache</code></b>:</dt>
 * <dd>Set this to true, if you want the
 * TGT to be obtained
 * from the ticket cache. Set this option
 * to false if you do not want this module to use the ticket cache.
 * (Default is False).
 * This module will
 * search for the ticket
 * cache in the following locations:
 * On Solaris and Linux
 * it will look for the ticket cache in /tmp/krb5cc_<code>uid</code>
 * where the uid is numeric user
 * identifier. If the ticket cache is
 * not available in the above location, or if we are on a
 * Windows platform, it will look for the cache as
 * {user.home}{file.separator}krb5cc_{user.name}.
 * You can override the ticket cache location by using
 * <code>ticketCache</code>.
 * For Windows, if a ticket cannot be retrieved from the file ticket cache,
 * it will use Local Security Authority (LSA) API to get the TGT.
 * <dt><b><code>ticketCache</code></b>:</dt>
 * <dd>Set this to the name of the ticket
 * cache that  contains user's TGT.
 * If this is set,  <code>useTicketCache</code>
 * must also be set to true; Otherwise a configuration error will
 * be returned.</dd>
 * <dt><b><code>renewTGT</code></b>:</dt>
 * <dd>Set this to true, if you want to renew
 * the TGT. If this is set, <code>useTicketCache</code> must also be
 * set to true; otherwise a configuration error will be returned.</dd>
 * <dt><b><code>doNotPrompt</code></b>:</dt>
 * <dd>Set this to true if you do not want to be
 * prompted for the password
 * if credentials can not be obtained from the cache, the keytab,
 * or through shared state.(Default is false)
 * If set to true, credential must be obtained through cache, keytab,
 * or shared state. Otherwise, authentication will fail.</dd>
 * <dt><b><code>useKeyTab</code></b>:</dt>
 * <dd>Set this to true if you
 * want the module to get the principal's key from the
 * the keytab.(default value is False)
 * If <code>keytab</code>
 * is not set then
 * the module will locate the keytab from the
 * Kerberos configuration file.
 * If it is not specified in the Kerberos configuration file
 * then it will look for the file
 * <code>{user.home}{file.separator}</code>krb5.keytab.</dd>
 * <dt><b><code>keyTab</code></b>:</dt>
 * <dd>Set this to the file name of the
 * keytab to get principal's secret key.</dd>
 * <dt><b><code>storeKey</code></b>:</dt>
 * <dd>Set this to true to if you want the keytab or the
 * principal's key to be stored in the Subject's private credentials.
 * For <code>isInitiator</code> being false, if <code>principal</code>
 * is "*", the {@link KeyTab} stored can be used by anyone, otherwise,
 * it's restricted to be used by the specified principal only.</dd>
 * <dt><b><code>principal</code></b>:</dt>
 * <dd>The name of the principal that should
 * be used. The principal can be a simple username such as
 * "<code>testuser</code>" or a service name such as
 * "<code>host/testhost.eng.sun.com</code>". You can use the
 * <code>principal</code>  option to set the principal when there are
 * credentials for multiple principals in the
 * <code>keyTab</code> or when you want a specific ticket cache only.
 * The principal can also be set using the system property
 * <code>sun.security.krb5.principal</code>. In addition, if this
 * system property is defined, then it will be used. If this property
 * is not set, then the principal name from the configuration will be
 * used.
 * The principal name can be set to "*" when <code>isInitiator</code> is false.
 * In this case, the acceptor is not bound to a single principal. It can
 * act as any principal an initiator requests if keys for that principal
 * can be found. When <code>isInitiator</code> is true, the principal name
 * cannot be set to "*".
 * </dd>
 * <dt><b><code>isInitiator</code></b>:</dt>
 * <dd>Set this to true, if initiator. Set this to false, if acceptor only.
 * (Default is true).
 * Note: Do not set this value to false for initiators.</dd>
 * </dl></blockquote>
 *
 * <p> This <code>LoginModule</code> also recognizes the following additional
 * <code>Configuration</code>
 * options that enable you to share username and passwords across different
 * authentication modules:
 * <blockquote><dl>
 *
 *    <dt><b><code>useFirstPass</code></b>:</dt>
 *                   <dd>if, true, this LoginModule retrieves the
 *                   username and password from the module's shared state,
 *                   using "javax.security.auth.login.name" and
 *                   "javax.security.auth.login.password" as the respective
 *                   keys. The retrieved values are used for authentication.
 *                   If authentication fails, no attempt for a retry
 *                   is made, and the failure is reported back to the
 *                   calling application.</dd>
 *
 *    <dt><b><code>tryFirstPass</code></b>:</dt>
 *                   <dd>if, true, this LoginModule retrieves the
 *                   the username and password from the module's shared
 *                   state using "javax.security.auth.login.name" and
 *                   "javax.security.auth.login.password" as the respective
 *                   keys.  The retrieved values are used for
 *                   authentication.
 *                   If authentication fails, the module uses the
 *                   CallbackHandler to retrieve a new username
 *                   and password, and another attempt to authenticate
 *                   is made. If the authentication fails,
 *                   the failure is reported back to the calling application</dd>
 *
 *    <dt><b><code>storePass</code></b>:</dt>
 *                   <dd>if, true, this LoginModule stores the username and
 *                   password obtained from the CallbackHandler in the
 *                   modules shared state, using
 *                   "javax.security.auth.login.name" and
 *                   "javax.security.auth.login.password" as the respective
 *                   keys.  This is not performed if existing values already
 *                   exist for the username and password in the shared
 *                   state, or if authentication fails.</dd>
 *
 *    <dt><b><code>clearPass</code></b>:</dt>
 *                   <dd>if, true, this LoginModule clears the
 *                   username and password stored in the module's shared
 *                   state  after both phases of authentication
 *                   (login and commit) have completed.</dd>
 * </dl></blockquote>
 * <p>If the principal system property or key is already provided, the value of
 * "javax.security.auth.login.name" in the shared state is ignored.
 * <p>When multiple mechanisms to retrieve a ticket or key is provided, the
 * preference order is:
 * <ol>
 * <li>ticket cache
 * <li>keytab
 * <li>shared state
 * <li>user prompt
 * </ol>
 * <p>Note that if any step fails, it will fallback to the next step.
 * There's only one exception, if the shared state step fails and
 * <code>useFirstPass</code>=true, no user prompt is made.
 * <p>Examples of some configuration values for Krb5LoginModule in
 * JAAS config file and the results are:
 * <ul>
 * <p> <code>doNotPrompt</code>=true;
 * </ul>
 * <p> This is an illegal combination since none of <code>useTicketCache</code>,
 * <code>useKeyTab</code>, <code>useFirstPass</code> and <code>tryFirstPass</code>
 * is set and the user can not be prompted for the password.
 *<ul>
 * <p> <code>ticketCache</code> = &lt;filename&gt;;
 *</ul>
 * <p> This is an illegal combination since <code>useTicketCache</code>
 * is not set to true and the ticketCache is set. A configuration error
 * will occur.
 * <ul>
 * <p> <code>renewTGT</code>=true;
 *</ul>
 * <p> This is an illegal combination since <code>useTicketCache</code> is
 * not set to true and renewTGT is set. A configuration error will occur.
 * <ul>
 * <p> <code>storeKey</code>=true
 * <code>useTicketCache</code> = true
 * <code>doNotPrompt</code>=true;;
 *</ul>
 * <p> This is an illegal combination since  <code>storeKey</code> is set to
 * true but the key can not be obtained either by prompting the user or from
 * the keytab, or from the shared state. A configuration error will occur.
 * <ul>
 * <p>  <code>keyTab</code> = &lt;filename&gt; <code>doNotPrompt</code>=true ;
 * </ul>
 * <p>This is an illegal combination since useKeyTab is not set to true and
 * the keyTab is set. A configuration error will occur.
 * <ul>
 * <p> <code>debug=true </code>
 *</ul>
 * <p> Prompt the user for the principal name and the password.
 * Use the authentication exchange to get TGT from the KDC and
 * populate the <code>Subject</code> with the principal and TGT.
 * Output debug messages.
 * <ul>
 * <p> <code>useTicketCache</code> = true <code>doNotPrompt</code>=true;
 *</ul>
 * <p>Check the default cache for TGT and populate the <code>Subject</code>
 * with the principal and TGT. If the TGT is not available,
 * do not prompt the user, instead fail the authentication.
 * <ul>
 * <p><code>principal</code>=&lt;name&gt;<code>useTicketCache</code> = true
 * <code>doNotPrompt</code>=true;
 *</ul>
 * <p> Get the TGT from the default cache for the principal and populate the
 * Subject's principal and private creds set. If ticket cache is
 * not available or does not contain the principal's TGT
 * authentication will fail.
 * <ul>
 * <p> <code>useTicketCache</code> = true
 * <code>ticketCache</code>=&lt;file name&gt;<code>useKeyTab</code> = true
 * <code> keyTab</code>=&lt;keytab filename&gt;
 * <code>principal</code> = &lt;principal name&gt;
 * <code>doNotPrompt</code>=true;
 *</ul>
 * <p>  Search the cache for the principal's TGT. If it is not available
 * use the key in the keytab to perform authentication exchange with the
 * KDC and acquire the TGT.
 * The Subject will be populated with the principal and the TGT.
 * If the key is not available or valid then authentication will fail.
 * <ul>
 * <p><code>useTicketCache</code> = true
 * <code>ticketCache</code>=&lt;file name&gt;
 *</ul>
 * <p> The TGT will be obtained from the cache specified.
 * The Kerberos principal name used will be the principal name in
 * the Ticket cache. If the TGT is not available in the
 * ticket cache the user will be prompted for the principal name
 * and the password. The TGT will be obtained using the authentication
 * exchange with the KDC.
 * The Subject will be populated with the TGT.
 *<ul>
 * <p> <code>useKeyTab</code> = true
 * <code>keyTab</code>=&lt;keytab filename&gt;
 * <code>principal</code>= &lt;principal name&gt;
 * <code>storeKey</code>=true;
 *</ul>
 * <p>  The key for the principal will be retrieved from the keytab.
 * If the key is not available in the keytab the user will be prompted
 * for the principal's password. The Subject will be populated
 * with the principal's key either from the keytab or derived from the
 * password entered.
 * <ul>
 * <p> <code>useKeyTab</code> = true
 * <code>keyTab</code>=&lt;keytabname&gt;
 * <code>storeKey</code>=true
 * <code>doNotPrompt</code>=false;
 *</ul>
 * <p>The user will be prompted for the service principal name.
 * If the principal's
 * longterm key is available in the keytab , it will be added to the
 * Subject's private credentials. An authentication exchange will be
 * attempted with the principal name and the key from the Keytab.
 * If successful the TGT will be added to the
 * Subject's private credentials set. Otherwise the authentication will
 * fail.
 * <ul>
 * <p> <code>isInitiator</code> = false <code>useKeyTab</code> = true
 * <code>keyTab</code>=&lt;keytabname&gt;
 * <code>storeKey</code>=true
 * <code>principal</code>=*;
 *</ul>
 * <p>The acceptor will be an unbound acceptor and it can act as any principal
 * as long that principal has keys in the keytab.
 *<ul>
 * <p>
 * <code>useTicketCache</code>=true
 * <code>ticketCache</code>=&lt;file name&gt;;
 * <code>useKeyTab</code> = true
 * <code>keyTab</code>=&lt;file name&gt; <code>storeKey</code>=true
 * <code>principal</code>= &lt;principal name&gt;
 *</ul>
 * <p>
 * The client's TGT will be retrieved from the ticket cache and added to the
 * <code>Subject</code>'s private credentials. If the TGT is not available
 * in the ticket cache, or the TGT's client name does not match the principal
 * name, Java will use a secret key to obtain the TGT using the authentication
 * exchange and added to the Subject's private credentials.
 * This secret key will be first retrieved from the keytab. If the key
 * is not available, the user will be prompted for the password. In either
 * case, the key derived from the password will be added to the
 * Subject's private credentials set.
 * <ul>
 * <p><code>isInitiator</code> = false
 *</ul>
 * <p>Configured to act as acceptor only, credentials are not acquired
 * via AS exchange. For acceptors only, set this value to false.
 * For initiators, do not set this value to false.
 * <ul>
 * <p><code>isInitiator</code> = true
 *</ul>
 * <p>Configured to act as initiator, credentials are acquired
 * via AS exchange. For initiators, set this value to true, or leave this
 * option unset, in which case default value (true) will be used.
 *
 * @author Ram Marti
 */

@jdk.Exported
public class Krb5LoginModule implements LoginModule {

    // initial state
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, Object> sharedState;
    private Map<String, ?> options;

    // configurable option
    private boolean debug = false;
    private boolean storeKey = false;
    private boolean doNotPrompt = false;
    private boolean useTicketCache = false;
    private boolean useKeyTab = false;
    private String ticketCacheName = null;
    private String keyTabName = null;
    private String princName = null;

    private boolean useFirstPass = false;
    private boolean tryFirstPass = false;
    private boolean storePass = false;
    private boolean clearPass = false;
    private boolean refreshKrb5Config = false;
    private boolean renewTGT = false;

    // specify if initiator.
    // perform authentication exchange if initiator
    private boolean isInitiator = true;

    // the authentication status
    private boolean succeeded = false;
    private boolean commitSucceeded = false;
    private String username;

    // Encryption keys calculated from password. Assigned when storekey == true
    // and useKeyTab == false (or true but not found)
    private EncryptionKey[] encKeys = null;

    KeyTab ktab = null;

    private Credentials cred = null;

    private PrincipalName principal = null;
    private KerberosPrincipal kerbClientPrinc = null;
    private KerberosTicket kerbTicket = null;
    private KerberosKey[] kerbKeys = null;
    private StringBuffer krb5PrincName = null;
    private boolean unboundServer = false;
    private char[] password = null;

    private static final String NAME = "javax.security.auth.login.name";
    private static final String PWD = "javax.security.auth.login.password";
    private static final ResourceBundle rb = AccessController.doPrivileged(
            new PrivilegedAction<ResourceBundle>() {
                public ResourceBundle run() {
                    return ResourceBundle.getBundle(
                            "sun.security.util.AuthResources");
                }
            }
    );

    /**
     * Initialize this <code>LoginModule</code>.
     *
     * <p>
     * @param subject the <code>Subject</code> to be authenticated. <p>
     *
     * @param callbackHandler a <code>CallbackHandler</code> for
     *                  communication with the end user (prompting for
     *                  usernames and passwords, for example). <p>
     *
     * @param sharedState shared <code>LoginModule</code> state. <p>
     *
     * @param options options specified in the login
     *                  <code>Configuration</code> for this particular
     *                  <code>LoginModule</code>.
     */
    // Unchecked warning from (Map<String, Object>)sharedState is safe
    // since javax.security.auth.login.LoginContext passes a raw HashMap.
    // Unchecked warnings from options.get(String) are safe since we are
    // passing known keys.
    @SuppressWarnings("unchecked")
    public void initialize(Subject subject,
                           CallbackHandler callbackHandler,
                           Map<String, ?> sharedState,
                           Map<String, ?> options) {

        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = (Map<String, Object>)sharedState;
        this.options = options;

        // initialize any configured options

        debug = "true".equalsIgnoreCase((String)options.get("debug"));
        storeKey = "true".equalsIgnoreCase((String)options.get("storeKey"));
        doNotPrompt = "true".equalsIgnoreCase((String)options.get
                                              ("doNotPrompt"));
        useTicketCache = "true".equalsIgnoreCase((String)options.get
                                                 ("useTicketCache"));
        useKeyTab = "true".equalsIgnoreCase((String)options.get("useKeyTab"));
        ticketCacheName = (String)options.get("ticketCache");
        keyTabName = (String)options.get("keyTab");
        if (keyTabName != null) {
            keyTabName = sun.security.krb5.internal.ktab.KeyTab.normalize(
                         keyTabName);
        }
        princName = (String)options.get("principal");
        refreshKrb5Config =
            "true".equalsIgnoreCase((String)options.get("refreshKrb5Config"));
        renewTGT =
            "true".equalsIgnoreCase((String)options.get("renewTGT"));

        // check isInitiator value
        String isInitiatorValue = ((String)options.get("isInitiator"));
        if (isInitiatorValue == null) {
            // use default, if value not set
        } else {
            isInitiator = "true".equalsIgnoreCase(isInitiatorValue);
        }

        tryFirstPass =
            "true".equalsIgnoreCase
            ((String)options.get("tryFirstPass"));
        useFirstPass =
            "true".equalsIgnoreCase
            ((String)options.get("useFirstPass"));
        storePass =
            "true".equalsIgnoreCase((String)options.get("storePass"));
        clearPass =
            "true".equalsIgnoreCase((String)options.get("clearPass"));
        if (debug) {
            System.out.print("Debug is  " + debug
                             + " storeKey " + storeKey
                             + " useTicketCache " + useTicketCache
                             + " useKeyTab " + useKeyTab
                             + " doNotPrompt " + doNotPrompt
                             + " ticketCache is " + ticketCacheName
                             + " isInitiator " + isInitiator
                             + " KeyTab is " + keyTabName
                             + " refreshKrb5Config is " + refreshKrb5Config
                             + " principal is " + princName
                             + " tryFirstPass is " + tryFirstPass
                             + " useFirstPass is " + useFirstPass
                             + " storePass is " + storePass
                             + " clearPass is " + clearPass + "\n");
        }
    }


    /**
     * Authenticate the user
     *
     * <p>
     *
     * @return true in all cases since this <code>LoginModule</code>
     *          should not be ignored.
     *
     * @exception FailedLoginException if the authentication fails. <p>
     *
     * @exception LoginException if this <code>LoginModule</code>
     *          is unable to perform the authentication.
     */
    public boolean login() throws LoginException {

        if (refreshKrb5Config) {
            try {
                if (debug) {
                    System.out.println("Refreshing Kerberos configuration");
                }
                sun.security.krb5.Config.refresh();
            } catch (KrbException ke) {
                LoginException le = new LoginException(ke.getMessage());
                le.initCause(ke);
                throw le;
            }
        }
        String principalProperty = System.getProperty
            ("sun.security.krb5.principal");
        if (principalProperty != null) {
            krb5PrincName = new StringBuffer(principalProperty);
        } else {
            if (princName != null) {
                krb5PrincName = new StringBuffer(princName);
            }
        }

        validateConfiguration();

        if (krb5PrincName != null && krb5PrincName.toString().equals("*")) {
            unboundServer = true;
        }

        if (tryFirstPass) {
            try {
                attemptAuthentication(true);
                if (debug)
                    System.out.println("\t\t[Krb5LoginModule] " +
                                       "authentication succeeded");
                succeeded = true;
                cleanState();
                return true;
            } catch (LoginException le) {
                // authentication failed -- try again below by prompting
                cleanState();
                if (debug) {
                    System.out.println("\t\t[Krb5LoginModule] " +
                                       "tryFirstPass failed with:" +
                                       le.getMessage());
                }
            }
        } else if (useFirstPass) {
            try {
                attemptAuthentication(true);
                succeeded = true;
                cleanState();
                return true;
            } catch (LoginException e) {
                // authentication failed -- clean out state
                if (debug) {
                    System.out.println("\t\t[Krb5LoginModule] " +
                                       "authentication failed \n" +
                                       e.getMessage());
                }
                succeeded = false;
                cleanState();
                throw e;
            }
        }

        // attempt the authentication by getting the username and pwd
        // by prompting or configuration i.e. not from shared state

        try {
            attemptAuthentication(false);
            succeeded = true;
            cleanState();
            return true;
        } catch (LoginException e) {
            // authentication failed -- clean out state
            if (debug) {
                System.out.println("\t\t[Krb5LoginModule] " +
                                   "authentication failed \n" +
                                   e.getMessage());
            }
            succeeded = false;
            cleanState();
            throw e;
        }
    }
    /**
     * process the configuration options
     * Get the TGT either out of
     * cache or from the KDC using the password entered
     * Check the  permission before getting the TGT
     */

    private void attemptAuthentication(boolean getPasswdFromSharedState)
        throws LoginException {

        /*
         * Check the creds cache to see whether
         * we have TGT for this client principal
         */
        if (krb5PrincName != null) {
            try {
                principal = new PrincipalName
                    (krb5PrincName.toString(),
                     PrincipalName.KRB_NT_PRINCIPAL);
            } catch (KrbException e) {
                LoginException le = new LoginException(e.getMessage());
                le.initCause(e);
                throw le;
            }
        }

        try {
            if (useTicketCache) {
                // ticketCacheName == null implies the default cache
                if (debug)
                    System.out.println("Acquire TGT from Cache");
                cred  = Credentials.acquireTGTFromCache
                    (principal, ticketCacheName);

                if (cred != null) {
                    // check to renew credentials
                    if (!isCurrent(cred)) {
                        if (renewTGT) {
                            Credentials newCred = renewCredentials(cred);
                            if (newCred != null) {
                                newCred.setProxy(cred.getProxy());
                                cred = newCred;
                            }
                        } else {
                            // credentials have expired
                            cred = null;
                            if (debug)
                                System.out.println("Credentials are" +
                                                " no longer valid");
                        }
                    }
                }

                if (cred != null) {
                    // get the principal name from the ticket cache
                    if (principal == null) {
                        principal = cred.getProxy() != null
                            ? cred.getProxy().getClient()
                            : cred.getClient();
                    }
                }
                if (debug) {
                    System.out.println("Principal is " + principal);
                    if (cred == null) {
                        System.out.println
                            ("null credentials from Ticket Cache");
                    }
                }
            }

            // cred = null indicates that we didn't get the creds
            // from the cache or useTicketCache was false

            if (cred == null) {
                // We need the principal name whether we use keytab
                // or AS Exchange
                if (principal == null) {
                    promptForName(getPasswdFromSharedState);
                    principal = new PrincipalName
                        (krb5PrincName.toString(),
                         PrincipalName.KRB_NT_PRINCIPAL);
                }

                /*
                 * Before dynamic KeyTab support (6894072), here we check if
                 * the keytab contains keys for the principal. If no, keytab
                 * will not be used and password is prompted for.
                 *
                 * After 6894072, we normally don't check it, and expect the
                 * keys can be populated until a real connection is made. The
                 * check is still done when isInitiator == true, where the keys
                 * will be used right now.
                 *
                 * Probably tricky relations:
                 *
                 * useKeyTab is config flag, but when it's true but the ktab
                 * does not contains keys for principal, we would use password
                 * and keep the flag unchanged (for reuse?). In this method,
                 * we use (ktab != null) to check whether keytab is used.
                 * After this method (and when storeKey == true), we use
                 * (encKeys == null) to check.
                 */
                if (useKeyTab) {
                    if (!unboundServer) {
                        KerberosPrincipal kp =
                                new KerberosPrincipal(principal.getName());
                        ktab = (keyTabName == null)
                                ? KeyTab.getInstance(kp)
                                : KeyTab.getInstance(kp, new File(keyTabName));
                    } else {
                        ktab = (keyTabName == null)
                                ? KeyTab.getUnboundInstance()
                                : KeyTab.getUnboundInstance(new File(keyTabName));
                    }
                    if (isInitiator) {
                        if (Krb5Util.keysFromJavaxKeyTab(ktab, principal).length
                                == 0) {
                            ktab = null;
                            if (debug) {
                                System.out.println
                                    ("Key for the principal " +
                                     principal  +
                                     " not available in " +
                                     ((keyTabName == null) ?
                                      "default key tab" : keyTabName));
                            }
                        }
                    }
                }

                KrbAsReqBuilder builder;

                if (ktab == null) {
                    promptForPass(getPasswdFromSharedState);
                    builder = new KrbAsReqBuilder(principal, password);
                    if (isInitiator) {
                        // XXX Even if isInitiator=false, it might be
                        // better to do an AS-REQ so that keys can be
                        // updated with PA info
                        cred = builder.action().getCreds();
                    }
                    if (storeKey) {
                        encKeys = builder.getKeys(isInitiator);
                        // When encKeys is empty, the login actually fails.
                        // For compatibility, exception is thrown in commit().
                    }
                } else {
                    builder = new KrbAsReqBuilder(principal, ktab);
                    if (isInitiator) {
                        cred = builder.action().getCreds();
                    }
                }
                builder.destroy();

                if (debug) {
                    System.out.println("principal is " + principal);
                    if (ktab != null) {
                        System.out.println("Will use keytab");
                    } else if (storeKey) {
                        for (int i = 0; i < encKeys.length; i++) {
                            System.out.println(encKeys[i].toString());
                        }
                    }
                }

                // we should hava a non-null cred
                if (isInitiator && (cred == null)) {
                    throw new LoginException
                        ("TGT Can not be obtained from the KDC ");
                }

            }
        } catch (KrbException e) {
            LoginException le = new LoginException(e.getMessage());
            le.initCause(e);
            throw le;
        } catch (IOException ioe) {
            LoginException ie = new LoginException(ioe.getMessage());
            ie.initCause(ioe);
            throw ie;
        }
    }

    private void promptForName(boolean getPasswdFromSharedState)
        throws LoginException {
        krb5PrincName = new StringBuffer("");
        if (getPasswdFromSharedState) {
            // use the name saved by the first module in the stack
            username = (String)sharedState.get(NAME);
            if (debug) {
                System.out.println
                    ("username from shared state is " + username + "\n");
            }
            if (username == null) {
                System.out.println
                    ("username from shared state is null\n");
                throw new LoginException
                    ("Username can not be obtained from sharedstate ");
            }
            if (debug) {
                System.out.println
                    ("username from shared state is " + username + "\n");
            }
            if (username != null && username.length() > 0) {
                krb5PrincName.insert(0, username);
                return;
            }
        }

        if (doNotPrompt) {
            throw new LoginException
                ("Unable to obtain Principal Name for authentication ");
        } else {
            if (callbackHandler == null)
                throw new LoginException("No CallbackHandler "
                                         + "available "
                                         + "to garner authentication "
                                         + "information from the user");
            try {
                String defUsername = System.getProperty("user.name");

                Callback[] callbacks = new Callback[1];
                MessageFormat form = new MessageFormat(
                                       rb.getString(
                                       "Kerberos.username.defUsername."));
                Object[] source =  {defUsername};
                callbacks[0] = new NameCallback(form.format(source));
                callbackHandler.handle(callbacks);
                username = ((NameCallback)callbacks[0]).getName();
                if (username == null || username.length() == 0)
                    username = defUsername;
                krb5PrincName.insert(0, username);

            } catch (java.io.IOException ioe) {
                throw new LoginException(ioe.getMessage());
            } catch (UnsupportedCallbackException uce) {
                throw new LoginException
                    (uce.getMessage()
                     +" not available to garner "
                     +" authentication information "
                     +" from the user");
            }
        }
    }

    private void promptForPass(boolean getPasswdFromSharedState)
        throws LoginException {

        if (getPasswdFromSharedState) {
            // use the password saved by the first module in the stack
            password = (char[])sharedState.get(PWD);
            if (password == null) {
                if (debug) {
                    System.out.println
                        ("Password from shared state is null");
                }
                throw new LoginException
                    ("Password can not be obtained from sharedstate ");
            }
            if (debug) {
                System.out.println
                    ("Get password from shared state");
            }
            return;
        }
        if (doNotPrompt) {
            throw new LoginException
                ("Unable to obtain password from user\n");
        } else {
            if (callbackHandler == null)
                throw new LoginException("No CallbackHandler "
                                         + "available "
                                         + "to garner authentication "
                                         + "information from the user");
            try {
                Callback[] callbacks = new Callback[1];
                String userName = krb5PrincName.toString();
                MessageFormat form = new MessageFormat(
                                         rb.getString(
                                         "Kerberos.password.for.username."));
                Object[] source = {userName};
                callbacks[0] = new PasswordCallback(
                                                    form.format(source),
                                                    false);
                callbackHandler.handle(callbacks);
                char[] tmpPassword = ((PasswordCallback)
                                      callbacks[0]).getPassword();
                if (tmpPassword == null) {
                    throw new LoginException("No password provided");
                }
                password = new char[tmpPassword.length];
                System.arraycopy(tmpPassword, 0,
                                 password, 0, tmpPassword.length);
                ((PasswordCallback)callbacks[0]).clearPassword();


                // clear tmpPassword
                for (int i = 0; i < tmpPassword.length; i++)
                    tmpPassword[i] = ' ';
                tmpPassword = null;
                if (debug) {
                    System.out.println("\t\t[Krb5LoginModule] " +
                                       "user entered username: " +
                                       krb5PrincName);
                    System.out.println();
                }
            } catch (java.io.IOException ioe) {
                throw new LoginException(ioe.getMessage());
            } catch (UnsupportedCallbackException uce) {
                throw new LoginException(uce.getMessage()
                                         +" not available to garner "
                                         +" authentication information "
                                         + "from the user");
            }
        }
    }

    private void validateConfiguration() throws LoginException {
        if (doNotPrompt && !useTicketCache && !useKeyTab
                && !tryFirstPass && !useFirstPass)
            throw new LoginException
                ("Configuration Error"
                 + " - either doNotPrompt should be "
                 + " false or at least one of useTicketCache, "
                 + " useKeyTab, tryFirstPass and useFirstPass"
                 + " should be true");
        if (ticketCacheName != null && !useTicketCache)
            throw new LoginException
                ("Configuration Error "
                 + " - useTicketCache should be set "
                 + "to true to use the ticket cache"
                 + ticketCacheName);
        if (keyTabName != null & !useKeyTab)
            throw new LoginException
                ("Configuration Error - useKeyTab should be set to true "
                 + "to use the keytab" + keyTabName);
        if (storeKey && doNotPrompt && !useKeyTab
                && !tryFirstPass && !useFirstPass)
            throw new LoginException
                ("Configuration Error - either doNotPrompt should be set to "
                 + " false or at least one of tryFirstPass, useFirstPass "
                 + "or useKeyTab must be set to true for storeKey option");
        if (renewTGT && !useTicketCache)
            throw new LoginException
                ("Configuration Error"
                 + " - either useTicketCache should be "
                 + " true or renewTGT should be false");
        if (krb5PrincName != null && krb5PrincName.toString().equals("*")) {
            if (isInitiator) {
                throw new LoginException
                    ("Configuration Error"
                    + " - principal cannot be * when isInitiator is true");
            }
        }
    }

    private boolean isCurrent(Credentials creds)
    {
        Date endTime = creds.getEndTime();
        if (endTime != null) {
            return (System.currentTimeMillis() <= endTime.getTime());
        }
        return true;
    }

    private Credentials renewCredentials(Credentials creds)
    {
        Credentials lcreds;
        try {
            if (!creds.isRenewable())
                throw new RefreshFailedException("This ticket" +
                                " is not renewable");
            if (System.currentTimeMillis() > cred.getRenewTill().getTime())
                throw new RefreshFailedException("This ticket is past "
                                             + "its last renewal time.");
            lcreds = creds.renew();
            if (debug)
                System.out.println("Renewed Kerberos Ticket");
        } catch (Exception e) {
            lcreds = null;
            if (debug)
                System.out.println("Ticket could not be renewed : "
                                + e.getMessage());
        }
        return lcreds;
    }

    /**
     * <p> This method is called if the LoginContext's
     * overall authentication succeeded
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
     * LoginModules succeeded).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * <code>login</code> method), then this method associates a
     * <code>Krb5Principal</code>
     * with the <code>Subject</code> located in the
     * <code>LoginModule</code>. It adds Kerberos Credentials to the
     *  the Subject's private credentials set. If this LoginModule's own
     * authentication attempted failed, then this method removes
     * any state that was originally saved.
     *
     * <p>
     *
     * @exception LoginException if the commit fails.
     *
     * @return true if this LoginModule's own login and commit
     *          attempts succeeded, or false otherwise.
     */

    public boolean commit() throws LoginException {

        /*
         * Let us add the Krb5 Creds to the Subject's
         * private credentials. The credentials are of type
         * KerberosKey or KerberosTicket
         */
        if (succeeded == false) {
            return false;
        } else {

            if (isInitiator && (cred == null)) {
                succeeded = false;
                throw new LoginException("Null Client Credential");
            }

            if (subject.isReadOnly()) {
                cleanKerberosCred();
                throw new LoginException("Subject is Readonly");
            }

            /*
             * Add the Principal (authenticated identity)
             * to the Subject's principal set and
             * add the credentials (TGT or Service key) to the
             * Subject's private credentials
             */

            Set<Object> privCredSet =  subject.getPrivateCredentials();
            Set<java.security.Principal> princSet  = subject.getPrincipals();
            kerbClientPrinc = new KerberosPrincipal(principal.getName());

            // create Kerberos Ticket
            if (isInitiator) {
                kerbTicket = Krb5Util.credsToTicket(cred);
                if (cred.getProxy() != null) {
                    KerberosSecrets.getJavaxSecurityAuthKerberosAccess()
                            .kerberosTicketSetProxy(kerbTicket,Krb5Util.credsToTicket(cred.getProxy()));
                }
            }

            if (storeKey && encKeys != null) {
                if (encKeys.length == 0) {
                    succeeded = false;
                    throw new LoginException("Null Server Key ");
                }

                kerbKeys = new KerberosKey[encKeys.length];
                for (int i = 0; i < encKeys.length; i ++) {
                    Integer temp = encKeys[i].getKeyVersionNumber();
                    kerbKeys[i] = new KerberosKey(kerbClientPrinc,
                                          encKeys[i].getBytes(),
                                          encKeys[i].getEType(),
                                          (temp == null?
                                          0: temp.intValue()));
                }

            }
            // Let us add the kerbClientPrinc,kerbTicket and KeyTab/KerbKey (if
            // storeKey is true)

            // We won't add "*" as a KerberosPrincipal
            if (!unboundServer &&
                    !princSet.contains(kerbClientPrinc)) {
                princSet.add(kerbClientPrinc);
            }

            // add the TGT
            if (kerbTicket != null) {
                if (!privCredSet.contains(kerbTicket))
                    privCredSet.add(kerbTicket);
            }

            if (storeKey) {
                if (encKeys == null) {
                    if (ktab != null) {
                        if (!privCredSet.contains(ktab)) {
                            privCredSet.add(ktab);
                        }
                    } else {
                        succeeded = false;
                        throw new LoginException("No key to store");
                    }
                } else {
                    for (int i = 0; i < kerbKeys.length; i ++) {
                        if (!privCredSet.contains(kerbKeys[i])) {
                            privCredSet.add(kerbKeys[i]);
                        }
                        encKeys[i].destroy();
                        encKeys[i] = null;
                        if (debug) {
                            System.out.println("Added server's key"
                                            + kerbKeys[i]);
                            System.out.println("\t\t[Krb5LoginModule] " +
                                           "added Krb5Principal  " +
                                           kerbClientPrinc.toString()
                                           + " to Subject");
                        }
                    }
                }
            }
        }
        commitSucceeded = true;
        if (debug)
            System.out.println("Commit Succeeded \n");
        return true;
    }

    /**
     * <p> This method is called if the LoginContext's
     * overall authentication failed.
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
     * LoginModules did not succeed).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * <code>login</code> and <code>commit</code> methods),
     * then this method cleans up any state that was originally saved.
     *
     * <p>
     *
     * @exception LoginException if the abort fails.
     *
     * @return false if this LoginModule's own login and/or commit attempts
     *          failed, and true otherwise.
     */

    public boolean abort() throws LoginException {
        if (succeeded == false) {
            return false;
        } else if (succeeded == true && commitSucceeded == false) {
            // login succeeded but overall authentication failed
            succeeded = false;
            cleanKerberosCred();
        } else {
            // overall authentication succeeded and commit succeeded,
            // but someone else's commit failed
            logout();
        }
        return true;
    }

    /**
     * Logout the user.
     *
     * <p> This method removes the <code>Krb5Principal</code>
     * that was added by the <code>commit</code> method.
     *
     * <p>
     *
     * @exception LoginException if the logout fails.
     *
     * @return true in all cases since this <code>LoginModule</code>
     *          should not be ignored.
     */
    public boolean logout() throws LoginException {

        if (debug) {
            System.out.println("\t\t[Krb5LoginModule]: " +
                "Entering logout");
        }

        if (subject.isReadOnly()) {
            cleanKerberosCred();
            throw new LoginException("Subject is Readonly");
        }

        subject.getPrincipals().remove(kerbClientPrinc);
           // Let us remove all Kerberos credentials stored in the Subject
        Iterator<Object> it = subject.getPrivateCredentials().iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof KerberosTicket ||
                    o instanceof KerberosKey ||
                    o instanceof KeyTab) {
                it.remove();
            }
        }
        // clean the kerberos ticket and keys
        cleanKerberosCred();

        succeeded = false;
        commitSucceeded = false;
        if (debug) {
            System.out.println("\t\t[Krb5LoginModule]: " +
                               "logged out Subject");
        }
        return true;
    }

    /**
     * Clean Kerberos credentials
     */
    private void cleanKerberosCred() throws LoginException {
        // Clean the ticket and server key
        try {
            if (kerbTicket != null)
                kerbTicket.destroy();
            if (kerbKeys != null) {
                for (int i = 0; i < kerbKeys.length; i++) {
                    kerbKeys[i].destroy();
                }
            }
        } catch (DestroyFailedException e) {
            throw new LoginException
                ("Destroy Failed on Kerberos Private Credentials");
        }
        kerbTicket = null;
        kerbKeys = null;
        kerbClientPrinc = null;
    }

    /**
     * Clean out the state
     */
    private void cleanState() {

        // save input as shared state only if
        // authentication succeeded
        if (succeeded) {
            if (storePass &&
                !sharedState.containsKey(NAME) &&
                !sharedState.containsKey(PWD)) {
                sharedState.put(NAME, username);
                sharedState.put(PWD, password);
            }
        } else {
            // remove temp results for the next try
            encKeys = null;
            ktab = null;
            principal = null;
        }
        username = null;
        password = null;
        if (krb5PrincName != null && krb5PrincName.length() != 0)
            krb5PrincName.delete(0, krb5PrincName.length());
        krb5PrincName = null;
        if (clearPass) {
            sharedState.remove(NAME);
            sharedState.remove(PWD);
        }
    }
}
