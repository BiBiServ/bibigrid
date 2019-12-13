package de.unibi.cebitec.bibigrid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


public class LoggerConfigurationTest {

    static Logger LOG = LoggerFactory.getLogger(LoggerFactory.class);

    public static void main(String [] args){
        MDC.put("first","HILFE");

        new Thread(new T("Apfel")).start();
        new Thread(new T("Birne")).start();
        new Thread(new T("Banane")).start();
        new Thread(new T("Kiwi")).start();
    }
}


class T implements Runnable {

    String context;

    static Logger LOG = LoggerFactory.getLogger(T.class);

    public T(String context ){
        this.context = context;
        System.out.println(context);
        MDC.put("first",context);
        LOG.info("Test");
        LOG.info("Noch nen test");
    }

    @Override
    public void run() {
        MDC.put("first",context);
        for (int i =0; i < 10; i++) {
            LOG.error("Call "+i);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
