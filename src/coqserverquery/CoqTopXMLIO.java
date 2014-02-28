/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package coqserverquery;

//import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
//import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

//import nu.xom.ParsingException;
//import nu.xom.ValidityException;
import org.apache.commons.lang3.StringEscapeUtils;


/**
 *
 * @author Abhishek
 */
public class CoqTopXMLIO {
    
private  Process process;
private  BufferedReader result;
//private static BufferedReader err;
private  PrintWriter input;
//private static PrintWriter input;

    
    public CoqTopXMLIO() throws IOException {
        
        
            String command="coqtop";
//            process = new ProcessBuilder(command,"-ideslave").directory( "C:\\Users\\Abhishek\\Desktop\\vnuprl\\coq").start();
            process = new ProcessBuilder(command,"-ideslave").start();
        
            input = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);
            result=new BufferedReader(new InputStreamReader(process.getInputStream()));
            System.out.println("started coq");
    }
    
    public synchronized CoqRecMesg communicate(CoqSendMesg msg)
    {
   //     System.out.println("writing:" +msg.toXML());
        input.write(msg.toXML());
        input.flush();
        System.out.println("wrote:" +msg.toXML());
        CoqRecMesg ret= new CoqRecMesg();
        
        ret.parseXMLFromStream(result);
        return ret;        
    }

    public CoqRecMesg interpret(String code)
    {
        return communicate(new CoqSendMesg(code));
    }
    
    
    /**
     * Assumes that this query was executed 
     * outside the file being edited. e.g. the separate query interface
     * By rewinding, it ensures that the coqtop state is not changed.
     * 
     * @param code
     * @return 
     */
    public CoqRecMesg query(String code)
    {
        CoqRecMesg rec= communicate(new CoqSendMesg(code));
        if(rec.success)
        {
            rewindForQuery();            
            String reply=rec.nuDoc.getRootElement().getFirstChildElement("string").getValue();
            String creply=reply;
            String warnMesg="Warning: query commands should not be inserted in scripts";
            if(reply.startsWith(warnMesg))
                creply=reply.substring(warnMesg.length());
            rec.conciseReply=creply;
        }
        return rec;
    }

    void rewindForQuery()
    {
        CoqRecMesg rec=rewind(1);
        assert(rec.success);
        assert(rec.getExtraRewoudSteps()==0);        
    }
    
    public CoqRecMesg setOption(String code)
    {
        return communicate(new CoqSendMesg(code,"interp"," id=\"-2\" raw=\"\""));
    }

    public CoqRecMesg rewind(int steps)
    {
        return communicate(new CoqSendRewindMeg(steps));
    }
    
    public String getVersion()
    {
        CoqRecMesg rec=communicate(CoqSendMesg.getVersionMesg());
        if(!rec.success)
            return "failure in init of CoqTop";
        else
            return rec.nuDoc.toXML();
    }
    
    public nu.xom.Document getGoal()
    {
        CoqRecMesg rec=communicate(CoqSendMesg.goalMessage());
        if(!rec.success)
            return null;
        else
            return rec.nuDoc;
    }

    public static class CoqSendMesg{
        public String mesg;
        public String type;
        public String extras;


        static CoqSendMesg getVersionMesg()
        {
            return new CoqSendMesg("", "about");
        }
        
        public CoqSendMesg(String mesg, String type) {
            this.mesg = mesg;
            this.type = type;
            this.extras="";
        }

        public CoqSendMesg(String mesg) {
            this.mesg = mesg;
            this.type= "interp";
            this.extras="";
        }
        
        public CoqSendMesg(String mesg, String type, String extras) {
            this.mesg = mesg;
            this.type = type;
            this.extras=extras;
        }

        public static CoqSendMesg goalMessage()
        {
            return new CoqSendMesg("", "goal");
        }
        public String toXML()
        {
            return "<call val=\""+ type +"\""+extras+">"+ StringEscapeUtils.escapeXml(mesg)+"</call>";
        }
    }
    
    public static class CoqSendRewindMeg extends CoqSendMesg
    {

        int numSteps;
        
        public CoqSendRewindMeg() {
            super("", "rewind");
            numSteps=1;
        }

        public CoqSendRewindMeg( int numSteps) {
            super("", "rewind");
            this.numSteps = numSteps;
        }
               
        @Override
        public String toXML()
        {            
            return "<call val=\"rewind\" steps=\""+numSteps+"\"/>";
        }        
    }
    
    public static class CoqRecMesg{
       nu.xom.Document nuDoc;
        //Document doc;
        boolean success;
     //   Element contents;
        private static final int BUF_SIZE=262144;
        private static final int NUM_TRIALS=1024;
        String conciseReply;
        
        char [] buf=new char [BUF_SIZE];
        
        void trySleep(int milis)
        {
           try {
               Thread.sleep(milis);
           } catch (InterruptedException ex) {
               ex.printStackTrace();
           }
        }
        
        int getExtraRewoudSteps()
        {
            nu.xom.Element intStr=nuDoc.getRootElement().getFirstChildElement("int");
            return Integer.parseInt(intStr.getValue());
        }
        
        public void parseXMLFromStream(BufferedReader result) {
            try {
                nu.xom.Builder b = new nu.xom.Builder();
                String answer = "";
                int count = 0;
                while (!result.ready())
                    trySleep(10);
                //    Thread.sleep(1);
                while (count < NUM_TRIALS) {
                    while (result.ready()) {
                        int numRead=result.read(buf,0,BUF_SIZE);
                        if(numRead>0)
                        {
                           answer = answer + new String(buf,0,numRead);
                        }
                    }
                    try {
                       System.out.println("trying to parse:"+answer);
                        nuDoc = b.build(answer.trim(), null);
                        String status=nuDoc.getRootElement().getAttribute("val").getValue();
                      //  System.out.println("status="+status);
                        success=(status.equals("good"));
                        break;
                    }
                    catch (Exception ex) {
                        //Exceptions.printStackTrace(ex);
                        System.err.println("parse error count=" + count +"size o/p:"+answer.length());
                        count = count + 1;
                        trySleep(2*(count));
                        continue;

                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    
}