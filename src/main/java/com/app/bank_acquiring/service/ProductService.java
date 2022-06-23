package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ProductRepository;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ShopService shopService;

    @Transactional
    public Product getProduct(Long id, UserDetails currentUser) {
        Account current = accountRepository.findByUsername(currentUser.getUsername());
        Product product = productRepository.getOne(id);
        validateProductIdAccess(product, current);
        return product;
    }

    @Transactional
    public void deleteProduct(Long id, UserDetails currentUser) {
        Account current = accountRepository.findByUsername(currentUser.getUsername());
        Product product = productRepository.getOne(id);
        validateProductIdAccess(product, current);
        productRepository.delete(product);
    }

    @Transactional
    public void copyProducts(long[] prodsIds, Long shopId, Long targetShopId, UserDetails currentUser) {
        Shop shopFrom = shopService.getShop(shopId, currentUser); //just validation issue
        Shop shopTo = shopService.getShop(targetShopId, currentUser);
        for (int i = 0; i < prodsIds.length; i++) {
            Product oldProduct = getProduct(prodsIds[i], currentUser);
            Product newProduct = new Product();
            newProduct.setName(oldProduct.getName());
            newProduct.setType(oldProduct.getType());
            newProduct.setPurchasePrice(oldProduct.getPurchasePrice());
            newProduct.setSellingPrice(oldProduct.getSellingPrice());
            newProduct.setBarCode(oldProduct.getBarCode());
            newProduct.setVendorCode(oldProduct.getVendorCode());
            newProduct.setMeasurementUnit(oldProduct.getMeasurementUnit());
            newProduct.setShop(shopTo);
            saveProduct(newProduct);
        }
    }

    public void saveProduct(Product product) {
        productRepository.save(product);
    }

    private void validateProductIdAccess(Product product, Account account) {
        if (product != null) {
            if (!account.getShops().contains(product.getShop())) {
                throw new RuntimeException("Current user doesn't have access to this product");
            }
        }
    }


    public byte[] createExcelFile(Long accountId, Long shopId, List<Product> products) {
        Workbook workbook = generateExcel(products);
        File excelFile = new File("C:/temp/bank/" + accountId + "/" + shopId + "/products.xlsx");
        try {
            FileUtils.createParentDirectories(excelFile);
            FileOutputStream outputStream = new FileOutputStream(excelFile.getAbsolutePath());
            workbook.write(outputStream);
            workbook.close();
            return FileUtils.readFileToByteArray(excelFile);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[]{};
        }

    }

    private Workbook generateExcel(List<Product> products) {
        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Товары");
        for (int i = 0; i < 6; i++) {
            sheet.setColumnWidth(i, 4000);
        }
        Row header = sheet.createRow(0);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        headerStyle.setFont(font);

        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("Наименование");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(1);
        headerCell.setCellValue("Тип");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(2);
        headerCell.setCellValue("Артикул");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(3);
        headerCell.setCellValue("Цена закупки");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(4);
        headerCell.setCellValue("Цена продажи");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(5);
        headerCell.setCellValue("Штрихкод");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(6);
        headerCell.setCellValue("Остаток");
        headerCell.setCellStyle(headerStyle);

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            CellStyle style = workbook.createCellStyle();
            style.setWrapText(true);

            Row row = sheet.createRow(2 + i);
            Cell cell = row.createCell(0);
            if (product.getName() != null) {
                cell.setCellValue(product.getName());
            }
            cell.setCellStyle(style);

            cell = row.createCell(1);
            if (product.getType() != null) {
                cell.setCellValue(product.getType().getExplanation());
            }
            cell.setCellStyle(style);

            cell = row.createCell(2);
            if (product.getVendorCode() != null) {
                cell.setCellValue(product.getVendorCode());
            }
            cell.setCellStyle(style);

            cell = row.createCell(3);
            if (product.getPurchasePrice() != null) {
                cell.setCellValue(product.getPurchasePrice().toString());
            }
            cell.setCellStyle(style);

            cell = row.createCell(4);
            if (product.getSellingPrice() != null) {
                cell.setCellValue(product.getSellingPrice().toString());
            }
            cell.setCellStyle(style);

            cell = row.createCell(5);
            cell.setCellValue(product.getBarCode());
            cell.setCellStyle(style);

            cell = row.createCell(6);
            cell.setCellValue(product.getBalance());
            cell.setCellStyle(style);
        }
        return workbook;
    }

}
