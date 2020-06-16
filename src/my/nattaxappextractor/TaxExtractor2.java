/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.nattaxappextractor;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.DocumentException;
import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;

public class TaxExtractor2 {

    public static String[] taxesNames = {"TARIFA", "DOC/TED", "IOF", "ANUIDADE", "ENCARGOS", "ENCARGO", "TAR"};
    public static String[] retrieve = {"BAIXA", "RESGATE", "BX"};
    public static String[] investment = {"APLIC.INVEST", "APLICACAO", "APLICACOES"};
    public static String[] debit = {"PAGTO", "CONTA", "TRANSF", "GASTOS", "CHEQUE", "DEBITO", "DIF.TITUL.CC","BRADESCO"};
    public static String[] credit = {"DEPOSITO", "LIQUIDACAO", "TED-TRANSF"};

    public static String[] condNames = {"MARCEL", "TERRAZZA MAGGIORE", "LE MONT", "ARRUDA BOTELHO", "PORTO FINO"};

    public static String[] condKeys = {"054.962.014/0001-15", "006.047.003/0001-67", "006.187.015/0001-97", "054.531.389/0001-20", "060.912.037/0001-18"};

    @SuppressWarnings("unchecked")
    public static void disableAccessWarnings() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);

            Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
            Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
            putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
        } catch (Exception ignored) {
        }
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            String str = strNum.replace(".", "");
            str = str.replace(",", ".");
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static void print(Map<String, Condominium> condMap, String dest) {

        try {
            BufferedWriter StrW = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest + "/Aplicacoes_Resgates.csv"), "UTF-8"));

            for (int i = 0; i < condKeys.length; i++) {
                Condominium c = condMap.get(condKeys[i]);
                String date = "";
                String value = "=";
                //System.out.println(c.name);
                StrW.write(c.name + "\n");
                for (int j = 0; j < c.appRescDate.size(); j++) {
                    if (!date.equals(c.appRescDate.get(j))) {
                        if (j == 0) {
                            date = c.appRescDate.get(j);
                            value = value + c.appRescValue.get(j).replace(".", "");
                        } else {
                            //System.out.println(date+" "+value.replace(".", ","));
                            StrW.write(date + ";" + value + "\n");
                            date = c.appRescDate.get(j);
                            value = "=" + c.appRescValue.get(j).replace(".", "");
                        }
                    } else {
                        value = value + "+" + c.appRescValue.get(j).replace(".", "");
                    }
                }
                //System.out.println(date+" "+value.replace(".", ","));
                StrW.write(date + ";" + value + "\n");
            }
            StrW.close();
        } catch (FileNotFoundException ex) {

            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //TODO: RESOLVER BUG DO DIA 31

    public static void printT(Map<String, Condominium> condMap, String dest) {

        try {
            BufferedWriter StrW = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest + "/Tarifas.csv"), "UTF-8"));

            for (int i = 0; i < condKeys.length; i++) {
                Condominium c = condMap.get(condKeys[i]);
                String date = "";
                String value = "=";
                String sum = "=";
                //System.out.println(c.name);
                StrW.write(c.name + "\n");
                for (int j = 0; j < c.taxDate.size(); j++) {
                    if (c.taxDate.get(j) == null) {
                        Component frame = null;
                        JOptionPane.showMessageDialog(frame,
                                "Verifique se o mês selecionado é o mesmo que o dos extratos.",
                                "Mês errado",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    if (!date.equals(c.taxDate.get(j))) {
                        if (j == 0) {
                            date = c.taxDate.get(j);
                            value = value + c.taxValue.get(j).replace(".", "");
                            sum = sum + c.taxValue.get(j).replace(".", "");
                        } else {
                            //System.out.println(date+" "+value.replace(".", ","));
                            StrW.write(date + ";" + value + "\n");
                            date = c.taxDate.get(j);
                            value = "=" + c.taxValue.get(j).replace(".", "");
                            sum = sum + "+" + c.taxValue.get(j).replace(".", "");
                        }
                    } else {
                        value = value + "+" + c.taxValue.get(j).replace(".", "");
                        sum = sum + "+" + c.taxValue.get(j).replace(".", "");
                    }
                }
                //System.out.println(date+" "+value.replace(".", ","));
                StrW.write(date + ";" + value + "\n");
                StrW.write("" + ";" + sum + "\n");
            }

            StrW.close();

        } catch (FileNotFoundException ex) {

            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Condominium> extractor(String source, String dest, String m) throws IOException, DocumentException {
        disableAccessWarnings();
        Map<String, Condominium> condMap = new HashMap<>();
        String month = m;
        source = source.replace("\\", "/");
        dest = dest.replace("\\", "/");

        for (int i = 0; i < condNames.length; i++) {
            condMap.put(condKeys[i], new Condominium(condNames[i]));
        }

        String files[] = {"/82.pdf", "/85.pdf", "/88.pdf", "/89.pdf", "/90.pdf"};

        for (int h = 0; h < files.length; h++) {
            PdfReader reader = null;
            try {
                reader = new PdfReader(source + files[h]);
            } catch (IOException e) {
                if (source.equals("") || source.equals("Nenhuma pasta selecionada.")) {
                    Component frame = null;
                    JOptionPane.showMessageDialog(frame,
                            "selecione a pasta onde os extratos estão armazenados.",
                            "Pasta não selecionada",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    Component frame = null;
                    JOptionPane.showMessageDialog(frame,
                            "Verifique se todos os extratos estão na pasta selecionada e identificados apenas com os números dos prédios.",
                            "Extrato(s) não encontrado(s)",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
            int totalPages = reader.getNumberOfPages();
            String textFromPage = "";
            Condominium cond = null;
            String lastStringFromPage = null;
            String[] lineFrag = null;
            String date = null;
            List<String> lineList = null;
            List<String> ToplineList = null;
            String[] dateSplit = null;
            boolean end = false;

            for (int i = 1; i <= totalPages; i++) {
                if (end) {
                    break;
                }
                textFromPage = PdfTextExtractor.getTextFromPage(reader, i);
                String[] lines = textFromPage.split("\\r?\\n");
                if (i == 1) {
                    lineFrag = lines[1].split(" ");
                    if (lineFrag[lineFrag.length - 2].equals("CNPJ:")) {
                        cond = condMap.get(lineFrag[lineFrag.length - 1]);
                    }
                }
                if (cond != null) {
                    int j = 0;
                    //PRIMEIRA PAGINA
                    if (i == 1) {
                        j = 8;
                    }
                    while (j < lines.length) {
                        //System.out.println(lines[j]);
                        lineFrag = lines[j].split(" ");
                        //CASO A LINHA CONTENHA UMA DATA
                        dateSplit = lineFrag[0].split("/");
                        if (dateSplit.length == 3) {
                            if (Integer.parseInt(dateSplit[1]) > Integer.parseInt(month)) {
                                break;
                            }
                        }
                        if (dateSplit.length == 3 && dateSplit[1].equals(month)) {
                            date = lineFrag[0];
                            //System.out.println(date+""+v);
                            if (isNumeric(lineFrag[lineFrag.length - 1])) {
                                lineList = Arrays.asList(lineFrag);
                                if (lineList.contains("Total")) {
                                    end = true;
                                    break;
                                }
                                //RESGATE: valor negativo
                                String v = null;
                                if (lineList.contains(retrieve[0]) || lineList.contains(retrieve[1]) || lineList.contains(retrieve[2])) {
                                    v = lineFrag[lineFrag.length - 2];
                                    cond.appRescDate.add(date);
                                    cond.appRescValue.add("-" + v);
                                    j++;
                                    continue;
                                    //APLICACAO: valor positivo
                                } else if (lineList.contains(investment[0]) || lineList.contains(investment[1]) || lineList.contains(investment[2])) {
                                    v = lineFrag[lineFrag.length - 2].replace("-", "");
                                    cond.appRescDate.add(date);
                                    cond.appRescValue.add(v);
                                    j++;
                                    continue;
                                    //TARIFA
                                } else if (lineList.contains(taxesNames[0]) || lineList.contains(taxesNames[1]) || lineList.contains(taxesNames[2])
                                        || lineList.contains(taxesNames[3]) || lineList.contains(taxesNames[4]) || lineList.contains(taxesNames[5])
                                        || lineList.contains(taxesNames[6])) {
                                    v = lineFrag[lineFrag.length - 2].replace("-", "");
                                    //System.out.println(date+" "+v);
                                    cond.taxDate.add(date);
                                    cond.taxValue.add(v);
                                    j++;
                                    continue;
                                    //TODO: PAGAMENTOS E RECEBIMENTOS

                                } else if (lineList.contains(debit[0]) || lineList.contains(debit[1]) || lineList.contains(debit[2])
                                        || lineList.contains(debit[3]) || lineList.contains(debit[4]) || lineList.contains(debit[5])
                                        || lineList.contains(debit[6]) || lineList.contains(debit[7])) {
                                    v = lineFrag[lineFrag.length - 2].replace(".", "");
                                    v = v.replace(",", ".");
                                    if (Double.valueOf(v) < 0) {
                                        cond.debitEntry.add(new Entry(lines[j]));
                                        j++;   
                                        continue;
                                    }
                                } else if (lineList.contains(credit[0]) || lineList.contains(credit[1])) {
                                    v = lineFrag[lineFrag.length - 2].replace(".", "");
                                    v = v.replace(",", ".");
                                    if (Double.valueOf(v) > 0) {
                                        cond.creditEntry.add(new Entry(lines[j]));
                                        j++;
                                        continue;
                                    }

                                    //CASO DE LINHA QUEBRADA COM DATA
                                } else if (isNumeric(lineFrag[lineFrag.length - 1])) {
                                    String[] topRowFrag = lines[j - 1].split(" ");
                                    ToplineList = Arrays.asList(topRowFrag);
                                    lineList = Arrays.asList(lineFrag);
                                    //RESGATE: valor negativo
                                    if (lineList.contains(retrieve[0]) || lineList.contains(retrieve[1]) || lineList.contains(retrieve[2])
                                            || ToplineList.contains(retrieve[0]) || ToplineList.contains(retrieve[1]) || ToplineList.contains(retrieve[2])) {
                                        v = lineFrag[lineFrag.length - 2];
                                        if (!lineList.contains("CHEQUE") && !lineList.contains("GASTOS")) {
                                            cond.appRescDate.add(date);
                                            cond.appRescValue.add("-" + v);
                                            j++;
                                            continue;
                                        } else {
                                            cond.debitEntry.add(new Entry(date + " " + lines[j]));
                                            j++;
                                            continue;
                                        }
                                        //APLICACAO: valor positivo
                                    } else if (lineList.contains(investment[0]) || lineList.contains(investment[1]) || lineList.contains(investment[2])
                                            || ToplineList.contains(investment[0]) || ToplineList.contains(investment[1]) || ToplineList.contains(investment[2])) {
                                        v = lineFrag[lineFrag.length - 2].replace("-", "");
                                        if (!lineList.contains("CHEQUE") && !lineList.contains("GASTOS")) {
                                            cond.appRescDate.add(date);
                                            cond.appRescValue.add(v);
                                            j++;
                                            continue;
                                        } else {
                                            cond.debitEntry.add(new Entry(date + " " + lines[j]));
                                            j++;
                                            continue;
                                        }
                                        //TARIFA
                                    } else if (lineList.contains(taxesNames[0]) || lineList.contains(taxesNames[1]) || lineList.contains(taxesNames[2])
                                            || lineList.contains(taxesNames[3]) || lineList.contains(taxesNames[4]) || lineList.contains(taxesNames[5])
                                            || lineList.contains(taxesNames[6])
                                            || ToplineList.contains(taxesNames[0]) || ToplineList.contains(taxesNames[1]) || ToplineList.contains(taxesNames[2])
                                            || ToplineList.contains(taxesNames[3]) || ToplineList.contains(taxesNames[4]) || ToplineList.contains(taxesNames[5])
                                            || ToplineList.contains(taxesNames[6])) {
                                        v = lineFrag[lineFrag.length - 2].replace("-", "");
                                        //System.out.println(date + " " + v);
                                        cond.taxDate.add(date);
                                        cond.taxValue.add(v);
                                        j++;
                                        continue;
                                        //TODO: PAGAMENTOS E RECEBIMENTOS
                                    } else if (lineList.contains(debit[0]) || lineList.contains(debit[1]) || lineList.contains(debit[2])
                                            || lineList.contains(debit[3]) || lineList.contains(debit[4]) || lineList.contains(debit[5])
                                            || lineList.contains(debit[6]) || lineList.contains(debit[7])
                                            || ToplineList.contains(debit[0]) || ToplineList.contains(debit[1]) || ToplineList.contains(debit[2])
                                            || ToplineList.contains(debit[3]) || ToplineList.contains(debit[4]) || ToplineList.contains(debit[5])
                                            || ToplineList.contains(debit[6]) || ToplineList.contains(debit[7])) {
                                        v = lineFrag[lineFrag.length - 2].replace(".", "");
                                        v = v.replace(",", ".");
                                        if (Double.valueOf(v) < 0) {
                                            String temp = "";
                                            for (int k = 1; k < lineFrag.length; k++) {
                                                temp = temp + lineFrag[k] + " ";
                                            }
                                            cond.debitEntry.add(new Entry(date + " " + lines[j - 1] + " " + lines[j + 1] + " " + temp));
                                            j++;
                                            continue;
                                        }
                                    } else if (lineList.contains(credit[0]) || lineList.contains(credit[1])
                                            || ToplineList.contains(credit[0]) || ToplineList.contains(credit[1])) {
                                        v = lineFrag[lineFrag.length - 2].replace(".", "");
                                        v = v.replace(",", ".");
                                        if (Double.valueOf(v) > 0) {
                                            String temp = "";
                                            for (int k = 1; k < lineFrag.length; k++) {
                                                temp = temp + lineFrag[k] + " ";
                                            }
                                            cond.creditEntry.add(new Entry(date + " " + lines[j - 1] + " " + lines[j + 1] + " " + temp));
                                            j++;
                                            continue;
                                        }
                                    }
                                } // ELSE CASO DE LINHA QUEBRADA COM DATA
                            } // IF isNumeric(lineFrag[lineFrag.length-1])&& dateSlit[1].equals(month)
                            //IF dateSplit.length==3
                            //CASO LINHA SEM DATA ?QUEBRADA?
                        } else {

                            if (isNumeric(lineFrag[lineFrag.length - 1])) {
                                String[] topRowFrag = null;
                                if (j == 0) {
                                    topRowFrag = lastStringFromPage.split(" ");
                                } else {
                                    topRowFrag = lines[j - 1].split(" ");
                                }
                                ToplineList = Arrays.asList(topRowFrag);
                                lineList = Arrays.asList(lineFrag);
                                if (lineList.contains("Total")) {
                                    end = true;
                                    break;
                                }
                                String v = null;
                                //RESGATE: valor negativo
                                if (lineList.contains(retrieve[0]) || lineList.contains(retrieve[1]) || lineList.contains(retrieve[2])
                                        || ToplineList.contains(retrieve[0]) || ToplineList.contains(retrieve[1]) || ToplineList.contains(retrieve[2])) {
                                    v = lineFrag[lineFrag.length - 2];
                                    if (!lineList.contains("CHEQUE") && !lineList.contains("GASTOS")) {
                                        cond.appRescDate.add(date);
                                        cond.appRescValue.add("-" + v);
                                        j++;
                                        continue;
                                    } else {
                                        cond.debitEntry.add(new Entry(date + " " + lines[j]));
                                        j++;
                                        continue;
                                    }
                                    //APLICACAO: valor positivo
                                } else if (lineList.contains(investment[0]) || lineList.contains(investment[1]) || lineList.contains(investment[2])
                                        || ToplineList.contains(investment[0]) || ToplineList.contains(investment[1]) || ToplineList.contains(investment[2])) {
                                    v = lineFrag[lineFrag.length - 2].replace("-", "");
                                    if (!lineList.contains("CHEQUE") && !lineList.contains("GASTOS")) {
                                        cond.appRescDate.add(date);
                                        cond.appRescValue.add(v);
                                        j++;
                                        continue;
                                    } else {
                                        cond.debitEntry.add(new Entry(date + " " + lines[j]));
                                        j++;
                                        continue;
                                    }
                                    //TARIFA
                                } else if ((lineList.contains(taxesNames[0]) || lineList.contains(taxesNames[1]) || lineList.contains(taxesNames[2])
                                        || lineList.contains(taxesNames[3]) || lineList.contains(taxesNames[4]) || lineList.contains(taxesNames[5])
                                        || lineList.contains(taxesNames[6])
                                        || ToplineList.contains(taxesNames[0]) || ToplineList.contains(taxesNames[1]) || ToplineList.contains(taxesNames[2])
                                        || ToplineList.contains(taxesNames[3]) || ToplineList.contains(taxesNames[4]) || ToplineList.contains(taxesNames[5])
                                        || ToplineList.contains(taxesNames[6])) && !lineList.contains("CHEQUE")) {
                                    v = lineFrag[lineFrag.length - 2].replace("-", "");
                                    //System.out.println(date+" "+v);
                                    cond.taxDate.add(date);
                                    cond.taxValue.add(v);
                                    j++;
                                    continue;
                                    //TODO: PAGAMENTOS E RECEBIMENTOS
                                } else if (lineList.contains(debit[0]) || lineList.contains(debit[1]) || lineList.contains(debit[2])
                                        || lineList.contains(debit[3]) || lineList.contains(debit[4]) || lineList.contains(debit[5])
                                        || lineList.contains(debit[6]) || lineList.contains(debit[7])
                                        || ToplineList.contains(debit[0]) || ToplineList.contains(debit[1]) || ToplineList.contains(debit[2])
                                        || ToplineList.contains(debit[3]) || ToplineList.contains(debit[4]) || ToplineList.contains(debit[5])
                                        || ToplineList.contains(debit[6]) || ToplineList.contains(debit[7])) {
                                    v = lineFrag[lineFrag.length - 2].replace(".", "");
                                    v = v.replace(",", ".");
                                    System.out.println(lines[j]);
                                    if (!lineList.contains("NF"))
                                    if (Double.valueOf(v) < 0) {
                                        if ((lineList.contains("CHEQUE")&&lineList.contains("COMPENSADO"))||lineList.contains("GASTOS")) {
                                            cond.debitEntry.add(new Entry(date + " " + lines[j]));
                                            j++;
                                            continue;
                                        }
                                        if (j - 1 == -1) {
                                            cond.debitEntry.add(new Entry(date + " " + lastStringFromPage + " " + lines[j + 1] + " " + lines[j]));
                                        } else {
                                            cond.debitEntry.add(new Entry(date + " " + lines[j - 1] + " " + lines[j + 1] + " " + lines[j]));
                                        }
                                        j++;
                                        continue;
                                    }
                                } else if (lineList.contains(credit[0]) || lineList.contains(credit[1])
                                        || ToplineList.contains(credit[0]) || ToplineList.contains(credit[1])) {
                                    v = lineFrag[lineFrag.length - 2].replace(".", "");
                                    v = v.replace(",", ".");
                                    if (Double.valueOf(v) > 0) {
                                        if (j - 1 == -1) {
                                            cond.creditEntry.add(new Entry(date + " " + lastStringFromPage + " " + lines[j + 1] + " " + lines[j]));
                                        } else {
                                            cond.creditEntry.add(new Entry(date + " " + lines[j - 1] + " " + lines[j + 1] + " " + lines[j]));
                                        }
                                        j++;
                                        continue;
                                    }
                                }
                            }//IF isNumeric(lineFrag[lineFrag.length-1])
                        }// ELSE CASO LINHA SEM DATA ?QUEBRADA?

                        j++;
                    }// WHILE j<lines.length
                    lastStringFromPage = lines[lines.length - 1];

                }//IF cond!=null

            }// FOR int i = 1; i <= totalPages; i++

        }
        if (dest.equals("") || dest.equals("Nenhuma pasta selecionada.")) {
            Component frame = null;
            JOptionPane.showMessageDialog(frame,
                    "selecione a pasta onde as aplicações, resgates e tarifas devem ser salvas.",
                    "Pasta não selecionada",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            print(condMap, dest);
            printT(condMap, dest);
        }

        return condMap;
    }// MAIN

}//CLASS
