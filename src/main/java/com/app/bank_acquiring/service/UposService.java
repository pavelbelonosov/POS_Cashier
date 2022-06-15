package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.repository.AccountRepository;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class UposService {

    private final String uposBase = "C:/temp/bank/upos";

    @Async
    public boolean createUserUpos(Long accountId, Terminal terminal) {
        try {
            File upos = new File(this.uposBase);
            File userUpos = new File("C:/temp/bank/" + accountId + "/" + terminal.getShop().getId() + "/" + terminal.getTid());
            FileUtils.copyDirectory(upos, userUpos);
            try (PrintWriter pw = new PrintWriter(userUpos + "/pinpad.ini")) {
                //pw.println("pinpadIpAddr=" + terminal.getIp());
                //pw.println("ipport=8888");
                pw.println("header=" + terminal.getChequeHeader());
                pw.println("comport=9");
                pw.println("showscreens=1");
                pw.println("printerfile=cheque.txt");
                pw.println("terminalId=" + terminal.getTid());
                pw.println("merchantId=" + terminal.getMid());
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Async
    public boolean updateUposSettings(Long accountId, Terminal terminal) {
        File pinpadIni = new File("C:/temp/bank/" + accountId + "/" + terminal.getShop().getId() + "/" + terminal.getTid() + "/pinpad.ini");
        try (PrintWriter pw = new PrintWriter(pinpadIni)) {
            //pw.println("pinpadIpAddr=" + terminal.getIp());
            //pw.println("ipport=8888");
            pw.println("header=" + terminal.getChequeHeader());
            pw.println("comport=99");
            pw.println("showscreens=0");
            pw.println("printerfile=cheque.txt");
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    public boolean testPSDB(Long accountId, Long shopId, String terminalTid) {
        String dir = "C:/temp/bank/" + accountId + "/" + shopId + "/" + terminalTid + "/";
        Process process;
        try {
            process = new ProcessBuilder(dir + "loadparm.exe", "47", "2").start();
            //process = new ProcessBuilder(dir + "loadparm.exe", "1", (int)(amount * 100) + "").start();
            process.waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean makePayment(Long accountId, Long shopId, String terminalTid, double amount) {
        String dir = "C:/temp/bank/" + accountId + "/" + shopId + "/" + terminalTid + "/";
        Process process;
        try {
            process = new ProcessBuilder(dir + "loadparm.exe", "9", "1").start();
            //process = new ProcessBuilder(dir + "loadparm.exe", "1", (int)(amount * 100) + "").start();
            process.waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String readCheque(Long accountId, Long shopId, String terminalTid) {
        String dir = "C:/temp/bank/" + accountId + "/" + shopId + "/" + terminalTid + "/";
        try (FileInputStream fis = new FileInputStream(dir + "cheque.txt")) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer, 0, fis.available());
            String content = new String(buffer, "cp866");
            //return Arrays.asList(content.split("\n"));
            return content;
        } catch (Exception e) {
            return "";
        }
    }

    public boolean deleteUserUpos(Long accountId, Long shopId, String terminalTid) {
        File userUpos = new File("C:/temp/bank/" + accountId + "/" + shopId + "/" + terminalTid + "/");
        try {
            FileUtils.deleteDirectory(userUpos);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean defineTransactionStatus(String cheque) {

        if (cheque!=null&&(cheque.contains("одобрено") || cheque.contains("итоги совпали")
                || cheque.contains("Процессинг:работает"))) {
            return true;
        }
        return false;
    }
}
