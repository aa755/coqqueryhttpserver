/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coqserverquery;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CoqServerQuery {

  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
    server.createContext("/coq_query", new MyHandler(args[0]));
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  static class MyHandler implements HttpHandler {

      CoqTopXMLIO coqtop=null;
    public MyHandler(String initCommand) {
        try {
        coqtop=new CoqTopXMLIO();
        coqtop.interpret(initCommand);
      } catch (IOException ex) {
        Logger.getLogger(CoqServerQuery.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    @Override
    public synchronized void handle(HttpExchange t) throws IOException {
      String query = t.getRequestURI().getQuery();

      String response = "Your query was :" + query+"\n";
      if(coqtop==null)
      {
        response=response+"there was a problem in starting coqtop\n";
      }
      else
      {
        CoqTopXMLIO.CoqRecMesg rec= coqtop.query(query);
        response=response+"coqtop replied:\n"+ rec.conciseReply;
      }
      t.sendResponseHeaders(200, response.length());
      try (OutputStream os = t.getResponseBody()) {
        os.write(response.getBytes());
      }
    }
  }

}
