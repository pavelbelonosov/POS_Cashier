package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.repository.AccountRepository;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class UposService {

    private final String uposBase = "C:/temp/bank/upos";

    @Autowired
    private AccountRepository accountRepository;

    public void createUserUpos(Long accountId, Terminal terminal) throws IOException {
        File upos = new File(this.uposBase);
        File userUpos = new File("C:/temp/bank/" + accountId + "/" + terminal.getTid());
        FileUtils.copyDirectory(upos, userUpos);
        try (PrintWriter pw = new PrintWriter(userUpos + "/pinpad.ini")) {
            //pw.println("pinpadIpAddr=" + terminal.getIp());
            //pw.println("ipport=8888");
            pw.println("header=" + terminal.getChequeHeader());
            pw.println("comport=9");
            pw.println("showscreens=1");
            pw.println("printerfile=cheque.txt");
        }
    }

    public void updateUposSettings(Long accountId, Terminal terminal) throws FileNotFoundException {
        File pinpadIni = new File("C:/temp/bank/" + accountId + "/" + terminal.getTid() + "/pinpad.ini");
        try (PrintWriter pw = new PrintWriter(pinpadIni)) {
            //pw.println("pinpadIpAddr=" + terminal.getIp());
            //pw.println("ipport=8888");
            pw.println("header=" + terminal.getChequeHeader());
            pw.println("comport=99");
            pw.println("showscreens=0");
            pw.println("printerfile=cheque.txt");
        }
    }

    public boolean makePayment(Long accountId, String terminalTid, double amount) {
        String dir = "C:/temp/bank/" + accountId + "/" + terminalTid + "/";
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

    public String readCheque(Long accountId, String terminalTid) throws IOException {
        String dir = "C:/temp/bank/" + accountId + "/" + terminalTid + "/";
        try (FileInputStream fis = new FileInputStream(dir + "cheque.txt")) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer, 0, fis.available());
            String content = new String(buffer, "cp866");
            //return Arrays.asList(content.split("\n"));
            return content;
        }
    }

    public boolean deleteUserUpos(Long accountId, String terminalTid) {
        File userUpos = new File("C:/temp/bank/" + accountId + "/" + terminalTid + "/");
        try {
            FileUtils.deleteDirectory(userUpos);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean defineTransactionStatus(String cheque) {
        if (cheque.contains("одобрено") || cheque.contains("итоги совпали")) {
            return true;
        }
        return false;
    }
}
