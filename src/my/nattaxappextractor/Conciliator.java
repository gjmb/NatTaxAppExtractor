/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.nattaxappextractor;

import java.io.FileOutputStream;
import java.io.IOException;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.DocumentException;
import static java.nio.charset.StandardCharsets.*;
import java.util.*;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.BaseColor;

/**
 *
 * @author gabri
 */
public class Conciliator {

    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public static double creditTotalG(CondominiumG c) {
        double total = 0.00;

        //Soma
        //TODO: Ignorar transferencia
        for (int i = 0; i < c.creditEntry.size(); i++) {
            String[] e = c.creditEntry.get(i).content.split(" ");
            List<String> l = Arrays.asList(e);
            String v = e[e.length - 3];
            if (!l.contains("REC.") && !l.contains("RECEBIDO")) {
                total = total + Double.parseDouble(v);
            }
        }
        //Busca por isencao

        for (int i = 0; i < c.debitEntry.size(); i++) {
            String[] e = c.debitEntry.get(i).content.split(" ");
            List<String> l = Arrays.asList(e);
            if (l.contains("00000213") && l.contains("Recibo:")) {
                //System.out.println("Oi");
                String v = e[e.length - 3];
                total = total - Double.parseDouble(v);
                c.debitEntry.get(i).errorType = 0;
            }

        }

        return round(total, 2);
    }

    public static double creditTotal(Condominium c) {
        double total = 0.00;

        //Soma
        for (int i = 0; i < c.creditEntry.size(); i++) {
            String[] e = c.creditEntry.get(i).content.split(" ");
            String v = e[e.length - 2].replace(".", "");
            v = v.replace(",", ".");
            total = total + Double.parseDouble(v);
        }

        return round(total, 2);
    }

    public static void checker(Map<String, Condominium> condMap, Map<String, CondominiumG> condMapG, String dest) {
        dest = dest.replace("\\", "/");

        for (int index = 0; index < ExcelReader.condKeysG.length; index++) {

            Condominium c = condMap.get(TaxExtractor2.condKeys[index]);
            CondominiumG g = condMapG.get(ExcelReader.condKeysG[index]);

            Document document = new Document(PageSize.A4, 0f, 0f, 0f, 0f);
            float fntSize, lineSpacing;
            fntSize = 6.7f;
            lineSpacing = 10f;
            //Font f1 = FontFactory.getFont(FontFactory.TIMES_ROMAN, 12);
            //f1.setColor(BaseColor.BLUE);
            //p = new Paragraph(new Phrase(lineSpacing, line, FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize)));

            try {
                dest = dest.replace("\\", "/");
                PdfWriter.getInstance(document, new FileOutputStream(dest + "/relatorio_" + ExcelReader.condKeysG[index] + ".pdf"));
                document.open();
                Paragraph p = new Paragraph(new Phrase(lineSpacing, "*****************************", FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize)));
                document.add(p);
                p = new Paragraph(new Phrase(lineSpacing, ExcelReader.condKeysG[index] + " - " + c.name, FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize)));
                document.add(p);
                p = new Paragraph(new Phrase(lineSpacing, "*****************************", FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize)));
                document.add(p);
                /////////////////////////////////////////////
                ///////verifica diferenca de credito////////
                ///////////////////////////////////////////
                double tg = creditTotalG(g);
                double t = creditTotal(c);
                if (tg > t) {
                    p = new Paragraph(new Phrase(lineSpacing, "DIFERENCA DE CREDITO ", FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize)));
                    document.add(p);
                    Font f1 = FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize);
                    f1.setColor(BaseColor.BLUE);
                    p = new Paragraph(new Phrase(lineSpacing, "    VALOR MAIOR: GOSOFT", f1));
                    document.add(p);
                    p = new Paragraph(new Phrase(lineSpacing, "        DIFERENCA: " + round(tg - t, 2), f1));
                    document.add(p);
                    double dif = round((tg - t), 2);
                    //busca no gosoft um credito de mesmo valor
                    for (int i = 0; i < g.creditEntry.size(); i++) {
                        String[] entryContent = g.creditEntry.get(i).content.split(" ");
                        String v = entryContent[entryContent.length - 3];
                        if (dif == round(Double.parseDouble(v), 2)) {
                            byte[] text = g.creditEntry.get(i).content.getBytes(ISO_8859_1);
                            g.creditEntry.get(i).content = new String(text, UTF_8);
                            p = new Paragraph(new Phrase(lineSpacing, "            CREDITO DE MESMO VALOR: " + g.creditEntry.get(i).content, f1));
                            document.add(p);

                        }
                    }
                    //busca no gosoft um debito de mesmo valor
                    for (int i = 0; i < g.debitEntry.size(); i++) {
                        String[] entryContent = g.debitEntry.get(i).content.split(" ");
                        String v = entryContent[entryContent.length - 3];

                        if (dif == round(Double.parseDouble(v), 2)) {
                            byte[] text = g.debitEntry.get(i).content.getBytes(ISO_8859_1);
                            g.debitEntry.get(i).content = new String(text, UTF_8);
                            f1.setColor(BaseColor.RED);
                            p = new Paragraph(new Phrase(lineSpacing, "            DEBITO DE MESMO VALOR: " + g.debitEntry.get(i).content, f1));
                            document.add(p);

                        }
                    }

                } else if (t > tg) {
                    p = new Paragraph(new Phrase(lineSpacing, "DIFERENCA DE CREDITO ", FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize)));
                    document.add(p);
                    Font f1 = FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize);
                    f1.setColor(BaseColor.BLUE);
                    p = new Paragraph(new Phrase(lineSpacing, "    VALOR MAIOR: BANCO", f1));
                    document.add(p);
                    p = new Paragraph(new Phrase(lineSpacing, "        DIFERENCA: " + round(t - tg, 2), f1));
                    document.add(p);
                    double dif = round(t - tg, 2);
                    //busca no banco um credito de mesmo valor
                    for (int i = 0; i < c.creditEntry.size(); i++) {
                        String[] entryContent = c.creditEntry.get(i).content.split(" ");
                        String v = entryContent[entryContent.length - 2].replace(".", "");
                        v = v.replace(",", ".");
                        if (dif == round(Double.parseDouble(v), 2)) {
                            p = new Paragraph(new Phrase(lineSpacing, "            CREDITO DE MESMO VALOR: " + c.creditEntry.get(i).content, f1));
                            document.add(p);

                        }
                    }
                    //busca no banco um debito de mesmo valor
                    for (int i = 0; i < c.debitEntry.size(); i++) {
                        String[] entryContent = c.debitEntry.get(i).content.split(" ");
                        String v = entryContent[entryContent.length - 2].replace(".", "");
                        v = v.replace(",", ".");
                        v = v.replace("-", "");
                        if (dif == round(Double.parseDouble(v), 2)) {
                            f1.setColor(BaseColor.RED);
                            p = new Paragraph(new Phrase(lineSpacing, "            DEBITOS DE MESMO VALOR: " + c.debitEntry.get(i).content, f1));
                            document.add(p);

                        }
                    }
                }

                /////////////////////////////////////////////
                ///////verifica diferenca de debitos////////
                ///////////////////////////////////////////
                for (int i = 0; i < c.debitEntry.size(); i++) {
                    if (c.debitEntry.get(i).errorType == -1) {
                        String[] entryContent = c.debitEntry.get(i).content.split(" ");
                        String v = entryContent[entryContent.length - 2].replace(".", "");
                        v = v.replace(",", ".");
                        v = v.replace("-", "");
                        if (v.charAt(v.length() - 1) == '0') {
                            v = v.substring(0, v.length() - 1);
                        }
                        //ystem.out.println("250 " + v);
                        List<String> l = Arrays.asList(entryContent);

                        for (int j = 0; j < g.debitEntry.size(); j++) {
                            if (g.debitEntry.get(j).errorType == -1) {
                                String[] entryContentG = g.debitEntry.get(j).content.split(" ");
                                //System.out.println(g.debitEntry.get(j).content);
                                List<String> lG = Arrays.asList(entryContentG);
                                if (lG.contains(v)) {
                                    //CASO DE CHEQUES REGISTRADOS PELA CIBRACON
                                    if (l.contains("CHEQUE")) {
                                        if (!lG.contains("CH")) {
                                            continue;
                                        }
                                        //TODO: RASTREAR CHEQUES PELO NUMERO
                                    }
                                    //System.out.println("264 " + entryContentG[1] + " " + entryContent[0]);

                                    if (entryContentG[1].equals(entryContent[0])) {
                                        c.debitEntry.get(i).errorType = 0;
                                        g.debitEntry.get(j).errorType = 0;
                                        // System.out.println("268 " + c.debitEntry.get(i).content);
                                        //System.out.println("269 " + g.debitEntry.get(j).content);
                                        break;
                                    } else {
                                        //System.out.println("272 " + c.debitEntry.get(i).content);
                                        //System.out.println("273 " + g.debitEntry.get(j).content);
                                        c.debitEntry.get(i).errorType = 1;
                                        g.debitEntry.get(j).errorType = 1;
                                        break;
                                    }
                                } else {
                                    //CASO DE CHEQUES REGISTRADOS PELA CIBRACON: BUG DO 41
                                    if (l.contains("CHEQUE")) {
                                        if (!lG.contains(entryContent[entryContent.length - 3])) {
                                        } else {

                                            if (entryContentG[1].equals(entryContent[0])) {
                                                c.debitEntry.get(i).errorType = 0;
                                                g.debitEntry.get(j).errorType = 0;
                                                // System.out.println("239 "+c.debitEntry.get(i).content);
                                                //  System.out.println("240 "+g.debitEntry.get(j).content);

                                            } else {
                                                c.debitEntry.get(i).errorType = 1;
                                                g.debitEntry.get(j).errorType = 1;
                                                // System.out.println("245 "+c.debitEntry.get(i).content);
                                                //  System.out.println("246 "+g.debitEntry.get(j).content);

                                            }
                                        }
                                    }
                                }
                            }
                        }//for j < g.debitEntry.size()

                    }//if c.debitEntry.get(i).errorType==-1

                }//for i<c.debitEntry.size()

                for (int i = 0; i < c.debitEntry.size(); i++) {
                    if (c.debitEntry.get(i).errorType == -1) {
                        String[] entryContent = c.debitEntry.get(i).content.split(" ");
                        String v = entryContent[entryContent.length - 2].replace(".", "");
                        v = v.replace(",", ".");
                        v = v.replace("-", "");
                        if (v.charAt(v.length() - 1) == '0') {
                            v = v.substring(0, v.length() - 1);
                        }
                        //System.out.println("331 " + v);
                        List<String> l = Arrays.asList(entryContent);
                        for (int j = 0; j < g.debitEntry.size(); j++) {
                            if (g.debitEntry.get(j).errorType == -1) {
                                String[] entryContentG = g.debitEntry.get(j).content.split(" ");
                                List<String> lG = Arrays.asList(entryContentG);
                                if (lG.contains(v)) {
                                    if (entryContentG[1].equals(entryContent[0])) {
                                        c.debitEntry.get(i).errorType = 0;
                                        g.debitEntry.get(j).errorType = 0;
                                        //    System.out.println("318 "+c.debitEntry.get(i).content);
                                        //   System.out.println("319 "+g.debitEntry.get(j).content);
                                        break;
                                    } else {
                                        c.debitEntry.get(i).errorType = 1;
                                        g.debitEntry.get(j).errorType = 1;
                                        //    System.out.println("324 " +c.debitEntry.get(i).content);
                                        //    System.out.println("325 "+g.debitEntry.get(j).content);
                                        break;
                                    }
                                }
                            }
                        }//for j < g.debitEntry.size()
                    }//if c.debitEntry.get(i).errorType==-1
                }//for i<c.debitEntry.size()

                ////////////////////////////////////////////////////////////////////////////
                ///////Tenta resolver o bug dos multiplos lancamentos e um pagamento////////
                ////////////////////////////////////////////////////////////////////////////
                // VERIFICA PAGAMENTO DE DEPESAS PARA A CIBRACON
                for (int i = 0; i < c.debitEntry.size(); i++) {
                    String[] entryContent = c.debitEntry.get(i).content.split(" ");
                    List<String> l0 = Arrays.asList(entryContent);
                    String v = entryContent[entryContent.length - 2].replace(".", "");
                    v = v.replace(",", ".");
                    v = v.replace("-", "");
                    if (v.charAt(v.length() - 1) == '0') {
                        v = v.substring(0, v.length() - 1);
                    }

                    double sum = 0;
                    if (l0.contains("CIBRACON") && c.debitEntry.get(i).errorType == -1) {
                        System.out.println(c.debitEntry.get(i).content);
                        System.out.println("v:" + v);
                        for (int j = 0; j < g.debitEntry.size(); j++) {
                            String[] entryContentG = g.debitEntry.get(j).content.split(" ");
                            List<String> l1 = Arrays.asList(entryContentG);
                            String v1 = entryContentG[entryContentG.length - 3];
                            if (l1.contains("00000213") && g.debitEntry.get(j).errorType == -1) {
                                System.out.println("v1:" + v1);
                                sum = sum + Double.parseDouble(v1);
                                System.out.println("sum:" + sum);
                                
                                if (Double.toString(round(sum, 2)).equals(v)) {
                                    g.debitEntry.get(j).errorType = 0;
                                    c.debitEntry.get(i).errorType = 0;
                                    break;
                                } else if (round(sum, 2) > round(Double.valueOf(v), 2)) {
                                    sum = sum - Double.parseDouble(v1);
                                } else {
                                    g.debitEntry.get(j).errorType = 0;
                                }
                            }
                        }
                        if (round(sum, 2) < round(Double.valueOf(v), 2)) {
                            for (int j = 0; j < g.debitEntry.size(); j++) {
                                String[] entryContentG = g.debitEntry.get(j).content.split(" ");
                                List<String> l1 = Arrays.asList(entryContentG);
                                String v1 = entryContentG[entryContentG.length - 3];

                                if (l1.contains("00002118") && g.debitEntry.get(j).errorType == -1) {
                                    sum = sum + Double.parseDouble(v1);
                                    if (Double.toString(round(sum, 2)).equals(v)) {
                                        g.debitEntry.get(j).errorType = 0;
                                        c.debitEntry.get(i).errorType = 0;
                                        break;
                                    } else {
                                        g.debitEntry.get(j).errorType = 0;
                                    }
                                }
                            }//int j = 0; j < g.debitEntry.size(); j++
                        }//if round(sum, 2) < round(Double.valueOf(v), 2)
                    }//if l0.contains("CIBRACON")&& c.debitEntry.get(i).errorType==-1
                }//int i = 0; i < c.debitEntry.size(); i++

                for (int i = 0; i < c.debitEntry.size(); i++) {
                    if (c.debitEntry.get(i).errorType == -1) {
                        String[] entryContent = c.debitEntry.get(i).content.split(" ");
                        //System.out.println(c.debitEntry.get(i).content +" "+c.debitEntry.get(i).errorType);
                        String v = entryContent[entryContent.length - 2].replace(".", "");
                        v = v.replace(",", ".");
                        v = v.replace("-", "");
                        if (v.charAt(v.length() - 1) == '0') {
                            v = v.substring(0, v.length() - 1);
                        }

                        //System.out.println("v: "+v);
                        for (int j = 0; j < g.debitEntry.size(); j++) {
                            String[] entryContentG = g.debitEntry.get(j).content.split(" ");
                            String v1 = entryContentG[entryContentG.length - 3];
                            
                            double sum = Double.parseDouble(v1);
                            for (int k = j + 1; k < g.debitEntry.size(); k++) {
                                if (k == g.debitEntry.size()) {
                                    break;
                                }
                                String[] entryContentG1 = g.debitEntry.get(k).content.split(" ");
                                String v2 = entryContentG1[entryContentG1.length - 3];
                               
                                if (entryContentG[1].equals(entryContent[0]) && g.debitEntry.get(j).errorType == -1 && c.debitEntry.get(i).errorType == -1) {
                                    //System.out.println(g.debitEntry.get(j).content+" "+g.debitEntry.get(j).errorType);
                                    sum = round(sum, 2) + round(Double.parseDouble(v2), 2);
                                    //System.out.println(sum);
                                    if (round(sum, 2) == round(Double.parseDouble(v), 2)) {
                                        c.debitEntry.get(i).errorType = 2;
                                        break;
                                    } else if (round(sum, 2) > round(Double.parseDouble(v), 2)) {
                                        sum = sum - Double.parseDouble(v2);
                                    }
                                }
                            }
                        }//j < g.debitEntry.size()

                    }
                }

                for (int i = 0; i < c.debitEntry.size(); i++) {
                    String[] entryContent = c.debitEntry.get(i).content.split(" ");
                    if (c.debitEntry.get(i).errorType == 2) {
                        c.debitEntry.get(i).errorType = 0;
                        for (int j = 0; j < g.debitEntry.size(); j++) {
                            String[] entryContentG = g.debitEntry.get(j).content.split(" ");
                            if (g.debitEntry.get(j).errorType == -1 && entryContentG[1].equals(entryContent[0])) {
                                g.debitEntry.get(j).errorType = 0;
                                //p.printf("    "+g.debitEntry.get(j).content+ " "+g.debitEntry.get(j).errorType +"\n");
                            }
                        }
                    }

                }

                /////////////////////////////////////////////
                ///////mostra diferencas encontradas////////
                ///////////////////////////////////////////
                p = new Paragraph(new Phrase(lineSpacing, "BANCO: LANCAMENTO NAO BAIXADO/LANCADO", FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize)));
                document.add(p);
                Font f1 = FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize);
                f1.setColor(BaseColor.RED);

                for (int i = 0; i < c.debitEntry.size(); i++) {
                    if (c.debitEntry.get(i).errorType < 0) {
                        p = new Paragraph(new Phrase(lineSpacing, "    " + c.debitEntry.get(i).content, f1));
                        document.add(p);
                    }
                }
                p = new Paragraph(new Phrase(lineSpacing, "GOSOFT: PAGAMENTO NAO ENCONTRADO", FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize)));
                document.add(p);

                //TODO: Ignorar transferencia
                for (int i = 0; i < g.debitEntry.size(); i++) {
                    String[] entryContentG = g.debitEntry.get(i).content.split(" ");
                    List<String> l = Arrays.asList(entryContentG);
                    byte[] text = g.debitEntry.get(i).content.getBytes(ISO_8859_1);
                    g.debitEntry.get(i).content = new String(text, UTF_8);
                    if (g.debitEntry.get(i).errorType == -1 && !l.contains("TRANSF.") && !l.contains("TRANSFERENCIA")) {
                        p = new Paragraph(new Phrase(lineSpacing, "    " + g.debitEntry.get(i).content, f1));
                        document.add(p);

                    }
                }
                p = new Paragraph(new Phrase(lineSpacing, "BANCO: LANCAMENTO BAIXADO DATA ERRADA", FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize)));
                document.add(p);
                f1.setColor(BaseColor.DARK_GRAY);
                for (int i = 0; i < c.debitEntry.size(); i++) {
                    if (c.debitEntry.get(i).errorType == 1) {
                        p = new Paragraph(new Phrase(lineSpacing, "    " + c.debitEntry.get(i).content, f1));
                        document.add(p);
                    }
                }
                p = new Paragraph(new Phrase(lineSpacing, "GOSOFT: LANCAMENTO BAIXADO DATA ERRADA", FontFactory.getFont(FontFactory.TIMES_ROMAN, fntSize)));
                document.add(p);

                for (int i = 0; i < g.debitEntry.size(); i++) {
                    if (g.debitEntry.get(i).errorType == 1) {
                        p = new Paragraph(new Phrase(lineSpacing, "    " + g.debitEntry.get(i).content, f1));
                        document.add(p);

                    }
                }

            } catch (DocumentException de) {
                System.err.println(de.getMessage());
            } catch (IOException ioe) {
                System.err.println(ioe.getMessage());
            }
            document.close();

        }// for index<condKeysG.length

    }

}
