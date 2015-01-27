/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent.tools;

import orca.nodeagent.NodeAgentServiceStub;
import orca.nodeagent.documents.DriverElement;
import orca.nodeagent.documents.ResultElement;
import orca.nodeagent.util.Serializer;
import orca.tools.axis2.Axis2ClientConfigurationManager;

import org.apache.axis2.context.ConfigurationContext;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;


public class DriverTool
{
    protected String location = "http://localhost:8080/axis2/services/NodeManagerService";
    protected String repository = null;
    protected String config = null;

    public DriverTool()
    {
    }

    public DriverTool(String location)
    {
        this.location = location;
    }

    public DriverTool(String location, String repository, String config)
    {
        if (location != null) {
            this.location = location;
        }

        this.repository = repository;
        this.config = config;
    }

    public NodeAgentServiceStub getStub() throws Exception
    {
        // if (repository != null || config != null) {
        // ConfigurationContext cc =
        // ConfigurationContextFactory.createConfigurationContextFromFileSystem(repository,
        // config);
        // return new NodeAgentServiceStub(cc, location);
        // } else {
        // return new NodeAgentServiceStub(location);
        // }
        ConfigurationContext context = Axis2ClientConfigurationManager.getInstance().getContext(
            repository,
            config);

        return new NodeAgentServiceStub(context, location);
    }

    public int installDriver(String id, String className, String pkg)
    {
        int code = 0;

        try {
            DriverElement args = new DriverElement();

            args.setDriverId(id);
            args.setClassName(className);

            // args.setPath(path);
            if (pkg != null) {
                FileDataSource ds = new FileDataSource(pkg);
                DataHandler d = new DataHandler(ds);
                args.setPkg(d);
            }

            NodeAgentServiceStub stub = getStub();
            ResultElement result = stub.installDriver(args);

            Properties p = Serializer.serialize(result.getProperties());

            // System.out.println("Exit code: " + result.getCode());
            // System.out.println("Properties: " + p.toString());
            code = result.getCode();
        } catch (Exception e) {
            e.printStackTrace();
            code = -1;
        }

        return code;
    }

    public int upgradeDriver(String id, String className, String pkg)
    {
        int code = 0;

        try {
            DriverElement args = new DriverElement();

            args.setDriverId(id);
            args.setClassName(className);

            // args.setPath(path);
            if (pkg != null) {
                FileDataSource ds = new FileDataSource(pkg);
                DataHandler d = new DataHandler(ds);
                args.setPkg(d);
            }

            NodeAgentServiceStub stub = getStub();
            ResultElement result = stub.upgradeDriver(args);

            Properties p = Serializer.serialize(result.getProperties());

            // System.out.println("Exit code: " + result.getCode());
            // System.out.println("Properties: " + p.toString());
            code = result.getCode();
        } catch (Exception e) {
            e.printStackTrace();
            code = -1;
        }

        return code;
    }

    public int uninstallDriver(String id)
    {
        int code = 0;

        try {
            DriverElement args = new DriverElement();

            args.setDriverId(id);

            NodeAgentServiceStub stub = getStub();
            ResultElement result = stub.uninstallDriver(args);

            Properties p = Serializer.serialize(result.getProperties());

            // System.out.println("Exit code: " + result.getCode());
            // System.out.println("Properties: " + p.toString());
            code = result.getCode();
        } catch (Exception e) {
            e.printStackTrace();
            code = -1;
        }

        return code;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public void setRepository(String repository)
    {
        this.repository = repository;
    }

    public void setConfig(String config)
    {
        this.config = config;
    }

    public static DriverTool getDriverTool(String[] args)
    {
        DriverTool tool = new DriverTool();

        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (args[i].equals("-l")) {
                    if ((i + 1) < args.length) {
                        tool.setLocation(args[i + 1]);
                        i++;
                    } else {
                        return null;
                    }
                } else if (args[i].equals("-r")) {
                    if ((i + 1) < args.length) {
                        tool.setRepository(args[i + 1]);
                        i++;
                    } else {
                        return null;
                    }
                }
            } else if (args[i].equals("-c")) {
                if ((i + 1) < args.length) {
                    tool.setConfig(args[i + 1]);
                    i++;
                } else {
                    return null;
                }
            }
        }

        return tool;
    }

    public static void printUsage()
    {
        System.out.println(
            "Usage [modifiers] command parameters:\n" + "Modifiers can be:\n" +
            "-l location -> location of nodemanager service\n" + "-r path -> path to repository\n" +
            "-c path -> path to axis2.xml\n" +
            "install id class [path] [pkg] --> installs the driver\n" +
            "upgrade id class [path] [pkg] --> upgrades the driver\n" +
            "uninstall id --> uninstalls the driver");
    }

    public static int processInstall(DriverTool tool, String[] args, int index)
    {
        if ((index + 1) >= args.length) {
            printUsage();

            return -1;
        } else {
            String id = args[index];
            String className = args[index + 1];

            // String path = null;
            String pkg = null;

            if ((index + 2) < args.length) {
                // path = args[index + 2];
                pkg = args[index + 2];
            }

            int code = tool.installDriver(id, className, pkg);

            // System.out.println("Exit code: " + code);
            return code;
        }
    }

    public static int processUpgradeDriver(DriverTool tool, String[] args, int index)
    {
        int i = index;

        if ((i + 1) >= args.length) {
            printUsage();

            return -1;
        } else {
            String id = args[i];
            String className = args[i + 1];

            // String path = null;
            String pkg = null;

            if ((i + 2) < args.length) {
                // path = args[i + 2];
                pkg = args[i + 2];
            }

            int code = tool.upgradeDriver(id, className, pkg);

            // System.out.println("Exit code: " + code);
            return code;
        }

        // try {
        // ConfigurationContext context =
        // Axis2ClientConfigurationManager.getInstance().getContext(null, null);
        // Assert.assertNotNull(context);
        // ConfigurationContext context2 =
        // Axis2ClientConfigurationManager.getInstance().getContext(null, null);
        // Assert.assertNotNull(context2);
        // Assert.assertSame(context, context2);
        // System.out.println("context: " + context.toString());
        // } catch (Exception e) {
        // throw new RuntimeException(e);
        // }
    }

    public static int processUninstallDriver(DriverTool tool, String[] args, int index)
    {
        int i = index;

        if (i >= args.length) {
            printUsage();

            return -1;
        } else {
            String id = args[i];
            int code = tool.uninstallDriver(id);

            // System.out.println("Exit code: " + code);
            return code;
        }
    }

    public static void main(String[] args)
    {
        if (args.length == 0) {
            printUsage();
            System.exit(-1);
        }

        ToolHelper helper = new ToolHelper(args);
        DriverTool tool = new DriverTool(helper.getLocation(),
                                         helper.getRepository(),
                                         helper.getConfig());

        int index = helper.getIndex();

        // System.out.println("Config: " + helper.getConfig());
        // System.out.println("Repository: " + helper.getRepository());
        // System.out.println("Location: " + helper.getLocation());
        if (args[index].equals("install")) {
            System.exit(processInstall(tool, args, index + 1));
        } else if (args[index].equals("upgrade")) {
            System.exit(processUpgradeDriver(tool, args, index + 1));
        } else if (args[index].equals("uninstall")) {
            System.exit(processUninstallDriver(tool, args, index + 1));
        } else {
            System.err.println("Unsupported operation");
            System.exit(-1);
        }
    }
}