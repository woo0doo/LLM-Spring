package org.kcc.llm.service;

import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ExcelService {

    // 만약 여러 열을 읽고 싶다면, 예시로 이런 식 메서드도 가능
    public List<Map<String, String>> parseExcelAllColumns(InputStream excelInputStream) {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(excelInputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row == null) continue;

                // 각 셀(0 ~ row.getLastCellNum()-1) 반복
                Map<String, String> rowMap = new HashMap<>();
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null) {
                        // 셀 타입에 맞게 변환
                        String value = cell.toString(); // 간단 변환
                        // 열 이름을 키로 쓸 수도, 'Column_c'처럼 쓸 수도
                        rowMap.put("Column_" + c, value);
                    }
                }
                rows.add(rowMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }
}
