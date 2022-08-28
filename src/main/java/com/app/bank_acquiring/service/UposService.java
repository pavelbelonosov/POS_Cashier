package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.transaction.Type;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class UposService {

    private final Logger logger = LoggerFactory.getLogger(UposService.class);
    //private final String uposBase = "C:/temp/bank/upos";
    private final String uposBase = "./upos_base";
    private final String userUposDir = "/usr/src/app/usersUpos/";

    @Async
    public boolean createUserUpos(Long accountId, Terminal terminal) {
        try {
            File upos = new File(this.uposBase);
            File userUpos = new File(userUposDir + accountId + "/" + terminal.getShop().getId() + "/" + terminal.getTid());
            FileUtils.copyDirectory(upos, userUpos);
            try (PrintWriter pw = new PrintWriter(userUpos + "/pinpad.ini")) {
                pw.println("PinpadIPAddr=" + terminal.getIp());
                pw.println("PinpadIPPort=8888");
                pw.println("header=" + terminal.getChequeHeader());
                //pw.println("comport=9");
                //pw.println("showscreens=1");
                pw.println("printerfile=cheque.txt");
                pw.println("terminalId=" + terminal.getTid());
                pw.println("merchantId=" + terminal.getMid());
            }
            return true;
        } catch (IOException e) {
            logger.error("Error while creating user UPOS: " + e.getMessage());
            return false;
        }
    }

    @Async
    public boolean updateUposSettings(Long accountId, Terminal terminal) {
        File pinpadIni = new File(userUposDir + accountId + "/" + terminal.getShop().getId() + "/" + terminal.getTid() + "/pinpad.ini");
        try (PrintWriter pw = new PrintWriter(pinpadIni)) {
            pw.println("PinpadIPAddr=" + terminal.getIp());
            pw.println("PinpadIPPort=8888");
            pw.println("header=" + terminal.getChequeHeader());
            //pw.println("comport=99");
            //pw.println("showscreens=0");
            pw.println("printerfile=cheque.txt");
            return true;
        } catch (FileNotFoundException e) {
            logger.error("Error while updating pinpad settings: " + e.getMessage());
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
                    //process = new ProcessBuilder(dir + "loadparm.exe", "9", "1").start();
                    process = new ProcessBuilder(dir + "sb_pilot.exe", "1", (int) (amount * 100) + "").start();
                    break;
                case REFUND:
                    //process = new ProcessBuilder(dir + "loadparm.exe", "9", "1").start();
                    process = new ProcessBuilder(dir + "sb_pilot.exe", "3", (int) (amount * 100) + "").start();
                    break;
                case CLOSE_DAY:
                    process = new ProcessBuilder(dir + "sb_pilot.exe", "7").start();
                    break;
                case XREPORT:
                    process = new ProcessBuilder(dir + "sb_pilot.exe", "9", "1").start();
                    break;
                case TEST:
                    process = new ProcessBuilder(dir + "sb_pilot.exe", "47", "2").start();
                    break;
                default:
                    process = new ProcessBuilder(dir + "sb_pilot.exe", "47", "2").start();
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
        String dir = userUposDir + accountId + "/" + shopId + "/" + terminalTid + "/";
        try (FileInputStream fis = new FileInputStream(dir + "cheque.txt")) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer, 0, fis.available());
            String content = new String(buffer, "cp866");
            //return Arrays.asList(content.split("\n"));
            return content.replaceAll("=", "").stripTrailing();
        } catch (Exception e) {
            logger.error("Error while parsing cheque: " + e.getMessage());
            return "";
        }
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

    public boolean defineTransactionStatus(String cheque) {
        cheque = cheque.toLowerCase();
        return cheque != null && (cheque.contains("одобрено")
                || cheque.contains("итоги совпали")
                || cheque.contains("процессинг:работает")
                || cheque.contains("сводный чек")
                || cheque.contains("контрольная лента"));

    }
}
