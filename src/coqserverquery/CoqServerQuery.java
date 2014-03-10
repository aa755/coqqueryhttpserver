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
    HttpServer server = HttpServer.create(new InetSocketAddress(4987), 0);
    server.createContext("/coq_query", new MyHandler(args[0]));
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  static class MyHandler implements HttpHandler {

      CoqTopXMLIO coqtop=null;
      static String getHeader(String query)
      {
          return               "<!DOCTYPE html>\n" +
"<html>\n" +
" \n" +
"  <head>\n" +
"    <title>Query Interface</title>\n" +
"     <link href=\"http://www.nuprl.org/html/verification/v1/html/coqdoc.css\" rel=\"stylesheet\" type=\"text/css\"/>\n" +
"    <meta charset=\"UTF-8\">\n" +
"    <meta name=\"viewport\" content=\"width=device-width\">\n" +
"<script> \n" +
"function goto_url(){\n" +
"    var word = document.getElementsByName(\"coqQuery\")[0].value;\n" +
"            //document.forms[0].elements[0].value;//by index\n" +
"    var escaped = escape(word);//apply url encoding    \n" +
"    var url = \"/coq_query?\"+escaped;\n" +
"    location.href = url;\n" +
"}\n" +
"</script>  </head>\n" +
"  \n" +
"  \n" +
"  <body>\n" +
"\n" +
"        <div id=\"query\"><input type=\"text\" name=\"coqQuery\" value=\n" +
"                     \""+query+"\"\n" +
"                     size=\"80\" /> </div>\n" +
"        <input type=\"button\" value=\"Query\" name=\"querySubmit\" onclick=\"goto_url();\" />\n" +
"    <!--div id=\"response\">Change this</div-->\n" +
"    <ul>\n" +
"      <li> To locate an object ob (a Coq <span class=\"id\" type=\"keyword\">\n" +
"              Definition/Lemma/Inductive</span>\n" +
"           e.t.c.), \n" +
"          query \"Locate ob.\". For example, type \"Locate sequent_true.\" (excluding quotes) \n" +
"            above and click Query.\n" +
"      </li>\n" +
"      <li> To view the definition of an object, your query should be \n" +
"          \"Print ob_fullname.\", where ob_fullname is is full name of the\n" +
"           object ob (including the prefixes). \n" +
"            The output of \"Locate ob.\" contains the full name.\n" +
"             For example, try the query \"Print sequents.sequent_true.\" . \n" +
"             Also, usually the object X.Y can be found in the file X.v .\n" +
"            </li>\n" +
"    </ul>\n" +
"    <h2>Coqtop's response:</h2>\n" +
"    <pre>";

      }
      String footer="    </pre>\n" +
"  </body>\n" +
"</html>\n" +
"";
      
    public MyHandler(String initCommand) {
        try {
        coqtop=new CoqTopXMLIO();
        coqtop.interpret(initCommand);
      } catch (IOException ex) {
        Logger.getLogger(CoqServerQuery.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

      static boolean isQuerySecure(String query) 
      {
          if (!query.startsWith("Print") 
                  && !query.startsWith("Locate")
                  && !query.startsWith("Check")
                  && !query.startsWith("SearchAbout") 
                  && !query.startsWith("SearchAbout")) 
              return false;
          
          return !query.contains(";");
      }
      
      static String securityError="Security Error: For security reasons, a query must start with \n"
              + "Print, Check, Locate, SearchAbout or SearchPattern \n"
              + "and must NOT contain a semicolon(;). \n If you beleive "
              + "your query is legitimate, \n please (anonymously)email the authors of the paper.";

      @Override
    public synchronized void handle(HttpExchange t) throws IOException {
      String query = t.getRequestURI().getQuery();
      System.out.println("remote IP:"+t.getRemoteAddress());
      System.out.println("URI:"+t.getRequestURI());
      String responseBody = "";//Your query was :" + query+"\n";
      if (query != null && !query.isEmpty()) {
            if (coqtop == null || !isQuerySecure(query)) {
                if(coqtop==null)
                responseBody = responseBody + "There was a problem in starting coqtop\n."
                        + "Sorry about that. Please (anonymously)email the authors of the paper ASAP.";
                else
                responseBody = responseBody + securityError;                
            } else {
                CoqTopXMLIO.CoqRecMesg rec = coqtop.query(query);
                if(rec.success)
                {
                    responseBody = responseBody + rec.conciseReply;
                }
                else
                {
                    responseBody= responseBody+ rec.nuDoc.toXML();
                }
            }
        }
      if(query==null || query.isEmpty())
      {
          query="Please type your query here.";
      }
      String response=getHeader(query)+responseBody+footer;
      t.sendResponseHeaders(200, response.length());
      try (OutputStream os = t.getResponseBody()) {
        os.write(response.getBytes());
      }
    }
  }

}
