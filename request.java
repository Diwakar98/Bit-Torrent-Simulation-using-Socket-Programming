import java.io.*;
import java.net.*;
import java.util.*;
import java.security.MessageDigest;

public class request extends Thread{
    public static int no_of_packets;
    public int start,end,message_size;
    public String hostname;
    public StringBuilder data;
    Socket clientSocket;
    private int flag;
    public boolean done;
    request(int start, int end, int message_size, String hostname, Socket clientSocket, int flag){
        this.start = start;
        this.end = end;
        this.message_size = message_size;
        this.hostname = hostname;
        this.clientSocket = clientSocket;
        this.flag = flag;
    }   
    public void run(){
        try{
            read();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    public static String createChecksum(String filename) throws Exception {
       InputStream fis =  new FileInputStream(filename);
       byte[] buffer = new byte[1024];
       MessageDigest complete = MessageDigest.getInstance("MD5");
       int numRead;
       do{
           numRead = fis.read(buffer);
           if (numRead > 0) {
               complete.update(buffer, 0, numRead);
           }
       }while (numRead != -1);

       fis.close();
       byte[] b = complete.digest();
       String result = "";
       for (int i=0; i < b.length; i++) {
           result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring(1);
       }
       return result;
   }
    public void read()throws IOException{
        int start = this.start;
        int end = this.end;
        int message_size = this.message_size;
        String hostname = this.hostname;
        if(flag==1){
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());    
            String sentence = "GET /big.txt HTTP/1.1\r\nHost: "+hostname+"\r\nConnection: keep-alive\r\nRange: bytes=";
            while(start <= end){
                int last = Math.min(end,start+message_size-1);
                outToServer.writeBytes(sentence+start+"-"+last+"\r\n\r\n");
                start += message_size;
            }
            System.out.println("Get: \t"+this.start+"-"+this.end+" \t"+this.hostname);
        }
        else if(flag==0){
            int packets = 0;
            try{
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                int receivedData = 0;
                String s2= "", s3="";
                boolean flag = false;
                String parsing_str2 = "Content-Length: ";
                String parsing_str3 = "Content-Range: bytes ";
                int parsing_body1 = 0;
                int parsing_body2 = 0;
                int parsing_body3 = 0;
                int parsing_body4 = 0;
                int content_length = 0;
                int start_byte_range = 0;
                int end_byte_range = 0;
                StringBuilder sb1 = new StringBuilder();
                while(receivedData != -1){
                    receivedData = inFromServer.read();
                    parsing_body1 = parsing_body2;
                    parsing_body2 = parsing_body3;
                    parsing_body3 = parsing_body4;
                    parsing_body4 = receivedData;
                    if (receivedData != -1){

                        if(s2.length()<16) s2+=(char)receivedData;
                        else s2 = s2.substring(1) + (char)receivedData;

                        if(s3.length()<21) s3+=(char)receivedData;
                        else s3 = s3.substring(1) + (char)receivedData;

                        if(s3.equals(parsing_str3)){
                            String temp = "";
                            while((receivedData = inFromServer.read()) != 45){
                                temp+=(char)receivedData;
                            }
                            start_byte_range = Integer.parseInt(temp);
                            temp = "";
                            while((receivedData = inFromServer.read()) != 47){
                                temp+=(char)receivedData;
                            }
                            end_byte_range = Integer.parseInt(temp);
                        }

                        if(parsing_body1==13 && parsing_body2==10 && parsing_body3==13 && parsing_body4==10){
                            flag = true;
                        }

                        if(s2.equals(parsing_str2)){
                            String temp = "";
                            while((receivedData = inFromServer.read()) != 10){
                                temp+=(char)receivedData;
                            }
                            temp = temp.substring(0,temp.length()-1);
                            content_length = Integer.parseInt(temp);
                        }

                        if(flag){
                            for(int i=0; i<content_length; i++){
                                receivedData = inFromServer.read();
                                sb1.append((char)receivedData);
                            }
                            flag = false;
                            packets++;
                        }
                    }
                }
                request.no_of_packets = request.no_of_packets + packets;
                this.data = sb1;
                this.done = true;
                System.out.println("Received: \t"+this.start+"-"+this.end+" \t"+this.hostname);
            }
            catch(Exception e){
                // request.no_of_packets = request.no_of_packets - 1;
                System.out.println("READ TIMEOUT: \t\t"+this.start+"-"+this.end);
            }
        } 
    }
    public static void main(String[] args) throws Exception {
        String md5_original = "70a4b9f4707d258f559f91615297a3ec";
        Scanner sc = new Scanner(new File("input.csv"));
        sc.useDelimiter(",");
        int start = 0;
        int size = 10000;
        int total_size = 6488666;

        ArrayList<String> domains = new ArrayList<String>();
        ArrayList<Integer> tcp_conn = new ArrayList<Integer>();
        ArrayList<Long> start_time = new ArrayList<Long>();
        ArrayList<Long> end_time = new ArrayList<Long>();
        ArrayList<request> requests = new ArrayList<request>();
        ArrayList<request> requests1 = new ArrayList<request>();

        int total_tcp_conn = 0;

        while (sc.hasNext()){
            domains.add(sc.next().trim());
            int temp = Integer.parseInt(sc.next().trim());
            tcp_conn.add(temp);   
            total_tcp_conn+=temp;
        }
        sc.close();

        request.no_of_packets = 0;
        File file = new File("output1.txt");
        FileWriter fw = new FileWriter(file); 

        long t1 = System.nanoTime();
        for(int i=0; i<domains.size(); i++){
            for(int j=0; j<tcp_conn.get(i); j++){
                try{
                    Socket clientSocket = new Socket(domains.get(i), 80);
                    clientSocket.setSoTimeout(20*1000);
                    request obj = new request(start,Math.min(start + (total_size/total_tcp_conn), total_size-1),10000,domains.get(i),clientSocket,1);
                    request obj1 = new request(start,Math.min(start + (total_size/total_tcp_conn), total_size-1),10000,domains.get(i),clientSocket,0);
                    start = start + (total_size/total_tcp_conn)+1;
                    start_time.add(System.nanoTime());
                    obj.start();
                    obj1.start();
                    requests.add(obj1);
                    requests1.add(obj); 
                }
                catch(Exception e){
                    j--;
                    System.out.println("Some error occured, try again letter");
                }
            }
        }
        for(int i=0; i<requests.size(); i++){
            requests.get(i).join();
            requests1.get(i).join();
            while(requests.get(i).done==false){
                try{
                    Socket clientSocket = new Socket(requests.get(i).hostname, 80);
                    clientSocket.setSoTimeout(20*1000);
                    int start_ = requests.get(i).start;
                    int end_ = requests.get(i).end;
                    int message_size_ = requests.get(i).message_size;
                    request obj = new request(start_,end_,message_size_,requests.get(i).hostname,clientSocket,1);
                    request obj1 = new request(start_,end_,message_size_,requests.get(i).hostname,clientSocket,0);
                    start_time.set(i,System.nanoTime());
                    obj.start();
                    obj1.start();
                    requests.set(i,obj1);
                    requests1.set(i,obj);
                    obj.join();
                    obj1.join();
                }   
                catch(Exception e){
                    // e.printStackTrace();
                    // System.out.println("Reconnecting");
                }
            }
            end_time.add(System.nanoTime());
            requests.get(i).clientSocket.close();
        }
        long t2 = System.nanoTime();
        for(int i=0; i<requests.size(); i++){
            fw.write(requests.get(i).data.toString());
        }
        fw.flush();
        fw.close();

        System.out.println("\nFILE DOWNLOADED: "+ request.no_of_packets + " Packets received");
        // System.out.println((double)(t2 - t1) / 1_000_000_000);
        long avg_start_time = 0;
        long avg_end_time = 0;
        
        for(int i=0; i<start_time.size(); i++){
            double time = (double)(end_time.get(i) - start_time.get(i)) / 1_000_000_000;
            avg_start_time += start_time.get(i);
            avg_end_time += end_time.get(i);     
        }

        FileWriter analysis = new FileWriter(new File("analysis.txt"));
        for(int i=0; i<requests.size(); i++){
            double time = (double)(end_time.get(i) - start_time.get(i)) / 1_000_000_000;
            String str = requests.get(i).hostname+" "+(i+1)+":"+time+"\n";
            // System.out.println(str);
            analysis.write(str);
        }
        analysis.flush();
        analysis.close();
        


        System.out.println("TOTAL TIME = "+ (double)(t2 - t1) / 1_000_000_000);
        try{
            System.out.println("MD5 sum of origianl file: \t"+md5_original);
            System.out.println("MD5 sum of downloaded file:\t"+createChecksum("output1.txt"));
        }
        catch(Exception e){
            System.out.println("Error");
        }

    }
}