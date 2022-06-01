package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Account;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.repository.AccountRepository;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Arrays;
import java.util.List;

@Service
public class UposService {

    private final String uposBase = "C:/temp/bank/upos";

    @Autowired
    AccountRepository accountRepository;

    public void createUserUpos(Account account, Terminal terminal) throws IOException {
        File uposBase = new File(this.uposBase);
        File userUpos = new File("C:/temp/bank/" + account.getId() + "/" + terminal.getTid());
        FileUtils.copyDirectory(uposBase, userUpos);
        try (PrintWriter pw = new PrintWriter(userUpos + "/pinpad.ini")) {
            //pw.println("pinpadIpAddr=" + terminal.getIp());
            //pw.println("ipport=8888");
            pw.println("comport=9");
            pw.println("showscreens=1");
            pw.println("printerfile=cheque.txt");
        }
    }

    public boolean makePayment(Account account, Terminal terminal, int amount)  {
        String dir = "C:/temp/bank/" + account.getId() + "/" + terminal.getTid()+"/";
        Process process = null;
        try {
            process = new ProcessBuilder(dir + "loadparm.exe", "9", "1").start();
            process.waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String readCheque(Account account, Terminal terminal) throws IOException {
        String dir = "C:/temp/bank/" + account.getId() + "/" + terminal.getTid()+"/";
        try (FileInputStream fis = new FileInputStream(dir+"cheque.txt")) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer, 0, fis.available());
            String content = new String(buffer, "cp866");
            //return Arrays.asList(content.split("\n"));
            return content;
        }
    }

    public boolean deleteUserUpos(Account account, Terminal terminal){
        File userUpos = new File("C:/temp/bank/" + account.getId() + "/" + terminal.getTid()+"/");
        try {
            FileUtils.deleteDirectory(userUpos);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
