package org.funz.run;

import java.io.File;
import java.util.HashMap;
import org.funz.Project;
import org.funz.calculator.Calculator;
import org.funz.calculator.network.Session;
import org.funz.log.LogConsole;
import org.funz.run.Client.DataListener;
import org.funz.util.ASCII;
import org.funz.util.Disk;
import org.funz.util.ParserUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Marshals the network requests from GUI to Calculator.
 */
public class ClientTest {

    int port;

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(ClientTest.class.getName());
    }
    Calculator calc;
    Thread r;

    @Before
    public void setUp() throws Exception {
        System.err.println("########################################### setUp ###########################################");
        Session.REQUEST_TIMEOUT = 5000; //2s instead of 1 minute...
        File conf = new File("dist/calculator.xml");
        assert conf.exists();
        calc = new Calculator("file:dist/calculator.xml", new LogConsole(), new LogConsole());
        r = new Thread() {
            @Override
            public void run() {
                calc.run();
            }
        };
        r.start();
        Thread.sleep(1000);
        port = calc._port;
        System.err.println("########################################### /setUp ###########################################");
    }

    @After
    public void tearDown() {
        System.err.println("########################################### tearDown ###########################################");
        calc.askToStop("tearDown ClientTest");
        try {
            r.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        System.err.println("########################################### /tearDown ###########################################");
    }

    @Test
    public void test10Cases() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++ test10Cases");

        Project prj = new Project("test");
        prj.setCode("R");

        for (int i = 0; i < 10; i++) {

            Client gui = new Client("127.0.0.1", port) {
                public void log(String m) {
                    System.err.println("------------------------------------------------[CLIENT]>log " + m);
                }
            };

            System.err.println("------------------------------------------------[CLIENT]>reserve " + gui.reserve(prj, new StringBuffer(), new StringBuffer()));

            assert gui.isConnected() : "Client " + i + " not connected !";
            assert gui.isReserved() : "Client " + i + " not reserved !";

            System.err.println("------------------------------------------------[CLIENT]>newCase " + gui.newCase(new HashMap()));

            System.err.println("------------------------------------------------[CLIENT]>putFile " + gui.putFile(new File("src/test/samples/novar.R"), new File("src/test/samples/")));

            System.err.println("------------------------------------------------[CLIENT]>execute " + gui.execute("R", new DataListener() {
                public void informationLineArrived(String str) {
                    System.err.println("------------------------------------------------[CLIENT]>execute>info " + str);
                }
            }));

            System.err.println("------------------------------------------------[CLIENT]>archiveResults " + gui.archiveResults());

            System.err.println("------------------------------------------------[CLIENT]>transferArchive " + gui.transferArchive(new File("."), new Disk.ProgressObserver() {
                public void newDataBlock(int i) {
                    System.err.println("------------------------------------------------[CLIENT]>transferArchive>newDataBlock " + i);
                }

                public void setTotalSize(long l) {
                    System.err.println("------------------------------------------------[CLIENT]>transferArchive>setTotalSize " + l);
                }
            }));

            System.err.println("------------------------------------------------[CLIENT]>unreserve " + gui.unreserve());

            assert !gui.isReserved() : "Client " + i + " still reserved !";

            File results = new File("results.zip");
            assert results.exists() : "results file does not exist";
            assert results.delete() : "Could not delete file";

            gui.disconnect();
            assert !gui.isConnected() : "Client still connected !";
        }
    }

    @Test
    public void test10Batches() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++ test10Batches");

        Project prj = new Project("test");
        prj.setCode("R");

        for (int i = 0; i < 3; i++) {
            System.err.println("+++++++++++++++++++++++ STARTING Client " + i);

            Client gui = new Client("127.0.0.1", port) {
                public void log(String m) {
                    System.err.println("------------------------------------------------[CLIENT]>log " + m);
                }
            };

            System.err.println("------------------------------------------------[CLIENT]>reserve " + gui.reserve(prj, new StringBuffer(), new StringBuffer()));

            assert gui.isConnected() : "Client not connected !";
            assert gui.isReserved() : "Client not reserved !";

            System.err.println("------------------------------------------------[CLIENT]>newCase " + gui.newCase(new HashMap()));

            System.err.println("------------------------------------------------[CLIENT]>putFile " + gui.putFile(new File("src/test/samples/novar.R"), new File("src/test/samples/")));

            System.err.println("------------------------------------------------[CLIENT]>execute " + gui.execute("R", new DataListener() {
                public void informationLineArrived(String str) {
                    System.err.println("------------------------------------------------[CLIENT]>execute>info " + str);
                }
            }));

            System.err.println("------------------------------------------------[CLIENT]>archiveResults " + gui.archiveResults());

            System.err.println("------------------------------------------------[CLIENT]>transferArchive " + gui.transferArchive(new File("."), new Disk.ProgressObserver() {
                public void newDataBlock(int i) {
                    System.err.println("------------------------------------------------[CLIENT]>transferArchive>newDataBlock " + i);
                }

                public void setTotalSize(long l) {
                    System.err.println("------------------------------------------------[CLIENT]>transferArchive>setTotalSize " + l);
                }
            }));

            System.err.println("------------------------------------------------[CLIENT]>unreserve " + gui.unreserve());

            assert !gui.isReserved() : "Client " + i + " still reserved !";

            File results = new File("results.zip");
            assert results.exists() : "results file does not exist";
            assert results.delete() : "Could not delete file";

            System.err.println("+++++++++++++++++++++++ SUCCEEDED Client " + i);

            gui.disconnect();
            assert !gui.isConnected() : "Client still connected !";
        }

    }

    @Test
    public void testProtocol() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++ testProtocol");

        assert Client.getProtocol().equals(Session.getProtocol()) : "Protocol Mismatch !";
    }

    @Test
    public void testCaseFast() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++ testCaseFast ");

        File f = File.createTempFile("fast", "tmp");
        ASCII.saveFile(f, ParserUtils.getASCIIFileContent(new File("src/test/samples/novar.R")).replace("t=0", "t=" + Session.REQUEST_TIMEOUT / 10 / 1000));

        testCase(f);
    }

    @Test
    public void testCaseLong() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++ testCaseLong ");

        File f = File.createTempFile("long", "tmp");
        ASCII.saveFile(f, ParserUtils.getASCIIFileContent(new File("src/test/samples/novar.R")).replace("t=0", "t=" + Session.REQUEST_TIMEOUT * 2 / 1000));

        testCase(f);
    }

    public void testCase(File f) throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++ testCase " + f);

        Client gui = new Client("127.0.0.1", port) {
            public void log(String m) {
                System.err.println("    [CLIENT]>log " + m);
            }
        };

        Project prj = new Project("test");
        prj.setCode("R");

        System.err.println("------------------------------------------------[CLIENT]>reserve " + gui.reserve(prj, new StringBuffer(), new StringBuffer()));

        assert gui.isConnected() : "Client not connected !";
        assert gui.isReserved() : "Client not reserved !";

        System.err.println("------------------------------------------------[CLIENT]>newCase " + gui.newCase(new HashMap()));

        System.err.println("------------------------------------------------[CLIENT]>putFile " + gui.putFile(f, f.getParentFile()));

        System.err.println("------------------------------------------------[CLIENT]>execute " + gui.execute("R", new DataListener() {
            public void informationLineArrived(String str) {
                System.err.println("[CLIENT]>execute>info " + str);
            }
        }));

        System.err.println("------------------------------------------------[CLIENT]>archiveResults " + gui.archiveResults());

        System.err.println("------------------------------------------------[CLIENT]>transferArchive " + gui.transferArchive(new File("."), new Disk.ProgressObserver() {
            public void newDataBlock(int i) {
                System.err.println("------------------------------------------------[CLIENT]>transferArchive>newDataBlock " + i);
            }

            public void setTotalSize(long l) {
                System.err.println("------------------------------------------------[CLIENT]>transferArchive>setTotalSize " + l);
            }
        }));

        System.err.println("------------------------------------------------[CLIENT]>unreserve " + gui.unreserve());

        assert !gui.isReserved() : "Client still reserved !";

        File results = new File("results.zip");
        assert results.exists();
        results.delete();

        gui.disconnect();

        assert !gui.isConnected() : "Client still connected !";
    }

    @Test
    public void testCaseBreakClient() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++ testCaseBreakClient");

        Client gui = new Client("127.0.0.1", port) {
            public void log(String m) {
                System.err.println("    [CLIENT]>log " + m);
            }
        };

        Project prj = new Project("test");
        prj.setCode("R");

        System.err.println("------------------------------------------------[CLIENT]>reserve " + gui.reserve(prj, new StringBuffer(), new StringBuffer()));

        assert gui.isConnected() : "Client not connected !";
        assert gui.isReserved() : "Client not reserved !";

        System.err.println("------------------------------------------------[CLIENT]>newCase " + gui.newCase(new HashMap()));
        //Nothing then, check that server will auto-unreserve soon...

        assert calc.getActivity().startsWith("already reserved") : "Bad activity: " + calc.getActivity();

        Thread.sleep(Session.REQUEST_TIMEOUT * 2);

        assert calc.getActivity().startsWith("idle") : "Bad activity: " + calc.getActivity();

        try {
            gui._socket.close();
            gui._socket = null;
            synchronized (gui.readWatcher) {
                gui.readWatcher.notify();
            }
        } catch (Exception e) {
        }
    }

    @Test
    public void testCase() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++ testCase");

        Client gui = new Client("127.0.0.1", port) {
            public void log(String m) {
                System.err.println("    [CLIENT]>log " + m);
            }
        };

        Project prj = new Project("test");
        prj.setCode("R");

        System.err.println("------------------------------------------------[CLIENT]>reserve " + gui.reserve(prj, new StringBuffer(), new StringBuffer()));

        assert gui.isConnected() : "Client not connected !";
        assert gui.isReserved() : "Client not reserved !";

        System.err.println("------------------------------------------------[CLIENT]>newCase " + gui.newCase(new HashMap()));

        System.err.println("------------------------------------------------[CLIENT]>putFile " + gui.putFile(new File("src/test/samples/novar.R"), new File("src/test/samples/")));

        System.err.println("------------------------------------------------[CLIENT]>execute " + gui.execute("R", new DataListener() {
            public void informationLineArrived(String str) {
                System.err.println("[CLIENT]>execute>info " + str);
            }
        }));

        System.err.println("------------------------------------------------[CLIENT]>archiveResults " + gui.archiveResults());

        System.err.println("------------------------------------------------[CLIENT]>transferArchive " + gui.transferArchive(new File("."), new Disk.ProgressObserver() {
            public void newDataBlock(int i) {
                System.err.println("------------------------------------------------[CLIENT]>transferArchive>newDataBlock " + i);
            }

            public void setTotalSize(long l) {
                System.err.println("------------------------------------------------[CLIENT]>transferArchive>setTotalSize " + l);
            }
        }));

        System.err.println("------------------------------------------------[CLIENT]>unreserve " + gui.unreserve());

        assert !gui.isReserved() : "Client still reserved !";

        File results = new File("results.zip");
        assert results.exists();
        results.delete();

        gui.disconnect();

        assert !gui.isConnected() : "Client still connected !";
    }

    @Test
    public void testFailedCase() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++ testCase");

        Client gui = new Client("127.0.0.1", port) {
            public void log(String m) {
                System.err.println("    [CLIENT]>log " + m);
            }
        };

        Project prj = new Project("test");
        prj.setCode("R");

        System.err.println("------------------------------------------------[CLIENT]>reserve " + gui.reserve(prj, new StringBuffer(), new StringBuffer()));

        assert gui.isConnected() : "Client not connected !";
        assert gui.isReserved() : "Client not reserved !";

        System.err.println("------------------------------------------------[CLIENT]>newCase " + gui.newCase(new HashMap()));

        System.err.println("------------------------------------------------[CLIENT]>putFile " + gui.putFile(new File("src/test/samples/novar.R"), new File("src/test/samples/")));

        System.err.println("------------------------------------------------[CLIENT]>execute " + gui.execute("RR", new DataListener() {
            public void informationLineArrived(String str) {
                System.err.println("[CLIENT]>execute>info " + str);
            }
        }));

        System.err.println("------------------------------------------------[CLIENT]>archiveResults " + gui.archiveResults());

        System.err.println("------------------------------------------------[CLIENT]>transferArchive " + gui.transferArchive(new File("."), new Disk.ProgressObserver() {
            public void newDataBlock(int i) {
                System.err.println("------------------------------------------------[CLIENT]>transferArchive>newDataBlock " + i);
            }

            public void setTotalSize(long l) {
                System.err.println("------------------------------------------------[CLIENT]>transferArchive>setTotalSize " + l);
            }
        }));

        System.err.println("------------------------------------------------[CLIENT]>unreserve " + gui.unreserve());

        assert !gui.isReserved() : "Client still reserved !";

        File results = new File("results.zip");
        assert results.exists();
        results.delete();

        gui.disconnect();

        assert !gui.isConnected() : "Client still connected !";
    }

    @Test
    public void testReserveUnreserve() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++ testReserveUnreserve");

        Client gui = new Client("127.0.0.1", port) {
            public void log(String m) {
                System.err.println("------------------------------------------------[CLIENT]>log " + m);
            }
        };

        Project prj = new Project("test");
        prj.setCode("R");

        Session.RESERVE_TIMEOUT = 3000;

        assert gui.reserve(prj, new StringBuffer(), new StringBuffer()) : " impossible to first reserve";

        System.err.println("############################ sleeping " + Session.RESERVE_TIMEOUT / 2);
        Thread.sleep(Session.RESERVE_TIMEOUT / 2);

        assert gui.unreserve() : "Impossible to unreserve";

        gui.disconnect();
    }

    @Test
    public void testReserveTimeOut() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++ testReserveTimeOut");

        Client gui0 = new Client("127.0.0.1", port) {
            public void log(String m) {
                System.err.println("------------------------------------------------[CLIENT]>log " + m);
            }
        };

        Project prj = new Project("test");
        prj.setCode("R");

        Session.RESERVE_TIMEOUT = 3000;

        System.err.println("############################ reserve 1 ############################");
        assert gui0.reserve(prj, new StringBuffer(), new StringBuffer()) : " impossible to first reserve";

        System.err.println("############################ sleeping " + Session.RESERVE_TIMEOUT * 3 + " ############################");
        Thread.sleep(Session.RESERVE_TIMEOUT * 3);

        Client gui = new Client("127.0.0.1", port) {
            public void log(String m) {
                System.err.println("------------------------------------------------[CLIENT]>log " + m);
            }
        };

        System.err.println("############################ reserve 2 ############################");
        assert gui.reserve(prj, new StringBuffer(), new StringBuffer()) : "Impossible to next reserve";

        System.err.println("############################ sleeping " + Session.RESERVE_TIMEOUT / 3 + " ############################");
        Thread.sleep(Session.RESERVE_TIMEOUT / 3);

        System.err.println("############################ unreserve ############################");
        assert gui.unreserve() : "Impossible to next unreserve";

        gui0.disconnect();
        gui.disconnect();
    }
}
