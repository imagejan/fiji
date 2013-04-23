package edu.utexas.clm.archipelago.network.shell;


import com.jcraft.jsch.*;
import edu.utexas.clm.archipelago.exception.ShellExecutionException;
import edu.utexas.clm.archipelago.listen.NodeShellListener;
import edu.utexas.clm.archipelago.network.node.NodeManager;
import edu.utexas.clm.archipelago.network.shell.ssh.JSchUtility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

public class SSHNodeShell implements NodeShell
{

    private static final SSHNodeShell shell = new SSHNodeShell();
    
    protected String getArguments(final NodeManager.NodeParameters param,
                                  NodeShellListener listener)
    {
        final String eroot = param.getExecRoot();
        return "--jar-path " + eroot +
                "/plugins/ --jar-path " + eroot +"/jars/ --classpath " + eroot +
                " --allow-multiple --main-class edu.utexas.clm.archipelago.Fiji_Archipelago "
                + param.getID() + " 2>&1 > ~/" + param.getHost() + "_" + param.getID() + ".log";
    }
    
    protected void handleJSE(final JSchException jse, final NodeManager.NodeParameters param)
            throws ShellExecutionException
    {
        if (jse.getMessage().equals("Auth cancel"))
        {
            throw new ShellExecutionException(
                    "Authentication failed on " + param.getHost(), jse);
        }
        else if(jse.getCause() != null && jse.getCause() instanceof UnknownHostException)
        {
            throw new ShellExecutionException("Unknown host " + param.getHost(), jse);
        }

        throw new ShellExecutionException(jse);
    }
    
    public boolean start(final NodeManager.NodeParameters param, final NodeShellListener listener)
            throws ShellExecutionException
    {

        try
        {
            final JSchUtility util = new JSchUtility(param, listener, getArguments(param, listener));

            if (util.fileExists())
            {
                Channel c = util.exec();
                InputStream is = c.getInputStream();
                OutputStream os = c.getOutputStream();
                listener.ioStreamsReady(is, os);
                return true;
            }
            else
            {
                return false;
            }
        }
        catch (JSchException jse)
        {
            handleJSE(jse, param);
            return false;
        }
        catch (IOException ioe)
        {
            throw new ShellExecutionException(ioe);
        }
    }
    
    public NodeShellParameters defaultParameters()
    {
        final String userHome = System.getProperty("user.home");
        final String sep = "/";
        final NodeShellParameters nsp = new NodeShellParameters();
        nsp.addKey("ssh-port", 22);
        nsp.addKey("keyfile", new File(userHome + sep + ".ssh" + sep + "id_dsa"));
        nsp.addKey("executable", "fiji");
        return nsp;
    }

    public String paramToString(NodeShellParameters nsp)
    {
        try
        {
            return ":" + nsp.getInteger("ssh-port");
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public String name()
    {
        return "SSH Shell";
    }

    public String description()
    {
        return "Uses ssh streams to connect to remote nodes";
    }

    public static SSHNodeShell getShell()
    {
        return shell;
    }

}
