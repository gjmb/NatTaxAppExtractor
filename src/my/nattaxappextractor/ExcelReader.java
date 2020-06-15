/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.nattaxappextractor;

import java.awt.Component;
import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import javax.swing.JOptionPane;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author gabri
 *
 *
 */

public class ExcelReader {

     public static String[] condNames = {"MARCEL", "TERRAZZA MAGGIORE", "LE MONT", "ARRUDA BOTELHO", "PORTO FINO"};
     public static String[] condKeysG = {"0082", "0085", "0088", "0089", "0090"};

    public static String[] getFilesNames(String directory) {
        File folder = new File(directory);
        File[] listOfFiles = folder.listFiles();
        String[] fileNames = new String[listOfFiles.length];
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                //String [] splitedName=listOfFiles[i].getName().split(".");
                //List<String> l = Arrays.asList(splitedName);
                //  if(l.contains("xls"))
                fileNames[i] = listOfFiles[i].getName();
            }
        }
        return fileNames;
    }

    /**
     * @param args the command line arguments
     */
    public static Map<String, CondominiumG> extractor (String source) {
        // TODO code application logic here
        if (source.equals("") || source.equals("Nenhuma pasta selecionada.")) {
                    Component frame = null;
                    JOptionPane.showMessageDialog(frame,
                            "selecione a pasta onde os relatórios estão armazenados.",
                            "Pasta não selecionada",
                            JOptionPane.ERROR_MESSAGE);
        }
        int count=0;
        Map<String, CondominiumG> condMapG = new HashMap<>();
        for (int i = 0; i < condNames.length; i++) {
            condMapG.put(condKeysG[i], new CondominiumG(condNames[i]));
        }
        source = source.replace("\\","/");
        //System.out.println(source);
        String[] fileNames = getFilesNames(source);
        for (int i = 0; i < fileNames.length; i++) {
            String[] splitedName = fileNames[i].split("\\.");
            //System.out.println(splitedName.length);
            List<String> l = Arrays.asList(splitedName);
            if (l.contains("xls")) {
                ArrayList<String> lines = new ArrayList<>();
                try {
                    FileInputStream file = new FileInputStream(new File(source+ "/" + fileNames[i]));

                    //Create Workbook instance holding reference to .xlsx file
                    XSSFWorkbook workbook = new XSSFWorkbook(file);

                    //Get first/desired sheet from the workbook
                    XSSFSheet sheet = workbook.getSheetAt(0);

                    //Iterate through each rows one by one
                    Iterator<Row> rowIterator = sheet.iterator();
                    FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
                    int index = 0;
                    while (rowIterator.hasNext()) {
                        Row row = rowIterator.next();
                        lines.add("");

                        //For each row, iterate through all the columns
                        Iterator<Cell> cellIterator = row.cellIterator();

                        while (cellIterator.hasNext()) {
                            Cell cell = cellIterator.next();
                            //Check the cell type and format accordingly
                            switch (formulaEvaluator.evaluateInCell(cell).getCellTypeEnum()) {
                                case NUMERIC:
                                    lines.set(index, lines.get(index) + cell.getNumericCellValue() + " ");
                                    break;
                                case STRING:
                                    lines.set(index, lines.get(index) + cell.getStringCellValue() + " ");
                                    break;
                                default:
                                    break;

                            }
                        }
                        index++;
                    }
                    file.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //for(int i = 0;i<lines.size();i++ ){
                //   System.out.println(lines.get(i));
                // }
                CondominiumG cond = null;
                String balance = "";

                String[] lineFrag = lines.get(3).split(" ");
                String key = lineFrag[1];
                //System.out.println(key);
                if (condMapG.containsKey(key)) {
                    cond = condMapG.get(key);
                    count++;
                } else {
                    cond = null;
                }

                if (cond != null) {
                    String[] b = lines.get(8).split(" ");
                    l = Arrays.asList(b);
                    //Primeira pagina
                    boolean band = false;
                    if (cond.name.equals("BANDEIRANTES")) {
                        b = lines.get(10).split(" ");
                        band = true;
                    }

                    if ((l.contains("SALDO") && l.contains("ANTERIOR")) || band) {
                        //System.out.println(lines.size());
                        balance = b[2];

                        int index = 9;
                        if (band) {
                            index = 11;
                        }

                        for (int j = index; j < lines.size(); j++) {

                            b = lines.get(j).split(" ");

                            String[] dt = null;
                            if (b.length > 2) {
                                dt = b[1].split("/");
                            } else {
                                continue;
                            }
                            if (dt.length != 3) {
                                dt = b[0].split("/");
                                if (dt.length != 3) {
                                    continue;
                                }
                            }
                            //System.out.println(b[1]);
                            //System.out.println(lines.get(j));
                            String bal = b[b.length - 2];

                            //DEBITO
                            //System.out.println(balance+" "+ bal);
                            if (Double.parseDouble(balance) > Double.parseDouble(bal)) {
                                cond.debitEntry.add(new Entry(lines.get(j)));
                                balance = bal;
                                //CREDITO
                            } else {
                                cond.creditEntry.add(new Entry(lines.get(j)));
                                balance = bal;
                            }

                        }//for int j = index; j < lines.size(); j++

                    } //if l.contains("SALDO") && l.contains("ANTERIOR")) || band

                }// if cond!=null 

                // System.out.println(fileNames[i]);
            }
        }
        //System.out.println(condMapG.size() + " " +condNames.length + " " + condKeysG.length);
        if(count<5){
                    Component frame = null;
                    JOptionPane.showMessageDialog(frame,
                            "Verifique se todos os relatórios estão armazenados na pasta selecionada.",
                            "Relatório(s) não encontrados",
                            JOptionPane.ERROR_MESSAGE);
        }
     return condMapG;
     
    }// MAIN                             
}
