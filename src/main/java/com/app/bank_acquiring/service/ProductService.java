package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ProductRepository;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

@Service
@AllArgsConstructor
public class ProductService {
    private final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private ProductRepository productRepository;
    private AccountRepository accountRepository;
    private ShopService shopService;

    @Transactional
    @Cacheable(value = "products", key = "#id")
    public Product getProduct(@NonNull Long id, @NonNull String currentUser) {
        Account current = accountRepository.findByUsername(currentUser);
        Product product = productRepository.getOne(id);
        validateProductIdAccess(product, current);
        return product;
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(@NonNull Long id, @NonNull String currentUser) {
        Account current = accountRepository.findByUsername(currentUser);
        Product product = productRepository.getById(id);
        validateProductIdAccess(product, current);
        productRepository.delete(product);
    }

    @Transactional
    public void copyProducts(long[] prodsIds, Long shopId, Long targetShopId, String currentUser) {
        Shop shopFrom = shopService.getShop(shopId, currentUser); //validation issue
        Shop shopTo = shopService.getShop(targetShopId, currentUser);

        if (prodsIds != null) {
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
    }

    @CachePut(value = "products", key = "#product.id")
    public Product saveProduct(Product product) {
        return product != null ? productRepository.save(product) : null;
    }

    private void validateProductIdAccess(Product product, Account account) {
        try {
            if (product == null || account == null || account.getShops() == null
                    || !account.getShops().contains(product.getShop())) {
                logger.error("ID validation error: given account(id "
                        + (account != null ? account.getId() : "not valid") + ") doesn't have permission to this product(id "
                        + (product!=null?product.getId():"not valid") + ")");
                throw new IdValidationException("Current user doesn't have access to this product");
            }
        } catch (EntityNotFoundException e) {
            logger.error("ID validation error: entity not exist");
            throw new IdValidationException("Current user doesn't have access to this product");
        }
    }


    public byte[] createExcelFile(Long accountId, Long shopId, List<Product> products) {
        try {
            if (accountId == null || shopId == null || products == null) throw new IllegalArgumentException();
            Workbook workbook = generateExcel(products);
            File excelFile = new File("/usr/src/app/usersUpos/" + accountId + "/" + shopId + "/products.xlsx");
            FileUtils.createParentDirectories(excelFile);
            FileOutputStream outputStream = new FileOutputStream(excelFile.getAbsolutePath());
            workbook.write(outputStream);
            workbook.close();

            logger.info("ProductsExcel file successfully created at: " + excelFile.getAbsolutePath());
            return FileUtils.readFileToByteArray(excelFile);
        } catch (Exception e) {
            logger.error("Error while creating excel file: " + e.getMessage());
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
