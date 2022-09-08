package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.transaction.Type;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class UposService {

    private final Logger logger = LoggerFactory.getLogger(UposService.class);
    //private final String userUposDir = "C:/temp/bank/upos/";//windows
    private final String uposBase = "./upos_base";
    private final String userUposDir = "./usersUpos/";

    /**
     * The method is used to create or update upos files for given app user by app user id.
     * @param accountId app user repository ID
     * @param terminal terminal, belonging to given through id user
     * @param createNewFlag if true - upos files are copied into app user dir on system
     * @return
     */
    @Async
    public boolean setUserUpos(Long accountId, Terminal terminal, boolean createNewFlag) {
        try {
            File userUpos = new File(userUposDir + accountId + "/" + terminal.getShop().getId() + "/" + terminal.getTid());
            if (createNewFlag) {
                File upos = new File(this.uposBase);
                FileUtils.copyDirectory(upos, userUpos);
            }
            try (PrintWriter pw = new PrintWriter(userUpos + "/pinpad.ini");
                 PrintWriter script = new PrintWriter(userUpos + "/script.sh")) {
                pw.println("PinpadIPAddr=" + terminal.getIp());
                pw.println("PinpadIPPort=8888");
                pw.println("header=" + terminal.getChequeHeader());
                pw.println("printerfile=cheque.txt");
                pw.println("terminalId=" + terminal.getTid());
                pw.println("merchantId=" + terminal.getMid());
                pw.println("LockPath=" + userUpos.getAbsolutePath());
                script.println("#!/bin/sh");
                script.println("echo \"" + terminal.getIp() + "      pos" + terminal.getTid() + "_ip\" >> /etc/hosts");
            }
            new ProcessBuilder(userUpos.getAbsolutePath() + "/script.sh").start();
            return true;
        } catch (IOException e) {
            logger.error("Error while setting user UPOS: " + e.getMessage());
            return false;
        }
    }

    /**
     * The method performs acquiring operation with POS-terminal. In case of any process exception or
     * terminal's disconnection state, current operation is reckoned failed and method returns false.
     *
     * @param accountId       account repository ID
     * @param shopId          ID of shop, belonging to given account
     * @param terminalTid     TID of terminal in use
     * @param amount          payment/refund amount
     * @param transactionType type of transaction
     * @return operation state
     */
    public boolean makeOperation(Long accountId, Long shopId, String terminalTid, double amount, Type transactionType) {
        String dir = userUposDir + accountId + "/" + shopId + "/" + terminalTid + "/";
        Process process;
        try {
            switch (transactionType) {
                case PAYMENT:
                    process = new ProcessBuilder(dir + "sb_pilot", "1", (int) (amount * 100) + "").start();
                    break;
                case REFUND:
                    process = new ProcessBuilder(dir + "sb_pilot", "3", (int) (amount * 100) + "").start();
                    break;
                case CLOSE_DAY:
                    process = new ProcessBuilder(dir + "sb_pilot", "7").start();
                    break;
                case XREPORT:
                    process = new ProcessBuilder(dir + "sb_pilot", "9", "1").start();
                    break;
                case TEST:
                    process = new ProcessBuilder(dir + "sb_pilot", "47", "2").start();
                    break;
                default:
                    process = new ProcessBuilder(dir + "sb_pilot", "47", "2").start();
            }
            process.waitFor();
            return true;
        } catch (Exception e) {
            logger.error("Error while making acquiring transaction: " + e.getMessage());
            return false;
        }
    }

    public boolean makeReportOperation(Long accountId, Long shopId, String terminalTid, Type transactionType) {
        return makeOperation(accountId, shopId, terminalTid, 0, transactionType);
    }

    public boolean testPSDB(Long accountId, Long shopId, String terminalTid) {
        return makeOperation(accountId, shopId, terminalTid, 0, Type.TEST);
    }

    public String readCheque(Long accountId, Long shopId, String terminalTid) {
        String fileCheque = userUposDir + accountId + "/" + shopId + "/" + terminalTid + "/cheque.txt";
        String content = readFileContent(fileCheque);
        return content.replaceAll("=", "").stripTrailing();
    }

    public int getTransactionResponseCode(Long accountId, Long shopId, String terminalTid) {
        String fileE = userUposDir + accountId + "/" + shopId + "/" + terminalTid + "/e";
        String content = readFileContent(fileE);
        return content != "" ? Integer.parseInt(content.split(",")[0]) : -1;
    }

    public boolean deleteUserUpos(Long accountId, Long shopId, String terminalTid) {
        File userUpos = new File(userUposDir + accountId + "/" + shopId + "/" + terminalTid + "/");
        try {
            FileUtils.deleteDirectory(userUpos);
            return true;
        } catch (IOException e) {
            logger.error("Error while deleting UPOS: " + e.getMessage());
            return false;
        }
    }

    public boolean defineTransactionStatusByCheque(String cheque) {
        cheque = cheque.toLowerCase();
        return cheque != null && (cheque.contains("одобрено")
                || cheque.contains("итоги совпали")
                || cheque.contains("процессинг:работает")
                || cheque.contains("сводный чек")
                || cheque.contains("контрольная лента"));
    }

    private String readFileContent(String file) {
        try {
            return Files.readString(Paths.get(file), Charset.forName("KOI8-R"));
        } catch (IOException e) {
            logger.error("Error while parsing file: " + file + "  " + e.getMessage());
            return "";
        }
    }
}
