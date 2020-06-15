/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.nattaxappextractor;

/**
 *
 * @author gabri
 */
public class Entry {
    public String content ="";
  //-1: PAGAMENTO NÃO ENCONTRADO
  //0 : ENCONTRADO
  //1 : DATA DE BAIXA ERRADA
  int errorType=-1;
  public Entry(String content){
    this.content=content;
  }
    
}
