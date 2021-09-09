package samples;
public class BlackBoard {
    public static void main() throws Exception{
        try {
            int x = 0; //source
            if (x==0){
                x = x + 1;
            }
            x = x + 2; //sink
        }
        finally {
            System.out.println("Message");
        }
    }



}